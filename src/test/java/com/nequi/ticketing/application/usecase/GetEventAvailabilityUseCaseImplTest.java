package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.exception.EventNotFoundException;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetEventAvailabilityUseCaseImplTest {

    @Mock
    private EventRepositoryPort eventRepository;

    @InjectMocks
    private GetEventAvailabilityUseCaseImpl getEventAvailabilityUseCase;

    @Test
    void execute_WhenEventExists_ShouldReturnAvailableTickets() {
        // Arrange
        String eventId = "event-123";
        Event event = new Event(
                eventId, 
                "Test Event", 
                LocalDateTime.now(), 
                "Venue", 
                100, 
                42, // Expected availability
                0L, 
                Instant.now()
        );

        when(eventRepository.findById(eventId)).thenReturn(Mono.just(event));

        // Act & Assert
        StepVerifier.create(getEventAvailabilityUseCase.execute(eventId))
                .expectNext(42)
                .verifyComplete();
    }

    @Test
    void execute_WhenEventDoesNotExist_ShouldThrowException() {
        // Arrange
        String eventId = "none";
        when(eventRepository.findById(eventId)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(getEventAvailabilityUseCase.execute(eventId))
                .expectError(EventNotFoundException.class)
                .verify();
    }
}
