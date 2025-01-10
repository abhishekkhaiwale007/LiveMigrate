package com.livemigrate.config;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
@RequiredArgsConstructor
public class MetricsConfiguration {

    /**
     * Creates a MeterRegistry that will collect various metrics about the migration process.
     * These metrics will help us monitor the health and performance of the system.
     */
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    /**
     * Creates a Timer metric to track the duration of record transformations.
     * This helps us monitor the performance of our migration process.
     */
    @Bean
    public Timer migrationTimer(MeterRegistry registry) {
        return Timer.builder("migration.record.transform")
                .description("Time taken to transform records from V1 to V2")
                .tag("type", "record_transformation")
                .publishPercentiles(0.5, 0.95, 0.99) // Track median and percentiles
                .register(registry);
    }

    /**
     * Creates a Counter metric to track the total number of processed records.
     * This helps us monitor the progress of the migration.
     */
    @Bean
    public Counter processedRecordsCounter(MeterRegistry registry) {
        return Counter.builder("migration.records.processed")
                .description("Total number of processed records")
                .tag("type", "record_processing")
                .register(registry);
    }

    /**
     * Creates a Counter metric to track migration errors.
     * This helps us monitor the reliability of our migration process.
     */
    @Bean
    public Counter errorCounter(MeterRegistry registry) {
        return Counter.builder("migration.errors")
                .description("Total number of migration errors")
                .tag("type", "error_tracking")
                .register(registry);
    }

    /**
     * Creates a Gauge metric to track the current migration progress.
     * This provides a real-time view of how far along the migration is.
     */
    @Bean
    public Gauge migrationProgressGauge(MeterRegistry registry) {
        return Gauge.builder("migration.progress", () -> 0.0)
                .description("Current migration progress percentage")
                .tag("type", "progress_tracking")
                .register(registry);
    }

    /**
     * Creates a Timer metric to track the latency of record access through the SmartProxy.
     * This helps us ensure that our migration isn't significantly impacting system performance.
     */
    @Bean
    public Timer proxyAccessTimer(MeterRegistry registry) {
        return Timer.builder("smartproxy.access.time")
                .description("Time taken to access records through SmartProxy")
                .tag("type", "proxy_access")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }
}