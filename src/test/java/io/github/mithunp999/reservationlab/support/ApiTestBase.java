package io.github.mithunp999.reservationlab.support;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import io.github.mithunp999.reservationlab.booking.InMemoryBookingRepository;
import io.github.mithunp999.reservationlab.payment.IdempotencyStore;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

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

	@Autowired
	IdempotencyStore idempotencyStore;

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
		idempotencyStore.clear();
	}

	protected String createHeld() {
		return given().contentType(ContentType.JSON)
				.body(Map.of("eventId", "evt-1", "seats", 2, "customerEmail", "a@example.test"))
				.post("/bookings").then().statusCode(201).extract().path("id");
	}

	/** Pays with a fresh Idempotency-Key. */
	protected Response pay(String bookingId, String body) {
		return pay(bookingId, body, UUID.randomUUID().toString());
	}

	protected Response pay(String bookingId, String body, String idempotencyKey) {
		return given().contentType(ContentType.JSON).header("Idempotency-Key", idempotencyKey).body(body)
				.post("/bookings/{id}/payments", bookingId);
	}

	protected void assertStatus(String bookingId, String status) {
		given().get("/bookings/{id}", bookingId).then().statusCode(200).body("status", equalTo(status));
	}
}
