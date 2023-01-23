package io.mosip.registration.service.security.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.micrometer.core.annotation.Counted;
import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.clientcrypto.util.ClientCryptoUtils;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoginMode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.UserDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.login.LoginService;
import io.mosip.registration.service.security.AuthenticationService;
import io.mosip.registration.util.common.BIRBuilder;
import io.mosip.registration.util.common.OTPManager;
import io.mosip.registration.util.restclient.AuthTokenUtilService;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

/**
 * Service class for Authentication
 * 
 * @author SaravanaKumar G
 *
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {
	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(AuthenticationServiceImpl.class);

	@Autowired
	private LoginService loginService;

	@Autowired
	private OTPManager otpManager;

	@Autowired
	private BioAPIFactory bioAPIFactory;

	@Autowired
	private UserDetailDAO userDetailDAO;
	
	@Autowired
	protected BIRBuilder birBuilder;

	@Autowired
	private AuthTokenUtilService authTokenUtilService;

	@Autowired
	private ServiceDelegateUtil serviceDelegateUtil;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.security.AuthenticationServiceImpl#
	 * authValidator(java.lang.String,
	 * io.mosip.registration.dto.AuthenticationValidatorDTO)
	 */
	@Counted(recordFailuresOnly = true, extraTags = {"type" , "biometric-login"})
	public Boolean authValidator(String userId, String modality, List<BiometricsDto> biometrics) {
		LOGGER.info("OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				modality + " >> authValidator invoked.");
		try {
			BiometricType biometricType = BiometricType.fromValue(modality);
			List<BIR> record = new ArrayList<>();
			List<UserBiometric> userBiometrics = userDetailDAO.getUserSpecificBioDetails(userId, biometricType.value());
			if (userBiometrics.isEmpty())
				return false;
			userBiometrics.forEach(userBiometric -> {
				try {
					BIR bir = CbeffValidator.getBIRFromXML(userBiometric.getBioRawImage());
					record.add(bir.getBirs().get(0));
				} catch (Exception e) {
					LOGGER.error("Failed deserialization of BIR data of operator with exception >> ", e);
					// Since de-serialization failed, we assume that we stored BDB in database and
					// generating BIR from it
					record.add(birBuilder.buildBir(userBiometric.getUserBiometricId().getBioAttributeCode(),
							userBiometric.getQualityScore(), userBiometric.getBioIsoImage(), ProcessedLevelType.PROCESSED));
				}
			});

			List<BIR> sample = new ArrayList<>(biometrics.size());
			biometrics.forEach(biometricDto -> {
				sample.add(birBuilder.buildBir(biometricDto, ProcessedLevelType.RAW));
			});

			return verifyBiometrics(biometricType, modality, sample, record);

		} catch (BiometricException | RuntimeException e) {
			LOGGER.error("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(e));
		}
		return false;
	}

	private boolean verifyBiometrics(BiometricType biometricType, String modality,
									 List<BIR> sample, List<BIR> record) throws BiometricException {
		iBioProviderApi bioProvider = bioAPIFactory.getBioProvider(biometricType, BiometricFunction.MATCH);
		if (Objects.isNull(bioProvider))
			return false;

		LOGGER.info("OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				modality + " >> Bioprovider instance found : " + bioProvider);
		return bioProvider.verify(sample, record, biometricType, null);
	}

	

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.registration.service.security.AuthenticationServiceImpl#
	 * authValidator(java.lang.String, java.lang.String, java.lang.String)
	 */
	public AuthTokenDTO authValidator(String validatorType, String userId, String otp, boolean haveToSaveAuthToken) {
		return otpManager.validateOTP(userId, otp, haveToSaveAuthToken);
	}



	/**
	 * to validate the password and send appropriate message to display.
	 *
	 * @param authenticationValidatorDTO - DTO which contains the username and
	 *                                   password entered by the user
	 * @return appropriate message after validation
	 */
	@Counted(recordFailuresOnly = true, extraTags = {"type" , "pwd-login"})
	public Boolean validatePassword(AuthenticationValidatorDTO authenticationValidatorDTO) throws  RegBaseCheckedException {
		LOGGER.debug("Validating credentials using database >>>> {}", authenticationValidatorDTO.getUserId());
		try {
			//Always mandate user to reach server to validate pwd when machine is online
			//As in case of new user, any valid authtoken will be simply allowed
			//to avoid any such scenario, mandate to fetch new token when login
			if(serviceDelegateUtil.isNetworkAvailable()) {
				authTokenUtilService.getAuthTokenAndRefreshToken(LoginMode.PASSWORD);
			}

			UserDTO userDTO = loginService.getUserDetail(authenticationValidatorDTO.getUserId());

			if (null != userDTO && null != userDTO.getSalt() && HMACUtils2
							.digestAsPlainTextWithSalt(authenticationValidatorDTO.getPassword().getBytes(),
									ClientCryptoUtils.decodeBase64Data(userDTO.getSalt()))
							.equals(userDTO.getUserPassword().getPwd())) {
				return  true;
			}

			if (null != userDTO && null == userDTO.getSalt()) {
				throw new RegBaseCheckedException(RegistrationConstants.CREDS_NOT_FOUND,
						RegistrationConstants.CREDS_NOT_FOUND);
			}

		} catch (RegBaseCheckedException e) {
			throw e;
		} catch (RuntimeException | NoSuchAlgorithmException runtimeException) {
			LOGGER.error("Pwd validation failed", runtimeException);
		}
		throw new RegBaseCheckedException(RegistrationConstants.PWD_MISMATCH, RegistrationConstants.PWD_MISMATCH);
	}

}
