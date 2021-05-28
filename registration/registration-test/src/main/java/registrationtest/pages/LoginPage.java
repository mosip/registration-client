package registrationtest.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.web.util.WebUtils;
import org.testfx.api.FxRobot;

import com.aventstack.extentreports.Status;

import io.mosip.registration.dto.mastersync.GenericDto;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import registrationtest.controls.Alerts;
import registrationtest.pojo.output.RID;
import registrationtest.runapplication.StartApplication;
import registrationtest.utility.ComboBoxUtil;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.JsonUtil;
import registrationtest.utility.PropertiesUtil;
import  registrationtest.utility.WaitsUtil;

public class LoginPage {
	private static final Logger logger = LogManager.getLogger(LoginPage.class); 


	FxRobot robot;
	Stage applicationPrimaryStage;
	Scene scene;
	Node node;

	TextField userIdTextField;
	String userId="#userId";

	TextField passwordTextField;
	String password="#password";

	String loginScreen="#loginScreen";	
	String homeSelectionMenu="#homeSelectionMenu";
	String logout="#logout";
	String submit="#submit1";
	String success="Success";
	String exit="#exit";
	String appLanguage="#appLanguage";

	WaitsUtil waitsUtil;
	Alerts alerts;

	public LoginPage(FxRobot robot, Stage applicationPrimaryStage,Scene scene)
	{
		logger.info("LoginPage Constructor");
		this.robot=robot;
		this.applicationPrimaryStage=applicationPrimaryStage;
		this.scene=scene;
		waitsUtil=new WaitsUtil(robot);
		alerts=new Alerts(robot);
	}

	public LoginPage(FxRobot robot)
	{
		logger.info("LoginPage Constructor");

		this.robot=robot;
		waitsUtil=new WaitsUtil(robot);
		waitsUtil.clickNodeAssert( loginScreen);
	}

	public void selectAppLang()
	{
		try {
//			Platform.runLater(new Runnable() {
//				public void run() {

					GenericDto dto=new GenericDto();
					dto.setCode("fra");
					dto.setName("français");
					dto.setLangCode("fra");

					ComboBox comboBox= waitsUtil.lookupById(appLanguage);

					comboBox.getSelectionModel().select(dto); 


					try {
						Thread.sleep(5000);
						//Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ComboItemTimeWait")));
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//				}
//			});
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	public String getUserId() {
		logger.info("getUserId");

		return userIdTextField.getText();
	}

	public void setUserId(String userIdText) {
		logger.info("setUserId" +userIdText);

		try {
			
					userIdTextField=waitsUtil.lookupByIdTextField(userId, robot);

					assertNotNull(userIdTextField, "userIdTextField not present");

					//robot.clickOn(userIdTextField);

					userIdTextField.clear();
					
					userIdTextField.setText(userIdText);
						
					System.out.println("User id Entered ");
					waitsUtil.clickNodeAssert("#sub1");
					//	robot.press(KeyCode.ENTER).release(KeyCode.ENTER);
					//Thread.sleep(5000);
					assertEquals(userIdText, userIdTextField.getText(),"User id is not as same as entered");
				



		}catch(Exception e)
		{
			logger.error(e.getMessage());
		}

	}
	/**
	 * Verify HomePage after password enter
	 * @param pwd
	 * @return
	 */

	public HomePage setPassword(String pwd) {

		logger.info("setPassword" );

		try {
			
			passwordTextField=waitsUtil.lookupByIdTextField(password, robot);

			assertNotNull(passwordTextField, "passwordTextField Not Present");

			//robot.clickOn(passwordTextField);

			passwordTextField.setText(pwd);

			waitsUtil.clickNodeAssert("#sub2");
			//robot.press(KeyCode.ENTER).release(KeyCode.ENTER);

			//robot.clickOn(submit);
			
		}catch(Exception e)
		{
			logger.error(e.getMessage());
		}
		return new HomePage(robot);

	}

	/**
	 * verifyAuthentication after password enter
	 * @param pwd
	 * @return
	 * @throws InterruptedException 
	 */

	public boolean verifyAuthentication(String pwd,Stage applicationPrimaryStage)  {
		boolean flag=false;
		try
		{passwordTextField=waitsUtil.lookupByIdTextField(password, robot);

		assertNotNull(passwordTextField, "passwordTextField Not Present");

		robot.clickOn(passwordTextField);

		passwordTextField.setText(pwd);

		waitsUtil.clickNodeAssert(submit);


		Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("SyncWait")));
		waitsUtil.clickNodeAssert(success);
		waitsUtil.clickNodeAssert(exit);

		flag=true;
		}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}
		return flag;

	}


	public void loadLoginScene(Stage applicationPrimaryStage) 
	{
		logger.info("In Login test Loaded");

		try {
			waitsUtil.clickNodeAssert("Update Later");
			waitsUtil.clickNodeAssert(loginScreen);


			scene=applicationPrimaryStage.getScene();
			node=scene.lookup(loginScreen);
			while(node.isDisable())
			{Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ComboItemTimeWait"))); 

			System.out.println("Disable login screen waiting to get it on");
			}
			assertNotNull(node,"Login Page is not shown");

			ExtentReportUtil.step1.log(Status.PASS, "Successfully Screen Loaded");

		}catch(Exception e)
		{
			logger.error(e.getMessage());
		}


	}

	public void logout()
	{
		/**
		 * Click Menu
		 * Logout
		 */
		waitsUtil.clickNodeAssert( homeSelectionMenu);

		waitsUtil.clickNodeAssert(logout);


	}

}
