package io.mosip.registration.test.service;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.registration.dto.ResponseDTO;
import org.junit.Assert;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.UserDetailDto;
import io.mosip.registration.dto.UserDetailResponseDto;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserRole;
import io.mosip.registration.entity.id.UserRoleId;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.service.operator.impl.UserDetailServiceImpl;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.test.config.TestClientCryptoServiceImpl;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ RegistrationAppHealthCheckUtil.class,UserDetailDAO.class, ApplicationContext.class, SessionContext.class, CryptoUtil.class })
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
	
	@Mock
	private ObjectMapper objectMapper;

	private ObjectMapper mapper = new ObjectMapper();

	String[] input = {"testUser1", "testUser2"};


	@Before
	public void init() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class, RegistrationAppHealthCheckUtil.class, SessionContext.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(SessionContext.userId()).thenReturn("110011");
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		List<String> roles = new ArrayList<>();
		roles.add("REGISTRATION_OFFICER");
		userContext.setRoles(roles);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");

		ClientCryptoService mockedClientCryptoService = mock(TestClientCryptoServiceImpl.class);
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(mockedClientCryptoService);
		Mockito.when(clientCryptoFacade.decrypt(Mockito.any())).thenReturn("[]".getBytes(StandardCharsets.UTF_8));

		Map<String, Object> map = new HashMap<>();
		map.put(RegistrationConstants.MACHINE_CENTER_REMAP_FLAG, false);
		ApplicationContext.setApplicationMap(map);

		Mockito.when(baseService.getCenterId()).thenReturn("10011");
		Mockito.when(baseService.getStationId()).thenReturn("11002");
		Mockito.when(baseService.isInitialSync()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);

		Mockito.when(baseService.getGlobalConfigValueOf(RegistrationConstants.INITIAL_SETUP)).thenReturn(RegistrationConstants.DISABLE);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
	}

	@SuppressWarnings("unchecked")
    @Test
    public void userDtls() throws Exception {
        // Mock static
        PowerMockito.mockStatic(CryptoUtil.class);
        PowerMockito.when(CryptoUtil.decodeURLSafeBase64(Mockito.anyString()))
                .thenReturn("[{\"userId\":\"110011\"}]".getBytes());

        // Prepare input list
        List<UserDetailDto> list = new ArrayList<>();
        UserDetailDto userDetails = new UserDetailDto();
        userDetails.setUserId("110011");
        userDetails.setRegCenterId("10011");
        list.add(userDetails);

        // Prepare DAO existing user
        List<UserDetail> existingUserDetails = new ArrayList<>();
        UserDetail existing = new UserDetail();
        existing.setId("110012"); // should get deleted
        existingUserDetails.add(existing);

        // Stub dependencies
        Mockito.when(clientCryptoFacade.decrypt(Mockito.any()))
                .thenReturn("[{\"userId\":\"110011\"}]".getBytes());
        Mockito.when(objectMapper.readValue(Mockito.any(byte[].class), Mockito.any(TypeReference.class)))
                .thenReturn(list);
        Mockito.when(userDetailDAO.getAllUsers()).thenReturn(existingUserDetails);
        Mockito.doNothing().when(userDetailDAO).deleteUser(Mockito.any());
        Mockito.doNothing().when(userDetailDAO).save(Mockito.any());
        Mockito.doNothing().when(baseService).proceedWithMasterAndKeySync(Mockito.any());

        Map<String, Object> usrDetailMap = new LinkedHashMap<>();
        usrDetailMap.put("userDetails", "dummyBase64"); // value doesn’t matter, gets mocked
        LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("response", usrDetailMap);

        Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyString()))
                .thenReturn(responseMap);

        // Call method
        ResponseDTO response = userDetailServiceImpl.save("System");

        // Assertions
        assertNotNull(response);

        // Verify interactions
        Mockito.verify(userDetailDAO).deleteUser(Mockito.any(UserDetail.class));
        Mockito.verify(userDetailDAO).save(Mockito.any(UserDetailDto.class));
    }



	@Test
	public void HttpClientErrorExceptionHandled() throws Exception {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.mockStatic(CryptoUtil.class);

		PowerMockito.when(CryptoUtil.decodeURLSafeBase64(Mockito.anyString()))
				.thenReturn("dummy".getBytes());

		Mockito.when(serviceDelegateUtil.get(
						Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyString()))
				.thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);

		userDetailServiceImpl.save("System");
	}



	@Test
	public void userDtlsException1() throws Exception {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.mockStatic(CryptoUtil.class); // 👈 add this

		List<UserDetailDto> list = new ArrayList<>();
		UserDetailDto userDetails = new UserDetailDto();
		userDetails.setUserId("110011");
		list.add(userDetails);

		doNothing().when(userDetailDAO).save(Mockito.any());

		Mockito.when(serviceDelegateUtil.get(
						Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyString()))
				.thenThrow(new ConnectionException());

		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);

		// 👇 Fix: stub CryptoUtil and decrypt so data is not null
		Mockito.when(CryptoUtil.decodeURLSafeBase64(Mockito.anyString()))
				.thenReturn("dummy".getBytes());
		Mockito.when(clientCryptoFacade.decrypt(Mockito.any(byte[].class)))
				.thenReturn("some-json".getBytes());

		// Now Jackson gets non-null byte[]
		Mockito.when(objectMapper.readValue(
						Mockito.any(byte[].class), Mockito.any(TypeReference.class)))
				.thenReturn(list);

		userDetailServiceImpl.save("System");  // act
	}





	@SuppressWarnings("unchecked")
	@Test
	public void userDtlsFail() throws Exception {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		PowerMockito.mockStatic(CryptoUtil.class); // 

		UserDetailResponseDto userDetail = new UserDetailResponseDto();
		List<UserDetailDto> list = new ArrayList<>();
		UserDetailDto userDetails = new UserDetailDto();
		userDetails.setUserId("110011");
		list.add(userDetails);
		userDetail.setUserDetails(list);

		Map<String, String> map = new HashMap<>();
		map.put(RegistrationConstants.USER_CENTER_ID, "10011");

		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		Map<String, Object> userDetailsMap = new LinkedHashMap<>();
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

		Map<String, Object> usrDetailMap = new LinkedHashMap<>();
		usrDetailMap.put("userDetails", CryptoUtil.encodeToURLSafeBase64(
				mapper.writeValueAsString(userDetailsList).getBytes()));
		responseMap.put("response", usrDetailMap);

		// ✅ Fix: stub CryptoUtil and decrypt
		Mockito.when(CryptoUtil.decodeURLSafeBase64(Mockito.anyString()))
				.thenReturn("dummy".getBytes());
		Mockito.when(clientCryptoFacade.decrypt(Mockito.any(byte[].class)))
				.thenReturn("some-json".getBytes());

		// ✅ Fix: mock byte[] variant of readValue
		Mockito.when(objectMapper.readValue(Mockito.any(byte[].class), Mockito.any(TypeReference.class)))
				.thenReturn(list);

		doNothing().when(userDetailDAO).save(Mockito.any());
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyString()))
				.thenReturn(responseMap);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);

		userDetailServiceImpl.save("System");  // act
	}

	@Test
	public void userDtlsTestFail() throws Exception {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(RegistrationAppHealthCheckUtil.isDiskSpaceAvailable()).thenReturn(true);

		LinkedHashMap<String, Object> serviceResponse = new LinkedHashMap<>();

		// errors
		Map<String, Object> errorDetails = new LinkedHashMap<>();
		errorDetails.put("errorCode", "KER-SNC-303");
		errorDetails.put("message", "Registration center user not found");
		List<Map<String, Object>> errorList = new ArrayList<>();
		errorList.add(errorDetails);
		serviceResponse.put("errors", errorList);

		// 👇 Force RESPONSE to null, not a map
		serviceResponse.put(RegistrationConstants.RESPONSE, null);

		Mockito.when(serviceDelegateUtil.get(
						Mockito.anyString(),
						Mockito.any(),
						Mockito.anyBoolean(),
						Mockito.anyString()))
				.thenReturn(serviceResponse);

		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);

		userDetailServiceImpl.save("System");

		// verify save() never called
		Mockito.verify(userDetailDAO, Mockito.never()).save(Mockito.any());
	}


	@Test
	public void userDtlsFailNetwork() throws RegBaseCheckedException, ConnectionException, JsonProcessingException {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		UserDetailResponseDto userDetail = new UserDetailResponseDto();
		List<UserDetailDto> list = new ArrayList<>();
		UserDetailDto userDetails = new UserDetailDto();
		userDetails.setUserId("110011");
		//userDetails.setName("SUPERADMIN");
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

	@Test
	public void getAllUsersTest() {
		List<UserDetail> existingUserDetails = new ArrayList<>();
		UserDetail user = new UserDetail();
		user.setId("110012");
		existingUserDetails.add(user);
		Mockito.when(userDetailDAO.getAllUsers()).thenReturn(existingUserDetails);
		Assert.assertNotNull(userDetailServiceImpl.getAllUsers());
	}
	
	@Test
	public void getUserRoleByUserIdTest() {
		List<UserDetail> existingUserDetails = new ArrayList<>();
		UserDetail user = new UserDetail();
		user.setId("110012");
		existingUserDetails.add(user);
		List<UserRole> userRoles = new ArrayList<>();
		UserRole userRole = new UserRole();
		UserRoleId roleId = new UserRoleId();
		roleId.setUsrId("110012");
		roleId.setRoleCode("SUPERVISOR");
		userRole.setUserRoleId(roleId);
		userRoles.add(userRole);
		Mockito.when(userDetailDAO.getUserRoleByUserId(Mockito.anyString())).thenReturn(userRoles);
		Assert.assertNotNull(userDetailServiceImpl.getUserRoleByUserId("110012"));
	}
	
	@Test
	public void isValidUserTest() {
		UserDetail user = new UserDetail();
		user.setId("110012");
		Mockito.when(userDetailDAO.getUserDetail(Mockito.anyString())).thenReturn(user);
		Assert.assertTrue(userDetailServiceImpl.isValidUser("110012"));
	}
	
	@Test
	public void isValidUserFailureTest() {
		Mockito.when(userDetailDAO.getUserDetail(Mockito.anyString())).thenReturn(null);
		Assert.assertFalse(userDetailServiceImpl.isValidUser("110012"));
	}
	
	@Test
	public void checkLoggedInUserRolesModifiedTest() {
		List<String> roles = java.util.Arrays.asList("REGISTRATION_OFFICER", "REGISTRATION_SUPERVISOR");
		Assert.assertNotNull(userDetailServiceImpl.checkLoggedInUserRoles(roles));
	}
}
