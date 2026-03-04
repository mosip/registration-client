package registrationtest.pages;

import org.testfx.api.FxRobot;

import javafx.scene.control.TextField;
import registrationtest.controls.Buttons;
import registrationtest.utility.WaitsUtil;

public class AuthenticationPage {

    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(AuthenticationPage.class);
    FxRobot robot;
    WaitsUtil waitsUtil;
    String AuthenticationImg = "#AuthenticationImg";
    String password = "#password";
    String username = "#username";
    String submitbtn = "#submitbtn";

    public AuthenticationPage(FxRobot robot) {
        logger.info("Constructor AuthenticationPage ");
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);

    }

    public void enterPassword(String pwd) {
        logger.info("enterPassword");
        TextField textfieldpwd = waitsUtil.waitForNode(password, TextField.class);
        textfieldpwd.setText(pwd);
    }

    public void enterUserName(String userid) {
        logger.info("enterUserName");
        TextField textfielduserid = waitsUtil.waitForNode(username, TextField.class);
        textfielduserid.setText(userid);
    }

    public void clicksubmitBtn() {
        logger.info("clicksubmitBtn");

        waitsUtil.clickNodeAssert(submitbtn);
    }

}
