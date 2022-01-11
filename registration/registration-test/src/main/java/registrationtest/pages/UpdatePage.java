package registrationtest.pages;

import java.util.List;



import org.testfx.api.FxRobot;

import javafx.scene.control.TextField;

import registrationtest.utility.JsonUtil;
import registrationtest.utility.WaitsUtil;

public class UpdatePage {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(UpdatePage.class);
    String alertImage = "#alertImage";
    String exit = "#exit";
    String success = "#context";
    String cancel = "#cancel";
    String confirm = "#confirm";
    String uinId = "#uinId";

    WaitsUtil waitsUtil;
    FxRobot robot;

    /**
     * Alerts Constuctor
     * 
     * @param robot
     */
    public UpdatePage(FxRobot robot) {
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
        logger.info("In UpdatePage Constructor");
    }

    /**
     * Enter uinId -
     */
    public void enterUinId(String uinNumber) {
        TextField useruin = waitsUtil.lookupByIdTextField(uinId, robot);
        useruin.setText(uinNumber);
        logger.info("enterUinId");
    }

    public void selectRadioButton(FxRobot robot, String JsonIdentity) {
        List<String> updateUINAttributes = null;
        try {
            updateUINAttributes = JsonUtil.JsonObjArrayListParsing(JsonIdentity, "updateUINAttributes");
        } catch (Exception e) {
            logger.error("", e);
        }
        for (String attr : updateUINAttributes)
            waitsUtil.clickNodeAssert("#" + attr);
    }

}
