package io.github.mithunp999.reservationlab.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import io.github.mithunp999.reservationlab.booking.InMemoryBookingRepository;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({ TestClockConfig.class, TestGatewayConfig.class })
public abstract class ApiTestBase {

	@LocalServerPort
	int port;

	@Autowired
	protected MutableClock clock;

	@Autowired
	InMemoryBookingRepository bookings;

	@Autowired
	protected FakePaymentGateway gateway;

	private static boolean allureFilterAdded;

	@BeforeEach
	void resetState() {
		RestAssured.port = port;
		// Register the Allure filter once for the whole JVM; adding it per test would stack
		// duplicate request/response attachments.
		if (!allureFilterAdded) {
			RestAssured.filters(new AllureRestAssured());
			allureFilterAdded = true;
		}
		clock.reset(TestClockConfig.START);
		bookings.deleteAll();
		gateway.reset();
	}
}
