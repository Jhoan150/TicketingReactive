# Colección de Comandos cURL - Nequi Ticketing Reactive

Puedes importar estos comandos directamente en Postman (File > Import > Raw text) o ejecutarlos en tu terminal.

## 1. Eventos

### Crear un nuevo evento
```bash
curl --location 'http://localhost:8080/api/v1/events' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Concierto Karol G",
    "date": "2026-08-15T20:00:00",
    "venue": "Estadio Atanasio Girardot",
    "totalCapacity": 100
}'
```

### Listar todos los eventos con disponibilidad
```bash
curl --location 'http://localhost:8080/api/v1/events'
```

### Consultar disponibilidad específica de un evento
*(Reemplaza {eventId} con un ID real)*
```bash
curl --location 'http://localhost:8080/api/v1/events/{eventId}/availability'
```

---

## 2. Órdenes y Compras

### Iniciar una reserva/compra de tickets
*(Reemplaza {eventId} con un ID real)*
```bash
curl --location 'http://localhost:8080/api/v1/orders' \
--header 'Content-Type: application/json' \
--data '{
    "eventId": "{eventId}",
    "userId": "user_id_123",
    "quantity": 2
}'
```

### Consultar estado de una orden
*(Reemplaza {orderId} con el ID recibido en el paso anterior)*
```bash
curl --location 'http://localhost:8080/api/v1/orders/{orderId}'
```

---

## 3. Utilidades de Salud (Actuator)

### Verificar estado del sistema
```bash
curl --location 'http://localhost:8080/actuator/health'
```
