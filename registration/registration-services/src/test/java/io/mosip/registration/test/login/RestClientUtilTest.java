package io.mosip.registration.test.login;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.datapipeline.model.Field;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.OtpGeneratorRequestDTO;
import io.mosip.registration.dto.OtpGeneratorResponseDTO;
import io.mosip.registration.entity.FileSignature;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.FileSignatureRepository;
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

	private RequestHTTPDTO requestHTTPDTO;

	@Mock
	RestTemplate plainRestTemplate;
	
	@Mock
	private FileSignatureRepository fileSignatureRepository;

	@Before
	public void init() throws Exception {
		OtpGeneratorResponseDTO generatorResponseDto = new OtpGeneratorResponseDTO();
		generatorResponseDto.setOtp("099977");
		OtpGeneratorRequestDTO otpGeneratorRequestDTO = new OtpGeneratorRequestDTO();
		otpGeneratorRequestDTO.setKey("tutuy");
		HttpEntity<?> httpEntity = new HttpEntity<OtpGeneratorRequestDTO>(otpGeneratorRequestDTO);
		URI uri = new URI("https://localhost:8080/otpmanager/otps");
		requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setClazz(OtpGeneratorResponseDTO.class);
		requestHTTPDTO.setHttpEntity(httpEntity);
		requestHTTPDTO.setHttpMethod(HttpMethod.POST);
		requestHTTPDTO.setUri(uri);
		requestHTTPDTO.setRequestBody(otpGeneratorRequestDTO);
		requestHTTPDTO.setHttpHeaders(new HttpHeaders());
		
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.HTTP_API_READ_TIMEOUT, "30");
		appMap.put(RegistrationConstants.HTTP_API_WRITE_TIMEOUT, "30");
		PowerMockito.mockStatic(ApplicationContext.class);
		Mockito.when(ApplicationContext.map()).thenReturn(appMap);
		
		FileSignature fileSignature = new FileSignature();
		//fileSignature.setSignature("");
	    
	    assertEquals(null, fileSignature.getSignature());
		
		
	}


	@SuppressWarnings("unchecked")
	@Test
	public void invokeHeadersTest() throws RegBaseCheckedException, ConnectionException, URISyntaxException {
		ResponseEntity<?> response = new ResponseEntity<>("Success", HttpStatus.OK);
		Mockito.when(plainRestTemplate.exchange(Mockito.any(URI.class), Mockito.any(HttpMethod.class), Mockito.any(HttpEntity.class), Mockito.any(Class.class))).thenReturn(response);
		Assert.assertNotNull(restClientUtil.invokeURL(requestHTTPDTO));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void invokeForTokenTest() throws URISyntaxException {
		ResponseEntity<?> response = new ResponseEntity<>("Success", HttpStatus.OK);
		Mockito.when(plainRestTemplate.exchange(Mockito.any(URI.class), Mockito.any(HttpMethod.class), Mockito.any(HttpEntity.class), Mockito.any(Class.class))).thenReturn(response);
		Assert.assertNotNull(restClientUtil.invokeForToken(requestHTTPDTO));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void isConnectedToSyncServerTest() throws MalformedURLException, URISyntaxException {
		ResponseEntity<?> response = new ResponseEntity<>("Success", HttpStatus.OK);
		Mockito.when(plainRestTemplate.getForEntity(Mockito.any(URI.class), Mockito.any(Class.class))).thenReturn(response);
		Assert.assertTrue(restClientUtil.isConnectedToSyncServer("https://localhost:8080/otpmanager/otps"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void downloadFileTest() throws Exception {
		File file = new File(getClass().getClassLoader().getResource("emptyJson.json").getFile());
		requestHTTPDTO.setFilePath(file.toPath());
		requestHTTPDTO.setFileEncrypted(true);
		FileSignature fileSign = new FileSignature();
		
		fileSign.setContentLength(10);
		Optional<FileSignature> fileSignature = Optional.of(fileSign);
		Mockito.when(fileSignatureRepository.findByFileName(Mockito.anyString())).thenReturn(fileSignature);
		Mockito.when(fileSignatureRepository.save(Mockito.any(FileSignature.class))).thenReturn(new FileSignature());
		Mockito.when(plainRestTemplate.execute(Mockito.any(URI.class), Mockito.any(HttpMethod.class), Mockito.any(RequestCallback.class), Mockito.any(ResponseExtractor.class))).thenReturn(file);
		restClientUtil.downloadFile(requestHTTPDTO);
	}
	
	

}
