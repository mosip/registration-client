
package  registrationtest.testcases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testfx.api.FxRobot;
import org.testfx.api.FxRobotContext;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

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
import registrationtest.controls.Buttons;
import registrationtest.pages.AuthenticationPage;
import registrationtest.pages.BiometricUploadPage;
import registrationtest.pages.DemographicPage;
import registrationtest.pages.EodApprovalPage;
import registrationtest.pages.HomePage;
import registrationtest.pages.LoginPage;
import registrationtest.pages.UploadPacketPage;
import registrationtest.pages.WebViewDocument;
import registrationtest.pojo.output.*;
import registrationtest.pojo.schema.Root;
import registrationtest.pojo.schema.Schema;
import  registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.JsonUtil;
import  registrationtest.utility.PropertiesUtil;
import registrationtest.utility.RobotActions;



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
public class LostUINLogout {

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
	RID rid,rid2;
	AuthenticationPage authenticationPage;
	RobotActions robotActions;
	EodApprovalPage eodApprovalPage;
	UploadPacketPage uploadPacketPage;
	
	
	public RID LostUINAdult(FxRobot robot,String loginUserid,String loginPwd,String supervisorUserid,
			String supervisorUserpwd,Stage applicationPrimaryStage1,String jsonIdentity,HashMap<String, String> documentUpload
			)  {

		try {
		ExtentReportUtil.test2=ExtentReportUtil.reports.createTest("Lost UIN Scenario");
		
		
		
		ExtentReportUtil.step1=ExtentReportUtil.test2.createNode("STEP 1-Loading RegClient");
		
		loginPage=new LoginPage(robot);
		buttons=new Buttons(robot);
		authenticationPage=new AuthenticationPage(robot);	
		robotActions=new RobotActions(robot);
		
		//Load Login screen
		loginPage.loadLoginScene(applicationPrimaryStage1);
		
		
		ExtentReportUtil.step2=ExtentReportUtil.test2.createNode("STEP 2-Operator Enter Details ");
		
		//Enter userid and password
		loginPage.setUserId(loginUserid);
		homePage=loginPage.setPassword(loginPwd);
		ExtentReportUtil.step2.log(Status.PASS, "Operator logs in");
		
		//New Registration
		homePage.clickHomeImg();
		homePage.clickSynchronizeData();
		
		demographicPage=homePage.clicklostUINImage();
		
		ExtentReportUtil.step3=ExtentReportUtil.test2.createNode("STEP 3-Demographic, Biometric upload ");
		
		webViewDocument=demographicPage.scemaDemoDocUploadLostUIN(jsonIdentity,documentUpload);
		

		ExtentReportUtil.step3.log(Status.PASS, "Demographic, Biometric upload done");

		//buttons.clickContinueBtn();

		//webViewDocument=biometricUploadPage.bioUpload(list);

		buttons.clickContinueBtn();

		ExtentReportUtil.step4=ExtentReportUtil.test2.createNode("STEP 4-Accept Preview ");
		
		
		rid=webViewDocument.acceptPreview(); //return thread and wait on thread

		buttons.clickContinueBtn();

		ExtentReportUtil.step4.log(Status.PASS, "Accept Preview done"  + rid.getWebviewPreview());

		/**
		 * Authentication enter password
		 * Click Continue 
		 */

		authenticationPage.enterPassword(loginPwd);

		buttons.clickContinueBtn();


		/**
		 * Click Home, eodapprove, approval Button, authenticate button
		 * Enter user details
		 */

		rid2=webViewDocument.getacknowledgement();
		homePage.clickHomeImg();
		
		
		
		ExtentReportUtil.step5=ExtentReportUtil.test2.createNode("STEP 5-Approve Packet ");
		
		

		eodApprovalPage=homePage.clickeodApprovalImageView( applicationPrimaryStage, scene);
		eodApprovalPage.clickOnfilterField();
		eodApprovalPage.enterFilterDetails(rid.getRid());
		eodApprovalPage.clickOnApprovalBtn();
		authenticationPage=eodApprovalPage.clickOnAuthenticateBtn();
		authenticationPage.enterUserName(supervisorUserid);
		authenticationPage.enterPassword(supervisorUserpwd);
		authenticationPage.clicksubmitBtn();
		robotActions.clickWindow();
		homePage.clickHomeImg();	
		buttons.clickConfirmBtn();
		ExtentReportUtil.step5.log(Status.PASS, "Approve Packet done"  + rid2.getWebViewAck());

		assertEquals(rid.getRid(), rid2.getRid());
		/**
		 * Upload the packet
		 */
		
		ExtentReportUtil.step6=ExtentReportUtil.test2.createNode("STEP 6-Upload Packet ");
		

		uploadPacketPage=homePage.clickuploadPacketImageView( applicationPrimaryStage, scene);
		uploadPacketPage.selectPacket(rid.getRid());
		buttons.clickuploadBtn();
		/**
		 * Verify Success Upload
		 */
		result=uploadPacketPage.verifyPacketUpload(rid.getRid());


		//Logout Regclient
		loginPage.logout();


		buttons.clickConfirmBtn();

	
		rid.setResult(result);
		
		if(result==true)
		{ExtentReportUtil.step6.log(Status.PASS, "Upload Packet done");

			ExtentReportUtil.test2.log(Status.PASS, "TESTCASE PASS\n" +" RID-"+ rid.rid +" DATE TIME-"+ rid.ridDateTime +" ENVIRONMENT-" +System.getProperty("mosip.hostname"));
		}		else {
			ExtentReportUtil.step6.log(Status.FAIL, "Upload Fail");

			ExtentReportUtil.test2.log(Status.FAIL, "TESTCASE FAIL");
		}
		//assertTrue(result,"TestCase Failed");
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
return rid;
	}
}

