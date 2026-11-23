package io.github.mithunp999.reservationlab.booking;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.mithunp999.reservationlab.support.ApiTestBase;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

class BookingApiTest extends ApiTestBase {

	private static final Map<String, Object> VALID = Map.of(
			"eventId", "evt-1", "seats", 2, "customerEmail", "a@example.test");

	private Response create() {
		return given().contentType(ContentType.JSON).body(VALID).post("/bookings");
	}

	@Test
	void createReturns201WithLocationAndHeldBooking() {
		Response r = create();
		String id = r.path("id");
		r.then().statusCode(201)
				.header("Location", endsWith("/bookings/" + id))
				.body("eventId", equalTo("evt-1"))
				.body("seats", equalTo(2))
				.body("customerEmail", equalTo("a@example.test"))
				.body("status", equalTo("HELD"))
				.body("holdExpiresAt", equalTo("2026-01-01T10:10:00Z"));
	}

	@Test
	void getReturnsTheBooking() {
		String id = create().path("id");
		given().get("/bookings/{id}", id).then().statusCode(200)
				.body("id", equalTo(id))
				.body("status", equalTo("HELD"));
	}

	@Test
	void getUnknownIdReturns404() {
		given().get("/bookings/{id}", UUID.randomUUID()).then().statusCode(404)
				.body("code", equalTo("BOOKING_NOT_FOUND"));
	}

	@Test
	void cancelReturns200AndCancelled() {
		String id = create().path("id");
		given().post("/bookings/{id}/cancel", id).then().statusCode(200)
				.body("status", equalTo("CANCELLED"));
	}

	@Test
	void cancelTwiceReturns409() {
		String id = create().path("id");
		given().post("/bookings/{id}/cancel", id).then().statusCode(200);
		given().post("/bookings/{id}/cancel", id).then().statusCode(409)
				.body("code", equalTo("ILLEGAL_TRANSITION"));
	}

	@Test
	void getAfterHoldExpiryShowsExpired() {
		String id = create().path("id");
		clock.advance(Duration.ofMinutes(10).plusSeconds(1));
		given().get("/bookings/{id}", id).then().statusCode(200)
				.body("status", equalTo("EXPIRED"));
	}
}
