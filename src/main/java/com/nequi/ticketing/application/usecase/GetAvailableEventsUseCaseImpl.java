package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.port.in.GetAvailableEventsUseCase;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Returns all events that still have tickets available for purchase.
 */
@Service
public class GetAvailableEventsUseCaseImpl implements GetAvailableEventsUseCase {

    private final EventRepositoryPort eventRepository;

    public GetAvailableEventsUseCaseImpl(EventRepositoryPort eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Flux<Event> execute() {
        return eventRepository.findAllWithAvailableTickets();
    }
}
