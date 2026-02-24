package com.nequi.ticketing.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * HTTP request body for creating a new event.
 *
 * @param name          event display name
 * @param date          event date-time in ISO-8601 format (e.g. "2026-06-01T20:00:00")
 * @param venue         venue name
 * @param totalCapacity total number of tickets to generate (1-50000)
 */
public record CreateEventRequest(
        @NotBlank(message = "Event name is required")
        String name,

        @NotBlank(message = "Event date is required")
        String date,

        @NotBlank(message = "Venue is required")
        String venue,

        @Min(value = 1, message = "Total capacity must be at least 1")
        @Max(value = 50000, message = "Total capacity cannot exceed 50000")
        int totalCapacity
) {}
