package io.github.mithunp999.reservationlab.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/** API tests against the @Primary {@link FakePaymentGateway}. */
@Import({ TestClockConfig.class, TestGatewayConfig.class })
public abstract class ApiTestBase extends ApiTestSupport {

	@Autowired
	protected FakePaymentGateway gateway;

	@BeforeEach
	void resetGateway() {
		gateway.reset();
	}
}
