package io.mosip.registration.metrics;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;
import io.mosip.registration.config.LoggingJsonMeterRegistry;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dao.impl.RegistrationDAOImpl;
import io.mosip.registration.repositories.RegistrationRepository;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;


/**
 * 
 * @author M1063027
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ RegistrationAppHealthCheckUtil.class })
public class PacketMetricsTest {

	@InjectMocks
	private PacketMetrics packetMetrics;

	@InjectMocks
	private RegistrationDAOImpl registrationDAO;

	@Mock
	private RegistrationRepository registrationRepository;

	@Mock
	private IdentitySchemaService identitySchemaService;

	@Mock
	private org.springframework.context.ApplicationContext applicationContext;

	@Before
	public void init() throws Exception {
		List<Tag> immuTags = new ArrayList<Tag>();
		ImmutableTag tag1 = new ImmutableTag("key1", "value1");
		immuTags.add(tag1);
		packetMetrics = new PacketMetrics(immuTags, applicationContext);
		Mockito.when(applicationContext.getBean(RegistrationDAO.class)).thenReturn(registrationDAO);
	}

	@Test
	public void packetMetricsConstructorTest() {
		packetMetrics = new PacketMetrics(applicationContext);
	}

	@Test
	public void isRequiredFieldTest() {
		List<Object[]> objectTagList = new ArrayList<Object[]>();
		Object[] objects = { "client-state", "server-state", new Long(30) };
		objectTagList.add(objects);
		LoggingJsonMeterRegistry registry = new LoggingJsonMeterRegistry();
		Mockito.when(registrationDAO.getStatusBasedCount()).thenReturn(objectTagList);
		Mockito.when(registrationRepository.getStatusBasedCount()).thenReturn(objectTagList);
		packetMetrics.bindTo(registry);
	}

	@Test
	public void isRequiredFieldNullTest() {
		List<Object[]> objectTagList = null;
		LoggingJsonMeterRegistry registry = new LoggingJsonMeterRegistry();
		Mockito.when(registrationDAO.getStatusBasedCount()).thenReturn(objectTagList);
		Mockito.when(registrationRepository.getStatusBasedCount()).thenReturn(objectTagList);
		packetMetrics.bindTo(registry);
	}
}
