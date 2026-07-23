package io.mosip.registration.service.bio.quality.aggregator;

import io.mosip.registration.service.bio.quality.IBiometricScoreAggregator;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy implementation for median score aggregation.
 */
@Component
public class MedianScoreAggregator implements IBiometricScoreAggregator {

	@Override
	public double aggregate(Map<String, Double> scores, Map<String, Double> w) {
		if (scores == null || scores.isEmpty()) {
			return 0.0;
		}
		List<Double> sorted = scores.values().stream()
				.sorted()
				.collect(Collectors.toList());
		int n = sorted.size();
		return (n % 2 == 0)
				? (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0
				: sorted.get(n / 2);
	}

	@Override
	public String getStrategyName() {
		return "MEDIAN";
	}
}
