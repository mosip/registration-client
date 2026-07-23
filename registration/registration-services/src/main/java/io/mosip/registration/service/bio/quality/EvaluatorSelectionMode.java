package io.mosip.registration.service.bio.quality;

/**
 * Selection mode defining how configured quality evaluators are executed and resolved.
 */
public enum EvaluatorSelectionMode {

	/**
	 * Run all configured evaluators. Any evaluator failure will fail the entire check.
	 */
	ALL,

	/**
	 * Run configured evaluators until the first success wins. Individual failures are silently skipped.
	 */
	ANY_ONE,

	/**
	 * Run only the subset of configured evaluators specified.
	 */
	SUBSET
}
