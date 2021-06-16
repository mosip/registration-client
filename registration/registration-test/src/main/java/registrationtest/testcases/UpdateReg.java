
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
import registrationtest.pages.UpdatePage;
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
import registrationtest.utility.WaitsUtil;

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
public class UpdateReg {
	private static final Logger logger = LogManager.getLogger(UpdateReg.class);  
	
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
	SelectLanguagePage selectLanguagePage;
	Alerts alerts;
	UpdatePage updatePage;
	

	public RID updateRegistration(FxRobot robot,String loginUserid,String loginPwd,String supervisorUserid,
			String supervisorUserpwd,Stage applicationPrimaryStage1,String jsonContent,String flow,String fileName
			)  {
		try {
		
		ExtentReportUtil.test1=ExtentReportUtil.reports.createTest("New Adult Registration Scenario : " + flow +" FileName : " + fileName);
		ExtentReportUtil.step1=ExtentReportUtil.test1.createNode("STEP 1-Loading Reg"
				+ "Client");
		
		loginPage=new LoginPage(robot);
		buttons=new Buttons(robot);
		authenticationPage=new AuthenticationPage(robot);	
		robotActions=new RobotActions(robot);
		selectLanguagePage=new SelectLanguagePage(robot);
		updatePage=new UpdatePage(robot);
		//Load Login screen
		loginPage.loadLoginScene(applicationPrimaryStage1);
		demographicPage=new DemographicPage(robot);
		
		ExtentReportUtil.step2=ExtentReportUtil.test1.createNode("STEP 2-Operator Enter Details ");
		
		//Enter userid and password
		
		
		loginPage.selectAppLang();
		
		loginPage.setUserId(loginUserid);
		
		homePage=loginPage.setPassword(loginPwd);
		
		
		ExtentReportUtil.step2.log(Status.PASS, "Operator logs in");
		
		homePage.clickuinUpdateImage();
		selectLanguagePage.selectLang();
		buttons.clicksubmitBtn();
		
		updatePage.enterUinId("5931539704");
		updatePage.selectRadioButton(robot,jsonContent);
		buttons.clickContinueBtn();
		
		
		webViewDocument=demographicPage.scemaDemoDocUploadAdult(jsonContent,flow);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return rid;

}}

