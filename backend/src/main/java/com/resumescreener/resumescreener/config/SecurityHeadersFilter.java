package com.resumescreener.resumescreener.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        // Prevent clickjacking attacks
        response.setHeader("X-Frame-Options", "DENY");

        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Enable XSS protection in older browsers
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Content Security Policy - restrictive by default
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data:; " +
                "font-src 'self'");

        // HSTS - Forces HTTPS (365 days)
        response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains");

        // Referrer Policy - no referrer info sent
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions Policy - disable unnecessary features
        response.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=()");

        filterChain.doFilter(request, response);
    }
}
