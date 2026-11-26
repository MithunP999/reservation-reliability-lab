package io.github.mithunp999.reservationlab.gateway;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * POST /v1/charges with the client's Idempotency-Key on every attempt. 201 -> PENDING, 402 -> DECLINED. Anything
 * else throws once retries are exhausted, and PaymentService turns that into a retryable 503.
 */
@Component
public class HttpPaymentGateway implements PaymentGateway {

	private final RestClient restClient;
	private final RetryPolicy retryPolicy;

	public HttpPaymentGateway(RestClient paymentGatewayRestClient, RetryPolicy retryPolicy) {
		this.restClient = paymentGatewayRestClient;
		this.retryPolicy = retryPolicy;
	}

	@Override
	public ChargeResult charge(ChargeRequest request) {
		return retryPolicy.execute(() -> post(request));
	}

	private ChargeResult post(ChargeRequest request) {
		try {
			return restClient.post()
					.uri("/v1/charges")
					.header("Idempotency-Key", request.idempotencyKey())
					.body(new ChargeBody(request.bookingId(), request.amountMinor(), request.currency()))
					.retrieve()
					.body(ChargeResult.class);
		}
		catch (HttpClientErrorException e) {
			// A decline is an answer, not a failure: return it so it is never retried.
			if (e.getStatusCode().isSameCodeAs(HttpStatus.PAYMENT_REQUIRED)) {
				return new ChargeResult(null, ChargeResult.Status.DECLINED);
			}
			throw e;
		}
	}

	record ChargeBody(UUID reference, long amountMinor, String currency) {
	}
}
