package io.github.mithunp999.reservationlab.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.mithunp999.reservationlab.support.MutableClock;

class BookingExpiryTest {

	private static final Duration HOLD = Duration.ofMinutes(10);

	private MutableClock clock;
	private Booking held;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
		held = Booking.hold("evt-1", 2, "a@example.test", clock, HOLD);
	}

	@Test
	void holdExpiresLazilyAfterHoldDuration() {
		clock.advance(Duration.ofMinutes(9).plusSeconds(59));
		assertThat(held.refreshExpiry(clock).status()).isEqualTo(BookingStatus.HELD);

		clock.advance(Duration.ofSeconds(2));
		assertThat(held.refreshExpiry(clock).status()).isEqualTo(BookingStatus.EXPIRED);
	}

	@Test
	void expiryPausesWhilePaymentIsPending() {
		Booking pending = held.startPayment(clock);
		clock.advance(Duration.ofMinutes(30));
		assertThat(pending.refreshExpiry(clock).status()).isEqualTo(BookingStatus.PAYMENT_PENDING);
	}

	@Test
	void failedPaymentAfterHoldPassedReturnsToHeldThenExpires() {
		Booking pending = held.startPayment(clock);
		clock.advance(Duration.ofMinutes(30));
		Booking back = pending.paymentFailed();
		assertThat(back.status()).isEqualTo(BookingStatus.HELD);
		assertThat(back.refreshExpiry(clock).status()).isEqualTo(BookingStatus.EXPIRED);
	}
}
