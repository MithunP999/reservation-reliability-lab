package io.github.mithunp999.reservationlab.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {

	private final ZoneId zone;
	private volatile Instant now;

	public MutableClock(Instant start) {
		this(start, ZoneOffset.UTC);
	}

	private MutableClock(Instant start, ZoneId zone) {
		this.now = start;
		this.zone = zone;
	}

	public void reset(Instant instant) {
		now = instant;
	}

	public void advance(Duration d) {
		now = now.plus(d);
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId newZone) {
		return new MutableClock(now, newZone);
	}

	@Override
	public Instant instant() {
		return now;
	}
}
