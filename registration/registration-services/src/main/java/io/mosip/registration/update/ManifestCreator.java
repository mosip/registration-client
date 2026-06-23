package io.mosip.registration.update;

import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestCreator {

    private static final Logger logger = LoggerFactory.getLogger(ManifestCreator.class);
    private static final String MANIFEST_FILE_NAME = "MANIFEST.MF";
    private static final String LIST_MODE = "--list";

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                throw new IllegalArgumentException(
                        "Usage: [--list <version> <targetPath> <file1> ...] OR <version> <libraryFolderPath> <targetPath>");
            }
            if (LIST_MODE.equals(args[0])) {
                // --list <version> <targetPath> <file1> [<file2> ...]
                // Builds the root MANIFEST.MF over an explicit, tolerant set of orchestration
                // artifacts (jre21.zip, _launcher.jar, migration.exe, rollback.exe, run.bat).
                // Files that do not exist yet (e.g. artifacts produced by other 1.3.0 sub-tasks)
                // are skipped with a warning rather than failing the build.
                if (args.length < 4) {
                    throw new IllegalArgumentException("Usage: --list <version> <targetPath> <file1> [<file2> ...]");
                }
                String version = args[1];
                String targetPath = args[2];
                List<File> files = new ArrayList<>();
                for (String path : Arrays.asList(args).subList(3, args.length)) {
                    files.add(new File(path));
                }
                createFromFiles(version, new File(targetPath, MANIFEST_FILE_NAME), files);
            } else {
                // <version> <libraryFolderPath> <targetPath>
                if (args.length != 3) {
                    throw new IllegalArgumentException("Usage: <version> <libraryFolderPath> <targetPath>");
                }
                String version = args[0];
                String libraryFolderPath = args[1];
                String targetPath = args[2];
                createFromFolder(version, new File(libraryFolderPath), new File(targetPath, MANIFEST_FILE_NAME));
            }
        } catch (Exception e) {
            // Fail loudly with a non-zero exit so configure.sh (set -e) aborts the build instead of
            // continuing to sign/package a stale or missing MANIFEST.MF. Errors (not Exceptions) are
            // left to propagate uncaught — the JVM still exits non-zero, so the build still aborts.
            logger.error("Failed to create the manifest", e);
            System.exit(1);
        }
    }

    /**
     * Builds a manifest containing one entry per file in {@code libFolder} (per-file integrity
     * hashes). Used for {@code lib/MANIFEST.MF}, which is bundled inside {@code lib.zip}.
     */
    static void createFromFolder(String version, File libFolder, File targetFile) throws Exception {
        if (!(libFolder.exists() && libFolder.isDirectory())) {
            // Throw (not return) so main() exits non-zero and configure.sh (set -e) aborts at the real
            // cause, rather than continuing to sign a MANIFEST.MF that was never written.
            throw new IllegalStateException("Library folder does not exist or is not a directory: " + libFolder);
        }
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
        File[] children = libFolder.listFiles();
        if (children == null) {
            throw new IllegalStateException("Unable to list files in " + libFolder.getPath());
        }
        for (File file : children) {
            if (file.isFile()) {
                addEntryInManifest(manifest, file);
            } else {
                logger.warn("Skipping non-regular file in manifest folder: {}", file.getName());
            }
        }
        write(manifest, targetFile);
    }

    /**
     * Builds a manifest over an explicit list of files, tolerating files that are absent (logged and
     * skipped). Used for the root {@code ./MANIFEST.MF}, whose orchestration artifacts may be
     * produced by other 1.3.0 sub-tasks and not present at the time this runs.
     */
    static void createFromFiles(String version, File targetFile, List<File> files) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
        for (File file : files) {
            if (file.exists() && file.isFile()) {
                addEntryInManifest(manifest, file);
            } else {
                logger.warn("Skipping manifest entry, file not present yet: {}", file.getName());
            }
        }
        write(manifest, targetFile);
    }

    private static void write(Manifest manifest, File targetFile) throws Exception {
        try (FileOutputStream out = new FileOutputStream(targetFile)) {
            manifest.write(out);
        }
        logger.info("Created {} ({} entries)", targetFile.getPath(), manifest.getEntries().size());
    }

    private static void addEntryInManifest(Manifest manifest, File file) throws Exception {
        String hashText = HMACUtils2.digestAsPlainText(FileUtils.readFileToByteArray(file));
        Attributes attribute = new Attributes();
        attribute.put(Attributes.Name.CONTENT_TYPE, hashText);
        manifest.getEntries().put(file.getName(), attribute);
    }
}
