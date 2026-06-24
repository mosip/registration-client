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
            Robot awtRobot = new Robot();
            Thread.sleep(500);
            for (int i = 0; i < 12; i++) {
                awtRobot.keyPress(KeyEvent.VK_ESCAPE);
                awtRobot.keyRelease(KeyEvent.VK_ESCAPE);
                Thread.sleep(250);
            }
            logger.info("Dismissed native print/save dialog");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Dialog dismiss thread interrupted", e);
        } catch (Exception e) {
            logger.error("Failed to dismiss native print/save dialog", e);
        }
    }

}
