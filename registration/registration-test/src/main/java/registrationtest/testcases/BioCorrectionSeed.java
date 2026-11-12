package registrationtest.testcases;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.testfx.api.FxRobot;

import com.aventstack.extentreports.Status;

import javafx.stage.Stage;
import registrationtest.api.AdditionalInfoRequestClient;
import registrationtest.pojo.output.RID;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.PropertiesUtil;

/**
 * A BIOMETRIC_CORRECTION request only exists after registration processor pauses a packet whose biometric quality
 * is below level-4 (score under 30). When BioCorrection test data has additionalInfoRequestId "AUTO", this creates
 * such a NEW packet with low MockMDS scores, waits for the request and puts its id into the test data.
 */
public final class BioCorrectionSeed {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BioCorrectionSeed.class);
    private static final String AUTO = "AUTO";
    private static final String PROCESS = "BIOMETRIC_CORRECTION";

    private BioCorrectionSeed() {
    }

    public static String prepare(FxRobot robot, String operatorId, String operatorPwd, String supervisorId,
            String supervisorPwd, Stage stage, String jsonContent, String fileName, ApplicationContext context)
            throws Exception {
        JSONObject root = new JSONObject(jsonContent);
        JSONObject identity = root.getJSONObject("identity");
        if (!AUTO.equalsIgnoreCase(identity.optString("additionalInfoRequestId"))) {
            return jsonContent;
        }

        String seedFile = valueOrDefault("bioCorrection.seedFile", "NewAdult.json");
        String lowScore = valueOrDefault("bioCorrection.seedScore", "20");
        String seedPath = seedFile.startsWith("/") ? seedFile : PropertiesUtil.getKeyValue("datadir") + seedFile;
        JSONObject seed = new JSONObject(new String(Files.readAllBytes(
                new File(System.getProperty("user.dir") + seedPath).toPath()), StandardCharsets.UTF_8));
        JSONObject seedIdentity = seed.getJSONObject("identity");
        for (int i = 1; i <= 4; i++) {
            seedIdentity.put("score" + i, lowScore);
        }

        RID seedRid = new NewReg().newRegistration(robot, operatorId, operatorPwd, supervisorId, supervisorPwd,
                stage, seed.toString(), "New", seedIdentity.optString("ageGroup", "ADULT"),
                fileName + " (low quality packet for " + PROCESS + ")", context);
        if (seedRid == null || !Boolean.TRUE.equals(seedRid.result)) {
            throw new IllegalStateException("Low quality NEW packet for " + PROCESS + " was not uploaded");
        }

        long waitSeconds = Long.parseLong(valueOrDefault("bioCorrection.requestWaitSeconds", "1800"));
        String requestId = AdditionalInfoRequestClient.waitForRequestId(seedRid.rid.trim(), PROCESS, waitSeconds);
        logger.info("{} request {} created for {}", PROCESS, requestId, seedRid.rid);
        ExtentReportUtil.test1.log(Status.INFO, PROCESS + " request created by server: " + requestId);
        ExtentReportUtil.reports.flush();

        identity.put("additionalInfoRequestId", requestId);
        return root.toString();
    }

    private static String valueOrDefault(String key, String defaultValue) throws Exception {
        String value = PropertiesUtil.getKeyValue(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
