package com.nequi.ticketing.domain.model;

import com.nequi.ticketing.domain.enums.OrderStatus;

import java.time.Instant;

/**
 * Represents a purchase order submitted by a user for tickets to an event.
 *
 * @param orderId   unique identifier (UUID)
 * @param eventId   identifier of the event tickets are purchased for
 * @param userId    identifier of the user making the purchase
 * @param quantity  number of tickets requested
 * @param status    current state of this order
 * @param failureReason optional message when order fails or is cancelled
 * @param createdAt timestamp when the order was submitted
 * @param updatedAt timestamp of the last state update
 */
public record Order(
        String orderId,
        String eventId,
        String userId,
        int quantity,
        OrderStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Returns a new Order with the given updated status.
     *
     * @param newStatus the new status to apply
     * @return updated order record
     */
    public Order withStatus(OrderStatus newStatus) {
        return new Order(orderId, eventId, userId, quantity,
                newStatus, failureReason, createdAt, Instant.now());
    }

    /**
     * Returns a new Order with CANCELLED status and a reason.
     *
     * @param reason explanation for cancellation
     * @return updated order record
     */
    public Order cancelled(String reason) {
        return new Order(orderId, eventId, userId, quantity,
                OrderStatus.CANCELLED, reason, createdAt, Instant.now());
    }

    /**
     * Returns a new Order with FAILED status and a reason.
     *
     * @param reason explanation for the failure
     * @return updated order record
     */
    public Order failed(String reason) {
        return new Order(orderId, eventId, userId, quantity,
                OrderStatus.FAILED, reason, createdAt, Instant.now());
    }
}
