package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.enums.OrderStatus;
import com.nequi.ticketing.domain.exception.OrderNotFoundException;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.port.in.ProcessOrderUseCase;
import com.nequi.ticketing.domain.port.out.OrderRepositoryPort;
import com.nequi.ticketing.domain.port.out.TicketRepositoryPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Processes a queued purchase order by transitioning reserved tickets to SOLD state.
 *
 * This use case is invoked by the SQS consumer after dequeuing an order message.
 * It implements at-least-once delivery semantics: if the consumer crashes between
 * processing and ACKing, the message is redelivered. Idempotency is achieved by
 * checking the current order status before processing.
 */
@Service
public class ProcessOrderUseCaseImpl implements ProcessOrderUseCase {

    private final OrderRepositoryPort orderRepository;
    private final TicketRepositoryPort ticketRepository;

    public ProcessOrderUseCaseImpl(OrderRepositoryPort orderRepository,
                                   TicketRepositoryPort ticketRepository) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public Mono<Order> execute(String orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .flatMap(order -> switch (order.status()) {
                    case CONFIRMED -> Mono.just(order);
                    case CANCELLED, FAILED -> Mono.just(order);
                    case PENDING, PROCESSING -> processOrder(order);
                });
    }

    private Mono<Order> processOrder(Order order) {
        Order processingOrder = order.withStatus(OrderStatus.PROCESSING);

        return orderRepository.save(processingOrder)
                .flatMap(saved ->
                        ticketRepository.findByOrderId(order.orderId())
                                .map(ticket -> ticket.sell())
                                .collectList()
                                .flatMap(soldTickets ->
                                        ticketRepository.saveAll(soldTickets).then())
                )
                .then(Mono.defer(() -> {
                    Order confirmedOrder = processingOrder.withStatus(OrderStatus.CONFIRMED);
                    return orderRepository.save(confirmedOrder);
                }))
                .onErrorResume(error -> {
                    Order failedOrder = order.failed(error.getMessage());
                    return orderRepository.save(failedOrder);
                });
    }
}
