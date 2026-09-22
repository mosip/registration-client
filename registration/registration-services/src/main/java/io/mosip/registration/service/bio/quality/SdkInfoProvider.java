package io.mosip.registration.service.bio.quality;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.model.SDKInfo;
import io.mosip.kernel.biometrics.spi.IBioApi;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;

/**
 * Fetches the real {@link SDKInfo} (owner organization, sdk/api version etc.)
 * from the SDK BioAPIFactory already has live for a modality.
 *
 * <p>BioAPIFactory/iBioProviderApi never expose the SDKInfo returned by the
 * SDK's own init() call, so this reflects into the concrete provider's
 * private {@code sdkRegistry} field to reuse the exact {@link IBioApi}
 * instance BioAPIFactory itself already instantiated and manages - it does
 * NOT read a config string and reflectively instantiate a second SDK object
 * of its own. Reflecting on a config-supplied class name (the previous
 * approach) would let anyone with server config-write access get arbitrary
 * code to run on every field machine that syncs it, and would separately
 * risk a second live instance of a licensed SDK (extra license-seat
 * consumption, an init() call with different params than the vendor
 * expects). Going through BioAPIFactory's own already-bootstrapped instance
 * avoids both.
 *
 * <p>This only works against {@code BioProviderImpl_V_0_9} (the only
 * provider version this feature has been built and tested against); for any
 * other/unrecognised provider shape this returns null rather than guessing.
 * Resolved once per modality and cached, since this info is static for the
 * lifetime of the running app.
 */
@Component
public class SdkInfoProvider {

	private static final Logger LOGGER = AppConfig.getLogger(SdkInfoProvider.class);

	@Autowired
	private BioAPIFactory bioAPIFactory;

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
		try {
			iBioProviderApi provider = bioAPIFactory.getBioProvider(biometricType, BiometricFunction.QUALITY_CHECK);
			if (provider == null) {
				return null;
			}

			IBioApi liveSdk = extractLiveSdkInstance(provider, biometricType);
			if (liveSdk == null) {
				return null;
			}

			// Reuses the already-bootstrapped instance - does not create a new one.
			SDKInfo sdkInfo = liveSdk.init(null);
			LOGGER.info("SdkInfoProvider: Resolved SDKInfo for {} via BioAPIFactory's live provider: {}",
					biometricType, sdkInfo);
			return sdkInfo;
		} catch (Exception e) {
			LOGGER.warn("SdkInfoProvider: Unable to resolve SDKInfo for {} via BioAPIFactory", biometricType, e);
			return null;
		}
	}

	/**
	 * Reflects into BioProviderImpl_V_0_9's private
	 * {@code sdkRegistry: Map<BiometricType, Map<BiometricFunction, IBioApi>>}
	 * field to get the IBioApi instance it already has live for this
	 * modality/function, instead of instantiating a new one ourselves.
	 */
	@SuppressWarnings("unchecked")
	private IBioApi extractLiveSdkInstance(iBioProviderApi provider, BiometricType biometricType) throws Exception {
		// The bean BioAPIFactory returns is a Spring AOP proxy (the @Counted/@Timed
		// micrometer annotations on the provider's methods trigger JDK dynamic
		// proxying), not the concrete BioProviderImpl_V_0_9 instance itself - its
		// declared fields (including sdkRegistry) live on the real target object
		// behind the proxy, so unwrap that first.
		Object target = provider;
		if (AopUtils.isAopProxy(provider) && provider instanceof Advised) {
			Object unwrapped = ((Advised) provider).getTargetSource().getTarget();
			if (unwrapped != null) {
				target = unwrapped;
			}
		}

		Field registryField;
		try {
			registryField = target.getClass().getDeclaredField("sdkRegistry");
		} catch (NoSuchFieldException nsfe) {
			LOGGER.warn("SdkInfoProvider: Provider {} has no sdkRegistry field - unsupported provider version",
					target.getClass().getName());
			return null;
		}
		registryField.setAccessible(true);
		Object registryValue = registryField.get(target);
		if (!(registryValue instanceof Map)) {
			return null;
		}

		Object perModality = ((Map<BiometricType, Object>) registryValue).get(biometricType);
		if (!(perModality instanceof Map)) {
			return null;
		}

		Object sdkForFunction = ((Map<BiometricFunction, Object>) perModality).get(BiometricFunction.QUALITY_CHECK);
		return sdkForFunction instanceof IBioApi ? (IBioApi) sdkForFunction : null;
	}
}
