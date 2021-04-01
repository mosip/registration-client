package  qa114.testcases;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;


import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.api.FxRobotContext;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.CsvFileSource;
import org.testfx.service.query.NodeQuery;
import org.testfx.util.WaitForAsyncUtils;
import org.testfx.util.NodeQueryUtils;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.RegistrationPreviewController;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.dto.mastersync.GenericDto;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import net.logstash.logback.encoder.com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import qa114.controls.Buttons;
import qa114.pages.DemographicPage;

import qa114.pages.HomePage;
import qa114.pages.LoginPage;
import qa114.pages.WebViewDocument;

import qa114.pojo.output.*;
import qa114.pojo.schema.Root;
import qa114.pojo.schema.Schema;
import qa114.pojo.testdata.RootTestData;
import qa114.pojo.testdata.TestData;
import qa114.utility.JsonUtil;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventTarget;




/***
 * 
 * @author Neeharika.Garg
 * This Script contains New Registration Flow Packet creation till approval
 * Steps Run this using Junit
 * First start method invokes and this will launch Registration Client and through dependency injection
 * Fxrobot will take control of primary stage and perform keyboard and mouse driven activities.
 *
 */
@ExtendWith(ApplicationExtension.class)

class NewRegistrationMethods extends Application{

	FxRobot robot;
	Schema schema;
	TestData testdata;
	Root root; 
	Scene scene;
	Node node;
	Boolean flagContinueBtnFileUpload=true;
	Boolean flagContinueBtnBioUpload=true;
	Boolean flagContinueBtnDemograph=true;
	private static ApplicationContext applicationContext;
	private static Stage applicationPrimaryStage;
	private static String upgradeServer = null;
	private static String tpmRequired = "Y";
	LoginPage loginPage;
	HomePage homePage;
	Buttons buttons;
	RID result;
	FxRobotContext context;
	RootTestData datavalue;
	String RIDResult="";


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


			System.out.println("Automaiton Script - Done with Start invoke");
			context = new FxRobotContext();
			context.setPointPosition(Pos.CENTER);
			robot=new FxRobot(); 


		} catch(Throwable e) {
			e.printStackTrace();
		}
	}







	@ParameterizedTest
	@CsvFileSource(resources = "/login.csv" , numLinesToSkip = 1)
	void NewRegistrationTest(String userid,String password) throws Exception {

		NewRegistrationTestCall(userid,password);
	}





	void NewRegistrationTestCall(String userid, String password) throws Exception {


		/**
		 * Initialize Robot and FXContext.
		 */

		robot=new FxRobot(); 

		loginPage=new LoginPage(robot,applicationPrimaryStage,scene);
		System.out.println("In Login test Loaded");
		loginPage.loadLoginScene(applicationPrimaryStage);

		/**
		 * userid,password Picked from CSV
		 * 
		 */

		loginPage.setUserId(userid);
		loginPage.setPassword( password);


		/**
		 * Click on New Registration
		 * 
		 */
		homePage=new HomePage(robot,applicationPrimaryStage,scene);
		buttons=new Buttons(robot);

		homePage.clickNewRegistration();




		/**
		 * Read Schema json
		 * Load json into pojo classes
		 * Root class is the main flow for schema
		 * Using Schema till Document upload
		 */

		schemaRead();


		/**
		 * 
		 * Upload Bio metric docs and click continue once enable
		 */

		Thread.sleep(2000); 
		biometricDetailsUpload();


		/**
		 * HTML Review page
		 * Approve ---
		 * click continue
		 */
		WebViewDocument webViewDocument=new WebViewDocument(robot, applicationPrimaryStage, scene);
		
			//result=webViewDocument.AcceptPreviewPickRID( applicationPrimaryStage, scene);
		
		
		String s=webViewDocument.acceptPreviewPickRID( applicationPrimaryStage, scene);
		
		System.out.println(s);
		//webViewDocument.AcceptPreviewPickRID( applicationPrimaryStage, scene);
		
		
		
			//RIDResult=result.getRid().toString();
			
	//		System.out.println(result.rid);
	
	

		buttons.clickContinueBtn();

		/**
		 * Authentication enter password
		 * Click Continue 
		 */

		robot.clickOn("#password"); 
		robot.write(password);
		buttons.clickContinueBtn();

		/**
		 * Click Home, eodapprove, approval Button, authenticate button
		 * Enter user details
		 */

		robot.clickOn("#homeimg"); 	
		robot.clickOn("#eodApprovalImageView"); 
		
		//TextField filterField=robot.lookup("#filterField").queryAs(TextField.class);
		robot.clickOn("#filterField");
				
		robot.write(s);
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
		robot.clickOn(s);

		robot.press(KeyCode.TAB).release(KeyCode.TAB);
		robot.press(KeyCode.TAB).release(KeyCode.TAB);
		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);

		robot.clickOn("#uploadBtn");


		/**
		 * Verify Success Upload
		 */
