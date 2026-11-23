package io.github.mithunp999.reservationlab.booking;

import io.github.mithunp999.reservationlab.support.ApiTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@Feature("Booking")
@Story("Validation")
@DisplayName("Booking validation API")
class BookingValidationApiTest extends ApiTestBase {

    static Stream<Arguments> badBodies() {
        return Stream.of(
                Arguments.of("missing eventId", Map.of("seats", 2, "customerEmail", "a@example.com"), "eventId"),
                Arguments.of("blank eventId", Map.of("eventId", " ", "seats", 2, "customerEmail", "a@example.com"), "eventId"),
                Arguments.of("missing seats", Map.of("eventId", "ev-1", "customerEmail", "a@example.com"), "seats"),
                Arguments.of("seats zero", Map.of("eventId", "ev-1", "seats", 0, "customerEmail", "a@example.com"), "seats"),
                Arguments.of("seats seven", Map.of("eventId", "ev-1", "seats", 7, "customerEmail", "a@example.com"), "seats"),
                Arguments.of("missing email", Map.of("eventId", "ev-1", "seats", 2), "customerEmail"),
                Arguments.of("bad email", Map.of("eventId", "ev-1", "seats", 2, "customerEmail", "not-an-email"), "customerEmail"));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Invalid body returns 400 with a field error")
    @MethodSource("badBodies")
    void invalidBodyReturns400WithFieldError(String name, Map<String, Object> body, String field) {
        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/bookings")
        .then()
            .statusCode(400)
            .body("code", equalTo("VALIDATION_FAILED"))
            .body("errors.field", hasItem(field));
    }

    @Test
    @DisplayName("Malformed JSON returns 400 VALIDATION_FAILED")
    void malformedJsonReturns400WithValidationFailedCode() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"seats\":\"two\"}")
        .when()
            .post("/bookings")
        .then()
            .statusCode(400)
            .body("code", equalTo("VALIDATION_FAILED"));
    }
}
