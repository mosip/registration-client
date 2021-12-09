package io.mosip.registration.test.service;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.test.config.TestClientCryptoServiceImpl;
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
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.UserDetailDto;
import io.mosip.registration.dto.UserDetailResponseDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.service.operator.impl.UserDetailServiceImpl;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ RegistrationAppHealthCheckUtil.class,UserDetailDAO.class, ApplicationContext.class, SessionContext.class })
public class UserDetailServcieTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	private UserOnboardService userOnboardService;

	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	@InjectMocks
	private UserDetailServiceImpl userDetailServiceImpl;

	@Mock
	private UserDetailDAO userDetailDAO;

	@Mock
	private BaseService baseService;

	@Mock
	private ClientCryptoFacade clientCryptoFacade;

	@Mock
	private ApplicationContext context;

	@Mock
	private RegistrationCenterDAO registrationCenterDAO;

	@Mock
	private CenterMachineReMapService centerMachineReMapService;

	@Mock
	private GlobalParamService globalParamService;

	@Mock
	private MachineMasterRepository machineMasterRepository;

	@Mock
	private LocalConfigService localConfigService;

	private ObjectMapper mapper = new ObjectMapper();

	@Before
	public void init() {
		PowerMockito.mockStatic(ApplicationContext.class, RegistrationAppHealthCheckUtil.class, SessionContext.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);

		ClientCryptoService mockedClientCryptoService = mock(TestClientCryptoServiceImpl.class);
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(mockedClientCryptoService);
		Mockito.when(clientCryptoFacade.decrypt(Mockito.any())).thenReturn("[]".getBytes(StandardCharsets.UTF_8));

		Map<String, Object> map = new HashMap<>();
		map.put(RegistrationConstants.MACHINE_CENTER_REMAP_FLAG, false);
		ApplicationContext.getInstance().setApplicationMap(map);

		Mockito.when(baseService.getCenterId()).thenReturn("10011");
		Mockito.when(baseService.getStationId()).thenReturn("11002");
		Mockito.when(baseService.isInitialSync()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);

		Mockito.when(baseService.getGlobalConfigValueOf(RegistrationConstants.INITIAL_SETUP)).thenReturn(RegistrationConstants.DISABLE);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
	}

	@Test
	public void userDtls() throws RegBaseCheckedException, ConnectionException, JsonProcessingException {
		UserDetailResponseDto userDetail = new UserDetailResponseDto();
		List<UserDetailDto> list = new ArrayList<>();
		UserDetailDto userDetails = new UserDetailDto();
		userDetails.setUserName("110011");
		userDetails.setName("SUPERADMIN");
		userDetails.setRegCenterId("10011");
		list.add(userDetails);
		userDetail.setUserDetails(list);
		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
		LinkedHashMap<String, Object> responseMap=new LinkedHashMap<>();
		Map<String, Object> userDetailsMap = new HashMap<>();
		List<String> rolesList = new ArrayList<>();
		List<Object> userDetailsList = new ArrayList<>();
		rolesList.add("SUPERADMIN");
		userDetailsMap.put("userName", "mosip");
		userDetailsMap.put("mail", "superadmin@mosip.io");
		userDetailsMap.put("mobile", "999999999");
		userDetailsMap.put("userPassword",
				"e1NTSEE1MTJ9MERSeklnR2szMHpTNXJ2aVh6emRrZGdGaU9DWWZjbkVUVW5kNjQ3cXBXK0t1aExoTTNMR0t2LzZ3NUQranNjWmFoS1JGcklhdUJRZGZFRVZkcG82R2gzYVFqNXRUbWVQ");
		userDetailsMap.put("name", "superadmin");
		userDetailsMap.put("roles", rolesList);
		userDetailsMap.put("regCenterId", "10011");
		userDetailsList.add(userDetailsMap);
		Map<String, Object> usrDetailMap = new HashMap<>();
		usrDetailMap.put("userDetails", CryptoUtil.encodeToURLSafeBase64(
				mapper.writeValueAsString(userDetailsList).getBytes()));
		responseMap.put("response", usrDetailMap);
		doNothing().when(userDetailDAO).save(Mockito.any());
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(),Mockito.anyString()))
				.thenReturn(responseMap);
		doNothing().when(baseService).proceedWithMasterAndKeySync(Mockito.any());

		userDetailServiceImpl.save("System");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void userDtlsException() throws RegBaseCheckedException, ConnectionException {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		UserDetailResponseDto userDetail = new UserDetailResponseDto();
		List<UserDetailDto> list = new ArrayList<>();
		UserDetailDto userDetails = new UserDetailDto();
		userDetails.setUserName("110011");
		userDetails.setName("SUPERADMIN");
		list.add(userDetails);
		userDetail.setUserDetails(list);
		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
		doNothing().when(userDetailDAO).save(Mockito.any());
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(),Mockito.anyString()))
				.thenThrow(HttpClientErrorException.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		userDetailServiceImpl.save("System");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void userDtlsException1() throws RegBaseCheckedException, ConnectionException {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		UserDetailResponseDto userDetail = new UserDetailResponseDto();
		List<UserDetailDto> list = new ArrayList<>();
		UserDetailDto userDetails = new UserDetailDto();
		userDetails.setUserName("110011");
		userDetails.setName("SUPERADMIN");
		list.add(userDetails);
		userDetail.setUserDetails(list);
		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
		doNothing().when(userDetailDAO).save(Mockito.any());
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(),Mockito.anyString()))
				.thenThrow(ConnectionException.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		userDetailServiceImpl.save("System");
	}
	
	@Test
	public void userDtlsFail() throws RegBaseCheckedException, ConnectionException, JsonProcessingException {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		UserDetailResponseDto userDetail = new UserDetailResponseDto();
		List<UserDetailDto> list = new ArrayList<>();
		UserDetailDto userDetails = new UserDetailDto();
		userDetails.setUserName("110011");
		userDetails.setName("SUPERADMIN");
		list.add(userDetails);
		userDetail.setUserDetails(list);
		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
		Map<String, String> userDetailErrorMap = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap=new LinkedHashMap<>();
		Map<String, Object> userDetailsMap = new HashMap<>();
		List<String> rolesList = new ArrayList<>();
		List<Object> userDetailsList = new ArrayList<>();
		rolesList.add("SUPERADMIN");
		userDetailsMap.put("userName", "mosip");
		userDetailsMap.put("mail", "superadmin@mosip.io");
		userDetailsMap.put("mobile", "999999999");
		userDetailsMap.put("userPassword",
				"e1NTSEE1MTJ9MERSeklnR2szMHpTNXJ2aVh6emRrZGdGaU9DWWZjbkVUVW5kNjQ3cXBXK0t1aExoTTNMR0t2LzZ3NUQranNjWmFoS1JGcklhdUJRZGZFRVZkcG82R2gzYVFqNXRUbWVQ");
		userDetailsMap.put("name", "superadmin");
		userDetailsMap.put("roles", rolesList);
		userDetailsMap.put("regCenterId", "10011");
		Map<String, Object> usrDetailMap = new HashMap<>();
		usrDetailMap.put("userDetails", CryptoUtil.encodeToURLSafeBase64(
				mapper.writeValueAsString(userDetailsList).getBytes()));
		//usrDetailMap.put("userDetails", userDetailsList);
		responseMap.put("response", usrDetailMap);
		doNothing().when(userDetailDAO).save(Mockito.any());
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(),Mockito.anyString()))
				.thenReturn(responseMap);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		userDetailServiceImpl.save("System");
	}
	
	@Test
	public void userDtlsTestFail() throws RegBaseCheckedException, ConnectionException {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		UserDetailResponseDto userDetail = new UserDetailResponseDto();
		List<UserDetailDto> list = new ArrayList<>();
		UserDetailDto userDetails = new UserDetailDto();
		userDetails.setUserName("110011");
		userDetails.setName("SUPERADMIN");
		list.add(userDetails);
		userDetail.setUserDetails(list);
		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
		LinkedHashMap<String, Object> responseMap=new LinkedHashMap<>();
		Map<String, Object> userDetailsMap = new LinkedHashMap<>();
		userDetailsMap.put("errorCode", "KER-SNC-303");
		userDetailsMap.put("message", "Registration center user not found ");
		List<Map<String, Object>> userFailureList=new ArrayList<>();
		userFailureList.add(userDetailsMap);
		responseMap.put("errors", userFailureList);
		doNothing().when(userDetailDAO).save(Mockito.any());
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(),Mockito.anyString()))
				.thenReturn(responseMap);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		userDetailServiceImpl.save("System");
	}
	
	@Test
	public void userDtlsFailNetwork() throws RegBaseCheckedException, ConnectionException, JsonProcessingException {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		UserDetailResponseDto userDetail = new UserDetailResponseDto();
		List<UserDetailDto> list = new ArrayList<>();
		UserDetailDto userDetails = new UserDetailDto();
		userDetails.setUserName("110011");
		userDetails.setName("SUPERADMIN");
		list.add(userDetails);
		userDetail.setUserDetails(list);
		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");
		Map<String, String> userDetailErrorMap = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap=new LinkedHashMap<>();
		Map<String, Object> userDetailsMap = new HashMap<>();
		List<String> rolesList = new ArrayList<>();
		List<Object> userDetailsList = new ArrayList<>();
		rolesList.add("SUPERADMIN");
		userDetailsMap.put("userName", "mosip");
		userDetailsMap.put("mail", "superadmin@mosip.io");
		userDetailsMap.put("mobile", "999999999");
		userDetailsMap.put("userPassword",
				"e1NTSEE1MTJ9MERSeklnR2szMHpTNXJ2aVh6emRrZGdGaU9DWWZjbkVUVW5kNjQ3cXBXK0t1aExoTTNMR0t2LzZ3NUQranNjWmFoS1JGcklhdUJRZGZFRVZkcG82R2gzYVFqNXRUbWVQ");
		userDetailsMap.put("name", "superadmin");
		userDetailsMap.put("roles", rolesList);
		userDetailsMap.put("regCenterId", "10011");
		Map<String, Object> usrDetailMap = new HashMap<>();
		usrDetailMap.put("userDetails", CryptoUtil.encodeToURLSafeBase64(
				mapper.writeValueAsString(userDetailsList).getBytes()));
		//usrDetailMap.put("userDetails", userDetailsList);
		responseMap.put("response", usrDetailMap);
		doNothing().when(userDetailDAO).save(Mockito.any());
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(),Mockito.anyString()))
				.thenReturn(responseMap);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(false);
		userDetailServiceImpl.save("System");
	}


}
