package com.resumescreener.resumescreener.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingInterceptor.class);

    private static final int REQUESTS_PER_MINUTE = 60;
    private static final long WINDOW_SIZE_MILLIS = 60_000; // 1 minute

    private final Map<String, RateLimit> rateLimitMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) throws IOException {

        // Get client identifier (IP address)
        String clientId = getClientIdentifier(request);

        // Get or create rate limit for this client
        RateLimit rateLimit = rateLimitMap.computeIfAbsent(
                clientId,
                k -> new RateLimit(REQUESTS_PER_MINUTE, WINDOW_SIZE_MILLIS)
        );

        // Check if rate limit exceeded
        if (rateLimit.isExceeded()) {
            logger.warn("Rate limit exceeded for client: {}", clientId);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");

            String jsonResponse = "{\"error\": \"Rate limit exceeded. Maximum 60 requests per minute.\"}";
            response.getWriter().write(jsonResponse);

            return false;
        }

        // Record this request
        rateLimit.recordRequest();

        // Add rate limit info to response headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(REQUESTS_PER_MINUTE));
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(REQUESTS_PER_MINUTE - rateLimit.getRequestCount()));

        return true;
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get real client IP (considering proxies)
        String clientIp = request.getHeader("X-Forwarded-For");

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("X-Real-IP");
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        // If multiple IPs in X-Forwarded-For, use the first one
        if (clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }

        return clientIp;
    }

    // Inner class for tracking rate limits
    private static class RateLimit {
        private final int maxRequests;
        private final long windowSizeMillis;
        private long windowStartTime;
        private int requestCount;

        RateLimit(int maxRequests, long windowSizeMillis) {
            this.maxRequests = maxRequests;
            this.windowSizeMillis = windowSizeMillis;
            this.windowStartTime = System.currentTimeMillis();
            this.requestCount = 0;
        }

        synchronized boolean isExceeded() {
            long now = System.currentTimeMillis();

            // Reset window if expired
            if (now - windowStartTime >= windowSizeMillis) {
                windowStartTime = now;
                requestCount = 0;
                return false;
            }

            return requestCount >= maxRequests;
        }

        synchronized void recordRequest() {
            requestCount++;
        }

        synchronized int getRequestCount() {
            return requestCount;
        }
    }
}
