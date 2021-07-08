
package  registrationtest.testcases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testfx.api.FxRobot;
import org.testfx.api.FxRobotContext;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import io.mosip.registration.config.AppConfig;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dao.impl.RegistrationDAOImpl;
import javafx.application.Application;
import javafx.application.Platform;
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
import registrationtest.controls.Alerts;
import registrationtest.controls.Buttons;
import registrationtest.pages.AuthenticationPage;
import registrationtest.pages.BiometricUploadPage;
import registrationtest.pages.DemographicPage;
import registrationtest.pages.EodApprovalPage;
import registrationtest.pages.HomePage;
import registrationtest.pages.LoginPage;
import registrationtest.pages.SelectLanguagePage;
import registrationtest.pages.UploadPacketPage;
import registrationtest.pages.WebViewDocument;
import registrationtest.pojo.output.*;
import registrationtest.pojo.schema.Root;
import registrationtest.pojo.schema.Schema;
import registrationtest.runapplication.NewRegistrationAdultTest;
import  registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.JsonUtil;
import  registrationtest.utility.PropertiesUtil;
import registrationtest.utility.RobotActions;
import org.apache.log4j.LogManager; 

import org.apache.log4j.Logger;


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
public class LoginNewRegLogout {
	private static final Logger logger = LogManager.getLogger(LoginNewRegLogout.class);  
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
	Boolean result;
	DemographicPage demographicPage;
	BiometricUploadPage biometricUploadPage;
	Buttons buttons;
	WebViewDocument webViewDocument;
	RID rid1,rid2;
	AuthenticationPage authenticationPage;
	RobotActions robotActions;
	EodApprovalPage eodApprovalPage;
	UploadPacketPage uploadPacketPage;
	SelectLanguagePage selectLanguagePage;
	Alerts alerts;
	
