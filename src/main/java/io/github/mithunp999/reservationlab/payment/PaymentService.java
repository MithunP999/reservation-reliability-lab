package io.github.mithunp999.reservationlab.payment;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.github.mithunp999.reservationlab.booking.Booking;
import io.github.mithunp999.reservationlab.booking.BookingRepository;
import io.github.mithunp999.reservationlab.booking.BookingService;
import io.github.mithunp999.reservationlab.booking.BookingStatus;
import io.github.mithunp999.reservationlab.booking.IllegalTransitionException;
import io.github.mithunp999.reservationlab.gateway.ChargeRequest;
import io.github.mithunp999.reservationlab.gateway.ChargeResult;
import io.github.mithunp999.reservationlab.gateway.PaymentGateway;

@Service
public class PaymentService {

	private final BookingService bookings;
	private final BookingRepository repository;
	private final PaymentGateway gateway;
	private final Clock clock;

	public PaymentService(BookingService bookings, BookingRepository repository, PaymentGateway gateway,
			Clock clock) {
		this.bookings = bookings;
		this.repository = repository;
		this.gateway = gateway;
		this.clock = clock;
	}

	public Payment pay(UUID bookingId, PaymentRequest request) {
		Booking current = bookings.get(bookingId);

		// Claim before charging: only the caller that wins HELD -> PAYMENT_PENDING may call the gateway.
		Booking pending = current.startPayment(clock);
		if (!repository.replace(current, pending)) {
			throw new IllegalTransitionException(bookings.get(bookingId).status(), BookingStatus.PAYMENT_PENDING);
		}

		// If this throws, the booking stays PAYMENT_PENDING: the outcome is unknown, so the hold is not released.
		UUID paymentId = UUID.randomUUID();
		ChargeResult result = gateway.charge(
				new ChargeRequest(bookingId, request.amountMinor(), request.currency(), paymentId.toString()));

		if (result.status() == ChargeResult.Status.DECLINED) {
			// Nothing else moves a PAYMENT_PENDING booking yet, so this cannot lose.
			repository.replace(pending, pending.paymentFailed());
			throw new PaymentDeclinedException(bookingId);
		}
		return new Payment(paymentId, bookingId, result.gatewayRef(), result.status());
	}
}
