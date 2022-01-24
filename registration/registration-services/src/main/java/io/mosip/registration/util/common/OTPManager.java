package io.mosip.registration.util.common;

import io.mosip.registration.util.restclient.AuthTokenUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.LoginMode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.LoginUserDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

/**
 * OTP Manager
 * 
 * @author Saravanan
 *
 */
@Component
public class OTPManager extends BaseService {

	/**
	 * Instance of LOGGER
	 */
	private static final Logger LOGGER = AppConfig.getLogger(OTPManager.class);

	@Autowired
	private AuthTokenUtilService authTokenUtilService;

	/**
	 * This method is used to get the OTP for the User from Kernel's AuthN Web-Service. 
	 * 
	 * <p>Sends the username to the OTP service to get the OTP. Based on the response received,
	 * appropriate {@link ResponseDTO} is created</p>
	 * 
	 * <p>If application is offline, web-service will not invoked and {@link ErrorResponseDTO} 
	 * error response is returned.</p>
	 * 
	 * <p>
	 * Returns the {@link ResponseDTO} object.
	 * </p>
	 * 
	 * <p>
	 * If OTP is fetched successfully and sent to the user,
	 * {@link SuccessResponseDTO} will be set in {@link ResponseDTO} object
	 * </p>
	 * 
	 * <p>
	 * If any exception occurs, {@link ErrorResponseDTO} will be set in
	 * {@link ResponseDTO} object
	 * </p>
	 * 
	 * @param userId
	 *            the user id of the user for whom OTP has to be requested
	 * @return the {@link ResponseDTO} object. Sends {@link SuccessResponseDTO} if
	 *         OTP is sent to the user, else {@link ErrorResponseDTO}
	 */
	@SuppressWarnings("unchecked")
	public ResponseDTO getOTP(final String userId) {
		LOGGER.info(LoggerConstants.OTP_MANAGER_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Send OTP Started");

		// Create Response to return to UI layer
		ResponseDTO response = new ResponseDTO();
		try {
			if (!serviceDelegateUtil.isNetworkAvailable()) { /* Check Network Connectivity */
				setErrorResponse(response, RegistrationConstants.CONNECTION_ERROR, null);
				return response;
			}

			String message = authTokenUtilService.sendOtpWithRetryWrapper(userId);
			if(message != null)
				return setSuccessResponse(response, message, null);

		} catch (Throwable e) {
			LOGGER.error("Failed to send OTP", e);
		}
		LOGGER.info(LoggerConstants.OTP_MANAGER_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Send OTP ended");
		return setErrorResponse(response, RegistrationConstants.OTP_GENERATION_ERROR_MESSAGE, null);
	}

	/**
	 * This method is used to validate the entered OTP against the user through
	 * Kernel's AuthN Web-Service. Based on the response received, appropriate
	 * {@link AuthTokenDTO} is created
	 * 
	 * <p>
	 * Returns the {@link AuthTokenDTO} object.
	 * </p>
	 * 
	 * <p>
	 * If application is offline, web-service will not invoked and empty
	 * {@link AuthTokenDTO} object is returned.
	 * </p>
	 * 
	 * <p>
	 * If OTP is validated successfully, the token response upon invoking the rest
	 * API will be set to the {@link AuthTokenDTO} object.
	 * </p>
	 * 
	 * <p>
	 * If any exception occurs, the {@link AuthTokenDTO} will be set to null.
	 * </p>
	 * 
	 * @param userId
	 *            the user id of the user to be validated against
	 * @param otp
	 *            the user entered OTP
	 * @param haveToSaveAuthToken
	 *            flag indicating whether the Authorization Token have to be saved
	 *            in context
	 * @return the {@link AuthTokenDTO} object.
	 */
	public AuthTokenDTO validateOTP(String userId, String otp, boolean haveToSaveAuthToken) {

		LOGGER.info(LoggerConstants.OTP_MANAGER_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Validate OTP Started");

		AuthTokenDTO authTokenDTO = new AuthTokenDTO();
		
		try {
			/* Check Network Connectivity */
			if (serviceDelegateUtil.isNetworkAvailable()) {

				LoginUserDTO loginUserDTO = new LoginUserDTO();
				loginUserDTO.setUserId(userId);
				loginUserDTO.setOtp(otp);
				
				LOGGER.info(LoggerConstants.OTP_MANAGER_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
						RegistrationConstants.APPLICATION_ID, "Validate OTP ended");				

				// Obtain otpValidatorResponseDto from service delegate util
				authTokenDTO = authTokenUtilService.getAuthTokenAndRefreshToken(LoginMode.OTP, loginUserDTO);
			} 
		} catch (RegBaseCheckedException | HttpClientErrorException | HttpServerErrorException | ResourceAccessException
				| RegBaseUncheckedException exception) {
			
			authTokenDTO = null;

			LOGGER.error(LoggerConstants.OTP_MANAGER_LOGGER_TITLE, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));

		}
		return authTokenDTO;
	}
}
