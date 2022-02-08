package io.mosip.registration.util.advice;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.clientcrypto.util.ClientCryptoUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.keymanagerservice.dto.KeyPairGenerateResponseDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyRequestDto;
import io.mosip.kernel.signature.dto.JWTSignatureVerifyResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.entity.FileSignature;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.repositories.FileSignatureRepository;
import io.mosip.registration.service.sync.PublicKeySync;
import io.mosip.registration.util.restclient.RequestHTTPDTO;

/**
 * All the responses of the rest call services which are invoking from the
 * reg-client will get signed from this class.
 * 
 * @author Sreekar Chukka
 * @since 1.0.0
 *
 */
@Aspect
@Component
public class ResponseSignatureAdvice {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = AppConfig.getLogger(ResponseSignatureAdvice.class);

	private static final String CERTIFICATE_API_PATH = "/v1/syncdata/getCertificate";
	private static final List<String> IGNORE_ERROR_CODES = new ArrayList<String>();

	static {
		IGNORE_ERROR_CODES.add("KER-KMS-012");
		IGNORE_ERROR_CODES.add("KER-KMS-002");
	}

	@Value("${mosip.utc-datetime-pattern:yyyy-MM-dd'T'HH:mm:ss.SSS'Z'}")
	private String DATETIME_PATTERN;

	@Value("${mosip.sign.refid:SIGN}")
	private String signRefId;

	@Autowired
    private SignatureService signatureService;
	
	@Autowired
	private KeymanagerService keymanagerService;

	@Autowired
	private PublicKeySync publicKeySync;

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private FileSignatureRepository fileSignatureRepository;

	/**
	 * <p>
	 * It is an after returning method in which for each and everytime after
	 * successfully invoking the
	 * "io.mosip.registration.util.restclient.RestClientUtil.invoke()" method, this
	 * method will be called.
	 * </p>
	 * 
	 * Here we are passing three arguments as parameters
	 * <ol>
	 * <li>SignIn Key - Public Key from Kernel</li>
	 * <li>Response - Signature from response header</li>
	 * <li>Response Body - Getting from the Service response</li>
	 * </ol>
	 * 
	 * The above three values are passed to the {@link SignatureService} where the
	 * validation will happen for the response that we send
	 * 
	 * @param joinPoint - the JointPoint
	 * @param result    - the object result
	 * @return the rest client response as {@link Map}
	 * @throws RegBaseCheckedException - the exception class that handles all the
	 *                                 checked exceptions
	 */
	@SuppressWarnings("unchecked")
	@AfterReturning(pointcut = "execution(* io.mosip.registration.util.restclient.RestClientUtil.invokeURL(..))", returning = "result")
	public synchronized Map<String, Object> responseSignatureValidation(JoinPoint joinPoint, Object result)
			throws RegBaseCheckedException {
		LOGGER.info("Response signature advice triggered...");

		HttpHeaders responseHeader = null;
		Object[] requestHTTPDTO = joinPoint.getArgs();
		RequestHTTPDTO requestDto = (RequestHTTPDTO) requestHTTPDTO[0];
		LinkedHashMap<String, Object> restClientResponse = null;
		try {
			restClientResponse = (LinkedHashMap<String, Object>) result;
			if (null == requestDto || !requestDto.getIsSignRequired())
				return restClientResponse;

			LinkedHashMap<String, Object> responseBodyMap = (LinkedHashMap<String, Object>) restClientResponse
					.get(RegistrationConstants.REST_RESPONSE_BODY);
			if (null != responseBodyMap && responseBodyMap.size() > 0 && null != responseBodyMap.get(RegistrationConstants.RESPONSE)) {

				if (responseBodyMap.get(RegistrationConstants.RESPONSE) instanceof LinkedHashMap){
					LinkedHashMap<String, Object> resp = (LinkedHashMap<String, Object>) responseBodyMap
							.get(RegistrationConstants.RESPONSE);
					checkAndUploadCertificate(resp, joinPoint);
				}

				responseHeader = (HttpHeaders) restClientResponse.get(RegistrationConstants.REST_RESPONSE_HEADERS);

				if (responseHeader != null && responseHeader.containsKey(RegistrationConstants.RESPONSE_SIGNATURE)
						&& responseHeader.get(RegistrationConstants.RESPONSE_SIGNATURE) != null
						&& !responseHeader.get(RegistrationConstants.RESPONSE_SIGNATURE).isEmpty()
						&& isResponseSignatureValid(responseHeader.get(RegistrationConstants.RESPONSE_SIGNATURE).get(0),
								new ObjectMapper().writeValueAsString(responseBodyMap))) {
					LOGGER.info("Response signature is valid... {}", requestDto.getUri());
					return restClientResponse;
				} else {
					LOGGER.info("Response signature is INVALID... {}", requestDto.getUri());
					restClientResponse.put(RegistrationConstants.REST_RESPONSE_BODY, new LinkedHashMap<>());
					restClientResponse.put(RegistrationConstants.REST_RESPONSE_HEADERS, new LinkedHashMap<>());
				}
			}
		} catch (RuntimeException | JsonProcessingException regBaseCheckedException) {
			LOGGER.error(regBaseCheckedException.getMessage(), regBaseCheckedException);
			throw new RegBaseCheckedException("Exception in response signature", regBaseCheckedException.getMessage());
		}

		LOGGER.info("Successfully leaving response signature advice.");
		return restClientResponse;
	}


