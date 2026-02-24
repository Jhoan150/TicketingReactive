package com.nequi.ticketing.domain.port.in;

import reactor.core.publisher.Mono;

/**
 * Input port for querying real-time ticket availability for a specific event.
 */
public interface GetEventAvailabilityUseCase {

    /**
     * Returns the current number of available tickets for a given event.
     * This reflects live inventory considering AVAILABLE, RESERVED, and SOLD tickets.
     *
     * @param eventId the event to query
     * @return count of currently available tickets
     */
    Mono<Integer> execute(String eventId);
}
