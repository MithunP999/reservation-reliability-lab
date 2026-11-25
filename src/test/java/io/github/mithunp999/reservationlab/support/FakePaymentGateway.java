package io.github.mithunp999.reservationlab.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.mithunp999.reservationlab.gateway.ChargeRequest;
import io.github.mithunp999.reservationlab.gateway.ChargeResult;
import io.github.mithunp999.reservationlab.gateway.PaymentGateway;

/** Records every charge. Accepts (PENDING) unless told to decline or to fail. */
public class FakePaymentGateway implements PaymentGateway {

	private final List<ChargeRequest> calls = new CopyOnWriteArrayList<>();
	private final AtomicInteger failuresLeft = new AtomicInteger();
	private volatile boolean decline;

	@Override
	public ChargeResult charge(ChargeRequest request) {
		calls.add(request);
		if (failuresLeft.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
			throw new IllegalStateException("simulated gateway failure");
		}
		return new ChargeResult("fake-ref-" + calls.size(),
				decline ? ChargeResult.Status.DECLINED : ChargeResult.Status.PENDING);
	}

	public void declineCharges() {
		decline = true;
	}

	/** The next {@code n} charges are recorded, then throw. */
	public void failNext(int n) {
		failuresLeft.set(n);
	}

	public List<ChargeRequest> calls() {
		return List.copyOf(calls);
	}

	public void reset() {
		calls.clear();
		failuresLeft.set(0);
		decline = false;
	}
}
