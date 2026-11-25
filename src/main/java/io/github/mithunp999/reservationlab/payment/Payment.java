package io.github.mithunp999.reservationlab.payment;

import java.util.UUID;

import io.github.mithunp999.reservationlab.gateway.ChargeResult;

/** Returned to the client. Not stored yet: nothing reads it back. */
public record Payment(UUID paymentId, UUID bookingId, String gatewayRef, ChargeResult.Status status) {
}
