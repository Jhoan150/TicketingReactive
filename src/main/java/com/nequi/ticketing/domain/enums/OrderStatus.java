package com.nequi.ticketing.domain.enums;

/**
 * Represents the lifecycle states of a purchase order.
 */
public enum OrderStatus {

    /** Order has been received and is queued for processing. */
    PENDING,

    /** Order is being processed by the async consumer. */
    PROCESSING,

    /** Order has been confirmed and tickets are sold. */
    CONFIRMED,

    /** Order was cancelled. */
    CANCELLED,

    /** Order failed due to an unexpected error during processing. */
    FAILED
}
