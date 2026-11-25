package io.github.mithunp999.reservationlab.payment;

import java.util.UUID;

public class PaymentDeclinedException extends RuntimeException {

	public PaymentDeclinedException(UUID bookingId) {
		super("Payment declined for booking " + bookingId);
	}
}
