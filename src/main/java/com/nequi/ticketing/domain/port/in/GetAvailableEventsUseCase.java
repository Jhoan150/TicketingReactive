package com.nequi.ticketing.domain.port.in;

import com.nequi.ticketing.domain.model.Event;
import reactor.core.publisher.Flux;

/**
 * Input port for retrieving all available events.
 */
public interface GetAvailableEventsUseCase {

    /**
     * Returns a reactive stream of all events that have available tickets.
     *
     * @return Flux of events with at least one available ticket
     */
    Flux<Event> execute();
}
