package io.mosip.registration.test.util.restclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.crypto.SecretKey;

import io.mosip.commons.packet.constants.CryptomanagerConstant;
import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.kernel.keymanagerservice.dto.KeyPairGenerateResponseDto;
import io.mosip.kernel.keymanagerservice.dto.UploadCertificateRequestDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.kernel.signature.constant.SignatureConstant;
import io.mosip.kernel.signature.dto.JWTSignatureResponseDto;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyResponseDto;
import io.mosip.kernel.signature.dto.ValidatorResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import io.mosip.registration.service.sync.impl.PublicKeySyncImpl;
import org.aspectj.lang.JoinPoint;
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
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.signatureutil.spi.SignatureUtil;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.keygenerator.bouncycastle.KeyGenerator;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.entity.FileSignature;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.FileSignatureRepository;
import io.mosip.registration.util.advice.ResponseSignatureAdvice;
import io.mosip.registration.util.restclient.RequestHTTPDTO;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ApplicationContext.class })
public class ResponseSignatureAdviceTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	private ResponseSignatureAdvice responseSignatureAdvice;

	@Mock
	RestTemplate restTemplate;

	@Mock
	SignatureService signatureService;

	@Mock
	private JoinPoint joinPointMock;

	@Mock
	private ClientCryptoService clientCryptoService;

	@Mock
	private ClientCryptoFacade clientCryptoFacade;

	@Mock
	private KeymanagerService keymanagerService;

	@Mock
	private PublicKeySyncImpl publicKeySync;
	
	@Mock
	private CryptoCoreSpec cryptoCore;
	
	@Mock
	private FileSignatureRepository fileSignatureRepository;
	


	@Before
	public void init() throws Exception {
		PowerMockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		ReflectionTestUtils.setField(responseSignatureAdvice, "DATETIME_PATTERN", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		ReflectionTestUtils.setField(responseSignatureAdvice, "signRefId", "SIGN");

		KeyPairGenerateResponseDto certificateDto = new KeyPairGenerateResponseDto();
		certificateDto.setCertificate("test");
		Mockito.when(keymanagerService.getCertificate(RegistrationConstants.RESPONSE_SIGNATURE_PUBLIC_KEY_APP_ID,
				Optional.of(RegistrationConstants.RESPONSE_SIGNATURE_PUBLIC_KEY_REF_ID)))
				.thenReturn(certificateDto);
	}

	@Test
	public void responseSignatureTest()
			throws RegBaseCheckedException, URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException {

		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setIsSignRequired(true);
		requestHTTPDTO.setUri(new URI("/v1/mosip/test"));
		Object[] args = new Object[1];
		args[0] = requestHTTPDTO;
		Mockito.when(joinPointMock.getArgs()).thenReturn(args);
		Map<String, Object> mapResponse = new LinkedHashMap<>();
		mapResponse.put("lastSyncTime", "2019-04-23T06:20:28.633Z");
		mapResponse.put("publicKey", null);
		mapResponse.put("issuedAt", null);
		mapResponse.put("expiryAt", null);
		Map<String, Object> linkedMapResponse = new LinkedHashMap<>();
		linkedMapResponse.put("id", null);
		linkedMapResponse.put("version", null);
		linkedMapResponse.put("responsetime", "2019-04-23T06:20:28.660Z");
		linkedMapResponse.put("metadata", null);
		linkedMapResponse.put("response", mapResponse);
		linkedMapResponse.put("errors", null);
		HttpHeaders header = new HttpHeaders();
		header.add("response-signature",
				" S6or4K8KD_bqdiDN-UjtyBSI-LPpm800xJF7VKsXIRcnf3z4MV5EbcBGoqc_OcstF6J1FYLTI5uCsonTIj7m4mNnf1H7jOTlZKErjBw0sDSt2PiLSVJdE642SRjD8RXEZGWl_BqGel5PyWfHnBP5Cmmflrtb2oXI8CqEoU7YDwXfcr0wNhy1mtlHpKQx9O82HqhHy59S7iMcBcdIE46rhm7sJkrnOYOU6hwcuGiOYZvbl_y_iOUn5HEZX_41iycQ5PZADDIngF8zJhLOAs1OS9MfJfaTBMtsvKwzfp3NGw6OXoAymYVlykCldCjDOIz6AlM2noKBz0vpc6i8Lxglhg");

		Map<String, Object> linkedMap = new LinkedHashMap<>();
		linkedMap.put(RegistrationConstants.REST_RESPONSE_BODY, linkedMapResponse);
		linkedMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, header);

		JWTSignatureVerifyResponseDto jwtSignatureResponseDto = new JWTSignatureVerifyResponseDto();
		jwtSignatureResponseDto.setSignatureValid(true);
		Mockito.when(signatureService.jwtVerify(Mockito.any())).thenReturn(jwtSignatureResponseDto);
		responseSignatureAdvice.responseSignatureValidation(joinPointMock, linkedMap);
	}

	@Test
	public void responseSignatureTestCaseFail()
			throws RegBaseCheckedException, URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException {

		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setIsSignRequired(true);
		requestHTTPDTO.setUri(new URI("/v1/mosip/test"));
		Object[] args = new Object[1];
		args[0] = requestHTTPDTO;
		Mockito.when(joinPointMock.getArgs()).thenReturn(args);
		Map<String, Object> mapResponse = new LinkedHashMap<>();
		mapResponse.put("lastSyncTime", "2019-04-23T06:20:28.633Z");
		mapResponse.put("publicKey", null);
		mapResponse.put("issuedAt", null);
		mapResponse.put("expiryAt", null);
		Map<String, Object> linkedMapResponse = new LinkedHashMap<>();
		linkedMapResponse.put("id", null);
		linkedMapResponse.put("version", null);
		linkedMapResponse.put("responsetime", "2019-04-23T06:20:28.660Z");
		linkedMapResponse.put("metadata", null);
		linkedMapResponse.put("response", mapResponse);
		linkedMapResponse.put("errors", null);

		HttpHeaders header = new HttpHeaders();
		header.add("response-signature",
				" S6or4K8KD_bqdiDN-UjtyBSI-LPpm800xJF7VKsXIRcnf3z4MV5EbcBGoqc_OcstF6J1FYLTI5uCsonTIj7m4mNnf1H7jOTlZKErjBw0sDSt2PiLSVJdE642SRjD8RXEZGWl_BqGel5PyWfHnBP5Cmmflrtb2oXI8CqEoU7YDwXfcr0wNhy1mtlHpKQx9O82HqhHy59S7iMcBcdIE46rhm7sJkrnOYOU6hwcuGiOYZvbl_y_iOUn5HEZX_41iycQ5PZADDIngF8zJhLOAs1OS9MfJfaTBMtsvKwzfp3NGw6OXoAymYVlykCldCjDOIz6AlM2noKBz0vpc6i8Lxglhg");

		Map<String, Object> linkedMap = new LinkedHashMap<>();
		linkedMap.put(RegistrationConstants.REST_RESPONSE_BODY, linkedMapResponse);
		linkedMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, header);

		JWTSignatureVerifyResponseDto jwtSignatureResponseDto = new JWTSignatureVerifyResponseDto();
		jwtSignatureResponseDto.setSignatureValid(false);
		Mockito.when(signatureService.jwtVerify(Mockito.any())).thenReturn(jwtSignatureResponseDto);

		responseSignatureAdvice.responseSignatureValidation(joinPointMock, linkedMap);

	}

	@Test
	public void responseSignatureTestFalse()
			throws RegBaseCheckedException, URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException {

		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setIsSignRequired(true);
		requestHTTPDTO.setUri(new URI("/v1/mosip/test"));
		Object[] args = new Object[1];
		args[0] = requestHTTPDTO; 
		Mockito.when(joinPointMock.getArgs()).thenReturn(args);
		Map<String, Object> mapResponse = new LinkedHashMap<>();
		mapResponse.put("lastSyncTime", "2019-04-23T06:20:28.633Z");
		mapResponse.put("publicKey", null);
		mapResponse.put("issuedAt", null);
		mapResponse.put("expiryAt", null);
		Map<String, Object> linkedMapResponse = new LinkedHashMap<>();
		linkedMapResponse.put("id", null);
		linkedMapResponse.put("version", null);
		linkedMapResponse.put("responsetime", "2019-04-23T06:20:28.660Z");
		linkedMapResponse.put("metadata", null);
		linkedMapResponse.put("response", mapResponse);
		linkedMapResponse.put("errors", null);
		HttpHeaders header = new HttpHeaders();
		header.add("response-signature",
				" S6or4K8KD_bqdiDN-UjtyBSI-LPpm800xJF7VKsXIRcnf3z4MV5EbcBGoqc_OcstF6J1FYLTI5uCsonTIj7m4mNnf1H7jOTlZKErjBw0sDSt2PiLSVJdE642SRjD8RXEZGWl_BqGel5PyWfHnBP5Cmmflrtb2oXI8CqEoU7YDwXfcr0wNhy1mtlHpKQx9O82HqhHy59S7iMcBcdIE46rhm7sJkrnOYOU6hwcuGiOYZvbl_y_iOUn5HEZX_41iycQ5PZADDIngF8zJhLOAs1OS9MfJfaTBMtsvKwzfp3NGw6OXoAymYVlykCldCjDOIz6AlM2noKBz0vpc6i8Lxglhg");

		Map<String, Object> linkedMap = new LinkedHashMap<>();
		linkedMap.put(RegistrationConstants.REST_RESPONSE_BODY, linkedMapResponse);
		linkedMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, header);

		JWTSignatureVerifyResponseDto jwtSignatureResponseDto = new JWTSignatureVerifyResponseDto();
		jwtSignatureResponseDto.setSignatureValid(false);
		Mockito.when(signatureService.jwtVerify(Mockito.any())).thenReturn(jwtSignatureResponseDto);

		responseSignatureAdvice.responseSignatureValidation(joinPointMock, linkedMap);

	}

	@Test
	public void responseSignatureTestNewKey()
			throws RegBaseCheckedException, URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException {

		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setIsSignRequired(true);
		requestHTTPDTO.setUri(new URI("/v1/mosip/test"));
		Object[] args = new Object[1];
		args[0] = requestHTTPDTO;
		Mockito.when(joinPointMock.getArgs()).thenReturn(args);
		Map<String, Object> mapResponse = new LinkedHashMap<>();
		mapResponse.put("lastSyncTime", "2019-04-23T06:20:28.633Z");
		mapResponse.put("publicKey",
				"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgIxusCzIYkOkWjG65eeLGNSXoNghIiH1wj1lxW1ZGqr35gM4od_5MXTmRAVamgFlPko8zfFgli-h0c2yLsPbPC2IGrHLB0FQp_MaCAst2xzQvG73nAr8Fkh-geJJ0KRvZE6TCYXNdRVczHfcxctyS4PGHCrHYv6GURzDlQ5SGmXko-xA92ULxpVrD-mYlZ7uOvr92dRJGR15p-D7cNXdBWwpc812aKTwYpHd719fryXrQ4JDrdeNXsjn7Q9BlehObc_MdAn1q3glsfx_VkuYhctT-vOEHiynkKfPlSMRd041U6pGNKgoqEuyvUlTRT7SgZQgzV9m0MEhWP9peehliQIDAQAB");
		mapResponse.put("issuedAt", null);
		mapResponse.put("expiryAt", null);
		Map<String, Object> linkedMapResponse = new LinkedHashMap<>();
		linkedMapResponse.put("id", null);
		linkedMapResponse.put("version", null);
		linkedMapResponse.put("responsetime", "2019-04-23T06:20:28.660Z");
		linkedMapResponse.put("metadata", null);
		linkedMapResponse.put("response", mapResponse);
		linkedMapResponse.put("errors", null);
		HttpHeaders header = new HttpHeaders();
		header.add("response-signature",
				" S6or4K8KD_bqdiDN-UjtyBSI-LPpm800xJF7VKsXIRcnf3z4MV5EbcBGoqc_OcstF6J1FYLTI5uCsonTIj7m4mNnf1H7jOTlZKErjBw0sDSt2PiLSVJdE642SRjD8RXEZGWl_BqGel5PyWfHnBP5Cmmflrtb2oXI8CqEoU7YDwXfcr0wNhy1mtlHpKQx9O82HqhHy59S7iMcBcdIE46rhm7sJkrnOYOU6hwcuGiOYZvbl_y_iOUn5HEZX_41iycQ5PZADDIngF8zJhLOAs1OS9MfJfaTBMtsvKwzfp3NGw6OXoAymYVlykCldCjDOIz6AlM2noKBz0vpc6i8Lxglhg");

		Map<String, Object> linkedMap = new LinkedHashMap<>();
		linkedMap.put(RegistrationConstants.REST_RESPONSE_BODY, linkedMapResponse);
		linkedMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, header);

		JWTSignatureVerifyResponseDto jwtSignatureResponseDto = new JWTSignatureVerifyResponseDto();
		jwtSignatureResponseDto.setSignatureValid(true);
		Mockito.when(signatureService.jwtVerify(Mockito.any())).thenReturn(jwtSignatureResponseDto);

		responseSignatureAdvice.responseSignatureValidation(joinPointMock, linkedMap);

	}

	@Test
	public void responseSignatureTestFail() throws RegBaseCheckedException, URISyntaxException {

		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setIsSignRequired(false);
		requestHTTPDTO.setUri(new URI("/v1/mosip/test"));
		Object[] args = new Object[1];
		args[0] = requestHTTPDTO;
		Mockito.when(joinPointMock.getArgs()).thenReturn(args);
		Map<String, Object> mapResponse = new LinkedHashMap<>();
		mapResponse.put("lastSyncTime", "2019-04-23T06:20:28.633Z");
		mapResponse.put("publicKey", null);
		mapResponse.put("issuedAt", null);
		mapResponse.put("expiryAt", null);
		Map<String, Object> linkedMapResponse = new LinkedHashMap<>();
		linkedMapResponse.put("id", null);
		linkedMapResponse.put("version", null);
		linkedMapResponse.put("responsetime", "2019-04-23T06:20:28.660Z");
		linkedMapResponse.put("metadata", null);
		linkedMapResponse.put("response", mapResponse);
		linkedMapResponse.put("errors", null);
		Map<String, Object> linkedMapHeader = new LinkedHashMap<>();
		linkedMapHeader.put("pragma", "no-cache");
		linkedMapHeader.put("response-signature",
				" S6or4K8KD_bqdiDN-UjtyBSI-LPpm800xJF7VKsXIRcnf3z4MV5EbcBGoqc_OcstF6J1FYLTI5uCsonTIj7m4mNnf1H7jOTlZKErjBw0sDSt2PiLSVJdE642SRjD8RXEZGWl_BqGel5PyWfHnBP5Cmmflrtb2oXI8CqEoU7YDwXfcr0wNhy1mtlHpKQx9O82HqhHy59S7iMcBcdIE46rhm7sJkrnOYOU6hwcuGiOYZvbl_y_iOUn5HEZX_41iycQ5PZADDIngF8zJhLOAs1OS9MfJfaTBMtsvKwzfp3NGw6OXoAymYVlykCldCjDOIz6AlM2noKBz0vpc6i8Lxglhg");

		Map<String, Object> linkedMap = new LinkedHashMap<>();
		linkedMap.put(RegistrationConstants.REST_RESPONSE_BODY, linkedMapResponse);
		linkedMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, linkedMapHeader);

		responseSignatureAdvice.responseSignatureValidation(joinPointMock, linkedMap);

	}

	@Test(expected = Throwable.class)
	public void responseSignatureTestException() throws RegBaseCheckedException, URISyntaxException {

		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setIsSignRequired(true);
		requestHTTPDTO.setUri(new URI("/v1/mosip/test"));
		Object[] args = new Object[1];
		args[0] = requestHTTPDTO;
		Mockito.when(joinPointMock.getArgs()).thenReturn(args);
		Map<String, Object> mapResponse = new LinkedHashMap<>();
		mapResponse.put("lastSyncTime", "2019-04-23T06:20:28.633Z");
		mapResponse.put("publicKey", null);
		mapResponse.put("issuedAt", null);
		mapResponse.put("expiryAt", null);
		Map<String, Object> linkedMapResponse = new LinkedHashMap<>();
		linkedMapResponse.put("id", null);
		linkedMapResponse.put("version", null);
		linkedMapResponse.put("responsetime", "2019-04-23T06:20:28.660Z");
		linkedMapResponse.put("metadata", null);
		linkedMapResponse.put("response", mapResponse);
		linkedMapResponse.put("errors", null);
		Map<String, Object> linkedMapHeader = new LinkedHashMap<>();
		linkedMapHeader.put("pragma", "no-cache");
		linkedMapHeader.put("response-signature",
				" S6or4K8KD_bqdiDN-UjtyBSI-LPpm800xJF7VKsXIRcnf3z4MV5EbcBGoqc_OcstF6J1FYLTI5uCsonTIj7m4mNnf1H7jOTlZKErjBw0sDSt2PiLSVJdE642SRjD8RXEZGWl_BqGel5PyWfHnBP5Cmmflrtb2oXI8CqEoU7YDwXfcr0wNhy1mtlHpKQx9O82HqhHy59S7iMcBcdIE46rhm7sJkrnOYOU6hwcuGiOYZvbl_y_iOUn5HEZX_41iycQ5PZADDIngF8zJhLOAs1OS9MfJfaTBMtsvKwzfp3NGw6OXoAymYVlykCldCjDOIz6AlM2noKBz0vpc6i8Lxglhg");

		Map<String, Object> linkedMap = new LinkedHashMap<>();
		linkedMap.put(RegistrationConstants.REST_RESPONSE_BODY, linkedMapResponse);
		linkedMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, linkedMapHeader);

		responseSignatureAdvice.responseSignatureValidation(joinPointMock, linkedMap);

	}

	@Test
	public void fileSignatureValidationTest() throws RegBaseCheckedException, URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException {
		
		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setIsSignRequired(true);
		requestHTTPDTO.setFileEncrypted(true);
		requestHTTPDTO.setFilePath(null);
		requestHTTPDTO.setUri(new URI("/v1/mosip/test"));
		Object[] args = new Object[1];
		args[0] = requestHTTPDTO;
//		Optional<FileSignature> fileSignature = Mockito.mock(fileSignature.getClass());
//		fileSignature
		Mockito.when(joinPointMock.getArgs()).thenReturn(args);
//		Mockito.when(fileSignatureRepository.findByFileName(Mockito.anyString())).thenReturn(fileSignature);
//		PowerMockito.when(clientCryptoFacade.decrypt()).thenReturn(cryptoCore);
		JWTSignatureVerifyResponseDto jwtSignatureResponseDto = new JWTSignatureVerifyResponseDto();
		jwtSignatureResponseDto.setSignatureValid(true);
		jwtSignatureResponseDto.setMessage(SignatureConstant.VALIDATION_SUCCESSFUL);
		Mockito.when(signatureService.jwtVerify(Mockito.any())).thenReturn(jwtSignatureResponseDto);
		
		responseSignatureAdvice.fileSignatureValidation(joinPointMock);
		
	}
	
	@Test
	public void checkAndUploadCertificateTest() throws RegBaseCheckedException, URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException {
		
		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		requestHTTPDTO.setIsSignRequired(true);
		requestHTTPDTO.setUri(new URI("/v1/mosip/test"));
		Object[] args = new Object[1];
		args[0] = requestHTTPDTO;
		Mockito.when(joinPointMock.getArgs()).thenReturn(args);
		LinkedHashMap<String, Object> mapResponse = new LinkedHashMap<>();
		mapResponse.put("lastSyncTime", "2019-04-23T06:20:28.633Z");
		mapResponse.put("publicKey", null);
		mapResponse.put("issuedAt", null);
		mapResponse.put("expiryAt", null);
		
		KeyPairGenerateResponseDto certificateDto = new KeyPairGenerateResponseDto();
		certificateDto.setCertificate("test");
		Mockito.when(keymanagerService.getCertificate(RegistrationConstants.RESPONSE_SIGNATURE_PUBLIC_KEY_APP_ID,
				Optional.of(RegistrationConstants.RESPONSE_SIGNATURE_PUBLIC_KEY_REF_ID)))
				.thenReturn(certificateDto);
		publicKeySync.saveSignPublicKey("test");
		responseSignatureAdvice.checkAndUploadCertificate(mapResponse, joinPointMock);
	}
	
}
