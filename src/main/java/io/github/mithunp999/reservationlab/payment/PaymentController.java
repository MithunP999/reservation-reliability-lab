package io.github.mithunp999.reservationlab.payment;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import io.github.mithunp999.reservationlab.common.ApiProblemException;
import jakarta.validation.Valid;

@RestController
public class PaymentController {

	private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_-]{1,64}");

	private final PaymentService service;

	public PaymentController(PaymentService service) {
		this.service = service;
	}

	@PostMapping("/bookings/{id}/payments")
	ResponseEntity<Object> pay(@PathVariable UUID id,
			@RequestHeader(name = "Idempotency-Key", required = false) String key,
			@Valid @RequestBody PaymentRequest request) {
		if (key == null || !KEY.matcher(key).matches()) {
			throw new ApiProblemException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED",
					"Idempotency-Key header is required: 1-64 characters from A-Z, a-z, 0-9, _ and -");
		}
		PaymentOutcome outcome = service.pay(id, key, request);
		ResponseEntity.BodyBuilder response = ResponseEntity.status(outcome.status());
		if (outcome.replayed()) {
			response.header("Idempotent-Replayed", "true");
		}
		return response.body(outcome.body());
	}
}
