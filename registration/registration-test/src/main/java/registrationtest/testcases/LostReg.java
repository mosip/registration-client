
package registrationtest.testcases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.testfx.api.FxRobot;
import org.testfx.api.FxRobotContext;

import com.aventstack.extentreports.Status;

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





public class LostReg {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(LostReg.class);

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
    WaitsUtil waitsUtil;
    String exit="#exit";
 
    public RID lostRegistration(FxRobot robot, String loginUserid, String loginPwd, String supervisorUserid,
            String supervisorUserpwd, Stage applicationPrimaryStage1, String jsonContent, String flow, String ageGroup,
            String fileName, ApplicationContext applicationContext) {

        try {
            logger.info("Lost UIN Scenario : " + flow + " FileName : " + fileName);
            ExtentReportUtil.test1 = ExtentReportUtil.reports
                    .createTest("Lost UIN Scenario : " + flow + " FileName : " + fileName);

            loginPage = new LoginPage(robot);
            buttons = new Buttons(robot);
            authenticationPage = new AuthenticationPage(robot);
            robotActions = new RobotActions(robot);
            selectLanguagePage = new SelectLanguagePage(robot);
            alerts = new Alerts(robot);
            waitsUtil=new WaitsUtil();
          
            rid1 = null;
            rid2 = null;
            result = false;
            // Load Login screen
            buttons.clickcancelBtn();
            loginPage.loadLoginScene(applicationPrimaryStage1);

            // Enter userid and password

            if (PropertiesUtil.getKeyValue("multilang").equals("Y"))
            {
            loginPage.selectAppLang();
            buttons.clickcancelBtn();
            }
            loginPage.setUserId(loginUserid);
            homePage = loginPage.setPassword(loginPwd);
            // New Registration
            homePage.clickHomeImg();
            ExtentReportUtil.test1.info("Operator Logs in");
            if (PropertiesUtil.getKeyValue("sync").equals("Y"))
                homePage.clickSynchronizeData();

            demographicPage = homePage.clicklostUINImage();

            if (PropertiesUtil.getKeyValue("multilang").equals("Y")) {
                selectLanguagePage.selectLang();
                buttons.clicksubmitBtn();
            }

            webViewDocument = demographicPage.screensFlow(jsonContent, flow, ageGroup);

            buttons.clicknextBtn();

            rid1 = webViewDocument.acceptPreview(flow); // return thread and wait on thread

            buttons.clicknextBtn();

            if (!rid1.rid.trim().isEmpty()) {
                ExtentReportUtil.test1.info("Demo, Doc, Bio - Done");
                ExtentReportUtil.test1.info("Preview done");
            } else {
                ExtentReportUtil.test1.info("Preview not valid");
            }
            /**
             * Authentication enter password Click Continue
             */
            authenticationPage.enterUserName(loginUserid);
            authenticationPage.enterPassword(loginPwd);

            buttons.clickAuthenticateBtn();

            try {

                List<String> exceptionFlag = JsonUtil.JsonObjArrayListParsing(jsonContent, "bioExceptionAttributes");
                if (exceptionFlag != null) {
                    /**
                     * Reviewer enter password Click Continue
                     */
                    authenticationPage.enterUserName(PropertiesUtil.getKeyValue("reviewerUserid"));
                    authenticationPage.enterPassword(PropertiesUtil.getKeyValue("reviewerpwd"));
                    buttons.clickAuthenticateBtn();

                }

            } catch (Exception e) {
                logger.error("", e);
            }

            /**
             * Click Home, eodapprove, approval Button, authenticate button Enter user
             * details
             */

            rid2 = webViewDocument.getacknowledgement(flow);
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
            // waitsUtil.clickNodeAssert(exit);
            homePage.clickHomeImg();

            if (!rid2.rid.trim().isEmpty()) {
                ExtentReportUtil.test1.info("Approve Packet done");
                assertEquals(rid1.getRid(), rid2.getRid());
            } else {
                ExtentReportUtil.step5.log(Status.FAIL, "Approve Packet valid");

            }

            /**
             * Upload the packet
             */
            if (PropertiesUtil.getKeyValue("upload").equals("Y")) {

                uploadPacketPage = homePage.clickuploadPacketImageView(applicationPrimaryStage, scene);
                uploadPacketPage.selectPacket(rid1.getRid().trim());
                buttons.clickuploadBtn();
                /**
                 * Verify Success Upload
                 */
                result = uploadPacketPage.verifyPacketUpload(rid1.getRid().trim());
                ExtentReportUtil.test1.info("Upload Packet done");
            } else if (PropertiesUtil.getKeyValue("upload").equals("N")) {
                result = true;
            }
//Logout Regclient
            rid1.appidrid = rid1.getAppidrid(applicationContext, rid1.rid);
            rid1.setResult(result);
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
            ExtentReportUtil.test1.info("Approve Packet Details Below" + rid2.getWebViewAck());
        } else {
            ExtentReportUtil.test1.log(Status.FAIL, "TESTCASE FAIL");

        }

        ExtentReportUtil.test1.info("Test Data Below" + jsonContent);
        ExtentReportUtil.reports.flush();

        return rid1;
    }
}
