package com.gullycricket.backend.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbQueryTimer {

    static final String REQUEST_DB_TIME_MS = DbQueryTimer.class.getName() + ".dbTimeMs";
    static final String REQUEST_DB_QUERY_COUNT = DbQueryTimer.class.getName() + ".dbQueryCount";

    private final MeterRegistry meterRegistry;

    @Value("${app.performance.slow-query-ms:150}")
    private long slowQueryMs;

    public <T> T record(String queryName, Supplier<T> query) {
        long started = System.nanoTime();
        try {
            return query.get();
        } finally {
            long elapsedNanos = System.nanoTime() - started;
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

            Timer.builder("gully.db.read")
                    .description("Read-model JDBC latency including connection acquisition, SQL execution, and row mapping")
                    .tag("query", queryName)
                    .register(meterRegistry)
                    .record(elapsedNanos, TimeUnit.NANOSECONDS);

            addRequestTiming(elapsedMs);

            if (elapsedMs >= slowQueryMs) {
                log.warn("Slow JDBC read query={} jdbcTotalMs={}", queryName, elapsedMs);
            } else {
                log.debug("JDBC read timing query={} jdbcTotalMs={}", queryName, elapsedMs);
            }
        }
    }

    private void addRequestTiming(long elapsedMs) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        Object currentTotal = request.getAttribute(REQUEST_DB_TIME_MS);
        long totalMs = currentTotal instanceof Number number ? number.longValue() : 0L;
        request.setAttribute(REQUEST_DB_TIME_MS, totalMs + elapsedMs);

        Object currentCount = request.getAttribute(REQUEST_DB_QUERY_COUNT);
        int count = currentCount instanceof Number number ? number.intValue() : 0;
        request.setAttribute(REQUEST_DB_QUERY_COUNT, count + 1);
    }
}
