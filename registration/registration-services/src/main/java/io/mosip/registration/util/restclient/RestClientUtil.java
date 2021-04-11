package io.mosip.registration.util.restclient;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.exception.RegBaseCheckedException;

/**
 * This is a general method which gives the response for all httpmethod
 * designators.
 *
 * @author Yaswanth S
 * @since 1.0.0
 */
@Service
public class RestClientUtil {

	/**
	 * Rest Template is a interaction with HTTP servers and enforces RESTful systems
	 */
	private static final Logger LOGGER = AppConfig.getLogger(RestClientUtil.class);



	/**
	 * Access resource using restTemplate {@link RestTemplate}
	 * Note: restTemplate is synchronous client
	 * @param requestHTTPDTO
	 * @return
	 * @throws RestClientException
	 */
	public Map<String, Object> invokeURL(RequestHTTPDTO requestHTTPDTO) throws RestClientException {
		LOGGER.debug("invoke method called {} ", requestHTTPDTO.getUri());

		RestTemplate restTemplate = new RestTemplate(requestHTTPDTO.getSimpleClientHttpRequestFactory());
		Map<String, Object> responseMap = null;

		// TODO need to be removed after checking this properly
		try {
			if (requestHTTPDTO.getUri().toString().contains("https"))
				turnOffSslChecking();
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			LOGGER.error(e.getMessage(), e);
		}

		ResponseEntity<?> responseEntity = restTemplate.exchange(requestHTTPDTO.getUri(), requestHTTPDTO.getHttpMethod(),
				requestHTTPDTO.getHttpEntity(), requestHTTPDTO.getClazz());
		
		if (responseEntity != null && responseEntity.hasBody()) {
			responseMap = new LinkedHashMap<>();
			responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, responseEntity.getBody());
			responseMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, responseEntity.getHeaders());
		}

		LOGGER.debug("invoke method ended {} ", requestHTTPDTO.getUri());
		return responseMap;
	}
	

	public Map<String, Object> invokeForToken(RequestHTTPDTO requestHTTPDTO)
			throws RestClientException {
		requestHTTPDTO.setHttpEntity(new HttpEntity<>(requestHTTPDTO.getRequestBody(), requestHTTPDTO.getHttpHeaders()));
		LOGGER.debug("invokeForToken method called {} ", requestHTTPDTO.getUri());

		RestTemplate restTemplate = new RestTemplate(requestHTTPDTO.getSimpleClientHttpRequestFactory());
		Map<String, Object> responseMap = null;

		// TODO need to be removed after checking this properly
		try {
			if (requestHTTPDTO.getUri().toString().contains("https"))
				turnOffSslChecking();
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			LOGGER.error(e.getMessage(), e);
		}

		ResponseEntity<?> responseEntity = restTemplate.exchange(requestHTTPDTO.getUri(), requestHTTPDTO.getHttpMethod(),
				requestHTTPDTO.getHttpEntity(), requestHTTPDTO.getClazz());

		if (responseEntity != null && responseEntity.hasBody()) {
			responseMap = new LinkedHashMap<>();
			responseMap.put(RegistrationConstants.REST_RESPONSE_BODY, responseEntity.getBody());
			responseMap.put(RegistrationConstants.REST_RESPONSE_HEADERS, responseEntity.getHeaders());
		}

		LOGGER.debug("invokeForToken method ended {} ", requestHTTPDTO.getUri());
		return responseMap;
	}

	/**
	 * Turn off ssl checking.
	 *
	 * @throws NoSuchAlgorithmException 
	 * 				the no such algorithm exception
	 * @throws KeyManagementException 
	 * 				the key management exception
	 */
	public static void turnOffSslChecking() throws NoSuchAlgorithmException, KeyManagementException {
		// Install the all-trusting trust manager
		final SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, UNQUESTIONING_TRUST_MANAGER, null);
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}

	/** The Constant UNQUESTIONING_TRUST_MANAGER. */
	public static final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[] { new X509TrustManager() {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		/* (non-Javadoc)
		 * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], java.lang.String)
		 */
		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1)
				throws java.security.cert.CertificateException {
			// TODO Auto-generated method stub

		}

		/* (non-Javadoc)
		 * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String)
		 */
		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1)
				throws java.security.cert.CertificateException {
			// TODO Auto-generated method stub

		}
	} };

}
