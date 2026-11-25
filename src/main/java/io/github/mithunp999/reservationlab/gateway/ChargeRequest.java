package io.github.mithunp999.reservationlab.gateway;

import java.util.UUID;

/** {@code amountMinor} is in minor units (paise, cents). */
public record ChargeRequest(UUID bookingId, long amountMinor, String currency, String idempotencyKey) {
}
