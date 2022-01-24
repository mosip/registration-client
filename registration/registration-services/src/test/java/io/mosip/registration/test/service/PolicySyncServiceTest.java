package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

import io.mosip.kernel.cryptomanager.util.CryptomanagerUtils;
import io.mosip.kernel.keymanagerservice.dto.KeyPairGenerateResponseDto;
import io.mosip.kernel.keymanagerservice.dto.UploadCertificateRequestDto;
import io.mosip.kernel.keymanagerservice.dto.UploadCertificateResponseDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.kernel.keymanagerservice.util.KeymanagerUtil;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.impl.RegistrationCenterDAOImpl;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.entity.RegistrationCenter;
import io.mosip.registration.entity.id.RegistartionCenterId;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.repositories.RegistrationCenterRepository;
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.service.sync.impl.PolicySyncServiceImpl;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
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

	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;

	@InjectMocks
	private PolicySyncServiceImpl policySyncServiceImpl;

	@Mock
	private RegistrationCenterDAOImpl registrationCenterDAO;

	@Mock
	private CenterMachineReMapService centerMachineReMapService;

	@Mock
	private MachineMasterRepository machineMasterRepository;

	@Mock
	private RegistrationCenterRepository registrationCenterRepository;

	@Mock
    private RetryTemplate retryTemplate;
	
	@Mock
	private UserDetailService userDetailService;
	
	@Mock
	private KeymanagerService keymanagerService;
	
	@Mock
	private KeymanagerUtil keymanagerUtil;

	@Mock
	private CryptomanagerUtils cryptomanagerUtils;

	@Before
	public void initialize() {
		Map<String, Object> temp = new HashMap<String, Object>();
		temp.put("mosip.registration.key_policy_sync_threshold_value", "1");
		temp.put("mosip.registration.retry.delay.policy.sync", 1000l);
		temp.put("mosip.registration.retry.maxattempts.policy.sync", 2);
		temp.put(RegistrationConstants.INITIAL_SETUP, RegistrationConstants.DISABLE);

		PowerMockito.mockStatic(ApplicationContext.class, RegistrationAppHealthCheckUtil.class, SessionContext.class,
				RegistrationSystemPropertiesChecker.class);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(SessionContext.userId()).thenReturn("110012");
		Mockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");
		Mockito.when(ApplicationContext.map()).thenReturn(temp);
		
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);

		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);
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
		policySyncServiceImpl.init();
	}

	@Test
	public void fetchPolicySuccessTest() throws ExhaustedRetryException, Throwable {
		Mockito.when(retryTemplate.execute(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            RetryCallback retry = invocation.getArgument(0);
            return retry.doWithRetry(null);
        });
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		LinkedHashMap<String, Object> publicKeySyncResponse = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put(RegistrationConstants.CERTIFICATE, "TEST");
		publicKeySyncResponse.put(RegistrationConstants.RESPONSE, responseMap);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(publicKeySyncResponse);
		KeyPairGenerateResponseDto certificateDto = new KeyPairGenerateResponseDto();
		certificateDto.setCertificate("TEST");
		Mockito.when(keymanagerService.getCertificate(Mockito.anyString(), Mockito.any())).thenReturn(certificateDto);
		Certificate cert = Mockito.mock(Certificate.class);
		Mockito.when(keymanagerUtil.convertToCertificate(Mockito.anyString())).thenReturn(cert);
		Mockito.when(cryptomanagerUtils.getCertificateThumbprint(Mockito.any())).thenReturn("TEST".getBytes());
		assertNotNull(policySyncServiceImpl.fetchPolicy().getSuccessResponseDTO());
	}
	
	@Test
	public void fetchPolicySuccessTest2() throws ExhaustedRetryException, Throwable {
		Mockito.when(retryTemplate.execute(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            RetryCallback retry = invocation.getArgument(0);
            return retry.doWithRetry(null);
        });
		
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		LinkedHashMap<String, Object> publicKeySyncResponse = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put(RegistrationConstants.CERTIFICATE, "TEST");
		publicKeySyncResponse.put(RegistrationConstants.RESPONSE, responseMap);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(publicKeySyncResponse);
		Mockito.when(keymanagerService.getCertificate(Mockito.anyString(), Mockito.any())).thenThrow(new RuntimeException());
		Mockito.when(keymanagerService.uploadOtherDomainCertificate(Mockito.any(UploadCertificateRequestDto.class))).thenReturn(new UploadCertificateResponseDto());
		
		assertNotNull(policySyncServiceImpl.fetchPolicy().getSuccessResponseDTO());
	}
	
	@Test
	public void fetchPolicyFailureTest() throws ExhaustedRetryException, Throwable {
		Mockito.when(retryTemplate.execute(Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(RegBaseCheckedException.class);
		
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		LinkedHashMap<String, Object> publicKeySyncResponse = new LinkedHashMap<>();
		List<LinkedHashMap<String, String>> errors = new ArrayList<>();
		LinkedHashMap<String, String> errorMap = new LinkedHashMap<>();
		errorMap.put("EXP-001", "Test Exception");
		errors.add(errorMap);
		publicKeySyncResponse.put(RegistrationConstants.ERRORS, errors);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(publicKeySyncResponse);
		
		assertNotNull(policySyncServiceImpl.fetchPolicy().getErrorResponseDTOs());
	}
	
	@Test
	public void fetchPolicyInternetFailureTest() throws ExhaustedRetryException, Throwable {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(false);
		assertNotNull(policySyncServiceImpl.fetchPolicy().getErrorResponseDTOs());
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void fetchPolicyValidationFailureTest() throws ExhaustedRetryException, Throwable {
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(null);
		policySyncServiceImpl.fetchPolicy();
	}
	
	@Test
	public void checkKeyValidationSuccessTest() {
		KeyPairGenerateResponseDto certificateDto = new KeyPairGenerateResponseDto();
		certificateDto.setCertificate("TEST");
		Mockito.when(keymanagerService.getCertificate(Mockito.anyString(), Mockito.any())).thenReturn(certificateDto);
		assertNotNull(policySyncServiceImpl.checkKeyValidation().getSuccessResponseDTO());
	}
	
	@Test
	public void checkKeyValidationFailureTest() {
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(null);
		assertNotNull(policySyncServiceImpl.checkKeyValidation().getErrorResponseDTOs());
	}

}
