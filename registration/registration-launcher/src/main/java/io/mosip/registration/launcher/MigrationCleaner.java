package io.mosip.registration.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

    private static boolean deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        boolean deleted = file.delete();
        if (!deleted) {
            LOGGER.warn("Could not delete {}", file);
        }
        return deleted;
    }
}
