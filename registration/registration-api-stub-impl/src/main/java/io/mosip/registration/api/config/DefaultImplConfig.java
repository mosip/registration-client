package io.mosip.registration.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = { "io.mosip.registration.api.impl.scanner", "io.mosip.registration.api.impl.gps" })
@EnableConfigurationProperties
@Configuration
public class DefaultImplConfig {


}