package io.github.mithunp999.reservationlab.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.mithunp999.reservationlab.gateway.ChargeRequest;
import io.github.mithunp999.reservationlab.gateway.ChargeResult;
import io.github.mithunp999.reservationlab.gateway.PaymentGateway;

/** Records every charge. Accepts (PENDING) unless told to decline. */
public class FakePaymentGateway implements PaymentGateway {

	private final List<ChargeRequest> calls = new CopyOnWriteArrayList<>();
	private volatile boolean decline;

	@Override
	public ChargeResult charge(ChargeRequest request) {
		calls.add(request);
		return new ChargeResult("fake-ref-" + calls.size(),
				decline ? ChargeResult.Status.DECLINED : ChargeResult.Status.PENDING);
	}

	public void declineCharges() {
		decline = true;
	}

	public List<ChargeRequest> calls() {
		return List.copyOf(calls);
	}

	public void reset() {
		calls.clear();
		decline = false;
	}
}
