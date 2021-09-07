package io.mosip.registration.test.login;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import org.junit.*;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.*;

import io.mosip.registration.dto.OtpGeneratorRequestDTO;
import io.mosip.registration.dto.OtpGeneratorResponseDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.util.restclient.RequestHTTPDTO;
import io.mosip.registration.util.restclient.RestClientUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ApplicationContext.class })
public class RestClientUtilTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	RestClientUtil restClientUtil;

	@Mock
	RequestHTTPDTO requestHTTPDTO;

	@Mock
	RestTemplate plainRestTemplate;

	@Before
	public void init() throws Exception {
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.HTTP_API_READ_TIMEOUT, "30");
		appMap.put(RegistrationConstants.HTTP_API_WRITE_TIMEOUT, "30");
		PowerMockito.mockStatic(ApplicationContext.class);
		Mockito.when(ApplicationContext.map()).thenReturn(appMap);
	}


	@Test
	public void invokeHeadersTest() throws RegBaseCheckedException, ConnectionException, URISyntaxException {
		OtpGeneratorResponseDTO generatorResponseDto = new OtpGeneratorResponseDTO();
		generatorResponseDto.setOtp("099977");
		OtpGeneratorRequestDTO otpGeneratorRequestDTO = new OtpGeneratorRequestDTO();
		otpGeneratorRequestDTO.setKey("tutuy");
		HttpEntity<?> httpEntity = new HttpEntity<OtpGeneratorRequestDTO>(otpGeneratorRequestDTO);
		URI uri = new URI("https://localhost:8080/otpmanager/otps");
		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setClazz(OtpGeneratorResponseDTO.class);
		requestHTTPDTO.setHttpEntity(httpEntity);
		requestHTTPDTO.setHttpMethod(HttpMethod.POST);
		requestHTTPDTO.setUri(uri);
		Assert.assertNull(restClientUtil.invokeURL(requestHTTPDTO));
	}

}
