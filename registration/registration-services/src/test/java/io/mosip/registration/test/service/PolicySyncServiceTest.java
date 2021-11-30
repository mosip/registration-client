package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.impl.RegistrationCenterDAOImpl;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.RegistrationCenter;
import io.mosip.registration.entity.id.RegistartionCenterId;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.RegistrationCenterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import org.junit.Before;
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
import org.springframework.web.client.HttpClientErrorException;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.sync.impl.PolicySyncServiceImpl;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

/**
 * 
 * @author Brahmananda Reddy
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ RegistrationAppHealthCheckUtil.class, ApplicationContext.class, SessionContext.class ,
		RegistrationSystemPropertiesChecker.class})
public class PolicySyncServiceTest {
	@Rule
	public MockitoRule MockitoRule = MockitoJUnit.rule();

	private ApplicationContext applicationContext = ApplicationContext.getInstance();

	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;
	@Mock
	private UserOnboardDAO userOnboardDAO;

	@InjectMocks
	private PolicySyncServiceImpl policySyncServiceImpl;

	@Mock
	private BaseService baseService;

	@Mock
	private RegistrationCenterDAOImpl registrationCenterDAO;

	@Mock
	private CenterMachineReMapService centerMachineReMapService;

	@Mock
	private GlobalParamService globalParamService;

	@Mock
	private MachineMasterRepository machineMasterRepository;

	@Mock
	private LocalConfigService localConfigService;

	@Mock
	private RegistrationCenterRepository registrationCenterRepository;

	private String machineId = "machineId";
	private String centerId = "centerId";

	@Before
	public void initialize() {
		Map<String, Object> temp = new HashMap<String, Object>();
		temp.put("mosip.registration.key_policy_sync_threshold_value", "1");
		applicationContext.setApplicationMap(temp);

		PowerMockito.mockStatic(ApplicationContext.class, RegistrationAppHealthCheckUtil.class, SessionContext.class,
				RegistrationSystemPropertiesChecker.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");

		Mockito.when(baseService.getCenterId()).thenReturn("10011");
		Mockito.when(baseService.getStationId()).thenReturn("11002");
		Mockito.when(baseService.isInitialSync()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);

		//Mockito.when(baseService.getGlobalConfigValueOf(RegistrationConstants.INITIAL_SETUP)).thenReturn(RegistrationConstants.DISABLE);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");

		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setRegCenterId("10011");
		machine.setIsActive(true);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);

		String centerId = "centerId";
		RegistrationCenter registrationCenter = new RegistrationCenter();
		registrationCenter.setRegistartionCenterId(new RegistartionCenterId());
		registrationCenter.getRegistartionCenterId().setId(centerId);
		registrationCenter.getRegistartionCenterId().setLangCode("eng");
		Optional<RegistrationCenter> mockedCenter = Optional.of(registrationCenter);
		Mockito.when(registrationCenterRepository.findByIsActiveTrueAndRegistartionCenterIdIdAndRegistartionCenterIdLangCode(Mockito.anyString(),
				Mockito.anyString())).thenReturn(mockedCenter);

	}

	@Test
	public void fetch() throws RegBaseCheckedException, ConnectionException {
		Map<String, Object> responseMap = new LinkedHashMap<>();
		LinkedHashMap<String, Object> valuesMap = new LinkedHashMap<>();
		valuesMap.put("publicKey",
				"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtCR2L_MwUv4ctfGulWf4ZoWkSyBHbfkVtE_xAmzzIDWHP1V5hGxg8jt8hLtYYFwBNj4l_PTZGkblcVg-IePHilmQiVDptTVVA2PGtwRdud7QL4xox8RXmIf-xa-JmP2E804iVM-Ki8aPf1yuxXNUwLxZsflFww73lc-SGVUHupD8Os0qNZbbJl0BYioNG4WmPMHy3WJ-7jGN0HEV-9E18yf_enR0YewUmUI6Rxxb606-w8iQyWfSJq6UOfFmH5WAn-oTOoTIwg_fBxXuG_FlDoNWs6N5JtI18BMsUQA_GQZJct6TyXcBNUrcBYhZERvPlRGqIOoTl-T2sPJ5ST9eswIDAQAB");
		valuesMap.put("issuedAt", "2020-04-09T05:51:17.334");
		valuesMap.put("expiryAt", "2020-04-09T05:51:17.334");
		responseMap.put(RegistrationConstants.RESPONSE, valuesMap);

		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenReturn(responseMap);

		assertNotNull(policySyncServiceImpl.fetchPolicy());

	}

	@Test
	public void netWorkAvailable() throws RegBaseCheckedException {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(false);
		assertNotNull(policySyncServiceImpl.fetchPolicy());

	}

	@Test
	public void testKeyStore()
			throws ParseException, RegBaseCheckedException, ConnectionException {

		DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd");
		Date date = dateFormat.parse("2019-4-5");
		Timestamp timestamp = new Timestamp(date.getTime());

		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);

		Map<String, Object> responseMap = new LinkedHashMap<>();
		LinkedHashMap<String, Object> valuesMap = new LinkedHashMap<>();
		valuesMap.put("publicKey",
				"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtCR2L_MwUv4ctfGulWf4ZoWkSyBHbfkVtE_xAmzzIDWHP1V5hGxg8jt8hLtYYFwBNj4l_PTZGkblcVg-IePHilmQiVDptTVVA2PGtwRdud7QL4xox8RXmIf-xa-JmP2E804iVM-Ki8aPf1yuxXNUwLxZsflFww73lc-SGVUHupD8Os0qNZbbJl0BYioNG4WmPMHy3WJ-7jGN0HEV-9E18yf_enR0YewUmUI6Rxxb606-w8iQyWfSJq6UOfFmH5WAn-oTOoTIwg_fBxXuG_FlDoNWs6N5JtI18BMsUQA_GQZJct6TyXcBNUrcBYhZERvPlRGqIOoTl-T2sPJ5ST9eswIDAQAB");
		valuesMap.put("issuedAt", "2020-04-09T05:51:17.334");
		valuesMap.put("expiryAt", "2020-04-09T05:51:17.334");
		responseMap.put(RegistrationConstants.RESPONSE, valuesMap);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenReturn(responseMap);

		assertNotNull(policySyncServiceImpl.fetchPolicy());
	}
	
	@Test
	public void testKeyStoreError()
			throws ParseException, RegBaseCheckedException, ConnectionException {

		DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd");
		Date date = dateFormat.parse("2019-4-5");
		Timestamp timestamp = new Timestamp(date.getTime());

		Map<String, Object> responseMap = new LinkedHashMap<>();
		List<LinkedHashMap<String, Object>> valuesMap = new ArrayList<>();
		LinkedHashMap<String, Object> errorMap = new LinkedHashMap<>();
		errorMap.put("errorCode", "KER-KMS-005");
		errorMap.put("message", "Required String parameter 'timeStamp' is not present");
		valuesMap.add(errorMap);
		responseMap.put(RegistrationConstants.RESPONSE, null);
		responseMap.put(RegistrationConstants.ERRORS, valuesMap);

		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenReturn(responseMap);

		assertNotNull(policySyncServiceImpl.fetchPolicy());

	}
	
	@Test
	public void failureTestException() throws RegBaseCheckedException, ConnectionException {
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenThrow(ConnectionException.class);
		ResponseDTO responseDTO = policySyncServiceImpl.fetchPolicy();
		assertNotNull(responseDTO.getErrorResponseDTOs());
	}

	@Test
	public void failureTest() throws RegBaseCheckedException, ConnectionException {
		Mockito.when(serviceDelegateUtil.get(Mockito.any(), Mockito.any(), Mockito.anyBoolean(),
				Mockito.any())).thenThrow(RegBaseCheckedException.class);
		assertNotNull(policySyncServiceImpl.fetchPolicy());
	}

	@Test
	public void getPublicKeyfailureTest()
			throws RegBaseCheckedException, ConnectionException {
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenThrow(HttpClientErrorException.class);

		assertNotNull(policySyncServiceImpl.fetchPolicy());
	}

}
