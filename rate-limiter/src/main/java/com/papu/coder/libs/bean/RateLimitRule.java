package com.papu.coder.libs.bean;

import java.util.List;

public class RateLimitRule {

    private int limit;

    private long windowSeconds;

    private List<RateLimitKey> keyType;

    private String keyName;
    
    private String scope;

    private String algorithm;
    
    private boolean failOpenOnError = false;

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public long getWindowSeconds() {
		return windowSeconds;
	}

	public void setWindowSeconds(long windowSeconds) {
		this.windowSeconds = windowSeconds;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public List<RateLimitKey> getKeyType() {
		return keyType;
	}

	public void setKeyType(List<RateLimitKey> keyType) {
		this.keyType = keyType;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	public boolean isFailOpenOnError() {
		return failOpenOnError;
	}

	public void setFailOpenOnError(boolean failOpenOnError) {
		this.failOpenOnError = failOpenOnError;
	}

    
}
