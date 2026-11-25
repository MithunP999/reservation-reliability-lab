package io.github.mithunp999.reservationlab.common;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.mithunp999.reservationlab.booking.BookingNotFoundException;
import io.github.mithunp999.reservationlab.booking.IllegalTransitionException;
import io.github.mithunp999.reservationlab.payment.PaymentDeclinedException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail validation(MethodArgumentNotValidException e) {
		List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
				.map(f -> Map.of("field", f.getField(), "message", String.valueOf(f.getDefaultMessage())))
				.toList();
		return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", errors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail unreadable(HttpMessageNotReadableException e) {
		return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request body is malformed", List.of());
	}

	@ExceptionHandler(BookingNotFoundException.class)
	ProblemDetail notFound(BookingNotFoundException e) {
		return problem(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", e.getMessage(), null);
	}

	@ExceptionHandler(IllegalTransitionException.class)
	ProblemDetail illegalTransition(IllegalTransitionException e) {
		return problem(HttpStatus.CONFLICT, "ILLEGAL_TRANSITION", e.getMessage(), null);
	}

	@ExceptionHandler(PaymentDeclinedException.class)
	ProblemDetail paymentDeclined(PaymentDeclinedException e) {
		return problem(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_DECLINED", e.getMessage(), null);
	}

	private static ProblemDetail problem(HttpStatus status, String code, String detail, List<?> errors) {
		ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
		p.setProperty("code", code);
		if (errors != null) {
			p.setProperty("errors", errors);
		}
		return p;
	}
}
