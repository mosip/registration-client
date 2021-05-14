package io.mosip.registration.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.mosip.registration.dao.RegistrationDAO;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static java.util.Collections.emptyList;

public class PacketMetrics implements MeterBinder {

    private final Iterable<Tag> tags;
    private ApplicationContext applicationContext;

    public PacketMetrics(ApplicationContext applicationContext) {
        this(emptyList(), applicationContext);
    }

    public PacketMetrics(Iterable<Tag> tags, ApplicationContext applicationContext) {
        this.tags = tags;
        this.applicationContext = applicationContext;
    }


    @Override
    public void bindTo(MeterRegistry registry) {
        RegistrationDAO registrationDAO = applicationContext.getBean(RegistrationDAO.class);

        if(registrationDAO == null)
            return;

        List<Object[]> result = registrationDAO.getStatusBasedCount();

        if(result != null) {
            for (Object[] entry : result) {
                Gauge.builder(String.format("packet.%s.%s", entry[0], entry[1]), entry[2], (count) -> {
                    return Double.valueOf((String)count);
                }).strongReference(true).tags(tags).register(registry);
            }
        }
        else
            Gauge.builder(String.format("packet.%s.%s", "NA", "NA"), 0, Double::valueOf)
                    .strongReference(true).tags(tags).register(registry);
    }
}
