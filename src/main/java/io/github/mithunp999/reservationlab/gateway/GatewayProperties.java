package io.github.mithunp999.reservationlab.gateway;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("gateway")
public record GatewayProperties(String baseUrl, @DefaultValue("1s") Duration connectTimeout,
		@DefaultValue("2s") Duration readTimeout, @DefaultValue("3") int maxAttempts) {
}
