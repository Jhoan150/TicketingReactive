package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.enums.OrderStatus;
import com.nequi.ticketing.domain.enums.TicketStatus;
import com.nequi.ticketing.domain.exception.EventNotFoundException;
import com.nequi.ticketing.domain.exception.TicketNotAvailableException;
import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.port.in.ReserveTicketsUseCase;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
import com.nequi.ticketing.domain.port.out.OrderQueuePort;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReserveTicketsUseCase Tests")
class ReserveTicketsUseCaseImplTest {

    @Mock private EventRepositoryPort eventRepository;
    @Mock private TicketRepositoryPort ticketRepository;
    @Mock private OrderRepositoryPort orderRepository;
    @Mock private OrderQueuePort orderQueue;

    private ReserveTicketsUseCaseImpl useCase;

    private static final String EVENT_ID = UUID.randomUUID().toString();
    private static final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        useCase = new ReserveTicketsUseCaseImpl(eventRepository, ticketRepository,
                orderRepository, orderQueue);
    }

    @Test
    @DisplayName("Should successfully reserve tickets and return PENDING order")
    void shouldReserveTicketsSuccessfully() {
        // Given
        Event event = new Event(EVENT_ID, "Concert", LocalDateTime.now().plusDays(30),
                "Coliseo", 100, 50, 0L, Instant.now());

        Ticket ticket1 = new Ticket(UUID.randomUUID().toString(), EVENT_ID,
                TicketStatus.AVAILABLE, null, null, null, Instant.now());
        Ticket ticket2 = new Ticket(UUID.randomUUID().toString(), EVENT_ID,
                TicketStatus.AVAILABLE, null, null, null, Instant.now());

        Order savedOrder = new Order(UUID.randomUUID().toString(), EVENT_ID, USER_ID,
                2, OrderStatus.PENDING, null, Instant.now(), Instant.now());

        when(eventRepository.findById(EVENT_ID)).thenReturn(Mono.just(event));
        when(eventRepository.updateWithOptimisticLock(any(), eq(0L))).thenReturn(Mono.just(event));
        when(ticketRepository.findByEventIdAndStatus(EVENT_ID, TicketStatus.AVAILABLE, 2))
                .thenReturn(Flux.just(ticket1, ticket2));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.just(ticket1, ticket2));
        when(orderRepository.save(any())).thenReturn(Mono.just(savedOrder));
        when(orderQueue.enqueue(any())).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(useCase.execute(
                        new ReserveTicketsUseCase.ReserveTicketsCommand(EVENT_ID, USER_ID, 2)))
                .assertNext(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
                    assertThat(order.eventId()).isEqualTo(EVENT_ID);
                    assertThat(order.quantity()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw TicketNotAvailableException when not enough tickets")
    void shouldThrowWhenNotEnoughTickets() {
        // Given
        Event event = new Event(EVENT_ID, "Concert", LocalDateTime.now().plusDays(30),
                "Coliseo", 100, 1, 0L, Instant.now()); // only 1 available

        when(eventRepository.findById(EVENT_ID)).thenReturn(Mono.just(event));

        // When & Then
        StepVerifier.create(useCase.execute(
                        new ReserveTicketsUseCase.ReserveTicketsCommand(EVENT_ID, USER_ID, 5)))
                .expectError(TicketNotAvailableException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw EventNotFoundException when event does not exist")
    void shouldThrowWhenEventNotFound() {
        // Given
        when(eventRepository.findById(anyString())).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(useCase.execute(
                        new ReserveTicketsUseCase.ReserveTicketsCommand("invalid-id", USER_ID, 2)))
                .expectError(EventNotFoundException.class)
                .verify();
    }
}
