package io.mosip.registration.service.bio.impl;

import static io.mosip.registration.constants.LoggerConstants.BIO_SERVICE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.InputStream;
import java.time.temporal.ValueRange;
import java.util.*;

import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.enums.Modality;
import io.mosip.registration.service.IdentitySchemaService;
import lombok.NonNull;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.commons.packet.constants.Biometric;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.ProcessedLevelType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
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
import io.mosip.registration.service.bio.quality.BiometricQualityOrchestrator;
import io.mosip.registration.util.common.BIRBuilder;

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
	private BIRBuilder birBuilder;

	@Autowired
	private BiometricQualityOrchestrator biometricQualityOrchestrator;

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
		LOGGER.info("Entering into captureModality method.. {}", System.currentTimeMillis());
		List<BiometricsDto> list = new ArrayList<BiometricsDto>();

		try {
			MdmBioDevice bioDevice = deviceSpecificationFactory.getDeviceInfoByModality(mdmRequestDto.getModality());
			MosipDeviceSpecificationProvider deviceSpecificationProvider = deviceSpecificationFactory
					.getMdsProvider(bioDevice.getSpecVersion());
			List<BiometricsDto> biometricsDtos = deviceSpecificationProvider.rCapture(bioDevice, mdmRequestDto);

			for (BiometricsDto biometricsDto : biometricsDtos) {
				if (biometricsDto == null) {
					continue;
				}

				// Corrupt Data: the device returned a segment with no readable biometric
				// payload at all - reject it outright rather than scoring garbage.
				if (biometricsDto.getAttributeISO() == null || biometricsDto.getAttributeISO().length == 0) {
					LOGGER.error("BioServiceImpl: Corrupt/unreadable biometric data for attribute {}",
							biometricsDto.getBioAttribute());
					throw new RegBaseCheckedException(
							RegistrationExceptionConstants.REG_CORRUPT_BIOMETRIC_DATA.getErrorCode(),
							RegistrationExceptionConstants.REG_CORRUPT_BIOMETRIC_DATA.getErrorMessage());
				}

				if (!ValueRange.of(0, RegistrationConstants.MAX_BIO_QUALITY_SCORE).isValidValue((long) biometricsDto.getQualityScore()))
					throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_SCORE_RANGE_ERROR.getErrorCode(),
							RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_SCORE_RANGE_ERROR.getErrorMessage());

				if (RegistrationConstants.ENABLE.equalsIgnoreCase((String) ApplicationContext.map()
						.getOrDefault(RegistrationConstants.QUALITY_CHECK_WITH_SDK, RegistrationConstants.DISABLE))) {
					// Route through the BiometricQualityOrchestrator for multi-source evaluation.
					// A configured source (SDK or SBI) that fails blocks here - it is NOT caught
					// and silently worked around; it propagates up to the caller as-is so the
					// operator sees the specific error and is prompted to re-capture.
					BiometricQualityOrchestrator.OrchestrationResult orchestrationResult =
							biometricQualityOrchestrator.orchestrate(biometricsDto);
					// Only surface the aggregate when a strategy was explicitly configured;
					// otherwise leave it unset so the UI falls back to the raw SDK score.
					if (orchestrationResult.isAggregationExplicitlyConfigured()) {
						biometricsDto.setAggregatedScore(orchestrationResult.getAggregatedScore());
						biometricsDto.setAggregationStrategy(orchestrationResult.getAggregationStrategy());
					}
					Double sdkOnlyScore = orchestrationResult.getEvaluatorScores().get("SDK");
					if (sdkOnlyScore != null) {
						biometricsDto.setSdkScore(sdkOnlyScore);
					}

					// Enforce re-capture when the score that will actually be shown/used
					// (aggregate, else SDK, else raw SBI) falls below the existing threshold.
					double displayScore = biometricsDto.getDisplayScore();
					Modality modality = Modality.getModality(biometricsDto.getBioAttribute());
					double threshold = getMDMQualityThreshold(modality);
					// Configuration Error only when the threshold key is genuinely missing or
					// unparseable - NOT when an administrator has explicitly configured 0
					// (which legitimately means "no threshold gating for this modality").
					// getMDMQualityThreshold() collapses both cases to 0, so the distinction
					// has to be made via isMDMQualityThresholdConfigured() instead of just
					// checking threshold <= 0.
					if (threshold <= 0 && !isMDMQualityThresholdConfigured(modality)) {
						LOGGER.error("BioServiceImpl: Missing/unparseable quality threshold config for attribute {}",
								biometricsDto.getBioAttribute());
						throw new RegBaseCheckedException(
								RegistrationExceptionConstants.REG_QUALITY_CONFIG_ERROR.getErrorCode(),
								RegistrationExceptionConstants.REG_QUALITY_CONFIG_ERROR.getErrorMessage());
					}
					if (displayScore < threshold) {
						// Below-threshold does NOT abort the capture outright - the operator is
						// still prompted to re-capture (via the existing per-attempt retry UI,
						// which now shows this attempt's real score instead of losing it), and
						// once retries are exhausted the best-scoring attempt is force-accepted
						// (RegistrationDTO#addAllBiometrics). Hard-blocking here with an
						// exception would discard the score entirely instead of recording it
						// and letting that mechanism run.
						LOGGER.info("BioServiceImpl: Quality score {} below threshold {} for attribute {} - recorded, re-capture will be prompted",
								displayScore, threshold, biometricsDto.getBioAttribute());
					}
				}
				list.add(biometricsDto);
			}

			// Partial Capture: the device returned fewer biometric segments than this
			// request required. mdmRequestDto.getCount() is already net of exceptions
			// by the time it reaches here: the active caller,
			// GenericBiometricsController#rCapture(), computes
			// modality.getAttributes().size() - exceptionBioAttributes.size() before
			// building the MDMRequestDto, and the 0.9.5 MDS provider independently
			// recomputes the same net-of-exceptions count in its own getCount() before
			// overwriting it - so an exception-marked finger does not trip this check
			// (see BioServiceTest#captureModality_oneAttributeMarkedException_notBlockedAsPartialCapture).
			if (list.size() < mdmRequestDto.getCount()) {
				LOGGER.error("BioServiceImpl: Partial capture for modality {} - expected {} got {}",
						mdmRequestDto.getModality(), mdmRequestDto.getCount(), list.size());
				throw new RegBaseCheckedException(
						RegistrationExceptionConstants.REG_PARTIAL_CAPTURE.getErrorCode(),
						RegistrationExceptionConstants.REG_PARTIAL_CAPTURE.getErrorMessage());
			}
		} catch (RegBaseCheckedException e) {
			throw e;
		} catch (Throwable t) {
			LOGGER.error("Failed in rcapture", t);
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorCode(),
					RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorMessage());
		}
		LOGGER.info("Ended captureModality method.. {}" , System.currentTimeMillis());
		return list;
	}


	@Override
	public List<BiometricsDto> captureModalityForAuth(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException {
		LOGGER.info("Started {} capture for authentication at {} ", mdmRequestDto.getModality(), System.currentTimeMillis());
		if (deviceSpecificationFactory.isDeviceAvailable(mdmRequestDto.getModality()))
			return captureModality(mdmRequestDto);

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

			try {
				return deviceSpecificationProvider.stream(mdmBioDevice, modality);
			} catch (Throwable t) {
				LOGGER.error("Failed to stream / streaming interrupted", t);
			}
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_STREAM_ERROR.getErrorCode(),
					RegistrationExceptionConstants.MDS_STREAM_ERROR.getErrorMessage());
		}
		throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
				RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());
	}

	@Override
	public double getSDKScore(BiometricsDto biometricsDto) throws BiometricException {
		BiometricType biometricType = BiometricType
				.fromValue(Biometric.getSingleTypeByAttribute(biometricsDto.getBioAttribute()).name());
		BIR bir = birBuilder.buildBir(biometricsDto, ProcessedLevelType.RAW);
		BIR[] birList = new BIR[] { bir };
		Map<BiometricType, Float> scoreMap = bioAPIFactory
				.getBioProvider(biometricType, BiometricFunction.QUALITY_CHECK)
				.getModalityQuality(birList, null);
		
		return scoreMap.get(biometricType);
	}

	@Override
	public Map<String, Boolean> getCapturedBiometrics(@NonNull UiFieldDTO fieldDto, double idVersion,
                                                      @NonNull RegistrationDTO registrationDTO) {
		Map<String, Boolean> capturedContext = new HashMap<>();
//		try {
			Map<Modality, List<String>> groupedAttributes = getGroupedAttributes(fieldDto.getBioAttributes());
			for(Modality modality : groupedAttributes.keySet()) {
				double quality = 0;
				List<String> capturedAttributes = new ArrayList<>();
				//iterating through configured bio-attributes
				for(String attribute : groupedAttributes.get(modality)) {
					BiometricsDto biometricsDto = registrationDTO.getBiometric(fieldDto.getId(), attribute);
					//its null, then check exception list
					if(biometricsDto == null) {
						capturedContext.put(attribute, registrationDTO.isBiometricExceptionAvailable(fieldDto.getId(), attribute));
						continue;
					}
					//its force captured, not required to validate threshold
					if(biometricsDto.isForceCaptured()) {
						capturedContext.put(attribute, true);
						continue;
					}
					quality = quality + biometricsDto.getDisplayScore();
					capturedAttributes.add(attribute);
				}
				//if some attributes are captured, determine capture status based on threshold check
				for(String attr : capturedAttributes) {
					capturedContext.put(attr, (quality / capturedAttributes.size()) >= getMDMQualityThreshold(modality));
				}
			}
		/*} catch (RegBaseCheckedException e) {
			LOGGER.error("Failed to fetch Id schema with version {} due to {}", idVersion, e);
		}*/
		LOGGER.info("Biometric field {} biometrics-captured-context >> {}", fieldDto.getId(), capturedContext);
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
		String thresholdScore = getGlobalConfigValueOf(thresholdConfigKeyFor(modality));
		if (thresholdScore == null) {
			return 0;
		}
		try {
			return Double.valueOf(thresholdScore);
		} catch (NumberFormatException nfe) {
			LOGGER.error("BioServiceImpl: Unparseable quality threshold '{}' configured for modality {}",
					thresholdScore, modality);
			return 0;
		}
	}

	/**
	 * Whether a quality threshold is actually configured (present and
	 * parseable) for this modality, as opposed to {@link #getMDMQualityThreshold}
	 * returning 0 because the key is missing/invalid vs. an administrator
	 * explicitly configuring 0 (disabling threshold gating). Callers that need
	 * to tell "misconfigured" apart from "intentionally zero" should check
	 * this rather than only looking at whether the threshold is <= 0.
	 */
	@Override
	public boolean isMDMQualityThresholdConfigured(@NonNull Modality modality) {
		String thresholdScore = getGlobalConfigValueOf(thresholdConfigKeyFor(modality));
		if (thresholdScore == null) {
			return false;
		}
		try {
			Double.valueOf(thresholdScore);
			return true;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

	private String thresholdConfigKeyFor(Modality modality) {
		switch (modality) {
			case FINGERPRINT_SLAB_LEFT:
				return RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD;
			case FINGERPRINT_SLAB_RIGHT:
				return RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD;
			case FINGERPRINT_SLAB_THUMBS:
				return RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD;
			case IRIS_DOUBLE:
				return RegistrationConstants.IRIS_THRESHOLD;
			case FACE:
				return RegistrationConstants.FACE_THRESHOLD;
			default:
				return null;
		}
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