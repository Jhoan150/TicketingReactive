package com.nequi.ticketing.infrastructure.adapter.in.web;

import com.nequi.ticketing.domain.model.Order;
import com.nequi.ticketing.domain.port.in.GetOrderStatusUseCase;
import com.nequi.ticketing.domain.port.in.ReserveTicketsUseCase;
import com.nequi.ticketing.infrastructure.adapter.in.web.dto.CreateOrderRequest;
import com.nequi.ticketing.infrastructure.adapter.in.web.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * WebFlux REST controller for Order endpoints.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final ReserveTicketsUseCase reserveTicketsUseCase;
    private final GetOrderStatusUseCase getOrderStatusUseCase;

    public OrderController(ReserveTicketsUseCase reserveTicketsUseCase,
                           GetOrderStatusUseCase getOrderStatusUseCase) {
        this.reserveTicketsUseCase = reserveTicketsUseCase;
        this.getOrderStatusUseCase = getOrderStatusUseCase;
    }

    /**
     * Initiates a ticket purchase: reserves tickets and enqueues the order.
     *
     * @param request purchase details
     * @return 202 Accepted with the created order ID and PENDING status
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var command = new ReserveTicketsUseCase.ReserveTicketsCommand(
                request.eventId(),
                request.userId(),
                request.quantity()
        );
        return reserveTicketsUseCase.execute(command).map(this::toResponse);
    }

    /**
     * Returns the current status of a purchase order.
     *
     * @param orderId the order identifier
     * @return order details with current status
     */
    @GetMapping("/{orderId}")
    public Mono<OrderResponse> getOrderStatus(@PathVariable String orderId) {
        return getOrderStatusUseCase.execute(orderId).map(this::toResponse);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.orderId(),
                order.eventId(),
                order.userId(),
                order.quantity(),
                order.status().name(),
                order.failureReason(),
                order.createdAt(),
                order.updatedAt()
        );
    }
}
