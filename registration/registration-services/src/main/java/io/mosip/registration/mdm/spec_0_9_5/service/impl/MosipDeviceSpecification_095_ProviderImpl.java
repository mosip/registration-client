package io.mosip.registration.mdm.spec_0_9_5.service.impl;

import static io.mosip.registration.constants.LoggerConstants.MOSIP_BIO_DEVICE_INTEGERATOR;
import static io.mosip.registration.constants.LoggerConstants.MOSIP_BIO_DEVICE_MANAGER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.DeviceException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.constants.MosipBioDeviceConstants;
import io.mosip.registration.mdm.dto.Biometric;
import io.mosip.registration.mdm.dto.DeviceInfo;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.dto.MdmDeviceInfo;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationHelper;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.DeviceDiscoveryRequest;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.RCaptureRequestBioDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.RCaptureRequestDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.request.StreamRequestDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.DeviceDiscoveryMDSResponse;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.DigitalId;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.MdmDeviceInfoResponse;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseBiometricsDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseDTO;
import io.mosip.registration.mdm.spec_0_9_5.dto.response.RCaptureResponseDataDTO;

@Service
public class MosipDeviceSpecification_095_ProviderImpl implements MosipDeviceSpecificationProvider {

	private static final Logger LOGGER = AppConfig.getLogger(MosipDeviceSpecification_095_ProviderImpl.class);

	private static final String SPEC_VERSION = "0.9.5";

	private static final String loggerClassName = "MosipDeviceSpecification_095_ProviderImpl";

	@Autowired
	private MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper;

	// TODO - remove, and use helper. as this leads to circular dependency
	@Autowired
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	@Autowired
	private ObjectMapper objectMapper;

	@Value("${mosip.registration.mdm.trust.domain.rcapture:DEVICE}")
	private String rCaptureTrustDomain;

	@Value("${mosip.registration.mdm.trust.domain.digitalId:DEVICE}")
	private String digitalIdTrustDomain;

	@Override
	public String getSpecVersion() {
		return SPEC_VERSION;
	}

	@Override
	public List<MdmBioDevice> getMdmDevices(String deviceInfoResponse, int port) {
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"received device info response on port : " + port);

		List<MdmBioDevice> mdmBioDevices = new LinkedList<>();

		List<MdmDeviceInfoResponse> deviceInfoResponses;
		try {

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "parsing device info response to 095 dto");
			deviceInfoResponses = (mosipDeviceSpecificationHelper.getMapper().readValue(deviceInfoResponse,
					new TypeReference<List<MdmDeviceInfoResponse>>() {
					}));

