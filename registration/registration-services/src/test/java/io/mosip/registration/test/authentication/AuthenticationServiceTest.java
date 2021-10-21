package io.mosip.registration.test.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.constants.LoginMode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.impl.UserDetailDAOImpl;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.util.common.OTPManager;
import io.mosip.registration.util.restclient.AuthTokenUtilService;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

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

import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.impl.BioProviderImpl_V_0_9;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.UserDTO;
import io.mosip.registration.dto.UserPasswordDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.entity.id.UserBiometricId;
import io.mosip.registration.service.bio.impl.BioServiceImpl;
import io.mosip.registration.service.login.LoginService;
import io.mosip.registration.service.security.impl.AuthenticationServiceImpl;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ HMACUtils2.class, CryptoUtil.class})
public class AuthenticationServiceTest {

	@Mock
	OTPManager otpManager;
	
	@Mock
	private LoginService loginService;

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private AuthenticationServiceImpl authenticationServiceImpl;
	
	@Mock
	private UserDetailDAOImpl userDetailDAO;
	
	@Mock
	private BioServiceImpl bioServiceImpl;
	
	@Mock
	private BioAPIFactory bioAPIFactory;
	
	@Mock
	private BioProviderImpl_V_0_9 bioProviderImpl;

	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;
	
	@Mock
	private AuthTokenUtilService authTokenUtilService;
	
	@Before
	public void initialize() {
		PowerMockito.mockStatic(HMACUtils2.class);
		PowerMockito.mockStatic(CryptoUtil.class);
	}
	
