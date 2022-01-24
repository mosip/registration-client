package registrationtest.utility;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;
import static org.awaitility.Awaitility.*;

import org.testfx.api.FxRobot;
import javafx.geometry.Bounds;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;


import org.testfx.util.WaitForAsyncUtils;

import static org.testfx.assertions.api.Assertions.assertThat;

/**
 * 
 * @author Neeharika.Garg
 * @References https://www.codota.com/code/java/classes/org.awaitility.Awaitility
 */
public class WaitsUtil {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(WaitsUtil.class);

    Node node;
    FxRobot robot;

    public WaitsUtil(FxRobot robot) {
        this.robot = robot;
    }

    public WaitsUtil() {

    }

    public <T extends Node> T lookupById(final String controlId) {

        // verifyThat(robot.lookup("nodeQuery").tryQuery().orElse(null), isNull());
        // assertThat(robot.lookup("#loginScreen").tryQuery()).isPresent();

        try {

            WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    Boolean b = robot.lookup(controlId).query().isVisible();
                    return b;
                }
            });
        } catch (TimeoutException e) {

            logger.error("", e);
            capture();
        }
        return robot.lookup(controlId).query();

    }

    public void clickNodeAssert(String id) {
        node = lookupById(id);

        assertThat(robot.lookup(id).tryQuery()).isNotNull();
        robot.moveTo(node);
        robot.clickOn(node);
        
        

    }

    public void scrollclickNodeAssert(String id) {
        node = lookupById(id);

        assertThat(robot.lookup(id).tryQuery()).isNotNull();
        // scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("proofscroll")));
        scrollVerticalDirectioncount(10);

        robot.moveTo(node);
        robot.clickOn(node);

    }

    public void scrollclickNodeAssert1(String id) {
        node = lookupById(id);

        assertThat(robot.lookup(id).tryQuery()).isNotNull();

        // Platform.runLater(() -> {
        ScrollPane scrollPane = lookupById("#scrollPane");
        scrollPane.setVvalue(scrollPane.getVmax());

        double h = scrollPane.getContent().getBoundsInLocal().getHeight();
        double y = (node.getBoundsInParent().getMaxY() + node.getBoundsInParent().getMinY()) / 2.0;
        double v = scrollPane.getViewportBounds().getHeight();
        scrollPane.setVvalue(scrollPane.getVmax() * ((y - 0.5 * v) / (h - v)));
        scrollPane.layout();
        scrollPane.setVvalue(0.5);
        scrollPane.requestFocus();
        // });
        robot.moveTo(node);

        robot.clickOn(node);

    }

    public void scrollclickNodeAssert2(String id)

    {
        node = lookupById(id);

        assertThat(robot.lookup(id).tryQuery()).isNotNull();
        ScrollPane pane = lookupById("#scrollPane");

        Bounds viewport = pane.getViewportBounds();
        double contentHeight = pane.getContent().getBoundsInLocal().getHeight();
        double contentWidth = pane.getContent().getBoundsInLocal().getWidth();
        double nodeMinY = node.getBoundsInParent().getMinY();
        double nodeMaxY = node.getBoundsInParent().getMaxY();
        double nodeMinX = node.getBoundsInParent().getMinX();
        double nodeMaxX = node.getBoundsInParent().getMaxX();
        double viewportMinY = (contentHeight - viewport.getHeight()) * pane.getVvalue();
        double viewportMaxY = viewportMinY + viewport.getHeight();
        double viewportMinX = (contentWidth - viewport.getWidth()) * pane.getHvalue();
        double viewportMaxX = viewportMinX + viewport.getWidth();
        if (nodeMinY < viewportMinY) {
            pane.setVvalue(nodeMinY / (contentHeight - viewport.getHeight()));
        } else if (nodeMaxY > viewportMaxY) {
            pane.setVvalue((nodeMaxY - viewport.getHeight()) / (contentHeight - viewport.getHeight()));
        }
        if (nodeMinX < viewportMinX) {
            pane.setHvalue(nodeMinX / (contentWidth - viewport.getWidth()));
        } else if (nodeMaxX > viewportMaxX) {
            pane.setHvalue((nodeMaxX - viewport.getWidth()) / (contentWidth - viewport.getWidth()));
        }

    }

    private void scrollVerticalDirectioncount(int scrollcount) {

        try {
            robot.scroll(scrollcount, VerticalDirection.DOWN);

        } catch (Exception e) {
            logger.error("", e);
        }

    }

    public <T extends Node> TextField lookupByIdTextField(String controlId, FxRobot robot) {
        try {
            with().dontCatchUncaughtExceptions().await().pollDelay(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS)
                    .until(() -> (robot.lookup(controlId).queryAs(TextField.class)) != null);
        } catch (Exception e) {
            logger.error("", e);
            capture();

        }

        return robot.lookup(controlId).queryAs(TextField.class);
    }

    
    public <T extends Node> Label lookupByIdLabel(String controlId, FxRobot robot) {
        try {
            with().dontCatchUncaughtExceptions().await().pollDelay(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS)
                    .until(() -> (robot.lookup(controlId).queryAs(Label.class)) != null);
        } catch (Exception e) {
            logger.error("", e);
            capture();

        }

        return robot.lookup(controlId).queryAs(Label.class);
    }
    
    public <T extends Node> Button lookupByIdButton(String controlId, FxRobot robot) {
        try {

            with().pollInSameThread().await().atMost(60, TimeUnit.SECONDS)
                    .until(() -> (robot.lookup(controlId).queryAs(Button.class)) != null);
        } catch (Exception e) {
            logger.error("", e);
            capture();

        }

        return robot.lookup(controlId).queryAs(Button.class);

    }

    public static String capture() {
        String snapshotpath = null;
        try {
            Robot rb = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            Rectangle rec = new Rectangle(0, 0, screenSize.width, screenSize.height);
            BufferedImage image = rb.createScreenCapture(rec);
            // Image myImage=SwingFXUtils.toFXImage(image, null);

            Path SNAPSHOTPATH = Paths.get(System.getProperty("user.dir"), "snapshot",
                    "snapshot" + DateUtil.getDateTime() + ".jpg");
            snapshotpath = SNAPSHOTPATH.toString();
            ImageIO.write(image, "jpg", new File(snapshotpath));

        } catch (Exception e) {
            logger.error("", e);

        }
        return snapshotpath;

    }

}
