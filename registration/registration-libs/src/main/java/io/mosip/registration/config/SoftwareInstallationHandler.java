package io.mosip.registration.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import io.mosip.kernel.core.util.HMACUtils2;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.util.LoggerFactory;

/**
 * Update the Application
 * 
 * @author Anusha
 *
 */
@Component
public class SoftwareInstallationHandler {


	private static final Logger logger = LoggerFactory.getLogger(SoftwareInstallationHandler.class);
	private static final String CONNECTION_TIMEOUT = "mosip.registration.sw.file.download.connection.timeout";
	private static final String READ_TIMEOUT = "mosip.registration.sw.file.download.read.timeout";
	private static final String PROPERTIES_FILE = "props/mosip-application.properties";
	private static final String manifestFile = "MANIFEST.MF";
	private static final String libFolder = "lib";
	private static final String SLASH = "/";

	private static String serverRegClientURL = null;
	private static String latestVersion = null;
	private static Manifest localManifest = null;
	private static Manifest serverManifest = null;

	private static String environment = null;
	private static boolean validation_failed = false;

	private static boolean patch_downloaded = false;
	private static boolean unknown_jars_found = false;

	private static String connectionTimeout;
	private static String readTimeout;


	public SoftwareInstallationHandler() throws Exception {
		try (InputStream keyStream = SoftwareInstallationHandler.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
			Properties properties = new Properties();
			properties.load(keyStream);
			logger.info("Loading {} completed", PROPERTIES_FILE);

			serverRegClientURL = properties.getProperty("mosip.reg.client.url");
			latestVersion = properties.getProperty("mosip.reg.version");
			environment = properties.getProperty("environment");
			connectionTimeout = properties.getProperty(CONNECTION_TIMEOUT, "50000");
			readTimeout = properties.getProperty(READ_TIMEOUT, "0");
			setLocalManifest();

			Objects.requireNonNull(serverRegClientURL, "'mosip.reg.client.url' IS NOT SET");
			Objects.requireNonNull(latestVersion, "'mosip.reg.version' IS NOT SET");

			//Objects.requireNonNull(localManifest, manifestFile + " - Not found");
			//SoftwareUpdateUtil.deleteUnknownJars(localManifest);

		}catch (IOException e) {
			logger.error("Failed to load {}", PROPERTIES_FILE, e);
			throw new Exception("REG-BUILD-001", e);
		} catch (Throwable t) {
			throw new Exception("REG-BUILD-002", t);
		}
	}


	public void validateBuildSetup() {
		try {
			setServerManifest();

			//When machine is offline / not reachable to server, serverManifest might be null
			String serverVersion = serverManifest == null ? null : serverManifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION);

			//if local manifest is deleted, it may be null
			String localVersion = localManifest == null ? null : localManifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION);

			if(localVersion == null && serverVersion == null) {
				logger.info("Both local and server manifest is not available!");
				validation_failed = true;
				return;
			}

			//only if the version is same then rewrite local manifest with server manifest.
			//if the version is different, then upgrade should handle it, and only checksum validation will be
			//done based on the local manifest file.
			if(localVersion.equals(serverVersion)) {
				serverManifest.write(new FileOutputStream(manifestFile));
				//reset the local manifest, as it's overwritten
				setLocalManifest();
			}

