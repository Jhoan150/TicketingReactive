package com.nequi.ticketing.domain.exception;

/**
 * Thrown when a requested order does not exist in the system.
 */
public class OrderNotFoundException extends DomainException {

    private static final String ERROR_CODE = "ORDER_NOT_FOUND";

    public OrderNotFoundException(String orderId) {
        super(ERROR_CODE, String.format("Order not found with id: [%s]", orderId));
    }
}
