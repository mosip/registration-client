
package registrationtest.testcases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.testfx.api.FxRobot;
import org.testfx.api.FxRobotContext;

import com.aventstack.extentreports.Status;

import io.mosip.registration.dao.RegistrationDAO;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import registrationtest.controls.Alerts;
import registrationtest.controls.Buttons;
import registrationtest.pages.AuthenticationPage;
import registrationtest.pages.BiometricUploadPage;
import registrationtest.pages.DemographicPage;
import registrationtest.pages.EodApprovalPage;
import registrationtest.pages.HomePage;
import registrationtest.pages.LoginPage;
import registrationtest.pages.SelectLanguagePage;
import registrationtest.pages.UpdatePage;
import registrationtest.pages.UploadPacketPage;
import registrationtest.pages.WebViewDocument;
import registrationtest.pojo.output.*;
import registrationtest.pojo.schema.Root;
import registrationtest.pojo.schema.Schema;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.JsonUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.RobotActions;
import registrationtest.utility.WaitsUtil;





public class UpdateReg {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(UpdateReg.class);
    @Autowired
    private RegistrationDAO registrationDAO;
    FxRobot robot;
    Schema schema;
    Root root;
    Scene scene;
    Node node;
    Boolean flagContinueBtnFileUpload = true;
    Boolean flagContinueBtnBioUpload = true;
    private static ApplicationContext applicationContext;
    private static Stage applicationPrimaryStage;
    private static String upgradeServer = null;
    private static String tpmRequired = "Y";
    LoginPage loginPage;
    HomePage homePage;
    PropertiesUtil propertiesUtil;
    FxRobotContext context;
    Boolean result;
    DemographicPage demographicPage;
    BiometricUploadPage biometricUploadPage;
    Buttons buttons;
    WebViewDocument webViewDocument;
    RID rid1, rid2;
    AuthenticationPage authenticationPage;
    RobotActions robotActions;
    EodApprovalPage eodApprovalPage;
    UploadPacketPage uploadPacketPage;
    SelectLanguagePage selectLanguagePage;
    Alerts alerts;
    UpdatePage updatePage;
    WaitsUtil waitsUtil;
    String exit="#exit";

    public RID updateRegistration(FxRobot robot, String loginUserid, String loginPwd, String supervisorUserid,
            String supervisorUserpwd, Stage applicationPrimaryStage1, String jsonContent, String process,
            String ageGroup, String fileName, ApplicationContext applicationContext) {
        try {
            logger.info("New Adult Registration Scenario : " + process + " FileName : " + fileName);
            ExtentReportUtil.test1 = ExtentReportUtil.reports
                    .createTest("Update Scenario : " + process + " FileName : " + fileName);

            loginPage = new LoginPage(robot);
            buttons = new Buttons(robot);
            authenticationPage = new AuthenticationPage(robot);
            robotActions = new RobotActions(robot);
            selectLanguagePage = new SelectLanguagePage(robot);
            updatePage = new UpdatePage(robot);
            waitsUtil = new WaitsUtil();
            demographicPage = new DemographicPage(robot);

            rid1 = null;
            rid2 = null;
            result = false;

            JSONArray eodTests = JsonUtil.getOptionalIdentityJsonArray(jsonContent, "eodTests");
            if (eodTests != null && eodTests.length() > 0) {
                result = runEodTestScenarios(robot, loginUserid, loginPwd, supervisorUserid, supervisorUserpwd,
                        applicationPrimaryStage1, jsonContent, process, ageGroup, applicationContext, eodTests);
            } else {
                performOperatorLogin(loginUserid, loginPwd, applicationPrimaryStage1);
                performUpdateFlow(jsonContent, process, ageGroup, loginUserid, loginPwd);
                performDefaultEodApproval(supervisorUserid, supervisorUserpwd, applicationPrimaryStage1, jsonContent,
                        process, ageGroup);
                result = performUploadIfConfigured(applicationPrimaryStage1);
                rid1.appidrid = rid1.getAppidrid(applicationContext, rid1.rid);
                rid1.setResult(result);
            }
        } catch (Exception e) {

            logger.error("", e);

            try {
                ExtentReportUtil.test1.addScreenCaptureFromPath(WaitsUtil.capture());
            } catch (IOException e1) {

                logger.error("", e1);
            }

        }
        try {
            homePage.clickHomeImg();
            buttons.clickConfirmBtn();
        } catch (Exception e) {
            logger.error("", e);
        }
        try {
            loginPage.logout();
            buttons.clickConfirmBtn();

        } catch (Exception e) {
            logger.error("", e);
        }

        if (result == true) {
            ExtentReportUtil.test1.log(Status.PASS,
                    "TESTCASE PASS\n" + "[Appid=" + rid1.rid + "] [RID=" + rid1.appidrid + "] [DATE TIME="
                            + rid1.ridDateTime + "] [ENVIRONMENT=" + System.getProperty("mosip.hostname") + "]");
            if (rid2 != null) {
                ExtentReportUtil.test1.info("Approve Packet Details Below" + rid2.getWebViewAck());
            }
        } else {
            ExtentReportUtil.test1.log(Status.FAIL, "TESTCASE FAIL");

        }

        ExtentReportUtil.test1.info("Test Data Below" + jsonContent);
        ExtentReportUtil.reports.flush();

        return rid1;
    }

