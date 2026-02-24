package com.nequi.ticketing.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;

/**
 * HTTP response body for a created or fetched event.
 *
 * @param eventId          unique event identifier
 * @param name             display name
 * @param date             event date-time
 * @param venue            venue name
 * @param totalCapacity    total tickets
 * @param availableTickets current available count
 */
public record EventResponse(
        String eventId,
        String name,
        LocalDateTime date,
        String venue,
        int totalCapacity,
        int availableTickets
) {}
