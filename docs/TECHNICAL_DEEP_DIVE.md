# Guía Técnica: Plataforma de Ticketing Reactiva (Deep Dive)

Esta guía ha sido diseñada para ayudarte a presentar el proyecto ante el equipo técnico de Nequi, destacando las decisiones de ingeniería, el stack tecnológico y los flujos críticos.

---

## 1. Arquitectura y Stack Tecnológico

### ¿Por qué esta arquitectura?
Hemos implementado **Clean Architecture** (Arquitectura Limpia) para garantizar que las reglas de negocio sean independientes de la infraestructura (DynamoDB, SQS, WebFlux). 

- **Domain Layer**: Contiene la lógica pura (Entidades, Excepciones, Puertos). Es el "corazón" del sistema.
- **Application/Use Cases Layer**: Coordina la ejecución de las reglas de negocio (ej. `ReserveTicketsUseCase`).
- **Infrastructure Layer**: Contiene los adaptadores externos (DynamoDB para persistencia, SQS para mensajería).

### El Stack de Vanguardia (Java 25)
- **Java 25**: Usamos `Records` para inmutabilidad y `Context-Propagation` mejorado.
- **Spring WebFlux**: Programación reactiva no bloqueante. Clave para manejar miles de conexiones con pocos recursos.
- **DynamoDB**: Base de datos NoSQL líder en latencia de milisegundos.
- **SQS FIFO**: Garantiza que las órdenes se procesen en el orden exacto en que llegaron, sin duplicados.

---

## 2. El Desafío de la Concurrencia (Optimistic Locking)

Uno de los puntos más críticos de la prueba es evitar la **sobreventa**.

### Cómo funciona nuestra solución:
1. Usamos **Optimistic Locking** mediante la anotación `@DynamoDbVersionAttribute` en la entidad `Event`.
2. Cada evento tiene una versión académica (ej. Versión 5).
3. Si 10 personas intentan comprar al mismo tiempo, las 10 leen la "Versión 5".
    - El primero que llega actualiza el stock y sube la versión a 6.
    - Los otros 9, al intentar escribir, envían: *"Actualiza si la versión todavía es 5"*. DynamoDB rechaza estas peticiones automáticamente (error 400).
4. **Resiliencia**: Implementamos una estrategia de **Retry with Jitter**:
    - Si hay un choque, el sistema espera unos milisegundos aleatorios y reintenta.
    - Esto permite que el flujo sea fluido sin que el usuario reciba errores al primer choque.

---

## 3. Flujo Crítico de Compra (Asíncronía y Mensajería)

Para garantizar tiempos de respuesta ultra-bajos, dividimos el proceso en dos:

### Fase 1: Reserva Temporal (Síncrona/Rápida)
- El usuario pide tickets.
- Validamos stock en DynamoDB.
- Creamos una orden en estado `PENDING`.
- **Encolamos en SQS**.
- Respondemos al usuario con un `HTTP 202 Accepted` y el `orderId`. El usuario no espera a que se procese el pago o la boleta final.

### Fase 2: Procesamiento (Asíncrono/Background)
- El `OrderConsumer` lee de SQS.
- Valida consistencia.
- Cambia el estado de la orden a `SUCCESS` o `FAILED`.
- El usuario puede consultar el estado final mediante el endpoint `GET /orders/{id}`.

---

## 4. Disponibilidad en Tiempo Real (SSE - Bonus Técnico)

Implementamos un flujo de **Server-Sent Events (SSE)** en el endpoint de disponibilidad. 
- En lugar de que el frontend haga "polling" cada segundo, el servidor mantiene una conexión abierta y "empuja" las actualizaciones de stock conforme ocurren. Esto es el estándar de oro en aplicaciones reactivas modernas.

---

## 5. Infraestructura y Calidad

- **Terraform**: Hemos incluido código de Infraestructura como Código (IaC) para demostrar cómo se despliega esto en AWS de forma repetible.
- **JaCoCo (90%+)**: Se exige una cobertura de tests muy alta para asegurar que cualquier cambio futuro no rompa la lógica central.
- **Docker**: Todo el entorno (LocalStack para DynamoDB/SQS) se levanta con un solo comando, facilitando la portabilidad.

---

## 6. Cumplimiento de Notas Generales (Negocio)

Aquí tienes la evidencia técnica de cómo cumplimos con las "Notas Generales" del PDF para tu presentación:

1. **Estado único**: La entidad `Ticket` es un `record` de Java con un campo `status` de tipo Enum `TicketStatus`. Por definición, un ticket no puede tener dos estados simultáneamente.
2. **Atomicidad y Auditoría**:
   - **Atomicidad**: Garantizada por DynamoDB en `EventDynamoDbAdapter.java`. Cada cambio de estado usa Escrituras Condicionales (`updateWithOptimisticLock`). Si el estado cambió mientras procesábamos, la operación falla atómicamente.
   - **Auditoría**: Cada `Ticket` y `Order` tiene timestamps (`createdAt`, `updatedAt`, `reservedAt`). Además, la persistencia en DynamoDB deja un rastro inmutable de la última transición exitosa.
3. **Estados no-venta**: Los estados `RESERVED` y `PENDING_CONFIRMATION` están aislados. Solo cuando el flujo asíncrono termina exitosamente en `ProcessOrderUseCaseImpl.java`, el ticket pasa a `SOLD`.
4. **SOLD es irreversible**: En todo el código base, no existe ningún método o Use Case que permita mover un ticket del estado `SOLD` de regreso a otro estado. El método `sell()` en `Ticket.java` es el sumidero final del flujo.
5. **COMPLIMENTARY no contable**: Hemos creado el método `complimentary()` en el dominio. Al ser un estado distinto a `SOLD`, los reportes contables pueden filtrar fácilmente por `status = SOLD` para obtener solo las ventas reales, cumpliendo con el requerimiento de negocio.

---

## Tips para la Presentación:
1. **Enfatiza la "Reactividad"**: No bloqueamos hilos; usamos el modelo de Event Loop.
2. **Explica el Retry**: No es solo reintentar, es el **Jitter** (latencia aleatoria) lo que evita que los procesos choquen en bucle.
3. **Clean Architecture**: Menciona que si mañana Nequi cambia DynamoDB por MongoDB, solo tendrías que cambiar un adaptador en la capa de infraestructura, sin tocar el dominio.

¡Mucho éxito en tu presentación! Tienes un proyecto sólido que demuestra dominio de sistemas distribuidos y arquitectura moderna.
