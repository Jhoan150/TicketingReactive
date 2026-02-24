package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAvailableEventsUseCaseImplTest {

    @Mock
    private EventRepositoryPort eventRepository;

    @InjectMocks
    private GetAvailableEventsUseCaseImpl getAvailableEventsUseCase;

    @Test
    void execute_ShouldReturnAvailableEvents() {
        // Arrange
        Event event1 = new Event("1", "Event 1", LocalDateTime.now(), "Venue 1", 100, 50, 0L, Instant.now());
        Event event2 = new Event("2", "Event 2", LocalDateTime.now(), "Venue 2", 100, 10, 0L, Instant.now());

        when(eventRepository.findAllWithAvailableTickets()).thenReturn(Flux.just(event1, event2));

        // Act & Assert
        StepVerifier.create(getAvailableEventsUseCase.execute())
                .expectNext(event1)
                .expectNext(event2)
                .verifyComplete();
    }
}
