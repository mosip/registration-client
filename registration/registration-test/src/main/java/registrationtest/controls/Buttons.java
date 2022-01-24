package registrationtest.controls;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.testfx.api.FxRobot;

import javafx.scene.control.Button;
import registrationtest.utility.WaitsUtil;

public class Buttons {

    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(Buttons.class);
    String continueBtn = "#continueBtn", backBtn = "#backBtn", confirm = "#confirm", uploadBtn = "#uploadBtn",
            cancel = "#cancel", submit = "#submit", next = "#next", authenticate = "#authenticate", print = "#print";

    WaitsUtil waitsUtil;
    FxRobot robot;

    /**
     * {@summary} Buttons Constructor
     * 
     * @param robot
     */
    public Buttons(FxRobot robot) {
        logger.info("In Button Constructor");
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);

    }

    /**
     * {@summary} - Continue Button Click
     */
    public void clickContinueBtn() {
        logger.info("clickContinueBtn");
        waitsUtil.clickNodeAssert(continueBtn);

    }

    /**
     * {@summary} Back Button Click
     */
    public void clickBackBtn() {
        logger.info("clickBackBtn");
        waitsUtil.clickNodeAssert(backBtn);

    }

    /**
     * Confirm Button Click
     */
    public void clickConfirmBtn() {
        logger.info("clickConfirmBtn");
        waitsUtil.clickNodeAssert(confirm);
    }

    /**
     * Upload Button Click
     */
    public void clickuploadBtn() {
        logger.info("clickuploadBtn");
        waitsUtil.clickNodeAssert(uploadBtn);
    }

    /**
     * Cancel Click
     */
    public void clickcancelBtn() {
        try {
            logger.info("clickcancelBtn");
            waitsUtil.clickNodeAssert(cancel);
        } catch (Exception e) {
            logger.error("clickcancelBtn not present", e);
        }
    }

    /**
     * Submit Click
     */
    public void clicksubmitBtn() {
        logger.info("clicksubmitBtn");
        waitsUtil.clickNodeAssert(submit);
    }

    /**
     * Continue Click
     */
    public void clicknextBtn() {
        logger.info("clicknextBtn");
        waitsUtil.clickNodeAssert(next);
    }

    /**
     * Authenticate Click
     */
    public void clickAuthenticateBtn() {
        logger.info("clickAuthenticateBtn");
        waitsUtil.clickNodeAssert(authenticate);

    }
}
