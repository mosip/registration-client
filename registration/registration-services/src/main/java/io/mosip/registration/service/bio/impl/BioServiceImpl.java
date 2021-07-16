package io.mosip.registration.service.bio.impl;

import static io.mosip.registration.constants.LoggerConstants.BIO_SERVICE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.InputStream;
import java.util.*;

import io.micrometer.core.annotation.Timed;
import io.mosip.registration.dto.schema.UiSchemaDTO;
import io.mosip.registration.enums.Modality;
import io.mosip.registration.service.IdentitySchemaService;
import lombok.NonNull;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.bio.BioService;

/**
 * This class {@code BioServiceImpl} handles all the biometric captures and
 * validations through MDM service
 * 
 * @author taleev.aalam
 * @author anusha
 *
 */
@Service
public class BioServiceImpl extends BaseService implements BioService {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(BioServiceImpl.class);

	@Autowired
	private BioAPIFactory bioAPIFactory;

	@Autowired
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	@Autowired
	private IdentitySchemaService identitySchemaService;


	/**
	 * Gets the registration DTO from session.
	 *
	 * @return the registration DTO from session
	 */
	protected RegistrationDTO getRegistrationDTOFromSession() {
		return (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
	}

	@Override
	public List<BiometricsDto> captureModality(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException {
		return captureRealModality(mdmRequestDto);
	}

	private List<BiometricsDto> captureRealModality(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException {
		LOGGER.info("Entering into captureModality method.. {}", System.currentTimeMillis());
		List<BiometricsDto> list = new ArrayList<BiometricsDto>();
		MdmBioDevice bioDevice = deviceSpecificationFactory.getDeviceInfoByModality(mdmRequestDto.getModality());
		MosipDeviceSpecificationProvider deviceSpecificationProvider = deviceSpecificationFactory
				.getMdsProvider(bioDevice.getSpecVersion());
		List<BiometricsDto> biometricsDtos = deviceSpecificationProvider.rCapture(bioDevice, mdmRequestDto);

		
		for (BiometricsDto biometricsDto : biometricsDtos) {
			if (biometricsDto != null
					&& isQualityScoreMaxInclusive(String.valueOf(biometricsDto.getQualityScore()))) {
					try {
						biometricsDto.setSdkScore(getSDKScore(biometricsDto));
					} catch (Exception exception) {
						LOGGER.error("Unable to fetch SDK Score ", exception.getCause());
						if(RegistrationConstants.ENABLE.equalsIgnoreCase((String) ApplicationContext.map()
										 .getOrDefault(RegistrationConstants.QUALITY_CHECK_WITH_SDK, RegistrationConstants.DISABLE)) ) {
							throw new RegBaseCheckedException(
								RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_CHECK_ERROR.getErrorCode(),
								RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_CHECK_ERROR.getErrorMessage()
										+ ExceptionUtils.getStackTrace(exception));
						 }
					}

				  if(RegistrationConstants.ENABLE.equalsIgnoreCase((String) ApplicationContext.map().getOrDefault(
							RegistrationConstants.UPDATE_SDK_QUALITY_SCORE, RegistrationConstants.DISABLE))) {
						LOGGER.info("Update MDM Quality score with Biometric SDK score flag is enabled..");
						biometricsDto.setQualityScore(biometricsDto.getSdkScore());
				  }
				list.add(biometricsDto);
			}
		}
		LOGGER.info("Ended captureModality method.. {}" , System.currentTimeMillis());
		return list;
	}

	@Override
	public List<BiometricsDto> captureModalityForAuth(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException {
		LOGGER.info("Started {} capture for authentication at {} ", mdmRequestDto.getModality(), System.currentTimeMillis());
		if (deviceSpecificationFactory.isDeviceAvailable(mdmRequestDto.getModality()))
			return captureRealModality(mdmRequestDto);

		throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
					RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());
	}

	@Override
	public InputStream getStream(String modality) throws RegBaseCheckedException {
		LOGGER.info("Streaming {} request at {}",modality, System.currentTimeMillis());
		MdmBioDevice bioDevice = deviceSpecificationFactory.getDeviceInfoByModality(modality);
		LOGGER.info("Bio-device found {} at {}",modality, System.currentTimeMillis());
		return getStream(bioDevice, modality);
	}

	@Override
	public InputStream getStream(MdmBioDevice mdmBioDevice, String modality) throws RegBaseCheckedException {
		LOGGER.info(BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID, "Starting stream");

		if (deviceSpecificationFactory.isDeviceAvailable(mdmBioDevice)) {
			MosipDeviceSpecificationProvider deviceSpecificationProvider = deviceSpecificationFactory
					.getMdsProvider(mdmBioDevice.getSpecVersion());
			LOGGER.info("{} found for spec version {} at {}",deviceSpecificationProvider,
					mdmBioDevice.getSpecVersion(), System.currentTimeMillis());
			return deviceSpecificationProvider.stream(mdmBioDevice, modality);
		}
		throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
				RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());
	}

	private boolean isQualityScoreMaxInclusive(String qualityScore) {
		LOGGER.info(LoggerConstants.BIO_SERVICE, APPLICATION_NAME, APPLICATION_ID,
				"Checking for max inclusive quality score");

		if (qualityScore == null) {
			return false;
		}
		return Double.parseDouble(qualityScore) <= RegistrationConstants.MAX_BIO_QUALITY_SCORE;
	}

	@Timed(value = "sdk", extraTags = {"function", "QUALITY_CHECK"})
	@Override
	public double getSDKScore(BiometricsDto biometricsDto) throws BiometricException {
		BiometricType biometricType = BiometricType
				.fromValue(Biometric.getSingleTypeByAttribute(biometricsDto.getBioAttribute()).name());
		BIR bir = buildBir(biometricsDto);
		BIR[] birList = new BIR[] { bir };
		Map<BiometricType, Float> scoreMap = bioAPIFactory
				.getBioProvider(biometricType, BiometricFunction.QUALITY_CHECK)
				.getModalityQuality(birList, null);
		
		return scoreMap.get(biometricType);

		
	}

	@Override
	public Map<String, Boolean> getCapturedBiometrics(String fieldId, double idVersion,
													  @NonNull RegistrationDTO registrationDTO) {
		Map<String, Boolean> capturedContext = new HashMap<>();
		try {
			Optional<UiSchemaDTO> fieldDto = identitySchemaService.getUISchema(idVersion).stream().filter(field ->
				RegistrationConstants.BIOMETRICS_TYPE.equals(field.getType()) && field.getId().equals(fieldId)).findFirst();
			if(!fieldDto.isPresent())
				return capturedContext;

			Map<Modality, List<String>> groupedAttributes = getGroupedAttributes(fieldDto.get().getBioAttributes());
			for(Modality modality : groupedAttributes.keySet()) {
				double quality = 0;
				List<String> capturedAttributes = new ArrayList<>();
				//iterating through configured bio-attributes
				for(String attribute : groupedAttributes.get(modality)) {
					BiometricsDto biometricsDto = registrationDTO.getBiometric(fieldDto.get().getSubType(), attribute);
					//its null, then check exception list
					if(biometricsDto == null) {
						capturedContext.put(attribute, registrationDTO.isBiometricExceptionAvailable(fieldDto.get().getSubType(), attribute));
						continue;
					}
					//its force captured, not required to validate threshold
					if(biometricsDto.isForceCaptured()) {
						capturedContext.put(attribute, true);
						continue;
					}
					quality = quality + biometricsDto.getQualityScore();
					capturedAttributes.add(attribute);
				}
				//if some attributes are captured, determine capture status based on threshold check
				for(String attr : capturedAttributes) {
					capturedContext.put(attr, (quality / capturedAttributes.size()) >= getMDMQualityThreshold(modality));
				}
			}
		} catch (RegBaseCheckedException e) {
			LOGGER.error("Failed to fetch Id schema with version {} due to {}", idVersion, e);
		}
		LOGGER.info("Biometric field {} biometrics-captured-context >> {}", fieldId, capturedContext);
		return capturedContext;
	}

	@Override
	public Map<String, List<String>> getSupportedBioAttributes(@NonNull List<String> modalities) {
		Map<String, List<String>> configuredAttributes= new HashMap<>();
		modalities.forEach( modality -> {
			switch (modality) {
				case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
					configuredAttributes.put(modality, RegistrationConstants.leftHandUiAttributes);
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
					configuredAttributes.put(modality, RegistrationConstants.rightHandUiAttributes);
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
					configuredAttributes.put(modality, RegistrationConstants.twoThumbsUiAttributes);
					break;
				case RegistrationConstants.IRIS:
				case RegistrationConstants.IRIS_DOUBLE:
					configuredAttributes.put(modality, RegistrationConstants.eyesUiAttributes);
					break;
				case RegistrationConstants.FACE:
				case RegistrationConstants.FACE_FULLFACE:
					configuredAttributes.put(modality, RegistrationConstants.faceUiAttributes);
					break;
			}
		});
		return configuredAttributes;
	}


	private Map<Modality, List<String>> getGroupedAttributes(@NonNull List<String> attributes) {
		Map<Modality, List<String>> groupedAttributes = new HashMap<>();
		for(Modality modality : Modality.values()) {
			groupedAttributes.put(modality,	ListUtils.intersection(modality.getAttributes(), attributes));
		}
		return groupedAttributes;
	}


	@Override
	public double getMDMQualityThreshold(@NonNull Modality modality) {
		String thresholdScore = null;
		switch (modality) {
			case FINGERPRINT_SLAB_LEFT:
				thresholdScore = getGlobalConfigValueOf(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD);
				break;
			case FINGERPRINT_SLAB_RIGHT:
				thresholdScore = getGlobalConfigValueOf(RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD);
				break;
			case FINGERPRINT_SLAB_THUMBS:
				thresholdScore = getGlobalConfigValueOf(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD);
				break;
			case IRIS_DOUBLE:
				thresholdScore = getGlobalConfigValueOf(RegistrationConstants.IRIS_THRESHOLD);
				break;
			case FACE:
				thresholdScore = getGlobalConfigValueOf(RegistrationConstants.FACE_THRESHOLD);
				break;
		}
		return thresholdScore == null ? 0 : Double.valueOf(thresholdScore);
	}

	@Override
	public int getRetryCount(@NonNull Modality modality) {
		String retryCount = null;
		switch (modality) {
			case FACE:
				retryCount = getGlobalConfigValueOf(RegistrationConstants.FACE_RETRY_COUNT);
				break;
			case IRIS_DOUBLE:
				retryCount = getGlobalConfigValueOf(RegistrationConstants.IRIS_RETRY_COUNT);
				break;
			case FINGERPRINT_SLAB_RIGHT:
			case FINGERPRINT_SLAB_LEFT:
			case FINGERPRINT_SLAB_THUMBS:
				retryCount = getGlobalConfigValueOf(RegistrationConstants.FINGERPRINT_RETRIES_COUNT);
				break;
			case EXCEPTION_PHOTO:
				retryCount = getGlobalConfigValueOf(RegistrationConstants.PHOTO_RETRY_COUNT);
				break;
		}
		return retryCount == null ? 0 : Integer.valueOf(retryCount);
	}


}
