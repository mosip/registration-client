package registrationtest.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProcessSchemaUtilTest {

    @Test
    void usesTheSameProcessFileNameTheClientWrites() {
        assertEquals("bioCorrectionProcess.json",
                ProcessSchemaUtil.resolveProcessFile("bioCorrection").getFileName().toString());
        assertEquals("newProcess.json",
                ProcessSchemaUtil.resolveProcessFile("New").getFileName().toString());
        assertEquals("lostProcess.json",
                ProcessSchemaUtil.resolveProcessFile("Lost").getFileName().toString());
        assertEquals("updateProcess.json",
                ProcessSchemaUtil.resolveProcessFile("Update").getFileName().toString());
    }
}
