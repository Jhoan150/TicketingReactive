package com.nequi.ticketing.domain.port.in;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Port for the Get Event Summary use case.
 * Provides a detailed count of tickets per status for a specific event.
 */
public interface GetEventSummaryUseCase {

    Mono<EventSummary> execute(String eventId);

    record EventSummary(
            String eventId,
            String name,
            LocalDateTime date,
            String venue,
            int totalCapacity,
            Map<String, Long> ticketCounts
    ) {}
}
