#!/bin/bash
# ──────────────────────────────────────────────────────────────────────────────
# LocalStack Initialization Script
# Creates DynamoDB tables and SQS queue for the Ticketing Reactive application.
# ──────────────────────────────────────────────────────────────────────────────

set -e
set -x

ENDPOINT="http://localstack:4566"
REGION="us-east-1"

echo "⏳ Waiting for LocalStack to be ready..."
until curl -sf "$ENDPOINT/_localstack/health" | grep -q '"dynamodb"' && curl -sf "$ENDPOINT/_localstack/health" | grep -q '"sqs"'; do
    sleep 2
done
echo "✅ LocalStack is ready."

# ─── DynamoDB Tables ─────────────────────────────────────────────────────────

echo "📦 Creating DynamoDB table: events"
aws dynamodb create-table \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --table-name events \
    --attribute-definitions \
        AttributeName=eventId,AttributeType=S \
    --key-schema \
        AttributeName=eventId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --no-cli-pager || echo "events table already exists"

echo "📦 Creating DynamoDB table: tickets"
aws dynamodb create-table \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --table-name tickets \
    --attribute-definitions \
        AttributeName=ticketId,AttributeType=S \
        AttributeName=eventId,AttributeType=S \
        AttributeName=status,AttributeType=S \
        AttributeName=orderId,AttributeType=S \
    --key-schema \
        AttributeName=ticketId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --global-secondary-indexes \
        "[
          {
            \"IndexName\": \"eventId-status-index\",
            \"KeySchema\": [
              {\"AttributeName\":\"eventId\",\"KeyType\":\"HASH\"},
              {\"AttributeName\":\"status\",\"KeyType\":\"RANGE\"}
            ],
            \"Projection\":{\"ProjectionType\":\"ALL\"}
          },
          {
            \"IndexName\": \"orderId-index\",
            \"KeySchema\": [
              {\"AttributeName\":\"orderId\",\"KeyType\":\"HASH\"}
            ],
            \"Projection\":{\"ProjectionType\":\"ALL\"}
          }
        ]" \
    --no-cli-pager || echo "tickets table already exists"

echo "📦 Creating DynamoDB table: orders"
aws dynamodb create-table \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --table-name orders \
    --attribute-definitions \
        AttributeName=orderId,AttributeType=S \
    --key-schema \
        AttributeName=orderId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --no-cli-pager || echo "orders table already exists"

# ─── SQS Queue  ──────────────────────────────────────

echo "📨 Creating SQS FIFO queue: purchase-orders-queue.fifo"
aws sqs create-queue --endpoint-url "$ENDPOINT" --region "$REGION" --queue-name "purchase-orders-queue.fifo" --attributes FifoQueue=true,VisibilityTimeout=30,MessageRetentionPeriod=86400 --no-cli-pager || echo "SQS queue already exists"

echo "🔍 Verifying SQS queue creation..."
aws sqs list-queues --endpoint-url "$ENDPOINT" --region "$REGION"

echo ""
echo "✅ All AWS resources initialized successfully!"
echo "   DynamoDB tables: events, tickets, orders"
echo "   SQS queue:       purchase-orders-queue.fifo"