			latestVersion = localManifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION);
			logger.info("Checksum validation started with manifest version : {}", latestVersion);

			deleteUnknownJars(localManifest);

			Map<String, Attributes> localAttributes = localManifest.getEntries();
			for (Map.Entry<String, Attributes> entry : localAttributes.entrySet()) {
				File file = new File(libFolder + File.separator + entry.getKey());
				String url = serverRegClientURL + latestVersion + SLASH + libFolder + SLASH + entry.getKey();
				if(!file.exists()) {
					logger.info("{} file doesn't exists, downloading it", entry.getKey());
					download(url, entry.getKey());
					logger.info("Successfully downloaded the file : {}", entry.getKey());
					patch_downloaded = true;
					continue;
				}

				if(!validateJarChecksum(file, entry.getValue())) {
					logger.info("{} file checksum validation failed, downloading it", entry.getKey());
					download(url, entry.getKey());
					logger.info("Successfully downloaded the latest file : {}", entry.getKey());
					patch_downloaded = true;
				}
			}
		} catch (Throwable e) {
			logger.error("Failed to validate build setup", e);
			validation_failed = true;
		}
		logger.info("Checksum validation completed validation_failed : {}, patch_downloaded : {}", validation_failed,
				patch_downloaded);
	}

	public boolean isValidationFailed() {
		return validation_failed;
	}

	private void setLocalManifest() {
		try {
			File localManifestFile = new File(manifestFile);
			if (localManifestFile.exists()) {
				localManifest = new Manifest(new FileInputStream(localManifestFile));
			}
		} catch (IOException e) {
			logger.error("Failed to load local manifest file", e);
		}
	}

	private void setServerManifest() {
		String url = serverRegClientURL + latestVersion + SLASH + manifestFile;
		try(InputStream in = download(url)) {
			serverManifest = new Manifest(in);
		} catch (Exception e) {
			logger.error("Failed to load server manifest file", e);
		}
	}

	protected static void deleteUnknownJars(Manifest localManifest) throws IOException {
		File dir = new File(libFolder);
		Objects.requireNonNull(dir.listFiles(), "No files found in libs");
		File[] libraries = dir.listFiles();
		Map<String, Attributes> entries = localManifest.getEntries();
		for (File file : libraries) {
			if(!entries.containsKey(file.getName())) {
				logger.error("Unknown file found {}", file.getName());
				deleteFile(file.getCanonicalPath());
			}
		}
	}

	protected static boolean deleteFile(String filePath) {
		logger.info("Deleting file {}", filePath);
		Path path = Path.of(filePath);
		try {
			FileUtils.forceDelete(path.toFile());
			return true;
		} catch (Exception e) {
			logger.error("Failed to delete file {}", path, e);
			deleteFileOnExit(filePath);
		}
		return false;
	}

	protected static void deleteFileOnExit(String filePath) {
		logger.info("Deleting file {}", filePath);
		Path path = Path.of(filePath);
		try {
			FileUtils.forceDeleteOnExit(path.toFile());
		} catch (Exception e) {
			logger.error("Failed to delete file on exit {}", path, e);
		}
	}


	protected static boolean validateJarChecksum(File file, Attributes entryAttributes) {
		try {
			if(entryAttributes != null) {
				String checkSum = HMACUtils2.digestAsPlainText(Files.readAllBytes(file.toPath()));
				String manifestCheckSum = entryAttributes.getValue(Attributes.Name.CONTENT_TYPE);
				return manifestCheckSum.equals(checkSum);
			}
		} catch (Exception e) {
			logger.error("Failed to check the file {} validity", file.getName(), e);
		}
		return false;
	}

	protected static void download(String url, String fileName) throws Exception {
		logger.info("invoking url : {}", url);
		try {
			if(connectionTimeout == null || connectionTimeout.trim().isBlank()) { connectionTimeout = "50000"; }
			if(readTimeout == null || readTimeout.trim().isBlank()) { readTimeout = "0"; }

			URL fileUrl = new URL(url);
			FileUtils.copyURLToFile(fileUrl, new File(libFolder + File.separator + fileName),
					Integer.parseInt(connectionTimeout), Integer.parseInt(readTimeout));
			return;

		} catch (IOException e) {
			logger.error("Failed to download {}", url, e);
			throw new Exception("REG-BUILD-005", e);
		}
	}

	protected static InputStream download(String url) throws Exception {
		logger.info("invoking url : {}", url);
		try {
			if(connectionTimeout == null || connectionTimeout.trim().isBlank()) { connectionTimeout = "50000"; }
			if(readTimeout == null || readTimeout.trim().isBlank()) { readTimeout = "0"; }

			final URLConnection connection = new URL(url).openConnection();
			connection.setConnectTimeout(Integer.parseInt(connectionTimeout));
			connection.setReadTimeout(Integer.parseInt(readTimeout));
			return connection.getInputStream();

		} catch (IOException e) {
			logger.error("Failed to download {}", url, e);
			throw new Exception("REG-BUILD-005", e);
		}
	}
}
