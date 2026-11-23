package io.github.mithunp999.reservationlab;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.github.mithunp999.reservationlab.support.ApiTestBase;

class HealthApiTest extends ApiTestBase {

	@Test
	void healthEndpointReportsUp() {
		given()
		.when()
			.get("/actuator/health")
		.then()
			.statusCode(200)
			.body("status", equalTo("UP"));
	}
}
