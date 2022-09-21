package io.mosip.registration.config;

import javax.sql.DataSource;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.mosip.kernel.auditmanager.config.AuditConfig;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;
import io.mosip.kernel.logger.logback.factory.Logfactory;
import io.mosip.kernel.templatemanager.velocity.builder.TemplateManagerBuilderImpl;

/**
 * Spring Configuration class for Registration-Service Module
 * 
 * @author Balaji Sridharan
 * @since 1.0.0
 *
 */
@Configuration
@EnableAspectJAutoProxy
@Import({ DaoConfig.class, AuditConfig.class, TemplateManagerBuilderImpl.class })
@EnableJpaRepositories(basePackages = "io.mosip.registration", repositoryBaseClass = HibernateRepositoryImpl.class)
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
		".*IdObjectCompositeValidator",
		".*IdObjectMasterDataValidator",
		".*PacketDecryptorImpl",
		".*IdSchemaUtils",
		".*OnlinePacketCryptoServiceImpl"}),
		basePackages = { "io.mosip.registration",
		"io.mosip.kernel.idvalidator", "io.mosip.kernel.ridgenerator", "io.mosip.kernel.qrcode",
		"io.mosip.kernel.crypto", "io.mosip.kernel.jsonvalidator", "io.mosip.kernel.idgenerator",
		"io.mosip.kernel.virusscanner", "io.mosip.kernel.transliteration", "io.mosip.kernel.applicanttype",
		"io.mosip.kernel.core.pdfgenerator.spi", "io.mosip.kernel.pdfgenerator.itext.impl",
		"io.mosip.kernel.idobjectvalidator.impl", "io.mosip.kernel.biosdk.provider.impl",
		"io.mosip.kernel.biosdk.provider.factory", "io.mosip.commons.packet",
		"io.mosip.registration.api.config" })
@PropertySource(value = { "classpath:spring.properties", "classpath:props/mosip-application.properties" })
@ImportAutoConfiguration(RefreshAutoConfiguration.class)
@EnableConfigurationProperties
@EnableRetry
public class AppConfig {
	
	@Value("${regclient.default.httpclient.connections.max.per.host:20}")
	private int defaultMaxConnectionPerRoute;

	@Value("${regclient.default.httpclient.connections.max:100}")
	private int defaultTotalMaxConnection;
	
	@Value("${regclient.selfToken.httpclient.connections.max.per.host:20}")
	private int selfTokenMaxConnectionPerRoute;

	@Value("${regclient.selfToken.httpclient.connections.max:100}")
	private int selfTokenTotalMaxConnection;

	@Autowired
	@Qualifier("dataSource")
	private DataSource datasource;

	public static Logger getLogger(Class<?> className) {
		return Logfactory.getSlf4jLogger(className);
	}

	@Bean
	@Primary
	public RestTemplate restTemplate() {
		HttpClientBuilder httpClientBuilder = HttpClients.custom().setMaxConnPerRoute(defaultMaxConnectionPerRoute)
				.setMaxConnTotal(defaultTotalMaxConnection).disableCookieManagement();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClientBuilder.build());

		return new RestTemplate(requestFactory);
	}

	@Bean
	public RestTemplate selfTokenRestTemplate() {
		HttpClientBuilder httpClientBuilder = HttpClients.custom().setMaxConnPerRoute(selfTokenMaxConnectionPerRoute)
				.setMaxConnTotal(selfTokenTotalMaxConnection).disableCookieManagement();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClientBuilder.build());

		return new RestTemplate(requestFactory);
	}

	@Bean
	public ObjectMapper mapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		return mapper;
	}

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager("entities");
	}
}
