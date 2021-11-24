package io.mosip.registration.test.config;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.logger.logback.impl.LoggerImpl;
import io.mosip.registration.config.AppConfig;

public class AppConfigTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	AppConfig appConfig;

	@Test
	public void getRestTemplateTest() {
		assertEquals(appConfig.restTemplate().getClass(), RestTemplate.class);
	}

	@Test
	public void getObjectMapper() {
		assertEquals(appConfig.mapper().getClass(), ObjectMapper.class);
	}

	@Test
	@Ignore
	public void getLogger() {
		assertEquals(appConfig.getLogger(this.getClass()).getClass(), LoggerImpl.class);
	}

}
