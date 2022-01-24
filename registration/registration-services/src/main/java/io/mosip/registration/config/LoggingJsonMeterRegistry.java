package io.mosip.registration.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import io.micrometer.core.instrument.util.TimeUtils;
import io.micrometer.core.lang.Nullable;
import io.micrometer.core.util.internal.logging.InternalLogger;
import io.micrometer.core.util.internal.logging.InternalLoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.micrometer.core.instrument.util.DoubleFormat.decimalOrNan;
import static io.micrometer.core.instrument.util.StringEscapeUtils.escapeJson;

public class LoggingJsonMeterRegistry extends StepMeterRegistry {

    private final static InternalLogger log = InternalLoggerFactory.getInstance(LoggingJsonMeterRegistry.class);
    static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private final LoggingRegistryConfig config;
    private final Consumer<String> loggingSink;

    public LoggingJsonMeterRegistry() {
        this(LoggingRegistryConfig.DEFAULT, Clock.SYSTEM);
    }

    public LoggingJsonMeterRegistry(LoggingRegistryConfig config, Clock clock) {
        this(config, clock, new NamedThreadFactory("logging-metrics-publisher"), log::info);
    }

    private LoggingJsonMeterRegistry(LoggingRegistryConfig config, Clock clock, ThreadFactory threadFactory, Consumer<String> loggingSink) {
        super(config, clock);
        this.config = config;
        this.loggingSink = loggingSink;
        config().namingConvention(NamingConvention.dot);
        start(threadFactory);
    }

    @Override
    protected void publish() {
        if (config.enabled()) {
            getMeters().stream()
                    .sorted((m1, m2) -> {
                        int typeComp = m1.getId().getType().compareTo(m2.getId().getType());
                        if (typeComp == 0) {
                            return m1.getId().getName().compareTo(m2.getId().getName());
                        }
                        return typeComp;
                    })
                    .forEach(m -> {
                        LoggingJsonMeterRegistry.Printer print = new LoggingJsonMeterRegistry.Printer(m);
                        m.use(
                                gauge -> loggingSink.accept(writeDocument(m, builder -> {
                                    String[] value = print.value(gauge.value()).split(" ");
                                    builder.append(",\"value\":").append(value[0]);
                                    if(value.length > 1)
                                        builder.append(",\"unit\":\"").append(value[1]).append("\"");
                                })),
                                counter -> {
                                    double count = counter.count();
                                    if (!config.logInactive() && count == 0) return;
                                    loggingSink.accept(writeDocument(m, builder -> {
                                        builder.append(",\"count\":").append(count);
                                    }));
                                },
                                timer -> {
                                    HistogramSnapshot snapshot = timer.takeSnapshot();
                                    long count = snapshot.count();
                                    if (!config.logInactive() && count == 0) return;
                                    loggingSink.accept(writeDocument(m, builder -> {
                                        builder.append(",\"count\":").append(timer.count());
                                        builder.append(",\"sum\":").append(timer.totalTime(getBaseTimeUnit()));
                                        builder.append(",\"mean\":").append(timer.mean(getBaseTimeUnit()));
                                        builder.append(",\"max\":").append(timer.max(getBaseTimeUnit()));
                                    }));
                                },
                                summary -> {
                                    HistogramSnapshot snapshot = summary.takeSnapshot();
                                    long count = snapshot.count();
                                    if (!config.logInactive() && count == 0) return;
                                    loggingSink.accept(writeDocument(m, builder -> {
                                        builder.append(",\"count\":").append(snapshot.count());
                                        builder.append(",\"sum\":").append(snapshot.total());
                                        builder.append(",\"mean\":").append(snapshot.mean());
                                        builder.append(",\"max\":").append(snapshot.max());
                                    }));
                                },
                                longTaskTimer -> {
                                    int activeTasks = longTaskTimer.activeTasks();
                                    if (!config.logInactive() && activeTasks == 0) return;
                                    loggingSink.accept(writeDocument(m, builder -> {
                                        builder.append(",\"active\":").append(activeTasks);
                                        builder.append(",\"duration\":").append(longTaskTimer.duration(getBaseTimeUnit()));
                                    }));
                                },
                                timeGauge -> {
                                    double value = timeGauge.value(getBaseTimeUnit());
                                    if (!config.logInactive() && value == 0) return;
                                    loggingSink.accept(writeDocument(m, builder -> {
                                        builder.append(",\"value\":").append(value);
                                    }));
                                },
                                counter -> {
                                    double count = counter.count();
                                    if (!config.logInactive() && count == 0) return;
                                    loggingSink.accept(writeDocument(m, builder -> {
                                        builder.append(",\"count\":").append(count);
                                    }));
                                },
                                timer -> {
                                    double count = timer.count();
                                    if (!config.logInactive() && count == 0) return;
                                    loggingSink.accept(writeDocument(m, builder -> {
                                        builder.append(",\"count\":").append(timer.count());
                                        builder.append(",\"sum\":").append(timer.totalTime(getBaseTimeUnit()));
                                        builder.append(",\"mean\":").append(timer.mean(getBaseTimeUnit()));
                                    }));
                                },
                                meter -> loggingSink.accept(writeMeter(meter, print))
                        );
                    });
        }
    }

