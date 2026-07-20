package io.mosip.registration.service.bio.quality.aggregator;

import io.mosip.registration.service.bio.quality.IBiometricScoreAggregator;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Strategy implementation for weighted average score aggregation.
 * 
 * @author Antigravity
 *
 */
@Component
public class WeightedAverageScoreAggregator implements IBiometricScoreAggregator {

	@Override
	public double aggregate(Map<String, Double> scores, Map<String, Double> w) {
		if (scores == null || scores.isEmpty()) {
			return 0.0;
		}
		double sum = 0, total = 0;
		for (Map.Entry<String, Double> e : scores.entrySet()) {
			double weight = (w != null) ? w.getOrDefault(e.getKey(), 1.0) : 1.0;
			sum += e.getValue() * weight;
			total += weight;
		}
		return total == 0 ? 0.0 : sum / total;
	}

	@Override
	public String getStrategyName() {
		return "WEIGHTED";
	}
}