Thread.sleep(5000);
		robot.clickOn("UPLOADED");
		robot.clickOn(s);
//	
//		Platform.runLater(() -> {
//
//
//
//			TableView<PacketStatusDTO> statusTable = robot.lookup("#resultTable").queryTableView();
//			
//				statusTable.getSelectionModel().
//			
//		});
//		
		
//		System.out.println("Successfuly " + result.getRid() + " UPLOADED");	
//		
//		ActionEvent event = null;
//        ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();   
//		
		robot.press(KeyCode.ALT);
		robot.press(KeyCode.F4);
		robot.release(KeyCode.F4);
		robot.release(KeyCode.ALT);

	

		/**
		 * Click Menu
		 * Logout
		 */
		robot.clickOn("#homeSelectionMenu");

		robot.clickOn("#logout");
		robot.clickOn("Confirm");	


		assertTrue(true);
		System.out.println("PASSED");
	}


	private void schemaRead() throws IOException, TimeoutException {


		String schemafile = System.getProperty("user.dir")+"\\SCHEMA_0.9.json";
		String testdatafile = System.getProperty("user.dir")+"\\src\\test\\resources\\testdata.json";


		String jsonFromSchema = Files.readString(Paths.get(schemafile));
		System.out.println("Automaiton Script - Printing Json" + jsonFromSchema);
		root = JsonUtil.convertJsonintoJava(jsonFromSchema, Root.class);

		System.out.println(root.getId() + " " + root.getIdVersion());

		String jsonfromdata = Files.readString(Paths.get(testdatafile));
		System.out.println("Automaiton Script - Printing Json" + jsonfromdata);
		datavalue = JsonUtil.convertJsonintoJava(jsonfromdata, RootTestData.class);

		System.out.println(datavalue.getTestData());



		System.out.println("Automaiton Script - Schema ID Follows :- ");

		for (int i = 2; i < root.getSchema().size()-1; i++) {
			if((i==10)||(i==16)||(i==20)||(i==25)||(i==30)||(i==35))  // Tried these (i%5==0) //((i>10)&&(i%5==0)) 
				robot.scroll(10, VerticalDirection.DOWN);
			schema = root.getSchema().get(i);
			System.out.println("Automaiton Script - id="+i + "=" +schema.getId() + "\tSchemaControlType=" + schema.getControlType());

			String id="#"+schema.getId(); 

			for (int j = 0; j < datavalue.getTestData().size()-1; j++) {
				testdata = datavalue.getTestData().get(j);
				if(schema.getId().equals(testdata.getId()))
				{

					try {
						switch (schema.getControlType()) {
						case "textbox":
							DemographicPage demo=new DemographicPage(robot)	;
							demo.setTextFields( id, testdata.getValue());
							break;
						case "ageDate":
							String date[]=testdata.getValue().split("-");
							robot.press(KeyCode.TAB).release(KeyCode.TAB);
							robot.write(date[0]);
							robot.press(KeyCode.TAB).release(KeyCode.TAB);
							robot.write(date[1]);
							robot.press(KeyCode.TAB).release(KeyCode.TAB);
							robot.write(date[2]);
							break;
						case "dropdown": 
							String d=testdata.getValue();
							System.out.println(d);

							GenericDto dto=new GenericDto();
							dto.setName(d);
							user_selects_combo_item(id,dto);
							break;

						case "checkbox":
							if(testdata.getValue().contains("Y"))
								robot.clickOn(id);
							break;

						case "fileupload":
							try {
							scene=applicationPrimaryStage.getScene();
							node=scene.lookup("#continueBtn");
						
							//if(!node.isDisable()) links.clickContinueBtn();
							if(flagContinueBtnDemograph==true && node.isVisible()) {
								//buttons.clickContinueBtn();
								robot.clickOn(node);
								flagContinueBtnDemograph=false;
							}
//							
//							if(flagContinueBtnFileUpload==true && node.isVisible()) {
//								links.clickContinueBtn();
//								flagContinueBtnFileUpload=false;
//							}

							DocumentCategoryDto documentCategoryDto=new DocumentCategoryDto();
							documentCategoryDto.setName(testdata.getValue());
							documentCategoryDto.setCode(schema.getSubType());

							user_selects_combo_item(id,documentCategoryDto);

							String scanBtn="#"+schema.getSubType();
							Button scanButton = robot.lookup(scanBtn).queryAs(Button.class);
							
							robot.clickOn("#docPreviewImgViewPane");
							robot.clickOn(scanButton);

							selectDocumentScan();
							}catch(Exception e)
							{
								e.printStackTrace();
							}

							break;

						}

					}
					catch(Exception e )
					{e.printStackTrace();
					}


					break;
				}

			}}
	}

