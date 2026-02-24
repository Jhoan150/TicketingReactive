package com.nequi.ticketing.infrastructure.adapter.out.dynamodb;

import com.nequi.ticketing.domain.model.Event;
import com.nequi.ticketing.domain.port.out.EventRepositoryPort;
import com.nequi.ticketing.infrastructure.adapter.out.dynamodb.entity.EventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DynamoDB adapter implementing
 *
 * Uses the DynamoDB Enhanced Client for type-safe operations.
 * Optimistic locking uses the @DynamoDbVersionAttribute annotation on EventEntity,
 * which automatically generates conditional write expressions.
 */
@Component
public class EventDynamoDbAdapter implements EventRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(EventDynamoDbAdapter.class);
    private static final String TABLE_NAME = "events";

    private final DynamoDbAsyncTable<EventEntity> table;

    public EventDynamoDbAdapter(DynamoDbEnhancedAsyncClient enhancedClient) {
        this.table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(EventEntity.class));
    }

    @Override
    public Mono<Event> findById(String eventId) {
        return Mono.fromFuture(table.getItem(r -> r.key(k -> k.partitionValue(eventId)).consistentRead(true)))
                .map(this::toDomain);
    }

    @Override
    public Mono<Event> save(Event event) {
        EventEntity entity = toEntity(event);
        return Mono.fromFuture(table.putItem(entity))
                .thenReturn(event);
    }

    @Override
    public Flux<Event> findAll() {
        return Flux.from(table.scan().items())
                .map(this::toDomain);
    }

    @Override
    public Flux<Event> findAllWithAvailableTickets() {
        Expression filterExpression = Expression.builder()
                .expression("availableTickets > :zero")
                .expressionValues(Map.of(":zero", AttributeValue.fromN("0")))
                .build();

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return Flux.from(table.scan(scanRequest).items())
                .map(this::toDomain);
    }

    @Override
    public Mono<Event> updateWithOptimisticLock(Event event, Long expectedVersion) {
        EventEntity entity = toEntity(event);
        entity.setVersion(expectedVersion);

        return Mono.fromFuture(table.putItem(entity))
                .thenReturn(event)
                .onErrorResume(ConditionalCheckFailedException.class, ex -> {
                    log.warn("Optimistic lock conflict on event [{}], DB version is not {}",
                            event.eventId(), expectedVersion);
                    return Mono.empty();
                });
    }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private Event toDomain(EventEntity e) {
        return new Event(
                e.getEventId(),
                e.getName(),
                LocalDateTime.parse(e.getDate()),
                e.getVenue(),
                e.getTotalCapacity(),
                e.getAvailableTickets(),
                e.getVersion(),
                Instant.parse(e.getCreatedAt())
        );
    }

    private EventEntity toEntity(Event e) {
        EventEntity entity = new EventEntity();
        entity.setEventId(e.eventId());
        entity.setName(e.name());
        entity.setDate(e.date().toString());
        entity.setVenue(e.venue());
        entity.setTotalCapacity(e.totalCapacity());
        entity.setAvailableTickets(e.availableTickets());
        entity.setVersion(e.version());
        entity.setCreatedAt(e.createdAt().toString());
        return entity;
    }
}
