package io.mosip.registration.api.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = { "io.mosip.registration.geoposition.rxtx" })
@Configuration
public class GeoPositionConfig {

}
