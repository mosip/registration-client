package io.mosip.registration.util.healthcheck;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.exception.RegBaseCheckedException;

/**
 * Registration System Properties Checker
 * 
 * @author Sivasankar Thalavai
 * @since 1.0.0
 */
public class RegistrationSystemPropertiesChecker {

	private static final Logger LOGGER = AppConfig.getLogger(RegistrationSystemPropertiesChecker.class);

	private RegistrationSystemPropertiesChecker() {

	}

	/**
	 * This method is used to get machine name.
	 * 
	 * @return machine name
	 * @throws RegBaseCheckedException 
	 */
	public static String getMachineId() {
		try {
			return InetAddress.getLocalHost().getHostName().toLowerCase();
		} catch (UnknownHostException e) {
			LOGGER.error("", e);
		}
		return null;
	}
}