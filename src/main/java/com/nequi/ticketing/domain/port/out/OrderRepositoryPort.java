package com.nequi.ticketing.domain.port.out;

import com.nequi.ticketing.domain.model.Order;
import reactor.core.publisher.Mono;

/**
 * Output port for Order persistence operations.
 * Implemented by the DynamoDB infrastructure adapter.
 */
public interface OrderRepositoryPort {

    /**
     * Finds an order by its unique identifier.
     *
     * @param orderId the order identifier
     * @return the order if found
     */
    Mono<Order> findById(String orderId);

    /**
     * Persists a new or updated order.
     *
     * @param order the order to save
     * @return the saved order
     */
    Mono<Order> save(Order order);
}
