package io.github.mithunp999.reservationlab.payment;

import org.springframework.http.HttpStatus;

/** What to send back: a {@link Payment} or a ProblemDetail, and whether it is a replay. */
public record PaymentOutcome(HttpStatus status, Object body, boolean replayed) {
}
