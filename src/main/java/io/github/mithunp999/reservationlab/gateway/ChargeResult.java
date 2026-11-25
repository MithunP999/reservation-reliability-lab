package io.github.mithunp999.reservationlab.gateway;

public record ChargeResult(String gatewayRef, Status status) {

	public enum Status {
		PENDING, DECLINED
	}
}
