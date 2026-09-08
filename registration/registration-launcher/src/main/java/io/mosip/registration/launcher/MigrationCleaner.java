/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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
import static io.mosip.registration.launcher.MigrationArtifacts.DIR_JRE21_TEMP_PARTIAL;
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

    // jre21_temp.partial/ is listed too: a run interrupted mid-unzip leaves one behind, and nothing
    // else reclaims it once the migration has completed -- it would sit there as ~200MB of dead
    // bytes on every client that ever hit an interrupted extraction.
    private static final String[] ARTIFACT_DIRS = {DIR_JRE21_TEMP, DIR_JRE21_TEMP_PARTIAL, DIR_ARTIFACTS};
    private static final String[] ARTIFACT_FILES = {FILE_RUN_BAT_BACKUP, FILE_MIGRATION_EXE, FILE_ROLLBACK_EXE};

    /** How long an interrupted download's partials are kept before being treated as abandoned. */
    private static final long RESUMABLE_RETENTION_MS = 30L * 24 * 60 * 60 * 1000;

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
            if (!dir.exists()) {
                continue;
            }
            if (DIR_ARTIFACTS.equals(name) && hasDownloadInProgress(dir)) {
                // Versions match, but .artifacts/ holds .part files from an upgrade whose download was
                // interrupted. The handler adopts the new ./MANIFEST.MF only after every artifact is
                // downloaded, so an interrupted attempt legitimately leaves the versions matching -- and
                // deleting the partials here would silently force the operator to re-fetch the whole
                // ~200MB JRE instead of resuming it.
                LOGGER.info("Keeping {} - it holds an in-progress resumable download", name);
                continue;
            }
            if (deleteRecursively(dir)) {
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
     * True when {@code .artifacts/} contains a resumable download's sidecar files, i.e. an upgrade was
     * started and interrupted rather than completed.
     */
    private static boolean hasDownloadInProgress(File artifactsDir) {
        File[] files = artifactsDir.listFiles();
        if (files == null) {
            return false;
        }
        long staleBefore = System.currentTimeMillis() - RESUMABLE_RETENTION_MS;
        boolean sawStalePartial = false;
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".part")) {
                // Bound the retention. An upgrade the operator started and abandoned would otherwise pin
                // a partial jre21.zip (up to ~200MB) on disk forever on an otherwise healthy client,
                // because a matched-version startup is exactly the state this guard protects. Past the
                // window the partial is treated as abandoned and cleaned with the rest; a later retry
                // simply downloads afresh.
                if (file.lastModified() >= staleBefore) {
                    return true;
                }
                sawStalePartial = true;
            }
        }
        // Logged only once the loop has actually concluded the directory is abandoned. Reporting it per
        // stale file would contradict the "Keeping .artifacts" line whenever a later partial is fresh.
        if (sawStalePartial) {
            LOGGER.info("Only stale partials in .artifacts/ (older than the retention window) - "
                    + "treating it as abandoned");
        }
        return false;
    }

    /**
     * Deletes {@code file} and its contents <b>without following symlinks</b>, returning {@code true}
     * if the tree is gone (a non-existent path counts as failure -- callers that tolerate absence should
     * check first). Shared with {@link JreMigrationStager}, which discards interrupted extractions.
     * <p>
     * A symlink encountered
     * inside a migration artifact dir is removed as a link (its target is left alone) — using the old
     * {@code File.listFiles()} recursion would descend through the link and delete files outside the
     * artifact directory on every normal startup.
     */
    static boolean deleteRecursively(File file) {
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
