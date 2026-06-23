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
            if (args.length > 0 && LIST_MODE.equals(args[0])) {
                // --list <version> <targetPath> <file1> [<file2> ...]
                // Builds the root MANIFEST.MF over an explicit, tolerant set of orchestration
                // artifacts (jre21.zip, _launcher.jar, migration.exe, rollback.exe, run.bat).
                // Files that do not exist yet (e.g. artifacts produced by other 1.3.0 sub-tasks)
                // are skipped with a warning rather than failing the build.
                String version = args[1];
                String targetPath = args[2];
                List<File> files = new ArrayList<>();
                for (String path : Arrays.asList(args).subList(3, args.length)) {
                    files.add(new File(path));
                }
                createFromFiles(version, new File(targetPath, MANIFEST_FILE_NAME), files);
            } else {
                // <version> <libraryFolderPath> <targetPath>
                String version = args[0];
                String libraryFolderPath = args[1];
                String targetPath = args[2];
                createFromFolder(version, new File(libraryFolderPath), new File(targetPath, MANIFEST_FILE_NAME));
            }
        } catch (Throwable e) {
            logger.error("Failed to create the manifest", e);
        }
    }

    /**
     * Builds a manifest containing one entry per file in {@code libFolder} (per-file integrity
     * hashes). Used for {@code lib/MANIFEST.MF}, which is bundled inside {@code lib.zip}.
     */
    static void createFromFolder(String version, File libFolder, File targetFile) throws Exception {
        if (!(libFolder.exists() && libFolder.isDirectory())) {
            logger.error("Library folder does not exist or is not a directory: {}", libFolder);
            return;
        }
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
        for (File file : libFolder.listFiles()) {
            addEntryInManifest(manifest, file);
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
