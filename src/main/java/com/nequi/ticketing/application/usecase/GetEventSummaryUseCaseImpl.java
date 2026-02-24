package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.enums.TicketStatus;
import com.nequi.ticketing.domain.exception.EventNotFoundException;
import com.nequi.ticketing.domain.port.in.GetEventSummaryUseCase;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
import com.nequi.ticketing.domain.port.out.TicketRepositoryPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class GetEventSummaryUseCaseImpl implements GetEventSummaryUseCase {

    private final EventRepositoryPort eventRepository;
    private final TicketRepositoryPort ticketRepository;

    public GetEventSummaryUseCaseImpl(EventRepositoryPort eventRepository,
                                     TicketRepositoryPort ticketRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public Mono<EventSummary> execute(String eventId) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new EventNotFoundException(eventId)))
                .flatMap(event -> {
                    return Flux.fromArray(TicketStatus.values())
                            .flatMap(status -> ticketRepository.countByEventIdAndStatus(eventId, status)
                                    .map(count -> Map.entry(status.name(), count)))
                            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                            .map(counts -> new EventSummary(
                                    event.eventId(),
                                    event.name(),
                                    event.date(),
                                    event.venue(),
                                    event.totalCapacity(),
                                    counts
                            ));
                });
    }
}
