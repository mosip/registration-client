package io.mosip.registration.service.bio.quality.evaluator;

import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.bio.quality.IBiometricQualityEvaluator;
import io.mosip.kernel.biometrics.model.QualityScore;
import org.springframework.stereotype.Component;

/**
 * SBI Quality Evaluator reads the quality score captured via MDM.
 */
@Component
public class SbiQualityEvaluator implements IBiometricQualityEvaluator {

	@Override
	public QualityScore evaluate(BiometricsDto dto) throws RegBaseCheckedException {
		QualityScore qs = new QualityScore();
		qs.setScore((long) dto.getQualityScore());
		return qs;
	}

	@Override
	public String getEvaluatorName() {
		return "SBI";
	}
}
