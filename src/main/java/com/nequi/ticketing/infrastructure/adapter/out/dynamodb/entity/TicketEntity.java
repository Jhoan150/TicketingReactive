package com.nequi.ticketing.infrastructure.adapter.out.dynamodb.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

/**
 * DynamoDB table entity for Ticket persistence.
 * Table: "tickets"
 * PK: ticketId (String)
 * GSI: eventId-status-index (eventId + status) for efficient status queries
 */
@DynamoDbBean
public class TicketEntity {

    private String ticketId;
    private String eventId;
    private String status;
    private String orderId;
    private String reservedAt;
    private String expiresAt;
    private String createdAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("ticketId")
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    @DynamoDbSecondaryPartitionKey(indexNames = {"eventId-status-index", "eventId-index"})
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    @DynamoDbSecondarySortKey(indexNames = {"eventId-status-index"})
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @DynamoDbSecondaryPartitionKey(indexNames = {"orderId-index"})
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getReservedAt() { return reservedAt; }
    public void setReservedAt(String reservedAt) { this.reservedAt = reservedAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
