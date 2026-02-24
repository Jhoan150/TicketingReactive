package com.nequi.ticketing.infrastructure.adapter.in.web;

import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.port.in.CreateEventUseCase;
import com.nequi.ticketing.domain.port.in.GetAvailableEventsUseCase;
import com.nequi.ticketing.domain.port.in.GetEventAvailabilityUseCase;
import com.nequi.ticketing.infrastructure.adapter.in.web.dto.CreateEventRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

@WebFluxTest(EventController.class)
class EventControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CreateEventUseCase createEventUseCase;

    @Autowired
    private GetAvailableEventsUseCase getAvailableEventsUseCase;

    @Autowired
    private GetEventAvailabilityUseCase getEventAvailabilityUseCase;

    @Test
    void createEvent_shouldReturnCreatedEvent() {
        String eventId = UUID.randomUUID().toString();
        Event event = new Event(eventId, "Test Event", LocalDateTime.now(), "Venue", 100, 100, 1L, Instant.now());
        
        Mockito.when(createEventUseCase.execute(any())).thenReturn(Mono.just(event));

        CreateEventRequest request = new CreateEventRequest("Test Event", "2026-12-01T20:00:00", "Venue", 100);

        webTestClient.post()
                .uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.eventId").isEqualTo(eventId)
                .jsonPath("$.name").isEqualTo("Test Event");
    }

    @Test
    void getAllEvents_shouldReturnList() {
        Event event = new Event(UUID.randomUUID().toString(), "Event 1", LocalDateTime.now(), "Venue", 100, 100, 1L, Instant.now());
        
        Mockito.when(getAvailableEventsUseCase.execute()).thenReturn(Flux.just(event));

        webTestClient.get()
                .uri("/api/v1/events")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Event.class)
                .hasSize(1);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CreateEventUseCase createEventUseCase() {
            return Mockito.mock(CreateEventUseCase.class);
        }
        @Bean
        public GetAvailableEventsUseCase getAvailableEventsUseCase() {
            return Mockito.mock(GetAvailableEventsUseCase.class);
        }
        @Bean
        public GetEventAvailabilityUseCase getEventAvailabilityUseCase() {
            return Mockito.mock(GetEventAvailabilityUseCase.class);
        }
    }
}
