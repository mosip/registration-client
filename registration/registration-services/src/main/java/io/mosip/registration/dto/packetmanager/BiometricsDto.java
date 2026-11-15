package io.mosip.registration.dto.packetmanager;

import io.mosip.commons.packet.constants.Biometric;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiometricsDto {
	
	private byte[] attributeISO;
	private String bioAttribute;
	private String bioSubType;
	private String modalityName;
	// Sentinel, not 0.0: qualityScore is a primitive, so a BiometricsDto built
	// via the no-arg constructor and never given a score would otherwise read
	// as an indistinguishable "real capture that scored 0" instead of "no SBI
	// score was ever reported" - SbiQualityEvaluator relies on this to detect
	// the Missing SBI Score scenario (0-100 is the valid range for a real
	// score, so anything negative can only be the sentinel).
	private double qualityScore = -1.0;
	private boolean isForceCaptured;
	private int numOfRetries;
	private boolean isCaptured;
	private String subType;
	// Same sentinel reasoning as qualityScore above: sdkScore/aggregatedScore
	// are only ever written when that source actually produced a score
	// (BioServiceImpl only calls setSdkScore/setAggregatedScore when the SDK
	// evaluator ran / an aggregation strategy was explicitly configured), so a
	// legitimately-computed score of exactly 0 must stay distinguishable from
	// "this source never ran" - both getDisplayScore() below and every UI
	// presence check (e.g. GenericBiometricsController#setCapturedValues) rely
	// on that distinction via a >= 0 check rather than > 0.
	private double sdkScore = -1.0;
	private double aggregatedScore = -1.0;
	// The aggregation strategy (MEAN, MEDIAN, WEIGHTED_AVERAGE, PRIORITY, FORMULA)
	// that actually produced aggregatedScore - set alongside it, so the packet
	// records how the number was derived, not just the number itself.
	private String aggregationStrategy;
	private String payLoad;
	private String signature;
	private String specVersion;
	

		
	public BiometricsDto(String bioAttribute, byte[] attributeISO, double qualityScore) {
		this.bioAttribute = bioAttribute;
		this.attributeISO = attributeISO;
		this.qualityScore = qualityScore;
		this.modalityName = Biometric.getModalityNameByAttribute(bioAttribute);
	}

	/**
	 * The score actually shown/used for threshold decisions: aggregate if the
	 * aggregation strategy was explicitly configured, else SDK, else raw SBI.
	 * Single source of truth for this precedence - callers should not
	 * reimplement it inline.
	 */
	public double getDisplayScore() {
		return aggregatedScore >= 0 ? aggregatedScore
				: sdkScore >= 0 ? sdkScore
				: qualityScore;
	}
}
