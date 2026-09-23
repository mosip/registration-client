/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

/**
 * Single source of truth for the on-disk names of the JRE-migration artifacts and directories.
 * <p>
 * Shared by {@link JreMigrationStager} (which creates/stages them) and {@link MigrationCleaner}
 * (which removes them after a completed migration) so the writer and the cleaner can never drift:
 * before this existed, the cleaner re-spelled the same names as independent string literals, and a
 * rename in one place silently left stale artifacts on disk (or cleaned the wrong file). Which names
 * are transitioned / integrity-verified / cleaned is still a per-role decision in each class, but the
 * names themselves are defined exactly once here.
 */
public final class MigrationArtifacts {

    private MigrationArtifacts() {
        // constants holder
    }

    // directories (relative to the application root)
    public static final String DIR_ARTIFACTS = ".artifacts";
    public static final String DIR_TEMP = ".TEMP";
    public static final String DIR_LIB = "lib";
    public static final String DIR_JRE21_TEMP = "jre21_temp";
    /**
     * Scratch directory jre21.zip is unzipped into, renamed to {@link #DIR_JRE21_TEMP} only once the
     * extraction completes. This is what makes the existence of {@code jre21_temp/} mean a COMPLETE
     * extraction. {@code migration.exe} uses the same name and the same rename-on-success protocol,
     * so whichever of the two is interrupted, the other discards the leftover and re-extracts.
     */
    public static final String DIR_JRE21_TEMP_PARTIAL = DIR_JRE21_TEMP + ".partial";

    /**
     * Scratch directory the Case A / Case D restore downloads into — a <b>sibling</b> of
     * {@link #DIR_TEMP}, never inside it. {@code ResumableDownloader} keeps {@code <name>.part} and
     * {@code <name>.part.meta} on an interrupted download so the next attempt can resume, and
     * {@code run.bat} copies {@code .TEMP/*} into {@code lib/} wholesale on the next start — so a
     * partial left inside {@code .TEMP/} would land on the classpath and fail the per-file check
     * against the lib manifest. {@code LibUpdater} keeps {@code lib.zip} out of {@code .TEMP/} for
     * exactly the same reason.
     */
    public static final String DIR_TEMP_RESTORE = DIR_TEMP + ".restore";

    // files (relative to the application root)
    public static final String FILE_JRE21_ZIP = "jre21.zip";
    public static final String FILE_RUN_BAT = "run.bat";
    public static final String FILE_RUN_BAT_BACKUP = "run.bat_jre11";
    public static final String FILE_MIGRATION_EXE = "migration.exe";
    public static final String FILE_ROLLBACK_EXE = "rollback.exe";
    public static final String FILE_LAUNCHER = "_launcher.jar";
}
