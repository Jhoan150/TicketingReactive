# Diagramas de Arquitectura - Nequi Ticketing Reactive

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
            UC->>Q: Enviar Mensaje (OrderCreated)
            UC-->>A: OrderResponse (202 Accepted)
            A-->>C: 202 Accepted (orderId)
        else Fallo (Conflicto/Sin Stock)
            D-->>UC: Error / Conflict
            UC-->>A: Error (InsufficientStock)
            A-->>C: 409 Conflict / 400 Bad Request
        end
    end

    Note over Q,D: Procesamiento Asíncrono
    Q->>UC: Consumer: ProcessOrder
    UC->>D: Finalizar Tickets (SOLD)
    UC->>D: Update Order (COMPLETED)
```

## 3. Modelo de Datos (DynamoDB)

### Tabla: `events`
- `eventId` (Hash Key)
- `availableTickets`
- `version` (para Optimistic Locking)

### Tabla: `tickets`
- `ticketId` (Hash Key)
- `eventId` (GSI)
- `status` (AVAILABLE, SOLD, RESERVED)
- `orderId` (GSI)

### Tabla: `orders`
- `orderId` (Hash Key)
- `status` (PENDING, COMPLETED, FAILED)
- `totalAmount`
```
