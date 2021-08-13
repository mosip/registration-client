package io.mosip.registration.api.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = { "io.mosip.registration.api.impl.scanner",
        "io.mosip.registration.api.impl.gps" })
@Configuration
public class DefaultImplConfig {


}