# Diagramas de Arquitectura - Ticketing Reactive

Este documento contiene la representación visual de la arquitectura y los flujos críticos del sistema.

## 1. Arquitectura de Alto Nivel

El sistema utiliza una **Arquitectura Hexagonal (Clean Architecture)** impulsada por eventos y programación reactiva.

```mermaid
graph TD
    User((Usuario/Cliente))
    
    subgraph "Infraestructura (Adaptadores In)"
        API[Spring WebFlux / REST]
    end
    
    subgraph "Aplicación (Casos de Uso)"
        UC_Reserve[ReserveTicketsUseCase]
        UC_Process[ProcessPurchaseUseCase]
    end
    
    subgraph "Dominio"
        Model[Entidades: Evento, Orden, Ticket]
    end
    
    subgraph "Infraestructura (Adaptadores Out)"
        DDB[(DynamoDB)]
        SQS[[Amazon SQS FIFO]]
    end

    User --> API
    API --> UC_Reserve
    UC_Reserve --> Model
    UC_Reserve --> DDB
    UC_Reserve --> SQS
    
    SQS -.-> UC_Process
    UC_Process --> DDB
```

## 2. Flujo de Reserva de Tickets (Secuencia)

Este diagrama muestra cómo se maneja la concurrencia y la asincronía durante una compra.

```mermaid
sequenceDiagram
    participant C as Cliente
    participant A as OrderController (WebFlux)
    participant UC as ReserveTicketsUseCase
    participant D as DynamoDB (Event/Ticket)
    participant Q as SQS (FIFO Queue)

    C->>A: POST /orders (eventId, qty)
    A->>UC: execute(command)
    
    rect rgba(200, 230, 255, 0.26)
        Note right of UC: Bloqueo Optimista
        UC->>D: Update Event (check version)
        alt Éxito (Stock disponible)
            D-->>UC: OK (version updated)
            UC->>D: Create Order (PENDING)
            Note over UC,C: En un flujo real: Aquí se redirige a Pasarela de Pagos
            UC->>Q: Enviar Mensaje (OrderCreated)
            UC-->>A: OrderResponse (202 Accepted)
            A-->>C: 202 Accepted (orderId)
        else Fallo (Conflicto/Sin Stock)
            D-->>UC: Error / Conflict
            UC-->>A: Error (InsufficientStock)
            A-->>C: 409 Conflict / 400 Bad Request
        end
    end

    Q->>UC: Consumer: ProcessOrder
    UC->>D: Finalizar Tickets (SOLD)
    UC->>D: Update Order (COMPLETED)
```

## 3. Diagrama de Casos de Uso (UML)

Este mapa muestra las interacciones de los actores con las funcionalidades del sistema.

```mermaid
graph LR
    Admin[Administrador]
    User[Cliente]
    Job[Sistema / Cron Job]

    subgraph "Ticketing Platform"
        UC1(Crear Evento e Inventario)
        UC2(Listar Eventos Disponibles)
        UC3(Reservar Tickets)
        UC4(Consultar Estado de Orden)
        UC5(Monitorear Disponibilidad SSE)
        UC6(Liberar Reservas Expiradas)
    end

    Admin --> UC1
    User --> UC2
    User --> UC3
    User --> UC4
    User --> UC5
    Job --> UC6
```

## 4. Flujo: Creación de Evento (Secuencia)

```mermaid
sequenceDiagram
    participant A as Administrador
    participant C as EventController
    participant UC as CreateEventUseCase
    participant ER as EventRepository
    participant TR as TicketRepository

    A->>C: POST /events (name, capacity)
    C->>UC: execute(command)
    UC->>ER: save(EventEntity)
    Note over UC,TR: Generación de N Tickets
    UC->>TR: saveAll(List<Ticket>)
    TR-->>UC: OK
    UC-->>C: EventResponse
    C-->>A: 201 Created
```

## 5. Flujo: Disponibilidad en Tiempo Real

```mermaid
sequenceDiagram
    participant C as Cliente
    participant API as EventController
    participant UC as GetAvailabilityUseCase
    participant D as DynamoDB

    C->>API: GET /events/{id}/availability
    API->>UC: execute(id)
    loop Cada intervalo/cambio
        UC->>D: findById(id)
        D-->>UC: Event (stock actual)
        UC-->>API: Stream Data
        API-->>C: Data: {available: 45}
    end
```

## 6. Diagrama de Estados: Ciclo de Vida del Ticket

Indispensable para entender las "Notas Generales" del negocio.

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE: Creación de Evento
    AVAILABLE --> RESERVED: POST /orders (Reserva Temporal)
    AVAILABLE --> COMPLIMENTARY: Asignación Manual
    RESERVED --> AVAILABLE: Expiración (10 min) / Cancelación
    RESERVED --> PENDING_CONFIRMATION: Inicio de Pago
    PENDING_CONFIRMATION --> SOLD: Confirmación Exitosa (SQS)
    PENDING_CONFIRMATION --> AVAILABLE: Fallo de Pago
    SOLD --> [*]: Estado Final
    COMPLIMENTARY --> [*]: Estado Final
```

## 8. Arquitectura de Despliegue en Producción

Esta arquitectura está diseñada para maxima disponibilidad, seguridad y escalabilidad automática en la nube.

```mermaid
graph LR
    User((Usuario/Bot)) --> AGW[AWS API Gateway]
    AGW --> WAF[AWS WAF - Seguridad]
    WAF --> ALB[Application Load Balancer]
    
    subgraph "Amazon EKS (Kubernetes Cluster)"
        Service[K8s Service] --> Pod1[Pod: Ticketing App]
        Service --> Pod2[Pod: Ticketing App]
        Pod1 -.-> SM[AWS Secrets Manager]
    end
    
    ALB --> Service
    
    Pod1 --> DDB[(DynamoDB Serverless)]
    Pod1 --> SQS[[SQS FIFO Queue]]
    
    Pod2 --> DDB
    Pod2 --> SQS
```
