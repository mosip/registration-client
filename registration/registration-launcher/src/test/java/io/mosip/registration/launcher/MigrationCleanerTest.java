package io.mosip.registration.launcher;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MigrationCleanerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void cleanup_removesAllPresentArtifacts() throws Exception {
        File base = folder.getRoot();

        File jreTemp = new File(base, "jre21_temp");
        Files.createDirectories(new File(jreTemp, "bin").toPath());
        Files.write(new File(jreTemp, "bin/java.exe").toPath(), "x".getBytes());

        File artifacts = new File(base, ".artifacts");
        Files.createDirectories(artifacts.toPath());
        Files.write(new File(artifacts, "jre21.zip").toPath(), "x".getBytes());

        Files.write(new File(base, "run.bat_jre11").toPath(), "x".getBytes());
        Files.write(new File(base, "migration.exe").toPath(), "x".getBytes());
        Files.write(new File(base, "rollback.exe").toPath(), "x".getBytes());

        List<String> removed = MigrationCleaner.cleanup(base);

        assertTrue(removed.contains("jre21_temp"));
        assertTrue(removed.contains(".artifacts"));
        assertTrue(removed.contains("run.bat_jre11"));
        assertTrue(removed.contains("migration.exe"));
        assertTrue(removed.contains("rollback.exe"));
        assertEquals(5, removed.size());

        assertFalse(jreTemp.exists());
        assertFalse(artifacts.exists());
        assertFalse(new File(base, "run.bat_jre11").exists());
        assertFalse(new File(base, "migration.exe").exists());
        assertFalse(new File(base, "rollback.exe").exists());
    }

    @Test
    public void cleanup_noArtifacts_returnsEmpty() {
        List<String> removed = MigrationCleaner.cleanup(folder.getRoot());
        assertTrue(removed.isEmpty());
    }

    @Test
    public void cleanup_onlySomeArtifacts_removesOnlyThose() throws Exception {
        File base = folder.getRoot();
        Files.write(new File(base, "migration.exe").toPath(), "x".getBytes());

        List<String> removed = MigrationCleaner.cleanup(base);

        assertEquals(1, removed.size());
        assertTrue(removed.contains("migration.exe"));
        assertFalse(new File(base, "migration.exe").exists());
    }
}
