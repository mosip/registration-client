package io.mosip.geoposition.rxtx;

import io.mosip.registration.api.geoposition.GeoPositionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoPositionConfig {

    @Bean
    public GeoPositionService getGeoPositionServiceImpl() {
        return new GeoPositionServiceImpl();
    }
}
