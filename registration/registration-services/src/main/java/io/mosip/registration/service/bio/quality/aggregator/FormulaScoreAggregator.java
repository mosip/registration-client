package io.mosip.registration.service.bio.quality.aggregator;

import io.mosip.registration.service.bio.quality.IBiometricScoreAggregator;
import io.mosip.registration.service.bio.quality.config.FormulaContext;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import org.springframework.expression.Expression;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
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
			// Configuration Error per the Error Scenarios table: FORMULA selected with
			// no expression configured is a misconfigured quality gate, not a case to
			// fail open on - block and notify the administrator rather than silently
			// substituting MEAN.
			LOGGER.error("FORMULA strategy set but no expression configured");
			throw new RegBaseUncheckedException(
					RegistrationExceptionConstants.REG_QUALITY_CONFIG_ERROR.getErrorCode(),
					RegistrationExceptionConstants.REG_QUALITY_CONFIG_ERROR.getErrorMessage());
		}

		// expr is sourced from config (mosip.registration.quality.formula.*), which
		// is populated by server config sync - StandardEvaluationContext would let
		// a synced formula invoke arbitrary methods/constructors/type references
		// (e.g. T(java.lang.Runtime).getRuntime().exec(...)) on every machine that
		// syncs it. SimpleEvaluationContext's read-only data-binding mode disables
		// all of that while still allowing the arithmetic/comparison/map-lookup
		// expressions this feature actually needs.
		EvaluationContext ctx = SimpleEvaluationContext.forReadOnlyDataBinding().build();
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
