package registrationtest.pages;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.WaitsUtil;

public class AuthenticationPage {

    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(AuthenticationPage.class);
    private static final long AUTH_FIELD_TIMEOUT_MS = 15_000;

    FxRobot robot;
    WaitsUtil waitsUtil;
    String password = "#password";
    String username = "#username";
    String submitbtn = "#submitbtn";
    String authenticate = "#authenticate";

    public AuthenticationPage(FxRobot robot) {
        logger.info("Constructor AuthenticationPage ");
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);

    }

    private void dismissAlertIfPresent() {
        robot.lookup("#exit").tryQuery().ifPresent(node -> {
            if (node.isVisible() && !node.isDisable()) {
                robot.clickOn(node);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void enterPassword(String pwd) {
        logger.info("enterPassword");
        dismissAlertIfPresent();
        long start = System.currentTimeMillis();
        Node node = waitsUtil.waitForVisibleAuthPassword(AUTH_FIELD_TIMEOUT_MS);
        logger.info("Auth password field ready in {} ms", System.currentTimeMillis() - start);
        TextInputControl passwordField = (TextInputControl) node;
        robot.interact(() -> {
            passwordField.clear();
            passwordField.setText(pwd);
        });
        WaitForAsyncUtils.waitForFxEvents();
        logger.info("Password entered in {} ms", System.currentTimeMillis() - start);
    }

    public void enterUserName(String userid) {
        logger.info("enterUserName");
        dismissAlertIfPresent();
        long start = System.currentTimeMillis();
        Node node = waitsUtil.waitForVisibleAuthUsername(AUTH_FIELD_TIMEOUT_MS);
        logger.info("Auth username field ready in {} ms", System.currentTimeMillis() - start);
        TextField userField = (TextField) node;
        if (!userField.isEditable()) {
            logger.info("Username field not editable, using pre-filled value: {}", userField.getText());
            return;
        }
        robot.interact(() -> {
            userField.clear();
            userField.setText(userid);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void clicksubmitBtn() {
        logger.info("clicksubmitBtn");
        dismissAlertIfPresent();
        waitsUtil.clickVisibleNodeInAnyWindow(submitbtn, AUTH_FIELD_TIMEOUT_MS);
    }

    public void setInvalidPassword(String pwdText) {
        logger.info("setInvalidPassword: {}", pwdText);
        dismissAlertIfPresent();
        Node node = waitsUtil.waitForVisibleAuthPassword(AUTH_FIELD_TIMEOUT_MS);
        TextInputControl passwordField = (TextInputControl) node;
        String invalidPwd = pwdText + "invalid";
        robot.interact(() -> {
            passwordField.clear();
            passwordField.setText(invalidPwd);
        });
        WaitForAsyncUtils.waitForFxEvents();
        waitsUtil.clickVisibleNodeInAnyWindow(authenticate, AUTH_FIELD_TIMEOUT_MS);
        Label contextLabel = waitsUtil.waitForNode("#context", Label.class);
        String actualMessage = contextLabel.getText().trim();
        logger.info("Packet auth failure popup message: {}", actualMessage);
        ExtentReportUtil.test1.info("Packet auth failure popup: " + actualMessage);
        assertTrue(actualMessage.toLowerCase().contains("authentication failed")
                        || actualMessage.toLowerCase().contains("failed"),
                "Expected authentication failure message but got: " + actualMessage);
        ExtentReportUtil.test1.pass("Operator packet auth failed as expected with invalid password");
        waitsUtil.clickVisibleNodeInAnyWindow("#exit", AUTH_FIELD_TIMEOUT_MS);
        WaitForAsyncUtils.waitForFxEvents();
        Node passwordAfterFailure = waitsUtil.waitForVisibleAuthPassword(AUTH_FIELD_TIMEOUT_MS);
        assertNotNull(passwordAfterFailure, "Password field not available for re-entry after auth failure");
        ExtentReportUtil.test1.pass("Password field available for re-entry after failed packet auth");
    }

    public void verifySupervisorEodAuthFailure(String supervisorPwd) {
        logger.info("verifySupervisorEodAuthFailure");
        dismissAlertIfPresent();
        enterPassword(supervisorPwd + "invalid");
        waitsUtil.clickVisibleNodeInAnyWindow(authenticate, AUTH_FIELD_TIMEOUT_MS);
        Label contextLabel = waitsUtil.waitForNode("#context", Label.class);
        String actualMessage = contextLabel.getText().trim();
        logger.info("Supervisor EOD auth failure popup message: {}", actualMessage);
        ExtentReportUtil.test1.info("Supervisor EOD auth failure popup: " + actualMessage);
        assertTrue(actualMessage.toLowerCase().contains("authenticate")
                        || actualMessage.toLowerCase().contains("failed"),
                "Expected supervisor EOD authentication failure message but got: " + actualMessage);
        ExtentReportUtil.test1.pass("Supervisor EOD authentication failed as expected with invalid credentials");
        waitsUtil.clickVisibleNodeInAnyWindow("#exit", AUTH_FIELD_TIMEOUT_MS);
        WaitForAsyncUtils.waitForFxEvents();
        dismissEodAuthWindow();
    }

    public void dismissEodAuthWindow() {
        logger.info("dismissEodAuthWindow");
        waitsUtil.clickIfPresent("#exitWindowImgView");
        waitsUtil.clickIfPresent("#exitWindowImgVwAuthPageTitle");
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void verifySameOperatorReviewerAuthFailure(String operatorUserid, String operatorPwd) {
        logger.info("verifySameOperatorReviewerAuthFailure for user: {}", operatorUserid);
        dismissAlertIfPresent();
        enterUserName(operatorUserid);
        enterPassword(operatorPwd);
        waitsUtil.clickVisibleNodeInAnyWindow(authenticate, AUTH_FIELD_TIMEOUT_MS);
        Label contextLabel = waitsUtil.waitForNode("#context", Label.class);
        String actualMessage = contextLabel.getText().trim();
        logger.info("Reviewer auth failure popup message: {}", actualMessage);
        ExtentReportUtil.test1.info("Reviewer auth failure popup: " + actualMessage);
        assertTrue(actualMessage.toLowerCase().contains("not authorized")
                        && actualMessage.toLowerCase().contains("reviewer"),
                "Expected reviewer authorization failure message but got: " + actualMessage);
        ExtentReportUtil.test1.pass("Reviewer authentication failed as expected with same operator credentials");
        waitsUtil.clickVisibleNodeInAnyWindow("#exit", AUTH_FIELD_TIMEOUT_MS);
        WaitForAsyncUtils.waitForFxEvents();
        Node passwordAfterFailure = waitsUtil.waitForVisibleAuthPassword(AUTH_FIELD_TIMEOUT_MS);
        assertNotNull(passwordAfterFailure, "Password field not available on reviewer authentication screen after failure");
        Node usernameAfterFailure = waitsUtil.waitForVisibleAuthUsername(AUTH_FIELD_TIMEOUT_MS);
        assertNotNull(usernameAfterFailure, "Username field not available on reviewer authentication screen after failure");
        ExtentReportUtil.test1.pass("Operator remains on reviewer authentication screen after failed attempt");
    }

}
