package io.mosip.registration.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Date;

import static java.util.Collections.emptyList;

public class SystemTimeMetrics implements MeterBinder {

    private final Iterable<Tag> tags;

    public SystemTimeMetrics() {
        this(emptyList());
    }

    public SystemTimeMetrics(Iterable<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();

        Gauge.builder("system.uptime", rb, RuntimeMXBean::getUptime)
                .tags(tags)
                .register(registry);

        Gauge.builder("system.time", System.currentTimeMillis(), Double::valueOf)
                .strongReference(true)
                .tags(tags)
                .register(registry);

    }
}
