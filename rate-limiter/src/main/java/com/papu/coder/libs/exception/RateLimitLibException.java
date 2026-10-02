package com.papu.coder.libs.exception;

import org.springframework.http.HttpStatus;

public class RateLimitLibException extends RuntimeException {

	private final HttpStatus status;

	public RateLimitLibException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}
	
	public RateLimitLibException(HttpStatus status, String message,Throwable cause) {
		super(message,cause);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
	
	
	
	
}
