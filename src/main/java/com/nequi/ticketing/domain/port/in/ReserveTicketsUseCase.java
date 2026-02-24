package com.nequi.ticketing.domain.port.in;

import com.nequi.ticketing.domain.model.Order;
import reactor.core.publisher.Mono;

/**
 * Input port for initiating a ticket purchase, which temporarily reserves tickets
 * and enqueues an async order for processing.
 */
public interface ReserveTicketsUseCase {

    /**
     * Reserves tickets for the specified event and enqueues the purchase order.
     * The reservation expires after {@link com.nequi.ticketing.domain.model.Ticket#RESERVATION_TTL_MINUTES} minutes.
     *
     * @param command reservation details
     * @return the created order (in PENDING state)
     */
    Mono<Order> execute(ReserveTicketsCommand command);

    /**
     * Immutable command for reserving tickets.
     *
     * @param eventId  identifier of the target event
     * @param userId   identifier of the requesting user
     * @param quantity number of tickets to reserve (max 10 per request)
     */
    record ReserveTicketsCommand(
            String eventId,
            String userId,
            int quantity
    ) {}
}
