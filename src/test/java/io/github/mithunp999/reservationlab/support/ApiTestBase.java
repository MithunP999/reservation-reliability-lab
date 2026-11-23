package io.github.mithunp999.reservationlab.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import io.github.mithunp999.reservationlab.booking.InMemoryBookingRepository;
import io.restassured.RestAssured;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestClockConfig.class)
public abstract class ApiTestBase {

	@LocalServerPort
	int port;

	@Autowired
	protected MutableClock clock;

	@Autowired
	InMemoryBookingRepository bookings;

	@BeforeEach
	void resetState() {
		RestAssured.port = port;
		clock.reset(TestClockConfig.START);
		bookings.deleteAll();
	}
}
