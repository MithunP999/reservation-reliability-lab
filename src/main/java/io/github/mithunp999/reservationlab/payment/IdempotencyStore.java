package io.github.mithunp999.reservationlab.payment;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Idempotency-Key results, in memory. Every state change is one atomic map operation on the key: {@code compute}
 * decides what a request may do, and only the request that moved an entry to IN_PROGRESS changes it afterwards
 * ({@code replace(key, owned, next)}). There is no get-then-put.
 */
// ponytail: keys never expire and the map grows without bound; add TTL cleanup when this is persisted.
@Component
public class IdempotencyStore {

	public enum State {
		IN_PROGRESS, RETRYABLE, COMPLETED
	}

	public enum Decision {
		/** New key: the caller owns it and must claim the booking. */
		NEW,
		/** A previous attempt failed at the gateway: the caller owns it again and already holds the booking. */
		RESUME,
		/** A result is stored: return it. */
		REPLAY,
		/** Another request with this key is running. */
		IN_PROGRESS,
		/** The key was used for a different request. */
		REUSED
	}

	/** Record equality is enough here: exact, in memory, no collisions. Hash it only once keys are persisted. */
	public record Fingerprint(UUID bookingId, long amountMinor, String currency) {
	}

	public record Entry(Fingerprint fingerprint, State state, UUID paymentId, HttpStatus storedStatus,
			Object storedBody) {

		Entry withState(State next) {
			return new Entry(fingerprint, next, paymentId, storedStatus, storedBody);
		}
	}

	public record Claim(Decision decision, Entry entry) {
	}

	private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

	public Claim claim(String key, Fingerprint fingerprint, UUID newPaymentId) {
		Decision[] decision = new Decision[1];
		Entry entry = entries.compute(key, (k, existing) -> {
			if (existing == null) {
				decision[0] = Decision.NEW;
				return new Entry(fingerprint, State.IN_PROGRESS, newPaymentId, null, null);
			}
			if (!existing.fingerprint().equals(fingerprint)) {
				decision[0] = Decision.REUSED;
				return existing;
			}
			return switch (existing.state()) {
				case IN_PROGRESS -> {
					decision[0] = Decision.IN_PROGRESS;
					yield existing;
				}
				case COMPLETED -> {
					decision[0] = Decision.REPLAY;
					yield existing;
				}
				case RETRYABLE -> {
					decision[0] = Decision.RESUME;
					yield existing.withState(State.IN_PROGRESS);
				}
			};
		});
		return new Claim(decision[0], entry);
	}

	public void complete(String key, Entry owned, HttpStatus status, Object body) {
		swap(key, owned, new Entry(owned.fingerprint(), State.COMPLETED, owned.paymentId(), status, body));
	}

	public void retryable(String key, Entry owned) {
		swap(key, owned, owned.withState(State.RETRYABLE));
	}

	/** Frees a key after an unexpected error before the booking was claimed, so a bug cannot lock it forever. */
	public void abandon(String key, Entry owned) {
		entries.remove(key, owned);
	}

	private void swap(String key, Entry owned, Entry next) {
		if (!entries.replace(key, owned, next)) {
			throw new IllegalStateException("Idempotency entry " + key + " changed while owned");
		}
	}

	/** For test reset only. */
	public void clear() {
		entries.clear();
	}
}
