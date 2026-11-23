package io.github.mithunp999.reservationlab.booking;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class BookingStatusTransitionTest {

    @ParameterizedTest(name="{0} -> {1} allowed = {2}")
    @CsvSource({
             // from HELD
        "HELD, HELD, false",
        "HELD, PAYMENT_PENDING, true",
        "HELD, CONFIRMED, false",
        "HELD, EXPIRED, true",
        "HELD, CANCELLED, true",
        // from PAYMENT_PENDING
        "PAYMENT_PENDING, HELD, true",
        "PAYMENT_PENDING, PAYMENT_PENDING, false",  
        "PAYMENT_PENDING, CONFIRMED, true",
        "PAYMENT_PENDING, EXPIRED, false",
        "PAYMENT_PENDING, CANCELLED, false",
        // from CONFIRMED (final)
        "CONFIRMED, HELD, false",
        "CONFIRMED, PAYMENT_PENDING, false",
        "CONFIRMED, CONFIRMED, false",
        "CONFIRMED, EXPIRED, false",
        "CONFIRMED, CANCELLED, false",
        // from EXPIRED (final)
        "EXPIRED, HELD, false",
        "EXPIRED, PAYMENT_PENDING, false",
        "EXPIRED, CONFIRMED, false",
        "EXPIRED, EXPIRED, false",
        "EXPIRED, CANCELLED, false",
        // from CANCELLED (final)
        "CANCELLED, HELD, false",
        "CANCELLED, PAYMENT_PENDING, false",
        "CANCELLED, CONFIRMED, false",
        "CANCELLED, EXPIRED, false",
        "CANCELLED, CANCELLED, false"

    })
    void transitionRules(
        BookingStatus from,
        BookingStatus to, 
        boolean allowed) {

        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }
}
