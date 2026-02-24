package com.nequi.ticketing.domain.port.in;

import com.nequi.ticketing.domain.model.Order;
import reactor.core.publisher.Mono;

/**
 * Input port for querying the current state of a purchase order.
 */
public interface GetOrderStatusUseCase {

    /**
     * Retrieves the current state of an order by its identifier.
     *
     * @param orderId the unique identifier of the order
     * @return the order if found
     */
    Mono<Order> execute(String orderId);
}
