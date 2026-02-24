package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.enums.OrderStatus;
import com.nequi.ticketing.domain.enums.TicketStatus;
import com.nequi.ticketing.domain.exception.OrderNotFoundException;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.port.out.OrderRepositoryPort;
import com.nequi.ticketing.domain.port.out.TicketRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessOrderUseCase Tests")
class ProcessOrderUseCaseImplTest {

    @Mock private OrderRepositoryPort orderRepository;
    @Mock private TicketRepositoryPort ticketRepository;

    private ProcessOrderUseCaseImpl useCase;

    private static final String ORDER_ID = UUID.randomUUID().toString();
    private static final String EVENT_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        useCase = new ProcessOrderUseCaseImpl(orderRepository, ticketRepository);
    }

    @Test
    @DisplayName("Should process PENDING order to CONFIRMED state")
    void shouldProcessOrderSuccessfully() {
        // Given
        Order pendingOrder = new Order(ORDER_ID, EVENT_ID, "user-1", 2,
                OrderStatus.PENDING, null, Instant.now(), Instant.now());

        Ticket reservedTicket = new Ticket(UUID.randomUUID().toString(), EVENT_ID,
                TicketStatus.RESERVED, ORDER_ID, Instant.now(), Instant.now().plusSeconds(600),
                Instant.now());

        when(orderRepository.findById(ORDER_ID)).thenReturn(Mono.just(pendingOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(ticketRepository.findByOrderId(ORDER_ID)).thenReturn(Flux.just(reservedTicket));
        when(ticketRepository.saveAll(any())).thenReturn(Flux.just(reservedTicket));

        // When & Then
        StepVerifier.create(useCase.execute(ORDER_ID))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should be idempotent: return CONFIRMED order if already processed")
    void shouldBeIdempotentForConfirmedOrder() {
        // Given
        Order confirmedOrder = new Order(ORDER_ID, EVENT_ID, "user-1", 2,
                OrderStatus.CONFIRMED, null, Instant.now(), Instant.now());

        when(orderRepository.findById(ORDER_ID)).thenReturn(Mono.just(confirmedOrder));

        // When & Then
        StepVerifier.create(useCase.execute(ORDER_ID))
                .assertNext(order -> assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException for unknown orderId")
    void shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("unknown-id"))
                .expectError(OrderNotFoundException.class)
                .verify();
    }
}
