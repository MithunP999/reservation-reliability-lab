package io.github.mithunp999.reservationlab.payment;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
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
@DisplayName ("Concurrent payments with the same key")
class ConcurrentDoublePaymentTest extends ApiTestBase {
    private static final int THREADS = 10;
    private static final String VALID = """
            {"amountMinor": 150000, "currency": "INR"}""";

    @RepeatedTest(20)
    @Story("Concurrency")
    @DisplayName("10 requests with the same key at once: one charge, the rest replay or see IN_PROGRESS")
    void  sameKeyConcurrentlyChargesOnce() throws Exception {
        String id = createHeld();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Response>> futures = new ArrayList<>();
        try{

            
            for(int i=0; i<THREADS; i++){
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    assertThat(start.await(10,SECONDS)).isTrue();
                    return pay(id, VALID, "same-key");    
            }));
        }
        assertThat(ready.await(10, SECONDS)).isTrue(); // Wait for all threads to be ready
        start.countDown(); // Release all threads to start the payment

        List<Response> responses = new ArrayList<>();
        for (Future<Response> future : futures) {
            responses.add(future.get(10, SECONDS)); // Wait for each thread to complete and collect the response
        }

        assertThat(gateway.calls()).hasSize(1); // the money check, always measured

        List<Response> original=responses.stream().filter (r -> r.statusCode() == 201 && r.header("Idempotent-Replayed") == null).toList();
        assertThat(original).hasSize(1);

        //Every other response either a replay of that 201 or IN_PROGRESS, depending on timing.
        for(Response r: responses){
            if(r == original.get(0)) {continue;}
            if(r.statusCode() == 201){
                assertThat(r.header("Idempotent-Replayed")).isEqualTo("true");
                assertThat(r.asString()).isEqualTo(original.get(0).asString());
            } else {
                assertThat(r.statusCode()).isEqualTo(409);
                assertThat(r.<String>path("code")).isEqualTo("IDEMPOTENCY_IN_PROGRESS");
            }
        }
            
        

        assertStatus(id, "PAYMENT_PENDING");
                } finally {
                        pool.shutdownNow();
                        assertThat(pool.awaitTermination(5, SECONDS)).isTrue();
    }
}
}

