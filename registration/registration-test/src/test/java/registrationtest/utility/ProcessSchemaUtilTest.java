package registrationtest.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProcessSchemaUtilTest {

    @Test
    void findsProcessFileWhenOnlyTheCapitalizationDiffers(@TempDir Path dir) throws Exception {
        Path written = dir.resolve("bioCorrectionProcess.json");
        Files.writeString(written, "{\"id\":\"BIOMETRIC_CORRECTION\"}");
        String previous = System.setProperty("user.dir", dir.toString());
        try {
            Path resolved = ProcessSchemaUtil.resolveProcessFile("bioCorrection");
            assertEquals("bioCorrectionProcess.json", resolved.getFileName().toString());
            assertTrue(Files.readString(resolved).contains("BIOMETRIC_CORRECTION"));
        } finally {
            if (previous == null) {
                System.clearProperty("user.dir");
            } else {
                System.setProperty("user.dir", previous);
            }
        }
    }
}
