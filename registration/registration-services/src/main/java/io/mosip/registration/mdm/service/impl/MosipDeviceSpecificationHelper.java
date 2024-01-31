package io.mosip.registration.mdm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.signature.constant.SignatureConstant;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyRequestDto;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.DeviceException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.dto.DeviceInfo;
import io.mosip.registration.mdm.dto.MDMError;
import io.mosip.registration.mdm.dto.MdmDeviceInfo;
import io.mosip.registration.mdm.sbi.spec_1_0.dto.response.MdmSbiDeviceInfoWrapper;

import org.apache.http.Consts;
import org.apache.http.client.config.RequestConfig;
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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

/**
 * All helper methods commons to spec implementations
 *
 * @author anusha
 */
@Component
public class MosipDeviceSpecificationHelper {

	private static final Logger LOGGER = AppConfig.getLogger(MosipDeviceSpecificationHelper.class);
	private static final String MDM_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private SignatureService signatureService;

	@Value("${mosip.registration.mdm.trust.domain.rcapture:DEVICE}")
	private String rCaptureTrustDomain;

	@Value("${mosip.registration.mdm.trust.domain.digitalId:DEVICE}")
	private String digitalIdTrustDomain;

	@Value("${mosip.registration.mdm.trust.domain.deviceinfo:DEVICE}")
	private String deviceInfoTrustDomain;

	private final String CONTENT_LENGTH = "Content-Length:";

