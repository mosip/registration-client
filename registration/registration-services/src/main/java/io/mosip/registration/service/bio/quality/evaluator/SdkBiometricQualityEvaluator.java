package io.mosip.registration.service.bio.quality.evaluator;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.model.QualityScore;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.bio.quality.IBiometricQualityEvaluator;
import io.mosip.registration.util.common.BIRBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SDK Biometric Quality Evaluator that calls the biometric SDK via BioAPIFactory.
 * Uses the existing iBioProviderApi.getModalityQuality() which is already
 * wired in the project via BioAPIFactory.
 */
@Component
public class SdkBiometricQualityEvaluator implements IBiometricQualityEvaluator {

	private static final Logger LOGGER = AppConfig.getLogger(SdkBiometricQualityEvaluator.class);

	private static final long DEFAULT_TIMEOUT_MS = 5000L;

	@Autowired
	private BioAPIFactory bioAPIFactory;

	@Autowired
	private BIRBuilder birBuilder;

	@Autowired
	private AuditManagerService auditFactory;

	@Override
	public QualityScore evaluate(BiometricsDto dto) throws RegBaseCheckedException {
		String bioAttribute = dto.getBioAttribute();
		LOGGER.info("SdkBiometricQualityEvaluator: Evaluating SDK quality for attribute: {}", bioAttribute);
		try {
			BiometricType biometricType = BiometricType
					.fromValue(Biometric.getSingleTypeByAttribute(bioAttribute).name());
			BIR bir = birBuilder.buildBir(dto, ProcessedLevelType.RAW);
			BIR[] birList = new BIR[] { bir };

			Map<BiometricType, Float> scoreMap = callSdkWithTimeout(biometricType, birList, bioAttribute);

			Float rawScore = scoreMap != null ? scoreMap.get(biometricType) : null;
			if (rawScore == null || rawScore < 0) {
				LOGGER.error("SdkBiometricQualityEvaluator: Invalid SDK score {} for {}", rawScore, bioAttribute);
				safeAudit(AuditEvent.SDK_QUALITY_EVAL_FAILED, bioAttribute, "INVALID_SCORE");
				throw new RegBaseCheckedException(
						RegistrationExceptionConstants.REG_SDK_INVALID_SCORE.getErrorCode(),
						RegistrationExceptionConstants.REG_SDK_INVALID_SCORE.getErrorMessage());
			}

			QualityScore qs = new QualityScore();
			qs.setScore((long) rawScore.floatValue());
			LOGGER.info("SdkBiometricQualityEvaluator: SDK score {} for attribute {}", rawScore, bioAttribute);
			safeAudit(AuditEvent.SDK_QUALITY_EVAL_SUCCESS, bioAttribute, "SDK_SUCCESS");
			return qs;

		} catch (RegBaseCheckedException re) {
			throw re;
		} catch (BiometricException be) {
			LOGGER.error("SdkBiometricQualityEvaluator: BiometricException while evaluating SDK quality", be);
			safeAudit(AuditEvent.SDK_QUALITY_EVAL_FAILED, bioAttribute, "SDK_EXCEPTION");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_SDK_EVALUATION_EXCEPTION.getErrorCode(),
					RegistrationExceptionConstants.REG_SDK_EVALUATION_EXCEPTION.getErrorMessage());
		} catch (Exception e) {
			LOGGER.error("SdkBiometricQualityEvaluator: Unexpected error evaluating SDK quality", e);
			safeAudit(AuditEvent.SDK_QUALITY_EVAL_FAILED, bioAttribute, "SDK_EXCEPTION");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_SDK_EVALUATION_EXCEPTION.getErrorCode(),
					RegistrationExceptionConstants.REG_SDK_EVALUATION_EXCEPTION.getErrorMessage());
		}
	}

	/**
	 * Records an audit event without letting an audit-subsystem failure disrupt
	 * quality evaluation itself (Audit Log Failure: continue, log a warning).
	 */
	private void safeAudit(AuditEvent event, String bioAttribute, String reason) {
		try {
			auditFactory.audit(event, Components.REG_BIOMETRICS, bioAttribute, reason);
		} catch (Exception e) {
			LOGGER.warn("Warning: Unable to record biometric quality audit details.", e);
		}
	}

	/**
	 * Runs the SDK's quality-check call on a worker thread and enforces the
	 * configured timeout (mosip.registration.quality.timeout.SDK, falling back
	 * to mosip.registration.quality.timeout.default, then a hardcoded default).
	 * Throws REG_SDK_QUALITY_TIMEOUT if the SDK doesn't respond in time.
	 */
	private Map<BiometricType, Float> callSdkWithTimeout(BiometricType biometricType, BIR[] birList,
			String bioAttribute) throws RegBaseCheckedException, BiometricException {
		long timeoutMs = resolveTimeoutMs();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Future<Map<BiometricType, Float>> future = executor.submit(() -> bioAPIFactory
					.getBioProvider(biometricType, BiometricFunction.QUALITY_CHECK)
					.getModalityQuality(birList, null));
			return future.get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (TimeoutException te) {
			LOGGER.error("SdkBiometricQualityEvaluator: SDK quality check timed out after {} ms for {}",
					timeoutMs, bioAttribute);
			safeAudit(AuditEvent.SDK_QUALITY_EVAL_TIMEOUT, bioAttribute, "SDK_TIMEOUT");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_SDK_QUALITY_TIMEOUT.getErrorCode(),
					RegistrationExceptionConstants.REG_SDK_QUALITY_TIMEOUT.getErrorMessage());
		} catch (java.util.concurrent.ExecutionException ee) {
			// Unwrap and rethrow as-is so the caller's own catch blocks classify it
			// correctly: BiometricException -> Invalid Score path, anything else ->
			// the generic SDK Exception path (REG_SDK_EVALUATION_EXCEPTION), instead
			// of miscategorizing every non-BiometricException failure as Invalid Score.
			Throwable cause = ee.getCause();
			if (cause instanceof BiometricException) {
				throw (BiometricException) cause;
			}
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(ie);
		} finally {
			executor.shutdownNow();
		}
	}

	private long resolveTimeoutMs() {
		String timeoutStr = (String) ApplicationContext.map().get(RegistrationConstants.QUALITY_TIMEOUT_PREFIX + "SDK");
		if (timeoutStr == null) {
			timeoutStr = (String) ApplicationContext.map().get(RegistrationConstants.QUALITY_TIMEOUT_PREFIX + "default");
		}
		if (timeoutStr != null) {
			try {
				return Long.parseLong(timeoutStr.trim());
			} catch (NumberFormatException nfe) {
				LOGGER.warn("SdkBiometricQualityEvaluator: Invalid timeout config '{}', using default {} ms",
						timeoutStr, DEFAULT_TIMEOUT_MS);
			}
		}
		return DEFAULT_TIMEOUT_MS;
	}

	@Override
	public String getEvaluatorName() {
		return "SDK";
	}
}
