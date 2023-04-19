package io.mosip.registration.test.util.restclient;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.springframework.core.env.Environment;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientException;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.kernel.clientcrypto.util.ClientCryptoUtils;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.LoginUserDTO;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.entity.UserPassword;
import io.mosip.registration.entity.UserToken;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.UserTokenRepository;
import io.mosip.registration.util.restclient.AuthTokenUtilService;
import io.mosip.registration.util.restclient.RestClientUtil;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ApplicationContext.class, SessionContext.class, CryptoUtil.class, ClientCryptoUtils.class })
public class AuthTokenUtilServiceTest {
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	private AuthTokenUtilService authTokenUtilService;
	
	@Mock
	private UserTokenRepository userTokenRepository;
	
	@Mock
    private RetryTemplate retryTemplate;
	
	@Mock
    private ClientCryptoFacade clientCryptoFacade;
	
	@Mock
	private ClientCryptoService clientCryptoService;
	
	@Mock
	private RestClientUtil restClientUtil;
	
	@Mock
	private UserDetailDAO userDetailDAO;
	
	@Mock
	private Environment environment;
	
	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	private String payload = "{\"jti\":\"7c10d55d-b4ac-423b-ad83-973b9a0dccb0\",\"exp\":%d,\"nbf\":0,\"iat\":1642874277,\"iss\":\"https://dev.mosip.net/keycloak/auth/realms/mosip\",\"aud\":\"account\",\"sub\":\"a70ef928-4d0b-4503-8df6-ca197eb8d39c\",\"typ\":\"Bearer\",\"azp\":\"mosip-admin-client\",\"auth_time\":1642874277,\"session_state\":\"dd9b3670-e527-4904-960b-0bf285ce45da\",\"acr\":\"1\",\"allowed-origins\":[\"https://qa-double-rc2.mosip.net\"],\"realm_access\":{\"roles\":[\"REGISTRATION_ADMIN\",\"REGISTRATION_SUPERVISOR\",\"ZONAL_ADMIN\",\"offline_access\",\"uma_authorization\",\"Default\",\"GLOBAL_ADMIN\"]},\"name\":\"Test110006 Auto110006\",\"preferred_username\":\"110006\",\"email\":\"110006@xyz.com\"}";
	
