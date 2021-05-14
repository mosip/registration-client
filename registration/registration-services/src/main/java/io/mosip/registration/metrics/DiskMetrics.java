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
            return Long.valueOf(t.getUsableSpace()).doubleValue();
        }).tags(tags).strongReference(true).register(registry);

        Gauge.builder("disk.free", dir, (t)  -> {
            return Long.valueOf(t.getFreeSpace()).doubleValue();
        }).tags(tags).strongReference(true).register(registry);

        Gauge.builder("disk.total", dir, (t) -> {
            return Long.valueOf(t.getTotalSpace()).doubleValue();
        }).tags(tags).strongReference(true).register(registry);
    }
}