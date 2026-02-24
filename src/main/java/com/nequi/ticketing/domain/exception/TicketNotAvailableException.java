package com.nequi.ticketing.domain.exception;

/**
 * Thrown when there are not enough available tickets for a requested event.
 */
public class TicketNotAvailableException extends DomainException {

    private static final String ERROR_CODE = "TICKET_NOT_AVAILABLE";

    public TicketNotAvailableException(String eventId, int requested, int available) {
        super(ERROR_CODE, String.format(
                "Not enough tickets available for event [%s]. Requested: %d, Available: %d",
                eventId, requested, available));
    }
}
