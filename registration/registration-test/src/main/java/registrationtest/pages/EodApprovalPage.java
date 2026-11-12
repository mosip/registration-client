package registrationtest.pages;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.WaitsUtil;

public class EodApprovalPage {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EodApprovalPage.class);
    private static final long MODAL_TIMEOUT_MS = 15_000L;

    FxRobot robot;
    WaitsUtil waitsUtil;
    String filterField = "#filterField";
    String approvalBtn = "#approvalBtn";
    String rejectionBtn = "#rejectionBtn";
    String authenticateBtn = "#authenticateBtn";
    String rejectionComboBox = "#rejectionComboBox";
    String rejectionSubmit = "#rejectionSubmit";
    String webView = "#webView";

    public EodApprovalPage(FxRobot robot) {
        logger.info("EodApprovalPage Constructor");
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
    }

    public void openPendingApproval(Stage applicationPrimaryStage) {
        logger.info("openPendingApproval");
        HomePage homePage = new HomePage(robot);
        homePage.clickeodApprovalImageView(applicationPrimaryStage, null);
    }

    public void clickOnfilterField() {
        logger.info("clickOnfilterField ");
        waitsUtil.clickNodeAssert(filterField);
    }

    public void selectApplication(String rid) {
        clickOnfilterField();
        enterFilterDetails(rid);
        verifyApplicationDetailsVisible();
    }

    public void enterFilterDetails(String rid) {
        logger.info("enterFilterDetails RID " + rid);
        TextField textfield = waitsUtil.waitForNode(filterField, TextField.class);
        WaitForAsyncUtils.asyncFx(() -> textfield.setText(rid));
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void verifyApplicationDetailsVisible() {
        waitsUtil.waitForNode(webView);
        String html = readWebViewHtml();
        assertNotNull(html, "Pending Approval application details not displayed");
        assertTrue(!html.isBlank(), "Pending Approval WebView content is empty");
        ExtentReportUtil.test1.info("Pending Approval application details WebView loaded");
    }

    public void clickOnApprovalBtn() {
        logger.info("clickOnApprovalBtn ");
        waitsUtil.clickNodeAssert(approvalBtn);
    }

    public void clickOnRejectBtn() {
        logger.info("clickOnRejectBtn ");
        waitsUtil.clickNodeAssert(rejectionBtn);
    }

    public void selectRejectReason(String reason) {
        logger.info("selectRejectReason: {}", reason);
        ComboBox<String> comboBox = (ComboBox<String>) waitsUtil.waitForVisibleNodeInAnyWindow(rejectionComboBox,
                MODAL_TIMEOUT_MS);
        WaitForAsyncUtils.waitForFxEvents();
        if (reason == null || reason.isBlank()) {
            robot.interact(() -> comboBox.getSelectionModel().select(0));
        } else {
            robot.interact(() -> {
                for (String item : comboBox.getItems()) {
                    if (item != null && reason.equalsIgnoreCase(item)) {
                        comboBox.getSelectionModel().select(item);
                        return;
                    }
                }
                comboBox.getSelectionModel().select(0);
            });
        }
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void submitRejectReason() {
        logger.info("submitRejectReason");
        waitsUtil.clickVisibleNodeInAnyWindow(rejectionSubmit, MODAL_TIMEOUT_MS);
        WaitForAsyncUtils.waitForFxEvents();
    }

    public void verifyRejectMarkDisplayed() {
        waitsUtil.waitForNode(rejectionBtn);
        ExtentReportUtil.test1.info("Reject mark verified beside application in Pending Approval list");
    }

    public void verifyApproveMarkDisplayed() {
        waitsUtil.waitForNode(approvalBtn);
        ExtentReportUtil.test1.info("Approve mark verified beside application in Pending Approval list");
    }

    public void verifyApplicationInPendingApproval() {
        waitsUtil.assertNodeEnabled(approvalBtn);
        waitsUtil.assertNodeEnabled(rejectionBtn);
        verifyApplicationDetailsVisible();
        ExtentReportUtil.test1.info("Application is still available in Pending Approval list");
    }

    public void verifyApplicationNotInPendingApproval(String applicationId) {
        clickOnfilterField();
        enterFilterDetails(applicationId);
        waitsUtil.assertNodeDisabled(approvalBtn);
        waitsUtil.assertNodeDisabled(rejectionBtn);
        ExtentReportUtil.test1.info("Application removed from Pending Approval list: " + applicationId);
    }

    public AuthenticationPage clickOnAuthenticateBtn() {
        logger.info("clickOnAuthenticateBtn ");
        waitsUtil.clickNodeAssert(authenticateBtn);
        return new AuthenticationPage(robot);
    }

    private String readWebViewHtml() {
        final String[] html = new String[1];
        WaitForAsyncUtils.asyncFx(() -> {
            javafx.scene.web.WebView view = waitsUtil.lookupById(webView);
            html[0] = (String) view.getEngine().executeScript("document.body.innerHTML;");
        });
        WaitForAsyncUtils.waitForFxEvents();
        return html[0];
    }
}
