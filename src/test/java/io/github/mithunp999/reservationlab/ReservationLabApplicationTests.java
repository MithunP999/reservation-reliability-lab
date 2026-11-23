package io.github.mithunp999.reservationlab;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReservationLabApplicationTests {

	@Autowired
	io.github.mithunp999.reservationlab.booking.BookingProperties bookingProperties;

	@Test
	void contextLoads() {
		assertThat(bookingProperties.holdDuration()).isEqualTo(Duration.ofMinutes(10));
	}

}
