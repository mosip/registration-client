package io.mosip.registration.launcher.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extracts a zip archive ({@code lib.zip} / {@code jre21.zip}) into a target directory.
 * <p>
 * Guards against the "zip slip" path-traversal vulnerability: any entry that would resolve outside
 * the target directory is rejected. Pure JDK so it is safe under the Java 11 JRE.
 */
public final class ZipExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipExtractor.class);
    private static final int BUFFER_SIZE = 8192;

    private ZipExtractor() {
        // utility class
    }

    /**
     * Extracts {@code zipFile} into {@code targetDir} (created if missing). Existing files are
     * overwritten.
     *
     * @throws IOException if reading/writing fails, or an entry escapes {@code targetDir}
     */
    public static void extract(File zipFile, File targetDir) throws IOException {
        Files.createDirectories(targetDir.toPath());
        Path targetRoot = targetDir.toPath().toAbsolutePath().normalize();

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(zipFile.toPath())))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolved = targetRoot.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(targetRoot)) {
                    throw new IOException("Blocked zip entry resolving outside target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    try (OutputStream out = Files.newOutputStream(resolved)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        LOGGER.info("Extracted {} into {}", zipFile.getName(), targetRoot);
    }
}
