package com.nequi.ticketing.domain.port.out;

import com.nequi.ticketing.domain.model.Order;
import reactor.core.publisher.Mono;

/**
 * Output port for asynchronous order queue operations.
 * Implemented by the SQS infrastructure adapter.
 */
public interface OrderQueuePort {

    /**
     * Enqueues an order for asynchronous processing.
     *
     * @param order the order to enqueue
     * @return empty Mono on success
     */
    Mono<Void> enqueue(Order order);
}
