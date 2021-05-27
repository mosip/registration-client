package io.mosip.registration.api.config;

import io.mosip.registration.api.geoposition.GeoPositionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

import io.mosip.geoposition.rxtx.GeoPositionServiceImpl;

@ComponentScan(basePackages = { "io.mosip.geoposition.rxtx" })
@Configuration
public class GeoPositionConfig {

}
