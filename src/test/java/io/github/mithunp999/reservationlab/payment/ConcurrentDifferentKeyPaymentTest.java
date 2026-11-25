package io.github.mithunp999.reservationlab.payment;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import io.github.mithunp999.reservationlab.support.ApiTestBase;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;

@Feature("Idempotency")
@DisplayName("Concurrent payments with different keys")
class ConcurrentDifferentKeyPaymentTest extends ApiTestBase {

	private static final int THREADS = 10;
	private static final String VALID = """
			{"amountMinor": 150000, "currency": "INR"}""";

	@RepeatedTest(20)
	@Story("Concurrency")
	@DisplayName("10 different keys at once: one 201, nine 409 ILLEGAL_TRANSITION, one charge")
	void onlyOneOfTenConcurrentKeysWins() throws Exception {
		String id = createHeld();
		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		CountDownLatch ready = new CountDownLatch(THREADS);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<Response>> futures = new ArrayList<>();
			for (int i = 0; i < THREADS; i++) {
				String key = "k-" + i;
				futures.add(pool.submit(() -> {
					ready.countDown();
					assertThat(start.await(10, SECONDS)).isTrue();
					return pay(id, VALID, key);
				}));
			}
			// Release all requests together, only once every thread is parked on the latch.
			assertThat(ready.await(10, SECONDS)).isTrue();
			start.countDown();

			List<Response> responses = new ArrayList<>();
			for (Future<Response> f : futures) {
				responses.add(f.get(10, SECONDS));
			}

			List<Integer> expected = new ArrayList<>(Collections.nCopies(THREADS - 1, 409));
			expected.add(201);
			assertThat(responses).extracting(Response::statusCode).containsExactlyInAnyOrderElementsOf(expected);
			assertThat(responses).filteredOn(r -> r.statusCode() == 409)
					.extracting(r -> r.<String>path("code")).containsOnly("ILLEGAL_TRANSITION");
			assertThat(gateway.calls()).hasSize(1);
			assertStatus(id, "PAYMENT_PENDING");
		} finally {
			pool.shutdownNow();
			assertThat(pool.awaitTermination(5, SECONDS)).isTrue();
		}
	}
}
