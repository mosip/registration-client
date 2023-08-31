
package registrationtest.testcases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
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





public class NewReg {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(NewReg.class);
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
  
    public boolean initialRegclientSet(FxRobot robot, String loginUserid, String loginPwd, String filename,
            Stage applicationPrimaryStage1) {
        boolean flag = false;
        try {
          
            ExtentReportUtil.test1 = ExtentReportUtil.reports.createTest("Initial Launch " + filename);

            loginPage = new LoginPage(robot);
            buttons = new Buttons(robot);
            authenticationPage = new AuthenticationPage(robot);
            robotActions = new RobotActions(robot);
            webViewDocument = new WebViewDocument(robot);
            biometricUploadPage = new BiometricUploadPage(robot);
            alerts = new Alerts(robot);
            rid1 = new RID();
            rid2 = new RID();
            waitsUtil=new WaitsUtil();
            result = false;

            // Load Login screen
            buttons.clickcancelBtn();
            loginPage.loadLoginScene(applicationPrimaryStage1);
            ExtentReportUtil.test1.info("RegclientScreen Loaded");

            ExtentReportUtil.test1.info("Operator Enter Details ");
           
            // Enter userid and password
            loginPage.setUserId(loginUserid);
            flag = loginPage.verifyAuthentication(loginPwd, applicationPrimaryStage1);
            ExtentReportUtil.test1.info("Operator logs in");

            if (flag == true) {
                ExtentReportUtil.test1.log(Status.PASS, "SUCCESS Operator Onboards");
                ExtentReportUtil.reports.flush();
                alerts.clickAlertexit();
            } else {
                ExtentReportUtil.test1.log(Status.FAIL, "FAIL Operator Onboards");
                try {
                    alerts.clickAlertexit();
                } catch (Exception e) {
                    logger.error("", e);
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
            }

        } catch (Exception e) {
           
            logger.error(e.getMessage());

        }

        return flag;
    }

    
    public void operatorOnboard(FxRobot robot, String operatorUserid, String operatorPwd, String jsoncontent,String filename,
            Stage applicationPrimaryStage1) {
        boolean flag = false;
        try {
          
            ExtentReportUtil.test1 = ExtentReportUtil.reports.createTest("Onboard " + operatorUserid +  " "+filename);

            loginPage = new LoginPage(robot);
            buttons = new Buttons(robot);
            authenticationPage = new AuthenticationPage(robot);
            robotActions = new RobotActions(robot);
            webViewDocument = new WebViewDocument(robot);
            biometricUploadPage = new BiometricUploadPage(robot);
            alerts = new Alerts(robot);
            result = false;

            buttons.clickcancelBtn();
            // Load Login screen
            loginPage.loadLoginScene(applicationPrimaryStage1);
            ExtentReportUtil.test1.info("RegclientScreen Loaded");

            ExtentReportUtil.test1.info("Operator Enter Details ");

            // Enter userid and password
            if (PropertiesUtil.getKeyValue("multilang").equals("Y"))
            {
            loginPage.selectAppLang();
            buttons.clickcancelBtn();
            }
            loginPage.setUserId(operatorUserid);
            flag = loginPage.verifyOnbard(operatorPwd, jsoncontent);
        //    flag2 = loginPage.verifyOnboardBio(jsoncontent);
            
            ExtentReportUtil.test1.info("Operator logs in");

            if (flag == true ) {
                try {
                    alerts.clickAlertexit();
                } catch (Exception e) {
                    logger.error("", e);
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
                ExtentReportUtil.test1.log(Status.PASS, "SUCCESS Operator Onboards");
                ExtentReportUtil.reports.flush();
                alerts.clickAlertexit();
            } else {
                ExtentReportUtil.test1.log(Status.FAIL, "FAIL Operator Onboards");
                try {
                    alerts.clickAlertexit();
                } catch (Exception e) {
                    logger.error("", e);
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
            }

        } catch (Exception e) {

            logger.error(e.getMessage());
     
            
        }

    }

    public RID newRegistration(FxRobot robot, String loginUserid, String loginPwd, String supervisorUserid,
            String supervisorUserpwd, Stage applicationPrimaryStage1, String jsonContent, String process,
            String ageGroup, String fileName, ApplicationContext applicationContext) {

        try {
            logger.info("New Adult Registration Scenario : " + process + " FileName : " + fileName);
            ExtentReportUtil.test1 = ExtentReportUtil.reports
                    .createTest("New Registration Scenario : " + process + " FileName : " + fileName);

            loginPage = new LoginPage(robot);
            buttons = new Buttons(robot);
            authenticationPage = new AuthenticationPage(robot);
            robotActions = new RobotActions(robot);
            selectLanguagePage = new SelectLanguagePage(robot);

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

            demographicPage = homePage.clickNewRegistration();

            if (PropertiesUtil.getKeyValue("multilang").equals("Y")) {
                selectLanguagePage.selectLang();
                buttons.clicksubmitBtn();
            }

            webViewDocument = demographicPage.screensFlow(jsonContent, process, ageGroup);

            buttons.clicknextBtn();

            rid1 = webViewDocument.acceptPreview(process);

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
            rid2 = webViewDocument.getacknowledgement(process);

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
                ExtentReportUtil.test1.info("Approve Packet invalid");

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
            // Logout Regclient
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

    public RID loginlogout(FxRobot robot2, String loginUserid, String loginPwd, String supervisorUserid,
            String supervisorUserpwd, Stage primaryStage, JSONObject idjson, HashMap<String, String> documentUpload,
            String lang, String schemaversion, String jsonObjName, String idJsonPath) throws InterruptedException {

        loginPage = new LoginPage(robot);
        buttons = new Buttons(robot);
        authenticationPage = new AuthenticationPage(robot);
        robotActions = new RobotActions(robot);

        // Load Login screen
        // loadLoginScene verb
        loginPage.loadLoginScene(primaryStage);

        // Enter userid and password
        loginPage.setUserId(loginUserid);

        homePage = loginPage.setPassword(loginPwd);

        // Logout Regclient
        loginPage.logout();

        return null;
    }
}
