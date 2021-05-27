package io.mosip.registration.api.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = { "io.mosip.api.impl.scanner", "io.mosip.api.impl.gps" })
@Configuration
public class DefaultImplConfig {


}