package io.github.mithunp999.reservationlab.booking;

public enum BookingStatus {
	HELD, PAYMENT_PENDING, CONFIRMED, EXPIRED, CANCELLED;

	/** The only place the transition rules live. No default branch: a new status must be handled here. */
	public boolean canMoveTo(BookingStatus next) {
		return switch (this) {
			case HELD -> next == PAYMENT_PENDING || next == CANCELLED || next == EXPIRED;
			case PAYMENT_PENDING -> next == CONFIRMED || next == HELD;
			case CONFIRMED, EXPIRED, CANCELLED -> false;
		};
	}
}
