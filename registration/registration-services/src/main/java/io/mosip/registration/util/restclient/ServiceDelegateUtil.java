package io.mosip.registration.util.restclient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;

import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;

/**
 * This is a helper class .it invokes with different classes to get the response
 * 
 * @author Yaswanth S
 * @since 1.0.0
 *
 */
@Component("serviceDelegateUtil")
public class ServiceDelegateUtil {

	private static final Logger LOGGER = AppConfig.getLogger(ServiceDelegateUtil.class);

	@Autowired
	private RestClientUtil restClientUtil;

	@Autowired
	private Environment environment;



	/**
	 * Prepare and trigger GET request.
	 *
	 * @param serviceName
	 *            service to be invoked
	 * @param requestParams
	 *            parameters along with url
	 * @param hasPathParams
	 *            the has path params
	 * @param triggerPoint
	 *            system or user driven invocation
	 * @return Object requiredType of object response Body
	 * @throws RegBaseCheckedException
	 *             generalised exception with errorCode and errorMessage
	 * @throws ConnectionException
	 *             when client error exception from server / server exception
	 */
	public Object get(String serviceName, Map<String, String> requestParams, boolean hasPathParams, String triggerPoint)
			throws RegBaseCheckedException, ConnectionException {

		LOGGER.debug("Get method has been called - {}", serviceName);

		Map<String, Object> responseMap;
		Object responseBody = null;

		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();

		try {
			requestHTTPDTO = prepareGETRequest(requestHTTPDTO, serviceName, requestParams);
			requestHTTPDTO.setAuthRequired(
					Boolean.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.AUTH_REQUIRED)));
			requestHTTPDTO.setAuthZHeader(getEnvironmentProperty(serviceName, RegistrationConstants.AUTH_HEADER));
			requestHTTPDTO.setIsSignRequired(
					Boolean.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.SIGN_REQUIRED)));
			requestHTTPDTO.setTriggerPoint(triggerPoint);
			requestHTTPDTO.setRequestSignRequired(
					Boolean.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.REQUEST_SIGN_REQUIRED)));

			// URI creation
			String url = getEnvironmentProperty(serviceName, RegistrationConstants.SERVICE_URL);
			url = RegistrationAppHealthCheckUtil.prepareURLByHostName(url);
			Map<String, String> queryParams = new HashMap<>();
			for (String key : requestParams.keySet()) {
				if (!url.contains("{" + key + "}")) {
					queryParams.put(key, requestParams.get(key));
				}
			}

			if (hasPathParams) {
				requestHTTPDTO.setUri(UriComponentsBuilder.fromUriString(url).build(requestParams));
				url = requestHTTPDTO.getUri().toString();
			}

			/** Set URI */
			setURI(requestHTTPDTO, queryParams, url);

			responseMap = restClientUtil.invokeURL(requestHTTPDTO);

		}  catch (RestClientException e) {
			LOGGER.error(e.getMessage(), e);
			throw new ConnectionException(RegistrationExceptionConstants.ACCESS_ERROR.getErrorCode(),
					RegistrationExceptionConstants.ACCESS_ERROR.getErrorMessage(), e);
		}

		if (isResponseValid(responseMap, RegistrationConstants.REST_RESPONSE_BODY)) {
			responseBody = responseMap.get(RegistrationConstants.REST_RESPONSE_BODY);
		}
		LOGGER.debug("Get method has been ended - {}", serviceName);

		return responseBody;
	}


	/**
	 * prepare and trigger POST request.
	 *
	 * @param serviceName
	 *            service to be invoked
	 * @param object
	 *            request type
	 * @param triggerPoint
	 *            system or user driven invocation
	 * @return Object requiredType of object response Body
	 * @throws RegBaseCheckedException
	 *             generalised exception with errorCode and errorMessage
	 * @throws ConnectionException
	 *             when client error, server error, access error
	 */
	public Object post(String serviceName, Object object, String triggerPoint) throws RegBaseCheckedException, ConnectionException {
		LOGGER.debug("Post method called - {} ", serviceName);

		RequestHTTPDTO requestDto;
		Object responseBody = null;
		Map<String, Object> responseMap = null;

		try {
			requestDto = preparePOSTRequest(serviceName, object);
			requestDto.setAuthRequired(
					Boolean.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.AUTH_REQUIRED)));
			requestDto.setAuthZHeader(getEnvironmentProperty(serviceName, RegistrationConstants.AUTH_HEADER));
			requestDto.setIsSignRequired(
					Boolean.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.SIGN_REQUIRED)));
			requestDto.setTriggerPoint(triggerPoint);
			requestDto.setRequestSignRequired(
					Boolean.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.REQUEST_SIGN_REQUIRED)));
			responseMap = restClientUtil.invokeURL(requestDto);
		} catch (RestClientException e) {
			LOGGER.error(e.getMessage(), e);
			throw new ConnectionException(RegistrationExceptionConstants.ACCESS_ERROR.getErrorCode(),
					RegistrationExceptionConstants.ACCESS_ERROR.getErrorMessage(), e);
		}

		if (isResponseValid(responseMap, RegistrationConstants.REST_RESPONSE_BODY)) {
			responseBody = responseMap.get(RegistrationConstants.REST_RESPONSE_BODY);
		}
		LOGGER.debug("Post method ended - {} ", serviceName);

		return responseBody;
	}

	/**
	 * Builds the request and passess it to REST client util
	 * 
	 * @param url
	 *            - MDM service url
	 * @param serviceName
	 *            - MDM service name
	 * @param request
	 *            - request data
	 * @param responseType
	 *            - response format
	 * @return Object - response body
	 * @throws RegBaseCheckedException
	 *             - generalized exception with errorCode and errorMessage
	 */
	/*public Object invokeRestService(String url, String serviceName, Object request, Class<?> responseType)
			throws RegBaseCheckedException {

		LOGGER.debug(LoggerConstants.LOG_SERVICE_DELEGATE_UTIL_GET, APPLICATION_NAME, APPLICATION_ID,
				"invokeRestService method has been called");

		Map<String, Object> responseMap = null;
		Object responseBody = null;

		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();

		prepareRequest(requestHTTPDTO, serviceName, request, responseType, url);

		try {
			responseMap = restClientUtil.invoke(requestHTTPDTO);
		} catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException
				| SocketTimeoutException exception) {
			throw new RegBaseUncheckedException(
					RegistrationExceptionConstants.REG_SERVICE_DELEGATE_UTIL_CODE.getErrorCode(),
					RegistrationExceptionConstants.REG_SERVICE_DELEGATE_UTIL_CODE.getErrorMessage(), exception);
		}
		if (isResponseValid(responseMap, RegistrationConstants.REST_RESPONSE_BODY)) {
			responseBody = responseMap.get(RegistrationConstants.REST_RESPONSE_BODY);
		}
		LOGGER.debug(LoggerConstants.LOG_SERVICE_DELEGATE_UTIL_GET, APPLICATION_NAME, APPLICATION_ID,
				"invokeRestService method has been ended");

		return responseBody;

	}*/

	/**
	 * prepares the request
	 * 
	 * @param requestHTTPDTO
	 *            - holds the request data for a REST call
	 * @param serviceName
	 *            - service name
	 * @param request
	 *            - request data
	 * @param responseType
	 *            - response format
	 * @param url
	 *            - the URL
	 */
	/*protected void prepareRequest(RequestHTTPDTO requestHTTPDTO, String serviceName, Object request,
			Class<?> responseType, String url) {
		LOGGER.info(LoggerConstants.LOG_SERVICE_DELEGATE_UTIL_PREPARE_REQUEST, APPLICATION_NAME, APPLICATION_ID,
				"Preparing request");

		requestHTTPDTO.setHttpMethod(
				HttpMethod.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.HTTPMETHOD)));
		requestHTTPDTO.setHttpHeaders(new HttpHeaders());
		requestHTTPDTO.setRequestBody(request);
		requestHTTPDTO.setClazz(Object.class);
		requestHTTPDTO.setIsSignRequired(false);
		try {
			requestHTTPDTO.setUri(new URI(url));
		} catch (URISyntaxException uriSyntaxException) {
		}
		// set timeout
		setTimeout(requestHTTPDTO);
		// Headers
		setHeaders(requestHTTPDTO.getHttpHeaders(), getEnvironmentProperty(serviceName, RegistrationConstants.HEADERS));
		requestHTTPDTO.setAuthRequired(false);
	}*/

	/**
	 * Prepare GET request.
	 *
	 * @param requestHTTPDTO
	 *            the request HTTPDTO
	 * @param serviceName
	 *            service to be invoked
	 * @param requestParams
	 *            params need to add along with url
	 * @return RequestHTTPDTO requestHTTPDTO with required data
	 * @throws RegBaseCheckedException
	 *             the reg base checked exception
	 */
	private RequestHTTPDTO prepareGETRequest(RequestHTTPDTO requestHTTPDTO, final String serviceName,
			final Map<String, String> requestParams) throws RegBaseCheckedException {
		LOGGER.debug("Prepare Get request method called");

		// prepare httpDTO except rquest type and uri build
		prepareRequest(requestHTTPDTO, serviceName, null);

		// ResponseType
		Class<?> responseClass = null;
		try {
			responseClass = Class.forName(getEnvironmentProperty(serviceName, RegistrationConstants.RESPONSE_TYPE));
		} catch (ClassNotFoundException classNotFoundException) {
			throw new RegBaseCheckedException(
					RegistrationExceptionConstants.REG_CLASS_NOT_FOUND_ERROR_CODE.getErrorCode(),
					RegistrationExceptionConstants.REG_CLASS_NOT_FOUND_ERROR_CODE.getErrorMessage(),
					classNotFoundException);
		}

		requestHTTPDTO.setClazz(responseClass);

		LOGGER.debug("Prepare Get request method ended");

		return requestHTTPDTO;
	}

	/**
	 * Prepare POST request.
	 *
	 * @param serviceName
	 *            service to be invoked
	 * @param object
	 *            request type
	 * @return RequestHTTPDTO requestHTTPDTO with required data
	 */
	private RequestHTTPDTO preparePOSTRequest(final String serviceName, final Object object) {
		LOGGER.debug("Preparing post request for web-service");

		// DTO need to to be prepared
		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();

		// prepare httpDTO except rquest type and uri build
		prepareRequest(requestHTTPDTO, serviceName, object);

		// URI creation
		setURI(requestHTTPDTO, null, getEnvironmentProperty(serviceName, RegistrationConstants.SERVICE_URL));

		// RequestType
		requestHTTPDTO.setClazz(Object.class);

		LOGGER.debug("Completed preparing post request for web-service");

		return requestHTTPDTO;

	}

	/**
	 * Sets the URI.
	 *
	 * @param requestHTTPDTO
	 *            the request HTTPDTO
	 * @param requestParams
	 *            the request params
	 * @param url
	 *            the url
	 */
	private void setURI(RequestHTTPDTO requestHTTPDTO, Map<String, String> requestParams, String url) {
		LOGGER.debug("Preparing URI for web-service");

		// BuildURIComponent
		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);

		if (requestParams != null) {
			Set<String> set = requestParams.keySet();
			for (String queryParamName : set) {
				uriComponentsBuilder.queryParam(queryParamName, requestParams.get(queryParamName));

			}
		}
		URI uri = uriComponentsBuilder.build().toUri();

		requestHTTPDTO.setUri(uri);

		LOGGER.debug("Completed preparing URI for web-service");
	}

	/**
	 * Setup of headers
	 * 
	 * @param httpHeaders
	 *            http headers
	 * @param headers
	 *            headers
	 */
	private void setHeaders(HttpHeaders httpHeaders, String headers) {
		LOGGER.debug("Preparing Header for web-service request");

		String[] header = headers.split(",");
		String[] headerValues = null;
		if (header != null) {
			for (String subheader : header) {
				if (subheader != null) {
					headerValues = subheader.split(":");
					if (headerValues[0].equalsIgnoreCase("timestamp")) {
						headerValues[1] = DateUtils.formatToISOString(LocalDateTime.now());
					} else if (headerValues[0].equalsIgnoreCase("Center-Machine-RefId")) {
						headerValues[1] = String
								.valueOf(ApplicationContext.map().get(RegistrationConstants.USER_CENTER_ID))
								.concat(RegistrationConstants.UNDER_SCORE).concat(String
										.valueOf(ApplicationContext.map().get(RegistrationConstants.USER_STATION_ID)));
					} else if (headerValues[0].equalsIgnoreCase("authorization")) {
						headerValues[1] = "auth";
					} else if (headerValues[0].equalsIgnoreCase("signature")) {
						headerValues[1] = "sign";
					}
					httpHeaders.add(headerValues[0], headerValues[1]);
				}
			}
			httpHeaders.add("Cache-Control", "no-cache,max-age=0");
		}

		LOGGER.debug("Completed preparing Header for web-service request");
	}

	/**
	 * @param requestHTTPDTO
	 *            create requestedHTTPDTO
	 * @param serviceName
	 *            service name to be called
	 * @param requestBody
	 *            object to be included in HTTP entities
	 */
	private void prepareRequest(RequestHTTPDTO requestHTTPDTO, String serviceName, Object requestBody) {
		LOGGER.debug("Preparing RequestHTTPDTO object for web-service");

		requestHTTPDTO.setHttpMethod(
				HttpMethod.valueOf(getEnvironmentProperty(serviceName, RegistrationConstants.HTTPMETHOD)));
		requestHTTPDTO.setHttpHeaders(new HttpHeaders());
		requestHTTPDTO.setRequestBody(requestBody);
		// set timeout
		setTimeout(requestHTTPDTO);
		// Headers
		setHeaders(requestHTTPDTO.getHttpHeaders(), getEnvironmentProperty(serviceName, RegistrationConstants.HEADERS));

		LOGGER.debug("Completed preparing RequestHTTPDTO object for web-service");
	}

	/**
	 * Method to set the request timeout
	 * 
	 * @param requestHTTPDTO requestedHTTPDTO
	 */
	private void setTimeout(RequestHTTPDTO requestHTTPDTO) {
		// Timeout in milli second
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setReadTimeout(
				Integer.parseInt((String) ApplicationContext.map().get(RegistrationConstants.HTTP_API_READ_TIMEOUT)));
		requestFactory.setConnectTimeout(
				Integer.parseInt((String) ApplicationContext.map().get(RegistrationConstants.HTTP_API_WRITE_TIMEOUT)));
		requestHTTPDTO.setSimpleClientHttpRequestFactory(requestFactory);
	}

	/*private AuthNRequestDTO prepareAuthNRequestDTO(LoginMode loginMode) {
		LOGGER.info(LoggerConstants.LOG_SERVICE_DELEGATE_AUTH_DTO, APPLICATION_NAME, APPLICATION_ID,
				"Preparing AuthNRequestDTO Based on Login Mode >>>> " + loginMode);

		AuthNRequestDTO authNRequestDTO = new AuthNRequestDTO();
		LoginUserDTO loginUserDTO = (LoginUserDTO) ApplicationContext.map().get(RegistrationConstants.USER_DTO);

		switch (loginMode) {
		case PASSWORD:
			AuthNUserPasswordDTO authNUserPasswordDTO = new AuthNUserPasswordDTO();
			authNUserPasswordDTO
					.setAppId(String.valueOf(ApplicationContext.map().get(RegistrationConstants.REGISTRATION_CLIENT)));
			authNUserPasswordDTO.setUserName(loginUserDTO.getUserId());
			authNUserPasswordDTO.setPassword(loginUserDTO.getPassword());
			authNRequestDTO.setRequest(authNUserPasswordDTO);
			break;
		case OTP:
			AuthNUserOTPDTO authNUserOTPDTO = new AuthNUserOTPDTO();
			authNUserOTPDTO
					.setAppId(String.valueOf(ApplicationContext.map().get(RegistrationConstants.REGISTRATION_CLIENT)));
			authNUserOTPDTO.setUserId(loginUserDTO.getUserId());
			authNUserOTPDTO.setOtp(loginUserDTO.getOtp());
			authNRequestDTO.setRequest(authNUserOTPDTO);
			break;
		}

		LOGGER.info(LoggerConstants.LOG_SERVICE_DELEGATE_AUTH_DTO, APPLICATION_NAME, APPLICATION_ID,
				"Completed preparing AuthNRequestDTO Based on Login Mode >>> " + loginMode);

		return authNRequestDTO;
	}*/



	private String getEnvironmentProperty(String serviceName, String serviceComponent) {
		return environment.getProperty(serviceName.concat(RegistrationConstants.DOT).concat(serviceComponent));
	}



	private boolean isResponseValid(Map<String, Object> responseMap, String key) {
		return !(null == responseMap || responseMap.isEmpty() || !responseMap.containsKey(key));
	}




	/**
	 * Create a {@link RequestHTTPDTO} for a web-service. Add Cookie to the request
	 * 	  header and URL to request
	 * @param cookie
	 * @param requestURL
	 * @param httpMethod
	 * @return
	 * @throws URISyntaxException
	 */
	/*private RequestHTTPDTO buildRequestHTTPDTO(String cookie, String requestURL, HttpMethod httpMethod)
			throws URISyntaxException {
		RequestHTTPDTO requestHTTPDTO = new RequestHTTPDTO();
		// setting headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Cookie", cookie);
		requestHTTPDTO.setClazz(Object.class);
		requestHTTPDTO.setHttpHeaders(headers);

		requestHTTPDTO.setUri(new URI(requestURL));

		requestHTTPDTO.setHttpMethod(httpMethod);
		requestHTTPDTO.setIsSignRequired(false);
		requestHTTPDTO.setRequestSignRequired(false);

		// set simple client http request
		setTimeout(requestHTTPDTO);

		return requestHTTPDTO;
	}*/
}
