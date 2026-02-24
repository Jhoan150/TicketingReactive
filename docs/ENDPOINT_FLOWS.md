# Guía de Flujos de Ejecución por Endpoint

Esta documentación detalla la secuencia exacta de llamadas, desde que llega la petición HTTP hasta que se entrega la respuesta, siguiendo los principios de **Clean Architecture**.

---

## 1. Crear Evento (`POST /api/v1/events`)
**Propósito**: Registrar un nuevo evento y pre-generar sus entradas en el inventario.

1.  **Adaptador In (Web)**: `EventController.createEvent(CreateEventRequest)`
    - Valida el DTO.
    - Mapea a un comando de dominio.
2.  **Caso de Uso (Application)**: `CreateEventUseCaseImpl.execute(command)`
    - Crea la entidad `Event`.
    - Llama a `eventRepository.save(event)`.
    - Genera una lista de `Ticket` (estado `AVAILABLE`) basada en la capacidad total.
    - Llama a `ticketRepository.saveAll(tickets)`.
3.  **Adaptador Out (Infrastructure)**: `EventDynamoDbAdapter` / `TicketDynamoDbAdapter`
    - Realiza los `PutItem` en DynamoDB.
4.  **Respuesta**: Devuelve `201 Created` con el `EventResponse`.

---

## 2. Listar Eventos (`GET /api/v1/events`)
**Propósito**: Consultar todos los eventos disponibles.

1.  **Adaptador In (Web)**: `EventController.getAllEvents()`
2.  **Caso de Uso (Application)**: `GetAvailableEventsUseCaseImpl.execute()`
    - Llama a `eventRepository.findAll()`.
3.  **Adaptador Out (Infrastructure)**: `EventDynamoDbAdapter.findAll()`
    - Ejecuta un `Scan` o `Query` en DynamoDB.
4.  **Respuesta**: Devuelve un `Flux<EventResponse>` (Reactivo).

---

## 3. Reservar Boletas (`POST /api/v1/orders`)
**Propósito**: Iniciar el proceso de compra con bloqueo optimista y reserva temporal.

1.  **Adaptador In (Web)**: `OrderController.reserveTickets(CreateOrderRequest)`
2.  **Caso de Uso (Application)**: `ReserveTicketsUseCaseImpl.execute(command)`
    - **Paso A**: Busca el evento (`eventRepository.findById`).
    - **Paso B**: Valida stock.
    - **Paso C (Crítico)**: Llama a `eventRepository.updateWithOptimisticLock`. 
        - Si falla por conflicto de versión, el `retryWhen` reintenta el flujo con **Jitter**.
    - **Paso D**: Busca N tickets `AVAILABLE` en `TicketRepository`.
    - **Paso E**: Cambia el estado de los tickets a `RESERVED` con un TTL de 10 min.
    - **Paso F**: Crea la `Order` en estado `PENDING`.
    - **Paso G**: Llama a `orderQueue.enqueue(order)` (Publica a **SQS**).
3.  **Adaptador Out (Infrastructure)**: `OrderSqsAdapter` envía el mensaje a la cola FIFO.
4.  **Respuesta**: Devuelve `202 Accepted` con el `orderId`.

---

## 4. Consultar Estado de Orden (`GET /api/v1/orders/{id}`)
**Propósito**: Saber si la compra asíncrona ya terminó.

1.  **Adaptador In (Web)**: `OrderController.getOrderStatus(id)`
2.  **Caso de Uso (Application)**: `GetOrderStatusUseCaseImpl.execute(id)`
    - Llama a `orderRepository.findById(id)`.
3.  **Adaptador Out (Infrastructure)**: `OrderDynamoDbAdapter.findById(id)`.
4.  **Respuesta**: Devuelve la `Order` con su estado actual (PENDING, PROCESSING, CONFIRMED, FAILED).

---

## 5. Disponibilidad en Tiempo Real (`GET /api/v1/events/{id}/availability`)
**Propósito**: Streaming de datos (SSE) sobre el stock actual.

1.  **Adaptador In (Web)**: `EventController.getEventAvailability(id)`
    - Configura el header `text/event-stream`.
2.  **Caso de Uso (Application)**: `GetEventAvailabilityUseCaseImpl.execute(id)`
3.  **Flujo Reactivo**: 
    - Emite el valor actual del stock.
    - (Opcional) Se suscribe a cambios mediante el patrón Observer o Polling reactivo.
4.  **Respuesta**: Mantiene la conexión abierta enviando eventos de stock.

---

## 🔄 Flujos de Background (Fuera de los Endpoints)

### A. Procesamiento de SQS (`OrderConsumer`)
1.  `OrderSqsConsumer` detecta mensaje en SQS.
2.  Llama a `ProcessOrderUseCase.execute(orderId)`.
3.  `ProcessOrderUseCaseImpl`:
    - Cambia estado de boletas de `RESERVED` a `SOLD`.
    - Actualiza el estado de la `Order` a `CONFIRMED`.
    - Persiste todo en DynamoDB.

### B. Liberación de Expirados (`ReleaseExpiredJob`)
1.  Cada 60s, `ReleaseExpiredReservationsUseCaseImpl.scheduleRelease()` se dispara.
2.  Busca tickets `RESERVED` donde `now > expiresAt`.
3.  Llama a `ticket.release()` (vuelve a `AVAILABLE`).
4.  Llama a `event.withReleasedTickets(n)` para devolver el stock al contador global del evento.
5.  Actualiza DynamoDB usando bloqueo optimista.
