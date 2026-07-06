/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Reads {@code MANIFEST.MF} files and performs the version-comparison and per-file integrity checks
 * the launcher needs (design steps 2 and 6). The per-file hash stored under {@code Content-Type}
 * is validated with {@link HashUtil} so it matches what the build wrote.
 * <p>
 * Pure JDK + slf4j; safe to run under the Java 11 JRE in {@code _launcher.jar}.
 */
public final class ManifestVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestVerifier.class);

    private ManifestVerifier() {
        // utility class
    }

    /** Parses a manifest from raw bytes (e.g. the signature-verified manifest, kept in memory). */
    public static Manifest parse(byte[] manifestBytes) throws IOException {
        try (InputStream in = new ByteArrayInputStream(manifestBytes)) {
            return new Manifest(in);
        }
    }

    /** @return the {@code Manifest-Version} main attribute, or {@code null} if absent. */
    public static String getVersion(Manifest manifest) {
        return manifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION);
    }

    /**
     * @return the {@code Manifest-Version} main attribute of the given manifest file, or {@code null}
     *         if absent.
     * @throws IOException if the manifest cannot be read
     */
    public static String getVersion(File manifestFile) throws IOException {
        return getVersion(read(manifestFile));
    }

    /**
     * Validates every entry listed in {@code manifest} against the corresponding file in
     * {@code baseDir}. Always call with the <b>signature-verified</b> manifest (parsed via
     * {@link #parse(byte[])}) so the check cannot be subverted by a manifest unpacked from the
     * archive being verified — there is deliberately no {@code File}-based overload that would
     * re-read an unverified manifest from disk.
     * <p>
     * Each entry name is resolved as a {@code baseDir}-relative path, so the check honours whatever
     * layout the signed manifest declares (flat jars at the root, or nested under sub-directories
     * keyed with {@code '/'}). An entry whose name escapes the update root — a {@code ../} sequence or
     * an absolute path — is rejected (reported as mismatched) <b>before</b> any file is read, so a hash
     * is never computed over a file outside {@code baseDir}. A manifest whose entry names disagree with
     * the extracted layout likewise fails closed (the file is reported missing).
     *
     * @return the list of entry names that are missing, escape the root, or whose hash does not match
     *         (empty when all entries verify)
     */
    public static List<String> findMismatchedFiles(Manifest manifest, File baseDir) throws IOException {
        List<String> mismatched = new ArrayList<>();
        Path root = baseDir.toPath().toAbsolutePath().normalize();
        for (Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
            String name = entry.getKey();
            String expectedHash = entry.getValue().getValue(Attributes.Name.CONTENT_TYPE);
            Path resolved = root.resolve(name).normalize();
            // Containment guard: reject anything that escapes the update root (../ or absolute) and
            // anything that is not a regular file — fail closed without hashing a file outside baseDir.
            if (!resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
                LOGGER.warn("Manifest entry {} is missing or resolves outside {} — rejecting", name, baseDir);
                mismatched.add(name);
                continue;
            }
            if (expectedHash == null || !expectedHash.equals(HashUtil.sha256Hex(resolved.toFile()))) {
                LOGGER.warn("Hash mismatch for {}", name);
                mismatched.add(name);
            }
        }
        return mismatched;
    }

    /**
     * Returns the {@code baseDir}-relative paths (with {@code '/'} separators) of files <b>not</b>
     * listed in {@code manifest} and not in {@code ignore}. The signed manifest is treated as an
     * allowlist: anything extracted that the manifest does not enumerate is rejected, so a tampered
     * archive cannot smuggle in extra files.
     * <p>
     * The walk is <b>recursive</b> and compares each file's {@code baseDir}-relative path (with
     * {@code '/'} separators) against the manifest's entry-name set. Any extracted file whose relative
     * path is not an enumerated entry (and not in {@code ignore}) is reported — so a tampered archive
     * cannot hide an extra file under a nested directory where a top-level-only scan would miss it.
     *
     * @param ignore control files legitimately present in {@code baseDir} but not manifest entries
     *               (e.g. {@code MANIFEST.MF}, {@code MANIFEST.MF.sig}, {@code lib.zip})
     */
    public static List<String> findUnexpectedFiles(Manifest manifest, File baseDir, Set<String> ignore) {
        List<String> unexpected = new ArrayList<>();
        collectUnexpected(baseDir, baseDir, manifest.getEntries().keySet(), ignore, unexpected);
        return unexpected;
    }

    private static void collectUnexpected(File baseDir, File current, Set<String> entries,
                                          Set<String> ignore, List<String> unexpected) {
        File[] files = current.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collectUnexpected(baseDir, file, entries, ignore, unexpected);
                continue;
            }
            String relative = baseDir.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
            if (ignore.contains(relative) || entries.contains(relative)) {
                continue;
            }
            LOGGER.warn("Unexpected file not listed in manifest: {}", relative);
            unexpected.add(relative);
        }
    }

    /** @return {@code true} if {@code manifest} lists {@code entryName}. */
    public static boolean hasEntry(Manifest manifest, String entryName) {
        return manifest.getAttributes(entryName) != null;
    }

    /**
     * @return {@code true} only if {@code manifest} lists {@code entryName} with a {@code Content-Type}
     *         hash that matches {@code file}'s SHA-256.
     */
    public static boolean fileMatches(Manifest manifest, String entryName, File file) throws IOException {
        Attributes attrs = manifest.getAttributes(entryName);
        if (attrs == null) {
            return false;
        }
        String expectedHash = attrs.getValue(Attributes.Name.CONTENT_TYPE);
        return expectedHash != null && file.exists() && expectedHash.equals(HashUtil.sha256Hex(file));
    }

    private static Manifest read(File manifestFile) throws IOException {
        try (InputStream in = Files.newInputStream(manifestFile.toPath())) {
            return new Manifest(in);
        }
    }
}
