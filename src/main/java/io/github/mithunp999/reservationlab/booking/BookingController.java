package io.github.mithunp999.reservationlab.booking;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/bookings")
public class BookingController {

	private final BookingService service;

	public BookingController(BookingService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
		Booking booking = service.create(request);
		var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(booking.id());
		return ResponseEntity.created(location).body(BookingResponse.from(booking));
	}

	@GetMapping("/{id}")
	BookingResponse get(@PathVariable UUID id) {
		return BookingResponse.from(service.get(id));
	}

	@PostMapping("/{id}/cancel")
	BookingResponse cancel(@PathVariable UUID id) {
		return BookingResponse.from(service.cancel(id));
	}
}
