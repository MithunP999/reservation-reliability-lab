package io.github.mithunp999.reservationlab.booking;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {

	public BookingNotFoundException(UUID id) {
		super("Booking " + id + " not found");
	}
}
