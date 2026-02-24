package com.nequi.ticketing.domain.exception;

/**
 * Thrown when a requested event does not exist in the system.
 */
public class EventNotFoundException extends DomainException {

    private static final String ERROR_CODE = "EVENT_NOT_FOUND";

    public EventNotFoundException(String eventId) {
        super(ERROR_CODE, String.format("Event not found with id: [%s]", eventId));
    }
}
