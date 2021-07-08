package io.mosip.registration.service.bio;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.enums.Modality;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import lombok.NonNull;

/**
 * This class {@code BioService} handles all the biometric captures and
 * validations through MDM service
 * 
 * @author taleev.aalam
 * @since 1.0.0
 */
public interface BioService {

	/**
	 *
	 * @param mdmRequestDto
	 * @return
	 * @throws RegBaseCheckedException
	 */
	public List<BiometricsDto> captureModality(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException;

	/**
	 *
	 * @param mdmRequestDto
	 * @return
	 * @throws RegBaseCheckedException
	 */
	public List<BiometricsDto> captureModalityForAuth(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException;

	/**
	 * @param modality modality to find device subId
	 * @return live stream
	 * @throws RegBaseCheckedException
	 */
	public InputStream getStream(String modality) throws RegBaseCheckedException;

	/**
	 * @param mdmBioDevice bio Device info
	 * @param modality     modality to find device subId
	 * @return live stream
	 * @throws RegBaseCheckedException
	 */
	public InputStream getStream(MdmBioDevice mdmBioDevice, String modality) throws RegBaseCheckedException;

	/**
	 * @param biometricsDto captured biometrics dto
	 * 
	 * @return bir builded for biometrics dto
	 * 
	 */
	public BIR buildBir(BiometricsDto biometricsDto);

	/**
	 * @param bioAttribute biometric segment name
	 * @param qualityScore biometric quality score
	 * @param iso          biometric value in iso format
	 * @return bir builder for biometric
	 */
	public BIR buildBir(String bioAttribute, long qualityScore, byte[] iso, ProcessedLevelType processedLevelType);
	
	/**
	 * @param biometricsDto biometrics captured DTO
	 * @return sdk score
	 * @throws BiometricException 
	 */
	public double getSDKScore(BiometricsDto biometricsDto) throws BiometricException;

	/**
	 * Validates the list of biometricDTOs for the given fieldId as per UI spec validators and schema
	 * @param fieldId
	 * @param idVersion
	 * @param registrationDTO
	 * @return
	 */
	public Map<String, Boolean> getCapturedBiometrics(String fieldId, double idVersion,
													  @NonNull RegistrationDTO registrationDTO);

	/**
	 * Returns the list of supported and valid bio-attributes per modality
	 * @param modalities
	 * @return
	 */
	public Map<String, List<String>> getSupportedBioAttributes(List<String> modalities);

	/**
	 *
	 * @param modality
	 * @return
	 */
	public double getMDMQualityThreshold(@NonNull Modality modality);

	/**
	 *
	 * @param modality
	 * @return
	 */
	public int getRetryCount(@NonNull Modality modality);
}