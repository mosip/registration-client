package io.mosip.registration.test.service;

import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.cryptomanager.util.CryptomanagerUtils;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.kernel.keymanagerservice.util.KeymanagerUtil;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dao.impl.RegistrationCenterDAOImpl;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.id.RegMachineSpecId;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.repositories.MachineMasterRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.sync.impl.PublicKeySyncImpl;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

import static org.mockito.Mockito.doNothing;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ RegistrationAppHealthCheckUtil.class, ApplicationContext.class, SessionContext.class ,
		RegistrationSystemPropertiesChecker.class})
public class PublicKeySyncTest {

	@Rule
	public MockitoRule MockitoRule = MockitoJUnit.rule();

	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	@Mock
	private KeymanagerService keymanagerService;

	@Mock
	private KeymanagerUtil keymanagerUtil;

	@Mock
	private CryptomanagerUtils cryptomanagerUtils;

	private String signRefId = "SIGN";

	@InjectMocks
	private PublicKeySyncImpl publicKeySyncImpl;

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

	@Before
	public void init() {
		PowerMockito.mockStatic(ApplicationContext.class, RegistrationAppHealthCheckUtil.class, SessionContext.class,
				RegistrationSystemPropertiesChecker.class);
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");

		Mockito.when(baseService.getCenterId(Mockito.anyString())).thenReturn("10011");
		Mockito.when(baseService.getStationId()).thenReturn("11002");
		Mockito.when(baseService.isInitialSync()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive(Mockito.anyString())).thenReturn(true);

		//Mockito.when(baseService.getGlobalConfigValueOf(RegistrationConstants.INITIAL_SETUP)).thenReturn(RegistrationConstants.DISABLE);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("11002");

		MachineMaster machine = new MachineMaster();
		machine.setId("11002");
		machine.setIsActive(true);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);
	}

	@Test
	public void getPublicKey()
			throws ParseException, RegBaseCheckedException, ConnectionException {

		DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd");
		Date date = dateFormat.parse("2019-4-5");
		Timestamp timestamp = new Timestamp(date.getTime());

		Map<String, Object> responseMap = new LinkedHashMap<>();
		LinkedHashMap<String, Object> valuesMap = new LinkedHashMap<>();
		valuesMap.put("publicKey",
				"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtCR2L_MwUv4ctfGulWf4ZoWkSyBHbfkVtE_xAmzzIDWHP1V5hGxg8jt8hLtYYFwBNj4l_PTZGkblcVg-IePHilmQiVDptTVVA2PGtwRdud7QL4xox8RXmIf-xa-JmP2E804iVM-Ki8aPf1yuxXNUwLxZsflFww73lc-SGVUHupD8Os0qNZbbJl0BYioNG4WmPMHy3WJ-7jGN0HEV-9E18yf_enR0YewUmUI6Rxxb606-w8iQyWfSJq6UOfFmH5WAn-oTOoTIwg_fBxXuG_FlDoNWs6N5JtI18BMsUQA_GQZJct6TyXcBNUrcBYhZERvPlRGqIOoTl-T2sPJ5ST9eswIDAQAB");
		valuesMap.put("issuedAt", "2020-04-09T05:51:17.334");
		valuesMap.put("expiryAt", "2020-04-09T05:51:17.334");
		responseMap.put(RegistrationConstants.RESPONSE, valuesMap);
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenReturn(responseMap);

		publicKeySyncImpl.getPublicKey("user");

	}

	@Test
	public void getPublicKeyLogin()
			throws RegBaseCheckedException, ConnectionException  {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Map<String, Object> responseMap = new LinkedHashMap<>();
		LinkedHashMap<String, Object> valuesMap = new LinkedHashMap<>();
		valuesMap.put("publicKey",
				"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtCR2L_MwUv4ctfGulWf4ZoWkSyBHbfkVtE_xAmzzIDWHP1V5hGxg8jt8hLtYYFwBNj4l_PTZGkblcVg-IePHilmQiVDptTVVA2PGtwRdud7QL4xox8RXmIf-xa-JmP2E804iVM-Ki8aPf1yuxXNUwLxZsflFww73lc-SGVUHupD8Os0qNZbbJl0BYioNG4WmPMHy3WJ-7jGN0HEV-9E18yf_enR0YewUmUI6Rxxb606-w8iQyWfSJq6UOfFmH5WAn-oTOoTIwg_fBxXuG_FlDoNWs6N5JtI18BMsUQA_GQZJct6TyXcBNUrcBYhZERvPlRGqIOoTl-T2sPJ5ST9eswIDAQAB");
		valuesMap.put("issuedAt", "2020-04-09T05:51:17.334");
		valuesMap.put("expiryAt", "2020-04-09T05:51:17.334");
		responseMap.put(RegistrationConstants.RESPONSE, valuesMap);
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenReturn(responseMap);

		publicKeySyncImpl.getPublicKey("user");

	}

	@Test
	public void getPublicKeyLoginFailure()
			throws RegBaseCheckedException, ConnectionException {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Map<String, Object> responseMap = new LinkedHashMap<>();
		List<LinkedHashMap<String, Object>> valuesMap = new ArrayList<>();
		LinkedHashMap<String, Object> errorMap = new LinkedHashMap<>();
		errorMap.put("errorCode", "KER-KMS-005");
		errorMap.put("message", "Required String parameter 'timeStamp' is not present");
		valuesMap.add(errorMap);
		responseMap.put(RegistrationConstants.RESPONSE, null);
		responseMap.put(RegistrationConstants.ERRORS, valuesMap);
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenReturn(responseMap);

		publicKeySyncImpl.getPublicKey("user");
	}

	@Test
	public void getPublicKeyError()
			throws ParseException, RegBaseCheckedException, ConnectionException {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
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
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenReturn(responseMap);

		publicKeySyncImpl.getPublicKey("user");

	}

	@Test
	public void getPublicKeyException()
			throws ParseException, RegBaseCheckedException, ConnectionException {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
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
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenThrow(ConnectionException.class);

		publicKeySyncImpl.getPublicKey("user");

	}

	@Test(expected = PreConditionCheckException.class )
	public void getPublicKeyNetworkFailure()
			throws ParseException, RegBaseCheckedException, ConnectionException {

		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd");
		Date date = dateFormat.parse("2019-4-5");
		Timestamp timestamp = new Timestamp(date.getTime());

		Map<String, Object> responseMap = new LinkedHashMap<>();
		LinkedHashMap<String, Object> valuesMap = new LinkedHashMap<>();
		valuesMap.put("publicKey",
				"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtCR2L_MwUv4ctfGulWf4ZoWkSyBHbfkVtE_xAmzzIDWHP1V5hGxg8jt8hLtYYFwBNj4l_PTZGkblcVg-IePHilmQiVDptTVVA2PGtwRdud7QL4xox8RXmIf-xa-JmP2E804iVM-Ki8aPf1yuxXNUwLxZsflFww73lc-SGVUHupD8Os0qNZbbJl0BYioNG4WmPMHy3WJ-7jGN0HEV-9E18yf_enR0YewUmUI6Rxxb606-w8iQyWfSJq6UOfFmH5WAn-oTOoTIwg_fBxXuG_FlDoNWs6N5JtI18BMsUQA_GQZJct6TyXcBNUrcBYhZERvPlRGqIOoTl-T2sPJ5ST9eswIDAQAB");
		valuesMap.put("issuedAt", "2020-04-09T05:51:17.334");
		valuesMap.put("expiryAt", "2020-04-09T05:51:17.334");
		responseMap.put(RegistrationConstants.RESPONSE, valuesMap);
		Mockito.when(RegistrationAppHealthCheckUtil.isNetworkAvailable()).thenReturn(false);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(),
				Mockito.anyString())).thenReturn(responseMap);

		publicKeySyncImpl.getPublicKey("user");
	}

}
