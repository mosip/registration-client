package io.mosip.registration.service.bio.quality;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.model.QualityScore;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * BiometricQualityOrchestrator coordinates quality evaluation from multiple
 * evaluator sources (SBI, SDK, etc.) and aggregates them into a final score
 * using a configurable aggregation strategy.
 *
 * <p>Configuration is read from application context at runtime using the
 * {@code RegistrationConstants.QUALITY_*} prefix keys.
 */
@Component
public class BiometricQualityOrchestrator {

	private static final Logger LOGGER = AppConfig.getLogger(BiometricQualityOrchestrator.class);

	/** All available evaluators injected by Spring. */
	@Autowired
	private List<IBiometricQualityEvaluator> evaluators;

	/** All available aggregator strategies injected by Spring. */
	@Autowired
	private List<IBiometricScoreAggregator> aggregators;

	@Autowired
	private AuditManagerService auditFactory;

	/**
	 * Orchestrates the quality evaluation for a given biometric DTO.
	 *
	 * <p>Steps:
	 * <ol>
	 *   <li>Reads which evaluators are configured for the modality/attribute.</li>
	 *   <li>Runs each evaluator and collects scores.</li>
	 *   <li>Selects the configured aggregation strategy and aggregates scores.</li>
	 *   <li>Returns the aggregated score.</li>
	 * </ol>
	 *
	 * @param biometricsDto the captured biometric data
	 * @return the aggregated final quality score (0-100)
	 * @throws RegBaseCheckedException if no evaluators are configured or evaluation fails
	 */
	public double orchestrate(BiometricsDto biometricsDto) throws RegBaseCheckedException {
		String bioAttribute = biometricsDto.getBioAttribute();
		LOGGER.info("BiometricQualityOrchestrator: Starting quality orchestration for attribute {}", bioAttribute);

		// 1. Resolve modality (FINGER, IRIS, FACE) from bioAttribute for config lookup
		String modality = resolveModality(bioAttribute);

		
		String modalityKeyUpper = RegistrationConstants.QUALITY_EVALUATORS_MODALITY_PREFIX + modality.toUpperCase();
		String modalityKeyLower = RegistrationConstants.QUALITY_EVALUATORS_MODALITY_PREFIX + modality.toLowerCase();
		String attributeKey     = RegistrationConstants.QUALITY_EVALUATORS_PREFIX + bioAttribute;
		String defaultKey       = RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default";

		String evaluatorConfig;
		if (ApplicationContext.map().containsKey(modalityKeyUpper)) {
			evaluatorConfig = (String) ApplicationContext.map().get(modalityKeyUpper);
			LOGGER.info("BiometricQualityOrchestrator: Using modality config '{}' for modality {} (attribute {})",
					modalityKeyUpper, modality, bioAttribute);
		} else if (ApplicationContext.map().containsKey(modalityKeyLower)) {
			evaluatorConfig = (String) ApplicationContext.map().get(modalityKeyLower);
			LOGGER.info("BiometricQualityOrchestrator: Using modality config '{}' for modality {} (attribute {})",
					modalityKeyLower, modality, bioAttribute);
		} else if (ApplicationContext.map().containsKey(attributeKey)) {
			evaluatorConfig = (String) ApplicationContext.map().get(attributeKey);
			LOGGER.info("BiometricQualityOrchestrator: Using attribute config '{}' for attribute {}", attributeKey, bioAttribute);
		} else {
			evaluatorConfig = (String) ApplicationContext.map().getOrDefault(defaultKey, "SBI");
			LOGGER.info("BiometricQualityOrchestrator: Using default evaluator config for attribute {}", bioAttribute);
		}

		List<String> configuredEvaluatorNames = List.of(evaluatorConfig.split(",")).stream()
				.map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());



		// 2. Collect only the matching evaluators
		List<IBiometricQualityEvaluator> selectedEvaluators = evaluators.stream()
				.filter(e -> configuredEvaluatorNames.stream()
						.anyMatch(name -> name.equalsIgnoreCase(e.getEvaluatorName())))
				.collect(Collectors.toList());

