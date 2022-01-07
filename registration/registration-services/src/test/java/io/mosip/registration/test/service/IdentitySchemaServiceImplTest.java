package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.keygenerator.bouncycastle.KeyGenerator;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.entity.IdentitySchema;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.IdentitySchemaRepository;
import io.mosip.registration.service.impl.IdentitySchemaServiceImpl;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

/**
 * 
 * @author Rama Devi Idupulapati
 *
 */

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ UserOnBoardServiceImplTest.class, RegistrationSystemPropertiesChecker.class, ApplicationContext.class,
		RegistrationAppHealthCheckUtil.class, KeyGenerator.class, SecretKey.class, SessionContext.class })
public class IdentitySchemaServiceImplTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	private IdentitySchemaDao identitySchemaDao;

	@Mock
	private IdentitySchemaRepository identitySchemaRepository;

	@InjectMocks
	private IdentitySchemaServiceImpl identitySchemaServiceImpl;

	@Before
	public void init() throws Exception {
		Map<String, Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.USER_ON_BOARD_THRESHOLD_LIMIT, "10");
		appMap.put("mosip.registration.fingerprint_disable_flag", "Y");
		appMap.put("mosip.registration.iris_disable_flag", "Y");
		appMap.put("mosip.registration.face_disable_flag", "Y");
		appMap.put("mosip.registration.onboarduser_ida_auth", "Y");
		ApplicationContext.getInstance().setApplicationMap(appMap);

		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");
		;
	}

	@Test
	public void getLatestEffectiveSchemaVersionTest() throws RegBaseCheckedException {
		Double idVersion = 2.0;
		Mockito.when(identitySchemaDao.getLatestEffectiveSchemaVersion()).thenReturn(idVersion);
		assertNotNull(identitySchemaServiceImpl.getLatestEffectiveSchemaVersion());
	}

	@Test
	@Ignore
	public void getLatestEffectiveIDSchemaTest() throws RegBaseCheckedException {
		String schemaJson = "schemaJson";
		IdentitySchema idnSchema = new IdentitySchema();
		Mockito.when(identitySchemaRepository
				.findLatestEffectiveIdentitySchema(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime())))
				.thenReturn(idnSchema);

		Mockito.when(identitySchemaDao.getLatestEffectiveIDSchema()).thenReturn(schemaJson);
		assertNotNull(identitySchemaServiceImpl.getLatestEffectiveSchemaVersion());
	}

	@Test(expected = RegBaseUncheckedException.class)
	@Ignore
	public void getLatestEffectiveSchemaVersionFalseTest() throws RegBaseCheckedException {
		Double idVersion = null;
		Mockito.when(identitySchemaDao.getLatestEffectiveSchemaVersion()).thenReturn(idVersion);
		Mockito.when(Mockito.when(identitySchemaDao.getLatestEffectiveSchemaVersion()))
				.thenThrow(new RegBaseUncheckedException());
		assertNotNull(identitySchemaServiceImpl.getLatestEffectiveSchemaVersion());
	}

}
