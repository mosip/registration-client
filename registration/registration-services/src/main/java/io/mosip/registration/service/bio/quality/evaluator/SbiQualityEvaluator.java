package io.mosip.registration.service.bio.quality.evaluator;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.Components;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.bio.quality.IBiometricQualityEvaluator;
import io.mosip.kernel.biometrics.model.QualityScore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SBI Quality Evaluator reads the quality score captured via MDM.
 */
@Component
public class SbiQualityEvaluator implements IBiometricQualityEvaluator {

	private static final Logger LOGGER = AppConfig.getLogger(SbiQualityEvaluator.class);

	@Autowired
	private AuditManagerService auditFactory;

	@Override
	public QualityScore evaluate(BiometricsDto dto) throws RegBaseCheckedException {
		String bioAttribute = dto.getBioAttribute();
		double rawScore = dto.getQualityScore();

		// A negative score means the device never reported one (0-100 is the valid
		// range for a real, if poor, capture) - treat that as the score being missing.
		if (rawScore < 0) {
			LOGGER.error("SbiQualityEvaluator: Missing/invalid SBI score {} for {}", rawScore, bioAttribute);
			safeAudit(AuditEvent.QUALITY_ORCH_FAILED, bioAttribute, "SBI_SCORE_MISSING");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_SBI_SCORE_UNAVAILABLE.getErrorCode(),
					RegistrationExceptionConstants.REG_SBI_SCORE_UNAVAILABLE.getErrorMessage());
		}

		QualityScore qs = new QualityScore();
		qs.setScore((long) rawScore);
		return qs;
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

	@Override
	public String getEvaluatorName() {
		return "SBI";
	}
}
