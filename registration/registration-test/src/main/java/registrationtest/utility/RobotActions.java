package registrationtest.utility;



import org.testfx.api.FxRobot;

import javafx.scene.input.KeyCode;

public class RobotActions {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(RobotActions.class);

    FxRobot robot;

    public RobotActions(FxRobot robot) {
        this.robot = robot;
    }

    public void closeWindow() {
        robot.press(KeyCode.ALT);
        robot.press(KeyCode.F4);
        robot.release(KeyCode.F4);
        robot.release(KeyCode.ALT);

    }

    public void clickWindow() {
        robot.press(KeyCode.SPACE).release(KeyCode.SPACE);
    }

}
