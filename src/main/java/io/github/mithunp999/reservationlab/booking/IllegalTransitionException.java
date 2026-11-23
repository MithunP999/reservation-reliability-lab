package io.github.mithunp999.reservationlab.booking;

public class IllegalTransitionException extends RuntimeException {

	private final BookingStatus from;
	private final BookingStatus to;

	public IllegalTransitionException(BookingStatus from, BookingStatus to) {
		super("Illegal booking transition " + from + " -> " + to);
		this.from = from;
		this.to = to;
	}

	public BookingStatus from() {
		return from;
	}

	public BookingStatus to() {
		return to;
	}
}
