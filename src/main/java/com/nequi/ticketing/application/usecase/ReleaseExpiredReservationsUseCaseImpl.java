package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.port.in.ReleaseExpiredReservationsUseCase;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
import com.nequi.ticketing.domain.port.out.OrderRepositoryPort;
import com.nequi.ticketing.domain.port.out.TicketRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scheduled job that releases expired ticket reservations back to the inventory.
 *
 * Runs every 60 seconds to identify RESERVED tickets whose TTL has elapsed.
 * For each expired ticket: reverts to AVAILABLE, updates event inventory, and cancels the order.
 */
@Service
public class ReleaseExpiredReservationsUseCaseImpl implements ReleaseExpiredReservationsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReleaseExpiredReservationsUseCaseImpl.class);

    private final TicketRepositoryPort ticketRepository;
    private final EventRepositoryPort eventRepository;
    private final OrderRepositoryPort orderRepository;

    public ReleaseExpiredReservationsUseCaseImpl(TicketRepositoryPort ticketRepository,
                                                 EventRepositoryPort eventRepository,
                                                 OrderRepositoryPort orderRepository) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedDelayString = "${app.reservation.release-job-delay-ms:60000}")
    public void scheduleRelease() {
        execute()
                .doOnNext(count -> log.info("Released {} expired reservation(s)", count))
                .doOnError(error -> log.error("Error releasing expired reservations", error))
                .subscribe();
    }

    @Override
    public Mono<Integer> execute() {
        Instant now = Instant.now();

        return ticketRepository.findExpiredReservations(now)
                .collectList()
                .flatMap(expiredTickets -> {
                    if (expiredTickets.isEmpty()) {
                        return Mono.just(0);
                    }

                    log.info("Found {} expired ticket reservations to release", expiredTickets.size());

                    // Group by event to update inventory per event
                    Map<String, List<Ticket>> byEvent = expiredTickets.stream()
                            .collect(Collectors.groupingBy(Ticket::eventId));

                    // Release all tickets
                    List<Ticket> releasedTickets = expiredTickets.stream()
                            .map(Ticket::release)
                            .toList();

                    // Collect unique order IDs for cancellation
                    List<String> orderIds = expiredTickets.stream()
                            .map(Ticket::orderId)
                            .filter(id -> id != null)
                            .distinct()
                            .toList();

                    return ticketRepository.saveAll(releasedTickets)
                            .then(restoreEventInventory(byEvent))
                            .then(cancelOrders(orderIds))
                            .thenReturn(expiredTickets.size());
                });
    }

    private Mono<Void> restoreEventInventory(Map<String, List<Ticket>> ticketsByEvent) {
        return Mono.when(
                ticketsByEvent.entrySet().stream()
                        .map(entry -> eventRepository.findById(entry.getKey())
                                .flatMap(event -> {
                                    int count = entry.getValue().size();
                                    long currentVersion = event.version();
                                    var restored = event.withReleasedTickets(count);
                                    return eventRepository.updateWithOptimisticLock(restored, currentVersion);
                                }))
                        .toList()
        );
    }

    private Mono<Void> cancelOrders(List<String> orderIds) {
        return Mono.when(
                orderIds.stream()
                        .map(orderId -> orderRepository.findById(orderId)
                                .flatMap(order -> orderRepository.save(
                                        order.cancelled("Reservation expired after 10 minutes")))
                        )
                        .toList()
        );
    }
}
