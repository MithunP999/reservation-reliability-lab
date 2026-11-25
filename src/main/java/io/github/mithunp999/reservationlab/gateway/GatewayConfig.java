package io.github.mithunp999.reservationlab.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

	/** Placeholder until slice 7a adds the real client. */
	@Bean
	PaymentGateway paymentGateway() {
		return request -> {
			throw new IllegalStateException("gateway not configured");
		};
	}
}
