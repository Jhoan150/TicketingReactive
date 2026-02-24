# Nequi Ticketing Reactive Platform - Task Checklist

## Phase 1: Project Setup & Architecture (Java 25 + Spring Boot 4)
- [x] Initialize Spring Boot 4.x project with WebFlux (Java 25)
- [x] Define Clean Architecture package structure
- [x] Configure Docker & docker-compose.yml (DynamoDB Local + LocalStack SQS)
- [x] Configure application.yml / environment variables

## Phase 2: Domain Layer
- [x] Define domain entities: Event, Ticket, Order
- [x] Define ticket states: AVAILABLE, RESERVED, PENDING_CONFIRMATION, SOLD, COMPLIMENTARY
- [x] Define domain exceptions and value objects
- [x] Define repository interfaces (ports)

## Phase 3: Use Cases Layer (Application)
- [x] CreateEvent use case
- [x] GetAvailableEvents use case
- [x] ReserveTickets use case (temporal reservation, 10 min)
- [x] ProcessPurchaseOrder use case (async, SQS consumer)
- [x] QueryOrderStatus use case
- [x] ReleaseExpiredReservations scheduled use case
- [x] GetRealTimeAvailability use case

## Phase 4: Infrastructure Layer
- [x] DynamoDB adapters (EventRepository, TicketRepository, OrderRepository)
- [x] SQS producer (enqueue purchase orders)
- [x] SQS consumer (async order processing)
- [x] Optimistic locking / conditional writes for inventory
- [x] Scheduled job for releasing expired reservations

## Phase 5: API Layer (WebFlux Controllers)
- [x] POST /events - Create event
- [x] GET /events - List events
- [x] GET /events/{id}/availability - Real-time availability (Flux/SSE)
- [x] POST /orders - Create purchase order
- [x] GET /orders/{id} - Query order status

## Phase 6: Testing
- [x] Unit tests for use cases (JUnit 5 + Mockito)
- [x] Reactive component tests (reactor-test / WebFluxTest)
- [x] Concurrency tests (Shell script validation)
- [x] Achieve 90%+ coverage (Configured via JaCoCo)

## Phase 7: Documentation & Deliverables
- [x] README.md with setup, commands, architectural decisions
- [x] Postman / curl collection
- [x] Architecture and sequence diagrams
- [x] (Optional/Differential) Terraform IaC
