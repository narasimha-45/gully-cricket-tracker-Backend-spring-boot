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
 * Adds application timing to every response. Read-model JDBC time is reported
 * separately so a slow request can be split into DB work vs the rest of Spring,
 * mapping, serialization and socket write time.
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
            long dbMs = numberAttribute(request, DbQueryTimer.REQUEST_DB_TIME_MS);
            long dbQueryCount = numberAttribute(request, DbQueryTimer.REQUEST_DB_QUERY_COUNT);

            response.setHeader("X-Response-Time-Ms", Long.toString(elapsedMs));
            response.setHeader("X-DB-Time-Ms", Long.toString(dbMs));
            response.setHeader("X-DB-Query-Count", Long.toString(dbQueryCount));
            response.setHeader("Server-Timing", "app;dur=" + elapsedMs + ", db;dur=" + dbMs);

            if (elapsedMs >= slowRequestMs && !isActuator(request)) {
                log.warn("Slow request method={} path={} status={} durationMs={} jdbcReadMs={} jdbcReadQueries={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs, dbMs, dbQueryCount);
            } else {
                log.debug("Request timing method={} path={} status={} durationMs={} jdbcReadMs={} jdbcReadQueries={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs, dbMs, dbQueryCount);
            }
        }
    }

    private long numberAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private boolean isActuator(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
