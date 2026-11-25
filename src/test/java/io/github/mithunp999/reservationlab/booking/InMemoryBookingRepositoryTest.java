package io.github.mithunp999.reservationlab.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.github.mithunp999.reservationlab.support.MutableClock;

class InMemoryBookingRepositoryTest {

	@Test
	void replaceOnlySucceedsWhenExpectedIsStillStored() {
		var clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
		var repository = new InMemoryBookingRepository();
		Booking held = repository.save(Booking.hold("evt-1", 2, "a@example.test", clock, Duration.ofMinutes(10)));
		Booking pending = held.startPayment(clock);

		assertThat(repository.replace(held, pending)).isTrue();
		// A second writer that still holds the old HELD value loses.
		assertThat(repository.replace(held, held.cancel(clock))).isFalse();
		assertThat(repository.findById(held.id())).contains(pending);
	}
}
