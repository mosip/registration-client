package io.mosip.registration.test.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

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
import io.mosip.kernel.clientcrypto.util.ClientCryptoUtils;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.impl.UserDetailDAOImpl;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.UserDTO;
import io.mosip.registration.dto.UserPasswordDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.entity.id.UserBiometricId;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.login.LoginService;
import io.mosip.registration.service.security.impl.AuthenticationServiceImpl;
import io.mosip.registration.util.common.BIRBuilder;
import io.mosip.registration.util.common.OTPManager;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

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
	
	@Mock
	public UserDetailDAOImpl userDetailDaoImpl;
	
	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;
	
	@Mock
	private BIRBuilder birBuilder;
	
	@Mock
	private BioAPIFactory bioAPIFactory;
	
	@Mock
	private BioProviderImpl_V_0_9 bioProviderImpl;
	
	@InjectMocks
	private AuthenticationServiceImpl authenticationServiceImpl;
	
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
		Mockito.when(ClientCryptoUtils.decodeBase64Data("salt")).thenReturn("salt".getBytes());
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
		Mockito.when(ClientCryptoUtils.decodeBase64Data("salt")).thenReturn("salt".getBytes());
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
		assertEquals(RegistrationConstants.CREDS_NOT_FOUND, errorCode);
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
	public void authValidatorPositive() throws BiometricException {
		
		Boolean expectedResult = true;
		byte[] bioAttributesBytes = "slkdalskdjslkajdjadj".getBytes();
		List<BiometricsDto> listOfBios = new ArrayList<>();
		BiometricsDto bio1 = new BiometricsDto("leftEye", bioAttributesBytes, 84.5);
		BiometricsDto bio2 = new BiometricsDto("rightEye", bioAttributesBytes, 84.5);
		listOfBios.add(bio1);
		listOfBios.add(bio2);
		
		List<BIR> listBirs = new ArrayList<>();
		BIR bir1 = new BIR();
		BIR bir2 = new BIR();
		listBirs.add(bir1);
		listBirs.add(bir2);
		
		
		List<UserBiometric> userBiometrics = new ArrayList<>();
		UserBiometric userBiometric1 = new UserBiometric();
		userBiometric1.setBioIsoImage(bioAttributesBytes);
		userBiometric1.setQualityScore(85);
		UserBiometricId id1 = new UserBiometricId();
		id1.setBioAttributeCode("leftEye");
		userBiometric1.setUserBiometricId(id1);
		UserBiometric userBiometric2 = new UserBiometric();
		userBiometric2.setBioIsoImage(bioAttributesBytes);
		userBiometric2.setQualityScore(85);
		UserBiometricId id2 = new UserBiometricId();
		id2.setBioAttributeCode("leftEye");
		userBiometric2.setUserBiometricId(id2);
		UserBiometric userBiometric3 = new UserBiometric();
		userBiometric3.setBioIsoImage(bioAttributesBytes);
		userBiometric3.setQualityScore(85);
		UserBiometricId id3 = new UserBiometricId();
		id1.setBioAttributeCode("leftEye");
		userBiometric3.setUserBiometricId(id3);
		
		userBiometrics.add(userBiometric1);
		userBiometrics.add(userBiometric2);
		userBiometrics.add(userBiometric3);
		
		Mockito.when(userDetailDaoImpl.getUserSpecificBioDetails(Mockito.anyString(), Mockito.anyString())).thenReturn(userBiometrics);
		Mockito.when(birBuilder.buildBir(Mockito.anyString(), Mockito.anyInt(), Mockito.any(byte[].class), Mockito.any(ProcessedLevelType.class))).thenReturn(bir1);
		Mockito.when(birBuilder.buildBir(Mockito.any(BiometricsDto.class), Mockito.any(ProcessedLevelType.class))).thenReturn(bir2);
		Mockito.when(bioAPIFactory.getBioProvider(Mockito.any(BiometricType.class), Mockito.any(BiometricFunction.class))).thenReturn(bioProviderImpl);
		Mockito.when(bioProviderImpl.verify(Mockito.anyList(), Mockito.anyList(), Mockito.any(BiometricType.class), Mockito.isNull())).thenReturn(true);
		Boolean actualResult = authenticationServiceImpl.authValidator("110003", "IRIS", listOfBios); 
		
		assertEquals(expectedResult, actualResult);
	}

}
