package io.mosip.registration.test.util.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.util.restclient.AuthTokenUtilService;
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
import org.springframework.web.client.ResourceAccessException;

import io.mosip.registration.constants.LoginMode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.OtpGeneratorRequestDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.security.impl.AuthenticationServiceImpl;
import io.mosip.registration.util.common.OTPManager;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ RegistrationAppHealthCheckUtil.class, ApplicationContext.class })
public class OTPManagerTest {

	@InjectMocks
	private OTPManager otpManager;

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	@Mock
	private AuthTokenUtilService authTokenUtilService;

	@Mock
	private ClientCryptoFacade clientCryptoFacade;

	@Mock
	private ClientCryptoService clientCryptoService;

	@Before
	public void initialize() throws Exception {
		PowerMockito.mockStatic(ApplicationContext.class);

		Map<String, Object> applicationMap = new HashMap<>();
		applicationMap.put(RegistrationConstants.OTP_CHANNELS, "EMAIL");
		applicationMap.put(RegistrationConstants.REGISTRATION_CLIENT, "registrationclient");

		PowerMockito.doReturn(applicationMap).when(ApplicationContext.class, "map");
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(clientCryptoService.signData(Mockito.any())).thenReturn("test.test.test".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void getOTPSuccessResponseTest()
			throws RegBaseCheckedException, ConnectionException {
		Map<String, String> messageMap = new LinkedHashMap<>();
		HashMap<String, Object> responseMap = new LinkedHashMap<>();
		messageMap.put("message", "otp send succesfully");
		responseMap.put("response", messageMap);
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(authTokenUtilService.sendOtpWithRetryWrapper(Mockito.any())).thenReturn("otp send successfully");
		when(serviceDelegateUtil.post(Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(responseMap);
		assertNotNull(otpManager.getOTP("test").getSuccessResponseDTO());
	}

	@Test
	public void getOTPFailureResponseTest() throws ConnectionException, RegBaseCheckedException {
		OtpGeneratorRequestDTO otpGeneratorRequestDTO = new OtpGeneratorRequestDTO();
		otpGeneratorRequestDTO.setKey("mo");
		List<Map<String, String>> temp = new ArrayList<>();
		Map<String, String> map = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		map.put("message", "Invalid User Id type");
		temp.add(map);
		responseMap.put("errors", temp);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(authTokenUtilService.sendOtpWithRetryWrapper(Mockito.any())).thenThrow(ConnectionException.class);
		when(serviceDelegateUtil.post(Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(responseMap);
		assertNotNull(otpManager.getOTP(otpGeneratorRequestDTO.getKey()).getErrorResponseDTOs());
	}

	@Test
	public void validateOTPSuccessTest()
			throws RegBaseCheckedException {

		AuthTokenDTO authTokenDTO = new AuthTokenDTO();
		authTokenDTO.setCookie("12345");
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(authTokenUtilService.getAuthTokenAndRefreshToken(Mockito.any(LoginMode.class), Mockito.any()))
				.thenReturn(authTokenDTO);

		assertNotNull(otpManager.validateOTP("mosip", "12345", true));
	}

	@Test
	public void validateOTPFailureTest() throws RegBaseCheckedException {

		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST)).when(authTokenUtilService)
				.getAuthTokenAndRefreshToken(Mockito.any(LoginMode.class), Mockito.any());

		assertNull(otpManager.validateOTP("mosip", "12345", true));
	}

	@Test
	public void validateOTPExceptionTest() throws RegBaseCheckedException {

		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);

		Mockito.doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST)).when(authTokenUtilService)
				.getAuthTokenAndRefreshToken(Mockito.any(LoginMode.class), Mockito.any());
		assertNull(otpManager.validateOTP("mosip", "12345", true));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getOTPFailureHTTPTest()
			throws RegBaseCheckedException, ConnectionException {
		OtpGeneratorRequestDTO otpGeneratorRequestDTO = new OtpGeneratorRequestDTO();
		otpGeneratorRequestDTO.setKey("mo");

		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(authTokenUtilService.sendOtpWithRetryWrapper(Mockito.any())).thenThrow(ConnectionException.class);
		when(serviceDelegateUtil.post(Mockito.any(), Mockito.any(),
				Mockito.any())).thenThrow(HttpClientErrorException.class);

		assertNotNull(otpManager.getOTP(otpGeneratorRequestDTO.getKey()).getErrorResponseDTOs());
		assertSame(RegistrationConstants.OTP_GENERATION_ERROR_MESSAGE,
				otpManager.getOTP(otpGeneratorRequestDTO.getKey()).getErrorResponseDTOs().get(0).getMessage());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getOTPFailureIllegalTest()
			throws RegBaseCheckedException, ConnectionException {
		OtpGeneratorRequestDTO otpGeneratorRequestDTO = new OtpGeneratorRequestDTO();
		otpGeneratorRequestDTO.setKey("mo");

		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(false);

		when(serviceDelegateUtil.post(Mockito.any(), Mockito.any(),
				Mockito.any())).thenThrow(IllegalStateException.class);

		assertSame(RegistrationConstants.CONNECTION_ERROR,
				otpManager.getOTP(otpGeneratorRequestDTO.getKey()).getErrorResponseDTOs().get(0).getMessage());

	}

	@Test
	public void getOTPNoInternetTest() {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(false);

		assertEquals(otpManager.getOTP("Key").getErrorResponseDTOs().get(0).getMessage(),
				RegistrationConstants.CONNECTION_ERROR);

	}

	@Test
	public void validateOTPNoInternetTest() {
		PowerMockito.mockStatic(RegistrationAppHealthCheckUtil.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(false);

		assertNotNull(otpManager.validateOTP("Key", "123456", true));

	}

}
