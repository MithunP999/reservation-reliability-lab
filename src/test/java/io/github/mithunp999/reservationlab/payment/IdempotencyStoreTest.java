package io.github.mithunp999.reservationlab.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import io.github.mithunp999.reservationlab.payment.IdempotencyStore.Claim;
import io.github.mithunp999.reservationlab.payment.IdempotencyStore.Decision;
import io.github.mithunp999.reservationlab.payment.IdempotencyStore.Fingerprint;

class IdempotencyStoreTest {

	private final IdempotencyStore store = new IdempotencyStore();
	private final Fingerprint fp = new Fingerprint(UUID.randomUUID(), 100, "INR");
	private final UUID paymentId = UUID.randomUUID();

	@Test
	void newKeyIsClaimedAndBlocksTheSameKeyWhileInProgress() {
		assertThat(store.claim("k", fp, paymentId).decision()).isEqualTo(Decision.NEW);
		assertThat(store.claim("k", fp, UUID.randomUUID()).decision()).isEqualTo(Decision.IN_PROGRESS);
	}

	@Test
	void differentFingerprintIsReusedInAnyState() {
		Claim owned = store.claim("k", fp, paymentId);
		Fingerprint other = new Fingerprint(fp.bookingId(), 200, "INR");
		assertThat(store.claim("k", other, UUID.randomUUID()).decision()).isEqualTo(Decision.REUSED);

		store.complete("k", owned.entry(), HttpStatus.CREATED, "body");
		assertThat(store.claim("k", other, UUID.randomUUID()).decision()).isEqualTo(Decision.REUSED);
	}

	@Test
	void completedIsReplayedWithTheSameResult() {
		store.complete("k", store.claim("k", fp, paymentId).entry(), HttpStatus.CREATED, "body");

		Claim replay = store.claim("k", fp, UUID.randomUUID());
		assertThat(replay.decision()).isEqualTo(Decision.REPLAY);
		assertThat(replay.entry().paymentId()).isEqualTo(paymentId);
		assertThat(replay.entry().storedStatus()).isEqualTo(HttpStatus.CREATED);
		assertThat(replay.entry().storedBody()).isEqualTo("body");
	}

	@Test
	void retryableResumesOnceWithTheSamePaymentId() {
		store.retryable("k", store.claim("k", fp, paymentId).entry());

		Claim resume = store.claim("k", fp, UUID.randomUUID());
		assertThat(resume.decision()).isEqualTo(Decision.RESUME);
		assertThat(resume.entry().paymentId()).isEqualTo(paymentId);
		// The resume moved it back to IN_PROGRESS, so a second retry cannot resume in parallel.
		assertThat(store.claim("k", fp, UUID.randomUUID()).decision()).isEqualTo(Decision.IN_PROGRESS);
	}

	@Test
	void abandonedKeyCanBeClaimedAgain() {
		store.abandon("k", store.claim("k", fp, paymentId).entry());
		assertThat(store.claim("k", fp, UUID.randomUUID()).decision()).isEqualTo(Decision.NEW);
	}
}
