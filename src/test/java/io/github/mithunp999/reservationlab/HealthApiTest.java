package io.github.mithunp999.reservationlab;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class HealthApiTest {

	@LocalServerPort
	int port;

	@Test
	void healthEndpointReportsUp() {
		given()
			.port(port)
		.when()
			.get("/actuator/health")
		.then()
			.statusCode(200)
			.body("status", equalTo("UP"));
	}
}
