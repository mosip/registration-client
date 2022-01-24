package io.mosip.registration.mdm.sbi.spec_1_0.service.impl;

import static io.mosip.registration.constants.LoggerConstants.MOSIP_BIO_DEVICE_INTEGERATOR;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.mosip.kernel.core.util.CryptoUtil;
import org.apache.http.Consts;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
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
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.request.SbiDeviceDiscoveryRequest;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.request.SbiRCaptureRequestBioDTO;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.request.SbiRCaptureRequestDTO;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.request.StreamSbiRequestDTO;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.MdmDeviceInfoResponse;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.MdmSbiDeviceInfo;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.MdmSbiDeviceInfoWrapper;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.SbiDeviceDiscoveryMDSResponse;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.SbiDigitalId;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.SbiRCaptureResponseBiometricsDTO;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.SbiRCaptureResponseDTO;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.SbiRCaptureResponseDataDTO;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationHelper;

@Service
public class MosipDeviceSpecification_SBI_1_0_ProviderImpl implements MosipDeviceSpecificationProvider {

	private static final Logger LOGGER = AppConfig.getLogger(MosipDeviceSpecification_SBI_1_0_ProviderImpl.class);

	private static final String SPEC_VERSION = "1.0";

	private static final String loggerClassName = "MosipDeviceSpecification_SBI_1_0_ProviderImpl";

	@Autowired
	private MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper;

	// TODO - remove, and use helper. as this leads to circular dependency
	@Autowired
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	@Value("${mosip.registration.mdm.trust.domain.rcapture:DEVICE}")
	private String rCaptureTrustDomain;

	@Value("${mosip.registration.mdm.trust.domain.digitalId:FTM}")
	private String digitalIdTrustDomain;

