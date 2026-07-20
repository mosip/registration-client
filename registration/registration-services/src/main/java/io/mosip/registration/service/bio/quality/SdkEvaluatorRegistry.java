package io.mosip.registration.service.bio.quality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Registry for SDK-based evaluators. Reads the configured SDK vendor list
 * from application context and provides ordered evaluator names. Uses only
 * {@link SdkBiometricQualityEvaluator} since iBioProviderApi selection is
 * already handled internally by BioAPIFactory via the SDK vendor configuration.
 *
 * @author Antigravity
 */
@Component
public class SdkEvaluatorRegistry {

	private static final Logger LOGGER = AppConfig.getLogger(SdkEvaluatorRegistry.class);

	/**
	 * Returns the list of configured SDK vendor names from application context.
	 * Falls back to empty list when configuration is missing.
	 *
	 * @return list of configured SDK evaluator names (bean names / vendor names)
	 */
	public List<String> getConfiguredSdkVendors() {
		String vendorsConfig = (String) ApplicationContext.map()
				.getOrDefault(RegistrationConstants.QUALITY_SDK_VENDORS, "");

		if (vendorsConfig == null || vendorsConfig.trim().isEmpty()) {
			LOGGER.info("SdkEvaluatorRegistry: No SDK vendors configured under key {}", RegistrationConstants.QUALITY_SDK_VENDORS);
			return new ArrayList<>();
		}

		List<String> vendors = Arrays.stream(vendorsConfig.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
		LOGGER.info("SdkEvaluatorRegistry: Configured SDK vendors: {}", vendors);
		return vendors;
	}

	/**
	 * Checks whether any SDK vendors are configured.
	 *
	 * @return true if at least one vendor is configured
	 */
	public boolean hasConfiguredVendors() {
		return !getConfiguredSdkVendors().isEmpty();
	}
}
