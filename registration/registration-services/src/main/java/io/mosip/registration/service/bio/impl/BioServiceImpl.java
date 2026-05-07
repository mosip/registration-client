package io.mosip.registration.service.bio.impl;

import static io.mosip.registration.constants.LoggerConstants.BIO_SERVICE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.InputStream;
import java.time.temporal.ValueRange;
import java.util.*;
import java.util.concurrent.*;

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
import io.mosip.registration.dto.biometric.QualityEvaluationResult;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.bio.BioService;
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
	private IdentitySchemaService identitySchemaService;

	@Autowired
	private BIRBuilder birBuilder;

	private final ExecutorService sdkExecutor = Executors.newSingleThreadExecutor();

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

				if (!ValueRange.of(0, RegistrationConstants.MAX_BIO_QUALITY_SCORE).isValidValue((long) biometricsDto.getQualityScore()))
					throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_SCORE_RANGE_ERROR.getErrorCode(),
							RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_SCORE_RANGE_ERROR.getErrorMessage());

				QualityEvaluationResult result = evaluateQuality(biometricsDto);
				biometricsDto.setSdkScore(result.getScore());
				biometricsDto.setQualitySource(result.getSource());
				LOGGER.info("Quality evaluation complete: score={}, source={}, fallback={}",
						result.getScore(), result.getSource(), result.isFallback());
				list.add(biometricsDto);
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

	/**
	 * Orchestrates biometric quality evaluation across SDK and SBI sources.
	 *
	 * Decision rules:
	 * - SDK disabled in config: use SBI score (AS-03)
	 * - SDK enabled but provider unbound: fall back to SBI score (AS-02)
	 * - SDK enabled, provider bound, call fails (timeout/null/exception): block, no fallback
	 */
	private QualityEvaluationResult evaluateQuality(BiometricsDto biometricsDto) throws RegBaseCheckedException {
		boolean sdkEnabled = RegistrationConstants.ENABLE.equalsIgnoreCase(
				(String) ApplicationContext.map().getOrDefault(
						RegistrationConstants.QUALITY_CHECK_WITH_SDK, RegistrationConstants.DISABLE));

		if (!sdkEnabled) {
			LOGGER.info("SDK quality evaluation disabled, using SBI score for {}", biometricsDto.getBioAttribute());
			return new QualityEvaluationResult(biometricsDto.getQualityScore(), "SBI", false);
		}

		BiometricType biometricType = BiometricType
				.fromValue(io.mosip.commons.packet.constants.Biometric
						.getSingleTypeByAttribute(biometricsDto.getBioAttribute()).name());

		// Check provider availability before committing to SDK path
		Object provider = null;
		try {
			provider = bioAPIFactory.getBioProvider(biometricType, BiometricFunction.QUALITY_CHECK);
		} catch (Exception e) {
			LOGGER.warn("SDK provider not available for {}, falling back to SBI", biometricType);
		}

		if (provider == null) {
			LOGGER.info("SDK provider unbound, falling back to SBI score for {}", biometricsDto.getBioAttribute());
			double sbiScore = biometricsDto.getQualityScore();
			if (sbiScore <= 0) {
				LOGGER.error("SBI score also unavailable for {}", biometricsDto.getBioAttribute());
				throw new RegBaseCheckedException(
						RegistrationExceptionConstants.REG_SBI_SCORE_MISSING.getErrorCode(),
						RegistrationExceptionConstants.REG_SBI_SCORE_MISSING.getErrorMessage());
			}
			return new QualityEvaluationResult(sbiScore, "SBI", true);
		}

		// SDK provider is bound - call with timeout. Any failure here blocks registration.
		long timeoutMs = getSdkTimeoutMs();
		BIR bir = birBuilder.buildBir(biometricsDto, ProcessedLevelType.RAW);
		BIR[] birList = new BIR[]{bir};

		Future<Double> future = sdkExecutor.submit(() -> {
			try {
				Map<BiometricType, Float> scoreMap = bioAPIFactory
						.getBioProvider(biometricType, BiometricFunction.QUALITY_CHECK)
						.getModalityQuality(birList, null);
				return (scoreMap != null && scoreMap.get(biometricType) != null)
						? scoreMap.get(biometricType).doubleValue()
						: null;
			} catch (Exception e) {
				LOGGER.error("SDK provider threw during quality check", e);
				throw new RuntimeException(e);
			}
		});

		try {
			Double score = future.get(timeoutMs, TimeUnit.MILLISECONDS);
			if (score == null) {
				LOGGER.error("SDK returned null quality score for {}", biometricsDto.getBioAttribute());
				throw new RegBaseCheckedException(
						RegistrationExceptionConstants.REG_SDK_NULL_RESPONSE.getErrorCode(),
						RegistrationExceptionConstants.REG_SDK_NULL_RESPONSE.getErrorMessage());
			}
			LOGGER.info("SDK quality score {} received for {}", score, biometricsDto.getBioAttribute());
			return new QualityEvaluationResult(score, "SDK", false);

		} catch (TimeoutException e) {
			future.cancel(true);
			LOGGER.error("SDK quality evaluation timed out after {}ms for {}", timeoutMs, biometricsDto.getBioAttribute());
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_SDK_TIMEOUT.getErrorCode(),
					RegistrationExceptionConstants.REG_SDK_TIMEOUT.getErrorMessage());

		} catch (ExecutionException e) {
			LOGGER.error("SDK runtime error during quality evaluation for {}", biometricsDto.getBioAttribute(), e);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_SDK_RUNTIME_ERROR.getErrorCode(),
					RegistrationExceptionConstants.REG_SDK_RUNTIME_ERROR.getErrorMessage());

		} catch (RegBaseCheckedException e) {
			throw e;

		} catch (Exception e) {
			LOGGER.error("Unexpected error during SDK quality evaluation", e);
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_SDK_RUNTIME_ERROR.getErrorCode(),
					RegistrationExceptionConstants.REG_SDK_RUNTIME_ERROR.getErrorMessage());
		}
	}

	private long getSdkTimeoutMs() {
		String configured = getGlobalConfigValueOf(RegistrationConstants.SDK_QUALITY_TIMEOUT);
		try {
			return configured != null ? Long.parseLong(configured) : RegistrationConstants.SDK_QUALITY_TIMEOUT_DEFAULT_MS;
		} catch (NumberFormatException e) {
			return RegistrationConstants.SDK_QUALITY_TIMEOUT_DEFAULT_MS;
		}
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
					double effectiveScore = (biometricsDto.getSdkScore() > 0)
							? biometricsDto.getSdkScore()
							: biometricsDto.getQualityScore();
					if (effectiveScore <= 0) {
						LOGGER.warn("No valid quality score available for attribute {}", attribute);
						capturedContext.put(attribute, false);
						continue;
					}
					quality = quality + effectiveScore;
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
