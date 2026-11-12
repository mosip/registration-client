package registrationtest.utility;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;
import static org.awaitility.Awaitility.*;

import org.testfx.api.FxRobot;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Window;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;


import org.testfx.service.locator.BoundsLocatorException;
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

    /** Closes the registration alert when it is covering the screen. */
    public void dismissAlertIfPresent() {
        try {
            Optional<Node> icon = robot.lookup("#alertImage").tryQuery();
            if (icon.isEmpty() || !icon.get().isVisible()) {
                return;
            }
            Optional<Node> exit = robot.lookup("#exit").tryQuery();
            if (exit.isPresent() && exit.get().isVisible()) {
                robot.clickOn(exit.get());
                WaitForAsyncUtils.waitForFxEvents();
                logger.info("Dismissed alert");
            }
        } catch (RuntimeException e) {
            logger.error("Could not dismiss alert", e);
        }
    }

    public boolean isShown(String id) {
        try {
            Optional<Node> found = robot.lookup(id).tryQuery();
            return found.isPresent() && found.get().isVisible();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void clickNodeAssert(String id) {
    	waitForNode(id);
        node = lookupById(id);
        assertThat(robot.lookup(id).tryQuery()).isNotNull();
        try {
            robot.moveTo(node);
        } catch (RuntimeException e) {
            if (!isOutOfView(e)) {
                throw e;
            }
            scrollIntoView(node);
            robot.moveTo(node);
        }
        robot.clickOn(node);
    }

    private static boolean isOutOfView(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof BoundsLocatorException) {
                return true;
            }
        }
        return false;
    }

    /** Scrolls every enclosing ScrollPane so that the node is inside its viewport. */
    public static void scrollIntoView(Node node) {
        try {
            WaitForAsyncUtils.asyncFx(() -> {
                for (Parent p = node.getParent(); p != null; p = p.getParent()) {
                    if (!(p instanceof ScrollPane) || ((ScrollPane) p).getContent() == null) {
                        continue;
                    }
                    ScrollPane sp = (ScrollPane) p;
                    Node content = sp.getContent();
                    Bounds inContent = content.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
                    double contentHeight = content.getBoundsInLocal().getHeight();
                    double viewHeight = sp.getViewportBounds().getHeight();
                    if (inContent == null || contentHeight <= viewHeight) {
                        continue;
                    }
                    double top = inContent.getMinY() - Math.max(0, (viewHeight - inContent.getHeight()) / 2);
                    double ratio = Math.max(0, Math.min(1, top / (contentHeight - viewHeight)));
                    sp.setVvalue(sp.getVmin() + (sp.getVmax() - sp.getVmin()) * ratio);
                    sp.layout();
                }
            }).get(10, TimeUnit.SECONDS);
            WaitForAsyncUtils.waitForFxEvents();
        } catch (Exception e) {
            logger.error("Failed to scroll node into view", e);
        }
    }
    
    public void assertNodeDisabled(String id) {

        Node node = waitForNodePresent(id);

        assertThat(node)
                .as("Node not found: " + id)
                .isNotNull();

        assertThat(node.isDisable())
                .as("Expected node '" + id + "' to be disabled")
                .isTrue();
    }
    
    public void assertNodeEnabled(String id) {

        Node node = waitForNodePresent(id);

        assertThat(node)
                .as("Node not found: " + id)
                .isNotNull();

        assertThat(node.isDisable())
                .as("Expected node '" + id + "' to be enabled")
                .isFalse();
    }
    
    public Node waitForNodePresent(String id) {
        try {
            WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS, () -> robot.lookup(id).tryQuery().isPresent());
            return robot.lookup(id).query();
        } catch (TimeoutException e) {
            logger.error("Element not found within timeout: " + id, e);
            capture();
            throw new RuntimeException("Element not found within 20 sec: " + id, e);
        }
    }

	public void clickIfPresent(String id) {
		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Optional<Node> optionalNode = robot.lookup(id).tryQuery();
		if (optionalNode.isPresent()) {

			Node node = optionalNode.get();

			if (node.isVisible() && !node.isDisable()) {
				robot.moveTo(node);
				robot.clickOn(node);
			}
		}
	}
    
    public Node waitForNode(String id) {

        long startTime = System.currentTimeMillis();
        long timeout = 20_000L;

        Optional<Node> optionalNode;

        try {

            while (true) {

                optionalNode = robot.lookup(id).tryQuery();

                if (optionalNode.isPresent() && !optionalNode.get().isDisable()) {
                    return optionalNode.get();
                }

                WaitForAsyncUtils.waitForFxEvents();
                Thread.sleep(100); // prevent CPU spinning

                if (System.currentTimeMillis() - startTime > timeout) {
                    throw new RuntimeException(
                            "Element not found or not enabled within 20 sec : " + id);
                }
            }

        } catch (Exception e) {
            logger.error("Wait failed for element: " + id, e);
            capture();   // ✅ SINGLE PLACE FOR SCREENSHOT
            throw new RuntimeException(e);
        }
    }
    
    public <T extends Node> T waitForNode(String id, Class<T> type) {

        Node node = waitForNode(id); // capture already handled

        if (!type.isInstance(node)) {
            throw new RuntimeException(
                    "Node " + id + " is not of type " + type.getSimpleName()
            );
        }

        return type.cast(node);
    }

    public Node findVisibleNodeInAnyWindow(String selector) {
        Set<Node> nodes = robot.lookup(selector).queryAll();
        for (Node node : nodes) {
            if (node != null && node.isVisible() && !node.isDisable()) {
                return node;
            }
        }
        return null;
    }

    public Node waitForFirstVisibleNode(String selector, long timeoutMs) {
        try {
            WaitForAsyncUtils.waitFor(timeoutMs, TimeUnit.MILLISECONDS,
                    () -> findVisibleNodeInAnyWindow(selector) != null);
            return findVisibleNodeInAnyWindow(selector);
        } catch (TimeoutException e) {
            logger.error("Wait failed for visible element: " + selector, e);
            capture();
            throw new RuntimeException(
                    "Element not found within " + (timeoutMs / 1000) + " sec : " + selector, e);
        }
    }

    private Node findVisibleAuthPassword() {
        Set<Node> panes = robot.lookup("#pwdBasedLogin").queryAll();
        for (Node pane : panes) {
            if (pane != null && pane.isVisible() && !pane.isDisable()) {
                Node password = pane.lookup("#password");
                if (password != null && password.isVisible() && !password.isDisable()) {
                    return password;
                }
            }
        }
        return null;
    }

    public Node waitForVisibleAuthPassword(long timeoutMs) {
        try {
            WaitForAsyncUtils.waitFor(timeoutMs, TimeUnit.MILLISECONDS,
                    () -> findVisibleAuthPassword() != null);
            return findVisibleAuthPassword();
        } catch (TimeoutException e) {
            logger.error("Wait failed for operator auth password field", e);
            capture();
            throw new RuntimeException(
                    "Operator auth password field not found within " + (timeoutMs / 1000) + " sec", e);
        }
    }

    private Node findVisibleAuthUsername() {
        Set<Node> panes = robot.lookup("#pwdBasedLogin").queryAll();
        for (Node pane : panes) {
            if (pane != null && pane.isVisible() && !pane.isDisable()) {
                Node username = pane.lookup("#username");
                if (username != null && username.isVisible() && !username.isDisable()) {
                    return username;
                }
            }
        }
        return null;
    }

    public Node waitForVisibleAuthUsername(long timeoutMs) {
        try {
            WaitForAsyncUtils.waitFor(timeoutMs, TimeUnit.MILLISECONDS,
                    () -> findVisibleAuthUsername() != null);
            return findVisibleAuthUsername();
        } catch (TimeoutException e) {
            logger.error("Wait failed for operator auth username field", e);
            capture();
            throw new RuntimeException(
                    "Operator auth username field not found within " + (timeoutMs / 1000) + " sec", e);
        }
    }

    public Node waitForVisibleNodeInAnyWindow(String selector, long timeoutMs) {
        return waitForFirstVisibleNode(selector, timeoutMs);
    }

    public void clickVisibleNodeInAnyWindow(String selector, long timeoutMs) {
        Node node = waitForVisibleNodeInAnyWindow(selector, timeoutMs);
        robot.moveTo(node);
        robot.clickOn(node);
    }

    public void scrollclickNodeAssert(String id) {
    	waitForNode(id);
        node = lookupById(id);

        assertThat(robot.lookup(id).tryQuery()).isNotNull();
        // scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("proofscroll")));
        scrollVerticalDirectioncount(10);

        robot.moveTo(node);
        robot.clickOn(node);

    }

    public void scrollclickNodeAssert1(String id) {
    	waitForNode(id);
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
    

    {waitForNode(id);
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
    
    public static String capture() {
        String snapshotpath = null;
        try {
            BufferedImage image;
            if (HeadlessMode.isEnabled()) {
                image = snapshotTopWindow();
            } else {
                Robot rb = new Robot();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                Rectangle rec = new Rectangle(0, 0, screenSize.width, screenSize.height);
                image = rb.createScreenCapture(rec);
            }
            // Image myImage=SwingFXUtils.toFXImage(image, null);

            Path SNAPSHOTPATH = Paths.get(System.getProperty("user.dir"), "snapshot",
                    "snapshot" + DateUtil.getDateTime() + ".jpg");
            snapshotpath = SNAPSHOTPATH.toString();
            File snapshotFile = SNAPSHOTPATH.toFile();
            File snapshotDir = snapshotFile.getParentFile();
            if (snapshotDir != null && !snapshotDir.exists()) {
                snapshotDir.mkdirs();
            }
            ImageIO.write(image, "jpg", snapshotFile);

        } catch (Exception e) {
            logger.error("", e);

        }
        return snapshotpath;

    }

    private static BufferedImage snapshotTopWindow() throws Exception {
        WritableImage fxImage = WaitForAsyncUtils.asyncFx(() -> {
            List<Window> showing = Window.getWindows().filtered(Window::isShowing);
            Scene scene = showing.get(showing.size() - 1).getScene();
            return scene.snapshot(null);
        }).get(30, TimeUnit.SECONDS);
        BufferedImage argb = SwingFXUtils.fromFXImage(fxImage, null);
        // JPEG has no alpha channel
        BufferedImage rgb = new BufferedImage(argb.getWidth(), argb.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(argb, 0, 0, null);
        return rgb;
    }

}
