package com.nequi.ticketing.domain.model;

import com.nequi.ticketing.domain.enums.TicketStatus;

import java.time.Instant;

/**
 * Represents a single physical ticket associated with an event.
 *
 *
 * @param ticketId   unique identifier (UUID)
 * @param eventId    identifier of the associated event
 * @param status     current state of the ticket
 * @param orderId    optional identifier of the associated purchase order (null if AVAILABLE)
 * @param reservedAt timestamp when the ticket was reserved (null if not reserved)
 * @param expiresAt  timestamp when the reservation expires (null if not reserved)
 * @param createdAt  timestamp when the ticket was initially created
 */
public record Ticket(
        String ticketId,
        String eventId,
        TicketStatus status,
        String orderId,
        Instant reservedAt,
        Instant expiresAt,
        Instant createdAt
) {
    public static final int RESERVATION_TTL_MINUTES = 10;

    /**
     * Returns a new Ticket in RESERVED state with a 10-minute expiry.
     *
     * @param orderId   the order ID claiming this ticket
     * @param reservedAt the moment of reservation
     * @return updated ticket record
     */
    public Ticket reserve(String orderId, Instant reservedAt) {
        Instant expiry = reservedAt.plusSeconds(RESERVATION_TTL_MINUTES * 60L);
        return new Ticket(ticketId, eventId, TicketStatus.RESERVED,
                orderId, reservedAt, expiry, createdAt);
    }

    /**
     * Returns a new Ticket in PENDING_CONFIRMATION state.
     *
     * @return updated ticket record
     */
    public Ticket confirm() {
        return new Ticket(ticketId, eventId, TicketStatus.PENDING_CONFIRMATION,
                orderId, reservedAt, expiresAt, createdAt);
    }

    /**
     * Returns a new Ticket in SOLD state (final, irreversible).
     *
     * @return updated ticket record
     */
    public Ticket sell() {
        return new Ticket(ticketId, eventId, TicketStatus.SOLD,
                orderId, reservedAt, expiresAt, createdAt);
    }

    /**
     * Returns a new Ticket in COMPLIMENTARY state (final, not accountable).
     *
     * @return updated ticket record
     */
    public Ticket complimentary() {
        return new Ticket(ticketId, eventId, TicketStatus.COMPLIMENTARY,
                null, null, null, createdAt);
    }

    /**
     * Returns a new Ticket reverted to AVAILABLE state (reservation expired or cancelled).
     *
     * @return updated ticket record
     */
    public Ticket release() {
        return new Ticket(ticketId, eventId, TicketStatus.AVAILABLE,
                null, null, null, createdAt);
    }

    /**
     * Checks if this reservation has expired.
     *
     * @param now current time
     * @return true if ticket is RESERVED and the expiry time has passed
     */
    public boolean isExpired(Instant now) {
        return status == TicketStatus.RESERVED
                && expiresAt != null
                && now.isAfter(expiresAt);
    }
}
