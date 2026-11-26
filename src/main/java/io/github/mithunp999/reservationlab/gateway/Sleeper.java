package io.github.mithunp999.reservationlab.gateway;

import java.time.Duration;

/** Waits between retries. Production sleeps for real; tests inject a no-op. */
@FunctionalInterface
public interface Sleeper {

	void sleep(Duration duration) throws InterruptedException;
}
