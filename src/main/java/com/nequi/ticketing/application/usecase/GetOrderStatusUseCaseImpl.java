package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.exception.OrderNotFoundException;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.port.in.GetOrderStatusUseCase;
import com.nequi.ticketing.domain.port.out.OrderRepositoryPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Retrieves the current state of a purchase order by its identifier.
 */
@Service
public class GetOrderStatusUseCaseImpl implements GetOrderStatusUseCase {

    private final OrderRepositoryPort orderRepository;

    public GetOrderStatusUseCaseImpl(OrderRepositoryPort orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Mono<Order> execute(String orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)));
    }
}
