package com.papu.coder.libs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
@Conditional(RateLimitEnabledCondition.class)
public class RedisRateLimitConfig {
	
	@Bean
	public RedisScript<Long> rateLimitScript(){
		return RedisScript.of(
				new ClassPathResource("Scripts/rate-limit.lua"),Long.class);
	}
}
