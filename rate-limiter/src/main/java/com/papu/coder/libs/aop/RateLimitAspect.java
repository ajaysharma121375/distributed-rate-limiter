package com.papu.coder.libs.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.papu.coder.libs.bean.RateLimitRule;
import com.papu.coder.libs.config.RateLimitEnabledCondition;
import com.papu.coder.libs.config.RateLimitProperties;
import com.papu.coder.libs.exception.RateLimitLibException;
import com.papu.coder.libs.service.RedisRateLimiterService;

@Aspect
@Component
@Conditional(RateLimitEnabledCondition.class)
public class RateLimitAspect {

	private final RateLimitProperties rateLimitProperties;
	private final RateLimitKeyResolver keyResolver;
	private final RedisRateLimiterService rateLimiter;

	public RateLimitAspect(RateLimitProperties rateLimitProperties, RateLimitKeyResolver keyResolver,
			RedisRateLimiterService rateLimiter) {

		this.rateLimitProperties = rateLimitProperties;
		this.keyResolver = keyResolver;
		this.rateLimiter = rateLimiter;
	}

	@Around("@annotation(RateLimited)")
	public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {

		MethodSignature signature = (MethodSignature) joinPoint.getSignature();

		RateLimited rateLimited = signature.getMethod().getAnnotation(RateLimited.class);

		String ruleName = rateLimited.rules();
		RateLimitRule rule = rateLimitProperties.getRule(ruleName);

		long windowSeconds = rule.getWindowSeconds();
		int limit = rule.getLimit();

		String key = keyResolver.resolve(rule, joinPoint);

		boolean allowed = rateLimiter.allow(key, limit, windowSeconds,rule.isFailOpenOnError());

		if (!allowed) {
			throw new RateLimitLibException(HttpStatus.TOO_MANY_REQUESTS, "To many request");
		}

		return joinPoint.proceed();
	}
}