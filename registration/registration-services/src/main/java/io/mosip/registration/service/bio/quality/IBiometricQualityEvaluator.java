package io.mosip.registration.service.bio.quality;

import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.kernel.biometrics.model.QualityScore;

/**
 * Interface for biometric quality evaluator plugins.
 */
public interface IBiometricQualityEvaluator {

	/**
	 * Evaluates the biometric quality of a biometric DTO.
	 * 
	 * @param biometricsDto the biometric data transfer object
	 * @return the evaluated QualityScore
	 * @throws RegBaseCheckedException if a checked exception occurs during evaluation
	 */
	QualityScore evaluate(BiometricsDto biometricsDto) throws RegBaseCheckedException;

	/**
	 * Returns the unique evaluator name matched against configuration keys.
	 * 
	 * @return the unique evaluator name
	 */
	String getEvaluatorName();
}
