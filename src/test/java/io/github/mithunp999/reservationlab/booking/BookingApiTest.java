package io.github.mithunp999.reservationlab.booking;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.mithunp999.reservationlab.support.ApiTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Feature("Booking")
@DisplayName("Booking API")
class BookingApiTest extends ApiTestBase {

	private static final Map<String, Object> VALID = Map.of(
			"eventId", "evt-1", "seats", 2, "customerEmail", "a@example.test");

	private Response create() {
		return given().contentType(ContentType.JSON).body(VALID).post("/bookings");
	}

	@Test
	@Story("Create")
	@DisplayName("POST /bookings returns 201 with Location and a HELD booking")
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
	@Story("Get")
	@DisplayName("GET /bookings/{id} returns the booking")
	void getReturnsTheBooking() {
		String id = create().path("id");
		given().get("/bookings/{id}", id).then().statusCode(200)
				.body("id", equalTo(id))
				.body("status", equalTo("HELD"));
	}

	@Test
	@Story("Get")
	@DisplayName("GET unknown id returns 404 BOOKING_NOT_FOUND")
	void getUnknownIdReturns404() {
		given().get("/bookings/{id}", UUID.randomUUID()).then().statusCode(404)
				.body("code", equalTo("BOOKING_NOT_FOUND"));
	}

	@Test
	@Story("Cancel")
	@DisplayName("Cancel returns 200 and CANCELLED")
	void cancelReturns200AndCancelled() {
		String id = create().path("id");
		given().post("/bookings/{id}/cancel", id).then().statusCode(200)
				.body("status", equalTo("CANCELLED"));
	}

	@Test
	@Story("Cancel")
	@DisplayName("Cancelling twice returns 409 ILLEGAL_TRANSITION")
	void cancelTwiceReturns409() {
		String id = create().path("id");
		given().post("/bookings/{id}/cancel", id).then().statusCode(200);
		given().post("/bookings/{id}/cancel", id).then().statusCode(409)
				.body("code", equalTo("ILLEGAL_TRANSITION"));
	}

	@Test
	@Story("Hold expiry")
	@DisplayName("GET after the hold window shows EXPIRED")
	void getAfterHoldExpiryShowsExpired() {
		String id = create().path("id");
		clock.advance(Duration.ofMinutes(10).plusSeconds(1));
		given().get("/bookings/{id}", id).then().statusCode(200)
				.body("status", equalTo("EXPIRED"));
	}
}
