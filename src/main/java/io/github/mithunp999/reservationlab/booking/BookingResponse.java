package io.github.mithunp999.reservationlab.booking;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(UUID id, String eventId, int seats, String customerEmail, BookingStatus status,
		Instant holdExpiresAt) {

	static BookingResponse from(Booking b) {
		return new BookingResponse(b.id(), b.eventId(), b.seats(), b.customerEmail(), b.status(), b.holdExpiresAt());
	}
}