	@Test
	public void getOtpValidatorTest() {
		AuthTokenDTO authTokenDTO =new AuthTokenDTO();
		when(otpManager.validateOTP(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
				.thenReturn(authTokenDTO);
		assertNotNull(authenticationServiceImpl.authValidator("otp", "mosip", "12345", true));
	}

	
	@Test
	public void validatePasswordTest() throws NoSuchAlgorithmException, RegBaseCheckedException {
		UserDTO userDTO = new UserDTO();
		userDTO.setSalt("salt");
		UserPasswordDTO userPassword = new UserPasswordDTO();
		userPassword.setPwd("mosip");
		userDTO.setUserPassword(userPassword);
		
		AuthenticationValidatorDTO authenticationValidatorDTO=new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId("mosip");
		authenticationValidatorDTO.setPassword("mosip");
		PowerMockito.mockStatic(CryptoUtil.class, HMACUtils2.class);
		Mockito.when(loginService.getUserDetail("mosip")).thenReturn(userDTO);		
		Mockito.when(CryptoUtil.decodeURLSafeBase64("salt")).thenReturn("salt".getBytes());
		Mockito.when(HMACUtils2.digestAsPlainTextWithSalt("mosip".getBytes(), "salt".getBytes())).thenReturn("mosip");
		
		assertEquals(true, authenticationServiceImpl.validatePassword(authenticationValidatorDTO));
		
	}
	
	@Test
	public void validatePasswordNotMatchTest() throws NoSuchAlgorithmException {
		UserDTO userDTO = new UserDTO();
		userDTO.setSalt("salt");
		UserPasswordDTO userPassword = new UserPasswordDTO();
		userPassword.setPwd("mosip");
		userDTO.setUserPassword(userPassword);
		
		AuthenticationValidatorDTO authenticationValidatorDTO=new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId("mosip");
		authenticationValidatorDTO.setPassword("mosip");
		
		Mockito.when(loginService.getUserDetail("mosip")).thenReturn(userDTO);		
		Mockito.when(CryptoUtil.decodeBase64("salt")).thenReturn("salt".getBytes());		
		Mockito.when(HMACUtils2.digestAsPlainTextWithSalt("mosip1".getBytes(), "salt".getBytes())).thenReturn("mosip1");

		String errorCode = null;
		try {
			authenticationServiceImpl.validatePassword(authenticationValidatorDTO);
		} catch (RegBaseCheckedException e) {
			errorCode =  e.getErrorCode();
		}
		assertEquals(RegistrationConstants.PWD_MISMATCH, errorCode);
	}
	
	@Test
	public void validatePasswordFailureTest() {
		UserDTO userDTO = new UserDTO();
		userDTO.setId("mosip");
		AuthenticationValidatorDTO authenticationValidatorDTO=new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId("mosip");
		authenticationValidatorDTO.setPassword("mosip");
	
		Mockito.when(loginService.getUserDetail("mosip")).thenReturn(userDTO);			

		String errorCode = null;
		try {
			authenticationServiceImpl.validatePassword(authenticationValidatorDTO);
		} catch (RegBaseCheckedException e) {
			errorCode =  e.getErrorCode();
		}
		assertEquals(RegistrationConstants.PWD_MISMATCH, errorCode);
	}
	
	@Test
	public void validatePasswordFailure1Test() {		
		AuthenticationValidatorDTO authenticationValidatorDTO=new AuthenticationValidatorDTO();
		
		Mockito.when(loginService.getUserDetail("mosip")).thenReturn(null);
		String errorCode = null;
		try {
			authenticationServiceImpl.validatePassword(authenticationValidatorDTO);
		} catch (RegBaseCheckedException e) {
			errorCode =  e.getErrorCode();
		}
		assertEquals(RegistrationConstants.PWD_MISMATCH, errorCode);
		
	}
	
	@Test
	public void validatePasswordNetworkPassTest() throws Exception {
		AuthenticationValidatorDTO authenticationValidatorDTO=new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId("mosip");
		authenticationValidatorDTO.setPassword("mosip");
		
		UserDTO userDTO = new UserDTO();
		userDTO.setSalt("salt");
		UserPasswordDTO userPassword = new UserPasswordDTO();
		userPassword.setPwd("mosip");
		userDTO.setUserPassword(userPassword);
		
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		Mockito.when(authTokenUtilService.getAuthTokenAndRefreshToken(LoginMode.PASSWORD)).thenReturn(null);
		Mockito.when(loginService.getUserDetail(authenticationValidatorDTO.getUserId())).thenReturn(userDTO);
		PowerMockito.mockStatic(HMACUtils2.class, CryptoUtil.class);
		Mockito.when(CryptoUtil.decodeURLSafeBase64("salt")).thenReturn("salt".getBytes());
		Mockito.when(HMACUtils2.digestAsPlainTextWithSalt("mosip".getBytes(), "salt".getBytes())).thenReturn("mosip");
		
		Assert.assertTrue(authenticationServiceImpl.validatePassword(authenticationValidatorDTO));
		
	}
	
	@Test
	public void validatePasswordNetworkFailTest() throws Exception {
		AuthenticationValidatorDTO authenticationValidatorDTO=new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId("mosip");
		authenticationValidatorDTO.setPassword("mosip");
		
		UserDTO userDTO = new UserDTO();
		userDTO.setSalt("salt");
		UserPasswordDTO userPassword = new UserPasswordDTO();
		userPassword.setPwd("mosip");
		userDTO.setUserPassword(userPassword);
		
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(false);
		Mockito.when(authTokenUtilService.getAuthTokenAndRefreshToken(LoginMode.PASSWORD)).thenReturn(null);
		Mockito.when(loginService.getUserDetail(authenticationValidatorDTO.getUserId())).thenReturn(userDTO);
		PowerMockito.mockStatic(HMACUtils2.class, CryptoUtil.class);
		Mockito.when(CryptoUtil.decodeURLSafeBase64("salt")).thenReturn("salt".getBytes());
		Mockito.when(HMACUtils2.digestAsPlainTextWithSalt("mosip".getBytes(), "salt".getBytes())).thenReturn("mosip");
		
		Assert.assertTrue(authenticationServiceImpl.validatePassword(authenticationValidatorDTO));
		
	}
	
	@Test
	public void authValidatorTest() throws BiometricException {
		
		List<BiometricsDto> biometrics = new ArrayList<BiometricsDto>();
		
		List<BiometricsDto> expectedListOfBiometrics = new ArrayList<BiometricsDto>();
		String str1 = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAoHCBUVFRgWFRISERgYHBUYGBIS";
		byte[] byteData1 = new byte[str1.length()];
		BiometricsDto expectedBiometrics1 = new BiometricsDto("rightIndex", byteData1, 80.0);
		expectedBiometrics1.setCaptured(true);
		expectedBiometrics1.setForceCaptured(false);
		expectedBiometrics1.setModalityName("FINGERPRINT_SLAB_RIGHT");
		expectedBiometrics1.setNumOfRetries(0);
		expectedBiometrics1.setSdkScore(0.0);
		
		String str2 = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAoHCBUVFRgWFRISERgYHBUYGBIS";
		byte[] byteData2 = new byte[str2.length()];
		BiometricsDto expectedBiometrics2 = new BiometricsDto("rightLittle", byteData2, 80.0);
		expectedBiometrics2.setCaptured(true);
		expectedBiometrics2.setForceCaptured(false);
		expectedBiometrics2.setModalityName("FINGERPRINT_SLAB_RIGHT");
		expectedBiometrics2.setNumOfRetries(0);
		expectedBiometrics2.setSdkScore(0.0);
		
		expectedListOfBiometrics.add(expectedBiometrics1);
		expectedListOfBiometrics.add(expectedBiometrics2);
		
		BiometricType biometricType = BiometricType.fromValue("IRIS");
		
		List<UserBiometric> userBiometrics = new ArrayList<>();
		UserBiometric userBiometric = new UserBiometric();
		UserBiometricId userBiometricId = new UserBiometricId();
		userBiometricId.setBioAttributeCode("bioCode");
		userBiometricId.setBioTypeCode("bioCode");
		userBiometricId.setUsrId("mosip");
		userBiometric.setIsActive(true);
		userBiometric.setNumberOfRetry(1);
		userBiometric.setQualityScore(3);
		userBiometric.setUserBiometricId(userBiometricId);
		userBiometrics.add(userBiometric);
		
		BIR recordBir = new BIR();
		BIR sampleBir = new BIR();
		
		Mockito.when(userDetailDAO.getUserSpecificBioDetails(Mockito.anyString(), Mockito.anyString())).thenReturn(userBiometrics);
		Mockito.when(bioServiceImpl.buildBir(userBiometric.getUserBiometricId().getBioAttributeCode(),
				userBiometric.getQualityScore(), userBiometric.getBioIsoImage(), ProcessedLevelType.PROCESSED)).thenReturn(recordBir);
		Mockito.when(bioServiceImpl.buildBir(Mockito.any(BiometricsDto.class))).thenReturn(sampleBir);
		
		Mockito.when(bioAPIFactory.getBioProvider(biometricType, BiometricFunction.MATCH)).thenReturn(bioProviderImpl);
		Mockito.when(bioProviderImpl.verify(Mockito.anyList(), Mockito.anyList(), Mockito.any(BiometricType.class), Mockito.anyMap())).thenReturn(true);
		Assert.assertFalse(authenticationServiceImpl.authValidator("mosip", "IRIS", biometrics));
	}

	@Test
	public void authValidatorFailureTest() {
		
		BiometricType biometricType = BiometricType.fromValue("IRIS");
		List<UserBiometric> userBiometrics = new ArrayList<UserBiometric>();
		Mockito.when(userDetailDAO.getUserSpecificBioDetails("mosip", biometricType.value())).thenReturn(userBiometrics);
		Assert.assertFalse(authenticationServiceImpl.authValidator("mosip", "IRIS", null));
	}
}
