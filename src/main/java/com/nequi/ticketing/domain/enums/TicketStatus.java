package com.nequi.ticketing.domain.enums;

/**
 * Represents all possible states of a ticket in the ticketing system.
 */
public enum TicketStatus {

    /** Ticket is available for purchase. */
    AVAILABLE,

    /** Ticket is temporarily reserved (up to 10 minutes). Not a sale. */
    RESERVED,

    /** Ticket is awaiting payment confirmation. Not a sale. */
    PENDING_CONFIRMATION,

    /** Ticket has been sold. Final and irreversible. */
    SOLD,

    /** Ticket was given as a complimentary. Final but not accountable as sale. */
    COMPLIMENTARY
}
