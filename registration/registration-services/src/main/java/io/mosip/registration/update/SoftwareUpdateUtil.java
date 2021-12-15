package io.mosip.registration.update;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.exception.RegBaseCheckedException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class SoftwareUpdateUtil {

    private static final Logger LOGGER = AppConfig.getLogger(SoftwareUpdateUtil.class);
    private static final String CONNECTION_TIMEOUT = "mosip.registration.sw.file.download.connection.timeout";
    private static final String libFolder = "lib/";

    protected static void deleteUnknownJars(Manifest localManifest) throws IOException {
        File dir = new File(libFolder);
        Objects.requireNonNull(dir.listFiles(), "No files found in libs");
        File[] libraries = dir.listFiles();
        Map<String, Attributes> entries = localManifest.getEntries();
        for (File file : libraries) {
            if(!entries.containsKey(file.getName())) {
                org.apache.commons.io.FileUtils.forceDelete(file);
                LOGGER.error("Unknown file found {}, removed it", file.getName());
            }
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
            LOGGER.error("Failed to check the file {} validity", file.getName(), e);
        }
        return false;
    }

    protected static InputStream download(String url) throws RegBaseCheckedException {
        LOGGER.info("invoking url : {}", url);
        try {
            Integer connectionTimeout = ApplicationContext.getIntValueFromApplicationMap(CONNECTION_TIMEOUT);
            if(connectionTimeout == null) { connectionTimeout = 50000; }
            URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(connectionTimeout);
            if (hasSpace(connection.getContentLength())) { // Space Check
                return connection.getInputStream();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to download {}", url, e);
        }
        throw new RegBaseCheckedException("REG-BUILD-005", "Failed to download " + url);
    }


    private static boolean hasSpace(int bytes) throws RegBaseCheckedException {
        boolean hasSpace = bytes < new File(File.separator).getFreeSpace();
        if(!hasSpace)
            throw new RegBaseCheckedException("REG-BUILD-004", "Not enough space available");
        return true;
    }
}
