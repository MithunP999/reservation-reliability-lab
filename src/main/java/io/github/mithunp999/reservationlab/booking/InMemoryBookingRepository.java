package io.github.mithunp999.reservationlab.booking;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryBookingRepository implements BookingRepository {

	private final Map<UUID, Booking> store = new ConcurrentHashMap<>();

	@Override
	public Booking save(Booking booking) {
		store.put(booking.id(), booking);
		return booking;
	}

	@Override
	public Optional<Booking> findById(UUID id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public boolean replace(Booking expected, Booking updated) {
		return store.replace(expected.id(), expected, updated);
	}

	/** For test reset only. */
	public void deleteAll() {
		store.clear();
	}
}
