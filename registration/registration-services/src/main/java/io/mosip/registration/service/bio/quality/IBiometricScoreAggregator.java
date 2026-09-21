package io.mosip.registration.service.bio.quality;

import java.util.Map;

/**
 * Interface for biometric score aggregation strategies.
 */
public interface IBiometricScoreAggregator {

	/**
	 * Aggregates multiple evaluator scores into a single score.
	 *
	 * @param scores the map of evaluator name to score value
	 * @param weights the map of evaluator name to configuration weight
	 * @param formulaExpression the configured SpEL expression (per-attribute,
	 *        else default) for the FORMULA strategy, or null/empty when none is
	 *        configured or the active strategy doesn't use one. Passed explicitly
	 *        rather than via ambient/thread-local state so a strategy never picks
	 *        up a value left over from a different call.
	 * @return the aggregated score
	 */
	double aggregate(Map<String, Double> scores, Map<String, Double> weights, String formulaExpression);

	/**
	 * Returns the strategy name matching the configuration (e.g. MEAN, MEDIAN, etc.)
	 * 
	 * @return the strategy name
	 */
	String getStrategyName();
}
