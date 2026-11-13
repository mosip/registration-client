package registrationtest.pages;



import org.testfx.api.FxRobot;

import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import registrationtest.controls.Alerts;

import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.RobotActions;
import registrationtest.utility.WaitsUtil;

public class UploadPacketPage {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(UploadPacketPage.class);

    FxRobot robot;
    WaitsUtil waitsUtil;
    TextField filterField;
    String filterField1 = "#filterField";
    String approvalBtn = "#approvalBtn";
    String authenticateBtn = "#authenticateBtn";
    String uploaded = "PUSHED";
    String selectAll = "#selectAllCheckBox";
    RobotActions robotActions;
    Alerts alerts;

    UploadPacketPage(FxRobot robot) {
        logger.info("UploadPacketPage Constructor");
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
        robotActions = new RobotActions(robot);
        alerts=new Alerts(robot);
    }

    public void clickOnfilterField(FxRobot robot) {
        logger.info("clickOnfilterField");
        waitsUtil.clickNodeAssert(filterField1);
    }

    public void enterFilterDetails(String rid) {
        // TODO Auto-generated method stub
        logger.info("enter rid Details" + rid);
        robot.write(rid);
    }

    public void clickOnApprovalBtn(FxRobot robot) {
        logger.info("clickOnApprovalBtn");
        waitsUtil.clickNodeAssert(approvalBtn);
    }

    public AuthenticationPage clickOnAuthenticateBtn(FxRobot robot) {
        logger.info("clickOnAuthenticateBtn");
        waitsUtil.clickNodeAssert(authenticateBtn);
        return new AuthenticationPage(robot);
    }

    public void selectPacket(String rid) {

        logger.info("selectPacket" + rid);
        filterField = waitsUtil.waitForNode(filterField1, TextField.class);
        // The select-all handler records a row only when its index is inside the
        // filtered table. A search box left on one id therefore selects nothing
        // once earlier packets are still in the list.
        filterField.setText("");
        waitsUtil.clickNodeAssert(selectAll);
//		robot.moveTo(rid);
//		waitsUtil.clickNodeAssert( rid);
//		waitsUtil.clickNodeAssert("APPROVED");
//		robot.press(KeyCode.TAB).release(KeyCode.TAB);
//		robot.press(KeyCode.TAB).release(KeyCode.TAB);
//		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);

    }

    public boolean verifyPacketPresent(String rid) {
        logger.info("verifyPacketPresent {}", rid);
        try {
            filterField = waitsUtil.waitForNode(filterField1, TextField.class);
            filterField.setText(rid);
            Thread.sleep(500);
            waitsUtil.clickNodeAssert(rid);
            ExtentReportUtil.test1.pass("Application found in Application Upload task: " + rid);
            return true;
        } catch (Exception e) {
            logger.error("Application not found in Application Upload task: {}", rid, e);
            return false;
        }
    }

    public Boolean verifyPacketUpload(String rid) {
        logger.info("verifyPacketUpload" + rid);
        try {
            Node resultTable = waitsUtil.waitForVisibleNodeInAnyWindow("#resultTable", 120_000L);
            Node ridNode = findLabeled(resultTable, rid);
            Node statusNode = ridNode == null ? null : findLabeled(rowOf(ridNode), uploaded);
            if (ridNode == null || statusNode == null) {
                throw new IllegalStateException("Uploaded packet was not listed under PUSHED: " + rid);
            }
            robot.clickOn(statusNode);
            robot.clickOn(ridNode);
            return true;
        } catch (RuntimeException e) {
            logger.error("Failure Unable to upload", e);
            if (e instanceof IllegalStateException) {
                throw e;
            }
            throw new IllegalStateException("Uploaded packet was not listed under PUSHED: " + rid, e);
        } finally {
            waitsUtil.dismissAlertIfPresent();
            robotActions.closeWindow();
        }
    }

    private Node rowOf(Node node) {
        Node current = node;
        while (current != null && !(current instanceof javafx.scene.control.TableRow)) {
            current = current.getParent();
        }
        return current == null ? node : current;
    }

    private Node findLabeled(Node root, String text) {
        for (Node node : root.lookupAll("*")) {
            if (node instanceof Labeled && text.equals(((Labeled) node).getText())) {
                return node;
            }
        }
        return null;
    }

}
