package com.papu.coder.libs.service;

import java.util.Collections;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;

import com.papu.coder.libs.exception.RateLimitLibException;

public class RedisRateLimiterService {
	private static final String CLASS_NAME = RedisRateLimiterService.class.getCanonicalName();
//	@Autowired
//	BFLLoggerUtilExt logger;
	private final StringRedisTemplate redisTemplate;
	private final RedisScript<Long> rateLimitScript;

	public RedisRateLimiterService(StringRedisTemplate redisTemplate, RedisScript<Long> rateLimitScript) {
		this.redisTemplate = redisTemplate;
		this.rateLimitScript = rateLimitScript;
	}

	public boolean allow(String key, int limit, long windowSeconds, boolean failOpenOnError) {
		try {
			Long result = redisTemplate.execute(rateLimitScript, Collections.singletonList(key), String.valueOf(limit),
					String.valueOf(windowSeconds));
			return result != null && result == 1;
		} catch (Exception ex) {
//			logger.error(CLASS_NAME, BFLLoggerComponent.SERVICE, "Rate limiter Redis call failed for key [" + key + "]", ex);
			if (failOpenOnError) {
//				logger.warn(CLASS_NAME, BFLLoggerComponent.SERVICE, "Failing open for key [" + key + "] due to rate limiter unavailability");
				return true;
			}
			throw new RateLimitLibException(HttpStatus.SERVICE_UNAVAILABLE, "Rate limiter service is unavailable", ex);
		}
	}
}
