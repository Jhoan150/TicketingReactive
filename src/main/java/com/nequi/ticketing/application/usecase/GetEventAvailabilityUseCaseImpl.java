package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.exception.EventNotFoundException;
import com.nequi.ticketing.domain.port.in.GetEventAvailabilityUseCase;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Returns the current number of available tickets for a given event.
 * Reads directly from the event record, which is kept in sync by the reservation flow.
 */
@Service
public class GetEventAvailabilityUseCaseImpl implements GetEventAvailabilityUseCase {

    private final EventRepositoryPort eventRepository;

    public GetEventAvailabilityUseCaseImpl(EventRepositoryPort eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Mono<Integer> execute(String eventId) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new EventNotFoundException(eventId)))
                .map(event -> event.availableTickets());
    }
}
