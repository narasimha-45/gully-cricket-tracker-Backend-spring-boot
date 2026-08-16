package com.gullycricket.backend.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbQueryTimer {

    private final MeterRegistry meterRegistry;

    @Value("${app.performance.slow-query-ms:150}")
    private long slowQueryMs;

    public <T> T record(String queryName, Supplier<T> query) {
        long started = System.nanoTime();
        try {
            return query.get();
        } finally {
            long elapsedNanos = System.nanoTime() - started;
            Timer.builder("gully.db.read")
                    .description("Read-model JDBC latency including connection acquisition, SQL execution, and row mapping")
                    .tag("query", queryName)
                    .register(meterRegistry)
                    .record(elapsedNanos, TimeUnit.NANOSECONDS);

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
            if (elapsedMs >= slowQueryMs) {
                log.warn("Slow JDBC read query={} jdbcTotalMs={}", queryName, elapsedMs);
            } else {
                log.debug("JDBC read timing query={} jdbcTotalMs={}", queryName, elapsedMs);
            }
        }
    }
}
