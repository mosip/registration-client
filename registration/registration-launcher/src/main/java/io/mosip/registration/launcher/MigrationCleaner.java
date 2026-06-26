package io.mosip.registration.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static io.mosip.registration.launcher.MigrationArtifacts.DIR_ARTIFACTS;
import static io.mosip.registration.launcher.MigrationArtifacts.DIR_JRE21_TEMP;
import static io.mosip.registration.launcher.MigrationArtifacts.FILE_MIGRATION_EXE;
import static io.mosip.registration.launcher.MigrationArtifacts.FILE_ROLLBACK_EXE;
import static io.mosip.registration.launcher.MigrationArtifacts.FILE_RUN_BAT_BACKUP;

/**
 * Step 6 cleanup (design doc): when the manifest versions match, any migration artifacts left over
 * from a completed prior migration are removed before normal startup. Best-effort — failures are
 * logged but never block startup.
 */
public final class MigrationCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationCleaner.class);

    private static final String[] ARTIFACT_DIRS = {DIR_JRE21_TEMP, DIR_ARTIFACTS};
    private static final String[] ARTIFACT_FILES = {FILE_RUN_BAT_BACKUP, FILE_MIGRATION_EXE, FILE_ROLLBACK_EXE};

    private MigrationCleaner() {
        // utility class
    }

    /**
     * Removes the known migration artifacts under {@code baseDir} if present.
     *
     * @param baseDir the application root
     * @return the names of artifacts that were present and successfully removed
     */
    public static List<String> cleanup(File baseDir) {
        List<String> removed = new ArrayList<>();
        for (String name : ARTIFACT_DIRS) {
            File dir = new File(baseDir, name);
            if (dir.exists() && deleteRecursively(dir)) {
                removed.add(name);
            }
        }
        for (String name : ARTIFACT_FILES) {
            File file = new File(baseDir, name);
            if (file.exists()) {
                if (file.delete()) {
                    removed.add(name);
                } else {
                    LOGGER.warn("Could not delete migration artifact {}", file);
                }
            }
        }
        if (!removed.isEmpty()) {
            LOGGER.info("Cleaned up migration artifacts: {}", removed);
        }
        return removed;
    }

    /**
     * Deletes {@code file} and its contents <b>without following symlinks</b>. A symlink encountered
     * inside a migration artifact dir is removed as a link (its target is left alone) — using the old
     * {@code File.listFiles()} recursion would descend through the link and delete files outside the
     * artifact directory on every normal startup.
     */
    private static boolean deleteRecursively(File file) {
        try {
            // walkFileTree does not follow symlinks unless FOLLOW_LINKS is passed, so a symlink is
            // visited as a file (link deleted, target untouched).
            Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException e) {
            LOGGER.warn("Could not delete {}", file, e);
            return false;
        }
    }
}