	@Before
	public void init() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		Mockito.when(SessionContext.userId()).thenReturn("110012");
		Map<String,Object> appMap = new HashMap<>();
		appMap.put("mosip.registration.retry.delay.auth", 1000L);
		appMap.put("mosip.registration.retry.maxattempts.auth", 2);
		appMap.put(RegistrationConstants.HTTP_API_READ_TIMEOUT, "1000");
		appMap.put(RegistrationConstants.HTTP_API_WRITE_TIMEOUT, "1000");
		appMap.put(RegistrationConstants.REGISTRATION_CLIENT, "REGISTRATIONCLIENT");
		appMap.put(RegistrationConstants.OTP_CHANNELS, "email");
		LoginUserDTO loginUserDTO = new LoginUserDTO();
		loginUserDTO.setUserId("110012");
		loginUserDTO.setPassword("test-password");
		appMap.put(RegistrationConstants.USER_DTO, loginUserDTO);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
		authTokenUtilService.init();
	}
	
	@Test
	public void init1() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		Mockito.when(SessionContext.userId()).thenReturn("110012");
		Map<String,Object> appMap = new HashMap<>();
		appMap.put("mosip.registration.retry.delay.auth", 1000L);
		appMap.put("mosip.registration.retry.maxattempts.auth", 2);
		appMap.put(RegistrationConstants.HTTP_API_READ_TIMEOUT, "1000");
		appMap.put(RegistrationConstants.HTTP_API_WRITE_TIMEOUT, "1000");
		appMap.put(RegistrationConstants.REGISTRATION_CLIENT, "REGISTRATIONCLIENT");
		appMap.put(RegistrationConstants.OTP_CHANNELS, "email");
		LoginUserDTO loginUserDTO = new LoginUserDTO();
		appMap.put(RegistrationConstants.USER_DTO, loginUserDTO);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
		authTokenUtilService.init();
	}

	@Test
	public void hasAnyValidTokenSuccessTest() {
		Mockito.when(userTokenRepository.findTopByTokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByTokenExpiryDesc(Mockito.anyLong())).thenReturn(null);
		Mockito.when(userTokenRepository.findTopByRtokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByRtokenExpiryDesc(Mockito.anyLong())).thenReturn(new UserToken());
		Assert.assertTrue(authTokenUtilService.hasAnyValidToken());
	}
	
	@Test
	public void hasAnyValidTokenSuccessTest2() {
		Mockito.when(userTokenRepository.findTopByTokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByTokenExpiryDesc(Mockito.anyLong())).thenReturn(new UserToken());
		Assert.assertTrue(authTokenUtilService.hasAnyValidToken());
	}
	
	@Test
	public void hasAnyValidTokenFailureTest() {
		Mockito.when(userTokenRepository.findTopByTokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByTokenExpiryDesc(Mockito.anyLong())).thenReturn(null);
		//Mockito.when(userTokenRepository.findTopByRtokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByRtokenExpiryDesc(Mockito.anyLong())).thenReturn(null);
		Assert.assertFalse(authTokenUtilService.hasAnyValidToken());
	}
	
	@Test
	public void hasAnyValidTokenFailureTest2() {
		Mockito.when(userTokenRepository.findTopByTokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByTokenExpiryDesc(Mockito.anyLong())).thenReturn(null);
		Assert.assertFalse(authTokenUtilService.hasAnyValidToken());
	}
	
	@Test
	public void fetchAuthTokenTest() throws RegBaseCheckedException {
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		
		UserToken userToken = new UserToken();
		userToken.setToken("test-token");
		userToken.setRefreshToken("test-refresh-token");
		userToken.setUsrId("10011");
		userToken.setTokenExpiry(System.currentTimeMillis()/100);
		Mockito.when(userTokenRepository.findByUsrIdAndUserDetailIsActiveTrue(Mockito.anyString())).thenReturn(userToken);	
		Assert.assertNotNull(authTokenUtilService.fetchAuthToken("test").getCookie());
	}
	
	@Test
	public void fetchAuthTokenTest2() throws RegBaseCheckedException {
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		
		UserToken userToken = new UserToken();
		userToken.setToken("test-token");
		userToken.setRefreshToken("test-refresh-token");
		userToken.setUsrId("10011");
		userToken.setTokenExpiry(System.currentTimeMillis()/1000);
		userToken.setRtokenExpiry(System.currentTimeMillis()/100);
		Mockito.when(userTokenRepository.findByUsrIdAndUserDetailIsActiveTrue(Mockito.anyString())).thenReturn(userToken);
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(CryptoUtil.computeFingerPrint(Mockito.any(byte[].class), Mockito.anyString())).thenReturn("test");
		Mockito.when(clientCryptoService.signData(Mockito.any())).thenReturn("test".getBytes());
		Mockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any())).thenReturn("test");
		Mockito.when(serviceDelegateUtil.prepareURLByHostName(Mockito.anyString())).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/useridpwd");
		Mockito.when(environment.getProperty("auth_by_password.service.url")).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/useridpwd");
		Map<String, Object> responseMap = new LinkedHashMap<>();
		Map<String, Object> respBody = new LinkedHashMap<>();
		respBody.put("response", "test-response");
		responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, respBody);
		Mockito.when(restClientUtil.invokeForToken(Mockito.any())).thenReturn(responseMap);

		String testPayload = String.format(payload, (System.currentTimeMillis()/1000)+(2*60));
		testPayload = CryptoUtil.encodeToURLSafeBase64(testPayload.getBytes(StandardCharsets.UTF_8));

		String jsonObjData = "{\n" + 
				"  	\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."+testPayload+".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\",\n" +
				"	\"refreshToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."+testPayload+".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\",\n" +
				"	\"expiryTime\": 1000,\n" + 
				"	\"refreshExpiryTime\": 1000\n" +
				"}";
		Mockito.when(ClientCryptoUtils.decodeBase64Data(Mockito.anyString())).thenReturn("test".getBytes());
		Mockito.when(clientCryptoFacade.decrypt(Mockito.any())).thenReturn(jsonObjData.getBytes());
		Mockito.doNothing().when(userDetailDAO).updateAuthTokens(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong());
		Assert.assertNotNull(authTokenUtilService.fetchAuthToken("test").getCookie());		
	}
	
	@Test
	public void fetchAuthTokenTest3() throws Exception {
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		Mockito.when(userTokenRepository.findTopByTokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByTokenExpiryDesc(Mockito.anyLong())).thenReturn(null);
		Mockito.when(userTokenRepository.findTopByRtokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByRtokenExpiryDesc(Mockito.anyLong())).thenReturn(null);
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(clientCryptoService.signData(Mockito.any())).thenReturn("test".getBytes());
		Mockito.when(serviceDelegateUtil.prepareURLByHostName(Mockito.anyString())).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/useridpwd");
		Mockito.when(environment.getProperty("auth_by_password.service.url")).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/useridpwd");
		Map<String, Object> responseMap = new LinkedHashMap<>();
		Map<String, Object> respBody = new LinkedHashMap<>();
		respBody.put("response", CryptoUtil.encodeToURLSafeBase64("test-response".getBytes(StandardCharsets.UTF_8)));
		responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, respBody);
		Mockito.when(restClientUtil.invokeForToken(Mockito.any())).thenReturn(responseMap);
		String testPayload = String.format(payload, (System.currentTimeMillis()/1000)+(2*60));
		testPayload = CryptoUtil.encodeToURLSafeBase64(testPayload.getBytes(StandardCharsets.UTF_8));

		String jsonObjData = "{\n" +
				"  	\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."+testPayload+".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\",\n" +
				"	\"refreshToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."+testPayload+".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\",\n" +
				"	\"expiryTime\": 1000,\n" +
				"	\"refreshExpiryTime\": 1000\n" +
				"}";

		Mockito.when(clientCryptoFacade.decrypt(Mockito.any())).thenReturn(jsonObjData.getBytes());
		Mockito.doNothing().when(userDetailDAO).updateUserPwd(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(userDetailDAO).updateAuthTokens(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong());
		Assert.assertNotNull(authTokenUtilService.fetchAuthToken("test").getCookie());	
	}
	
	@Test
	public void fetchAuthTokenTest4() throws Exception {
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		UserToken userToken = new UserToken();
		userToken.setToken("test-token");
		userToken.setRefreshToken("test-refresh-token");
		userToken.setUsrId("10011");
		userToken.setTokenExpiry(System.currentTimeMillis()/100);

		Mockito.when(userTokenRepository.findTopByTokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByTokenExpiryDesc(Mockito.anyLong())).thenReturn(userToken);
		Mockito.when(userTokenRepository.findTopByRtokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByRtokenExpiryDesc(Mockito.anyLong())).thenReturn(null);
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(clientCryptoService.signData(Mockito.any())).thenReturn("test".getBytes());
		Mockito.when(serviceDelegateUtil.prepareURLByHostName(Mockito.anyString())).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/useridpwd");
		Mockito.when(environment.getProperty("auth_by_password.service.url")).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/useridpwd");
		Map<String, Object> responseMap = new LinkedHashMap<>();
		Map<String, Object> respBody = new LinkedHashMap<>();
		respBody.put("response", CryptoUtil.encodeToURLSafeBase64("test-response".getBytes(StandardCharsets.UTF_8)));
		responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, respBody);
		Mockito.when(restClientUtil.invokeForToken(Mockito.any())).thenReturn(responseMap);
		String testPayload = String.format(payload, (System.currentTimeMillis()/1000)+(2*60));
		testPayload = CryptoUtil.encodeToURLSafeBase64(testPayload.getBytes(StandardCharsets.UTF_8));

		String jsonObjData = "{\n" +
				"  	\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."+testPayload+".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\",\n" +
				"	\"refreshToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."+testPayload+".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\",\n" +
				"	\"expiryTime\": 1000,\n" +
				"	\"refreshExpiryTime\": 1000\n" +
				"}";

		Mockito.when(clientCryptoFacade.decrypt(Mockito.any())).thenReturn(jsonObjData.getBytes());
		Mockito.doNothing().when(userDetailDAO).updateUserPwd(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(userDetailDAO).updateAuthTokens(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong());
		Assert.assertNotNull(authTokenUtilService.fetchAuthToken("test").getCookie());	
	}


	@Test
	public void fetchAuthTokenTest5() throws Exception {
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(false);
		UserToken userToken1 = new UserToken();
		userToken1.setToken("test-token");
		userToken1.setRefreshToken("test-refresh-token");
		userToken1.setUsrId("10011");
		userToken1.setTokenExpiry(System.currentTimeMillis()/100);

		Mockito.when(userTokenRepository.findTopByTokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByTokenExpiryDesc(Mockito.anyLong())).thenReturn(null);
		Mockito.when(userTokenRepository.findTopByRtokenExpiryGreaterThanAndUserDetailIsActiveTrueOrderByRtokenExpiryDesc(Mockito.anyLong())).thenReturn(userToken1);
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(clientCryptoService.signData(Mockito.any())).thenReturn("test".getBytes());
		Mockito.when(serviceDelegateUtil.prepareURLByHostName(Mockito.anyString())).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/useridpwd");
		Mockito.when(environment.getProperty("auth_by_password.service.url")).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/useridpwd");
		Map<String, Object> responseMap = new LinkedHashMap<>();
		Map<String, Object> respBody = new LinkedHashMap<>();
		respBody.put("response", CryptoUtil.encodeToURLSafeBase64("test-response".getBytes(StandardCharsets.UTF_8)));
		responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, respBody);
		Mockito.when(restClientUtil.invokeForToken(Mockito.any())).thenReturn(responseMap);
		String testPayload = String.format(payload, (System.currentTimeMillis()/1000)+(2*60));
		testPayload = CryptoUtil.encodeToURLSafeBase64(testPayload.getBytes(StandardCharsets.UTF_8));

		String jsonObjData = "{\n" +
				"  	\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."+testPayload+".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\",\n" +
				"	\"refreshToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."+testPayload+".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\",\n" +
				"	\"expiryTime\": 1000,\n" +
				"	\"refreshExpiryTime\": 1000\n" +
				"}";

		Mockito.when(clientCryptoFacade.decrypt(Mockito.any())).thenReturn(jsonObjData.getBytes());
		Mockito.doNothing().when(userDetailDAO).updateUserPwd(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(userDetailDAO).updateAuthTokens(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong());
		Assert.assertNotNull(authTokenUtilService.fetchAuthToken("test").getCookie());	
	}

	
	@Test
	public void sendOtpTest() throws ExhaustedRetryException, Throwable {
		Mockito.when(retryTemplate.execute(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            RetryCallback retry = invocation.getArgument(0);
            return retry.doWithRetry(null);
        });
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(CryptoUtil.computeFingerPrint(Mockito.any(byte[].class), Mockito.anyString())).thenReturn("test");
		Mockito.when(clientCryptoService.signData(Mockito.any())).thenReturn("test".getBytes());
		Mockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any())).thenReturn("test");
		Mockito.when(serviceDelegateUtil.prepareURLByHostName(Mockito.anyString())).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/sendotp");
		Mockito.when(environment.getProperty("auth_by_otp.service.url")).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/sendotp");
		Map<String, Object> responseMap = new LinkedHashMap<>();
		Map<String, Object> respBody = new LinkedHashMap<>();
		LinkedHashMap<String, String> otpMessage = new LinkedHashMap<>();
		otpMessage.put("message", null);
		respBody.put("response", otpMessage);
		responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, respBody);
		Mockito.when(restClientUtil.invokeForToken(Mockito.any())).thenReturn(responseMap);
		Assert.assertNotNull(authTokenUtilService.sendOtpWithRetryWrapper("test"));
	}
	
	@Test
	public void sendOtpFailureTest() throws ExhaustedRetryException, RestClientException, Throwable {
		Mockito.when(retryTemplate.execute(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            RetryCallback retry = invocation.getArgument(0);
            return retry.doWithRetry(null);
        });
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(CryptoUtil.computeFingerPrint(Mockito.any(byte[].class), Mockito.anyString())).thenReturn("test");
		Mockito.when(clientCryptoService.signData(Mockito.any())).thenReturn("test".getBytes());
		Mockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any())).thenReturn("test");
		Mockito.when(serviceDelegateUtil.prepareURLByHostName(Mockito.anyString())).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/sendotp");
		Mockito.when(environment.getProperty("auth_by_otp.service.url")).thenReturn("https://dev.mosip.net/v1/syncdata/authenticate/sendotp");
		Map<String, Object> responseMap = new LinkedHashMap<>();
		Map<String, Object> respBody = new LinkedHashMap<>();
		LinkedHashMap<String, String> otpMessage = new LinkedHashMap<>();
		otpMessage.put("message", "OTP Sent");
		respBody.put("response", otpMessage);
		responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, respBody);
		Mockito.when(restClientUtil.invokeForToken(Mockito.any())).thenThrow(RestClientException.class);

		//Assert.assertNotNull(authTokenUtilService.sendOtpWithRetryWrapper("test"));
	}

}
