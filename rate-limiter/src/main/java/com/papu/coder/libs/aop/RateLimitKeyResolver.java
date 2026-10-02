package com.papu.coder.libs.aop;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import com.papu.coder.libs.bean.RateLimitKey;
import com.papu.coder.libs.bean.RateLimitRule;
import com.papu.coder.libs.exception.RateLimitLibException;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RateLimitKeyResolver {

	// avoids re-running bean introspection on every request for the same (class, field) pair
	private static final Map<String, Optional<Method>> GETTER_CACHE = new ConcurrentHashMap<>();

	private final HttpServletRequest request;

	public RateLimitKeyResolver(HttpServletRequest request) {
		this.request = request;
	}

	public String resolve(RateLimitRule rule, ProceedingJoinPoint joinPoint) {

		String keyName = rule.getKeyName();

		if (keyName == null || keyName.isBlank()) {
			throw new RateLimitLibException(HttpStatus.INTERNAL_SERVER_ERROR, "Rate limit rule is missing keyName");
		}

		List<String> keyValues = new ArrayList<>();
		// keyName is included so distinct rules never share a Redis bucket for the same resolved value
		keyValues.add(keyName);

		for (RateLimitKey key : rule.getKeyType()) {

			String value = resolveKey(key, request, joinPoint.getArgs());

			if (value == null || value.isBlank()) {
				throw new RateLimitLibException(HttpStatus.BAD_REQUEST, "Unable to resolve required rate limit key: " + key.getName());
			}

			keyValues.add(value);
		}

		return "rate_limit:" + String.join(":", keyValues);
	}

	private String resolveKey(RateLimitKey key, HttpServletRequest request, Object[] arguments) {

		switch (key.getLocation()) {

		case HEADER:
			return request.getHeader(key.getName().toLowerCase());

		case PARAM:
			return request.getParameter(key.getName());

		case PATH:
			return getPathVariable(key.getName(), request);

		case BODY:
			return getBodyValue(key.getName(), arguments);

		default:
			throw new IllegalArgumentException("Unsupported location: " + key.getLocation());
		}
	}

	private String getPathVariable(String name, HttpServletRequest request) {

		@SuppressWarnings("unchecked")
		Map<String, String> pathVariables = (Map<String, String>) request
				.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

		if (pathVariables == null) {
			return null;
		}

		return pathVariables.get(name);
	}

	private String getBodyValue(String fieldPath, Object[] arguments) {

		for (Object argument : arguments) {

			if (argument == null) {
				continue;
			}

			Object value = getNestedProperty(argument, fieldPath);

			if (value != null) {
				return value.toString();
			}
		}

		return null;
	}

	private Object getNestedProperty(Object object, String fieldPath) {

		Object currentObject = object;

		String[] fields = fieldPath.split("\\.");

		for (String field : fields) {

			if (currentObject == null) {
				return null;
			}

			Method getter = getCachedGetter(currentObject.getClass(), field);

			if (getter == null) {
				return null;
			}

			try {
				currentObject = getter.invoke(currentObject);
			} catch (Exception exception) {
				throw new IllegalStateException("Unable to resolve body field: " + fieldPath, exception);
			}
		}

		return currentObject;
	}

	private Method getCachedGetter(Class<?> type, String field) {

		String cacheKey = type.getName() + "#" + field;

		return GETTER_CACHE.computeIfAbsent(cacheKey, key -> {
			try {
				return Optional.ofNullable(new PropertyDescriptor(field, type).getReadMethod());
			} catch (IntrospectionException exception) {
				return Optional.empty();
			}
		}).orElse(null);
	}
}