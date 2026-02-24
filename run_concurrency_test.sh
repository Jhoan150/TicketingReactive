#!/bin/bash

# --- PRUEBA DE CONCURRENCIA "FUEGO REAL" ---
# Este script lanza 10 peticiones simultáneas para comprar 2 tickets cada una
# sobre un evento que solo tiene 10 tickets en total.
# Resultado esperado: 5 éxitos, 5 fallos, Stock final = 0.

API_URL="http://localhost:8080/api/v1"

echo "1. CREANDO EVENTO CON 10 TICKETS..."
# Crear el evento y extraer el eventId
EVENT_JSON=$(curl -s -X POST "$API_URL/events" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Concierto Concurrente",
    "date": "2026-06-01T20:00:00",
    "venue": "Estadio WSL",
    "totalCapacity": 10
  }')

EVENT_ID=$(echo $EVENT_JSON | grep -o '"eventId":"[^"]*' | cut -d'"' -f4)

if [ -z "$EVENT_ID" ]; then
    echo "❌ Error: No se pudo crear el evento o extraer el ID."
    echo "Respuesta: $EVENT_JSON"
    exit 1
fi

echo "✅ Evento creado con ID: $EVENT_ID"
echo "------------------------------------------------"
echo "2. LANZANDO 10 COMPRAS SIMULTÁNEAS (2 TICKETS CADA UNA)..."

# Lanzar 10 peticiones en paralelo
for i in {1..10}
do
  curl -s -X POST "$API_URL/orders" \
    -H "Content-Type: application/json" \
    -d "{
      \"eventId\": \"$EVENT_ID\",
      \"userId\": \"user_$i\",
      \"quantity\": 2
    }" -w "\nProceso $i: HTTP %{http_code}\n" &
done

# Esperar a que terminen todos los procesos de fondo
wait

echo "------------------------------------------------"
echo "3. VALIDANDO STOCK FINAL..."
curl -s "$API_URL/events/$EVENT_ID/availability" | grep -o '"availableTickets":[0-9]*'
echo ""
echo "Prueba terminada. Revisa los códigos HTTP arriba (202 = Success, 4xx/5xx = Fallo esperado por falta de stock)."
