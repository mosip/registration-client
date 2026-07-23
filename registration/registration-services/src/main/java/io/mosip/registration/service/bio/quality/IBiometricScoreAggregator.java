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
	 * @return the aggregated score
	 */
	double aggregate(Map<String, Double> scores, Map<String, Double> weights);

	/**
	 * Returns the strategy name matching the configuration (e.g. MEAN, MEDIAN, etc.)
	 * 
	 * @return the strategy name
	 */
	String getStrategyName();
}
