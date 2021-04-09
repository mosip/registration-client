package  registrationtest.testcases;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testfx.api.FxRobot;
import org.testfx.api.FxRobotContext;
import org.testfx.api.FxRobotException;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

import io.mosip.registration.config.AppConfig;
import io.mosip.registration.controller.Initialization;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import registrationtest.pages.DemographicPage;
import registrationtest.pages.HomePage;
import registrationtest.pages.LoginPage;
import registrationtest.pojo.schema.Root;
import registrationtest.pojo.schema.Schema;
import registrationtest.runapplication.StartApplication;
import registrationtest.utility.JsonUtil;
import  registrationtest.utility.PropertiesUtil;



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

public class LoginLogoutTest{

	static FxRobot robot;
	Schema schema;
	Root root; 
	Scene scene;
	Node node;
	Boolean flagContinueBtnFileUpload=true;
	Boolean flagContinueBtnBioUpload=true;
	private static Stage applicationPrimaryStage;

	LoginPage loginPage;
	HomePage homePage;
	PropertiesUtil propertiesUtil;
	FxRobotContext context;
	Boolean result=false;
	@FXML
	private WebView webView;


	@ParameterizedTest
	@CsvFileSource(resources = "/login.csv" , numLinesToSkip = 1)
	void loginlogout(String userid,String password) throws Exception {

		//Set FxRobotContext
		//robot=new FxRobot();

		loginPage=new LoginPage(robot);

		//Load Login screen

		loginPage.loadLoginScene(applicationPrimaryStage);

		//Enter userid and password
		loginPage.setUserId(userid);

		homePage=loginPage.setPassword( password);


		//Logout Regclient
		loginPage.logout();

		assertTrue(result,"TestCase Failed");
		//return result;

	}

	public static void main(String[] args) throws InterruptedException { //1

		robot=new FxRobot();
		LoginLogoutTest lg=new LoginLogoutTest();  
		
		Thread thread4 = new Thread() { //3 parallel running
			@Override
			public void run() {
				try {
					System.out.println("thread4 inside calling testcase"); 
					Thread.sleep(120000); //Needed
					robot=new FxRobot();
					LoginLogoutTest lg=new LoginLogoutTest();
					
					lg.loginlogout("110123", "mosip123");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}

		};
		
		thread4.start();
		Application.launch(StartApplication.class, args);//2 infine loop never yeild

	}	}