    String writeMeter(Meter meter, LoggingJsonMeterRegistry.Printer print) {
        Iterable<Measurement> measurements = meter.measure();
        List<String> names = new ArrayList<>();
        // Snapshot values should be used throughout this method as there are chances for values to be changed in-between.
        List<Double> values = new ArrayList<>();
        for (Measurement measurement : measurements) {
            double value = measurement.getValue();
            if (!Double.isFinite(value)) {
                continue;
            }
            names.add(measurement.getStatistic().getTagValueRepresentation());
            values.add(value);
        }
        if (names.isEmpty()) {
            return "";
        }
        return writeDocument(meter, builder -> {
            for (int i = 0; i < names.size(); i++) {
                builder.append(",\"").append(names.get(i)).append("\":\"").append(values.get(i)).append("\"");
            }
        });
    }

    private String writeDocument(Meter meter, Consumer<StringBuilder> consumer) {
        StringBuilder sb = new StringBuilder();
        String timestamp = generateTimestamp();
        String name = getConventionName(meter.getId());
        String type = meter.getId().getType().toString().toLowerCase();
        sb.append("{\"").append("@timestamp").append("\":\"").append(timestamp).append('"')
                .append(",\"name\":\"").append(escapeJson(name)).append('"')
                .append(",\"type\":\"").append(type).append('"');

        List<Tag> tags = getConventionTags(meter.getId());
        for (Tag tag : tags) {
            sb.append(",\"").append(escapeJson(tag.getKey())).append("\":\"")
                    .append(escapeJson(tag.getValue())).append('"');
        }
        consumer.accept(sb);
        sb.append('}');
        return sb.toString();
    }

    protected String generateTimestamp() {
        return TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(config().clock().wallTime()));
    }

    class Printer {
        private final Meter meter;

        Printer(Meter meter) {
            this.meter = meter;
        }

        String time(double time) {
            return TimeUtils.format(Duration.ofNanos((long) TimeUtils.convert(time, getBaseTimeUnit(), TimeUnit.NANOSECONDS)));
        }

        String rate(double rate) {
            return humanReadableBaseUnit(rate / (double) config.step().getSeconds()) + "/s";
        }

        String unitlessRate(double rate) {
            return decimalOrNan(rate / (double) config.step().getSeconds()) + "/s";
        }

        String value(double value) {
            return humanReadableBaseUnit(value);
        }

        // see https://stackoverflow.com/a/3758880/510017
        String humanReadableByteCount(double bytes) {
            int unit = 1024;
            if (bytes < unit || Double.isNaN(bytes)) return decimalOrNan(bytes) + " B";
            int exp = (int) (Math.log(bytes) / Math.log(unit));
            String pre = "KMGTPE".charAt(exp - 1) + "i";
            return decimalOrNan(bytes / Math.pow(unit, exp)) + " " + pre + "B";
        }

        String humanReadableBaseUnit(double value) {
            String baseUnit = meter.getId().getBaseUnit();
            if (BaseUnits.BYTES.equals(baseUnit)) {
                return humanReadableByteCount(value);
            }
            return decimalOrNan(value) + (baseUnit != null ? " " + baseUnit : "");
        }
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    public static LoggingJsonMeterRegistry.Builder builder(LoggingRegistryConfig config) {
        return new LoggingJsonMeterRegistry.Builder(config);
    }

    public static class Builder {
        private final LoggingRegistryConfig config;

        private Clock clock = Clock.SYSTEM;
        private ThreadFactory threadFactory = new NamedThreadFactory("logging-metrics-publisher");
        private Consumer<String> loggingSink = log::info;
        @Nullable
        private Function<Meter, String> meterIdPrinter;

        Builder(LoggingRegistryConfig config) {
            this.config = config;
        }

        public LoggingJsonMeterRegistry.Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public LoggingJsonMeterRegistry.Builder threadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public LoggingJsonMeterRegistry.Builder loggingSink(Consumer<String> loggingSink) {
            this.loggingSink = loggingSink;
            return this;
        }

        public LoggingJsonMeterRegistry build() {
            return new LoggingJsonMeterRegistry(config, clock, threadFactory, loggingSink);
        }
    }
}
