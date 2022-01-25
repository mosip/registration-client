package io.mosip.registration.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.mosip.registration.dao.RegistrationDAO;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
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
        
        List<Object[]> result = null;

        try {
            result = registrationDAO.getStatusBasedCount();
        } catch (DataAccessException exception) {
            //This will fail during upgrade process
        }

        if(result != null) {
            for (Object[] entry : result) {
                List<Tag> tagList = new ArrayList<>();
                tags.forEach(tag-> { tagList.add(tag); });
                tagList.add(Tag.of("client-state", entry[0] == null ? "NA" :(String) entry[0]));
                tagList.add(Tag.of("server-state", entry[1] == null ? "NA" : (String) entry[1]));
                Gauge.builder("packet.states", (Long)entry[2], Double::valueOf)
                        .strongReference(true).tags(tagList).register(registry);
            }
        }
        else {
            List<Tag> tagList = new ArrayList<>();
            tags.forEach(tag-> { tagList.add(tag); });
            tagList.add(Tag.of("client-state", "NA"));
            tagList.add(Tag.of("server-state", "NA"));
            Gauge.builder("packet.states", 0, Double::valueOf)
                    .strongReference(true).tags(tagList).register(registry);
        }
    }
}
