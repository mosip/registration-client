package io.mosip.registration.test.config;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.signature.service.SignatureService;
import io.mosip.kernel.signature.service.impl.SignatureServiceImpl;
import io.mosip.registration.config.DaoConfig;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationHelper;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.registration.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;


@Configuration
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
		".*IdObjectCompositeValidator",
		".*IdObjectMasterDataValidator",
		".*PacketDecryptorImpl",
		".*IdSchemaUtils", ".*OnlinePacketCryptoServiceImpl"}),
		basePackages = {
		"io.mosip.registration",
		"io.mosip.kernel.idvalidator", "io.mosip.kernel.ridgenerator", "io.mosip.kernel.qrcode",
		"io.mosip.kernel.crypto", "io.mosip.kernel.jsonvalidator", "io.mosip.kernel.idgenerator",
		"io.mosip.kernel.virusscanner", "io.mosip.kernel.transliteration", "io.mosip.kernel.applicanttype",
		"io.mosip.kernel.core.pdfgenerator.spi", "io.mosip.kernel.pdfgenerator.itext.impl",
		"io.mosip.kernel.idobjectvalidator.impl", "io.mosip.kernel.biosdk.provider.impl",
		"io.mosip.kernel.biosdk.provider.factory", "io.mosip.commons.packet",
		"io.mosip.registration.api.config"})
@PropertySource(value = { "classpath:spring-test.properties", "classpath:props/mosip-application.properties" })
public class TestDaoConfig extends DaoConfig {

	
	private static final String DRIVER_CLASS_NAME = "org.h2.Driver";
	private static final String URL = "jdbc:h2:mem:db;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:initial.sql'";
	private static final String GLOBAL_PARAM_PROPERTIES = "SELECT CODE, VAL FROM REG.GLOBAL_PARAM WHERE IS_ACTIVE=TRUE AND VAL IS NOT NULL";
	private static final String KEY = "CODE";
	private static final String VALUE = "VAL";
	private static final String LOCAL_PREFERENCES = "SELECT NAME, VAL FROM REG.LOCAL_PREFERENCES WHERE IS_DELETED=FALSE AND CONFIG_TYPE='CONFIGURATION' AND VAL IS NOT NULL";
	private static final String NAME = "NAME";
	private static final String SCHEMA_NAME = "REG";

	private static DataSource dataSource;
	private static JdbcTemplate jdbcTemplate;
	private static Properties keys = new Properties();
	private static ApplicationContext applicationContext;

	@Autowired
	private ConfigurableEnvironment environment;
	
	static {
		ApplicationContext.getInstance();

		try (InputStream configKeys = DaoConfig.class.getClassLoader().getResourceAsStream("spring-test.properties");
			 InputStream buildKeys = DaoConfig.class.getClassLoader().getResourceAsStream("props/mosip-application.properties")) {

			applicationContext = io.mosip.registration.context.ApplicationContext.getInstance();

			keys = new Properties();
			keys.load(configKeys);
			keys.load(buildKeys);
			keys.keySet().forEach( k -> {
				applicationContext.getApplicationMap().put((String) k, keys.get(k));
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	@Bean(name = "dataSource")
	public DataSource dataSource() {
		return setupDatasource();
	}

	
	@Bean
	public JdbcTemplate jdbcTemplate() {
		return new JdbcTemplate(dataSource());
	}
	
	@Bean
	@Lazy(false)
	public static PropertyPlaceholderConfigurer properties() {
		
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Resource[] resources = new ClassPathResource[] {new ClassPathResource("spring-test.properties")};
		ppc.setLocations(resources);

		ppc.setProperties(keys);
		ppc.setTrimValues(true);

		return ppc;
	}
	
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		vendorAdapter.setDatabase(Database.H2);
		vendorAdapter.setGenerateDdl(false);

		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource());
		em.setPackagesToScan("io.mosip.registration", "io.mosip.kernel");
		em.setJpaVendorAdapter(vendorAdapter);
		em.setJpaProperties(additionalProperties());

		return em;
	}

	@Override
	@Bean
	public JpaDialect jpaDialect() {
		return new HibernateJpaDialect();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see io.mosip.kernel.core.dao.config.BaseDaoConfig#transactionManager(javax.
	 * persistence.EntityManagerFactory)
	 */
	@Override
	@Bean
	public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager jpaTransactionManager = new JpaTransactionManager(entityManagerFactory);
		jpaTransactionManager.setDataSource(dataSource);
		jpaTransactionManager.setJpaDialect(jpaDialect());
		return jpaTransactionManager;
	}
	
	private Properties additionalProperties() {
		Properties properties = new Properties();
		properties.setProperty("hibernate.hbm2ddl.auto", "none");
		properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		//properties.setProperty("hibernate.current_session_context_class", keys.getProperty("hibernate.current_session_context_class"));
		//properties.setProperty("hibernate.jdbc.lob.non_contextual_creation", keys.getProperty("hibernate.jdbc.lob.non_contextual_creation"));
		properties.setProperty("hibernate.show_sql", "true");
		properties.setProperty("hibernate.format_sql", "false");
		return properties;
	}

	private static DriverManagerDataSource setupDatasource() {
		DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
		driverManagerDataSource.setDriverClassName(DRIVER_CLASS_NAME);
		driverManagerDataSource.setSchema(SCHEMA_NAME);
		driverManagerDataSource.setUrl(URL);
		return driverManagerDataSource;
	}

	@Primary
	@Bean
	public SignatureService signatureService() {
		return Mockito.mock(SignatureServiceImpl.class);
	}

	@Primary
	@Bean
	public BioAPIFactory bioAPIFactory() {
		return Mockito.mock(BioAPIFactory.class);
	}


}