		if (selectedEvaluators.isEmpty()) {
			LOGGER.error("BiometricQualityOrchestrator: No matching evaluators found for configured names: {}", configuredEvaluatorNames);
			auditFactory.audit(AuditEvent.QUALITY_ORCH_FAILED, Components.REG_BIOMETRICS,
					bioAttribute, "NO_EVALUATOR");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorCode(),
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorMessage());
		}

		// 3. Run each evaluator and collect scores
		Map<String, Double> scores = new HashMap<>();
		for (IBiometricQualityEvaluator evaluator : selectedEvaluators) {
			try {
				LOGGER.info("BiometricQualityOrchestrator: Running evaluator {} for attribute {}", evaluator.getEvaluatorName(), bioAttribute);
				QualityScore qs = evaluator.evaluate(biometricsDto);
				if (qs != null) {
					scores.put(evaluator.getEvaluatorName(), (double) qs.getScore());
				}
			} catch (RegBaseCheckedException e) {
				LOGGER.error("BiometricQualityOrchestrator: Evaluator {} failed for attribute {}: {}",
						evaluator.getEvaluatorName(), bioAttribute, e.getMessage());
				// Continue with other evaluators, not fatal unless all fail
			}
		}

		if (scores.isEmpty()) {
			LOGGER.error("BiometricQualityOrchestrator: All evaluators failed for attribute {}", bioAttribute);
			auditFactory.audit(AuditEvent.QUALITY_ORCH_FAILED, Components.REG_BIOMETRICS, bioAttribute, "ALL_EVALUATORS_FAILED");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorCode(),
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorMessage());
		}

		// 4. Read the configured aggregation strategy (Hierarchy: modality > attribute > default)
		String aggModalityKeyUpper = RegistrationConstants.QUALITY_AGGREGATION_MODALITY_PREFIX + modality.toUpperCase();
		String aggModalityKeyLower = RegistrationConstants.QUALITY_AGGREGATION_MODALITY_PREFIX + modality.toLowerCase();
		String aggAttributeKey     = RegistrationConstants.QUALITY_AGGREGATION_PREFIX + bioAttribute;
		String aggDefaultKey       = RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default";

		String strategyName;
		if (ApplicationContext.map().containsKey(aggModalityKeyUpper)) {
			strategyName = (String) ApplicationContext.map().get(aggModalityKeyUpper);
		} else if (ApplicationContext.map().containsKey(aggModalityKeyLower)) {
			strategyName = (String) ApplicationContext.map().get(aggModalityKeyLower);
		} else if (ApplicationContext.map().containsKey(aggAttributeKey)) {
			strategyName = (String) ApplicationContext.map().get(aggAttributeKey);
		} else {
			strategyName = (String) ApplicationContext.map().getOrDefault(aggDefaultKey, "MEAN");
		}
		strategyName = strategyName.trim().toUpperCase();

		// 5. Read per-evaluator weights (for weighted strategies)
		Map<String, Double> weights = new HashMap<>();
		for (String evaluatorName : scores.keySet()) {
			String weightKey = RegistrationConstants.QUALITY_WEIGHT_PREFIX + bioAttribute + "." + evaluatorName;
			String weightStr = (String) ApplicationContext.map().getOrDefault(weightKey, "1.0");
			try {
				weights.put(evaluatorName, Double.parseDouble(weightStr.trim()));
			} catch (NumberFormatException nfe) {
				LOGGER.warn("BiometricQualityOrchestrator: Invalid weight config for {}, defaulting to 1.0", weightKey);
				weights.put(evaluatorName, 1.0);
			}
		}

		// 6. Find and apply the aggregation strategy
		final String finalStrategyName = strategyName;
		IBiometricScoreAggregator selectedAggregator = aggregators.stream()
				.filter(a -> a.getStrategyName().equalsIgnoreCase(finalStrategyName))
				.findFirst()
				.orElse(aggregators.stream()
						.filter(a -> "MEAN".equalsIgnoreCase(a.getStrategyName()))
						.findFirst()
						.orElse(null));

		if (selectedAggregator == null) {
			LOGGER.error("BiometricQualityOrchestrator: No aggregator found for strategy {}", strategyName);
			auditFactory.audit(AuditEvent.QUALITY_ORCH_FAILED, Components.REG_BIOMETRICS, bioAttribute, "NO_AGGREGATOR");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorCode(),
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorMessage());
		}

		double aggregatedScore = selectedAggregator.aggregate(scores, weights);
		LOGGER.info("BiometricQualityOrchestrator: Aggregated score {} using strategy {} for attribute {}",
				aggregatedScore, finalStrategyName, bioAttribute);

		auditFactory.audit(AuditEvent.QUALITY_ORCH_COMPLETED, Components.REG_BIOMETRICS, bioAttribute, "ORCH_DONE");
		return aggregatedScore;
	}

	/**
	 * Resolves the modality name (e.g. FINGER, IRIS, FACE) for a bioAttribute.
	 *
	 * @param bioAttribute the attribute name (e.g. leftIndex, leftEye, face)
	 * @return modality name string or UNKNOWN
	 */
	private String resolveModality(String bioAttribute) {
		if (bioAttribute == null) {
			return "UNKNOWN";
		}
		try {
			BiometricType type = Biometric.getSingleTypeByAttribute(bioAttribute);
			if (type != null) {
				return type.name().toUpperCase();
			}
		} catch (Throwable t) {
			LOGGER.debug("BiometricQualityOrchestrator: Biometric.getSingleTypeByAttribute fallback for {}", bioAttribute);
		}

		// Fallback string matching for standard MOSIP attributes (e.g. in test environment)
		String attr = bioAttribute.toLowerCase();
		if (attr.contains("eye") || attr.contains("iris")) {
			return "IRIS";
		} else if (attr.contains("face")) {
			return "FACE";
		} else if (attr.contains("index") || attr.contains("thumb") || attr.contains("middle")
				|| attr.contains("ring") || attr.contains("little") || attr.contains("finger") || attr.contains("hand")) {
			return "FINGER";
		}
		return "UNKNOWN";
	}
}
