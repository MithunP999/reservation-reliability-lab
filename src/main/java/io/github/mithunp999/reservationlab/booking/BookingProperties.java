package io.github.mithunp999.reservationlab.booking;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("booking")
public record BookingProperties(@DefaultValue("10m") Duration holdDuration) {
}
