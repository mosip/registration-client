package io.mosip.registration.service.bio.quality.aggregator;

import io.mosip.registration.service.bio.quality.IBiometricScoreAggregator;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Strategy implementation that takes the highest score among all evaluators
 * (e.g. picks whichever of SBI/SDK reported the better quality).
 */
@Component
public class MaxScoreAggregator implements IBiometricScoreAggregator {

	@Override
	public double aggregate(Map<String, Double> scores, Map<String, Double> w, String formulaExpression) {
		if (scores == null || scores.isEmpty()) {
			return 0.0;
		}
		return scores.values().stream()
				.mapToDouble(Double::doubleValue)
				.max()
				.orElse(0.0);
	}

	@Override
	public String getStrategyName() {
		return "MAX";
	}
}
