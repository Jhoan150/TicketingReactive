package com.nequi.ticketing.infrastructure.adapter.in.web.dto;

import java.time.Instant;

/**
 * HTTP response body for an order status query.
 *
 * @param orderId       unique order identifier
 * @param eventId       associated event
 * @param userId        user who placed the order
 * @param quantity      number of tickets ordered
 * @param status        current order status
 * @param failureReason reason for failure or cancellation (nullable)
 * @param createdAt     order creation timestamp
 * @param updatedAt     last status update timestamp
 */
public record OrderResponse(
        String orderId,
        String eventId,
        String userId,
        int quantity,
        String status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {}
