package io.mosip.registration.service.bio.quality.aggregator;

import io.mosip.registration.service.bio.quality.IBiometricScoreAggregator;
import io.mosip.registration.service.bio.quality.config.FormulaContext;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strategy implementation for custom formula (SpEL-based) score aggregation.
 */
@Component
public class FormulaScoreAggregator implements IBiometricScoreAggregator {

	private static final Logger LOGGER = AppConfig.getLogger(FormulaScoreAggregator.class);
	private final ExpressionParser parser = new SpelExpressionParser();
	// Parsing a SpEL expression is comparatively expensive and formulas are
	// reused across many aggregate() calls (every attribute on every capture) -
	// cache the compiled Expression per formula string instead of reparsing it
	// each time.
	private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

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
		Expression compiledExpression = expressionCache.computeIfAbsent(expr, parser::parseExpression);
		Double result = compiledExpression.getValue(ctx, Double.class);
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