	public boolean initialRegclientSet(FxRobot robot,String loginUserid,String loginPwd,Stage applicationPrimaryStage1)
	{
		boolean flag = false;
		try {
			ExtentReportUtil.ExtentSetting();
			ExtentReportUtil.test1=ExtentReportUtil.reports.createTest("Verify Initial Application start");
			ExtentReportUtil.step1=ExtentReportUtil.test1.createNode("STEP 1-Loading RegClient");
			
			loginPage=new LoginPage(robot);
			buttons=new Buttons(robot);
			authenticationPage=new AuthenticationPage(robot);	
			robotActions=new RobotActions(robot);
			webViewDocument=new WebViewDocument(robot);
			alerts=new Alerts(robot);
			rid1=new RID();
			rid2=new RID();
			result=false;

			//Load Login screen
			loginPage.loadLoginScene(applicationPrimaryStage1);
			ExtentReportUtil.step1.log(Status.PASS, "RegclientScreen Loaded");
			
			ExtentReportUtil.step2=ExtentReportUtil.test1.createNode("STEP 2-Operator Enter Details ");
			
			//Enter userid and password
			loginPage.setUserId(loginUserid);
			 flag=loginPage.verifyAuthentication(loginPwd, applicationPrimaryStage1);
			ExtentReportUtil.step2.log(Status.PASS, "Operator logs in");
			
			if(flag==true)
			{
				ExtentReportUtil.test1.log(Status.PASS, "SUCCESS Restart Application");
			}
			else
			{
				ExtentReportUtil.test1.log(Status.FAIL, "FAIL");
			}
			
		}
		catch(Exception e)
		{

			logger.error(e.getMessage());
		}
		
		return flag;
	}
	public RID newRegistrationAdult(FxRobot robot,String loginUserid,String loginPwd,String supervisorUserid,
			String supervisorUserpwd,Stage applicationPrimaryStage1,String jsonContent,String flow,String fileName
			,ApplicationContext applicationContext)  {

		try {
		logger.info("New Adult Registration Scenario : " + flow +" FileName : " + fileName);
		ExtentReportUtil.test1=ExtentReportUtil.reports.createTest("New Adult Registration Scenario : " + flow +" FileName : " + fileName);
		
		
		
		ExtentReportUtil.step1=ExtentReportUtil.test1.createNode("STEP 1-Loading Reg"
				+ "Client");
		
		loginPage=new LoginPage(robot);
		buttons=new Buttons(robot);
		authenticationPage=new AuthenticationPage(robot);	
		robotActions=new RobotActions(robot);
		selectLanguagePage=new SelectLanguagePage(robot);
		
		//Load Login screen
		loginPage.loadLoginScene(applicationPrimaryStage1);
		
		
		ExtentReportUtil.step2=ExtentReportUtil.test1.createNode("STEP 2-Operator Enter Details ");
		
		//Enter userid and password
		
		
		loginPage.selectAppLang();
		
		loginPage.setUserId(loginUserid);
		
		homePage=loginPage.setPassword(loginPwd);
		
		
		ExtentReportUtil.step2.log(Status.PASS, "Operator logs in");
		
		//New Registration
		homePage.clickHomeImg();
		if(PropertiesUtil.getKeyValue("sync").equals("Y"))
		homePage.clickSynchronizeData();
		
		demographicPage=homePage.clickNewRegistration();
		
		if(PropertiesUtil.getKeyValue("multilang").equals("Y"))
		{
		selectLanguagePage.selectLang();
		buttons.clicksubmitBtn();
		}	
		ExtentReportUtil.step3=ExtentReportUtil.test1.createNode("STEP 3-Demographic, Biometric upload ");
		
		webViewDocument=demographicPage.scemaDemoDocUploadAdult(jsonContent,flow);

		ExtentReportUtil.step3.log(Status.PASS, "Demographic, Biometric upload done");

		buttons.clicknextBtn();

		ExtentReportUtil.step4=ExtentReportUtil.test1.createNode("STEP 4-Accept Preview ");
		
		
		rid1=webViewDocument.acceptPreview(flow);

		buttons.clicknextBtn();

		if(!rid1.rid.trim().isEmpty())
			ExtentReportUtil.step4.log(Status.PASS, "Accept Preview done" + rid1.getWebviewPreview());
			else
			{	ExtentReportUtil.step4.log(Status.FAIL,"Preview not valid");	
			return rid1;
			}
		/**
		 * Authentication enter password
		 * Click Continue 
		 */
		authenticationPage.enterUserName(loginUserid);
		authenticationPage.enterPassword(loginPwd);

		buttons.clickAuthenticateBtn();


		/**
		 * Click Home, eodapprove, approval Button, authenticate button
		 * Enter user details
		 */
		rid2=webViewDocument.getacknowledgement(flow);
		
		homePage.clickHomeImg();
		
		
		
		ExtentReportUtil.step5=ExtentReportUtil.test1.createNode("STEP 5-Approve Packet ");
		
		

		eodApprovalPage=homePage.clickeodApprovalImageView( applicationPrimaryStage, scene);
		eodApprovalPage.clickOnfilterField();
		eodApprovalPage.enterFilterDetails(rid1.getRid());
		eodApprovalPage.clickOnApprovalBtn();
		authenticationPage=eodApprovalPage.clickOnAuthenticateBtn();
		authenticationPage.enterUserName(supervisorUserid);
		authenticationPage.enterPassword(supervisorUserpwd);
		authenticationPage.clicksubmitBtn();
		robotActions.clickWindow();
		homePage.clickHomeImg();	
		buttons.clickConfirmBtn();
		if(!rid2.rid.trim().isEmpty())
		{
		ExtentReportUtil.step5.log(Status.PASS, "Approve Packet done" + rid2.getWebViewAck());
		assertEquals(rid1.getRid(), rid2.getRid());
		}else
		{	ExtentReportUtil.step5.log(Status.FAIL,"Approve Packet valid");	
		return rid1;
		}
		
		/**
		 * Upload the packet
		 */
		if(PropertiesUtil.getKeyValue("upload").equals("Y"))
			{
		
		ExtentReportUtil.step6=ExtentReportUtil.test1.createNode("STEP 6-Upload Packet ");
		

		uploadPacketPage=homePage.clickuploadPacketImageView( applicationPrimaryStage, scene);
		uploadPacketPage.selectPacket(rid1.getRid());
		buttons.clickuploadBtn();
		/**
		 * Verify Success Upload
		 */
		result=uploadPacketPage.verifyPacketUpload(rid1.getRid());
		ExtentReportUtil.step6.log(Status.PASS, "Upload Packet done");

		}
		else if(PropertiesUtil.getKeyValue("upload").equals("N")){
			result=true;
		}
		//Logout Regclient
		rid1.appidrid=rid1.getAppidrid(applicationContext, rid1.rid);
		rid1.setResult(result);
				}catch(Exception e)
				{

					logger.error(e.getMessage());
				}
				
				

				try
				{
					loginPage.logout();
					buttons.clickConfirmBtn();
					
				}
				catch(Exception e)
				{
					logger.error(e.getMessage());
				}

		
		if(result==true)
		{
			ExtentReportUtil.test1.log(Status.PASS, "TESTCASE PASS\n" +"[Appid="+ rid1.rid +"] [RID="+ rid1.appidrid +"] [DATE TIME="+ rid1.ridDateTime +"] [ENVIRONMENT=" +System.getProperty("mosip.hostname")+"]");
		}		else
			ExtentReportUtil.test1.log(Status.FAIL, "TESTCASE FAIL");
		ExtentReportUtil.reports.flush();
		
				
return rid1;
	}


	public RID loginlogout(FxRobot robot2, String loginUserid, String loginPwd, String supervisorUserid,
			String supervisorUserpwd, Stage primaryStage, JSONObject idjson, HashMap<String, String> documentUpload,
			String lang, String schemaversion, String jsonObjName, String idJsonPath) throws InterruptedException {
		
		loginPage=new LoginPage(robot);
		buttons=new Buttons(robot);
		authenticationPage=new AuthenticationPage(robot);	
		robotActions=new RobotActions(robot);

		//Load Login screen
		//loadLoginScene verb
		loginPage.loadLoginScene(primaryStage);

		//Enter userid and password
		loginPage.setUserId(loginUserid);


		homePage=loginPage.setPassword(loginPwd);

		//Logout Regclient
		loginPage.logout();
		
		return null;
	}
}

