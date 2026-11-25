package io.github.mithunp999.reservationlab.gateway;

public interface PaymentGateway {

	ChargeResult charge(ChargeRequest request);
}
