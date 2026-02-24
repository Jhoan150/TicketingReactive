package com.nequi.ticketing.infrastructure.adapter.in.web;

import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.port.in.CreateEventUseCase;
import com.nequi.ticketing.domain.port.in.GetAvailableEventsUseCase;
import com.nequi.ticketing.domain.port.in.GetEventAvailabilityUseCase;
import com.nequi.ticketing.infrastructure.adapter.in.web.dto.CreateEventRequest;
import com.nequi.ticketing.infrastructure.adapter.in.web.dto.EventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WebFlux REST controller for Event endpoints.
 *
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final CreateEventUseCase createEventUseCase;
    private final GetAvailableEventsUseCase getAvailableEventsUseCase;
    private final GetEventAvailabilityUseCase getEventAvailabilityUseCase;

    public EventController(CreateEventUseCase createEventUseCase,
                           GetAvailableEventsUseCase getAvailableEventsUseCase,
                           GetEventAvailabilityUseCase getEventAvailabilityUseCase) {
        this.createEventUseCase = createEventUseCase;
        this.getAvailableEventsUseCase = getAvailableEventsUseCase;
        this.getEventAvailabilityUseCase = getEventAvailabilityUseCase;
    }

    /**
     * Creates a new event with the specified capacity.
     *
     * @param request event creation details
     * @return 201 Created with the created event
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        var command = new CreateEventUseCase.CreateEventCommand(
                request.name(),
                request.date(),
                request.venue(),
                request.totalCapacity()
        );
        return createEventUseCase.execute(command).map(this::toResponse);
    }

    /**
     * Returns all events with available tickets.
     *
     * @return stream of events
     */
    @GetMapping
    public Flux<EventResponse> getAvailableEvents() {
        return getAvailableEventsUseCase.execute().map(this::toResponse);
    }

    /**
     * Returns the current number of available tickets for an event.
     *
     * @param eventId the event identifier
     * @return available ticket count
     */
    @GetMapping("/{eventId}/availability")
    public Mono<AvailabilityResponse> getEventAvailability(@PathVariable String eventId) {
        return getEventAvailabilityUseCase.execute(eventId)
                .map(count -> new AvailabilityResponse(eventId, count));
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.eventId(),
                event.name(),
                event.date(),
                event.venue(),
                event.totalCapacity(),
                event.availableTickets()
        );
    }

    record AvailabilityResponse(String eventId, int availableTickets) {}
}
