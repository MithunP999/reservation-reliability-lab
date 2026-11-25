package io.github.mithunp999.reservationlab.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class Problems {

	private Problems() {
	}

	/** RFC 9457 body with the {@code code} property every error in this API carries. */
	public static ProblemDetail of(HttpStatus status, String code, String detail) {
		ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
		p.setProperty("code", code);
		return p;
	}
}
