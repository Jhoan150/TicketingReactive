package com.nequi.ticketing.domain.port.out;

import com.nequi.ticketing.domain.model.Event;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for Event persistence operations.
 * Implemented by the DynamoDB infrastructure adapter.
 */
public interface EventRepositoryPort {

    /**
     * Finds an event by its unique identifier.
     *
     * @param eventId the event identifier
     * @return the event if found, empty otherwise
     */
    Mono<Event> findById(String eventId);

    /**
     * Persists a new or updated event.
     *
     * @param event the event to save
     * @return the saved event
     */
    Mono<Event> save(Event event);

    /**
     * Returns all events in the system.
     *
     * @return stream of all events
     */
    Flux<Event> findAll();

    /**
     * Returns all events that have at least one available ticket.
     *
     * @return stream of events with availableTickets > 0
     */
    Flux<Event> findAllWithAvailableTickets();

    /**
     * Atomically updates the available ticket count and version of an event using
     * conditional writes (optimistic locking). Fails if the stored version differs.
     *
     * @param event        the event with the new values
     * @param expectedVersion the version expected to be currently stored
     * @return the updated event, or empty if the condition failed
     */
    Mono<Event> updateWithOptimisticLock(Event event, Long expectedVersion);
}
