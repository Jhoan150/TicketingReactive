package com.nequi.ticketing.infrastructure.adapter.out.dynamodb;

import com.nequi.ticketing.domain.enums.TicketStatus;
import com.nequi.ticketing.domain.model.Ticket;
import com.nequi.ticketing.domain.port.out.TicketRepositoryPort;
import com.nequi.ticketing.infrastructure.adapter.out.dynamodb.entity.TicketEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.List;

/**
 * DynamoDB adapter implementing {@link TicketRepositoryPort}.
 * Uses GSI "eventId-status-index" for status-based ticket queries.
 */
@Component
public class TicketDynamoDbAdapter implements TicketRepositoryPort {

    private static final String TABLE_NAME = "tickets";
    private static final String EVENT_STATUS_INDEX = "eventId-status-index";
    private static final String ORDER_INDEX = "orderId-index";

    private final DynamoDbAsyncTable<TicketEntity> table;
    public TicketDynamoDbAdapter(DynamoDbEnhancedAsyncClient enhancedClient) {
        this.table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(TicketEntity.class));
    }

    @Override
    public Mono<Ticket> findById(String ticketId) {
        return Mono.fromFuture(table.getItem(r -> r.key(k -> k.partitionValue(ticketId))))
                .map(this::toDomain);
    }

    @Override
    public Flux<Ticket> findByEventIdAndStatus(String eventId, TicketStatus status, int limit) {
        DynamoDbAsyncIndex<TicketEntity> index = table.index(EVENT_STATUS_INDEX);

        Key key = Key.builder()
                .partitionValue(eventId)
                .sortValue(status.name())
                .build();

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(key))
                .limit(limit)
                .build();

        return Flux.from(index.query(request).flatMapIterable(page -> page.items()))
                .map(this::toDomain);
    }

    @Override
    public Flux<Ticket> findByOrderId(String orderId) {
        DynamoDbAsyncIndex<TicketEntity> index = table.index(ORDER_INDEX);

        Key key = Key.builder().partitionValue(orderId).build();
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(key))
                .build();

        return Flux.from(index.query(request).flatMapIterable(page -> page.items()))
                .map(this::toDomain);
    }

    @Override
    public Flux<Ticket> findExpiredReservations(Instant expiryThreshold) {
        var filterExpression = Expression.builder()
                .expression("#s = :reserved AND expiresAt <= :threshold")
                .expressionNames(java.util.Map.of("#s", "status"))
                .expressionValues(java.util.Map.of(
                        ":reserved", AttributeValue.fromS(TicketStatus.RESERVED.name()),
                        ":threshold", AttributeValue.fromS(expiryThreshold.toString())
                ))
                .build();

        return Flux.from(table.scan(r -> r.filterExpression(filterExpression)).items())
                .map(this::toDomain);
    }

    @Override
    public Mono<Ticket> save(Ticket ticket) {
        TicketEntity entity = toEntity(ticket);
        return Mono.fromFuture(table.putItem(entity)).thenReturn(ticket);
    }

    @Override
    public Flux<Ticket> saveAll(List<Ticket> tickets) {
        return Flux.fromIterable(tickets)
                .flatMap(this::save);
    }

    @Override
    public Mono<Long> countByEventIdAndStatus(String eventId, TicketStatus status) {
        DynamoDbAsyncIndex<TicketEntity> index = table.index(EVENT_STATUS_INDEX);

        Key key = Key.builder()
                .partitionValue(eventId)
                .sortValue(status.name())
                .build();

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(key))
                .build();

        return Flux.from(index.query(request))
                .map(page -> (long) page.items().size())
                .reduce(0L, Long::sum);
    }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private Ticket toDomain(TicketEntity e) {
        return new Ticket(
                e.getTicketId(),
                e.getEventId(),
                TicketStatus.valueOf(e.getStatus()),
                e.getOrderId(),
                e.getReservedAt() != null ? Instant.parse(e.getReservedAt()) : null,
                e.getExpiresAt() != null ? Instant.parse(e.getExpiresAt()) : null,
                Instant.parse(e.getCreatedAt())
        );
    }

    private TicketEntity toEntity(Ticket t) {
        TicketEntity entity = new TicketEntity();
        entity.setTicketId(t.ticketId());
        entity.setEventId(t.eventId());
        entity.setStatus(t.status().name());
        entity.setOrderId(t.orderId());
        entity.setReservedAt(t.reservedAt() != null ? t.reservedAt().toString() : null);
        entity.setExpiresAt(t.expiresAt() != null ? t.expiresAt().toString() : null);
        entity.setCreatedAt(t.createdAt().toString());
        return entity;
    }
}
