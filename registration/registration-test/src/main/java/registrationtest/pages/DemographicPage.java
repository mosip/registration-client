package registrationtest.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.assertj.core.api.SoftAssertions;
import org.awaitility.Awaitility;
import org.json.JSONObject;
import org.testfx.api.FxRobot;

import ch.qos.logback.classic.gaffer.PropertyUtil;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.dto.mastersync.GenericDto;
import javafx.application.Platform;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import registrationtest.controls.Buttons;
import registrationtest.pojo.output.RID;
import registrationtest.pojo.schema.Root;
import registrationtest.pojo.schema.Schema;
import registrationtest.pojo.testdata.RootTestData;
import registrationtest.pojo.testdata.TestData;
import registrationtest.utility.DateUtil;
import  registrationtest.utility.JsonUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class DemographicPage {

	private static final Logger logger = LogManager.getLogger(DemographicPage.class); 
	FxRobot robot;
	Stage applicationPrimaryStage;
	Scene scene;
	Node node;
	TextField demoTextField;
	Schema schema;
	TestData testdata;
	Root rootSchema; 
	RootTestData rootTestData;
	Boolean flagContinueBtnDemograph=true;

	Boolean flagContinueBtnDocumentUpload=true;
	DocumentUploadPage documentUploadPage;
	BiometricUploadPage biometricUploadPage;
	WaitsUtil waitsUtil;
	String DemoDetailsImg="#DemoDetailsImg";
	WebViewDocument webViewDocument;
	Buttons buttons;
	String schemafile;
	double schemaversion;


	public DemographicPage(FxRobot robot) {
		logger.info(" DemographicPage Constructor  ");

		this.robot=robot;
		waitsUtil=new WaitsUtil(robot);
		//waitsUtil.clickNodeAssert( DemoDetailsImg); 
		documentUploadPage=new DocumentUploadPage(robot);
		biometricUploadPage=new BiometricUploadPage(robot);
		buttons=new Buttons(robot);
	}

	public String getTextFields() {

		return demoTextField.getText();
	}

	public void setTextFields(String id,String idSchema) {
		logger.info(" setTextFields in " + id +" " + idSchema );

		try {
			demoTextField= waitsUtil.lookupById(id);
			assertNotNull(demoTextField, id+" not present");

			if(demoTextField.isEditable() && demoTextField.isVisible())
			{	
				robot.clickOn(id);	//Must Needed
				if(id.contains("Address") || id.contains("address"))demoTextField.setText(idSchema+DateUtil.getDateTime());
				else if(id.contains("fullName"))
				{	demoTextField.setText(idSchema+DateUtil.getDateTime());

				}
				else
					demoTextField.setText(idSchema);

			}	}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}
	}

	public void setTextFieldsChild(String id,String idSchema,RID rid1) {
		logger.info(" setTextFields in " + id +" " + idSchema );

		try {
			demoTextField= waitsUtil.lookupById(id);
			assertNotNull(demoTextField, id+" not present");

			if(demoTextField.isEditable() && demoTextField.isVisible())
			{	
				robot.clickOn(id);	//Must Needed
				if(id.contains("Address") || id.contains("address"))demoTextField.setText(idSchema+DateUtil.getDateTime());
				else if(id.contains("parentOrGuardianName"))
				{
					if(rid1.firstName.contains("/"))
					{
						String firstname[]=rid1.firstName.split("/");
						String temp=firstname[0];
						rid1.setFirstName(temp.trim());
						demoTextField.setText(rid1.firstName);
					}
				}
				else if(id.contains("parentOrGuardianRID"))
					demoTextField.setText(rid1.rid);
				else 
					demoTextField.setText(idSchema);
			}	}
		catch(Exception e)
		{
			logger.error(e.getMessage());
		}
	}

	/**
	 * {@summary For LOST UIN}
	 * @param schemaVersion
	 * @param JsonIdentity
	 * @param documentUpload
	 * @return
	 */
	public WebViewDocument scemaDemoDocUploadLostUIN(String JsonIdentity,HashMap<String, String> documentUpload)  {


		try {
			schemaversion=JsonUtil.JsonObjIntParsing(JsonIdentity,"IDSchemaVersion");

			schemafile = System.getProperty("user.dir")+"\\SCHEMA_"+schemaversion+".json";
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/**
		 *  convertJsonintoJava
		 */
		String jsonFromSchema;
		try {
			jsonFromSchema = Files.readString(Paths.get(schemafile));
			logger.info("Automaiton Script - Printing Json" + jsonFromSchema);

			rootSchema = JsonUtil.convertJsonintoJava(jsonFromSchema, Root.class);

			logger.info(rootSchema.getId() + " " + rootSchema.getIdVersion());
			logger.info("Automaiton Script - Printing Json" + JsonIdentity);

			logger.info("Automaiton Script - Schema ID Follows :- ");
		} catch (IOException e) {
			logger.error(e.getMessage());
		}



		for (int i = 2; i <= rootSchema.getSchema().size()-1; i++) {

			schema = rootSchema.getSchema().get(i);
			System.out.println("Automaiton Script - id="+i + "=" +schema.getId() + "\tSchemaControlType=" + schema.getControlType());

			String id="#"+schema.getId(); 
			String key=schema.getId(); 
			try {
				scrollVerticalDirection(i,schema);


				switch (schema.getControlType()) {
				case "textbox":
					logger.info("Schema Control Type textbox");
					String value=null;
					if(schema.getType().contains("simpleType"))
						value=JsonUtil.JsonObjSimpleParsing(JsonIdentity,key);
					else
						value=JsonUtil.JsonObjParsing(JsonIdentity,key);

					setTextFields(id,value);
					System.out.println("Textbox");
					break;
				case "ageDate":

					String dateofbirth[]=JsonUtil.JsonObjParsing(JsonIdentity,key).split("/");

					if(!schema.getLabel().secondary.equals(null)) {
						robot.press(KeyCode.TAB).release(KeyCode.TAB);
						robot.press(KeyCode.TAB).release(KeyCode.TAB);}
					else { robot.press(KeyCode.TAB).release(KeyCode.TAB);}
					robot.write(dateofbirth[0]);
					robot.press(KeyCode.TAB).release(KeyCode.TAB);
					robot.write(dateofbirth[1]);
					robot.press(KeyCode.TAB).release(KeyCode.TAB);
					robot.write(dateofbirth[2]);
					break;
				case "dropdown": 
					GenericDto dto=new GenericDto();
					if(schema.getType().contains("simpleType"))
						dto.setName(JsonUtil.JsonObjSimpleParsing(JsonIdentity,key));
					else
						dto.setName(JsonUtil.JsonObjParsing(JsonIdentity,key));
					user_selects_combo_item(id,dto);
					break;

				case "checkbox":
					if(JsonUtil.JsonObjParsing(JsonIdentity,key).contains("Y"))
						waitsUtil.clickNodeAssert(id);

					break;

				case "fileupload":
					try {
						if(flagContinueBtnDemograph==true) {
							buttons.clickContinueBtn();
							flagContinueBtnDemograph=false;								
						}


					}catch(Exception e)
					{
						logger.error(e.getMessage());
					}

					break;
				case "biometrics":
					try {
						if(flagContinueBtnDocumentUpload==true) {
							buttons.clickContinueBtn();
							flagContinueBtnDocumentUpload=false;								
						}
						if((schema.isInputRequired()) && (schema.isRequired()))
						{
							webViewDocument=biometricUploadPage.newRegbioUpload(schema.getBioAttributes());
						}

					}catch(Exception e)
					{

						logger.error(e.getMessage());
					}

					break;

				}

			}
			catch(Exception e )
			{
				logger.error(e.getMessage());
			}

		}
		return webViewDocument;


	}



	/**
	 * {@summary}  For New Registration adult
	 * @param schemaVersion
	 * @param JsonIdentity
	 * @param documentUpload
	 * @return
	 */
	public WebViewDocument scemaDemoDocUploadAdult(String JsonIdentity,HashMap<String, String> documentUpload)  {

		try {
			schemaversion=JsonUtil.JsonObjIntParsing(JsonIdentity,"IDSchemaVersion");

			schemafile = System.getProperty("user.dir")+"\\SCHEMA_"+schemaversion+".json";
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		/**
		 *  convertJsonintoJava
		 */
		String jsonFromSchema;
		try {
			jsonFromSchema = Files.readString(Paths.get(schemafile));
			logger.info("Automaiton Script - Printing Json" + jsonFromSchema);

			rootSchema = JsonUtil.convertJsonintoJava(jsonFromSchema, Root.class);

			logger.info(rootSchema.getId() + " " + rootSchema.getIdVersion());
			logger.info("Automaiton Script - Printing Json" + JsonIdentity);

			logger.info("Automaiton Script - Schema ID Follows :- ");
		} catch (IOException e) {
			logger.error(e.getMessage());
		}



		for (int i = 2; i < rootSchema.getSchema().size()-1; i++) {

			schema = rootSchema.getSchema().get(i);
			System.out.println("Automaiton Script - id="+i + "=" +schema.getId() + "\tSchemaControlType=" + schema.getControlType());

			String id="#"+schema.getId(); 
			String key=schema.getId(); 
			try {
				scrollVerticalDirection(i,schema);


				switch (schema.getControlType()) {
				case "textbox":
					logger.info("Schema Control Type textbox");
					String value=null;
					if(schema.getType().contains("simpleType"))
						value=JsonUtil.JsonObjSimpleParsing(JsonIdentity,key);
					else
						value=JsonUtil.JsonObjParsing(JsonIdentity,key);

					setTextFields(id,value);
					System.out.println("Textbox");
					break;
				case "ageDate":

					String dateofbirth[]=JsonUtil.JsonObjParsing(JsonIdentity,key).split("/");

					if(!schema.getLabel().secondary.equals(null)) {
						robot.press(KeyCode.TAB).release(KeyCode.TAB);
						robot.press(KeyCode.TAB).release(KeyCode.TAB);}
					else { robot.press(KeyCode.TAB).release(KeyCode.TAB);}
					robot.write(dateofbirth[0]);
					robot.press(KeyCode.TAB).release(KeyCode.TAB);
					robot.write(dateofbirth[1]);
					robot.press(KeyCode.TAB).release(KeyCode.TAB);
					robot.write(dateofbirth[2]);
					break;
				case "dropdown": 
					GenericDto dto=new GenericDto();
					if(schema.getType().contains("simpleType"))
						dto.setName(JsonUtil.JsonObjSimpleParsing(JsonIdentity,key));
					else
						dto.setName(JsonUtil.JsonObjParsing(JsonIdentity,key));
					user_selects_combo_item(id,dto);
					break;

				case "checkbox":
					if(JsonUtil.JsonObjParsing(JsonIdentity,key).contains("Y"))
						waitsUtil.clickNodeAssert(id);

					break;

				case "fileupload":
					try {
						if(flagContinueBtnDemograph==true) {
							buttons.clickContinueBtn();
							flagContinueBtnDemograph=false;								
						}

						//if(schema.getRequiredOn().equals("") ||(schema.getRequiredOn()).get(0).getExpr().contains("identity.isNew"))//eval expressions
						if(schema.isInputRequired() && (schema.getRequiredOn()).get(0).getExpr().contains("identity.isNew"))
							documentUploadPage.documentScan(documentUpload.get(key),schema,id);
					}catch(Exception e)
					{
						logger.error(e.getMessage());
					}

					break;
				case "biometrics":
					try {
						if(flagContinueBtnDocumentUpload==true) {
							buttons.clickContinueBtn();
							flagContinueBtnDocumentUpload=false;								
						}
						if((schema.isInputRequired()) && (schema.isRequired()))
						{
							webViewDocument=biometricUploadPage.newRegbioUpload(schema.getBioAttributes());
						}

					}catch(Exception e)
					{

						logger.error(e.getMessage());
					}

					break;

				}

			}
			catch(Exception e )
			{
				logger.error(e.getMessage());
			}

		}
		return webViewDocument;


	}





	/**
	 * {@summary}  For New  Child
	 * @param schemaVersion
	 * @param JsonIdentity
	 * @param documentUpload
	 * @return
	 */
	public WebViewDocument scemaDemoDocUploadChild(String JsonIdentity,HashMap<String, String> documentUpload,RID rid1)  {

		try {
			schemaversion=JsonUtil.JsonObjIntParsing(JsonIdentity,"IDSchemaVersion");

			schemafile = System.getProperty("user.dir")+"\\SCHEMA_"+schemaversion+".json";
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/**
		 *  convertJsonintoJava
		 */
		String jsonFromSchema;
		try {
			jsonFromSchema = Files.readString(Paths.get(schemafile));
			logger.info("Automaiton Script - Printing Json" + jsonFromSchema);

			rootSchema = JsonUtil.convertJsonintoJava(jsonFromSchema, Root.class);

			logger.info(rootSchema.getId() + " " + rootSchema.getIdVersion());
			logger.info("Automaiton Script - Printing Json" + JsonIdentity);

			logger.info("Automaiton Script - Schema ID Follows :- ");
		} catch (IOException e) {
			logger.error(e.getMessage());
		}



		for (int i = 2; i <= rootSchema.getSchema().size()-1; i++) {

			schema = rootSchema.getSchema().get(i);
			System.out.println("Automaiton Script - id="+i + "=" +schema.getId() + "\tSchemaControlType=" + schema.getControlType());

			String id="#"+schema.getId(); 
			String key=schema.getId(); 
			try {
				scrollVerticalDirection(i,schema);


				switch (schema.getControlType()) {
				case "textbox":
					logger.info("Schema Control Type textbox");
					String value=null;
					if(schema.getType().contains("simpleType"))
						value=JsonUtil.JsonObjSimpleParsing(JsonIdentity,key);
					else
						value=JsonUtil.JsonObjParsing(JsonIdentity,key);

					setTextFieldsChild(id,value,rid1);
					System.out.println("Textbox");
					break;
				case "ageDate":

					String dateofbirth[]=JsonUtil.JsonObjParsing(JsonIdentity,key).split("/");

					if(!schema.getLabel().secondary.equals(null)) {
						robot.press(KeyCode.TAB).release(KeyCode.TAB);
						robot.press(KeyCode.TAB).release(KeyCode.TAB);}
					else { robot.press(KeyCode.TAB).release(KeyCode.TAB);}
					robot.write(dateofbirth[0]);
					robot.press(KeyCode.TAB).release(KeyCode.TAB);
					robot.write(dateofbirth[1]);
					robot.press(KeyCode.TAB).release(KeyCode.TAB);
					robot.write(dateofbirth[2]);
					break;
				case "dropdown": 
					GenericDto dto=new GenericDto();
					if(schema.getType().contains("simpleType"))
						dto.setName(JsonUtil.JsonObjSimpleParsing(JsonIdentity,key));
					else
						dto.setName(JsonUtil.JsonObjParsing(JsonIdentity,key));
					user_selects_combo_item(id,dto);
					break;

				case "checkbox":
					if(JsonUtil.JsonObjParsing(JsonIdentity,key).contains("Y"))
						waitsUtil.clickNodeAssert(id);

					break;
				case "fileupload":
					try {
						if(flagContinueBtnDemograph==true) {
							buttons.clickContinueBtn();
							flagContinueBtnDemograph=false;								
						}
						if (schema.isInputRequired() &&(schema.getRequiredOn()).get(0).getExpr().contains("identity.isChild && identity.isNew"))
						{documentUploadPage.documentScan(documentUpload.get(key),schema,id);

						}
						else if(schema.isInputRequired() && (schema.getRequiredOn()).get(0).getExpr().contains("identity.isNew"))
							documentUploadPage.documentScan(documentUpload.get(key),schema,id);

					}catch(Exception e)
					{
						logger.error(e.getMessage());
					}

					break;
				case "biometrics":
					try {
						if(flagContinueBtnDocumentUpload==true) {
							buttons.clickContinueBtn();
							flagContinueBtnDocumentUpload=false;								
						}
						if(schema.isInputRequired() && schema.subType.equalsIgnoreCase("applicant"))
						{
							biometricUploadPage.newRegbioUpload("face");
							buttons.clickContinueBtn();
						}
						else if(schema.isInputRequired() && schema.subType.equalsIgnoreCase("introducer"))
						{
							webViewDocument=biometricUploadPage.newRegbioUpload(schema.getBioAttributes());
						}
					}catch(Exception e)
					{

						logger.error(e.getMessage());
					}

					break;

				}

			}
			catch(Exception e )
			{
				logger.error(e.getMessage());
			}

		}
		return webViewDocument;


	}






	private void scrollVerticalDirection(int i,Schema schema)  {
		if(schema.getId().equals(schema.getSubType()))
			try {
				if(i==Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection6")))
					robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection6")), VerticalDirection.DOWN);
				else
					robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")), VerticalDirection.DOWN);
			} catch (NumberFormatException | IOException e) {
				// TODO Auto-generated catch block
				logger.error(e.getMessage());
			}
		else
			try {
				if(i>Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection6")))

					robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")), VerticalDirection.DOWN);
			} catch (NumberFormatException | IOException e) {
				// TODO Auto-generated catch block
				logger.error(e.getMessage());
			}
	}

	public void user_selects_combo_item(String comboBoxId, GenericDto dto)  {
		Thread taskThread = new Thread(new Runnable() {
			@Override
			public void run() {


				Platform.runLater(new Runnable() {
					@Override
					public void run() {



						ComboBox comboBox= waitsUtil.lookupById(comboBoxId);

						comboBox.getSelectionModel().select(dto); 


					}}); 
			}});



		taskThread.start();
		try {
			taskThread.join();
			Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ComboItemTimeWait")));
		} catch (NumberFormatException | InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		} 

	}


}




