package io.mosip.registration.service.bio.quality;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.model.SDKInfo;
import io.mosip.kernel.biometrics.spi.IBioApi;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.context.ApplicationContext;

/**
 * Fetches the real {@link SDKInfo} (owner organization, sdk/api version etc.)
 * from the SDK configured for a modality. BioAPIFactory/iBioProviderApi -
 * the interface used elsewhere to run quality checks - never expose the
 * SDKInfo returned by the SDK's own init() call, so this instantiates the
 * configured IBioApi implementation directly (same className config used to
 * wire that SDK into BioAPIFactory) purely to read its self-reported info.
 * Instantiated once per modality and cached, since this info is static for
 * the lifetime of the running app.
 */
@Component
public class SdkInfoProvider {

	private static final Logger LOGGER = AppConfig.getLogger(SdkInfoProvider.class);

	private static final String FINGER_PROVIDER_KEY = "mosip.fingerprint.provider";
	private static final String IRIS_PROVIDER_KEY = "mosip.iris.provider";
	private static final String FACE_PROVIDER_KEY = "mosip.face.provider";

	private final Map<BiometricType, SDKInfo> cache = new ConcurrentHashMap<>();
	private static final SDKInfo NOT_AVAILABLE = new SDKInfo();

	public SDKInfo getSdkInfo(BiometricType biometricType) {
		SDKInfo cached = cache.get(biometricType);
		if (cached != null) {
			return cached == NOT_AVAILABLE ? null : cached;
		}

		SDKInfo sdkInfo = resolveSdkInfo(biometricType);
		cache.put(biometricType, sdkInfo == null ? NOT_AVAILABLE : sdkInfo);
		return sdkInfo;
	}

	private SDKInfo resolveSdkInfo(BiometricType biometricType) {
		String providerKey = providerKeyFor(biometricType);
		if (providerKey == null) {
			return null;
		}

		String className = (String) ApplicationContext.map().get(providerKey);
		if (className == null || className.trim().isEmpty()) {
			LOGGER.warn("SdkInfoProvider: No SDK provider configured under key {} for {}", providerKey, biometricType);
			return null;
		}

		try {
			Object instance = Class.forName(className.trim()).getDeclaredConstructor().newInstance();
			if (!(instance instanceof IBioApi)) {
				LOGGER.warn("SdkInfoProvider: Configured class {} does not implement IBioApi", className);
				return null;
			}
			SDKInfo sdkInfo = ((IBioApi) instance).init(null);
			LOGGER.info("SdkInfoProvider: Resolved SDKInfo for {} from {}: {}", biometricType, className, sdkInfo);
			return sdkInfo;
		} catch (Exception e) {
			LOGGER.warn("SdkInfoProvider: Unable to resolve SDKInfo for {} from {}", biometricType, className, e);
			return null;
		}
	}

	private String providerKeyFor(BiometricType biometricType) {
		switch (biometricType) {
			case FINGER:
				return FINGER_PROVIDER_KEY;
			case IRIS:
				return IRIS_PROVIDER_KEY;
			case FACE:
				return FACE_PROVIDER_KEY;
			default:
				return null;
		}
	}
}
