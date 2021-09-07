package registrationtest.pages;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import registrationtest.controls.Alerts;

import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class BioCorrectionPage {

    private static final Logger logger = LogManager.getLogger(BioCorrectionPage.class);
    FxRobot robot;
    TextField additionalInfoTextBox;
    String additionalInfoRequestId = "#additionalInfoRequestId";
    WaitsUtil waitsUtil;

    public BioCorrectionPage(FxRobot robot) {
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);

    }

    public void setAdditionalInfoRequestId(String value) {

        logger.info("set additional info ");

        try {
            additionalInfoTextBox = waitsUtil.lookupByIdTextField(additionalInfoRequestId, robot);

            assertNotNull(additionalInfoTextBox, "additionalInfoTextBox Not Present");

            additionalInfoTextBox.setText(value);


        } catch (Exception e) {
            logger.error("", e);
        }


    }
}