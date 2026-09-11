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
	private double qualityScore;
	private boolean isForceCaptured;
	private int numOfRetries;	
	private boolean isCaptured;
	private String subType;
	private double sdkScore;
	private double aggregatedScore;
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
		return aggregatedScore > 0 ? aggregatedScore
				: sdkScore > 0 ? sdkScore
				: qualityScore;
	}
}
