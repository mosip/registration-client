package io.mosip.registration.validator;

import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.exception.RegBaseCheckedException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ClientSetupValidator {

    private static final Logger logger = LoggerFactory.getLogger(ClientSetupValidator.class);

    private static final String PROPERTIES_FILE = "props/mosip-application.properties";
    private static final String manifestFile = "MANIFEST.MF";
    private static final String libFolder = "lib/";
    private static final String SLASH = "/";

    private static String serverRegClientURL = null;
    private static String latestVersion = null;
    private static Manifest localManifest = null;
    private static Manifest serverManifest = null;

    private static String environment = null;
    private static boolean validation_failed = false;
    private static Stack<String> messages = new Stack<>();


    public ClientSetupValidator() throws RegBaseCheckedException {
        try (InputStream keyStream = getClass().getResourceAsStream(PROPERTIES_FILE)) {
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
            deleteUnknownJars();

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
        ExecutorService executorService = null;
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

            deleteUnknownJars();

            //executorService = Executors.newFixedThreadPool(5);
            Map<String, Attributes> localAttributes = localManifest.getEntries();
            for (Map.Entry<String, Attributes> entry : localAttributes.entrySet()) {
                //executorService.execute(new Runnable() {
                //    @Override
                //    public void run() {
                        File file = new File(libFolder + SLASH + entry.getKey());
                        if(!file.exists() || !validateJarChecksum(file, entry.getValue())) {
                            logger.info("{} file checksum validation failed, downloading it", entry.getKey());
                            String url = serverRegClientURL + latestVersion + SLASH + libFolder + entry.getKey();
                            try {
                                Files.copy(download(url), file.toPath());
                            } catch (IOException | RegBaseCheckedException e) {
                                logger.error("Failed to download {}", url, e);
                                validation_failed = true;
                            }
                        }
                //    }
                //});
            }
        } catch (IOException e) {
            logger.error("Failed to validate build setup", e);
            if(executorService != null) { shutdownAndAwaitTermination(executorService); }
            throw new RegBaseCheckedException("REG-BUILD-002", "Failed to check client setup validation");
        }
    }

    public boolean isValidationFailed() {
        return validation_failed;
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
        try {
            serverManifest = new Manifest(download(url));
        } catch (IOException | RegBaseCheckedException e) {
            logger.error("Failed to load server manifest file", e);
        }
    }

    private boolean validateJarChecksum(File file, Attributes entryAttributes) {
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

    private void deleteUnknownJars() throws IOException {
        File dir = new File(libFolder);
        Objects.requireNonNull(dir.listFiles(), "No files found in libs");
        File[] libraries = dir.listFiles();
        Map<String, Attributes> entries = localManifest.getEntries();
        for (File file : libraries) {
            if(!entries.containsKey(file.getName())) {
                FileUtils.forceDelete(file);
                messages.push("Unknown file deleted : " + file.getName());
                logger.error("Unknown file found {}, removed it", file.getName());
            }
        }
    }

    private InputStream download(String url) throws RegBaseCheckedException {
        logger.info("invoking url : {}", url);
        try {
            messages.push("Downloading : " + url);
            URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(50000);
            if (hasSpace(connection.getContentLength())) { // Space Check
                return connection.getInputStream();
            }
        } catch (IOException e) {
            logger.error("Failed to download {}", url, e);
        }
        throw new RegBaseCheckedException("REG-BUILD-005", "Failed to download " + url);
    }

    private boolean hasSpace(int bytes) throws RegBaseCheckedException {
        boolean hasSpace = bytes < new File(File.separator).getFreeSpace();
        if(!hasSpace)
            throw new RegBaseCheckedException("REG-BUILD-004", "Not enough space available");
        return true;
    }

    //From https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ExecutorService.html
    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
