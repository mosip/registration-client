package registrationtest.utility;



import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import io.mosip.registration.controller.ClientApplication;
import javafx.scene.input.KeyCode;
import javafx.stage.Window;

public class RobotActions {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(RobotActions.class);

    FxRobot robot;

    public RobotActions(FxRobot robot) {
        this.robot = robot;
    }

    public void closeWindow() {
        if (HeadlessMode.isEnabled()) {
            hideTopPopupWindow();
            return;
        }
        robot.press(KeyCode.ALT);
        robot.press(KeyCode.F4);
        robot.release(KeyCode.F4);
        robot.release(KeyCode.ALT);

    }

    // Monocle has no window manager, so Alt+F4 does nothing there; the primary stage is never closed.
    private void hideTopPopupWindow() {
        try {
            WaitForAsyncUtils.asyncFx(() -> {
                List<Window> popups = new java.util.ArrayList<>();
                Window.getWindows().forEach(window -> {
                    if (window.isShowing() && window != ClientApplication.getPrimaryStage()) {
                        popups.add(window);
                    }
                });
                for (int i = popups.size() - 1; i >= 0; i--) {
                    popups.get(i).hide();
                }
            }).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Failed to close popup window", e);
        }
    }

    public void clickWindow() {
        robot.press(KeyCode.SPACE).release(KeyCode.SPACE);
    }

    public void dismissNativePrintDialogAsync() {
        if (HeadlessMode.isEnabled()) {
            return;
        }
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
