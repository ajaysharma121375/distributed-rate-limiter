package com.papu.coder.libs.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.papu.coder.libs.bean.RateLimitRule;
import com.papu.coder.libs.exception.RateLimitLibException;

@ConfigurationProperties(prefix = "rate-limit")
@Conditional(RateLimitEnabledCondition.class)
@Component
public class RateLimitProperties {

    private Map<String, RateLimitRule> rules = new HashMap<>();

    public RateLimitRule getRule(String ruleName) throws Exception {
    	RateLimitRule rateLimitRule = rules.get(ruleName);
    	if (null == rateLimitRule ) {
    		throw new RateLimitLibException(HttpStatus.INTERNAL_SERVER_ERROR,"Rule not found");
			
		}
        return rateLimitRule;
    }

    public void setRules(Map<String, RateLimitRule> rules) {
        this.rules = rules;
    }
}