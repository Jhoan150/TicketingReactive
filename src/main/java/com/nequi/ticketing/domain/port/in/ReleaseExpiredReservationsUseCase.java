package com.nequi.ticketing.domain.port.in;

import reactor.core.publisher.Mono;

/**
 * Input port for the scheduled job that identifies and releases expired ticket reservations.
 */
public interface ReleaseExpiredReservationsUseCase {

    /**
     * Scans for tickets in RESERVED state whose reservation has expired,
     * reverts them to AVAILABLE, updates event inventory, and cancels associated orders.
     *
     * @return count of tickets released
     */
    Mono<Integer> execute();
}