	@Value("${mosip.registration.mdm.trust.domain.deviceinfo:DEVICE}")
	private String deviceInfoTrustDomain;

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

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "parsing device info response to SBI 1_0 dto");
			deviceInfoResponses = (mosipDeviceSpecificationHelper.getMapper().readValue(deviceInfoResponse,
					new TypeReference<List<MdmDeviceInfoResponse>>() {
					}));
			for (MdmDeviceInfoResponse mdmDeviceInfoResponse : deviceInfoResponses) {
				if (mdmDeviceInfoResponse.getDeviceInfo() != null && !mdmDeviceInfoResponse.getDeviceInfo().isEmpty()) {
					DeviceInfo mdmDeviceInfo = mosipDeviceSpecificationHelper
							.getDeviceInfoDecoded(mdmDeviceInfoResponse.getDeviceInfo(), this.getClass());
					
					MdmBioDevice bioDevice = getBioDevice((MdmSbiDeviceInfoWrapper)mdmDeviceInfo);
					if (bioDevice != null) {
						bioDevice.setPort(port);
						mdmBioDevices.add(bioDevice);
					}
				}
			}
		} catch (Exception exception) {
			LOGGER.error(APPLICATION_NAME, APPLICATION_ID, "Exception while parsing deviceinfo response(SBI 1_0 spec)",
					ExceptionUtils.getStackTrace(exception));
		}
		return mdmBioDevices;
	}

	@Counted(extraTags = {"version", "1.0"})
	@Timed(extraTags = {"version", "1.0"})
	@Override
	public InputStream stream(MdmBioDevice bioDevice, String modality) throws RegBaseCheckedException {

		try {

			if (!isDeviceAvailable(bioDevice)) {
				throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
						RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());
			}

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Started Strema for modality : " + modality);
			String url = mosipDeviceSpecificationHelper.buildUrl(bioDevice.getPort(),
					MosipBioDeviceConstants.STREAM_ENDPOINT);

			StreamSbiRequestDTO streamSbiRequestDTO = new StreamSbiRequestDTO();

			String timeout = (String) ApplicationContext.getInstance().getApplicationMap()
			.getOrDefault(RegistrationConstants.CAPTURE_TIME_OUT, "60000");
			streamSbiRequestDTO.setDeviceSubId(getDeviceSubId(modality));
			streamSbiRequestDTO.setSerialNo(bioDevice.getSerialNumber());
			streamSbiRequestDTO.setTimeout(timeout);

			String request = new ObjectMapper().writeValueAsString(streamSbiRequestDTO);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Request for Stream...." + request);

			CloseableHttpClient client = HttpClients.createDefault();
			StringEntity requestEntity = new StringEntity(request, ContentType.create("Content-Type", Consts.UTF_8));
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Building Stream url...." + System.currentTimeMillis());
			HttpUriRequest httpUriRequest = RequestBuilder.create("STREAM").setUri(url).setEntity(requestEntity)
					.build();

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Requesting Stream url...." + System.currentTimeMillis());
			CloseableHttpResponse response = client.execute(httpUriRequest);
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Request completed.... " + System.currentTimeMillis());

			InputStream urlStream = null;
			if (response.getEntity() != null) {
				urlStream = response.getEntity().getContent();
			}
			
			try {
				byte[] byteArray = mosipDeviceSpecificationHelper.getJPEGByteArray(urlStream,
						System.currentTimeMillis() + Long.parseLong(streamSbiRequestDTO.getTimeout()));

				if (byteArray != null) {
					LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
							"Stream Request Completed" + System.currentTimeMillis());
					return urlStream;
				}

			} catch (RegBaseCheckedException regBaseCheckedException) {
				LOGGER.error("Stream Request failed", regBaseCheckedException);
			}
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Stream Request Completed" + System.currentTimeMillis());
			return urlStream;
		} catch (Exception exception) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_STREAM_ERROR.getErrorCode(),
					RegistrationExceptionConstants.MDS_STREAM_ERROR.getErrorMessage(), exception);

		}

	}

	@Counted(extraTags = {"version", "1.0"})
	@Timed(extraTags = {"version", "1.0"})
	@Override
	public List<BiometricsDto> rCapture(MdmBioDevice bioDevice, MDMRequestDto mdmRequestDto)
			throws RegBaseCheckedException {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Entering into rCapture method for moadlity : "
				+ mdmRequestDto.getModality() + "  ....." + System.currentTimeMillis());

		try {
			if (mdmRequestDto.getExceptions() != null) {
				mdmRequestDto.setExceptions(getExceptions(mdmRequestDto.getExceptions()));
			}

			String url = mosipDeviceSpecificationHelper.buildUrl(bioDevice.getPort(),
					MosipBioDeviceConstants.CAPTURE_ENDPOINT);

			SbiRCaptureRequestDTO rCaptureRequestDTO = getRCaptureRequest(bioDevice, mdmRequestDto);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Entering into Capture method....." + System.currentTimeMillis());
			ObjectMapper mapper = new ObjectMapper();
			String requestBody = mapper.writeValueAsString(rCaptureRequestDTO);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Request for RCapture...." + requestBody);

			CloseableHttpClient client = HttpClients.createDefault();
			StringEntity requestEntity = new StringEntity(requestBody,
					ContentType.create("Content-Type", Consts.UTF_8));
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Bulding capture url...." + System.currentTimeMillis());
			HttpUriRequest request = RequestBuilder.create("RCAPTURE").setUri(url).setEntity(requestEntity).build();
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Requesting capture url...." + System.currentTimeMillis());
			CloseableHttpResponse response = client.execute(request);
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Request completed.... " + System.currentTimeMillis());

			String val = EntityUtils.toString(response.getEntity());

			SbiRCaptureResponseDTO captureResponse = mapper.readValue(val.getBytes(StandardCharsets.UTF_8),
					SbiRCaptureResponseDTO.class);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Response Decode and leaving the method.... " + System.currentTimeMillis());

			List<SbiRCaptureResponseBiometricsDTO> sbiCaptureResponseBiometricsDTOs = captureResponse.getBiometrics();

			List<BiometricsDto> biometricDTOs = new LinkedList<>();

			for (SbiRCaptureResponseBiometricsDTO sbiRCaptureResponseBiometricsDTO : sbiCaptureResponseBiometricsDTOs) {

				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Getting data payload of biometric" + System.currentTimeMillis());

				mosipDeviceSpecificationHelper.validateJWTResponse(sbiRCaptureResponseBiometricsDTO.getData(), rCaptureTrustDomain);
				String payLoad = mosipDeviceSpecificationHelper.getPayLoad(sbiRCaptureResponseBiometricsDTO.getData());
				String signature = mosipDeviceSpecificationHelper.getSignature(sbiRCaptureResponseBiometricsDTO.getData());

				String decodedPayLoad = new String(CryptoUtil.decodeURLSafeBase64(payLoad));
				SbiRCaptureResponseDataDTO dataDTO = mapper.readValue(decodedPayLoad, SbiRCaptureResponseDataDTO.class);

				LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
						"Parsed decoded payload" + System.currentTimeMillis());

				String uiAttribute = Biometric.getUiSchemaAttributeName(dataDTO.getBioSubType(), SPEC_VERSION);
				BiometricsDto biometricDTO = new BiometricsDto(uiAttribute, dataDTO.getDecodedBioValue(),
						Double.parseDouble(dataDTO.getQualityScore()));
				biometricDTO.setPayLoad(decodedPayLoad);
				biometricDTO.setSignature(signature);
				biometricDTO.setSpecVersion(sbiRCaptureResponseBiometricsDTO.getSpecVersion());
				biometricDTO.setCaptured(true);
				biometricDTO.setModalityName(mdmRequestDto.getModality());
				biometricDTOs.add(biometricDTO);
			}

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"rCapture Completed" + System.currentTimeMillis());
			return biometricDTOs;
		} catch (Exception exception) {
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

	private SbiRCaptureRequestDTO getRCaptureRequest(MdmBioDevice bioDevice, MDMRequestDto mdmRequestDto)
			throws JsonParseException, JsonMappingException, IOException {

		SbiRCaptureRequestDTO sbiRCaptureRequestDTO = null;
		if (bioDevice != null) {
			List<SbiRCaptureRequestBioDTO> captureRequestBioDTOs = new LinkedList<>();
			captureRequestBioDTOs.add(new SbiRCaptureRequestBioDTO(getDeviceType(bioDevice.getDeviceType()), "1",
					mdmRequestDto.getExceptions(), String.valueOf(mdmRequestDto.getRequestedScore()), bioDevice.getSerialNumber(),
					bioDevice.getDeviceId(),bioDevice.getDeviceSubType(), getDeviceSubId(mdmRequestDto.getModality()), null));

			sbiRCaptureRequestDTO = new SbiRCaptureRequestDTO(mdmRequestDto.getEnvironment(), bioDevice.getPurpose(), bioDevice.getSpecVersion(),
					String.valueOf(mdmRequestDto.getTimeout()),
					LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
					mosipDeviceSpecificationHelper.generateMDMTransactionId(), captureRequestBioDTOs, null);
		}

		return sbiRCaptureRequestDTO;
	}

	private String getDeviceType(String deviceType) {
		//deviceType = deviceType.toLowerCase();

		return deviceType/* .contains("Finger") ? "FIR" : deviceType.contains("iris") ? "IIR" : "face" */;
	}

	private String getDeviceSubId(String modality) {
		modality = modality.toLowerCase();

		return modality.contains("left") ? "1"
				: modality.contains("right") ? "2"
						: (modality.contains("double") || modality.contains("thumbs") || modality.contains("two")) ? "3"
								: modality.contains("face") ? "0" : "0";
	}

	private MdmBioDevice getBioDevice(MdmSbiDeviceInfoWrapper deviceSbiInfo)
			throws IOException, RegBaseCheckedException, DeviceException {

		MdmBioDevice bioDevice = null;

		if (deviceSbiInfo.deviceInfo != null) {
			SbiDigitalId sbiDigitalId = getSbiDigitalId(deviceSbiInfo.deviceInfo.getDigitalId());
			bioDevice = new MdmBioDevice();
			bioDevice.setFirmWare(deviceSbiInfo.deviceInfo.getFirmware());
			bioDevice.setCertification(deviceSbiInfo.deviceInfo.getCertification());
			bioDevice.setSerialVersion(deviceSbiInfo.deviceInfo.getServiceVersion());
			bioDevice.setSpecVersion(deviceSpecificationFactory.getLatestSpecVersion(deviceSbiInfo.deviceInfo.getSpecVersion()));
			bioDevice.setPurpose(deviceSbiInfo.deviceInfo.getPurpose());

			bioDevice.setDeviceSubType(sbiDigitalId.getDeviceSubType());
			bioDevice.setDeviceType(sbiDigitalId.getType());
			bioDevice.setTimestamp(sbiDigitalId.getDateTime());
			bioDevice.setDeviceProviderName(sbiDigitalId.getDeviceProvider());
			bioDevice.setDeviceProviderId(sbiDigitalId.getDeviceProviderId());
			bioDevice.setDeviceModel(sbiDigitalId.getModel());
			bioDevice.setDeviceMake(sbiDigitalId.getMake());

			bioDevice.setSerialNumber(sbiDigitalId.getSerialNo());
			bioDevice.setCallbackId(deviceSbiInfo.deviceInfo.getCallbackId());
		}
		return bioDevice;
	}

	private SbiDigitalId getSbiDigitalId(String digitalId) throws IOException, RegBaseCheckedException, DeviceException {
		mosipDeviceSpecificationHelper.validateJWTResponse(digitalId, digitalIdTrustDomain);
		return mosipDeviceSpecificationHelper.getMapper().readValue(
				new String(CryptoUtil.decodeURLSafeBase64(mosipDeviceSpecificationHelper.getPayLoad(digitalId))),
				SbiDigitalId.class);
	}

	private static String getDevicCode(String deviceType) {
		switch (deviceType.toUpperCase()) {
		case RegistrationConstants.FINGERPRINT_UPPERCASE:
			deviceType = "FIR";
			break;

		case RegistrationConstants.IRIS:
			deviceType = "IIR";
			break;
		}

		return deviceType;

	}

	@Counted(recordFailuresOnly = true, extraTags = {"version", "1.0"})
	@Timed(extraTags = {"version", "1.0"})
	@Override
	public boolean isDeviceAvailable(MdmBioDevice mdmBioDevice) {

		boolean isDeviceAvailable = false;

		try {
			SbiDeviceDiscoveryRequest sbiDeviceDiscoveryRequest = new SbiDeviceDiscoveryRequest();
			sbiDeviceDiscoveryRequest.setType(mdmBioDevice.getDeviceType());

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Entering into Device availbale check....." + System.currentTimeMillis());

			ObjectMapper mapper = new ObjectMapper();
			String requestBody = mapper.writeValueAsString(sbiDeviceDiscoveryRequest);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Request for RCapture...." + requestBody);

			CloseableHttpClient client = HttpClients.createDefault();
			StringEntity requestEntity = new StringEntity(requestBody,
					ContentType.create("Content-Type", Consts.UTF_8));
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Bulding Device availbale check url...." + System.currentTimeMillis());

			HttpUriRequest request = RequestBuilder.create("SBIDISC")
					.setUri(mosipDeviceSpecificationHelper.buildUrl(mdmBioDevice.getPort(), "device"))
					.setEntity(requestEntity).build();

			CloseableHttpResponse response = client.execute(request);
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"parsing device discovery response to 092 dto");
			List<SbiDeviceDiscoveryMDSResponse> deviceList = (mosipDeviceSpecificationHelper.getMapper().readValue(
					EntityUtils.toString(response.getEntity()), new TypeReference<List<SbiDeviceDiscoveryMDSResponse>>() {	}));
			for(SbiDeviceDiscoveryMDSResponse response1 : deviceList) {
				if(response1 != null &&  response1.getSpecVersion() != null && response1.getDeviceStatus() != null && response1.getCertification() != null) {
					if(Arrays.asList(response1.getSpecVersion()).contains(SPEC_VERSION)) {
						isDeviceAvailable = true;
					}if (RegistrationConstants.DEVICE_STATUS_READY.equalsIgnoreCase(response1.getDeviceStatus())){
						isDeviceAvailable = true;
					} if(response1.getCertification().equals(mdmBioDevice.getCertification())) {
						isDeviceAvailable = true;
					}
				}
				
			}
			//isDeviceAvailable = deviceList.stream().anyMatch(resp ->
					//Arrays.asList(resp.getSpecVersion()).contains(SPEC_VERSION)
							//&& RegistrationConstants.DEVICE_STATUS_READY.equalsIgnoreCase(resp.getDeviceStatus())
							//&& resp.getCertification().equals(mdmBioDevice.getCertification()));

		} catch (Throwable exception) {
			LOGGER.error(MOSIP_BIO_DEVICE_INTEGERATOR, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
		}
		return isDeviceAvailable;
	}
}
