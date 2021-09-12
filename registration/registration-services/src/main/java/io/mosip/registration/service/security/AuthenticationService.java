package io.mosip.registration.service.security;

import java.util.List;

import io.mosip.registration.config.MetricTag;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;

public interface AuthenticationService {

	/**
	 * Validator for Biometric authentication
	 * @param userId
	 * @param modality
	 * @param biometrics
	 * @return
	 */
	Boolean authValidator(@MetricTag("userid") String userId, @MetricTag("modality") String modality, List<BiometricsDto> biometrics);
	
	/**
	 * Validator for OTP authentication
	 * 
	 * @param validatorType
	 *            The type of validator which is OTP
	 * @param userId
	 *            The userId
	 * @param otp
	 *            otp entered
	 * @param haveToSaveAuthToken
	 *            flag indicating whether the Authorization Token have to be saved
	 *            in context
	 * @return {@link AuthTokenDTO} returning authtokendto
	 */
	AuthTokenDTO authValidator(@MetricTag("validatortype") String validatorType, @MetricTag("userid") String userId, String otp, boolean haveToSaveAuthToken);


	/**
	 * This method is used to validate pwd authentication
	 * 
	 * @param authenticationValidatorDTO
	 *            The authentication validation inputs with user id and pwd
	 * @return String
	 */
	Boolean validatePassword(@MetricTag(value = "userid", extractor = "arg.userId") AuthenticationValidatorDTO authenticationValidatorDTO) throws RegBaseCheckedException;

}