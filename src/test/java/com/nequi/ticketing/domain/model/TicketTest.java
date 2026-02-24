package com.nequi.ticketing.domain.model;

import com.nequi.ticketing.domain.enums.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TicketTest {

    @Test
    void shouldCreateTicketWithCorrectInitialState() {
        String ticketId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        Ticket ticket = new Ticket(ticketId, eventId, TicketStatus.AVAILABLE, null, null, null, now);

        assertEquals(ticketId, ticket.ticketId());
        assertEquals(eventId, ticket.eventId());
        assertEquals(TicketStatus.AVAILABLE, ticket.status());
        assertNull(ticket.orderId());
        assertNull(ticket.reservedAt());
        assertNull(ticket.expiresAt());
        assertEquals(now, ticket.createdAt());
    }

    @Test
    void shouldTransitionToReserved() {
        Ticket available = new Ticket(UUID.randomUUID().toString(), UUID.randomUUID().toString(), TicketStatus.AVAILABLE, null, null, null, Instant.now());
        String orderId = "order-123";
        Instant reservedAt = Instant.now();

        Ticket reserved = available.reserve(orderId, reservedAt);

        assertEquals(TicketStatus.RESERVED, reserved.status());
        assertEquals(orderId, reserved.orderId());
        assertEquals(reservedAt, reserved.reservedAt());
        assertNotNull(reserved.expiresAt());
        assertTrue(reserved.expiresAt().isAfter(reservedAt));
    }

    @Test
    void shouldTransitionToPendingConfirmation() {
        Ticket reserved = new Ticket(UUID.randomUUID().toString(), UUID.randomUUID().toString(), TicketStatus.RESERVED, "order-1", Instant.now(), Instant.now().plusSeconds(600), Instant.now());

        Ticket pending = reserved.confirm();

        assertEquals(TicketStatus.PENDING_CONFIRMATION, pending.status());
        assertEquals("order-1", pending.orderId());
    }

    @Test
    void shouldTransitionToSold() {
        Ticket pending = new Ticket(UUID.randomUUID().toString(), UUID.randomUUID().toString(), TicketStatus.PENDING_CONFIRMATION, "order-1", Instant.now(), Instant.now().plusSeconds(600), Instant.now());

        Ticket sold = pending.sell();

        assertEquals(TicketStatus.SOLD, sold.status());
        assertEquals("order-1", sold.orderId());
    }

    @Test
    void shouldTransitionToComplimentary() {
        Ticket available = new Ticket(UUID.randomUUID().toString(), UUID.randomUUID().toString(), TicketStatus.AVAILABLE, null, null, null, Instant.now());

        Ticket complimentary = available.complimentary();

        assertEquals(TicketStatus.COMPLIMENTARY, complimentary.status());
        assertNull(complimentary.orderId());
    }

    @Test
    void shouldReleaseTicketBackToAvailable() {
        Ticket reserved = new Ticket(UUID.randomUUID().toString(), UUID.randomUUID().toString(), TicketStatus.RESERVED, "order-1", Instant.now(), Instant.now().plusSeconds(600), Instant.now());

        Ticket released = reserved.release();

        assertEquals(TicketStatus.AVAILABLE, released.status());
        assertNull(released.orderId());
        assertNull(released.reservedAt());
        assertNull(released.expiresAt());
    }

    @Test
    void shouldDetectExpiredReservation() {
        Instant now = Instant.now();
        Ticket expiredTicket = new Ticket(UUID.randomUUID().toString(), UUID.randomUUID().toString(), TicketStatus.RESERVED, "order-1", now.minusSeconds(700), now.minusSeconds(100), now.minusSeconds(1000));
        Ticket activeTicket = new Ticket(UUID.randomUUID().toString(), UUID.randomUUID().toString(), TicketStatus.RESERVED, "order-1", now.minusSeconds(100), now.plusSeconds(500), now.minusSeconds(1000));

        assertTrue(expiredTicket.isExpired(now));
        assertFalse(activeTicket.isExpired(now));
    }
}