    private boolean runEodTestScenarios(FxRobot robot, String loginUserid, String loginPwd, String supervisorUserid,
            String supervisorUserpwd, Stage applicationPrimaryStage1, String jsonContent, String process,
            String ageGroup, ApplicationContext applicationContext, JSONArray eodTests) throws Exception {
        boolean allPassed = true;
        RID lastRid = null;

        for (int i = 0; i < eodTests.length(); i++) {
            JSONObject eodTest = eodTests.getJSONObject(i);
            ExtentReportUtil.test1.info("Running Pending Approval scenario " + (i + 1) + " of " + eodTests.length());

            performOperatorLogin(loginUserid, loginPwd, applicationPrimaryStage1);
            performUpdateFlow(jsonContent, process, ageGroup, loginUserid, loginPwd);

            EodApprovalFlow eodApprovalFlow = new EodApprovalFlow(robot, homePage);
            boolean scenarioPassed = eodApprovalFlow.executeScenario(eodTest, jsonContent, process, ageGroup, rid1,
                    supervisorUserid, supervisorUserpwd, applicationPrimaryStage1);
            allPassed = allPassed && scenarioPassed;

            lastRid = rid1;
            lastRid.appidrid = lastRid.getAppidrid(applicationContext, lastRid.rid);
            lastRid.setResult(scenarioPassed);

            if (i < eodTests.length() - 1) {
                homePage.clickHomeImg();
                loginPage.logout();
                buttons.clickConfirmBtn();
            }
        }

        rid1 = lastRid;
        return allPassed;
    }

    private void performOperatorLogin(String loginUserid, String loginPwd, Stage applicationPrimaryStage1)
            throws IOException {
        buttons.clickcancelBtn();
        loginPage.loadLoginScene(applicationPrimaryStage1);

        if (PropertiesUtil.getKeyValue("multilang").equals("Y")) {
            loginPage.selectAppLang();
            buttons.clickcancelBtn();
        }

        loginPage.setUserId(loginUserid);
        homePage = loginPage.setPassword(loginPwd);
        ExtentReportUtil.test1.info("Operator Logs in");
    }

    private void performUpdateFlow(String jsonContent, String process, String ageGroup, String loginUserid,
            String loginPwd) throws Exception {
        homePage.clickuinUpdateImage();

        if (PropertiesUtil.getKeyValue("multilang").equals("Y")) {
            selectLanguagePage.selectLang();
            buttons.clicksubmitBtn();
        }
        updatePage.enterUinId(JsonUtil.JsonObjParsing(jsonContent, "UIN"));
        updatePage.selectRadioButton(robot, jsonContent);
        buttons.clickContinueBtn();

        webViewDocument = demographicPage.screensFlow(jsonContent, process, ageGroup);
        buttons.clickNextBtn();

        rid1 = webViewDocument.acceptPreview(process);
        webViewDocument.verifyPreviewScreen(jsonContent, process, ageGroup, rid1);
        buttons.clickNextBtn();

        if (!rid1.rid.trim().isEmpty()) {
            ExtentReportUtil.test1.info("Demo, Doc, Bio - Done");
            ExtentReportUtil.test1.info("Preview done");
        } else {
            ExtentReportUtil.test1.info("Preview not valid");
        }

        authenticationPage.enterUserName(loginUserid);
        authenticationPage.enterPassword(loginPwd);
        buttons.clickAuthenticateBtn();

        try {
            List<String> exceptionFlag = JsonUtil.JsonObjArrayListParsing(jsonContent, "bioExceptionAttributes");
            if (exceptionFlag != null) {
                authenticationPage.enterUserName(PropertiesUtil.getKeyValue("reviewerUserid"));
                authenticationPage.enterPassword(PropertiesUtil.getKeyValue("reviewerpwd"));
                buttons.clickAuthenticateBtn();
            }
        } catch (Exception e) {
            logger.error("", e);
        }

        rid2 = webViewDocument.getacknowledgement(process);
        webViewDocument.verifyAcknowledgementScreen(jsonContent, process, ageGroup, rid2);
    }

    private void performDefaultEodApproval(String supervisorUserid, String supervisorUserpwd,
            Stage applicationPrimaryStage1, String jsonContent, String process, String ageGroup) {
        homePage.clickHomeImg();

        eodApprovalPage = homePage.clickeodApprovalImageView(applicationPrimaryStage, scene);
        eodApprovalPage.clickOnfilterField();
        eodApprovalPage.enterFilterDetails(rid1.getRid().trim());
        eodApprovalPage.clickOnApprovalBtn();
        authenticationPage = eodApprovalPage.clickOnAuthenticateBtn();
        authenticationPage.enterUserName(supervisorUserid);
        authenticationPage.enterPassword(supervisorUserpwd);
        authenticationPage.clicksubmitBtn();
        robotActions.clickWindow();
        homePage.clickHomeImg();
        if (!rid2.rid.trim().isEmpty()) {
            ExtentReportUtil.test1.info("Approve Packet done");
            assertEquals(rid1.getRid(), rid2.getRid());
        } else {
            ExtentReportUtil.test1.info("Approve Packet invalid");
        }
    }

    private boolean performUploadIfConfigured(Stage applicationPrimaryStage1) throws IOException {
        if (PropertiesUtil.getKeyValue("upload").equals("Y")) {
            uploadPacketPage = homePage.clickuploadPacketImageView(applicationPrimaryStage, scene);
            uploadPacketPage.selectPacket(rid1.getRid().trim());
            buttons.clickuploadBtn();
            boolean uploadResult = uploadPacketPage.verifyPacketUpload(rid1.getRid().trim());
            ExtentReportUtil.test1.info("Upload Packet done");
            return uploadResult;
        }
        return true;
    }

}
