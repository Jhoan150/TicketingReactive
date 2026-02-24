package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.enums.TicketStatus;
import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReleaseExpiredReservations Tests")
class ReleaseExpiredReservationsUseCaseImplTest {

    @Mock private TicketRepositoryPort ticketRepository;
    @Mock private EventRepositoryPort eventRepository;
    @Mock private OrderRepositoryPort orderRepository;

    private ReleaseExpiredReservationsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReleaseExpiredReservationsUseCaseImpl(
                ticketRepository, eventRepository, orderRepository);
    }

    @Test
    @DisplayName("Should release expired reservations and return count")
    void shouldReleaseExpiredReservations() {
        // Given
        String eventId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();

        Ticket expiredTicket = new Ticket(UUID.randomUUID().toString(), eventId,
                TicketStatus.RESERVED, orderId,
                Instant.now().minusSeconds(700),  // reserved 11 min ago
                Instant.now().minusSeconds(100),  // expired 100s ago
                Instant.now().minusSeconds(700));

        Event event = new Event(eventId, "Concert", LocalDateTime.now(), "Venue",
                100, 49, 1L, Instant.now());

        when(ticketRepository.findExpiredReservations(any())).thenReturn(Flux.just(expiredTicket));
        when(ticketRepository.saveAll(any())).thenReturn(Flux.just(expiredTicket));
        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));
        when(eventRepository.updateWithOptimisticLock(any(), anyLong())).thenReturn(Mono.just(event));
        when(orderRepository.findById(orderId)).thenReturn(Mono.just(
                new com.nequi.ticketing.domain.model.Order(orderId, eventId, "user",
                        1, com.nequi.ticketing.domain.enums.OrderStatus.PENDING,
                        null, Instant.now(), Instant.now())));
        when(orderRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // When & Then
        StepVerifier.create(useCase.execute())
                .assertNext(count -> assertThat(count).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return 0 when no expired reservations found")
    void shouldReturnZeroWhenNoExpiredReservations() {
        when(ticketRepository.findExpiredReservations(any())).thenReturn(Flux.empty());

        StepVerifier.create(useCase.execute())
                .assertNext(count -> assertThat(count).isEqualTo(0))
                .verifyComplete();
    }
}
