package io.github.mithunp999.reservationlab.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import io.github.mithunp999.reservationlab.support.ApiTestSupport;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;

/** Hits the real {@link HttpPaymentGateway}; WireMock plays the payment provider. */
@Feature("Gateway retries")
@DisplayName("Gateway retries over HTTP")
@EnableWireMock(@ConfigureWireMock(name = "payment-gateway", baseUrlProperties = "gateway.base-url"))
@TestPropertySource(properties = "gateway.read-timeout=300ms")
class GatewayRetryApiTest extends ApiTestSupport {

	private static final String VALID = """
			{"amountMinor": 150000, "currency": "INR"}""";
	private static final String KEY = "key-1";
	private static final List<Duration> SLEEPS = new CopyOnWriteArrayList<>();

	@TestConfiguration
	static class NoOpSleeperConfig {

		/** Records the backoff instead of waiting. */
		@Bean
		@Primary
		Sleeper recordingSleeper() {
			return SLEEPS::add;
		}
	}

	@InjectWireMock("payment-gateway")
	WireMockServer wireMock;

	@Autowired
	RestClient paymentGatewayRestClient;

	private static boolean warmedUp;

	@BeforeEach
	void resetSleepsAndWarmUpOnce() {
		SLEEPS.clear();
		if (!warmedUp) {
			// The first gateway call in a fresh JVM (class loading, first connection) took ~650ms here, above the
			// 300ms read timeout, so whichever test ran first timed out. Warm the real client once, then reset
			// WireMock so the warm-up never shows in a request count.
			wireMock.stubFor(WireMock.post("/warmup").willReturn(WireMock.ok()));
			await().atMost(Duration.ofSeconds(10)).ignoreExceptions()
					.until(() -> paymentGatewayRestClient.post().uri("/warmup")
							.body(new HttpPaymentGateway.ChargeBody(UUID.randomUUID(), 1, "INR"))
							.retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful());
			wireMock.resetAll();
			warmedUp = true;
		}
	}

	@Test
	@Story("Retry on 5xx")
	@DisplayName("500 then 201: the retry succeeds with the same Idempotency-Key")
	void serverErrorThenCreatedRetriesWithSameKey() {
		String id = createHeld();
		wireMock.stubFor(charge().inScenario("flaky").whenScenarioStateIs(Scenario.STARTED)
				.willReturn(WireMock.serverError()).willSetStateTo("recovered"));
		wireMock.stubFor(charge().inScenario("flaky").whenScenarioStateIs("recovered").willReturn(created()));

		pay(id, VALID, KEY).then().statusCode(201).body("gatewayRef", equalTo("gw-1"));

		wireMock.verify(2, charges());
		wireMock.verify(2, charges().withHeader("Idempotency-Key", WireMock.equalTo(KEY))
				.withRequestBody(WireMock.equalToJson("""
						{"reference": "%s", "amountMinor": 150000, "currency": "INR"}""".formatted(id))));
		assertThat(SLEEPS).containsExactly(Duration.ofMillis(200));
		assertStatus(id, "PAYMENT_PENDING");
	}

	@Test
	@Story("Retry on timeout")
	@DisplayName("A response slower than the read timeout is retried with the same key")
	void readTimeoutThenCreatedRetriesWithSameKey() {
		String id = createHeld();
		wireMock.stubFor(charge().inScenario("slow").whenScenarioStateIs(Scenario.STARTED)
				.willReturn(created().withFixedDelay(1000)).willSetStateTo("fast"));
		wireMock.stubFor(charge().inScenario("slow").whenScenarioStateIs("fast").willReturn(created()));

		pay(id, VALID, KEY).then().statusCode(201);

		wireMock.verify(2, charges());
		// A changed key after a timeout is exactly how a double charge happens.
		wireMock.verify(2, charges().withHeader("Idempotency-Key", WireMock.equalTo(KEY)));
		assertThat(SLEEPS).containsExactly(Duration.ofMillis(200));
	}

	@Test
	@Story("No retry on 4xx")
	@DisplayName("402 is a decline: one request, and the same key replays the 402")
	void declinedIsNotRetriedAndReplays() {
		String id = createHeld();
		wireMock.stubFor(charge().willReturn(WireMock.aResponse().withStatus(402)));

		Response first = pay(id, VALID, KEY);
		Response second = pay(id, VALID, KEY);

		first.then().statusCode(402).body("code", equalTo("PAYMENT_DECLINED"))
				.header("Idempotent-Replayed", nullValue());
		second.then().statusCode(402).header("Idempotent-Replayed", equalTo("true"));
		assertThat(second.asString()).isEqualTo(first.asString());
		wireMock.verify(1, charges());
		assertThat(SLEEPS).isEmpty();
		assertStatus(id, "HELD");
	}

	@Test
	@Story("Retries exhausted")
	@DisplayName("500 three times: 503, booking stays PAYMENT_PENDING; the same key later resumes to 201")
	void exhaustedRetriesReturn503ThenSameKeyResumes() {
		String id = createHeld();
		wireMock.stubFor(charge().willReturn(WireMock.serverError()));

		pay(id, VALID, KEY).then().statusCode(503).body("code", equalTo("GATEWAY_UNAVAILABLE"));

		wireMock.verify(3, charges());
		assertThat(SLEEPS).containsExactly(Duration.ofMillis(200), Duration.ofMillis(400));
		assertStatus(id, "PAYMENT_PENDING");

		wireMock.stubFor(charge().willReturn(created()));
		pay(id, VALID, KEY).then().statusCode(201).header("Idempotent-Replayed", nullValue());

		wireMock.verify(4, charges());
		wireMock.verify(4, charges().withHeader("Idempotency-Key", WireMock.equalTo(KEY)));
		assertStatus(id, "PAYMENT_PENDING");
	}

	private static MappingBuilder charge() {
		return WireMock.post(WireMock.urlEqualTo("/v1/charges"));
	}

	private static RequestPatternBuilder charges() {
		return WireMock.postRequestedFor(WireMock.urlEqualTo("/v1/charges"));
	}

	private static ResponseDefinitionBuilder created() {
		return WireMock.aResponse().withStatus(201).withHeader("Content-Type", "application/json")
				.withBody("""
						{"gatewayRef": "gw-1", "status": "PENDING"}""");
	}
}
