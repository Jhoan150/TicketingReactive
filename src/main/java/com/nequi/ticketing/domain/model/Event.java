package com.nequi.ticketing.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Represents an event (concert, theater show, sports game) with its inventory metadata.
 *
 * The {@code version} field is used for optimistic locking in DynamoDB conditional writes,
 * ensuring atomic inventory updates under high concurrency.
 *
 * @param eventId          unique identifier (UUID)
 * @param name             display name of the event
 * @param date             scheduled date and time of the event
 * @param venue            location/venue name
 * @param totalCapacity    total number of tickets for this event
 * @param availableTickets current count of tickets in AVAILABLE state
 * @param version          optimistic locking version, incremented on every update
 * @param createdAt        timestamp when the event was created
 */
public record Event(
        String eventId,
        String name,
        LocalDateTime date,
        String venue,
        int totalCapacity,
        int availableTickets,
        Long version,
        Instant createdAt
) {
    /**
     * Returns a new Event with decremented available ticket count and incremented version.
     *
     * @param quantity number of tickets to reserve
     * @return updated event record
     */
    public Event withReservedTickets(int quantity) {
        Long nextVersion = (version == null) ? 1L : version + 1;
        return new Event(eventId, name, date, venue, totalCapacity,
                availableTickets - quantity, nextVersion, createdAt);
    }

    /**
     * Returns a new Event with incremented available ticket count and incremented version.
     *
     * @param quantity number of tickets to release back to inventory
     * @return updated event record
     */
    public Event withReleasedTickets(int quantity) {
        Long nextVersion = (version == null) ? 1L : version + 1;
        return new Event(eventId, name, date, venue, totalCapacity,
                availableTickets + quantity, nextVersion, createdAt);
    }

    /**
     * Checks if there are enough tickets available for the requested quantity.
     *
     * @param quantity requested quantity
     * @return true if enough tickets are available
     */
    public boolean hasAvailableTickets(int quantity) {
        return availableTickets >= quantity;
    }
}
