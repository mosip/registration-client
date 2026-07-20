package io.mosip.registration.service.bio.quality.evaluator;

import java.util.Map;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.model.QualityScore;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
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
 *
 * @author Antigravity
 */
@Component
public class SdkBiometricQualityEvaluator implements IBiometricQualityEvaluator {

	private static final Logger LOGGER = AppConfig.getLogger(SdkBiometricQualityEvaluator.class);

	@Autowired
	private BioAPIFactory bioAPIFactory;

	@Autowired
	private BIRBuilder birBuilder;

	@Override
	public QualityScore evaluate(BiometricsDto dto) throws RegBaseCheckedException {
		LOGGER.info("SdkBiometricQualityEvaluator: Evaluating SDK quality for attribute: {}", dto.getBioAttribute());
		try {
			BiometricType biometricType = BiometricType
					.fromValue(Biometric.getSingleTypeByAttribute(dto.getBioAttribute()).name());
			BIR bir = birBuilder.buildBir(dto, ProcessedLevelType.RAW);
			BIR[] birList = new BIR[] { bir };

			Map<BiometricType, Float> scoreMap = bioAPIFactory
					.getBioProvider(biometricType, BiometricFunction.QUALITY_CHECK)
					.getModalityQuality(birList, null);

			Float rawScore = scoreMap != null ? scoreMap.get(biometricType) : null;
			if (rawScore == null || rawScore < 0) {
				LOGGER.error("SdkBiometricQualityEvaluator: Invalid SDK score {} for {}", rawScore, dto.getBioAttribute());
				throw new RegBaseCheckedException(
						RegistrationExceptionConstants.REG_SDK_INVALID_SCORE.getErrorCode(),
						RegistrationExceptionConstants.REG_SDK_INVALID_SCORE.getErrorMessage());
			}

			QualityScore qs = new QualityScore();
			qs.setScore((long) rawScore.floatValue());
			LOGGER.info("SdkBiometricQualityEvaluator: SDK score {} for attribute {}", rawScore, dto.getBioAttribute());
			return qs;

		} catch (RegBaseCheckedException re) {
			throw re;
		} catch (BiometricException be) {
			LOGGER.error("SdkBiometricQualityEvaluator: BiometricException while evaluating SDK quality", be);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_SDK_INVALID_SCORE.getErrorCode(),
					RegistrationExceptionConstants.REG_SDK_INVALID_SCORE.getErrorMessage());
		} catch (Exception e) {
			LOGGER.error("SdkBiometricQualityEvaluator: Unexpected error evaluating SDK quality", e);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_SDK_INVALID_SCORE.getErrorCode(),
					RegistrationExceptionConstants.REG_SDK_INVALID_SCORE.getErrorMessage());
		}
	}

	@Override
	public String getEvaluatorName() {
		return "SDK";
	}
}
