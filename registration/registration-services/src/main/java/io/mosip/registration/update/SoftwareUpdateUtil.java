package io.mosip.registration.update;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class SoftwareUpdateUtil {

    private static final Logger LOGGER = AppConfig.getLogger(SoftwareUpdateUtil.class);
    private static final String CONNECTION_TIMEOUT = "mosip.registration.sw.file.download.connection.timeout";
    private static final String READ_TIMEOUT = "mosip.registration.sw.file.download.read.timeout";
    private static final String libFolder = "lib/";
    private static final String UNKNOWN_JARS = ".UNKNOWN_JARS";
    private static final String TEMP_DIRECTORY = ".TEMP";
    private static final String MANIFEST_FILE_NAME = "MANIFEST.MF";
    private static final String MANIFEST_SIG_FILE_NAME = MANIFEST_FILE_NAME + ".sig";
    public static final String ARTIFACTS_DIRECTORY = ".artifacts";
    // 50s timeout for establishing the connection to the upgrade server.
    private static final int DEFAULT_CONNECTION_TIMEOUT = 50000;
    // 30s inactivity-between-bytes timeout (not a total cap); avoids hanging forever on a stalled connection.
    private static final int DEFAULT_READ_TIMEOUT = 30000;

    protected static boolean deleteUnknownJars(Manifest localManifest) throws IOException {
        StringBuilder builder = new StringBuilder();
        File dir = new File(libFolder);
        Objects.requireNonNull(dir.listFiles(), "No files found in libs");
        File[] libraries = dir.listFiles();
        Map<String, Attributes> entries = localManifest.getEntries();
        for (File file : libraries) {
            // lib/MANIFEST.MF and its detached signature live inside lib/ but are created after
            // the manifest is generated, so they are not self-listed entries. Preserve them
            // instead of treating them as unknown jars (else the lib manifest deletes itself).
            if(MANIFEST_FILE_NAME.equals(file.getName()) || MANIFEST_SIG_FILE_NAME.equals(file.getName())) {
                continue;
            }
            if(!entries.containsKey(file.getName())) {
                LOGGER.error("Unknown file found {}", file.getName());
                deleteFile(file.getCanonicalPath());
                builder.append(file.getName());
                builder.append("\n");
            }
        }

        byte[] bytes =  builder.toString().trim().getBytes(StandardCharsets.UTF_8);
        if(bytes.length > 0) {
            LOGGER.error("Writing the unknown jar names");
            FileUtils.writeByteArrayToFile(new File(UNKNOWN_JARS), bytes);
            return true;
        }
        return false;
    }

    protected static boolean validateJarChecksum(File file, Attributes entryAttributes) {
        try {
            if(entryAttributes != null) {
                String checkSum = HMACUtils2.digestAsPlainText(Files.readAllBytes(file.toPath()));
                String manifestCheckSum = entryAttributes.getValue(Attributes.Name.CONTENT_TYPE);
                return manifestCheckSum.equals(checkSum);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to check the file {} validity", file.getName(), e);
        }
        return false;
    }

    protected static void download(String url, String fileName) throws RegBaseCheckedException {
        LOGGER.info("invoking url : {}", url);
        try {
            File tempDir = new File(TEMP_DIRECTORY);
            if(!tempDir.exists()) { tempDir.mkdirs(); }

            // Via getTimeout, not a local fallback: the old inline default was readTimeout = 0, which
            // HttpURLConnection reads as INFINITE, so an unconfigured client had no read timeout at all
            // and a stalled upgrade server hung the calling thread forever.
            int connectionTimeout = getTimeout(CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
            int readTimeout = getTimeout(READ_TIMEOUT, DEFAULT_READ_TIMEOUT);

            URL fileUrl = new URL(url);
            FileUtils.copyURLToFile(fileUrl, new File(TEMP_DIRECTORY + File.separator + fileName),
                    connectionTimeout, readTimeout);
            return;

        } catch (IOException e) {
            LOGGER.error("Failed to download {}", url, e);
        }
        throw new RegBaseCheckedException("REG-BUILD-005", "Failed to download " + url);
    }

    protected static InputStream download(String url) throws RegBaseCheckedException {
        LOGGER.info("invoking url : {}", url);
        try {
            // See download(String, String): the previous readTimeout = 0 fallback meant no read timeout.
            // This method fetches MANIFEST.MF and MANIFEST.MF.sig during an upgrade, so a server that
            // accepts the connection and then goes silent would stall the upgrade indefinitely.
            int connectionTimeout = getTimeout(CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
            int readTimeout = getTimeout(READ_TIMEOUT, DEFAULT_READ_TIMEOUT);

            final URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            return connection.getInputStream();

        } catch (IOException e) {
            LOGGER.error("Failed to download {}", url, e);
        }
        throw new RegBaseCheckedException("REG-BUILD-005", "Failed to download " + url);
    }

    /**
     * Downloads {@code url} into {@code targetDir/fileName} with resume support.
     * <p>
     * Delegates to {@link ResumableDownloader}, supplying the configured timeouts and translating the
     * low-level {@link IOException} into the service-layer {@link RegBaseCheckedException}. Used by
     * {@code softwareUpdateHandler} to stage upgrade artifacts into {@link #ARTIFACTS_DIRECTORY}.
     *
     * @param url       the source URL
     * @param targetDir the directory the file should be written into (created if missing)
     * @param fileName  the final file name within {@code targetDir}
     * @throws RegBaseCheckedException if the download cannot be completed
     */
    protected static void downloadResumable(String url, String targetDir, String fileName) throws RegBaseCheckedException {
        downloadResumable(url, targetDir, fileName, null);
    }

    /**
     * As {@link #downloadResumable(String, String, String)}, reporting byte-level progress for this
     * artifact so the caller can drive a determinate progress bar. A {@code null} listener disables
     * reporting.
     */
    protected static void downloadResumable(String url, String targetDir, String fileName,
                                            ResumableDownloader.ProgressListener progressListener)
            throws RegBaseCheckedException {
        try {
            int connectTimeout = getTimeout(CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
            int readTimeout = getTimeout(READ_TIMEOUT, DEFAULT_READ_TIMEOUT);
            if (progressListener == null) {
                // Delegate to the progress-free overload rather than allocating another no-op lambda;
                // ResumableDownloader already owns the single NO_PROGRESS instance for this case.
                ResumableDownloader.download(url, targetDir, fileName, connectTimeout, readTimeout);
            } else {
                ResumableDownloader.download(url, targetDir, fileName, connectTimeout, readTimeout,
                        progressListener);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to download {}", url, e);
            throw new RegBaseCheckedException("REG-BUILD-005", "Failed to download " + url, e);
        }
    }

    private static int getTimeout(String key, int defaultValue) {
        try {
            Integer value = ApplicationContext.getIntValueFromApplicationMap(key);
            // A configured 0 is NOT "no override" -- HttpURLConnection reads it as an INFINITE timeout,
            // so a stalled upgrade server would hang the download thread forever, defeating the whole
            // point of the defaults above. Negatives are rejected outright by setReadTimeout. Only a
            // strictly positive value is a usable override; anything else falls back to the default.
            // ResumableDownloader.requirePositiveTimeouts now rejects these outright as well; this
            // keeps a misconfigured client working on the defaults instead of failing the upgrade.
            if (value == null || value <= 0) {
                if (value != null) {
                    LOGGER.warn("Non-positive value {} for {}, using default {}", value, key, defaultValue);
                }
                return defaultValue;
            }
            return value;
        } catch (RuntimeException e) {
            // e.g. a non-numeric configured value -> NumberFormatException; fall back to the default
            LOGGER.warn("Invalid value for {}, using default {} : {}", key, defaultValue, e.getMessage());
            return defaultValue;
        }
    }

    protected static boolean deleteFile(String filePath) {
        LOGGER.info("Deleting file {}", filePath);
        Path path = Path.of(filePath);
        try {
            FileUtils.forceDelete(path.toFile());
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to delete file {}", path, e);
            deleteFileOnExit(filePath);
        }
        return false;
    }

    protected static void deleteFileOnExit(String filePath) {
        LOGGER.info("Deleting file {}", filePath);
        Path path = Path.of(filePath);
        try {
            FileUtils.forceDeleteOnExit(path.toFile());
        } catch (Exception e) {
            LOGGER.error("Failed to delete file on exit {}", path, e);
        }
    }

    protected static void clearTempDirectory() {
        try {
            File dir = new File(TEMP_DIRECTORY);
            if(dir.exists()) {
                FileUtils.cleanDirectory(dir);
                if (dir.delete()) 
                	LOGGER.info("Deleted temp file");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to clean and delete temp", e);
        }
    }
}
