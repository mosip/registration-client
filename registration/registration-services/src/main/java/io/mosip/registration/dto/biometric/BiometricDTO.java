package io.mosip.registration.dto.biometric;

import java.util.LinkedHashMap;
import java.util.Map;

import io.mosip.registration.builder.Builder;
import io.mosip.registration.constants.RegistrationConstants;
import lombok.Data;

/**
 * This class contains the Biometric details
 * 
 * @author Dinesh Asokan
 * @since 1.0.0
 *
 */
@Data
public class BiometricDTO {
	
	private byte[] attributeISO;
	private String bioAttribute;
	private double qualityScore;
	private boolean isForceCaptured;
	private int numOfRetries;
	private long formatType;	
	private boolean isCaptured;

	//TODO need to remove below fields and handle them

	private Map<String, BiometricInfoDTO> biometricsMap;

	
	public BiometricDTO(String bioAttribute, byte[] attributeISO, double qualityScore) {
		this.bioAttribute = bioAttribute;
		this.attributeISO = attributeISO;
		this.qualityScore = qualityScore;
	}

	public BiometricDTO() {
		biometricsMap = new LinkedHashMap<>();
		biometricsMap.put(RegistrationConstants.supervisorBiometricDTO, new BiometricInfoDTO());
		biometricsMap.put(RegistrationConstants.operatorBiometricDTO, new BiometricInfoDTO());
	}

	public Map<String, BiometricInfoDTO> getBiometricsMap() {
		return biometricsMap;
	}

	public void setBiometricsMap(Map<String, BiometricInfoDTO> biometricsMap) {
		this.biometricsMap = biometricsMap;
	}

	public BiometricInfoDTO getSupervisorBiometricDTO() {
		return this.biometricsMap.get(RegistrationConstants.supervisorBiometricDTO);
	}

	public void setSupervisorBiometricDTO(BiometricInfoDTO supervisorBiometricDTO) {
		this.biometricsMap.put(RegistrationConstants.supervisorBiometricDTO, supervisorBiometricDTO);
	}

	public BiometricInfoDTO getOperatorBiometricDTO() {
		return this.biometricsMap.get(RegistrationConstants.operatorBiometricDTO);
	}

	public void setOperatorBiometricDTO(BiometricInfoDTO operatorBiometricDTO) {
		this.biometricsMap.put(RegistrationConstants.operatorBiometricDTO, operatorBiometricDTO);
	}

	public void addBiometricsToMap(String key, BiometricInfoDTO biometricDTO) {
		biometricsMap.put(key, biometricDTO);
	}
}
