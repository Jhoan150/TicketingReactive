package com.nequi.ticketing.domain.exception;

/**
 * Thrown when a concurrent modification conflict is detected (optimistic locking failure).
 */
public class OptimisticLockException extends DomainException {

    private static final String ERROR_CODE = "OPTIMISTIC_LOCK_CONFLICT";

    public OptimisticLockException(String resourceType, String resourceId) {
        super(ERROR_CODE, String.format(
                "Concurrent modification conflict on [%s] with id: [%s]. Please retry.",
                resourceType, resourceId));
    }
}
