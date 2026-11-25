package io.github.mithunp999.reservationlab.payment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import io.github.mithunp999.reservationlab.gateway.ChargeRequest;
import io.github.mithunp999.reservationlab.support.ApiTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

@Feature("Idempotency")
@DisplayName("Idempotency-Key on payments")
class IdempotencyApiTest extends ApiTestBase {

	private static final String VALID = """
			{"amountMinor": 150000, "currency": "INR"}""";

	static Stream<String> badKeys() {
		return Stream.of("", "has space", "bad!key", "k".repeat(65));
	}

	@ParameterizedTest
	@NullSource
	@MethodSource("badKeys")
	@Story("Key required")
	@DisplayName("A missing or invalid Idempotency-Key returns 400 and never calls the gateway")
	void missingOrInvalidKeyReturns400(String key) {
		String id = createHeld();
		RequestSpecification request = given().contentType(ContentType.JSON).body(VALID);
		if (key != null) {
			request.header("Idempotency-Key", key);
		}

		request.post("/bookings/{id}/payments", id).then().statusCode(400)
				.body("code", equalTo("IDEMPOTENCY_KEY_REQUIRED"));

		assertThat(gateway.calls()).isEmpty();
		assertStatus(id, "HELD");
	}

	@Test
	@Story("Replay")
	@DisplayName("The same key twice replays the identical 201 and charges once")
	void sameKeyTwiceReplaysIdenticalResponse() {
		String id = createHeld();

		Response first = pay(id, VALID, "key-1");
		Response second = pay(id, VALID, "key-1");

		first.then().statusCode(201).header("Idempotent-Replayed", nullValue());
		second.then().statusCode(201).header("Idempotent-Replayed", equalTo("true"));
		assertThat(second.asString()).isEqualTo(first.asString());
		assertThat(gateway.calls()).hasSize(1);
	}

	@Test
	@Story("Replay")
	@DisplayName("A declined payment is a result: the same key replays the 402")
	void declinedThenSameKeyReplays402() {
		String id = createHeld();
		gateway.declineCharges();

		Response first = pay(id, VALID, "key-1");
		Response second = pay(id, VALID, "key-1");

		first.then().statusCode(402).body("code", equalTo("PAYMENT_DECLINED"));
		second.then().statusCode(402).header("Idempotent-Replayed", equalTo("true"));
		assertThat(second.asString()).isEqualTo(first.asString());
		assertThat(gateway.calls()).hasSize(1);
		assertStatus(id, "HELD");
	}

	@Test
	@Story("Key reuse")
	@DisplayName("The same key with a different amount returns 422 IDEMPOTENCY_KEY_REUSED")
	void sameKeyDifferentAmountReturns422() {
		String id = createHeld();
		pay(id, VALID, "key-1").then().statusCode(201);

		pay(id, """
				{"amountMinor": 200000, "currency": "INR"}""", "key-1")
				.then().statusCode(422).body("code", equalTo("IDEMPOTENCY_KEY_REUSED"));

		assertThat(gateway.calls()).hasSize(1);
	}

	@Test
	@Story("Gateway failure")
	@DisplayName("Gateway throws: 503, then a retry with the same key charges again with that key and succeeds")
	void gatewayFailureThenRetrySameKeySucceeds() {
		String id = createHeld();
		gateway.failNext(1);

		pay(id, VALID, "key-1").then().statusCode(503).body("code", equalTo("GATEWAY_UNAVAILABLE"));
		assertStatus(id, "PAYMENT_PENDING");

		pay(id, VALID, "key-1").then().statusCode(201).header("Idempotent-Replayed", nullValue());

		assertStatus(id, "PAYMENT_PENDING");
		assertThat(gateway.calls()).extracting(ChargeRequest::idempotencyKey).containsExactly("key-1", "key-1");
	}

	@Test
	@Story("Gateway failure")
	@DisplayName("After a gateway failure, a different key gets 409: the first key still holds the booking")
	void afterGatewayFailureDifferentKeyReturns409() {
		String id = createHeld();
		gateway.failNext(1);
		pay(id, VALID, "key-1").then().statusCode(503);

		pay(id, VALID, "key-2").then().statusCode(409).body("code", equalTo("ILLEGAL_TRANSITION"));

		assertThat(gateway.calls()).hasSize(1);
	}
}
