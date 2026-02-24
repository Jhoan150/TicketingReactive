# Ticketing Reactive Platform

Sistema de boletería de alto rendimiento construido con **Java 25**, **Spring Boot 4 (WebFlux)** y arquitectura reactiva. Diseñado para manejar picos de tráfico masivos (como la venta de entradas para conciertos) utilizando procesos no bloqueantes y persistencia escalable.

## Tecnologías Principales

- **Java 25**: Utilizando Virtual Threads y Performance improvements.
- **Spring Boot 4..0.3 (WebFlux)**: Stack reactivo completo para manejo eficiente.
- **DynamoDB**: Base de datos NoSQL para escalabilidad horizontal y baja latencia.
- **SQS**: Procesamiento asíncrono de órdenes con garantía de orden y deduplicación.
- **LocalStack**: Emulación de servicios AWS para desarrollo local.
- **Docker**: Containerización completa del stack.

## Arquitectura

El proyecto sigue los principios de **Arquitectura Limpia (Clean Architecture)**:

- **Domain**: Entidades de negocio, excepciones y lógica pura.
- **Application**: Casos de uso (Use Cases) que coordinan la lógica de negocio.
- **Infrastructure**: Adaptadores para DynamoDB, SQS, y controladores REST.

### Flujo de Compra (Optimistic Locking)
Para evitar la sobreventa, el sistema utiliza **Bloqueo Optimista** en DynamoDB. Cada actualización de inventario valida la versión del documento, asegurando que solo las peticiones con datos consistentes tengan éxito bajo alta concurrencia.

## Configuración y Ejecución

### Requisitos
- Docker y Docker Compose.
- WSL2 (si estás en Windows).

### Levantar el Entorno
Ejecuta el siguiente comando para compilar la aplicación y levantar todos los servicios (App, DynamoDB, SQS):

```bash
docker compose up --build
```

Esto levantará:
- **App**: `http://localhost:8080`
- **LocalStack**: Puertos AWS en `4566`
- **Init**: Script automático que crea las tablas y colas necesarias.

## Pruebas de Desarrollo

### Prueba de Concurrencia
He incluido un script de estrés para validar que el sistema no permite sobreventa bajo ataque simultáneo:

```bash
chmod +x run_concurrency_test.sh
./run_concurrency_test.sh
```

### Documentación de API
Puedes encontrar ejemplos de peticiones en la carpeta `docs/requests.http`. 

**Endpoints Principales:**
- `POST /api/v1/events`: Crear evento.
- `GET /api/v1/events`: Listar eventos disponibles.
- `POST /api/v1/orders`: Iniciar reserva/compra de tickets.
- `GET /api/v1/orders/{id}`: Consultar estado de la orden.
- `GET /api/v1/events/{id}/availability`: Disponibilidad en tiempo real.
