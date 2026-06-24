package registrationtest.testcases;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import registrationtest.pages.AuthenticationPage;
import registrationtest.pages.EodApprovalPage;
import registrationtest.pages.HomePage;
import registrationtest.pages.UploadPacketPage;
import registrationtest.pages.WebViewDocument;
import registrationtest.pojo.output.RID;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.JsonUtil;
import registrationtest.utility.WaitsUtil;

public class EodApprovalFlow {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EodApprovalFlow.class);
    private static final long AUTH_TIMEOUT_MS = 15_000L;

    private final WaitsUtil waitsUtil;
    private final EodApprovalPage eodApprovalPage;
    private AuthenticationPage authenticationPage;
    private final WebViewDocument webViewDocument;
    private final HomePage homePage;

    public EodApprovalFlow(FxRobot robot, HomePage homePage) {
        this.waitsUtil = new WaitsUtil(robot);
        this.eodApprovalPage = new EodApprovalPage(robot);
        this.authenticationPage = new AuthenticationPage(robot);
        this.webViewDocument = new WebViewDocument(robot);
        this.homePage = homePage;
    }

    public boolean executeScenario(JSONObject eodTest, String jsonContent, String process, String ageGroup,
            RID rid1, String supervisorUserid, String supervisorUserpwd, Stage applicationPrimaryStage) {
        String action = eodTest.optString("action", "approve");
        ExtentReportUtil.test1.info("Pending Approval scenario: " + action);

        homePage.clickHomeImg();
        eodApprovalPage.openPendingApproval(applicationPrimaryStage);
        eodApprovalPage.selectApplication(rid1.getRid().trim());

        switch (action.toLowerCase()) {
            case "viewdetails":
                return executeViewDetails(eodTest, jsonContent, process, ageGroup, rid1);
            case "reject":
                return executeReject(eodTest, rid1, supervisorUserid, supervisorUserpwd, applicationPrimaryStage);
            case "approve":
            default:
                return executeApprove(supervisorUserid, supervisorUserpwd);
        }
    }

    private boolean executeViewDetails(JSONObject eodTest, String jsonContent, String process, String ageGroup,
            RID rid1) {
        List<String> viewTests = resolveEodViewTests(eodTest, jsonContent);
        webViewDocument.verifyEodApplicationDetails(jsonContent, process, ageGroup, rid1, viewTests);
        ExtentReportUtil.test1.pass("Pending Approval application details verified for RID: " + rid1.getRid());
        homePage.clickHomeImg();
        return true;
    }

    private boolean executeApprove(String supervisorUserid, String supervisorUserpwd) {
        eodApprovalPage.clickOnApprovalBtn();
        eodApprovalPage.verifyApproveMarkDisplayed();
        authenticationPage = eodApprovalPage.clickOnAuthenticateBtn();
        authenticationPage.enterUserName(supervisorUserid);
        authenticationPage.enterPassword(supervisorUserpwd);
        authenticationPage.clicksubmitBtn();
        verifyEodAuthSuccess();
        homePage.clickHomeImg();
        ExtentReportUtil.test1.pass("Pending Approval approve authentication succeeded");
        return true;
    }

    private boolean executeReject(JSONObject eodTest, RID rid1, String supervisorUserid, String supervisorUserpwd,
            Stage applicationPrimaryStage) {
        eodApprovalPage.clickOnRejectBtn();
        eodApprovalPage.selectRejectReason(eodTest.optString("rejectReason", null));
        eodApprovalPage.submitRejectReason();
        eodApprovalPage.verifyRejectMarkDisplayed();
        ExtentReportUtil.test1.pass("Reject mark displayed for application: " + rid1.getRid());

        authenticationPage = eodApprovalPage.clickOnAuthenticateBtn();
        authenticationPage.enterUserName(supervisorUserid);

        String supervisorAuthPassword = eodTest.optString("supervisorAuthPassword", "valid");
        if ("invalid".equalsIgnoreCase(supervisorAuthPassword)) {
            authenticationPage.verifySupervisorEodAuthFailure(supervisorUserpwd);
            eodApprovalPage.selectApplication(rid1.getRid().trim());
            eodApprovalPage.verifyApplicationInPendingApproval();
            eodApprovalPage.verifyRejectMarkDisplayed();
            ExtentReportUtil.test1.pass("Application remains in Pending Approval after failed supervisor authentication");
            homePage.clickHomeImg();
            return true;
        }

        authenticationPage.enterPassword(supervisorUserpwd);
        authenticationPage.clicksubmitBtn();
        verifyEodAuthSuccess();
        eodApprovalPage.verifyApplicationNotInPendingApproval(rid1.getRid().trim());

        homePage.clickHomeImg();
        UploadPacketPage uploadPacketPage = homePage.clickuploadPacketImageView(applicationPrimaryStage, null);
        boolean present = uploadPacketPage.verifyPacketPresent(rid1.getRid().trim());
        assertTrue(present, "Rejected application not found in Application Upload task");
        ExtentReportUtil.test1.pass("Rejected application moved to Application Upload task");
        homePage.clickHomeImg();
        return present;
    }

    private List<String> resolveEodViewTests(JSONObject eodTest, String jsonContent) {
        if (eodTest.has("eodViewTests") && !eodTest.isNull("eodViewTests")) {
            JSONArray tests = eodTest.getJSONArray("eodViewTests");
            List<String> viewTests = new ArrayList<>();
            for (int i = 0; i < tests.length(); i++) {
                viewTests.add(tests.getString(i));
            }
            return viewTests;
        }
        List<String> configured = JsonUtil.getOptionalIdentityArrayList(jsonContent, "eodViewTests");
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        return Arrays.asList("applicationId", "uin", "dateTime", "demographics", "biometrics", "documents");
    }

    private void verifyEodAuthSuccess() {
        Label contextLabel = waitsUtil.waitForNode("#context", Label.class);
        String actualMessage = contextLabel.getText().trim();
        String expectedMessage = RegistrationUIConstants
                .getMessageLanguageSpecific(RegistrationUIConstants.AUTH_APPROVAL_SUCCESS_MSG);
        String expectedText = expectedMessage.split(RegistrationConstants.SPLITTER)[0].trim();
        assertTrue(actualMessage.contains(expectedText),
                "Expected EOD auth success message containing '" + expectedText + "' but got: " + actualMessage);
        ExtentReportUtil.test1.pass("Supervisor EOD authentication succeeded: " + actualMessage);
        waitsUtil.clickVisibleNodeInAnyWindow("#exit", AUTH_TIMEOUT_MS);
        WaitForAsyncUtils.waitForFxEvents();
    }
}
