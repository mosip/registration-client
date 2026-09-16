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
	 * Holds the final aggregated score together with the individual per-evaluator
	 * scores (e.g. "SBI" -> 44.0, "SDK" -> 75.0) that fed into it, so callers can
	 * display/store the raw sources alongside the aggregate.
	 */
	public static class OrchestrationResult {
		private final double aggregatedScore;
		private final Map<String, Double> evaluatorScores;
		private final boolean aggregationExplicitlyConfigured;

		public OrchestrationResult(double aggregatedScore, Map<String, Double> evaluatorScores,
				boolean aggregationExplicitlyConfigured) {
			this.aggregatedScore = aggregatedScore;
			this.evaluatorScores = evaluatorScores;
			this.aggregationExplicitlyConfigured = aggregationExplicitlyConfigured;
		}

		public double getAggregatedScore() {
			return aggregatedScore;
		}

		/**
		 * @return true if an aggregation strategy was explicitly set via config
		 *         (modality/attribute/default key); false if the orchestrator fell
		 *         back to its built-in MEAN default because nothing was configured.
		 */
		public boolean isAggregationExplicitlyConfigured() {
			return aggregationExplicitlyConfigured;
		}

		public Map<String, Double> getEvaluatorScores() {
			return evaluatorScores;
		}
	}

	/**
	 * Orchestrates the quality evaluation for a given biometric DTO.
	 *
	 * <p>Steps:
	 * <ol>
	 *   <li>Reads which evaluators are configured for the modality/attribute.</li>
	 *   <li>Runs each evaluator and collects scores.</li>
	 *   <li>Selects the configured aggregation strategy and aggregates scores.</li>
	 *   <li>Returns the aggregated score together with the individual evaluator scores.</li>
	 * </ol>
	 *
	 * @param biometricsDto the captured biometric data
	 * @return the aggregated final quality score (0-100) plus the raw per-evaluator scores
	 * @throws RegBaseCheckedException if no evaluators are configured or evaluation fails
	 */
	public OrchestrationResult orchestrate(BiometricsDto biometricsDto) throws RegBaseCheckedException {
		String bioAttribute = biometricsDto.getBioAttribute();
		LOGGER.info("BiometricQualityOrchestrator: Starting quality orchestration for attribute {}", bioAttribute);

		// 1. Resolve modality (FINGER, IRIS, FACE) from bioAttribute for config lookup
		String modality = resolveModality(bioAttribute);

		
		String modalityKeyUpper = RegistrationConstants.QUALITY_EVALUATORS_MODALITY_PREFIX + modality.toUpperCase();
		String modalityKeyLower = RegistrationConstants.QUALITY_EVALUATORS_MODALITY_PREFIX + modality.toLowerCase();
		String attributeKey     = RegistrationConstants.QUALITY_EVALUATORS_PREFIX + bioAttribute;
		String defaultKey       = RegistrationConstants.QUALITY_EVALUATORS_PREFIX + "default";

		// Hierarchy: attribute > modality > default. Attribute is the most specific
		// key, so it must win over a modality-wide setting - checking modality first
		// would make a per-attribute override (e.g. ...evaluators.leftIndex) unable
		// to ever take effect on a deployment that also sets a per-modality key
		// (e.g. ...evaluators.modality.FINGER).
		String evaluatorConfig;
		if (ApplicationContext.map().containsKey(attributeKey)) {
			evaluatorConfig = (String) ApplicationContext.map().get(attributeKey);
			LOGGER.info("BiometricQualityOrchestrator: Using attribute config '{}' for attribute {}", attributeKey, bioAttribute);
		} else if (ApplicationContext.map().containsKey(modalityKeyUpper)) {
			evaluatorConfig = (String) ApplicationContext.map().get(modalityKeyUpper);
			LOGGER.info("BiometricQualityOrchestrator: Using modality config '{}' for modality {} (attribute {})",
					modalityKeyUpper, modality, bioAttribute);
		} else if (ApplicationContext.map().containsKey(modalityKeyLower)) {
			evaluatorConfig = (String) ApplicationContext.map().get(modalityKeyLower);
			LOGGER.info("BiometricQualityOrchestrator: Using modality config '{}' for modality {} (attribute {})",
					modalityKeyLower, modality, bioAttribute);
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
			safeAudit(AuditEvent.QUALITY_ORCH_FAILED, bioAttribute, "NO_EVALUATOR");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorCode(),
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorMessage());
		}

		// 3. Run each evaluator and collect scores.
		// A configured evaluator that fails (invalid score, timeout, exception) blocks
		// immediately and does NOT silently fall back to the remaining sources - only
		// an evaluator that was never configured/selected is skipped silently.
		Map<String, Double> scores = new HashMap<>();
		for (IBiometricQualityEvaluator evaluator : selectedEvaluators) {
			LOGGER.info("BiometricQualityOrchestrator: Running evaluator {} for attribute {}", evaluator.getEvaluatorName(), bioAttribute);
			try {
				QualityScore qs = evaluator.evaluate(biometricsDto);
				// A null result is an Invalid Score, not a source to silently skip -
				// today both built-in evaluators throw instead of returning null, but
				// a third-party evaluator returning null must not let the capture
				// proceed on a partial score set (Error Scenarios: no silent fallback).
				if (qs == null) {
					// Caught and audited by the catch block immediately below.
					throw new RegBaseCheckedException(
							RegistrationExceptionConstants.REG_SDK_INVALID_SCORE.getErrorCode(),
							RegistrationExceptionConstants.REG_SDK_INVALID_SCORE.getErrorMessage());
				}
				scores.put(evaluator.getEvaluatorName(), (double) qs.getScore());
			} catch (RegBaseCheckedException e) {
				LOGGER.error("BiometricQualityOrchestrator: Evaluator {} failed for attribute {}: {}",
						evaluator.getEvaluatorName(), bioAttribute, e.getMessage());
				safeAudit(AuditEvent.QUALITY_ORCH_FAILED, bioAttribute, evaluator.getEvaluatorName() + "_FAILED");
				throw e;
			}
		}

		if (scores.isEmpty()) {
			LOGGER.error("BiometricQualityOrchestrator: All evaluators failed for attribute {}", bioAttribute);
			safeAudit(AuditEvent.QUALITY_ORCH_FAILED, bioAttribute, "ALL_EVALUATORS_FAILED");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorCode(),
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorMessage());
		}

		// 4. Read the configured aggregation strategy (Hierarchy: attribute > modality > default)
		String aggModalityKeyUpper = RegistrationConstants.QUALITY_AGGREGATION_MODALITY_PREFIX + modality.toUpperCase();
		String aggModalityKeyLower = RegistrationConstants.QUALITY_AGGREGATION_MODALITY_PREFIX + modality.toLowerCase();
		String aggAttributeKey     = RegistrationConstants.QUALITY_AGGREGATION_PREFIX + bioAttribute;
		String aggDefaultKey       = RegistrationConstants.QUALITY_AGGREGATION_PREFIX + "default";

		// Scoped strictly to spring.properties / mosip-application.properties: a DB
		// global-param or local-preference override does NOT count as "configured"
		// here, only the build's own config files do.
		boolean aggregationExplicitlyConfigured = io.mosip.registration.config.DaoConfig.isKeyPresentInPropertiesFile(aggModalityKeyUpper)
				|| io.mosip.registration.config.DaoConfig.isKeyPresentInPropertiesFile(aggModalityKeyLower)
				|| io.mosip.registration.config.DaoConfig.isKeyPresentInPropertiesFile(aggAttributeKey)
				|| io.mosip.registration.config.DaoConfig.isKeyPresentInPropertiesFile(aggDefaultKey);

		// When the strategy is explicitly set in the config files, take the value
		// straight from the file (not the merged runtime map, which can carry a
		// stale DB global-param or local-preference override on top of it).
		// Attribute is checked before modality here too, for the same reason as
		// the evaluator lookup above: it's the more specific key and must win.
		String strategyName;
		if (aggregationExplicitlyConfigured) {
			if (io.mosip.registration.config.DaoConfig.isKeyPresentInPropertiesFile(aggAttributeKey)) {
				strategyName = io.mosip.registration.config.DaoConfig.getPropertyValueFromFile(aggAttributeKey);
			} else if (io.mosip.registration.config.DaoConfig.isKeyPresentInPropertiesFile(aggModalityKeyUpper)) {
				strategyName = io.mosip.registration.config.DaoConfig.getPropertyValueFromFile(aggModalityKeyUpper);
			} else if (io.mosip.registration.config.DaoConfig.isKeyPresentInPropertiesFile(aggModalityKeyLower)) {
				strategyName = io.mosip.registration.config.DaoConfig.getPropertyValueFromFile(aggModalityKeyLower);
			} else {
				strategyName = io.mosip.registration.config.DaoConfig.getPropertyValueFromFile(aggDefaultKey);
			}
		} else {
			strategyName = "MEAN";
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
				.orElse(null);

		if (selectedAggregator == null) {
			if (aggregationExplicitlyConfigured) {
				// Configuration Error, not a silent MEAN substitution: an administrator
				// configured a strategy name (e.g. aggregation.modality.FINGER) that
				// doesn't match any registered aggregator - most likely a typo. Silently
				// falling back would quietly change the scoring method underneath them,
				// the same failure mode already fixed for FORMULA-without-expression.
				LOGGER.error("BiometricQualityOrchestrator: Configured aggregation strategy '{}' does not match any registered aggregator for attribute {}",
						strategyName, bioAttribute);
				safeAudit(AuditEvent.QUALITY_ORCH_FAILED, bioAttribute, "AGGREGATION_CONFIG_ERROR");
				throw new RegBaseCheckedException(
						RegistrationExceptionConstants.REG_QUALITY_CONFIG_ERROR.getErrorCode(),
						RegistrationExceptionConstants.REG_QUALITY_CONFIG_ERROR.getErrorMessage());
			}
			// Nothing was configured at all - "MEAN" is the built-in default. Its
			// absence is a wiring/packaging problem (MeanScoreAggregator not on the
			// classpath / not a Spring bean), not an administrator config mistake.
			LOGGER.error("BiometricQualityOrchestrator: No aggregator found for strategy {}", strategyName);
			safeAudit(AuditEvent.QUALITY_ORCH_FAILED, bioAttribute, "NO_AGGREGATOR");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorCode(),
					RegistrationExceptionConstants.REG_NO_QUALITY_SOURCE.getErrorMessage());
		}

		// Resolve the FORMULA strategy's SpEL expression (per-attribute, else
		// default) and pass it directly into aggregate() rather than through
		// ambient/thread-local state - a value set that way could otherwise leak
		// from one thread's FORMULA aggregation into a later, differently
		// configured evaluation on the same (pooled) thread. Non-FORMULA
		// strategies simply ignore the parameter.
		String formulaExpr = null;
		if ("FORMULA".equalsIgnoreCase(finalStrategyName)) {
			String formulaAttrKey = RegistrationConstants.QUALITY_FORMULA_PREFIX + bioAttribute;
			String formulaDefaultKey = RegistrationConstants.QUALITY_FORMULA_PREFIX + "default";
			formulaExpr = (String) ApplicationContext.map().get(formulaAttrKey);
			if (formulaExpr == null) {
				formulaExpr = (String) ApplicationContext.map().get(formulaDefaultKey);
			}
		}

		double aggregatedScore = selectedAggregator.aggregate(scores, weights, formulaExpr);
		LOGGER.info("BiometricQualityOrchestrator: Aggregated score {} using strategy {} for attribute {}",
				aggregatedScore, finalStrategyName, bioAttribute);

		safeAudit(AuditEvent.QUALITY_ORCH_COMPLETED, bioAttribute, "ORCH_DONE");
		return new OrchestrationResult(aggregatedScore, scores, aggregationExplicitlyConfigured);
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
			// type == null means bioAttribute genuinely doesn't match any known
			// Biometric constant (not an exception) - falls through to the string
			// matching below, same as any other unrecognized attribute.
		} catch (RuntimeException e) {
			// Biometric.getSingleTypeByAttribute is a plain linear scan over a fixed
			// enum and should never throw for a real, valid attribute - if it does,
			// that's a real bug (e.g. a malformed entry in the library) worth seeing,
			// not papering over silently at DEBUG. Narrowed from Throwable so Errors
			// (OutOfMemoryError etc.) still propagate instead of being swallowed.
			LOGGER.error("BiometricQualityOrchestrator: Biometric.getSingleTypeByAttribute threw for attribute {} - falling back to string matching",
					bioAttribute, e);
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
