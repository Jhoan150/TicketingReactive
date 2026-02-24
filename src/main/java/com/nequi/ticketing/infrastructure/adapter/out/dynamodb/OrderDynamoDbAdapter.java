package com.nequi.ticketing.infrastructure.adapter.out.dynamodb;

import com.nequi.ticketing.domain.enums.OrderStatus;
import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.port.out.OrderRepositoryPort;
import com.nequi.ticketing.infrastructure.adapter.out.dynamodb.entity.OrderEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;

/**
 * DynamoDB adapter implementing {@link OrderRepositoryPort}.
 */
@Component
public class OrderDynamoDbAdapter implements OrderRepositoryPort {

    private static final String TABLE_NAME = "orders";

    private final DynamoDbAsyncTable<OrderEntity> table;

    public OrderDynamoDbAdapter(DynamoDbEnhancedAsyncClient enhancedClient) {
        this.table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(OrderEntity.class));
    }

    @Override
    public Mono<Order> findById(String orderId) {
        return Mono.fromFuture(table.getItem(r -> r.key(k -> k.partitionValue(orderId))))
                .map(this::toDomain);
    }

    @Override
    public Mono<Order> save(Order order) {
        return Mono.fromFuture(table.putItem(toEntity(order)))
                .thenReturn(order);
    }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private Order toDomain(OrderEntity e) {
        return new Order(
                e.getOrderId(),
                e.getEventId(),
                e.getUserId(),
                e.getQuantity(),
                OrderStatus.valueOf(e.getStatus()),
                e.getFailureReason(),
                Instant.parse(e.getCreatedAt()),
                Instant.parse(e.getUpdatedAt())
        );
    }

    private OrderEntity toEntity(Order o) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(o.orderId());
        entity.setEventId(o.eventId());
        entity.setUserId(o.userId());
        entity.setQuantity(o.quantity());
        entity.setStatus(o.status().name());
        entity.setFailureReason(o.failureReason());
        entity.setCreatedAt(o.createdAt().toString());
        entity.setUpdatedAt(o.updatedAt().toString());
        return entity;
    }
}
