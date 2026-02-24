package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.enums.OrderStatus;
import com.nequi.ticketing.domain.exception.OrderNotFoundException;
import com.nequi.ticketing.domain.port.out.OrderRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderStatusUseCaseImplTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @InjectMocks
    private GetOrderStatusUseCaseImpl getOrderStatusUseCase;

    @Test
    void execute_WhenOrderExists_ShouldReturnOrder() {
        // Arrange
        String orderId = "order-123";
        Order order = new Order(orderId, "event-1", "user-1", 1, OrderStatus.PENDING, null, Instant.now(), Instant.now());

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        // Act & Assert
        StepVerifier.create(getOrderStatusUseCase.execute(orderId))
                .expectNext(order)
                .verifyComplete();
    }

    @Test
    void execute_WhenOrderDoesNotExist_ShouldThrowException() {
        // Arrange
        String orderId = "none";
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(getOrderStatusUseCase.execute(orderId))
                .expectError(OrderNotFoundException.class)
                .verify();
    }
}
