package com.nequi.ticketing.domain.port.in;

import com.nequi.ticketing.domain.model.Order;
import reactor.core.publisher.Mono;

/**
 * Input port for processing a queued order asynchronously.
 * Invoked by the SQS consumer after dequeuing an order message.
 */
public interface ProcessOrderUseCase {

    /**
     * Processes a pending order: validates reserved tickets, marks them as SOLD,
     * and updates the order status to CONFIRMED.
     *
     * @param orderId the identifier of the order to process
     * @return the updated order
     */
    Mono<Order> execute(String orderId);
}
