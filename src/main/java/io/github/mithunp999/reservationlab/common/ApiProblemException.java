package io.github.mithunp999.reservationlab.common;

import org.springframework.http.HttpStatus;

/** An error response that is not stored as an idempotent result. */
public class ApiProblemException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public ApiProblemException(HttpStatus status, String code, String detail) {
		super(detail);
		this.status = status;
		this.code = code;
	}

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}
}
