package io.github.mithunp999.reservationlab.gateway;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;


import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

/** Up to {@code maxAttempts} tries, backoff 200ms, 400ms, ... The caller keeps the same Idempotency-Key. */
@Component
public class RetryPolicy {

	private static final Duration FIRST_BACKOFF = Duration.ofMillis(200);

	private final int maxAttempts;
	private final Sleeper sleeper;

	public RetryPolicy(GatewayProperties properties, Sleeper sleeper) {
		this.maxAttempts = properties.maxAttempts();
		this.sleeper = sleeper;
	}

	public <T> T execute(Supplier<T> call) {
		for (int attempt = 1;; attempt++) {
			try {
				return call.get();
			}
			catch (RuntimeException e) {
				if (attempt >= maxAttempts || !isRetryable(e)) {
					throw e;
				}
				try {
					sleeper.sleep(FIRST_BACKOFF.multipliedBy(1L << (attempt - 1)));
				}
				catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					throw e;
				}
			}
		}
	}

	// retry on 5xx, or when any cause in the exception chain is an IOException
	// (SocketTimeoutException, HttpTimeoutException, connection refused). Never 4xx, especially not 402.
	static boolean isRetryable(Throwable e) {
		if(e instanceof HttpServerErrorException) {
			return true;
		}
		for(Throwable cause = e; cause != null; cause = cause.getCause()) {
			if(cause instanceof IOException) {
				return true;
			}
		}		
		return false;
	}
}
