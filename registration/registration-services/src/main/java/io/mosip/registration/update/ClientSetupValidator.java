package io.mosip.registration.update;

import io.mosip.registration.exception.RegBaseCheckedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ClientSetupValidator {

    private static final Logger logger = LoggerFactory.getLogger(ClientSetupValidator.class);

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
    private static Stack<String> messages = new Stack<>();


    public ClientSetupValidator() throws RegBaseCheckedException {
        try (InputStream keyStream = ClientSetupValidator.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            Properties properties = new Properties();
            properties.load(keyStream);
            logger.info("Loading {} completed", PROPERTIES_FILE);

            serverRegClientURL = properties.getProperty("mosip.reg.client.url");
            latestVersion = properties.getProperty("mosip.reg.version");
            environment = properties.getProperty("environment");
            setLocalManifest();

            Objects.requireNonNull(serverRegClientURL, "'mosip.reg.client.url' IS NOT SET");
            Objects.requireNonNull(latestVersion, "'mosip.reg.version' IS NOT SET");

            if("LOCAL".equals(environment)) {
                messages.push("IGNORING LOCAL REGISTRATION CLIENT SETUP VALIDATION AS ITS LOCAL ENVIRONMENT");
                return;
            }

            Objects.requireNonNull(localManifest, manifestFile + " - Not found");
            //SoftwareUpdateUtil.deleteUnknownJars(localManifest);

        } catch (RegBaseCheckedException e) {
            throw e;
        } catch (IOException e) {
            logger.error("Failed to load {}", PROPERTIES_FILE, e);
            throw new RegBaseCheckedException("REG-BUILD-001", "Failed to load properties");
        } catch (Throwable t) {
            throw new RegBaseCheckedException("REG-BUILD-002", t.getMessage());
        }
    }


    public void validateBuildSetup() throws RegBaseCheckedException {
        try {

            if("LOCAL".equals(environment)) {
                logger.warn("NOTE :: IGNORING LOCAL REGISTRATION CLIENT SETUP VALIDATION AS ITS LOCAL ENVIRONMENT");
                return;
            }

            setServerManifest();

            //When machine is offline / not reachable to server, serverManifest might be null
            String serverVersion = serverManifest == null ? null : serverManifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION);
            String localVersion = localManifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION);

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

            SoftwareUpdateUtil.clearTempDirectory();

            if(SoftwareUpdateUtil.deleteUnknownJars(localManifest)) {
                logger.info("Found unknown jars in the classpath !");
                unknown_jars_found = true;
                validation_failed = true;
            }

            Map<String, Attributes> localAttributes = localManifest.getEntries();
            for (Map.Entry<String, Attributes> entry : localAttributes.entrySet()) {
                File file = new File(libFolder + File.separator + entry.getKey());
                String url = serverRegClientURL + latestVersion + SLASH + libFolder + SLASH + entry.getKey();
                if(!file.exists()) {
                    logger.info("{} file doesn't exists, downloading it", entry.getKey());
                    SoftwareUpdateUtil.download(url, entry.getKey());
                    logger.info("Successfully downloaded the file : {}", entry.getKey());
                    patch_downloaded = true;
                    continue;
                }

                if(!SoftwareUpdateUtil.validateJarChecksum(file, entry.getValue())) {
                    logger.info("{} file checksum validation failed, downloading it", entry.getKey());
                    SoftwareUpdateUtil.download(url, entry.getKey());
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

    public boolean isPatch_downloaded() {
        return patch_downloaded;
    }

    public boolean isUnknown_jars_found() {
        return unknown_jars_found;
    }

    private void setLocalManifest() throws RegBaseCheckedException {
        try {
            File localManifestFile = new File(manifestFile);
            if (localManifestFile.exists()) {
                localManifest = new Manifest(new FileInputStream(localManifestFile));
            }
        } catch (IOException e) {
            logger.error("Failed to load local manifest file", e);
            throw new RegBaseCheckedException("REG-BUILD-003", "Local Manifest not found");
        }
    }

    private void setServerManifest() {
        String url = serverRegClientURL + latestVersion + SLASH + manifestFile;
        try(InputStream in = SoftwareUpdateUtil.download(url)) {
            serverManifest = new Manifest(in);
        } catch (IOException | RegBaseCheckedException e) {
            logger.error("Failed to load server manifest file", e);
        }
    }
}
