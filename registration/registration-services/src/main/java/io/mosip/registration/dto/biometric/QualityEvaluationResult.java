package io.mosip.registration.dto.biometric;

/**
 * Holds the outcome of a biometric quality evaluation, including the score,
 * the source that produced it (SDK or SBI), and whether a fallback occurred.
 */
public class QualityEvaluationResult {

	private final double score;
	private final String source;
	private final boolean fallback;

	public QualityEvaluationResult(double score, String source, boolean fallback) {
		this.score = score;
		this.source = source;
		this.fallback = fallback;
	}

	public double getScore() {
		return score;
	}

	public String getSource() {
		return source;
	}

	public boolean isFallback() {
		return fallback;
	}

	@Override
	public String toString() {
		return "QualityEvaluationResult{score=" + score + ", source='" + source + "', fallback=" + fallback + "}";
	}
}
