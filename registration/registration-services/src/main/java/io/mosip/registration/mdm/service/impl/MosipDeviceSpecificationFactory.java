
package io.mosip.registration.mdm.service.impl;

import static io.mosip.registration.constants.LoggerConstants.MOSIP_BIO_DEVICE_INTEGERATOR;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.constants.MosipBioDeviceConstants;
import io.mosip.registration.mdm.dto.Biometric;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.integrator.MosipDeviceSpecificationProvider;
import io.mosip.registration.service.BaseService;

/**
 * 
 * Handles all the Biometric Devices controls
 * 
 * @author balamurugan.ramamoorthy
 * 
 */
@Component
public class MosipDeviceSpecificationFactory {

	private static final Logger LOGGER = AppConfig.getLogger(MosipDeviceSpecificationFactory.class);
	private static final String loggerClassName = "MosipDeviceSpecificationFactory";

	@Value("${mosip.registration.mdm.default.portRangeFrom}")
	private int defaultMDSPortFrom;

	@Value("${mosip.registration.mdm.default.portRangeTo}")
	private int defaultMDSPortTo;

	@Value("${mosip.registration.mdm.threadpool.size:5}")
	private int threadPoolSize;

	@Value("${mosip.registration.mdm.connection.timeout:5}")
	private int connectionTimeout;

	@Autowired
	private List<MosipDeviceSpecificationProvider> deviceSpecificationProviders;

	@Autowired
	private MosipDeviceSpecificationHelper mosipDeviceSpecificationHelper;

	@Autowired
	private BaseService baseService;

	private int portFrom;
	private int portTo;

	/** Key is modality value is (specVersion, MdmBioDevice) */
	private static Map<String, MdmBioDevice> selectedDeviceInfoMap = new LinkedHashMap<>();

	private static Map<String, List<MdmBioDevice>> availableDeviceInfoMap = new LinkedHashMap<>();

	/**
	 * This method will prepare the device registry, device registry contains all
	 * the running biometric devices
	 * <p>
	 * In order to prepare device registry it will loop through the specified ports
	 * and identify on which port any particular biometric device is running
	 * </p>
	 * 
	 * Looks for all the configured ports available and initializes all the
	 * Biometric devices and saves it for future access
	 * 
	 * @throws RegBaseCheckedException - generalised exception with errorCode and
	 *                                 errorMessage
	 */
	public void initializeDeviceMap(boolean async) {
		LOGGER.debug("Entering initializeDeviceMap method for preparing device registry");

		if(baseService.isInitialSync()) {
			LOGGER.warn("DO nothing as it is still initial launch");
			return;
		}

		portFrom = getPortFrom();
		portTo = getPortTo();

		availableDeviceInfoMap.clear();
		selectedDeviceInfoMap.clear();

		LOGGER.info("Checking device info from port : {} to port : {} with thread pool size : {}", portFrom, portTo, threadPoolSize);

		if (portFrom >= 4500 && portTo <= 4600) {
			ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);
			for (int port = portFrom; port <= portTo; port++) {
				int currentPort = port;
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						try {
							initByPort(currentPort);
						} catch (RuntimeException exception) {
							LOGGER.error("Exception while mapping the response : ", exception);
						}
					}
				};

