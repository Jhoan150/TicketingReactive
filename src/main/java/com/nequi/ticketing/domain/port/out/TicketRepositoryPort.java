package com.nequi.ticketing.domain.port.out;

import com.nequi.ticketing.domain.enums.TicketStatus;
import com.nequi.ticketing.domain.model.Ticket;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Output port for Ticket persistence operations.
 * Implemented by the DynamoDB infrastructure adapter.
 */
public interface TicketRepositoryPort {

    /**
     * Finds a ticket by its unique identifier.
     *
     * @param ticketId the ticket identifier
     * @return the ticket if found
     */
    Mono<Ticket> findById(String ticketId);

    /**
     * Returns all tickets for a given event with the specified status.
     *
     * @param eventId the event identifier
     * @param status  the desired ticket status
     * @param limit   maximum number of tickets to return
     * @return stream of matching tickets
     */
    Flux<Ticket> findByEventIdAndStatus(String eventId, TicketStatus status, int limit);

    /**
     * Returns all tickets associated with a specific order.
     *
     * @param orderId the order identifier
     * @return stream of tickets for this order
     */
    Flux<Ticket> findByOrderId(String orderId);

    /**
     * Returns all RESERVED tickets whose expiry time is before the given instant.
     *
     * @param expiryThreshold the cutoff time
     * @return stream of expired reserved tickets
     */
    Flux<Ticket> findExpiredReservations(Instant expiryThreshold);

    /**
     * Saves a single ticket.
     *
     * @param ticket the ticket to persist
     * @return the saved ticket
     */
    Mono<Ticket> save(Ticket ticket);

    /**
     * Saves a batch of tickets atomically.
     *
     * @param tickets the list of tickets to persist
     * @return stream of saved tickets
     */
    Flux<Ticket> saveAll(List<Ticket> tickets);

    /**
     * Counts tickets for a given event and status.
     *
     * @param eventId the event identifier
     * @param status  the ticket status
     * @return the count of matching tickets
     */
    Mono<Long> countByEventIdAndStatus(String eventId, TicketStatus status);
}
