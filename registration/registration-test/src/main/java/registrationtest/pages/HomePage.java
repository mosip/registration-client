package registrationtest.pages;



import org.testfx.api.FxRobot;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import registrationtest.controls.Alerts;

import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class HomePage {

    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(HomePage.class);
    FxRobot robot;
    Stage applicationPrimaryStage;
    Scene scene;

    Button button;

    WaitsUtil waitsUtil;
    Node node;
    Alerts alerts;
    String homeimg = "#homeImgView";
    String exit = "#exit";
    String success = "#context";

    // operationalTasks
    String syncDataImageView = "#syncDataImageView", downloadPreRegDataImageView = "#downloadPreRegDataImageView",
            updateOperatorBiometricsImageView = "updateOperatorBiometricsImageView",
            uploadPacketImageView = "#uploadPacketImageView", remapImageView = "#remapImageView",
            checkUpdatesImageView = "#checkUpdatesImageView";

    // registrationTasks
    String newRegImage = "#NEW", uinUpdateImage = "#UPDATE", lostUINImage = "#LOST",
            biocorrectionImage = "#BIOMETRIC_CORRECTION";

    // eodProcesses
    String eodApprovalImageView = "#eodApprovalImageView", reRegistrationImageView = "#reRegistrationImageView",
            viewReportsImageView = "#viewReportsImageView";

    public HomePage(FxRobot robot, Stage applicationPrimaryStage, Scene scene) {
        this.robot = robot;
        this.applicationPrimaryStage = applicationPrimaryStage;
        this.scene = scene;
        waitsUtil = new WaitsUtil(robot);

        alerts = new Alerts(robot);

    }

    public HomePage(FxRobot robot) {
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
        waitsUtil.clickNodeAssert(homeimg);
    }

    public void clickHomeImg() {
        waitsUtil.clickNodeAssert(homeimg);
    }

    public void clickSynchronizeData() {
        try {
            waitsUtil.clickNodeAssert(syncDataImageView);
            Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("SyncWait")));

            waitsUtil.clickNodeAssert(success);
            waitsUtil.clickNodeAssert(exit);

        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }

    }

    public void clickdownloadPreRegDataImageView(Stage applicationPrimaryStage, Scene scene)

    {
        try {
            waitsUtil.clickNodeAssert(downloadPreRegDataImageView);
        } catch (Exception e) {
            logger.error("", e);
        }

    }

    public void clickupdateOperatorBiometricsImageView(Stage applicationPrimaryStage, Scene scene) {
        try {
            waitsUtil.clickNodeAssert(updateOperatorBiometricsImageView);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public UploadPacketPage clickuploadPacketImageView(Stage applicationPrimaryStage, Scene scene) {
        try {
            waitsUtil.clickNodeAssert(uploadPacketImageView);
        } catch (Exception e) {
            logger.error("", e);
        }
        return new UploadPacketPage(robot);
    }

    public void clickremapImageView(Stage applicationPrimaryStage, Scene scene) {
        try {
            waitsUtil.clickNodeAssert(remapImageView);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void clickcheckUpdatesImageView(Stage applicationPrimaryStage, Scene scene) {
        try {
            waitsUtil.clickNodeAssert(checkUpdatesImageView);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public DemographicPage clickNewRegistration() {
        try {

            waitsUtil.clickNodeAssert(newRegImage);
        } catch (Exception e) {
            logger.error("", e);
        }
        return new DemographicPage(robot);
    }

    public void clickuinUpdateImage() {
        try {
            waitsUtil.clickNodeAssert(uinUpdateImage);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void clickbioCorrectionImage() {
        try {
            waitsUtil.clickNodeAssert(biocorrectionImage);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public DemographicPage clicklostUINImage() {
        try {
            waitsUtil.clickNodeAssert(lostUINImage);
        } catch (Exception e) {
            logger.error("", e);
        }
        return new DemographicPage(robot);
    }

    public EodApprovalPage clickeodApprovalImageView(Stage applicationPrimaryStage, Scene scene) {
        try {
            waitsUtil.clickNodeAssert(eodApprovalImageView);
        } catch (Exception e) {
            logger.error("", e);
        }
        return new EodApprovalPage(robot);
    }

    public void clickreRegistrationImageView(Stage applicationPrimaryStage, Scene scene) {
        try {
            waitsUtil.clickNodeAssert(reRegistrationImageView);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void clickviewReportsImageView(Stage applicationPrimaryStage, Scene scene) {
        try {
            waitsUtil.clickNodeAssert(viewReportsImageView);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

}
