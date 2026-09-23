/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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
                // startsWith is only lexical: if an ancestor under targetRoot is a pre-existing symlink,
                // writing to 'resolved' would follow it outside the extraction root. Reject any symlinked
                // ancestor before creating dirs / opening the output stream.
                rejectSymlinkAncestor(targetRoot, resolved, entry.getName());
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

    /**
     * Rejects writing through a pre-existing symbolic link: walks from {@code resolved} up to (but not
     * past) {@code targetRoot} and fails if any existing path component is a symlink. This closes the
     * gap left by the purely lexical {@code startsWith} check, where {@code sub -> /elsewhere} would let
     * {@code sub/payload.jar} escape the extraction root.
     */
    private static void rejectSymlinkAncestor(Path targetRoot, Path resolved, String entryName) throws IOException {
        for (Path cursor = resolved; cursor != null && cursor.startsWith(targetRoot); cursor = cursor.getParent()) {
            if (cursor.equals(targetRoot)) {
                return;
            }
            if (Files.isSymbolicLink(cursor)) {
                throw new IOException("Blocked zip entry through symbolic link: " + entryName);
            }
        }
    }
}
