package io.github.mithunp999.reservationlab.booking;

import java.time.Clock;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.springframework.stereotype.Service;

@Service
public class BookingService {

	private final BookingRepository repository;
	private final BookingProperties properties;
	private final Clock clock;

	public BookingService(BookingRepository repository, BookingProperties properties, Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.clock = clock;
	}

	public Booking create(CreateBookingRequest request) {
		return repository.save(Booking.hold(request.eventId(), request.seats(), request.customerEmail(), clock,
				properties.holdDuration()));
	}

	public Booking get(UUID id) {
		return update(id, booking -> booking.refreshExpiry(clock));
	}

	public Booking cancel(UUID id) {
		return update(id, booking -> booking.cancel(clock));
	}

	/**
	 * Compare-and-set with retry, so a write based on a stale read can never overwrite a payment claim. A lost race
	 * re-reads, and the domain rules are applied to the new state.
	 */
	private Booking update(UUID id, UnaryOperator<Booking> change) {
		while (true) {
			Booking stored = find(id);
			Booking changed = change.apply(stored);
			if (changed.equals(stored) || repository.replace(stored, changed)) {
				return changed;
			}
		}
	}

	private Booking find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new BookingNotFoundException(id));
	}
}
