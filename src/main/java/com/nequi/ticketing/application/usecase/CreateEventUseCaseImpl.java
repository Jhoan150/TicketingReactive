package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.enums.TicketStatus;
import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.port.in.CreateEventUseCase;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
import com.nequi.ticketing.domain.port.out.TicketRepositoryPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Creates a new event and generates its full inventory of available tickets.
 *
 */
@Service
public class CreateEventUseCaseImpl implements CreateEventUseCase {

    private final EventRepositoryPort eventRepository;
    private final TicketRepositoryPort ticketRepository;

    public CreateEventUseCaseImpl(EventRepositoryPort eventRepository,
                                  TicketRepositoryPort ticketRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public Mono<Event> execute(CreateEventCommand command) {
        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        LocalDateTime eventDate;
        try {
            eventDate = LocalDateTime.parse(command.date(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (java.time.format.DateTimeParseException e) {
            eventDate = java.time.ZonedDateTime.parse(command.date()).toLocalDateTime();
        }

        Event event = new Event(
                eventId,
                command.name(),
                eventDate,
                command.venue(),
                command.totalCapacity(),
                command.totalCapacity(),
                null,                    
                now
        );

        List<Ticket> tickets = IntStream.range(0, command.totalCapacity())
                .mapToObj(i -> new Ticket(
                        UUID.randomUUID().toString(),
                        eventId,
                        TicketStatus.AVAILABLE,
                        null,
                        null,
                        null,
                        now
                ))
                .toList();

        return eventRepository.save(event)
                .flatMap(savedEvent ->
                        ticketRepository.saveAll(tickets)
                                .then(Mono.just(savedEvent))
                );
    }
}
