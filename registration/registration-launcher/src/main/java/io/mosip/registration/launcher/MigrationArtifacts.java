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

    // files (relative to the application root)
    public static final String FILE_JRE21_ZIP = "jre21.zip";
    public static final String FILE_RUN_BAT = "run.bat";
    public static final String FILE_RUN_BAT_BACKUP = "run.bat_jre11";
    public static final String FILE_MIGRATION_EXE = "migration.exe";
    public static final String FILE_ROLLBACK_EXE = "rollback.exe";
    public static final String FILE_LAUNCHER = "_launcher.jar";
}
