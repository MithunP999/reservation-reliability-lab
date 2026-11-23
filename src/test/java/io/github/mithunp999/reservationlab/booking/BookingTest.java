package io.github.mithunp999.reservationlab.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.mithunp999.reservationlab.support.MutableClock;

class BookingTest {

	private MutableClock clock;
	private Booking held;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
		held = Booking.hold("evt-1", 2, "a@example.test", clock, Duration.ofMinutes(10));
	}

	@Test
	void paymentStartedThenFailedReturnsToHeld() {
		Booking back = held.startPayment(clock).paymentFailed();
		assertThat(back.status()).isEqualTo(BookingStatus.HELD);
	}

	@Test
	void paymentSucceededConfirms() {
		assertThat(held.startPayment(clock).paymentSucceeded().status()).isEqualTo(BookingStatus.CONFIRMED);
	}

	@Test
	void cancelAfterExpiryIsRejected() {
		clock.advance(Duration.ofMinutes(11));
		assertThatThrownBy(() -> held.cancel(clock))
				.isInstanceOfSatisfying(IllegalTransitionException.class, e -> {
					assertThat(e.from()).isEqualTo(BookingStatus.EXPIRED);
					assertThat(e.to()).isEqualTo(BookingStatus.CANCELLED);
				});
	}

	@Test
	void cancelDuringPaymentIsRejected() {
		Booking pending = held.startPayment(clock);
		assertThatThrownBy(() -> pending.cancel(clock)).isInstanceOf(IllegalTransitionException.class);
	}
}
