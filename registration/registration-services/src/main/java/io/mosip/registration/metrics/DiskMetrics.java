package io.mosip.registration.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.io.File;

import static java.util.Collections.emptyList;

public class DiskMetrics implements MeterBinder {

    private final Iterable<Tag> tags;

    public DiskMetrics() {
        this(emptyList());
    }

    public DiskMetrics(Iterable<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        File dir = new File(System.getProperty("user.dir"));

        Gauge.builder("disk.usable", dir, (t) -> {
			return (double) t.getUsableSpace();
        }).tags(tags).strongReference(true).register(registry);

        Gauge.builder("disk.free", dir, (t)  -> {
			return (double) t.getFreeSpace();
        }).tags(tags).strongReference(true).register(registry);

        Gauge.builder("disk.total", dir, (t) -> {
			return (double) t.getTotalSpace();
        }).tags(tags).strongReference(true).register(registry);
    }
}