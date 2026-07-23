package io.mosip.registration.service.bio.quality.config;

/**
 * Thread-local context holder for evaluating SpEL expression context.
 */
public final class FormulaContext {

	private static final ThreadLocal<String> FORMULA = new ThreadLocal<>();

	private FormulaContext() {
		// Private constructor to prevent instantiation
	}

	public static void setFormula(String f) {
		FORMULA.set(f);
	}

	public static String getFormula() {
		return FORMULA.get();
	}

	public static void clear() {
		FORMULA.remove();
	}
}
