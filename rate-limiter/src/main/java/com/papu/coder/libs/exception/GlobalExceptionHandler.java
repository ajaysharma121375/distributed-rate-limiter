package com.papu.coder.libs.exception;

import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.papu.coder.libs.config.RateLimitEnabledCondition;

@RestControllerAdvice
@Conditional(RateLimitEnabledCondition.class)
public class GlobalExceptionHandler {

	@ExceptionHandler(RateLimitLibException.class)
	public ResponseEntity<String> handlerRateLimitLibException(RateLimitLibException exception){
		return ResponseEntity.status(exception.getStatus()).body(exception.getMessage());
	}
}
