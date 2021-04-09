package io.mosip.registration.constants;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ResourceBundle;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.context.ApplicationContext;

public class RegistrationUIConstants {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(RegistrationUIConstants.class);

	// Key values to read value from messages.properties file

	public static ResourceBundle bundle = ApplicationContext.getInstance().getApplicationLanguageMessagesBundle();

	public static void setBundle() {

		ApplicationContext applicationContext = ApplicationContext.getInstance();
		bundle = applicationContext.getBundle(applicationContext.getApplicationLanguage(),
				RegistrationConstants.MESSAGES);

	}

	/*
	 * public static final String OTP_VALIDITY = bundle.getString("OTP_VALIDITY");
	 * public static final String MINUTES = bundle.getString("MINUTES");
	 */

	public static String getMessageLanguageSpecific(String key) {
		try {
			return bundle.getString(key);
		} catch (Exception runtimeException) {
			LOGGER.error("REGISTRATION_UI_CONSTANTS", APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(runtimeException));
		}
		return key != null ? String.format(ERROR + " : %s", key) : ERROR;
	}

	// ALERT
	public static final String ERROR = bundle.getString("ERROR");
	
	public static final String REGEX_TYPE = "REGEX";
	public static final String SUPERVISOR_AUTHENTICATION_CONFIGURATION = "mosip.registration.supervisor_authentication_configuration";
}
