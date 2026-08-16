package com.gullycricket.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Adds an application processing time to every response and highlights genuinely
 * slow backend requests. Browser Network time also contains network/proxy time;
 * X-Response-Time-Ms makes it easy to separate that from Spring/DB time.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestTimingFilter extends OncePerRequestFilter {

    @Value("${app.performance.slow-request-ms:300}")
    private long slowRequestMs;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            response.setHeader("X-Response-Time-Ms", Long.toString(elapsedMs));
            response.addHeader("Server-Timing", "app;dur=" + elapsedMs);

            if (elapsedMs >= slowRequestMs && !isActuator(request)) {
                log.warn("Slow request method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
            } else {
                log.debug("Request timing method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
            }
        }
    }

    private boolean isActuator(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
