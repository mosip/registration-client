
package registrationtest.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.mosip.registration.dto.mastersync.GenericDto;


import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import registrationtest.controls.Alerts;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

import java.util.List;
import java.util.Optional;

public class LoginPage {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(LoginPage.class);

    FxRobot robot;
    Stage applicationPrimaryStage;
    Scene scene;
    Node node;

    TextField userIdTextField;
    String userId = "#userId";

    TextField passwordTextField;
    String password = "#password";

    String loginScreen = "#loginScreen";
    String homeSelectionMenu = "#homeSelectionMenu";
    String logout = "#logout";
    String submit = "#submit1";
    String userOnboardMessage = "#userOnboardMessage";
    String success = "Success";
    String exit = "#exit";
    String appLanguage = "#appLanguage";

    WaitsUtil waitsUtil;
    Alerts alerts;
    BiometricUploadPage biometricUploadPage;

    public LoginPage(FxRobot robot, Stage applicationPrimaryStage, Scene scene) {
        logger.info("LoginPage Constructor");
        this.robot = robot;
        this.applicationPrimaryStage = applicationPrimaryStage;
        this.scene = scene;
        waitsUtil = new WaitsUtil(robot);
        alerts = new Alerts(robot);
        biometricUploadPage = new BiometricUploadPage(robot);

    }

    public LoginPage(FxRobot robot) {
        logger.info("LoginPage Constructor");

        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
        waitsUtil.clickNodeAssert(loginScreen);
        biometricUploadPage = new BiometricUploadPage(robot);
    }

    public void selectAppLang() {
        try {
           String str = PropertiesUtil.getKeyValue("appLanguage");
           Platform.runLater(() -> {

        	   ComboBox<GenericDto> comboBox = waitsUtil.lookupById(appLanguage);

        	    Optional<GenericDto> op = comboBox.getItems().stream()
        	    		.filter(i -> i.getName().equalsIgnoreCase(str))
        	            .findFirst();

        	    comboBox.getSelectionModel().select(
        	            op.orElse(comboBox.getItems().get(0)));
        	});

        	WaitForAsyncUtils.waitForFxEvents();
        } catch (Exception e) {

            logger.error("", e);
        }
    }

    public String getUserId() {
        logger.info("getUserId");

        return userIdTextField.getText();
    }

    public void setUserId(String userIdText) {
        logger.info("setUserId" + userIdText);

        try {

        	userIdTextField = waitsUtil.waitForNode(userId, TextField.class);
        	assertNotNull(userIdTextField, "userIdTextField not present");

        	Platform.runLater(() -> {
        	    userIdTextField.clear();
        	    userIdTextField.setText(userIdText);
        	});

        	org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
            waitsUtil.clickNodeAssert("#sub1");

            assertEquals(userIdText, userIdTextField.getText(), "User id is not as same as entered");

        } catch (Exception e) {
            logger.error("", e);
        }

    }

    /**
     * Verify HomePage after password enter
     * 
     * @param pwd
     * @return
     */

    public HomePage setPassword(String pwd) {

        logger.info("setPassword");

        try {

            passwordTextField = waitsUtil.waitForNode(password, TextField.class);

            assertNotNull(passwordTextField, "passwordTextField Not Present");

            passwordTextField.setText(pwd);

            waitsUtil.clickNodeAssert("#sub2");

        } catch (Exception e) {
            logger.error("", e);
        }
        return new HomePage(robot);

    }

    /**
     * verifyAuthentication after password enter
     * 
     * @param pwd
     * @return
     * @throws InterruptedException
     */

    public boolean verifyAuthentication(String pwd, Stage applicationPrimaryStage) {
        boolean flag = false;
        try {
            passwordTextField = waitsUtil.waitForNode(password, TextField.class);

            assertNotNull(passwordTextField, "passwordTextField Not Present");

            passwordTextField.setText(pwd);

            // if home else fail or check the #context

            waitsUtil.clickNodeAssert("#sub2");
    
            waitsUtil.clickNodeAssert(success);

            flag = true;
        } catch (Exception e) {
            logger.error("", e);
        }
        return flag;

    }

    public boolean verifyOnbard(String pwd, String identity) {
        boolean flag = false;
        try {
            passwordTextField = waitsUtil.waitForNode(password, TextField.class);

            assertNotNull(passwordTextField, "passwordTextField Not Present");

            passwordTextField.setText(pwd);

            // if home else fail or check the #context

            waitsUtil.clickNodeAssert("#sub2");

            waitsUtil.clickNodeAssert("#homeImgView");
            flag = true;
            try {
                Node node = waitsUtil.lookupById("#getOnboardedPane");
                if (node.isVisible())
                    flag = verifyOnboardBio(identity);
            } catch (Exception e) {
                logger.error("", e);
               
            }
           
        }catch (Exception e) {
        	logger.error("", e);
            flag = false;
		}
        return flag;

    }

    public boolean verifyOnboardBio(String identity) {
        boolean flag = false;
        try {
            waitsUtil.clickNodeAssert("#getOnboardedPane");

            List<String> str = biometricUploadPage.bioAttributeList(identity);

            biometricUploadPage.newRegbioUpload("applicant", str, "#", identity, "ADULT");

            waitsUtil.clickNodeAssert("#continueBtn");


            waitsUtil.clickNodeAssert(userOnboardMessage);
            flag = true;
        } catch (Exception e) {
        	logger.error("", e);
            flag = false;
		}
        return flag;

    }

    public void loadLoginScene(Stage applicationPrimaryStage) {
        logger.info("In Login test Loaded");

        try {
            // alerts.clickAlertCancel();

            waitsUtil.clickNodeAssert(loginScreen);

            scene = applicationPrimaryStage.getScene();
            node = scene.lookup(loginScreen);
            node = scene.lookup(loginScreen);
            assertNotNull(node, "Login Page is not shown");

            long startTime = System.currentTimeMillis();
            long timeout = 20000; // 20 seconds

            while (node.isDisable()) {

                org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

                if (System.currentTimeMillis() - startTime > timeout) {
                    throw new RuntimeException("Login screen stayed disabled too long");
                }
            }
            ExtentReportUtil.test1.info("Successfully Screen Loaded");

		} catch (Exception e) {
			logger.error("Failed to load login scene", e);
			throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
		}

    }

    public void logout() {
        /**
         * Click Menu Logout
         */
        waitsUtil.clickNodeAssert(homeSelectionMenu);

        waitsUtil.clickNodeAssert(logout);

    }

}
