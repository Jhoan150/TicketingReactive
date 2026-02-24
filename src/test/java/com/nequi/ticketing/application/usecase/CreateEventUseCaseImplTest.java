package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.port.in.CreateEventUseCase.CreateEventCommand;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
import com.nequi.ticketing.domain.port.out.TicketRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateEventUseCaseImplTest {

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private TicketRepositoryPort ticketRepository;

    @InjectMocks
    private CreateEventUseCaseImpl createEventUseCase;

    private CreateEventCommand command;

    @BeforeEach
    void setUp() {
        command = new CreateEventCommand(
                "Test Event",
                "2026-06-01T20:00:00",
                "Test Venue",
                5
        );
    }

    @Test
    void execute_ShouldCreateEventAndGenerateTickets() {
        // Arrange
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(createEventUseCase.execute(command))
                .assertNext(event -> {
                    assertNotNull(event.eventId());
                    assertEquals(command.name(), event.name());
                    assertEquals(command.venue(), event.venue());
                    assertEquals(command.totalCapacity(), event.totalCapacity());
                    assertEquals(command.totalCapacity(), event.availableTickets());
                    assertNotNull(event.createdAt());
                })
                .verifyComplete();

        // Verify eventRepository.save was called
        verify(eventRepository, times(1)).save(any(Event.class));

        // Verify ticketRepository.saveAll was called with the correct number of tickets
        ArgumentCaptor<List> ticketsCaptor = ArgumentCaptor.forClass(List.class);
        verify(ticketRepository, times(1)).saveAll(ticketsCaptor.capture());
        assertEquals(command.totalCapacity(), ticketsCaptor.getValue().size());
    }

    @Test
    void execute_ShouldHandleZonedDateTimeFormat() {
        // Arrange
        CreateEventCommand zCommand = new CreateEventCommand(
                "Z Event",
                "2026-06-01T20:00:00Z",
                "Z Venue",
                10
        );
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(ticketRepository.saveAll(anyList())).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(createEventUseCase.execute(zCommand))
                .assertNext(event -> {
                    assertEquals("Z Event", event.name());
                    assertNotNull(event.date());
                })
                .verifyComplete();
    }

    @Test
    void execute_WhenRepositoryFails_ShouldPropagateError() {
        // Arrange
        when(eventRepository.save(any(Event.class))).thenReturn(Mono.error(new RuntimeException("DB Error")));

        // Act & Assert
        StepVerifier.create(createEventUseCase.execute(command))
                .expectError(RuntimeException.class)
                .verify();

        verify(ticketRepository, never()).saveAll(anyList());
    }
}
