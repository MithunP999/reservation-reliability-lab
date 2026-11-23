package io.github.mithunp999.reservationlab.booking;

import java.time.Clock;
import java.util.UUID;

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
		Booking stored = find(id);
		Booking fresh = stored.refreshExpiry(clock);
		return fresh.equals(stored) ? stored : repository.save(fresh);
	}

	public Booking cancel(UUID id) {
		return repository.save(find(id).cancel(clock));
	}

	private Booking find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new BookingNotFoundException(id));
	}
}
