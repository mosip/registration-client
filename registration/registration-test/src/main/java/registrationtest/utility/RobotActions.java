package registrationtest.utility;



import java.awt.Robot;
import java.awt.event.KeyEvent;

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

    public void dismissNativePrintDialogAsync() {
        Thread dialogHandler = new Thread(this::dismissNativePrintSaveDialog, "native-print-dialog-handler");
        dialogHandler.setDaemon(true);
        dialogHandler.start();
    }

    private void dismissNativePrintSaveDialog() {
        try {
            Thread.sleep(2000);
            Robot awtRobot = new Robot();
            awtRobot.delay(300);
            awtRobot.keyPress(KeyEvent.VK_ESCAPE);
            awtRobot.keyRelease(KeyEvent.VK_ESCAPE);
            Thread.sleep(500);
            awtRobot.keyPress(KeyEvent.VK_ESCAPE);
            awtRobot.keyRelease(KeyEvent.VK_ESCAPE);
            logger.info("Dismissed native print/save dialog");
        } catch (Exception e) {
            logger.error("Failed to dismiss native print/save dialog", e);
        }
    }

}
