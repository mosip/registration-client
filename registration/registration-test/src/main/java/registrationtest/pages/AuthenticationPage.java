package registrationtest.pages;

import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import registrationtest.utility.WaitsUtil;

public class AuthenticationPage {

    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(AuthenticationPage.class);
    private static final long AUTH_FIELD_TIMEOUT_MS = 15_000;

    FxRobot robot;
    WaitsUtil waitsUtil;
    String password = "#password";
    String username = "#username";
    String submitbtn = "#submitbtn";

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

}
