package io.mosip.registration.service.bio.quality.aggregator;

import io.mosip.registration.service.bio.quality.IBiometricScoreAggregator;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Strategy implementation for priority-based score aggregation.
 * Lower priority value (ranking weight) indicates higher priority.
 */
@Component
public class PriorityScoreAggregator implements IBiometricScoreAggregator {

	@Override
	public double aggregate(Map<String, Double> scores, Map<String, Double> w, String formulaExpression) {
		if (scores == null || scores.isEmpty()) {
			return 0.0;
		}
		if (w == null || w.isEmpty()) {
			return scores.values().stream().mapToDouble(Double::doubleValue).findFirst().orElse(0.0);
		}
		// Iterate scores (insertion-ordered by the orchestrator, matching the
		// configured evaluator order) rather than w, and sort by Stream.sorted()
		// which is stable for an ordered source - so when two evaluators tie on
		// weight (e.g. both left at the default 1.0, unconfigured), the winner is
		// deterministically the one listed first in evaluators config, not
		// whichever HashMap bucket order happened to come out first.
		return scores.entrySet().stream()
				.filter(e -> w.containsKey(e.getKey()))
				.sorted((e1, e2) -> Double.compare(w.get(e1.getKey()), w.get(e2.getKey())))
				.findFirst().map(Map.Entry::getValue).orElse(0.0);
	}

	@Override
	public String getStrategyName() {
		return "PRIORITY";
	}
}
