package io.mosip.registration.service.bio.quality.aggregator;

import io.mosip.registration.service.bio.quality.IBiometricScoreAggregator;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Strategy implementation for priority-based score aggregation.
 * Lower priority value (ranking weight) indicates higher priority.
 * 
 * @author Antigravity
 *
 */
@Component
public class PriorityScoreAggregator implements IBiometricScoreAggregator {

	@Override
	public double aggregate(Map<String, Double> scores, Map<String, Double> w) {
		if (scores == null || scores.isEmpty()) {
			return 0.0;
		}
		if (w == null || w.isEmpty()) {
			return scores.values().stream().mapToDouble(Double::doubleValue).findFirst().orElse(0.0);
		}
		return w.entrySet().stream()
				.sorted(Map.Entry.comparingByValue())
				.filter(e -> scores.containsKey(e.getKey()))
				.mapToDouble(e -> scores.get(e.getKey()))
				.findFirst().orElse(0.0);
	}

	@Override
	public String getStrategyName() {
		return "PRIORITY";
	}
}
