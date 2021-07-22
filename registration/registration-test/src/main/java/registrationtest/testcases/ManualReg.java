
package  registrationtest.testcases;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.testfx.api.FxRobot;
import org.testfx.api.FxRobotContext;

import com.aventstack.extentreports.Status;

import io.mosip.registration.dao.RegistrationDAO;
import javafx.scene.Node;
import javafx.scene.Scene;
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
import registrationtest.pojo.output.RID;
import registrationtest.pojo.schema.Root;
import registrationtest.pojo.schema.Schema;
import  registrationtest.utility.ExtentReportUtil;
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
public class ManualReg {
	private final Logger logger = LogManager.getLogger(ManualReg.class);  
	
	@Autowired
	FxRobot robot;
	Schema schema;
	Root root; 
	Scene scene;
	Node node;
	Boolean flagContinueBtnFileUpload=true;
	Boolean flagContinueBtnBioUpload=true;
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
	UpdatePage updatePage;


	public void manualRegistration(FxRobot robot,String loginUserid,String loginPwd,String supervisorUserid,
			String supervisorUserpwd,Stage applicationPrimaryStage1,ApplicationContext applicationContext)  {
		
		try {
			
			
			loginPage=new LoginPage(robot);
			buttons=new Buttons(robot);
			authenticationPage=new AuthenticationPage(robot);	
			robotActions=new RobotActions(robot);
			selectLanguagePage=new SelectLanguagePage(robot);
			updatePage=new UpdatePage(robot);

						//Load Login screen
			loginPage.loadLoginScene(applicationPrimaryStage1);
			demographicPage=new DemographicPage(robot);
			
			//Enter userid and password


			loginPage.selectAppLang();

			loginPage.setUserId(loginUserid);

			homePage=loginPage.setPassword(loginPwd);
			
			
			
			//New Registration
			homePage.clickHomeImg();
			
		}catch(Exception e)
		{
			logger.error("",e);
		}

		}		
	}

