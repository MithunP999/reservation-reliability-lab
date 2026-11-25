package io.github.mithunp999.reservationlab.payment;

import java.net.URI;
import java.time.Clock;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;

import io.github.mithunp999.reservationlab.booking.Booking;
import io.github.mithunp999.reservationlab.booking.BookingNotFoundException;
import io.github.mithunp999.reservationlab.booking.BookingRepository;
import io.github.mithunp999.reservationlab.booking.BookingService;
import io.github.mithunp999.reservationlab.booking.BookingStatus;
import io.github.mithunp999.reservationlab.booking.IllegalTransitionException;
import io.github.mithunp999.reservationlab.common.ApiProblemException;
import io.github.mithunp999.reservationlab.common.Problems;
import io.github.mithunp999.reservationlab.gateway.ChargeRequest;
import io.github.mithunp999.reservationlab.gateway.ChargeResult;
import io.github.mithunp999.reservationlab.gateway.PaymentGateway;
import io.github.mithunp999.reservationlab.payment.IdempotencyStore.Claim;
import io.github.mithunp999.reservationlab.payment.IdempotencyStore.Entry;
import io.github.mithunp999.reservationlab.payment.IdempotencyStore.Fingerprint;

@Service
public class PaymentService {

	private final BookingService bookings;
	private final BookingRepository repository;
	private final PaymentGateway gateway;
	private final IdempotencyStore store;
	private final Clock clock;

	public PaymentService(BookingService bookings, BookingRepository repository, PaymentGateway gateway,
			IdempotencyStore store, Clock clock) {
		this.bookings = bookings;
		this.repository = repository;
		this.gateway = gateway;
		this.store = store;
		this.clock = clock;
	}

	public PaymentOutcome pay(UUID bookingId, String key, PaymentRequest request) {
		Claim claim = store.claim(key, new Fingerprint(bookingId, request.amountMinor(), request.currency()),
				UUID.randomUUID());
		Entry owned = claim.entry();
		switch (claim.decision()) {
			case REPLAY -> {
				return new PaymentOutcome(owned.storedStatus(), owned.storedBody(), true);
			}
			case IN_PROGRESS -> throw new ApiProblemException(HttpStatus.CONFLICT, "IDEMPOTENCY_IN_PROGRESS",
					"A request with this Idempotency-Key is still in progress");
			case REUSED -> throw new ApiProblemException(HttpStatus.UNPROCESSABLE_CONTENT, "IDEMPOTENCY_KEY_REUSED",
					"This Idempotency-Key was already used for a different request");
			case NEW -> {
				try {
					claimBooking(bookingId);
				}
				catch (BookingNotFoundException e) {
					return complete(key, owned, problem(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", e, bookingId));
				}
				catch (IllegalTransitionException e) {
					return complete(key, owned, problem(HttpStatus.CONFLICT, "ILLEGAL_TRANSITION", e, bookingId));
				}
				catch (RuntimeException e) {
					store.abandon(key, owned);
					throw e;
				}
			}
			case RESUME -> {
				// This key already holds the booking (PAYMENT_PENDING) from the failed attempt: no second claim.
			}
		}
		return charge(bookingId, key, request, owned);
	}

	/** Claim before charging: only the caller that wins HELD -> PAYMENT_PENDING may call the gateway. */
	private void claimBooking(UUID bookingId) {
		Booking current = bookings.get(bookingId);
		Booking pending = current.startPayment(clock);
		if (!repository.replace(current, pending)) {
			throw new IllegalTransitionException(bookings.get(bookingId).status(), BookingStatus.PAYMENT_PENDING);
		}
	}

	private PaymentOutcome charge(UUID bookingId, String key, PaymentRequest request, Entry owned) {
		ChargeResult result;
		try {
			// The client's key goes to the gateway, so a retry of this key is deduplicated there too.
			result = gateway.charge(new ChargeRequest(bookingId, request.amountMinor(), request.currency(), key));
		}
		catch (RuntimeException e) {
			// Outcome unknown: the booking stays PAYMENT_PENDING and only this key may retry.
			store.retryable(key, owned);
			throw new ApiProblemException(HttpStatus.SERVICE_UNAVAILABLE, "GATEWAY_UNAVAILABLE",
					"Payment gateway unavailable; retry with the same Idempotency-Key");
		}

		if (result.status() == ChargeResult.Status.DECLINED) {
			// Release the booking before storing the 402, so a client that has seen 402 can pay again with a new key.
			// Only this key's owner moves a PAYMENT_PENDING booking, so this cannot lose.
			Booking pending = bookings.get(bookingId);
			repository.replace(pending, pending.paymentFailed());
			ProblemDetail declined = Problems.of(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_DECLINED",
					"Payment declined for booking " + bookingId);
			return complete(key, owned, withInstance(declined, bookingId));
		}
		return complete(key, owned, HttpStatus.CREATED,
				new Payment(owned.paymentId(), bookingId, result.gatewayRef(), result.status()));
	}

	private PaymentOutcome complete(String key, Entry owned, ProblemDetail problem) {
		return complete(key, owned, HttpStatus.valueOf(problem.getStatus()), problem);
	}

	private PaymentOutcome complete(String key, Entry owned, HttpStatus status, Object body) {
		store.complete(key, owned, status, body);
		return new PaymentOutcome(status, body, false);
	}

	private static ProblemDetail problem(HttpStatus status, String code, RuntimeException e, UUID bookingId) {
		return withInstance(Problems.of(status, code, e.getMessage()), bookingId);
	}

	/** Set up front so Spring never writes to a stored ProblemDetail that concurrent replays share. */
	private static ProblemDetail withInstance(ProblemDetail problem, UUID bookingId) {
		problem.setInstance(URI.create("/bookings/" + bookingId + "/payments"));
		return problem;
	}
}
