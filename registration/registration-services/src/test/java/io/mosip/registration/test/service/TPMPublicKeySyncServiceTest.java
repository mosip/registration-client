package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.service.spi.ClientCryptoService;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.sync.impl.TPMPublicKeySyncServiceImpl;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ApplicationContext.class, RegistrationSystemPropertiesChecker.class, CryptoUtil.class })
public class TPMPublicKeySyncServiceTest {
	
	@Rule
	public MockitoRule MockitoRule = MockitoJUnit.rule();
	
	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	@Mock
	private ClientCryptoFacade clientCryptoFacade;
	
	@Mock
	private ClientCryptoService clientCryptoService;
	
	@InjectMocks
	private TPMPublicKeySyncServiceImpl tpmPublicKeySyncServiceImpl;
	
	@Before
	public void initialize() {
		Map<String, Object> appMap = new HashMap<String, Object>();
		appMap.put(RegistrationConstants.REGISTRATION_CLIENT, "registrationclient");
		
		PowerMockito.mockStatic(ApplicationContext.class, RegistrationSystemPropertiesChecker.class);
		Mockito.when(ApplicationContext.map()).thenReturn(appMap);
		Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("110012");
	}
	
	@Test
	public void syncTPMPublicKeySuccessTest() throws RegBaseCheckedException, ConnectionException {
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(clientCryptoService.getEncryptionPublicPart()).thenReturn("test".getBytes());
		Mockito.when(clientCryptoService.getSigningPublicPart()).thenReturn("test".getBytes());
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any(byte[].class))).thenReturn("test");
		Map<String, Object> publicKeyResponse = new LinkedHashMap<>();
		publicKeyResponse.put(RegistrationConstants.RESPONSE, "Success");
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(publicKeyResponse);
		assertNotNull(tpmPublicKeySyncServiceImpl.syncTPMPublicKey().getSuccessResponseDTO());
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void syncTPMPublicKeyFailureTest() throws RegBaseCheckedException, ConnectionException {
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(clientCryptoService.getEncryptionPublicPart()).thenReturn("test".getBytes());
		Mockito.when(clientCryptoService.getSigningPublicPart()).thenReturn("test".getBytes());
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any(byte[].class))).thenReturn("test");
		Map<String, Object> publicKeyResponse = new LinkedHashMap<>();
		List<Map<String, String>> errors = new ArrayList<>();
		Map<String, String> errorMap = new LinkedHashMap<>();
		errorMap.put(RegistrationConstants.ERROR_CODE, "ERR-001");
		errorMap.put(RegistrationConstants.MESSAGE_CODE, "TPM Public key sync failed");
		errors.add(errorMap);
		publicKeyResponse.put(RegistrationConstants.ERRORS, errors);
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(publicKeyResponse);
		tpmPublicKeySyncServiceImpl.syncTPMPublicKey();
	}
	
	@Test(expected = RegBaseUncheckedException.class)
	public void syncTPMPublicKeyRuntimeExceptionTest() throws RegBaseCheckedException, ConnectionException {
		Mockito.when(clientCryptoFacade.getClientSecurity()).thenReturn(clientCryptoService);
		Mockito.when(clientCryptoService.getEncryptionPublicPart()).thenReturn("test".getBytes());
		Mockito.when(clientCryptoService.getSigningPublicPart()).thenReturn("test".getBytes());
		PowerMockito.mockStatic(CryptoUtil.class);
		Mockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any(byte[].class))).thenReturn("test");
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenThrow(RuntimeException.class);
		tpmPublicKeySyncServiceImpl.syncTPMPublicKey();
	}

}
