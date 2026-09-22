package io.mosip.registration.service.bio.quality.aggregator;

import io.mosip.registration.service.bio.quality.IBiometricScoreAggregator;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Strategy implementation for mean (average) score aggregation.
 */
@Component
public class MeanScoreAggregator implements IBiometricScoreAggregator {

	@Override
	public double aggregate(Map<String, Double> scores, Map<String, Double> w, String formulaExpression) {
		if (scores == null || scores.isEmpty()) {
			return 0.0;
		}
		return scores.values().stream()
				.mapToDouble(Double::doubleValue)
				.average().orElse(0.0);
	}

	@Override
	public String getStrategyName() {
		return "MEAN";
	}
}