	@AfterReturning(pointcut = "execution(* io.mosip.registration.util.restclient.RestClientUtil.downloadFile(..))")
	public synchronized void fileSignatureValidation(JoinPoint joinPoint) throws RegBaseCheckedException {
		LOGGER.info("File download response signature advice triggered...");
		Object[] requestArgument = joinPoint.getArgs();
		RequestHTTPDTO requestHTTPDTO = (RequestHTTPDTO) requestArgument[0];

		Optional<FileSignature> result = fileSignatureRepository.findByFileName(requestHTTPDTO.getFilePath().toFile().getName());
		try {
			if(result.isPresent()) {
				byte[] data = requestHTTPDTO.isFileEncrypted() ?
						clientCryptoFacade.decrypt(ClientCryptoUtils.decodeBase64Data(
								FileUtils.readFileToString(requestHTTPDTO.getFilePath().toFile(), StandardCharsets.UTF_8))) :
						FileUtils.readFileToByteArray(requestHTTPDTO.getFilePath().toFile());
				String actualData = String.format("{\"hash\":\"%s\"}", HMACUtils2.digestAsPlainText(data));
				if(isResponseSignatureValid(result.get().getSignature(), actualData)) {
					LOGGER.info("File signature check passed, {}", requestHTTPDTO.getFilePath());
					return;
				}
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		if (requestHTTPDTO.getFilePath().toFile().delete()) 
			LOGGER.info("response signature file deleted");
		throw new RegBaseCheckedException(RegistrationExceptionConstants.REG_FILE_SIGNATURE_ERROR.getErrorCode(),
				RegistrationExceptionConstants.REG_FILE_SIGNATURE_ERROR.getErrorMessage());
	}

	private boolean isResponseSignatureValid(String signature, String actualData) {
        KeyPairGenerateResponseDto certificateDto = keymanagerService
				.getCertificate(RegistrationConstants.RESPONSE_SIGNATURE_PUBLIC_KEY_APP_ID,
						Optional.of(RegistrationConstants.RESPONSE_SIGNATURE_PUBLIC_KEY_REF_ID));

		JWTSignatureVerifyRequestDto jwtSignatureVerifyRequestDto = new JWTSignatureVerifyRequestDto();
		jwtSignatureVerifyRequestDto.setJwtSignatureData(signature);
		jwtSignatureVerifyRequestDto.setActualData(CryptoUtil.encodeToURLSafeBase64(actualData.getBytes(StandardCharsets.UTF_8)));
		jwtSignatureVerifyRequestDto.setCertificateData(certificateDto.getCertificate());

		JWTSignatureVerifyResponseDto verifyResponseDto =  signatureService.jwtVerify(jwtSignatureVerifyRequestDto);
		return verifyResponseDto.isSignatureValid();
	}

	/**
	 * Checks if this is Sign certificate fetch API call
	 * if yes, it saves the certificate in keystore
	 * @param resp
	 * @param joinPoint
	 */
	private void checkAndUploadCertificate(LinkedHashMap<String, Object> resp, JoinPoint joinPoint) {
		if(joinPoint.getArgs() != null && joinPoint.getArgs() instanceof Object[] && joinPoint.getArgs()[0] != null) {
			RequestHTTPDTO requestDto = (RequestHTTPDTO) joinPoint.getArgs()[0];

			UriComponents uriComponents = UriComponentsBuilder.fromUri(requestDto.getUri()).build();
			if(!(CERTIFICATE_API_PATH.equals(uriComponents.getPath()) &&
					uriComponents.getQueryParams().containsKey(RegistrationConstants.GET_CERT_APP_ID) &&
					RegistrationConstants.KERNEL_APP_ID.equals(uriComponents.getQueryParams().getFirst(RegistrationConstants.GET_CERT_APP_ID)) &&
					uriComponents.getQueryParams().containsKey(RegistrationConstants.REF_ID) &&
					signRefId.equals(uriComponents.getQueryParams().getFirst(RegistrationConstants.REF_ID)))) {
				return;
			}

			publicKeySync.saveSignPublicKey(resp.get(RegistrationConstants.CERTIFICATE).toString());
		}
	}

}
