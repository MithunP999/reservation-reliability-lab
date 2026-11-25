package io.github.mithunp999.reservationlab.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestGatewayConfig {

	@Bean
	@Primary
	FakePaymentGateway fakePaymentGateway() {
		return new FakePaymentGateway();
	}
}
