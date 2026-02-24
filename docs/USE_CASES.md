# Diagramas de Casos de Uso y Flujos de Endpoint

Este documento detalla los casos de uso del sistema y los flujos lógicos para cada punto de entrada (Endpoint/Job).

---

## 1. Mapa General de Casos de Uso

Representa las funcionalidades disponibles para cada actor del sistema.

```mermaid
graph LR
    subgraph Actores
        Admin[Administrador]
        User[Persona / Cliente]
        System[Sistema / Cron Job]
    end

    subgraph "Ticketing Platform"
        UC1(Crear Evento e Inventario)
        UC2(Listar Eventos Disponibles)
        UC3(Reservar Tickets - POST/Orders)
        UC4(Consultar Estado de Orden)
        UC5(Monitorear Disponibilidad - SSE)
        UC6(Liberar Reservas Expiradas)
    end

    Admin --> UC1
    User --> UC2
    User --> UC3
    User --> UC4
    User --> UC5
    System --> UC6
```

---

## 2. Flujo: Creación de Evento (`POST /api/v1/events`)

**Caso de Uso:** El administrador registra un nuevo evento y el sistema genera automáticamente el inventario físico (tickets).

```mermaid
sequenceDiagram
    participant A as Admin
    participant C as EventController
    participant UC as CreateEventUseCase
    participant ER as EventRepo
    participant TR as TicketRepo

    A->>C: Envía JSON (nombre, capacidad)
    C->>UC: Inicia flujo reactivo
    UC->>ER: Persiste metadatos del Evento
    loop N veces (Capacidad)
        UC->>UC: Instancia Ticket(AVAILABLE)
    end
    UC->>TR: Batch Save (saveAll)
    TR-->>UC: Multi-insert asíncrono
    UC-->>C: Devuelve Event record
    C-->>A: HTTP 201 Created
```

---

## 3. Flujo: Reserva de Tickets (`POST /api/v1/orders`)

**Caso de Uso:** Un cliente solicita reservar N boletas. Es el flujo más crítico por la concurrencia.

```mermaid
sequenceDiagram
    participant C as Cliente
    participant API as OrderController
    participant UC as ReserveUseCase
    participant DB as DynamoDB
    participant Q as SQS FIFO

    C->>API: Solicita reserva
    API->>UC: Execute(Command)
    
    rect rgba(240, 240, 240, 0.16)
        Note over UC: Bucle de Reintento (Max 15)
        loop Hasta éxito o fin de reintentos
            UC->>DB: findById (Lee Evento + Versión)
            DB-->>UC: Evento Data
            
            alt Stock Insuficiente
                UC-->>API: Error (TicketNotAvailable)
                API-->>C: HTTP 400 Bad Request
            else Stock OK
                Note over UC,DB: Intento de Bloqueo Optimista
                UC->>DB: updateWithOptimisticLock (version = X)
                
                alt Éxito (Versión sigue siendo X)
                    DB-->>UC: OK (Versión ahora es X+1)
                    UC->>DB: findByEventIdAndStatus (Busca N Tickets)
                    UC->>DB: saveAll (Tickets RESERVED)
                    UC->>DB: save (Orden PENDING)
                    UC->>Q: Encola mensaje (OrderCreated)
                    UC-->>API: Retorna Orden
                    API-->>C: HTTP 202 Accepted
                else Conflicto de Versión (alguien más compró)
                    DB-->>UC: Empty/Conflict
                    Note over UC: Espera Jitter (ej. 150ms)
                    UC->>UC: Reintento automático
                end
            end
        end
    end
```

---

## 4. Flujo: Disponibilidad SSE (`GET /events/{id}/availability`)

**Caso de Uso:** El cliente desea ver el stock en tiempo real sin recargar la página.

```mermaid
sequenceDiagram
    participant C as Cliente
    participant API as EventController
    participant UC as GetAvailabilityUseCase
    participant DB as DynamoDB

    C->>API: Suscripción SSE (text/event-stream)
    loop Cada X segundos
        API->>UC: Consulta stock
        UC->>DB: findById(eventId)
        DB-->>UC: Retorna Event record
        UC-->>API: Envía evento SSE
        API-->>C: data: {"available": 15}
    end
```

---

## 5. Flujo: Procesamiento Asíncrono (SQS Consumer)

**Caso de Uso:** Confirmar la venta final tras la reserva exitosa.

```mermaid
sequenceDiagram
    participant Q as SQS Queue
    participant S as SqsConsumer
    participant UC as ProcessOrderUseCase
    participant DB as DynamoDB

    Q->>S: Mensaje disponible
    S->>UC: Process(orderId)
    UC->>DB: Busca Orden PENDING
    UC->>DB: Busca Tickets RESERVED para esa Orden
    UC->>DB: Actualiza Tickets a SOLD
    UC->>DB: Actualiza Orden a CONFIRMED
    UC-->>S: OK
    S->>Q: Borrar mensaje (ACK)
```
