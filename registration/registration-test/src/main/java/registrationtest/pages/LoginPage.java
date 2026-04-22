
package registrationtest.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.mosip.registration.dto.mastersync.GenericDto;


import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import javafx.stage.Window;
import registrationtest.controls.Alerts;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    String success = "#success";
    String failed = "#Failed";
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
    
	public void setInvalidUserId(String userIdText) {
		logger.info("setInvalidUserId: " + userIdText);
		try {
			userIdTextField = waitsUtil.waitForNode(userId, TextField.class);
			assertNotNull(userIdTextField, "userIdTextField not present");
			Platform.runLater(() -> {
				userIdTextField.clear();
				userIdTextField.setText(userIdText + "invalid");
			});
			org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
			waitsUtil.clickNodeAssert("#sub1");
			waitsUtil.waitForNode("#context");
			ExtentReportUtil.test1.info("Entered invalid user id: " + userIdText + "invalid");
			assertEquals(userIdText + "invalid", userIdTextField.getText(), "User id text mismatch");
			ExtentReportUtil.test1.pass("Invalid user id validation successful");
			waitsUtil.clickNodeAssert("#exit");
		} catch (AssertionError ae) {
			ExtentReportUtil.test1.fail("Assertion Failed: " + ae.getMessage());
			throw ae; // VERY IMPORTANT
		} catch (Exception e) {
			logger.error("", e);
			ExtentReportUtil.test1.fail("Exception occurred: " + e.getMessage());
			throw e;
		}
	}
	
	public void submitWithoutUserId() {
		logger.info("submitWithoutUserId");
		try {
			userIdTextField = (TextField) waitsUtil.waitForNode(userId);
			assertNotNull(userIdTextField, "userIdTextField not present");
			Platform.runLater(() -> {
				userIdTextField.clear();
			});
			org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
			waitsUtil.clickNodeAssert("#sub1");
			Node contextNode = waitsUtil.waitForNode("#context");
			assertNotNull(contextNode, "Context node not present");
			Label contextLabel = (Label) contextNode;
			String actualMessage = contextLabel.getText();
			logger.info("Popup message: " + actualMessage);
			ExtentReportUtil.test1.info("Popup message displayed: " + actualMessage);
			assertEquals("UserName is required. ", actualMessage, "Validation message mismatch");
			ExtentReportUtil.test1.pass("Empty username validation successful");
			waitsUtil.clickNodeAssert("#exit");
		} catch (AssertionError ae) {
			ExtentReportUtil.test1.fail("Assertion Failed: " + ae.getMessage());
			throw ae;
		} catch (Exception e) {
			logger.error("", e);
			ExtentReportUtil.test1.fail("Exception occurred: " + e.getMessage());
			throw e;
		}
	}
	
	public static void printAllIds(){
		try{
			Platform.runLater(()->{

				System.out.println("===== PRINTING IDS FROM ALL WINDOWS =====");

				for(Window window:Window.getWindows()){

					if(window.isShowing()){

						Scene scene=window.getScene();

						if(scene!=null){

							Parent root=scene.getRoot();

							printIdsFromNode(root);
						}
					}
				}

			});

			org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

		}catch(Exception e){
			logger.error("",e);
		}
	}

	private static void printIdsFromNode(Node node){

		String id=node.getId();
		String text="";

		if(node instanceof Labeled){
			text=((Labeled)node).getText();
		}
		else if(node instanceof TextInputControl){
			text=((TextInputControl)node).getText();
		}

		// Print only useful elements
		if(id!=null || (text!=null && !text.isEmpty())){
			System.out.println(
				"id="+id+
				" | text="+text+
				" | type="+node.getClass().getSimpleName()
			);
		}

		if(node instanceof Parent){
			for(Node child:((Parent)node).getChildrenUnmodifiable()){
				printIdsFromNode(child);
			}
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
    
    public void setInvalidPassword(String pwdText){
    	logger.info("setInvalidPassword: "+pwdText);
    	try{
    		passwordTextField=waitsUtil.waitForNode(password,TextField.class);
    		assertNotNull(passwordTextField,"passwordTextField not present");
    		Platform.runLater(()->{
    			passwordTextField.clear();
    			passwordTextField.setText(pwdText+"invalid");
    		});
    		org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
    		waitsUtil.clickNodeAssert("#sub2");
    		waitsUtil.waitForNode("#context");
    		ExtentReportUtil.test1.info("Entered invalid password: "+pwdText+"invalid");
    		assertEquals(pwdText+"invalid",passwordTextField.getText(),"Password text mismatch");
    		ExtentReportUtil.test1.pass("Invalid password validation successful");
    		waitsUtil.clickNodeAssert("#exit");
    	}catch(AssertionError ae){
    		ExtentReportUtil.test1.fail("Assertion Failed: "+ae.getMessage());
    		throw ae;
    	}catch(Exception e){
    		logger.error("",e);
    		ExtentReportUtil.test1.fail("Exception occurred: "+e.getMessage());
    		throw e;
    	}
    }
    
	public void submitWithoutPassword() {
		logger.info("submitWithoutPassword");
		try {
			passwordTextField = (TextField) waitsUtil.waitForNode(password);
			assertNotNull(passwordTextField, "passwordTextField not present");
			Platform.runLater(() -> {
				passwordTextField.clear();
			});
			org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
			waitsUtil.clickNodeAssert("#sub2");
			Node contextNode = waitsUtil.waitForNode("#context");
			assertNotNull(contextNode, "Context node not present");
			Label contextLabel = (Label) contextNode;
			String actualMessage = contextLabel.getText();
			logger.info("Popup message: " + actualMessage);
			ExtentReportUtil.test1.info("Popup message displayed: " + actualMessage);
			assertEquals("Password is required. ", actualMessage, "Password validation message mismatch");
			ExtentReportUtil.test1.pass("Empty password validation successful");
			waitsUtil.clickNodeAssert("#exit");
		} catch (AssertionError ae) {
			ExtentReportUtil.test1.fail("Assertion Failed: " + ae.getMessage());
			throw ae;
		} catch (Exception e) {
			logger.error("", e);
			ExtentReportUtil.test1.fail("Exception occurred: " + e.getMessage());
			throw e;
		}
	}
	
	public void verifyBackButtonFunctionality() {
		logger.info("verifyBackButtonFunctionality");
		try {
			waitsUtil.clickNodeAssert("#back");
			ExtentReportUtil.test1.info("Clicked BACK button");
			Node submitNode = waitsUtil.waitForNode("#sub1");
			assertNotNull(submitNode, "Submit button not visible after clicking BACK");
			ExtentReportUtil.test1.info("Navigated back to previous screen successfully");
			waitsUtil.clickNodeAssert("#sub1");
			ExtentReportUtil.test1.pass("Back button functionality verified successfully");
		} catch (AssertionError ae) {
			ExtentReportUtil.test1.fail("Assertion Failed: " + ae.getMessage());
			throw ae;
		} catch (Exception e) {
			logger.error("", e);
			ExtentReportUtil.test1.fail("Exception occurred: " + e.getMessage());
			throw e;
		}
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
            Thread.sleep(15000);
            waitsUtil.clickNodeAssert(success);

            flag = true;
        } catch (Exception e) {
            logger.error("", e);
        }
        return flag;

    }

    public boolean verifyOnbard(String identity) {
        boolean flag = false;
        try {
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
