package com.nequi.ticketing.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.*;

/**
 * HTTP request body for initiating a ticket purchase.
 *
 * @param eventId  identifier of the event to purchase tickets for
 * @param userId   identifier of the purchasing user
 * @param quantity number of tickets to purchase (1-10 per request)
 */
public record CreateOrderRequest(
        @NotBlank(message = "Event ID is required")
        String eventId,

        @NotBlank(message = "User ID is required")
        String userId,

        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 10, message = "Cannot purchase more than 10 tickets per order")
        int quantity
) {}
