package io.github.mithunp999.reservationlab.payment;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class PaymentController {

	private final PaymentService service;

	public PaymentController(PaymentService service) {
		this.service = service;
	}

	@PostMapping("/bookings/{id}/payments")
	@ResponseStatus(HttpStatus.CREATED)
	Payment pay(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request) {
		return service.pay(id, request);
	}
}
