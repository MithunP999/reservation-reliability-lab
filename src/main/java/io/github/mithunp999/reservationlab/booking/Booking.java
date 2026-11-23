package io.github.mithunp999.reservationlab.booking;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Booking(UUID id, String eventId, int seats, String customerEmail, BookingStatus status,
		Instant createdAt, Instant holdExpiresAt) {

	public Booking {
		Objects.requireNonNull(id);
		Objects.requireNonNull(eventId);
		Objects.requireNonNull(customerEmail);
		Objects.requireNonNull(status);
		Objects.requireNonNull(createdAt);
		Objects.requireNonNull(holdExpiresAt);
		if (seats <= 0) {
			throw new IllegalArgumentException("seats must be positive");
		}
	}

	public static Booking hold(String eventId, int seats, String customerEmail, Clock clock, Duration holdDuration) {
		Instant now = clock.instant();
		return new Booking(UUID.randomUUID(), eventId, seats, customerEmail, BookingStatus.HELD, now,
				now.plus(holdDuration));
	}

	/** Lazy expiry: only HELD expires. PAYMENT_PENDING is left alone because the money may still arrive. */
	public Booking refreshExpiry(Clock clock) {
		if (status == BookingStatus.HELD && clock.instant().isAfter(holdExpiresAt)) {
			return moveTo(BookingStatus.EXPIRED);
		}
		return this;
	}

	public Booking startPayment(Clock clock) {
		return refreshExpiry(clock).moveTo(BookingStatus.PAYMENT_PENDING);
	}

	public Booking cancel(Clock clock) {
		return refreshExpiry(clock).moveTo(BookingStatus.CANCELLED);
	}

	public Booking paymentSucceeded() {
		return moveTo(BookingStatus.CONFIRMED);
	}

	public Booking paymentFailed() {
		return moveTo(BookingStatus.HELD);
	}

	private Booking moveTo(BookingStatus next) {
		if (!status.canMoveTo(next)) {
			throw new IllegalTransitionException(status, next);
		}
		return new Booking(id, eventId, seats, customerEmail, next, createdAt, holdExpiresAt);
	}
}
