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

	/** Default SDK quality evaluation timeout in milliseconds */
	private static final int DEFAULT_SDK_TIMEOUT_MS = 10000;

	@Autowired
	private BioAPIFactory bioAPIFactory;

	@Autowired
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	@Autowired
	private IdentitySchemaService identitySchemaService;

	@Autowired
	private BIRBuilder birBuilder;

	/**
	 * Gets the registration DTO from session.
	 *
	 * @return the registration DTO from session
	 */
	protected RegistrationDTO getRegistrationDTOFromSession() {
		return (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
	}

	/**
	 * Checks if SDK-based quality evaluation is enabled in configuration.
	 *
	 * @return true if SDK quality check is enabled, false otherwise
	 */
	private boolean isSdkQualityCheckEnabled() {
		return RegistrationConstants.ENABLE.equalsIgnoreCase((String) ApplicationContext.map()
				.getOrDefault(RegistrationConstants.QUALITY_CHECK_WITH_SDK, RegistrationConstants.DISABLE));
	}

	/**
	 * Gets the configured SDK quality evaluation timeout in milliseconds.
	 *
	 * @return timeout in milliseconds
	 */
	private int getSdkTimeoutMs() {
		Integer timeout = ApplicationContext.getIntValueFromApplicationMap(
				RegistrationConstants.SDK_QUALITY_EVALUATION_TIMEOUT);
		return (timeout != null && timeout > 0) ? timeout : DEFAULT_SDK_TIMEOUT_MS;
	}

	@Override
	public List<BiometricsDto> captureModality(MDMRequestDto mdmRequestDto) throws RegBaseCheckedException {
		LOGGER.info("Entering into captureModality method.. {}", System.currentTimeMillis());
		List<BiometricsDto> list = new ArrayList<BiometricsDto>();
		boolean sdkEnabled = isSdkQualityCheckEnabled();

		try {
			MdmBioDevice bioDevice = deviceSpecificationFactory.getDeviceInfoByModality(mdmRequestDto.getModality());
			MosipDeviceSpecificationProvider deviceSpecificationProvider = deviceSpecificationFactory
					.getMdsProvider(bioDevice.getSpecVersion());
			List<BiometricsDto> biometricsDtos = deviceSpecificationProvider.rCapture(bioDevice, mdmRequestDto);

			for (BiometricsDto biometricsDto : biometricsDtos) {
				if (biometricsDto == null) {
					continue;
				}

				// Validate SBI quality score is in valid range
				if (!ValueRange.of(0, RegistrationConstants.MAX_BIO_QUALITY_SCORE).isValidValue((long) biometricsDto.getQualityScore()))
					throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_SCORE_RANGE_ERROR.getErrorCode(),
							RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_SCORE_RANGE_ERROR.getErrorMessage());

				// Validate biometric data is not corrupt
				if (biometricsDto.getAttributeISO() == null || biometricsDto.getAttributeISO().length == 0) {
					LOGGER.error("Captured biometric data is null or empty for attribute: {}", biometricsDto.getBioAttribute());
					logAuditEvent("QUALITY_EVAL", biometricsDto.getBioAttribute(), "CORRUPT_DATA",
							"Biometric data is null or empty", 0, "NONE");
					throw new RegBaseCheckedException(
							RegistrationExceptionConstants.REG_BIOMETRIC_CORRUPT_DATA.getErrorCode(),
							RegistrationExceptionConstants.REG_BIOMETRIC_CORRUPT_DATA.getErrorMessage());
				}

				if (sdkEnabled) {
					// SDK-based quality evaluation
					evaluateQualityWithSdk(biometricsDto);
				} else {
					// SBI fallback: SDK is disabled, use SBI quality score
					LOGGER.info("SDK-based quality evaluation is disabled. Using SBI quality score for attribute: {}",
							biometricsDto.getBioAttribute());
					logAuditEvent("QUALITY_EVAL", biometricsDto.getBioAttribute(), "SBI_SCORE_USED",
							"SDK disabled, using SBI quality score", biometricsDto.getQualityScore(), "SBI");
				}
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

	/**
	 * Evaluates biometric quality using SDK. When SDK is configured and enabled,
	 * this method is the primary quality evaluation path.
	 * 
	 * <p>Behavior:
	 * <ul>
	 *   <li>SDK score is obtained and stored in both sdkScore and qualityScore fields</li>
	 *   <li>If SDK returns invalid/null score, throws REG_BIOMETRIC_SDK_INVALID_SCORE</li>
	 *   <li>If SDK times out, throws REG_BIOMETRIC_SDK_TIMEOUT</li>
	 *   <li>If SDK throws exception, throws REG_BIOMETRIC_QUALITY_CHECK_ERROR</li>
	 *   <li>No silent fallback to SBI when SDK fails - operator must re-capture</li>
	 * </ul>
	 *
	 * @param biometricsDto the captured biometric data to evaluate
	 * @throws RegBaseCheckedException if SDK evaluation fails for any reason
	 */
	private void evaluateQualityWithSdk(BiometricsDto biometricsDto) throws RegBaseCheckedException {
		String bioAttribute = biometricsDto.getBioAttribute();
		LOGGER.info("Starting SDK-based quality evaluation for attribute: {}", bioAttribute);

		try {
			double sdkScore = getSDKScoreWithTimeout(biometricsDto);

			// Validate SDK score is in valid range
			if (!ValueRange.of(0, RegistrationConstants.MAX_BIO_QUALITY_SCORE).isValidValue((long) sdkScore)) {
				LOGGER.error("SDK returned invalid quality score {} for attribute: {}", sdkScore, bioAttribute);
				logAuditEvent("QUALITY_EVAL", bioAttribute, "SDK_INVALID_SCORE",
						"SDK returned score out of valid range: " + sdkScore, sdkScore, "SDK");
				throw new RegBaseCheckedException(
						RegistrationExceptionConstants.REG_BIOMETRIC_SDK_INVALID_SCORE.getErrorCode(),
						RegistrationExceptionConstants.REG_BIOMETRIC_SDK_INVALID_SCORE.getErrorMessage());
			}

			// Store SDK score
			biometricsDto.setSdkScore(sdkScore);
			// Replace qualityScore with SDK score so threshold validation uses SDK score
			biometricsDto.setQualityScore(sdkScore);

			LOGGER.info("SDK quality evaluation successful for attribute: {}, sdkScore: {}", bioAttribute, sdkScore);
			logAuditEvent("QUALITY_EVAL", bioAttribute, "SDK_SCORE_USED",
					"SDK quality evaluation successful", sdkScore, "SDK");

		} catch (RegBaseCheckedException e) {
			// Re-throw already-wrapped exceptions (timeout, invalid score)
			throw e;
		} catch (BiometricException e) {
			LOGGER.error("SDK biometric exception during quality evaluation for attribute: {}", bioAttribute, e);
			logAuditEvent("QUALITY_EVAL", bioAttribute, "SDK_EXCEPTION",
					"BiometricException: " + e.getMessage(), 0, "SDK");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_CHECK_ERROR.getErrorCode(),
					RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_CHECK_ERROR.getErrorMessage());
		} catch (TimeoutException e) {
			LOGGER.error("SDK quality evaluation timed out for attribute: {}", bioAttribute, e);
			logAuditEvent("QUALITY_EVAL", bioAttribute, "SDK_TIMEOUT",
					"SDK evaluation timed out after " + getSdkTimeoutMs() + "ms", 0, "SDK");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_BIOMETRIC_SDK_TIMEOUT.getErrorCode(),
					RegistrationExceptionConstants.REG_BIOMETRIC_SDK_TIMEOUT.getErrorMessage());
		} catch (Exception e) {
			LOGGER.error("Unexpected error during SDK quality evaluation for attribute: {}", bioAttribute, e);
			logAuditEvent("QUALITY_EVAL", bioAttribute, "SDK_ERROR",
					"Unexpected error: " + e.getMessage(), 0, "SDK");
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_CHECK_ERROR.getErrorCode(),
					RegistrationExceptionConstants.REG_BIOMETRIC_QUALITY_CHECK_ERROR.getErrorMessage());
		}
	}

	/**
	 * Calls getSDKScore with a configurable timeout to prevent blocking.
	 *
	 * @param biometricsDto the biometric data to evaluate
	 * @return the SDK quality score
	 * @throws BiometricException if SDK evaluation fails
	 * @throws TimeoutException if SDK evaluation times out
	 * @throws Exception for any other errors
	 */
	private double getSDKScoreWithTimeout(BiometricsDto biometricsDto)
			throws BiometricException, TimeoutException, Exception {
		int timeoutMs = getSdkTimeoutMs();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Future<Double> future = executor.submit(() -> getSDKScore(biometricsDto));
			return future.get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof BiometricException) {
				throw (BiometricException) cause;
			}
			throw new Exception("SDK quality evaluation failed", cause);
		} finally {
			executor.shutdownNow();
		}
	}

	/**
	 * Logs audit event for biometric quality evaluation.
	 * Failures in audit logging should not block the biometric capture process.
	 *
	 * @param eventType type of event (e.g., QUALITY_EVAL)
	 * @param bioAttribute the biometric attribute being evaluated
	 * @param status status of the evaluation (e.g., SDK_SCORE_USED, SBI_FALLBACK)
	 * @param message descriptive message
	 * @param score the quality score
	 * @param source the score source (SDK or SBI)
	 */
	private void logAuditEvent(String eventType, String bioAttribute, String status,
							   String message, double score, String source) {
		try {
			LOGGER.info("AUDIT [{}] attribute={}, status={}, score={}, source={}, message={}",
					eventType, bioAttribute, status, score, source, message);
		} catch (Exception e) {
			LOGGER.warn("Failed to log audit event for biometric quality evaluation: {}", e.getMessage());
		}
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
					quality = quality + biometricsDto.getQualityScore();
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
