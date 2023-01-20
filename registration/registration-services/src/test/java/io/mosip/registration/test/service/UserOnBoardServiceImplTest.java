package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;

import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.codec.digest.DigestUtils;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.crypto.spi.CryptoCoreSpec;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.cryptomanager.dto.CryptomanagerResponseDto;
import io.mosip.kernel.cryptomanager.service.CryptomanagerService;
import io.mosip.kernel.keygenerator.bouncycastle.util.KeyGeneratorUtils;
import io.mosip.kernel.keymanagerservice.dto.KeyPairGenerateResponseDto;
import io.mosip.kernel.keymanagerservice.dto.UploadCertificateResponseDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.kernel.keymanagerservice.util.KeymanagerUtil;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.RegistrationCenterDAO;
import io.mosip.registration.dao.UserOnboardDAO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.entity.MachineMaster;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.operator.UserDetailService;
import io.mosip.registration.service.operator.impl.UserOnboardServiceImpl;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.util.common.BIRBuilder;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;


/**
 * @author Sreekar Chukka
 *
 * @since 1.0.0
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ UserOnBoardServiceImplTest.class, RegistrationSystemPropertiesChecker.class, ApplicationContext.class,
		RegistrationAppHealthCheckUtil.class, SecretKey.class, SessionContext.class, HMACUtils2.class, CryptoUtil.class, DigestUtils.class, KeyGeneratorUtils.class, KeyGenerator.class, SecretKey.class })
public class UserOnBoardServiceImplTest {
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@Mock
	private RegistrationAppHealthCheckUtil registrationAppHealthCheckUtil;
	
	@InjectMocks
	private UserOnboardServiceImpl userOnboardServiceImpl;

	@InjectMocks
	private BaseService baseService;
	
	@Mock
	private UserOnboardDAO userOnBoardDao;
	
	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;
	
	@Mock
    private CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> cryptoCore;

	@Mock
	io.mosip.registration.context.ApplicationContext context;
	
	@Mock
	private KeymanagerService keymanagerService;
	
	@Mock
	private KeymanagerUtil keymanagerUtil;
	
	@Mock
	private CryptomanagerService cryptomanagerService;
	
	@Mock
	private BioAPIFactory bioAPIFactory;
	
	@Mock
	private iBioProviderApi bioProvider;
	
	@Mock
	private UserDetailService userDetailService;
	
	@Mock
	private CenterMachineReMapService centerMachineReMapService;
	
	@Mock
	private RegistrationCenterDAO registrationCenterDAO;
	
	@Mock
	private MachineMasterRepository machineMasterRepository;
	
	@Mock
	private BIRBuilder birBuilder;
	
	@Before
	public void init() throws Exception {
		Map<String, BiometricsDto> operatorBiometrics = new HashMap<>();
		
		BiometricsDto biometrics1 = new BiometricsDto("leftMiddle", "leftMiddle".getBytes(), 90.0);
		biometrics1.setModalityName(RegistrationConstants.FINGERPRINT_UPPERCASE);
		BiometricsDto biometrics2 = new BiometricsDto("leftRing", "leftRing".getBytes(), 90.0);
		biometrics2.setModalityName(RegistrationConstants.FINGERPRINT_UPPERCASE);
		BiometricsDto biometrics3 = new BiometricsDto("leftEye", "leftEye".getBytes(), 90.0);
		biometrics3.setModalityName(RegistrationConstants.IRIS);
		BiometricsDto biometrics4 = new BiometricsDto("rightLittle", "rightLittle".getBytes(), 90.0);
		biometrics4.setModalityName(RegistrationConstants.FINGERPRINT_UPPERCASE);
		
		operatorBiometrics.put(String.format("%s_%s_%s", "test", "test", ""), biometrics1);
		operatorBiometrics.put(String.format("%s_%s_%s", "test1", "test1", ""), biometrics2);
		operatorBiometrics.put(String.format("%s_%s_%s", "test2", "test2", ""), biometrics3);
		operatorBiometrics.put(String.format("%s_%s_%s", "test3", "test3", "exp"), biometrics4);
		ReflectionTestUtils.setField(userOnboardServiceImpl, "operatorBiometrics", operatorBiometrics);
		
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.USER_ON_BOARD_THRESHOLD_LIMIT, "10");		
		appMap.put("mosip.registration.fingerprint_disable_flag", "Y");
		appMap.put("mosip.registration.iris_disable_flag", "Y");
		appMap.put("mosip.registration.face_disable_flag", "Y");
		appMap.put("mosip.registration.onboarduser_ida_auth", "Y");
		appMap.put(RegistrationConstants.SERVER_ACTIVE_PROFILE, "dev.mosip.net");
		appMap.put(RegistrationConstants.ID_AUTH_DOMAIN_URI, "https://dev.mosip.net");
		appMap.put(RegistrationConstants.KEY_SPLITTER, "_");
		appMap.put(RegistrationConstants.INITIAL_SETUP, "Y");
		PowerMockito.mockStatic(ApplicationContext.class);
		Mockito.when(ApplicationContext.map()).thenReturn(appMap);
		Mockito.when(ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.SERVER_ACTIVE_PROFILE)).thenReturn("dev.mosip.net");
		
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(SessionContext.isSessionContextAvailable()).thenReturn(true);
		Mockito.when(SessionContext.userId()).thenReturn("110012");
		Mockito.when(userDetailService.isValidUser(Mockito.anyString())).thenReturn(true);
		Mockito.when(centerMachineReMapService.isMachineRemapped()).thenReturn(false);
		PowerMockito.mockStatic(RegistrationSystemPropertiesChecker.class);
		Mockito.when(RegistrationSystemPropertiesChecker.getMachineId()).thenReturn("b2ml27210");
		MachineMaster machineMaster = new MachineMaster();
		machineMaster.setId("10012");
		machineMaster.setIsActive(true);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machineMaster);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);
	}
	
	@Test
	public void userOnBoard() throws HttpClientErrorException, SocketTimeoutException, RegBaseCheckedException, ConnectionException, NoSuchAlgorithmException, CertificateEncodingException, BiometricException {
		Mockito.when(serviceDelegateUtil.prepareURLByHostName(Mockito.anyString())).thenReturn("https://dev.mosip.net");
		KeyPairGenerateResponseDto certificateDto = new KeyPairGenerateResponseDto();
		Mockito.when(keymanagerService.getCertificate(Mockito.anyString(), Mockito.any())).thenReturn(certificateDto);
		
		Map<String, Object> response = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put(RegistrationConstants.CERTIFICATE, "CERTIFICATE".getBytes());
		response.put(RegistrationConstants.RESPONSE, responseMap);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(response);
		Mockito.when(keymanagerService.uploadOtherDomainCertificate(Mockito.any())).thenReturn(new UploadCertificateResponseDto());
		
		Certificate certificate = Mockito.mock(Certificate.class);
		PublicKey publicKey = Mockito.mock(PublicKey.class);
		Mockito.when(certificate.getEncoded()).thenReturn("test".getBytes());
		Mockito.when(certificate.getPublicKey()).thenReturn(publicKey);
		
		Mockito.when(keymanagerUtil.convertToCertificate(Mockito.anyString())).thenReturn(certificate);
		
		PowerMockito.mockStatic(HMACUtils2.class);
		PowerMockito.when(HMACUtils2.digestAsPlainText(Mockito.any())).thenReturn("hash");
		
		BiometricsDto biometrics1 = new BiometricsDto("leftMiddle", "leftMiddle".getBytes(), 90.0);
		biometrics1.setModalityName(RegistrationConstants.FINGERPRINT_UPPERCASE);
		BiometricsDto biometrics2 = new BiometricsDto("leftRing", "leftRing".getBytes(), 90.0);
		biometrics2.setModalityName(RegistrationConstants.FINGERPRINT_UPPERCASE);
		BiometricsDto biometrics3 = new BiometricsDto("leftEye", "leftEye".getBytes(), 90.0);
		biometrics3.setModalityName(RegistrationConstants.IRIS);
		BiometricsDto biometrics4 = new BiometricsDto("rightLittle", "rightLittle".getBytes(), 90.0);
		biometrics4.setModalityName(RegistrationConstants.FINGERPRINT_UPPERCASE);
		BiometricsDto biometrics5 = new BiometricsDto("face", "face".getBytes(), 90.0);
		biometrics5.setModalityName(RegistrationConstants.FACE);
		
		List<BiometricsDto> biometrics = new ArrayList<>();
		biometrics.add(biometrics1);
		biometrics.add(biometrics2);
		biometrics.add(biometrics3);
		biometrics.add(biometrics4);
		biometrics.add(biometrics5);
		
		PowerMockito.mockStatic(CryptoUtil.class);
		PowerMockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any())).thenReturn("test");
		
		CryptomanagerResponseDto cryptomanagerResponseDto = new CryptomanagerResponseDto();
		cryptomanagerResponseDto.setData("test_data");
		Mockito.when(cryptomanagerService.encrypt(Mockito.any())).thenReturn(cryptomanagerResponseDto);
		PowerMockito.when(CryptoUtil.decodeURLSafeBase64(Mockito.anyString())).thenReturn("test_data".getBytes());
		
		PowerMockito.mockStatic(DigestUtils.class);
		PowerMockito.when(DigestUtils.sha256(Mockito.any(byte[].class))).thenReturn("test".getBytes());
		
		PowerMockito.mockStatic(KeyGeneratorUtils.class);
		KeyGenerator keyGenerator = PowerMockito.mock(KeyGenerator.class);
		PowerMockito.when(KeyGeneratorUtils.getKeyGenerator(Mockito.anyString(), Mockito.anyInt())).thenReturn(keyGenerator);
		SecretKey symmentricKey = PowerMockito.mock(SecretKey.class);
		PowerMockito.doReturn(symmentricKey).when(keyGenerator).generateKey();
		
		Mockito.when(cryptoCore.symmetricEncrypt(Mockito.any(SecretKey.class), Mockito.any(byte[].class), Mockito.any(byte[].class))).thenReturn("test".getBytes());
		Mockito.when(cryptoCore.asymmetricEncrypt(Mockito.any(), Mockito.any())).thenReturn("test".getBytes());
		
		LinkedHashMap<String, Object> onBoardResponse = new LinkedHashMap<>();
		LinkedHashMap<String, Object> rMap = new LinkedHashMap<>();
		rMap.put(RegistrationConstants.ON_BOARD_AUTH_STATUS, true);
		onBoardResponse.put(RegistrationConstants.RESPONSE, rMap);
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(onBoardResponse);
		
		BIR bir = new BIR();
		BDBInfo bdbInfo = new BDBInfo();
		bdbInfo.setType(Arrays.asList(BiometricType.FINGER));
		bir.setBdbInfo(bdbInfo);
		Mockito.when(birBuilder.buildBir(Mockito.any(BiometricsDto.class), Mockito.any(ProcessedLevelType.class))).thenReturn(bir);
		
		Mockito.when(bioAPIFactory.getBioProvider(Mockito.any(), Mockito.any())).thenReturn(bioProvider);
		List<BIR> templates = new ArrayList<>();
		Mockito.when(bioProvider.extractTemplate(Mockito.anyList(), Mockito.anyMap())).thenReturn(templates);
		Mockito.when(userOnBoardDao.insertExtractedTemplates(Mockito.anyList())).thenReturn(RegistrationConstants.SUCCESS);
		Mockito.when(userOnBoardDao.save()).thenReturn(RegistrationConstants.SUCCESS);

		assertNotNull(userOnboardServiceImpl.validateWithIDAuthAndSave(biometrics).getSuccessResponseDTO());		
	}
	
	@Test
	public void userOnBoardFailure() throws HttpClientErrorException, SocketTimeoutException, RegBaseCheckedException, ConnectionException, NoSuchAlgorithmException, CertificateEncodingException, BiometricException {
		Mockito.when(serviceDelegateUtil.prepareURLByHostName(Mockito.anyString())).thenReturn("https://dev.mosip.net");
		KeyPairGenerateResponseDto certificateDto = new KeyPairGenerateResponseDto();
		Mockito.when(keymanagerService.getCertificate(Mockito.anyString(), Mockito.any())).thenReturn(certificateDto);
		
		Map<String, Object> response = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put(RegistrationConstants.CERTIFICATE, "CERTIFICATE".getBytes());
		response.put(RegistrationConstants.RESPONSE, responseMap);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(response);
		Mockito.when(keymanagerService.uploadOtherDomainCertificate(Mockito.any())).thenReturn(new UploadCertificateResponseDto());
		
		Certificate certificate = Mockito.mock(Certificate.class);
		PublicKey publicKey = Mockito.mock(PublicKey.class);
		Mockito.when(certificate.getEncoded()).thenReturn("test".getBytes());
		Mockito.when(certificate.getPublicKey()).thenReturn(publicKey);
		
		Mockito.when(keymanagerUtil.convertToCertificate(Mockito.anyString())).thenReturn(certificate);
		
		PowerMockito.mockStatic(HMACUtils2.class);
		PowerMockito.when(HMACUtils2.digestAsPlainText(Mockito.any())).thenReturn("hash");
		
		BiometricsDto biometrics1 = new BiometricsDto("leftMiddle", "leftMiddle".getBytes(), 90.0);
		biometrics1.setModalityName(RegistrationConstants.FINGERPRINT_UPPERCASE);
		BiometricsDto biometrics2 = new BiometricsDto("leftRing", "leftRing".getBytes(), 90.0);
		biometrics2.setModalityName(RegistrationConstants.FINGERPRINT_UPPERCASE);
		BiometricsDto biometrics3 = new BiometricsDto("leftEye", "leftEye".getBytes(), 90.0);
		biometrics1.setModalityName(RegistrationConstants.IRIS);
		BiometricsDto biometrics4 = new BiometricsDto("rightLittle", "rightLittle".getBytes(), 90.0);
		biometrics2.setModalityName(RegistrationConstants.FINGERPRINT_UPPERCASE);
		BiometricsDto biometrics5 = new BiometricsDto("face", "face".getBytes(), 90.0);
		biometrics2.setModalityName(RegistrationConstants.FACE);
		
		List<BiometricsDto> biometrics = new ArrayList<>();
		biometrics.add(biometrics1);
		biometrics.add(biometrics2);
		biometrics.add(biometrics3);
		biometrics.add(biometrics4);
		biometrics.add(biometrics5);
		
		PowerMockito.mockStatic(CryptoUtil.class);
		PowerMockito.when(CryptoUtil.encodeToURLSafeBase64(Mockito.any())).thenReturn("test");
		
		CryptomanagerResponseDto cryptomanagerResponseDto = new CryptomanagerResponseDto();
		cryptomanagerResponseDto.setData("test_data");
		Mockito.when(cryptomanagerService.encrypt(Mockito.any())).thenReturn(cryptomanagerResponseDto);
		PowerMockito.when(CryptoUtil.decodeURLSafeBase64(Mockito.anyString())).thenReturn("test_data".getBytes());
		
		PowerMockito.mockStatic(DigestUtils.class);
		PowerMockito.when(DigestUtils.sha256(Mockito.any(byte[].class))).thenReturn("test".getBytes());
		
		PowerMockito.mockStatic(KeyGeneratorUtils.class);
		KeyGenerator keyGenerator = PowerMockito.mock(KeyGenerator.class);
		PowerMockito.when(KeyGeneratorUtils.getKeyGenerator(Mockito.anyString(), Mockito.anyInt())).thenReturn(keyGenerator);
		SecretKey symmentricKey = PowerMockito.mock(SecretKey.class);
		PowerMockito.doReturn(symmentricKey).when(keyGenerator).generateKey();
		
		Mockito.when(cryptoCore.asymmetricEncrypt(Mockito.any(), Mockito.any())).thenReturn("test".getBytes());
		
		LinkedHashMap<String, Object> onBoardResponse = new LinkedHashMap<>();
		LinkedHashMap<String, Object> respMap = new LinkedHashMap<>();
		respMap.put(RegistrationConstants.ON_BOARD_AUTH_STATUS, false);
		List<LinkedHashMap<String, Object>> errors = new ArrayList<>();
		LinkedHashMap<String, Object> rMap = new LinkedHashMap<>();
		rMap.put("errorMessage", "User Onboard Failed");
		errors.add(rMap);
		onBoardResponse.put(RegistrationConstants.RESPONSE, respMap);
		onBoardResponse.put(RegistrationConstants.ERRORS, errors);
		Mockito.when(serviceDelegateUtil.post(Mockito.anyString(), Mockito.any(), Mockito.anyString())).thenReturn(onBoardResponse);
		
		assertNotNull(userOnboardServiceImpl.validateWithIDAuthAndSave(biometrics).getErrorResponseDTOs());		
	}

	
	@Test
	public void getLastUpdatedTime() {
		Mockito.when(userOnBoardDao.getLastUpdatedTime(Mockito.anyString())).thenReturn(new Timestamp(System.currentTimeMillis()));
		assertNotNull(userOnboardServiceImpl.getLastUpdatedTime("User123"));
	}
	
	@Test
	public void addOperatorBiometricsTest() {
		BiometricsDto biometrics = new BiometricsDto("leftMiddle", "leftMiddle".getBytes(), 90.0);
		userOnboardServiceImpl.addOperatorBiometricException("test", "test");
		userOnboardServiceImpl.removeOperatorBiometrics("test1", "test1");
		userOnboardServiceImpl.removeOperatorBiometricException("test2", "test2");
		Assert.assertSame(userOnboardServiceImpl.addOperatorBiometrics("test", "test", biometrics), biometrics);
	}
	
	@Test
	public void getAllBiometricsTest() {
		assertNotNull(userOnboardServiceImpl.getAllBiometrics());
	}
	
	@Test
	public void getAllBiometricExceptionsTest() {
		assertNotNull(userOnboardServiceImpl.getAllBiometricExceptions());
	}
	
	@Test
	public void isBiometricExceptionTest() {
		Assert.assertTrue(userOnboardServiceImpl.isBiometricException("test3", "test3"));
	}
	
	@Test
	public void getBiometricsTest() {
		List<String> attributeNames = Arrays.asList("test");
		Assert.assertNotNull(userOnboardServiceImpl.getBiometrics("test", attributeNames));
	}

}
