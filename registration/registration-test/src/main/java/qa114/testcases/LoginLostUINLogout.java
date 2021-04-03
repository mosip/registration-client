
package  qa114.testcases;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testfx.api.FxRobot;
import org.testfx.api.FxRobotContext;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.controller.Initialization;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import qa114.controls.Buttons;
import qa114.pages.AuthenticationPage;
import qa114.pages.BiometricUploadPage;
import qa114.pages.DemographicPage;
import qa114.pages.HomePage;
import qa114.pages.LoginPage;
import qa114.pages.WebViewDocument;
import qa114.pojo.output.*;
import qa114.pojo.schema.Root;
import qa114.pojo.schema.Schema;
import qa114.utility.JsonUtil;
import  qa114.utility.PropertiesUtil;



/***
 * 
 * @author Neeharika.Garg
 * Login and Logout RegClient
 * Steps Run this using Junit
 * First start method invokes and this will launch Registration Client and through dependency injection
 * 
 * Fxrobot will take control of primary stage and perform keyboard and mouse driven activities.
 *
 */
@ExtendWith(ApplicationExtension.class)

class LoginLostUINLogout extends Application{

	FxRobot robot;
	Schema schema;
	Root root; 
	Scene scene;
	Node node;
	Boolean flagContinueBtnFileUpload=true;
	Boolean flagContinueBtnBioUpload=true;
	private static ApplicationContext applicationContext;
	private static Stage applicationPrimaryStage;
	private static String upgradeServer = null;
	private static String tpmRequired = "Y";
	LoginPage loginPage;
	HomePage homePage;
	PropertiesUtil propertiesUtil;
	FxRobotContext context;
	Boolean result=false;
	DemographicPage demographicPage;
	BiometricUploadPage biometricUploadPage;
	Buttons buttons;
	WebViewDocument webViewDocument;
	RID rid;
	AuthenticationPage authenticationPage;

	@FXML
	private WebView webView;


	@Start
	public void start(Stage primaryStage) {

		String[] args=new String[2];
		try {
			System.setProperty("java.net.useSystemProxies", "true");
			System.setProperty("file.encoding", "UTF-8");
			io.mosip.registration.context.ApplicationContext.getInstance();
			if (args.length > 1) {
				upgradeServer = args[0];
				tpmRequired = args[1];
				io.mosip.registration.context.ApplicationContext.setTPMUsageFlag(args[1]);
			}

			applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);

			System.out.println("Automaiton Script - ApplicationContext has taken");

			Initialization initialization=new Initialization();
			initialization.setApplicationContext(applicationContext);

			applicationPrimaryStage=primaryStage;

			initialization.start(primaryStage);

			System.out.println("Automaiton Script - ApplicationPrimaryStage has started");


			primaryStage=initialization.getPrimaryStage();
			primaryStage.show();

			System.out.println("Automaiton Script - Done with Start invoke");
			context = new FxRobotContext();
			context.setPointPosition(Pos.CENTER);


		} catch(Throwable e) {
			e.printStackTrace();
		}
	}


	@ParameterizedTest
	@CsvFileSource(resources = "/login.csv" , numLinesToSkip = 1)
	void loginlogout(String userid,String password) throws Exception {

		//Set FxRobotContext
		robot=new FxRobot();
		loginPage=new LoginPage(robot);
		buttons=new Buttons(robot);
		authenticationPage=new AuthenticationPage(robot);

		//Load Login screen
		loginPage.loadLoginScene(applicationPrimaryStage);

		//Enter userid and password
		loginPage.setUserId(userid);

		homePage=loginPage.setPassword( password);

		//New Registration

		demographicPage=homePage.clicklostUINImage( applicationPrimaryStage, scene);
		//biometricUploadPage=demographicPage.scemaDemoDocUpload("SCHEMA_0.9.json","testdata.json");

		buttons.clickContinueBtn();

		webViewDocument=biometricUploadPage.biometricDetailsUpload();

		buttons.clickContinueBtn();

		rid=webViewDocument.acceptPreview();

		buttons.clickContinueBtn();


		/**
		 * Authentication enter password
		 * Click Continue 
		 */

		authenticationPage.enterPassword(password);

		buttons.clickContinueBtn();


		/**
		 * Click Home, eodapprove, approval Button, authenticate button
		 * Enter user details
		 */

		homePage.clickHomeImg();
		homePage.clickeodApprovalImageView( applicationPrimaryStage, scene);


		
		robot.clickOn("#filterField");

		robot.write(rid.getRid());
		robot.clickOn("#approvalBtn"); 	
		robot.clickOn("#authenticateBtn");

		robot.clickOn("#username");
		robot.write(userid);
		robot.clickOn("#password");
		robot.write(password);
		robot.clickOn("#submitbtn");
		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);
		robot.clickOn("#homeimg"); 	
		robot.clickOn("Confirm");

		/**
		 * Upload the packet
		 */

		homePage.clickuploadPacketImageView( applicationPrimaryStage, scene);
		robot.clickOn(rid.getRid());

		robot.press(KeyCode.TAB).release(KeyCode.TAB);
		robot.press(KeyCode.TAB).release(KeyCode.TAB);
		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);

		robot.clickOn("#uploadBtn");


		/**
		 * Verify Success Upload
		 */
		Thread.sleep(5000);
		robot.clickOn("UPLOADED");
		
		
		robot.clickOn(rid.getRid());

		robot.press(KeyCode.ALT);
		robot.press(KeyCode.F4);
		robot.release(KeyCode.F4);
		robot.release(KeyCode.ALT);



		//Logout Regclient
		loginPage.logout();
		

		robot.clickOn("Confirm");
		
		rid.setResult(result);
		assertTrue(result,"TestCase Failed");
		

	}
}

