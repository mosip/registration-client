package registrationtest.controls;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.testfx.api.FxRobot;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;

import javafx.scene.Node;
import javafx.scene.control.Label;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.WaitsUtil;

public class Alerts {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(Alerts.class);
    String alertImage = "#alertImage";
    String exit = "#exit";
    String success = "#context";
    String cancel = "#cancel";
    String confirm = "#confirm";

    WaitsUtil waitsUtil;
    FxRobot robot;

    /**
     * Alerts Constuctor
     * 
     * @param robot
     */
    public Alerts(FxRobot robot) {
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
        logger.info("In Alerts Constructor");
    }

    /**
     * clickAlertImage - For example Tick , Cross
     */
    public void clickAlertImage() {
        waitsUtil.clickNodeAssert(alertImage);
        logger.info("clickAlertImage");
    }

    /**
     * clickAlertexit - Close X
     */
    public void clickAlertexit() {
        waitsUtil.clickNodeAssert(exit);
        logger.info("clickAlertexit");
    }

    /**
     * clickAlertSuccess - Success Text
     */
    public void clickAlertSuccess() {
        waitsUtil.clickNodeAssert(success);
        logger.info("clickAlertSuccess");
    }

    /**
     * clickAlertcancel - cancel Text
     */
    public void clickAlertCancel() {
        waitsUtil.clickNodeAssert(cancel);
        logger.info("clickAlertCancel");
    }

    /**
     * clickAlertConfirm - confirm Text
     */
    public void clickAlertConfirm() {
        waitsUtil.clickNodeAssert(confirm);
        logger.info("clickAlertConfirm");
    }

    public void verifyAndDismissPrintSuccessAlert() {
        Node contextNode = waitsUtil.waitForVisibleNodeInAnyWindow(success, 30_000L);
        assertNotNull(contextNode, "Print acknowledgement success alert not displayed");
        Label contextLabel = (Label) contextNode;
        String actualMessage = contextLabel.getText().trim();
        logger.info("Print acknowledgement popup message: {}", actualMessage);
        ExtentReportUtil.test1.info("Print acknowledgement popup: " + actualMessage);
        String expectedMessage = RegistrationUIConstants
                .getMessageLanguageSpecific(RegistrationUIConstants.PRINT_INITIATION_SUCCESS);
        String expectedText = expectedMessage.split(RegistrationConstants.SPLITTER)[0].trim();
        assertTrue(actualMessage.contains(expectedText),
                "Expected print success message containing '" + expectedText + "' but got: " + actualMessage);
        Node successIcon = waitsUtil.waitForVisibleNodeInAnyWindow(alertImage, 5_000L);
        assertNotNull(successIcon, "Print success alert icon not displayed");
        ExtentReportUtil.test1.pass("Print success alert verified");
        waitsUtil.clickVisibleNodeInAnyWindow(exit, 10_000L);
        logger.info("Dismissed print success alert");
    }

}
