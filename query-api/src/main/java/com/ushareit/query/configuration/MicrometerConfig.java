package com.ushareit.query.configuration;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class MicrometerConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config().meterFilter (
                    new MeterFilter() {
                        @Override
                        public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                            if (id.getType() == Meter.Type.TIMER && id.getName().matches("^(http|hystrix){1}.*")) {
                                return DistributionStatisticConfig.builder()
                                        .percentilesHistogram(true)
                                        .percentiles(0.5, 0.90,0.95, 0.99)
                                        .sla(Double.valueOf(Duration.ofMillis(50).toNanos()),
                                        		Double.valueOf(Duration.ofMillis(100).toNanos()),
                                        	    Double.valueOf(Duration.ofMillis(200).toNanos()),
                                        		Double.valueOf( Duration.ofSeconds(1).toNanos()),
                                        		Double.valueOf(Duration.ofSeconds(5).toNanos()))
                                        .minimumExpectedValue(Double.valueOf(Duration.ofMillis(1).toNanos()))
                                        .maximumExpectedValue(Double.valueOf(Duration.ofSeconds(5).toNanos()))
                                        .build()
                                        .merge(config);
                            } else {
                                return config;
                            }
                        }
                    });
        };
    }
}