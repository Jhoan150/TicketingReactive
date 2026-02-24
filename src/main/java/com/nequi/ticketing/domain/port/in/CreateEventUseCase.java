package com.nequi.ticketing.domain.port.in;

import com.nequi.ticketing.domain.model.Event;
import reactor.core.publisher.Mono;

/**
 * Input port for creating a new event with its initial ticket inventory.
 */
public interface CreateEventUseCase {

    /**
     * Creates a new event with the specified capacity and generates tickets in AVAILABLE state.
     *
     * @param command the event creation details
     * @return the created event
     */
    Mono<Event> execute(CreateEventCommand command);

    /**
     *
     * @param name          
     * @param date         
     * @param venue         
     * @param totalCapacity
     */
    record CreateEventCommand(
            String name,
            String date,
            String venue,
            int totalCapacity
    ) {}
}
