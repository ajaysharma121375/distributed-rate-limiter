package com.papu.coder.libs.bean;

import com.papu.coder.libs.enums.RateLimitKeyLocation;

public class RateLimitKey {


    private RateLimitKeyLocation location;

    private String name;

	public RateLimitKeyLocation getLocation() {
		return location;
	}

	public void setLocation(RateLimitKeyLocation location) {
		this.location = location;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    // getters and setters
}