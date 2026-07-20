package io.mosip.registration.service.bio.quality;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
 *
 * @author Antigravity
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

		// 1. Read the configured evaluator list for this attribute (or global fallback)
		String evaluatorConfig = (String) ApplicationContext.map()
				.getOrDefault(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + bioAttribute,
						ApplicationContext.map().getOrDefault(RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default", "SBI"));
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

		// 4. Read the configured aggregation strategy
		String strategyName = (String) ApplicationContext.map()
				.getOrDefault(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + bioAttribute,
						ApplicationContext.map().getOrDefault(RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default", "MEAN"));
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
}
