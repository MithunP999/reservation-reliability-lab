package io.github.mithunp999.reservationlab.payment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.mithunp999.reservationlab.gateway.ChargeRequest;
import io.github.mithunp999.reservationlab.support.ApiTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;

@Feature("Payments")
@DisplayName("Payment API")
class PaymentApiTest extends ApiTestBase {

	private static final String VALID = """
			{"amountMinor": 150000, "currency": "INR"}""";

	private String createHeld() {
		return given().contentType(ContentType.JSON)
				.body(Map.of("eventId", "evt-1", "seats", 2, "customerEmail", "a@example.test"))
				.post("/bookings").then().statusCode(201).extract().path("id");
	}

	private Response pay(String bookingId, String body) {
		return given().contentType(ContentType.JSON).body(body).post("/bookings/{id}/payments", bookingId);
	}

	private void assertStatus(String bookingId, String status) {
		given().get("/bookings/{id}", bookingId).then().statusCode(200).body("status", equalTo(status));
	}

	@Test
	@Story("Charge accepted")
	@DisplayName("Paying a held booking returns 201 and moves it to PAYMENT_PENDING")
	void payHeldReturns201AndPaymentPending() {
		String id = createHeld();

		String paymentId = pay(id, VALID).then().statusCode(201)
				.body("paymentId", notNullValue())
				.body("bookingId", equalTo(id))
				.body("gatewayRef", equalTo("fake-ref-1"))
				.body("status", equalTo("PENDING"))
				.extract().path("paymentId");

		assertStatus(id, "PAYMENT_PENDING");
		assertThat(gateway.calls())
				.containsExactly(new ChargeRequest(UUID.fromString(id), 150000L, "INR", paymentId));
	}

	@Test
	@Story("Charge declined")
	@DisplayName("A declined charge returns 402 PAYMENT_DECLINED and the booking goes back to HELD")
	void declinedReturns402AndHeld() {
		String id = createHeld();
		gateway.declineCharges();

		pay(id, VALID).then().statusCode(402).body("code", equalTo("PAYMENT_DECLINED"));

		assertStatus(id, "HELD");
		assertThat(gateway.calls()).hasSize(1);
	}

	@Test
	@Story("Illegal state")
	@DisplayName("Paying a cancelled booking returns 409 and never calls the gateway")
	void payCancelledReturns409WithoutGatewayCall() {
		String id = createHeld();
		given().post("/bookings/{id}/cancel", id).then().statusCode(200);

		pay(id, VALID).then().statusCode(409).body("code", equalTo("ILLEGAL_TRANSITION"));

		assertThat(gateway.calls()).isEmpty();
	}

	@Test 
	@Story("Illegal state")
	@DisplayName("Paying after the hold has expired returns 409 and never calls the gateway")
	void payExpiredReturns409WithoutGatewayCall() {
		String id = createHeld();
		// Simulate hold expiration 
		clock.advance(Duration.ofMinutes(10).plusSeconds(1));

		pay(id, VALID).then().statusCode(409).body("code", equalTo("ILLEGAL_TRANSITION"));

		assertThat(gateway.calls()).isEmpty();
		assertStatus(id, "EXPIRED");
	}

	@Test
	@Story("Illegal state")
	@DisplayName("Paying twice returns 409 and charges only once")
	void payTwiceReturns409AndChargesOnce() {
		String id = createHeld();
		pay(id, VALID).then().statusCode(201);

		pay(id, VALID).then().statusCode(409).body("code", equalTo("ILLEGAL_TRANSITION"));

		assertThat(gateway.calls()).hasSize(1);
	}

	@Test
	@Story("Unknown booking")
	@DisplayName("Paying an unknown booking returns 404 and never calls the gateway")
	void payUnknownReturns404() {
		pay(UUID.randomUUID().toString(), VALID).then().statusCode(404).body("code", equalTo("BOOKING_NOT_FOUND"));

		assertThat(gateway.calls()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"currency\": \"INR\"}",
			"{\"amountMinor\": 0, \"currency\": \"INR\"}",
			"{\"amountMinor\": -1, \"currency\": \"INR\"}",
			"{\"amountMinor\": 10.9, \"currency\": \"INR\"}",
			"{\"amountMinor\": 100}",
			"{\"amountMinor\": 100, \"currency\": \"inr\"}",
			"{\"amountMinor\": 100, \"currency\": \"INRR\"}" })
	@Story("Validation")
	@DisplayName("An invalid payment body returns 400 VALIDATION_FAILED and never calls the gateway")
	void invalidBodyReturns400(String body) {
		String id = createHeld();

		pay(id, body).then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));

		assertThat(gateway.calls()).isEmpty();
		assertStatus(id, "HELD");
	}
}
