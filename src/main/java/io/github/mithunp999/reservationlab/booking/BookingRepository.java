package io.github.mithunp999.reservationlab.booking;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {

	Booking save(Booking booking);

	Optional<Booking> findById(UUID id);
}