				if(async) { threadPool.submit(runnable); }
				else { threadPool.execute(runnable); }

			}
			if(!async) { awaitTerminationAfterShutdown(threadPool); }
		}

		LOGGER.debug("Exit initializeDeviceMap method for preparing device registry");
	}

	public Map<String, List<MdmBioDevice>> getAvailableDeviceInfoMap() {
		return availableDeviceInfoMap;
	}

	public int getPortTo() {
		try {
			Integer port = ApplicationContext.getIntValueFromApplicationMap(RegistrationConstants.MDM_END_PORT_RANGE);
			if(port != null) { return port; }
		} catch (RuntimeException runtimeException) {
			LOGGER.error("Exception while parsing  MDM_END_PORT_RANGE", runtimeException);
		}
		LOGGER.error("initializing default value for MDM_END_PORT_RANGE: {}", defaultMDSPortTo);
		return defaultMDSPortTo;
	}

	public int getPortFrom() {
		try {
			Integer port = ApplicationContext.getIntValueFromApplicationMap(RegistrationConstants.MDM_START_PORT_RANGE);
			if(port != null) { return port; }
		} catch (RuntimeException runtimeException) {
			LOGGER.error("Exception while parsing  MDM_START_PORT_RANGE", runtimeException);
		}
		LOGGER.error("initializing default value for MDM_START_PORT_RANGE: {}", defaultMDSPortFrom);
		return defaultMDSPortFrom;
	}

	/*
	 * Testing the network with method
	 */
	public static boolean checkServiceAvailability(String serviceUrl, String method) {
		int timeout = MosipDeviceSpecificationHelper.getMDMConnectionTimeout(method);
		LOGGER.debug("checkServiceAvailability serviceUrl : {}  with timeout {}",serviceUrl, timeout);
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(timeout)
				.setSocketTimeout(timeout)
				.setConnectionRequestTimeout(timeout)
				.build();
		HttpUriRequest request = RequestBuilder.create(method)
				.setUri(serviceUrl)
				.setConfig(requestConfig)
				.build();

		try (CloseableHttpClient client = HttpClients.createDefault()) {
			client.execute(request);
		} catch (Exception exception) {
			return false;
		}
		return true;

	}

	private void initByPort(Integer availablePort) {
		LOGGER.debug("Checking device on port : {}", availablePort);
		String url = mosipDeviceSpecificationHelper.buildUrl(availablePort,
				MosipBioDeviceConstants.DEVICE_INFO_ENDPOINT);

		if (!checkServiceAvailability(url, "MOSIPDINFO")) {
			LOGGER.info("No device is running at port number {}", availablePort);
			return;
		}

		try {
			String deviceInfoResponse = getDeviceInfoResponse(url);
			for (MosipDeviceSpecificationProvider deviceSpecificationProvider : deviceSpecificationProviders) {
				LOGGER.debug("Decoding deice info response with provider : {}", deviceSpecificationProvider);
				List<MdmBioDevice> mdmBioDevices = deviceSpecificationProvider.getMdmDevices(deviceInfoResponse,
						availablePort);
				for (MdmBioDevice bioDevice : mdmBioDevices) {
					String deviceType = getDeviceType(bioDevice.getDeviceType());
					String deviceSubType = getDeviceSubType(bioDevice.getDeviceSubType());
					if (bioDevice != null && deviceType != null && deviceSubType != null) {
						// Add to Device Info Map
						addToDeviceInfoMap(getKey(deviceType, deviceSubType), bioDevice);
					}
				}
			}
		} catch (RuntimeException runtimeException) {
			LOGGER.error("Failed to parse / validate MDM response", runtimeException);
		}
	}

	private void addToDeviceInfoMap(String key, MdmBioDevice bioDevice) {
		selectedDeviceInfoMap.put(key, bioDevice);
		
		if (!key.contains("single")) {
			if (availableDeviceInfoMap.containsKey(key)) {
				availableDeviceInfoMap.get(key).add(bioDevice);
			} else {
				List<MdmBioDevice> bioDevices = new ArrayList<>();
				bioDevices.add(bioDevice);
				availableDeviceInfoMap.put(key, bioDevices);
			}
		}
		
		LOGGER.debug(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Added for device into cache : " + bioDevice.getDeviceCode());
	}

	private String getKey(String type, String subType) {
		return String.format("%s_%s", type.toLowerCase(), subType.toLowerCase());
	}

	private String getDeviceType(String type) {

		type = type.toLowerCase();

		if (type.contains("finger") || type.contains("fir")) {
			return SingleType.FINGER.value();
		}
		if (type.contains("iris") || type.contains("iir")) {
			return SingleType.IRIS.value();
		}
		if (type.contains("face")) {
			return SingleType.FACE.value();
		}
		return null;
	}

	private String getDeviceSubType(String subType) {

		subType = subType.toLowerCase();

		if (subType.contains("slab") || subType.contains("slap")) {
			return "slab";
		}
		if (subType.contains("single")) {
			return "single";
		}
		if (subType.contains("double")) {
			return "double";
		}
		if (subType.contains("face")) {
			return "face";
		}
		return null;
	}

	private String getDeviceInfoResponse(String url) {
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(connectionTimeout * 1000)
				.setSocketTimeout(connectionTimeout * 1000)
				.setConnectionRequestTimeout(connectionTimeout * 1000)
				.build();
		HttpUriRequest request = RequestBuilder.create("MOSIPDINFO")
				.setUri(url)
				.setConfig(requestConfig)
				.build();
		
		CloseableHttpResponse clientResponse = null;
		String response = null;

		try (CloseableHttpClient client = HttpClients.createDefault()) {
			clientResponse = client.execute(request);
			response = EntityUtils.toString(clientResponse.getEntity());
		} catch (IOException exception) {
			LOGGER.error(MOSIP_BIO_DEVICE_INTEGERATOR, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
		}
		return response;
	}

	public String getLatestSpecVersion(String[] specVersion) {

		String latestSpecVersion = null;
		if (specVersion != null && specVersion.length > 0) {
			latestSpecVersion = specVersion[0];
			for (int index = 1; index < specVersion.length; index++) {

				latestSpecVersion = getLatestVersion(latestSpecVersion, specVersion[index]);
			}

			if (getMdsProvider(deviceSpecificationProviders, latestSpecVersion) == null) {
				List<String> specVersions = Arrays.asList(specVersion);
				    specVersions.remove(latestSpecVersion);    //NOSONAR removing latestSpecVersion here.
				if (!specVersions.isEmpty()) {
					latestSpecVersion = getLatestSpecVersion(specVersions.toArray(new String[0]));
				}
			}

		}

		return latestSpecVersion;
	}

	public MdmBioDevice getDeviceInfoByModality(String modality) throws RegBaseCheckedException {
		String deviceType = getDeviceType(modality);
		String deviceSubType = getDeviceSubType(modality);
		
		if (deviceType != null && deviceSubType != null) {
			String key = getKey(deviceType, deviceSubType);
			if (key != null && selectedDeviceInfoMap.containsKey(key))
				return selectedDeviceInfoMap.get(key);

				initializeDeviceMap(true);

			if (key != null && selectedDeviceInfoMap.containsKey(key))
				return selectedDeviceInfoMap.get(key);
		}

		LOGGER.info("Bio Device not found for modality : {} at {}", modality, System.currentTimeMillis());
		throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
				RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());
	}

	public boolean isDeviceAvailable(String modality) throws RegBaseCheckedException {

		return isDeviceAvailable(getDeviceInfoByModality(modality));

	}

	public boolean isDeviceAvailable(MdmBioDevice bioDevice) throws RegBaseCheckedException {

		if (bioDevice == null) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
					RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());
		}

		Optional<MosipDeviceSpecificationProvider> result = deviceSpecificationProviders.stream()
				.filter(provider -> provider.getSpecVersion().equalsIgnoreCase(bioDevice.getSpecVersion())
						&& provider.isDeviceAvailable(bioDevice))
				.findFirst();
		
		String deviceType = getDeviceType(bioDevice.getDeviceType());
		String deviceSubType = getDeviceSubType(bioDevice.getDeviceSubType());
		
		if (deviceType == null || deviceSubType == null) {
			LOGGER.error("DeviceType or DeviceSubType found null");
			return false;
		}
			
		String key = getKey(deviceType, deviceSubType);
		if (result.isPresent()) {
			return true;
		}
		selectedDeviceInfoMap.remove(key);
		initializeDeviceMap(true);
		return false;
	}

	private String getLatestVersion(String version1, String version2) {

		if (version1.equalsIgnoreCase(version2)) {
			return version1;
		}
		int version1Num = 0, version2Num = 0;

		for (int index = 0, limit = 0; (index < version1.length() || limit < version2.length());) {

			while (index < version1.length() && version1.charAt(index) != '.') {
				version1Num = version1Num * 10 + (version1.charAt(index) - '0');
				index++;
			}

			while (limit < version2.length() && version2.charAt(limit) != '.') {
				version2Num = version2Num * 10 + (version2.charAt(limit) - '0');
				limit++;
			}

			if (version1Num > version2Num)
				return version1;
			if (version2Num > version1Num)
				return version2;

			version1Num = version2Num = 0;
			index++;
			limit++;
		}
		return version1;
	}

	public String getLatestSpecVersion() {

		return getLatestSpecVersion(Biometric.getAvailableSpecVersions().toArray(new String[0]));

	}

	public String getSpecVersionByModality(String modality) throws RegBaseCheckedException {

		MdmBioDevice bioDevice = getDeviceInfoByModality(modality);

		if (bioDevice != null) {
			return bioDevice.getSpecVersion();
		}
		return null;

	}

	public MosipDeviceSpecificationProvider getMdsProvider(String specVersion) throws RegBaseCheckedException {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Finding MosipDeviceSpecificationProvider for spec version : " + specVersion);

		MosipDeviceSpecificationProvider deviceSpecificationProvider = getMdsProvider(deviceSpecificationProviders,
				specVersion);

		if (deviceSpecificationProvider == null) {
			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"MosipDeviceSpecificationProvider not found for spec version : " + specVersion);
			throw new RegBaseCheckedException(RegistrationExceptionConstants.MDS_PROVIDER_NOT_FOUND.getErrorCode(),
					RegistrationExceptionConstants.MDS_PROVIDER_NOT_FOUND.getErrorMessage());
		}
		return deviceSpecificationProvider;
	}

	private MosipDeviceSpecificationProvider getMdsProvider(
			List<MosipDeviceSpecificationProvider> deviceSpecificationProviders, String specVersion) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Finding MosipDeviceSpecificationProvider for spec version : " + specVersion + " in providers : "
						+ deviceSpecificationProviders);

		MosipDeviceSpecificationProvider deviceSpecificationProvider = null;

		if (deviceSpecificationProviders != null) {
			// Get Implemented provider
			for (MosipDeviceSpecificationProvider provider : deviceSpecificationProviders) {
				if (provider.getSpecVersion().equals(specVersion)) {
					deviceSpecificationProvider = provider;
					break;
				}
			}
		}
		return deviceSpecificationProvider;
	}

	public static Map<String, MdmBioDevice> getDeviceRegistryInfo() {
		return selectedDeviceInfoMap;
	}

	public static Map<String, List<MdmBioDevice>> getAvailableDeviceInfo() {
		return availableDeviceInfoMap;
	}
	
	public void modifySelectedDeviceInfo(String key, String serialNumber) {
		Optional<MdmBioDevice> bioDevice = availableDeviceInfoMap.get(key).stream()
				.filter(device -> device.getSerialNumber().equalsIgnoreCase(serialNumber)).findAny();
		if(bioDevice.isPresent()) {
			selectedDeviceInfoMap.put(key, bioDevice.get());
		}
	}

	public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
		threadPool.shutdown();
		LOGGER.info("Waiting for the termination of biometric device search threads...");
		try {
			if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
				LOGGER.error("Shutting down registry initialize executor service as timeout elapsed");
				threadPool.shutdownNow();
			}
		} catch (InterruptedException ex) {
			threadPool.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

}