			for (MdmDeviceInfoResponse mdmDeviceInfoResponse : deviceInfoResponses) {
				if (mdmDeviceInfoResponse.getDeviceInfo() != null && !mdmDeviceInfoResponse.getDeviceInfo().isEmpty()) {
					DeviceInfo deviceInfo = mosipDeviceSpecificationHelper
							.getDeviceInfoDecoded(mdmDeviceInfoResponse.getDeviceInfo(), this.getClass());
					MdmBioDevice bioDevice = getBioDevice((MdmDeviceInfo)deviceInfo);
					if (bioDevice != null) {
						bioDevice.setPort(port);
						mdmBioDevices.add(bioDevice);
					}
				}
			}
		} catch (Exception exception) {
			LOGGER.error(APPLICATION_NAME, APPLICATION_ID, "Exception while parsing deviceinfo response(095 spec)",
					ExceptionUtils.getStackTrace(exception));
		}
		return mdmBioDevices;
	}

	@Counted(extraTags = {"version", "0.9.5"})
	@Timed(extraTags = {"version", "0.9.5"})
	@Override
	public InputStream stream(MdmBioDevice bioDevice, String modality) throws RegBaseCheckedException {
		try {

			if (!isDeviceAvailable(bioDevice)) {
				throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
						RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());
			}

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Started Strema for modality : " + modality);

			String timeout = (String) ApplicationContext.getInstance().getApplicationMap()
					.getOrDefault(RegistrationConstants.CAPTURE_TIME_OUT, "60000");
			StreamRequestDTO streamRequestDTO = new StreamRequestDTO(bioDevice.getDeviceId(), getDeviceSubId(modality),
					timeout);

			String request = objectMapper.writeValueAsString(streamRequestDTO);
			CloseableHttpResponse response = mosipDeviceSpecificationHelper.getHttpClientResponse(
					bioDevice.getCallbackId() + MosipBioDeviceConstants.STREAM_ENDPOINT, "STREAM", request);

			InputStream urlStream = null;
			if (response.getEntity() != null) {
				urlStream = response.getEntity().getContent();
			}

			try {
				byte[] byteArray = mosipDeviceSpecificationHelper.getJPEGByteArray(urlStream,
						System.currentTimeMillis() + Long.parseLong(timeout));

				if (byteArray != null) {
					LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
							"Stream Request Completed" + System.currentTimeMillis());
					return urlStream;
				}

			} catch (RegBaseCheckedException regBaseCheckedException) {
				LOGGER.error("Stream Request failed", regBaseCheckedException);
			}

			if(urlStream != null)
				urlStream.close();

			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_STREAM_TIMEOUT.getErrorCode(),
					RegistrationExceptionConstants.MDS_STREAM_TIMEOUT.getErrorMessage());

		} catch (Exception exception) {
			LOGGER.error("Stream Request failed", exception);
		}
		throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_STREAM_ERROR.getErrorCode(),
				RegistrationExceptionConstants.MDS_STREAM_ERROR.getErrorMessage());
	}

	@Counted(extraTags = {"version", "0.9.5"})
	@Timed(extraTags = {"version", "0.9.5"})
	@Override
	public List<BiometricsDto> rCapture(MdmBioDevice bioDevice, MDMRequestDto mdmRequestDto)
			throws RegBaseCheckedException {

		try {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Entering into rCapture method for moadlity : " + mdmRequestDto.getModality() + "  ....."
							+ System.currentTimeMillis());

			if (mdmRequestDto.getExceptions() != null) {
				mdmRequestDto.setExceptions(getExceptions(mdmRequestDto.getExceptions()));
			}

			int count = getCount(mdmRequestDto.getModality(), getDefaultCount(mdmRequestDto.getModality()),
					mdmRequestDto.getExceptions() != null ? mdmRequestDto.getExceptions().length : 0);
			mdmRequestDto.setCount(count);

			RCaptureRequestDTO rCaptureRequestDTO = getRCaptureRequest(bioDevice, mdmRequestDto);
			if (rCaptureRequestDTO == null) {
				throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorCode(),
						" failed to construct Rcapture request");
			}
			String requestBody = objectMapper.writeValueAsString(rCaptureRequestDTO);
			LOGGER.debug("Request for RCapture....{}", requestBody);

			String val = mosipDeviceSpecificationHelper.getHttpClientResponseEntity(
					bioDevice != null ? bioDevice.getCallbackId() + MosipBioDeviceConstants.CAPTURE_ENDPOINT : MosipBioDeviceConstants.CAPTURE_ENDPOINT,
					"RCAPTURE", requestBody);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Request completed.... " + System.currentTimeMillis());
			
			RCaptureResponseDTO captureResponse = objectMapper.readValue(val.getBytes(StandardCharsets.UTF_8),
					RCaptureResponseDTO.class);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Response Decode and leaving the method.... " + System.currentTimeMillis());

			List<RCaptureResponseBiometricsDTO> captureResponseBiometricsDTOs = captureResponse.getBiometrics();

			List<BiometricsDto> biometricDTOs = new LinkedList<>();

			for (RCaptureResponseBiometricsDTO rCaptureResponseBiometricsDTO : captureResponseBiometricsDTOs) {

				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Getting data payload of biometric" + System.currentTimeMillis());
				if (rCaptureResponseBiometricsDTO.getData() == null
						|| rCaptureResponseBiometricsDTO.getData().isEmpty()) {
					throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorCode(),
							RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorMessage()
									+ " : Data is empty in RCapture " + " error Code  : "
									+ rCaptureResponseBiometricsDTO.getError().getErrorCode() + " error message : "
									+ rCaptureResponseBiometricsDTO.getError().getErrorInfo());
				}
				if (rCaptureResponseBiometricsDTO.getData() != null
						&& !rCaptureResponseBiometricsDTO.getData().isEmpty()) {
					mosipDeviceSpecificationHelper.validateJWTResponse(rCaptureResponseBiometricsDTO.getData(), rCaptureTrustDomain);
					String payLoad = mosipDeviceSpecificationHelper.getPayLoad(rCaptureResponseBiometricsDTO.getData());
					String signature = mosipDeviceSpecificationHelper.getSignature(rCaptureResponseBiometricsDTO.getData());

					String decodedPayLoad = new String(CryptoUtil.decodeURLSafeBase64(payLoad));
					RCaptureResponseDataDTO dataDTO = objectMapper.readValue(decodedPayLoad, RCaptureResponseDataDTO.class);

					LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
							"Parsed decoded payload" + System.currentTimeMillis());
					
					mosipDeviceSpecificationHelper.validateResponseTimestamp(dataDTO.getTimestamp());

					mosipDeviceSpecificationHelper.validateQualityScore(dataDTO.getQualityScore());

					if (dataDTO.getTransactionId() == null || (rCaptureRequestDTO != null
							&& !dataDTO.getTransactionId().equalsIgnoreCase(rCaptureRequestDTO.getTransactionId()))) {
						LOGGER.error(" RCapture TransactionId Mismatch : the request transaction id is:{} response Transaction id {}",
								rCaptureRequestDTO.getTransactionId(),dataDTO.getTransactionId());
						throw new RegBaseCheckedException(
								RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorCode(),
								" RCapture TransactionId Mismatch " );
					}
					
					if (rCaptureResponseBiometricsDTO.getSpecVersion() == null || !rCaptureResponseBiometricsDTO
							.getSpecVersion().equalsIgnoreCase(rCaptureRequestDTO.getSpecVersion())) {
						throw new RegBaseCheckedException(
								RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorCode(),
								RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorMessage()
										+ " : RCapture spec version Mismatch : " + " request spec version is : "
										+ rCaptureRequestDTO.getSpecVersion() + " and response spec version is :"
										+ rCaptureResponseBiometricsDTO.getSpecVersion());
					}


					String uiAttribute = Biometric.getUiSchemaAttributeName(dataDTO.getBioSubType(), SPEC_VERSION);

					if (uiAttribute == null || uiAttribute.isEmpty() || dataDTO.getBioSubType() == null
							|| dataDTO.getBioSubType().isEmpty()
							|| dataDTO.getBioSubType().equalsIgnoreCase(RegistrationConstants.JOB_UNKNOWN)) {

						uiAttribute = RegistrationConstants.JOB_UNKNOWN;
					}
					if (mdmRequestDto.getModality().equalsIgnoreCase(RegistrationConstants.FACE_FULLFACE)) {
						uiAttribute = "face";
					}

					BiometricsDto biometricDTO = new BiometricsDto(uiAttribute, dataDTO.getDecodedBioValue(),
							Double.parseDouble(dataDTO.getQualityScore()));
					biometricDTO.setPayLoad(decodedPayLoad);
					biometricDTO.setSignature(signature);
					biometricDTO.setSpecVersion(rCaptureResponseBiometricsDTO.getSpecVersion());
					biometricDTO.setCaptured(true);
					biometricDTO.setModalityName(mdmRequestDto.getModality());
					biometricDTOs.add(biometricDTO);
				}
			}
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"rCapture Completed" + System.currentTimeMillis());

			return biometricDTOs;
		} catch (Exception exception) {
			LOGGER.error("Failed to capture biometrics", exception);
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorCode(),
					RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorMessage(), exception);
		}
	}

	private String[] getExceptions(String[] exceptions) {

		if (exceptions != null) {
			for (int index = 0; index < exceptions.length; index++) {
				exceptions[index] = io.mosip.registration.mdm.dto.Biometric
						.getmdmRequestAttributeName(exceptions[index], SPEC_VERSION);
			}

		}

		return exceptions;

	}

	private RCaptureRequestDTO getRCaptureRequest(MdmBioDevice bioDevice, MDMRequestDto mdmRequestDto)
			throws JsonParseException, JsonMappingException, IOException {

		RCaptureRequestDTO rCaptureRequestDTO = null;

		if (bioDevice != null) {
			List<RCaptureRequestBioDTO> captureRequestBioDTOs = new LinkedList<>();
			captureRequestBioDTOs.add(
					new RCaptureRequestBioDTO(bioDevice.getDeviceType(), Integer.toString(mdmRequestDto.getCount()),
							null, mdmRequestDto.getExceptions(), String.valueOf(mdmRequestDto.getRequestedScore()),
							bioDevice.getDeviceId(), getDeviceSubId(mdmRequestDto.getModality()), null));

			rCaptureRequestDTO = new RCaptureRequestDTO(mdmRequestDto.getEnvironment(), "Registration", "0.9.5",
					String.valueOf(mdmRequestDto.getTimeout()),
					LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
					mosipDeviceSpecificationHelper.generateMDMTransactionId(), captureRequestBioDTOs,
					null);
		}

		return rCaptureRequestDTO;
	}

	private String getDeviceSubId(String modality) {
		modality = modality.toLowerCase();

		return modality.contains("left") ? "1"
				: modality.contains("right") ? "2"
						: (modality.contains("double") || modality.contains("thumbs") || modality.contains("two")) ? "3"
								: "0";
	}

	private MdmBioDevice getBioDevice(MdmDeviceInfo deviceInfo)
			throws IOException, RegBaseCheckedException, DeviceException {

		MdmBioDevice bioDevice = null;

		if (deviceInfo != null) {

			DigitalId digitalId = getDigitalId(deviceInfo.getDigitalId());

			bioDevice = new MdmBioDevice();
			bioDevice.setDeviceId(deviceInfo.getDeviceId());
			bioDevice.setFirmWare(deviceInfo.getFirmware());
			bioDevice.setCertification(deviceInfo.getCertification());
			bioDevice.setSerialVersion(deviceInfo.getServiceVersion());
			bioDevice.setSpecVersion(deviceSpecificationFactory.getLatestSpecVersion(deviceInfo.getSpecVersion()));
			bioDevice.setPurpose(deviceInfo.getPurpose());
			bioDevice.setDeviceCode(deviceInfo.getDeviceCode());

			bioDevice.setDeviceSubType(digitalId.getDeviceSubType());
			bioDevice.setDeviceType(digitalId.getType());
			bioDevice.setTimestamp(digitalId.getDateTime());
			bioDevice.setDeviceProviderName(digitalId.getDeviceProvider());
			bioDevice.setDeviceProviderId(digitalId.getDeviceProviderId());
			bioDevice.setDeviceModel(digitalId.getModel());
			bioDevice.setDeviceMake(digitalId.getMake());
			bioDevice.setSerialNumber(digitalId.getSerialNo());
			bioDevice.setCallbackId(deviceInfo.getCallbackId());
		}

		LOGGER.info(MOSIP_BIO_DEVICE_MANAGER, APPLICATION_NAME, APPLICATION_ID, "Adding Device to Registry : ");
		return bioDevice;
	}

	private DigitalId getDigitalId(String digitalId) throws IOException, RegBaseCheckedException, DeviceException {
		mosipDeviceSpecificationHelper.validateJWTResponse(digitalId, digitalIdTrustDomain);
		return mosipDeviceSpecificationHelper.getMapper().readValue(
				new String(CryptoUtil.decodeURLSafeBase64(mosipDeviceSpecificationHelper.getPayLoad(digitalId))),
				DigitalId.class);

	}

	private int getDefaultCount(String modality) {
		int defaultCount = 1;
		if (modality != null) {
			switch (modality) {
			case RegistrationConstants.FACE_FULLFACE:
				defaultCount = 1;
				break;
			case RegistrationConstants.IRIS_DOUBLE:
				defaultCount = 2;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
				defaultCount = 4;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
				defaultCount = 4;
				break;
			case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
				defaultCount = 2;
				break;
			}
		}
		return defaultCount;
	}

	private int getCount(String modality, int defaultCount, int exceptionsCount) {
		return RegistrationConstants.FACE_FULLFACE.equalsIgnoreCase(modality) ? 1 : (defaultCount - exceptionsCount);
	}

	@Counted(recordFailuresOnly = true, extraTags = {"version", "0.9.5"})
	@Timed(extraTags = {"version", "0.9.5"})
	@Override
	public boolean isDeviceAvailable(MdmBioDevice mdmBioDevice) {

		boolean isDeviceAvailable = false;

		try {
			DeviceDiscoveryRequest deviceDiscoveryRequest = new DeviceDiscoveryRequest();
			deviceDiscoveryRequest.setType(mdmBioDevice.getDeviceType());

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Entering into Device availbale check....." + System.currentTimeMillis());

			String requestBody = objectMapper.writeValueAsString(deviceDiscoveryRequest);

			LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Request for device discovery...." + requestBody);

			String response = mosipDeviceSpecificationHelper.getHttpClientResponseEntity(
					mosipDeviceSpecificationHelper.buildUrl(mdmBioDevice.getPort(), "device"),
					"MOSIPDISC",
					requestBody);

			LOGGER.info("Request completed {}. parsing device discovery response to 095 dto", System.currentTimeMillis());
			List<DeviceDiscoveryMDSResponse> deviceList = (mosipDeviceSpecificationHelper.getMapper().readValue(response,
					new TypeReference<List<DeviceDiscoveryMDSResponse>>() {}));

			isDeviceAvailable = deviceList.stream().anyMatch(device ->
					Arrays.asList(device.getSpecVersion()).contains(SPEC_VERSION)
							&& RegistrationConstants.DEVICE_STATUS_READY.equalsIgnoreCase(device.getDeviceStatus())
							&& device.getCertification().equals(mdmBioDevice.getCertification())
							&& device.getDeviceCode().equals(mdmBioDevice.getDeviceCode())
							&& device.getDeviceId().equals(mdmBioDevice.getDeviceId()));

		} catch (Throwable exception) {
			LOGGER.error(MOSIP_BIO_DEVICE_INTEGERATOR, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
		}
		return isDeviceAvailable;
	}
}