	public String getPayLoad(String data) throws RegBaseCheckedException {
		if (data == null || data.isEmpty()) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorCode(),
					RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorMessage());
		}
		Pattern pattern = Pattern.compile(RegistrationConstants.BIOMETRIC_SEPERATOR);
		Matcher matcher = pattern.matcher(data);
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_PAYLOAD_EMPTY.getErrorCode(),
				RegistrationExceptionConstants.MDS_PAYLOAD_EMPTY.getErrorMessage());
	}
	
	public String getSignature(String data) throws RegBaseCheckedException {
		if (data == null || data.isEmpty()) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorCode(),
					RegistrationExceptionConstants.MDS_JWT_INVALID.getErrorMessage());
		}
		Pattern pattern = Pattern.compile(RegistrationConstants.BIOMETRIC_SEPERATOR);
		Matcher matcher = pattern.matcher(data);
		if(matcher.find()) {
			//returns header..signature
			return data.replace(matcher.group(1),"");
		}

		throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_SIGNATURE_EMPTY.getErrorCode(),
				RegistrationExceptionConstants.MDS_SIGNATURE_EMPTY.getErrorMessage());
	}

	public DeviceInfo getDeviceInfoDecoded(String deviceInfo, Class<?> classType) {
		try {
			validateJWTResponse(deviceInfo, deviceInfoTrustDomain);
			String result = new String(CryptoUtil.decodeURLSafeBase64(getPayLoad(deviceInfo)));
			if(classType.getName().equals("io.mosip.registration.mdm.sbi.spec_1_0.service.impl.MosipDeviceSpecification_SBI_1_0_ProviderImpl")) {
				return mapper.readValue(result, MdmSbiDeviceInfoWrapper.class);
			} else {
				return mapper.readValue(result, MdmDeviceInfo.class);
			}
			
		} catch (Exception exception) {
			LOGGER.error(APPLICATION_ID, APPLICATION_NAME, "Failed to decode device info",
					ExceptionUtils.getStackTrace(exception));
		}
		return null;
	}

	public void validateJWTResponse(final String signedData, final String domain) throws DeviceException {
		JWTSignatureVerifyRequestDto jwtSignatureVerifyRequestDto = new JWTSignatureVerifyRequestDto();
		jwtSignatureVerifyRequestDto.setValidateTrust(true);
		jwtSignatureVerifyRequestDto.setDomain(domain);
		jwtSignatureVerifyRequestDto.setJwtSignatureData(signedData);
		
		JWTSignatureVerifyResponseDto jwtSignatureVerifyResponseDto = signatureService.jwtVerify(jwtSignatureVerifyRequestDto);
		if(!jwtSignatureVerifyResponseDto.isSignatureValid())
				throw new DeviceException(MDMError.MDM_INVALID_SIGNATURE.getErrorCode(), MDMError.MDM_INVALID_SIGNATURE.getErrorMessage());
		
		if (jwtSignatureVerifyRequestDto.getValidateTrust() && !jwtSignatureVerifyResponseDto.getTrustValid().equals(SignatureConstant.TRUST_VALID)) {
		      throw new DeviceException(MDMError.MDM_CERT_PATH_TRUST_FAILED.getErrorCode(), MDMError.MDM_CERT_PATH_TRUST_FAILED.getErrorMessage());
		}
	}

	public String generateMDMTransactionId() {
		return UUID.randomUUID().toString();
	}

	public String buildUrl(int port, String endPoint) {
		return String.format("%s:%s/%s", getRunningurl(), port, endPoint);
	}

	private String getRunningurl() {
		return "http://127.0.0.1";
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	/**
	 * Using the urlStream get the next JPEG image as a byte[]
	 *
	 * @return byte[] of the JPEG
	 * @throws IOException
	 * @throws RegBaseCheckedException
	 */
	public byte[] getJPEGByteArray(InputStream urlStream, long maxTimeLimit)
			throws IOException, RegBaseCheckedException {

		int currByte = -1;

		boolean captureContentLength = false;
		StringWriter contentLengthStringWriter = new StringWriter(128);
		StringWriter headerWriter = new StringWriter(128);

		int contentLength = 0;

		while ((currByte = urlStream.read()) > -1) {
			if (captureContentLength) {
				if (currByte == 10 || currByte == 13) {
					contentLength = Integer.parseInt(contentLengthStringWriter.toString().replace(" ", ""));
					break;
				}
				contentLengthStringWriter.write(currByte);

			} else {
				headerWriter.write(currByte);
				String tempString = headerWriter.toString();
				int indexOf = tempString.indexOf(CONTENT_LENGTH);
				if (indexOf > 0) {
					captureContentLength = true;
				}
			}
			timeOutCheck(maxTimeLimit);
		}

		// 255 indicates the start of the jpeg image
		while (urlStream.read() != 255) {

			timeOutCheck(maxTimeLimit);
		}

		// rest is the buffer
		byte[] imageBytes = new byte[contentLength + 1];
		// since we ate the original 255 , shove it back in
		imageBytes[0] = (byte) 255;
		int offset = 1;
		int numRead = 0;
		while (offset < imageBytes.length
				&& (numRead = urlStream.read(imageBytes, offset, imageBytes.length - offset)) >= 0) {
			timeOutCheck(maxTimeLimit);
			offset += numRead;
		}

		return imageBytes;
	}

	private void timeOutCheck(long maxTimeLimit) throws RegBaseCheckedException {

		if (System.currentTimeMillis() > maxTimeLimit) {

			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_STREAM_TIMEOUT.getErrorCode(),
					RegistrationExceptionConstants.MDS_STREAM_TIMEOUT.getErrorMessage());
		}
	}
	
	public void validateResponseTimestamp(String responseTime) throws RegBaseCheckedException {
		if(responseTime != null) {
			LocalDateTime ts = DateUtils.parseUTCToLocalDateTime(responseTime, MDM_DATETIME_PATTERN);
			//LocalDateTime ts = DateUtils.convertUTCToLocalDateTime(responseTime);
			if(ts.isAfter(LocalDateTime.now().minusMinutes(getAllowedLagInMinutes()))
					&& ts.isBefore(LocalDateTime.now().plusMinutes(getAllowedLagInMinutes())))
				return;
		}

		throw new RegBaseCheckedException(
				RegistrationExceptionConstants.MDS_CAPTURE_INVALID_TIME.getErrorCode(),
				RegistrationExceptionConstants.MDS_CAPTURE_INVALID_TIME.getErrorMessage());
	}

	private int getAllowedLagInMinutes() {
		return Integer.parseInt((String) ApplicationContext.map()
				.getOrDefault(RegistrationConstants.MDS_RESP_ALLOWED_LAG_MINS, "5"));
	}
	
	
	public void validateQualityScore(String qualityScore) throws RegBaseCheckedException {
		if (qualityScore == null || qualityScore.isEmpty()) {
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorCode(),
					RegistrationExceptionConstants.MDS_RCAPTURE_ERROR.getErrorMessage()
							+ " Identified Quality Score for capture biometrics is null or Empty");
		}
	}

	public static int getMDMConnectionTimeout(String method) {
		Integer timeout = ApplicationContext.getIntValueFromApplicationMap(
				String.format(RegistrationConstants.METHOD_BASED_MDM_CONNECTION_TIMEOUT, method.toUpperCase()));
		if(timeout == null || timeout == 0) {
			timeout = ApplicationContext.getIntValueFromApplicationMap(RegistrationConstants.MDM_CONNECTION_TIMEOUT);
		}
		return (timeout == null || timeout == 0) ? 10000 : timeout;
	}

	public String getHttpClientResponseEntity(String url, String method, String body) throws IOException {
		int timeout = getMDMConnectionTimeout(method);
		LOGGER.debug("MDM HTTP CALL method : {}  with timeout {}", method, timeout);
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(timeout)
				.setSocketTimeout(timeout)
				.setConnectionRequestTimeout(timeout)
				.build();
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			StringEntity requestEntity = new StringEntity(body, ContentType.create("Content-Type", Consts.UTF_8));
			HttpUriRequest httpUriRequest = RequestBuilder.create(method)
					.setConfig(requestConfig)
					.setUri(url)
					.setEntity(requestEntity)
					.build();
			CloseableHttpResponse response = client.execute(httpUriRequest);
			return EntityUtils.toString(response.getEntity());
		}
	}
	
	public CloseableHttpResponse getHttpClientResponse(String url, String method, String body) throws IOException {
		int timeout = getMDMConnectionTimeout(method);
		LOGGER.debug("MDM HTTP CALL method : {}  with timeout {}", method, timeout);
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(timeout)
				.setSocketTimeout(timeout)
				.setConnectionRequestTimeout(timeout)
				.build();
		CloseableHttpClient client = HttpClients.createDefault();
		StringEntity requestEntity = new StringEntity(body, ContentType.create("Content-Type", Consts.UTF_8));
		HttpUriRequest httpUriRequest = RequestBuilder.create(method)
				.setConfig(requestConfig)
				.setUri(url)
				.setEntity(requestEntity)
				.build();
		return client.execute(httpUriRequest);
	}
}
