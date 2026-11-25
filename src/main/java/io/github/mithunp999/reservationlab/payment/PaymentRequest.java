package io.github.mithunp999.reservationlab.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
		@NotNull @Positive Long amountMinor,
		@NotNull @Pattern(regexp = "[A-Z]{3}") String currency) {
}