/*
	private javafx.stage.Stage getTopModalStage() {
	    // Get a list of windows but ordered from top[0] to bottom[n] ones.
	    // It is needed to get the first found modal window.
	    final List<Window> allWindows = new ArrayList<>(robot.robotContext().getWindowFinder().listWindows());
	    Collections.reverse(allWindows);

	    return (javafx.stage.Stage) allWindows
	            .stream()
	            .filter(window -> window instanceof javafx.stage.Stage)
	            .filter(window -> ((javafx.stage.Stage) window).getModality() == Modality.APPLICATION_MODAL)
	            .findFirst()
	            .orElse(null);
	}
	*/
	private void biometricDetailsUpload() throws InterruptedException {
			//	scene=applicationPrimaryStage.getScene();

		//node=scene.lookup("#continueBtn");
		
		Node node=lookupById("#continueBtn");
		if(!node.isDisable()) robot.clickOn(node);

node=lookupById("#IRIS_DOUBLE");
		robot.clickOn(node);
		
		node=lookupById("#scanBtn");
		robot.clickOn(node);
		
		Thread.sleep(5000);
		
		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);


		
		
		node=lookupById("#FINGERPRINT_SLAB_RIGHT");
		robot.clickOn(node);
		
		node=lookupById("#scanBtn");
		robot.clickOn(node);
		
		Thread.sleep(5000);
		
		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);

		
		
		
		node=lookupById("#FINGERPRINT_SLAB_LEFT");
		robot.clickOn(node);
		
		node=lookupById("#scanBtn");
		robot.clickOn(node);
		
		/*
		//Button okButton = (Button) Alert.getDialogPane().lookupButton(ButtonType.CLOSE);
		
		  final javafx.stage.Stage actualAlertDialog = getTopModalStage();
		    assertNotNull(actualAlertDialog);

		    final DialogPane dialogPane = (DialogPane) actualAlertDialog.getScene().getRoot();
		    assertEquals(expectedHeader, dialogPane.getHeaderText());
		    assertEquals(expectedContent, dialogPane.getContentText());
		
*/
		
		//node=lookupById("Successfully");
		//robot.clickOn(node);
Thread.sleep(5000);
		
		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);

		
		node=scene.lookup("#FINGERPRINT_SLAB_THUMBS");//Right Hand Left Hand Thumbs Face
		robot.clickOn(node);
		robot.clickOn("#scanBtn");
		Thread.sleep(3000);
		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);


		
		node=lookupById("#FACE");
		robot.clickOn(node);
		
		node=lookupById("#scanBtn");
		robot.clickOn(node);
		
		//node=lookupById("Successfully");
		//robot.clickOn(node);
Thread.sleep(5000);
robot.press(KeyCode.SPACE).release(KeyCode.SPACE);



		
		node=lookupById("#continueBtn");
		robot.clickOn(node);
				
	}

	public void selectDocumentScan() throws InterruptedException, TimeoutException
	{	
	Node n=lookupById("#captureBtn");
	robot.clickOn(n);
	
	 n=lookupById("Success");
	robot.clickOn(n);
	robot.press(KeyCode.SPACE).release(KeyCode.SPACE);
	
	 n=lookupById("#saveBtn");
	robot.clickOn(n);
	
	robot.press(KeyCode.SPACE).release(KeyCode.SPACE);
	
	}


	public <T extends Node> T lookupById(final String controlId) {
		Awaitility
		.await()
		.pollDelay(700, TimeUnit.MILLISECONDS)
		.until(() -> robot.lookup(controlId).query() != null);

		return robot.lookup(controlId).query();
	}

	public void user_selects_combo_item(String comboBoxId, GenericDto dto) throws InterruptedException {



		Platform.runLater(new Runnable() {
			@Override
			public void run() {



				ComboBox comboBox= lookupById(comboBoxId);
				//robot.lookup(comboBoxId).query();

				comboBox.getSelectionModel().select(dto); 


			}}); 

		Thread.sleep(800);

	}

	public void user_selects_combo_item(String comboBoxId, DocumentCategoryDto dto) throws InterruptedException {



		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				ComboBox comboBox= robot.lookup(comboBoxId).query();

				comboBox.getSelectionModel().select(dto); 

			}});

		Thread.sleep(800);

	}

}

