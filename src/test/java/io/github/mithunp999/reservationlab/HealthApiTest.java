package io.github.mithunp999.reservationlab;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.mithunp999.reservationlab.support.ApiTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Health")
@DisplayName("Health API")
class HealthApiTest extends ApiTestBase {

	@Test
	@Story("Liveness")
	@DisplayName("GET /actuator/health reports UP")
	void healthEndpointReportsUp() {
		given()
		.when()
			.get("/actuator/health")
		.then()
			.statusCode(200)
			.body("status", equalTo("UP"));
	}
}
