package io.mosip.registration.service.bio.quality.aggregator;

import io.mosip.registration.service.bio.quality.IBiometricScoreAggregator;
import io.mosip.registration.service.bio.quality.config.FormulaContext;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Strategy implementation for custom formula (SpEL-based) score aggregation.
 * 
 * @author Antigravity
 *
 */
@Component
public class FormulaScoreAggregator implements IBiometricScoreAggregator {

	private static final Logger LOGGER = AppConfig.getLogger(FormulaScoreAggregator.class);
	private final ExpressionParser parser = new SpelExpressionParser();

	@Override
	public double aggregate(Map<String, Double> scores, Map<String, Double> w) {
		String expr = FormulaContext.getFormula();
		if (expr == null || expr.trim().isEmpty()) {
			LOGGER.warn("FORMULA strategy set but no expression found; using mean");
			if (scores == null || scores.isEmpty()) {
				return 0.0;
			}
			return scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
		}

		StandardEvaluationContext ctx = new StandardEvaluationContext();
		ctx.setVariable("scores", scores);
		Double result = parser.parseExpression(expr).getValue(ctx, Double.class);
		if (result == null) {
			throw new IllegalStateException("Formula evaluated to null: " + expr);
		}
		return result;
	}

	@Override
	public String getStrategyName() {
		return "FORMULA";
	}
}
