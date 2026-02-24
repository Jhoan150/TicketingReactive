package com.nequi.ticketing.application.usecase;

import com.nequi.ticketing.domain.enums.OrderStatus;
import com.nequi.ticketing.domain.exception.EventNotFoundException;
import com.nequi.ticketing.domain.exception.OptimisticLockException;
import com.nequi.ticketing.domain.exception.TicketNotAvailableException;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.port.in.ReserveTicketsUseCase;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
import com.nequi.ticketing.domain.port.out.OrderQueuePort;
import com.nequi.ticketing.domain.port.out.OrderRepositoryPort;
import com.nequi.ticketing.domain.port.out.TicketRepositoryPort;
import com.nequi.ticketing.domain.enums.TicketStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReserveTicketsUseCaseImpl implements ReserveTicketsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReserveTicketsUseCaseImpl.class);

    private static final int MAX_RETRY_ATTEMPTS = 15;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(200);

    private final EventRepositoryPort eventRepository;
    private final TicketRepositoryPort ticketRepository;
    private final OrderRepositoryPort orderRepository;
    private final OrderQueuePort orderQueue;

    public ReserveTicketsUseCaseImpl(EventRepositoryPort eventRepository,
                                     TicketRepositoryPort ticketRepository,
                                     OrderRepositoryPort orderRepository,
                                     OrderQueuePort orderQueue) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
        this.orderQueue = orderQueue;
    }

    @Override
    public Mono<Order> execute(ReserveTicketsCommand command) {
        log.info("Attempting to reserve {} tickets for event ID: {} by user ID: {}",
                command.quantity(), command.eventId(), command.userId());
        
        return attemptReservation(command)
                .retryWhen(Retry.fixedDelay(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                        .jitter(0.5)
                        .filter(ex -> {
                            boolean isOptimisticLock = ex instanceof OptimisticLockException;
                            if (isOptimisticLock) {
                                log.warn("Optimistic lock conflict detected for event {}. Retrying reservation.", command.eventId());
                            }
                            return isOptimisticLock;
                        })
                        .onRetryExhaustedThrow((retrySpec, retrySignal) -> {
                            log.error("Max retry attempts exhausted for event {} after optimistic lock conflicts. Last error: {}",
                                    command.eventId(), retrySignal.failure().getMessage());
                            return retrySignal.failure();
                        }));
    }

    private Mono<Order> attemptReservation(ReserveTicketsCommand command) {
        return eventRepository.findById(command.eventId())
                .switchIfEmpty(Mono.error(new EventNotFoundException(command.eventId())))
                .flatMap(event -> {
                    if (!event.hasAvailableTickets(command.quantity())) {
                        log.warn("Not enough stock for event {}. Requested: {}, Available: {}", 
                                command.eventId(), command.quantity(), event.availableTickets());
                        return Mono.error(new TicketNotAvailableException(
                                command.eventId(), command.quantity(), event.availableTickets()));
                    }

                    long currentVersion = event.version();
                    var updatedEvent = event.withReservedTickets(command.quantity());

                    return eventRepository.updateWithOptimisticLock(updatedEvent, currentVersion)
                            .switchIfEmpty(Mono.error(
                                    new OptimisticLockException("Event", command.eventId())))
                            .flatMap(savedEvent -> {
                                log.debug("Successfully updated event {} with new reserved ticket count. Version: {}", savedEvent.eventId(), savedEvent.version());
                                return reserveTicketsAndCreateOrder(command, savedEvent.eventId());
                            });
                });
    }


    private Mono<Order> reserveTicketsAndCreateOrder(ReserveTicketsCommand command, String eventId) {
        Instant now = Instant.now();
        String orderId = UUID.randomUUID().toString();
        log.debug("Starting ticket reservation and order creation for order ID: {}", orderId);

        return ticketRepository
                .findByEventIdAndStatus(eventId, TicketStatus.AVAILABLE, command.quantity())
                .collectList()
                .flatMap(tickets -> {
                    if (tickets.size() < command.quantity()) {
                        log.error("Found fewer available tickets ({}) than requested ({}) for event {}. This indicates a potential data inconsistency.",
                                tickets.size(), command.quantity(), eventId);
                        return Mono.error(new TicketNotAvailableException(eventId, command.quantity(), tickets.size()));
                    }
                    var reservedTickets = tickets.stream()
                            .map(t -> t.reserve(orderId, now))
                            .toList();

                    return ticketRepository.saveAll(reservedTickets).then();
                })
                .then(Mono.defer(() -> {
                    Order order = new Order(
                            orderId,
                            eventId,
                            command.userId(),
                            command.quantity(),
                            OrderStatus.PENDING,
                            null,
                            now,
                            now
                    );
                    return orderRepository.save(order)
                            .flatMap(savedOrder ->
                                    orderQueue.enqueue(savedOrder)
                                            .thenReturn(savedOrder));
                }));
    }
}
