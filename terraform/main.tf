# Terraform configuration for Nequi Ticketing Infrastructure
# This defines the required AWS resources (DynamoDB and SQS)

provider "aws" {
  region                      = "us-east-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    dynamodb = "http://localhost:4566"
    sqs      = "http://localhost:4566"
  }
}

# 1. DynamoDB: Events Table
resource "aws_dynamodb_table" "events" {
  name         = "events"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "eventId"

  attribute {
    name = "eventId"
    type = "S"
  }
}

# 2. DynamoDB: Tickets Table
resource "aws_dynamodb_table" "tickets" {
  name         = "tickets"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "ticketId"

  attribute {
    name = "ticketId"
    type = "S"
  }

  attribute {
    name = "eventId"
    type = "S"
  }

  attribute {
    name = "status"
    type = "S"
  }

  attribute {
    name = "orderId"
    type = "S"
  }

  # GSI for availability queries
  global_secondary_index {
    name            = "eventId-status-index"
    hash_key        = "eventId"
    range_key       = "status"
    projection_type = "ALL"
  }

  # GSI for order relative queries
  global_secondary_index {
    name            = "orderId-index"
    hash_key        = "orderId"
    projection_type = "ALL"
  }
}

# 3. DynamoDB: Orders Table
resource "aws_dynamodb_table" "orders" {
  name         = "orders"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "orderId"

  attribute {
    name = "orderId"
    type = "S"
  }
}

# 4. SQS: Purchase Orders Queue (FIFO)
resource "aws_sqs_queue" "purchase_orders" {
  name                        = "purchase-orders.fifo"
  fifo_queue                  = true
  content_based_deduplication = true
  visibility_timeout_seconds  = 30
}
