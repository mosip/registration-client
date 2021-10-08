package io.mosip.registration.util.advice;


import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.util.restclient.AuthTokenUtilService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.AuthTokenDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.util.restclient.RequestHTTPDTO;


/**
 * The Class RestClientAuthAdvice checks whether the invoking REST service
 * should required authentication. If required then the auth service is invoked
 * to get the token.
 * 
 * @author Balaji Sridharan
 * @author Mahesh Kumar
 */
@Aspect
@Component
public class RestClientAuthAdvice {

	private static final Logger LOGGER = AppConfig.getLogger(RestClientAuthAdvice.class);

	@Autowired
	private ClientCryptoFacade clientCryptoFacade;

	@Autowired
	private AuthTokenUtilService authTokenUtilService;


	/**
	 * The {@link Around} advice method which be invoked for all web services. This
	 * advice adds the Authorization Token to the Web-Service Request Header, if
	 * authorization is required. If Authorization Token had expired, a new token
	 * will be requested.
	 * 
	 * @param joinPoint
	 *            the join point of the advice
	 * @return the response from the web-service
	 * @throws RegBaseCheckedException
	 *             - generalized exception with errorCode and errorMessage
	 */
	@Around("execution(* io.mosip.registration.util.restclient.RestClientUtil.invokeURL(..))")
	public Object addAuthZToken(ProceedingJoinPoint joinPoint) throws RegBaseCheckedException, ConnectionException {
		RequestHTTPDTO requestHTTPDTO = (RequestHTTPDTO) joinPoint.getArgs()[0];
		try {
			LOGGER.info("Auth advice triggered to check add authZ token to web service request header...");

			if (requestHTTPDTO.isRequestSignRequired()) {
				addRequestSignature(requestHTTPDTO.getHttpHeaders(), requestHTTPDTO.getRequestBody());
			}

			if (requestHTTPDTO.isAuthRequired()) {
				String authZToken = getAuthZToken(requestHTTPDTO);
				setAuthHeaders(requestHTTPDTO.getHttpHeaders(), requestHTTPDTO.getAuthZHeader(), authZToken);
			}

			requestHTTPDTO.setHttpEntity(new HttpEntity<>(requestHTTPDTO.getRequestBody(), requestHTTPDTO.getHttpHeaders()));
			Object response = joinPoint.proceed(joinPoint.getArgs());

			LOGGER.info("completed with request Auth advice");
			return response;

		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error("Failed in AuthAdvice >> {} {}", requestHTTPDTO.getUri(), regBaseCheckedException);
			throw regBaseCheckedException;
		} catch (Throwable throwable) {
			LOGGER.error("UNKNOWN ERROR >> {} {}", requestHTTPDTO.getUri(), throwable);
			throw new RegBaseCheckedException("UNKNOWN_ERROR", throwable.getMessage());
		}
	}

	@Around("execution(* io.mosip.registration.util.restclient.RestClientUtil.downloadFile(..))")
	public void addAuthZTokenToDownloadRequest(ProceedingJoinPoint joinPoint) throws RegBaseCheckedException, ConnectionException {
		RequestHTTPDTO requestHTTPDTO = (RequestHTTPDTO) joinPoint.getArgs()[0];
		addAuthZToken(joinPoint);
	}

	private String getAuthZToken(RequestHTTPDTO requestHTTPDTO)
			throws RegBaseCheckedException {
		AuthTokenDTO authZToken = authTokenUtilService.fetchAuthToken(requestHTTPDTO.getTriggerPoint());
		return authZToken.getCookie();
	}


	/**
	 * Setup of Auth Headers.
	 *
	 * @param httpHeaders
	 *            http headers
	 * @param authHeader
	 *            auth header
	 * @param authZCookie
	 *            the Authorization Token or Cookie
	 */
	private void setAuthHeaders(HttpHeaders httpHeaders, String authHeader, String authZCookie) {
		LOGGER.info("Adding authZ token to request header");

		String[] arrayAuthHeaders = null;

		if (authHeader != null) {
			arrayAuthHeaders = authHeader.split(":");
			if (arrayAuthHeaders[1].equalsIgnoreCase(RegistrationConstants.REST_OAUTH)) {
				httpHeaders.add(RegistrationConstants.COOKIE, authZCookie);
			} else if (arrayAuthHeaders[1].equalsIgnoreCase(RegistrationConstants.AUTH_TYPE)) {
				httpHeaders.add(arrayAuthHeaders[0], arrayAuthHeaders[1]);
			}
		}

		LOGGER.info("Adding of authZ token to request header completed");
	}

	/**
	 * Add request signature to the request header
	 * 
	 * @param httpHeaders
	 *            the HTTP headers for the web-service request
	 * @param requestBody
	 *            the request body
	 * @throws RegBaseCheckedException
	 *             exception while generating request signature
	 */
	private void addRequestSignature(HttpHeaders httpHeaders, Object requestBody) throws RegBaseCheckedException {
		LOGGER.info("Adding request signature to request header");

		try {
			httpHeaders.add("request-signature", String.format("Authorization:%s", CryptoUtil
					.encodeToURLSafeBase64(clientCryptoFacade.getClientSecurity().signData(JsonUtils.javaObjectToJsonString(requestBody).getBytes()))));
			httpHeaders.add(RegistrationConstants.KEY_INDEX, CryptoUtil.computeFingerPrint(
					clientCryptoFacade.getClientSecurity().getEncryptionPublicPart(), null));
		} catch (JsonProcessingException jsonProcessingException) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.AUTHZ_ADDING_REQUEST_SIGN.getErrorCode(),
					RegistrationExceptionConstants.AUTHZ_ADDING_REQUEST_SIGN.getErrorMessage(),
					jsonProcessingException);
		}

		LOGGER.info("Completed adding request signature to request header completed");
	}

}
