package registrationtest.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.assertj.core.api.SoftAssertions;
import org.awaitility.Awaitility;
import org.json.JSONException;
import org.json.JSONObject;
import org.testfx.api.FxRobot;

import ch.qos.logback.classic.gaffer.PropertyUtil;
import io.mosip.registration.constants.RegistrationConstants;
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
import registrationtest.pojo.schema.Screens;
import registrationtest.utility.DateUtil;
import  registrationtest.utility.JsonUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class DemographicPage {

	private static final Logger logger = LogManager.getLogger(DemographicPage.class); 
	FxRobot robot;
	Stage applicationPrimaryStage;
	Scene scene;
	Screens screens;
	Node node;
	TextField demoTextField;
	Schema schema;
	Root rootSchema; 
	Boolean flagContinueBtnDemograph=true;

	Boolean flagContinueBtnDocumentUpload=true;
	DocumentUploadPage documentUploadPage;
	BiometricUploadPage biometricUploadPage;
	WaitsUtil waitsUtil;
	String DemoDetailsImg="#DemoDetailsImg";
	WebViewDocument webViewDocument;
	Buttons buttons;
	String schemaJsonFilePath;
	double schemaJsonFileVersion;
	String nameTab=null;
	LinkedHashMap<String,String> mapValue;
	LinkedHashMap<String,String> mapDropValue;
	String value;
	String jsonFromSchema;
	List<Screens> unOrderedScreensList, orderedScreensList;
	List<String> fieldsList;
	Boolean flagproofOf=true;
	Boolean flagBiometrics=true;
	LinkedHashMap<String, Integer> allignmentgroupMap;
	int age=0;
	public DemographicPage(FxRobot robot) {
		logger.info(" DemographicPage Constructor  ");

		this.robot=robot;
		waitsUtil=new WaitsUtil(robot);
		//waitsUtil.clickNodeAssert( DemoDetailsImg); 
		documentUploadPage=new DocumentUploadPage(robot);
		biometricUploadPage=new BiometricUploadPage(robot);
		buttons=new Buttons(robot);
		webViewDocument=new WebViewDocument(robot);
		allignmentgroupMap=new LinkedHashMap<String, Integer>();
	}

	public String getTextFields(String id) {

		demoTextField= waitsUtil.lookupById(id);
		while(demoTextField.getText()==null) {}
		System.out.println(demoTextField.getText());
		return demoTextField.getText();
	}

	public void setTextFields(String id,String idSchema) {

		Platform.runLater(new Runnable() {
			@Override
			public void run() {



				logger.info(" setTextFields in " + id +" " + idSchema );

				try {
					demoTextField= waitsUtil.lookupById(id);
					assertNotNull(demoTextField, id+" not present");

					if(demoTextField.isEditable() && demoTextField.isVisible() )
						demoTextField.setText(idSchema);

				}
				catch(Exception e)
				{
					logger.error(e.getMessage());
				}

			}
		});	
	}

	public void setTextFieldsChild(String id,String idSchema) {
		logger.info(" setTextFields in " + id +" " + idSchema );
		Platform.runLater(new Runnable() {
			@Override
			public void run() {

				try {
					demoTextField= waitsUtil.lookupById(id);
					assertNotNull(demoTextField, id+" not present");

					if(demoTextField.isEditable() && demoTextField.isVisible())
					{	
						demoTextField.setText(idSchema);
					}	}
				catch(Exception e)
				{
					logger.error(e.getMessage());
				}
			}
		});	
	}





	/**
	 * {@summary}  For New Registration adult
	 * @param schemaVersion
	 * @param JsonIdentity
	 * @param documentUpload
	 * @return
	 */
	public WebViewDocument scemaDemoDocUploadAdult(String JsonIdentity,String scenario)  {

		/**
		 *  convert jsonFromSchema intoJava
		 */

		try {
			jsonFromSchema=getSchemaJsonTxt(JsonIdentity);
			rootSchema = JsonUtil.convertJsonintoJava(jsonFromSchema, Root.class);
			logger.info(rootSchema.getIdVersion()); logger.info("Automaiton Script - Printing Input file Json" + JsonIdentity);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}


		/**
		 * sortSchemaScreen Based on Order
		 * 
		 * Set Array index Based on the Order of the screens
		 * 
		 */
		unOrderedScreensList=rootSchema.getScreens();



		if(!unOrderedScreensList.isEmpty())
		{
			orderedScreensList=sortSchemaScreen(unOrderedScreensList);
		}
		else
		{orderedScreensList=singleSchemaScreen(rootSchema);
		}
		for(int k=0;k<orderedScreensList.size();k++)
		{	
			try {
				if(k>0) buttons.clicknextBtn();	
			}catch(Exception e)
			{
				logger.error(e.getMessage());
			}
			screens=orderedScreensList.get(k);

			logger.info("Order" + screens.getOrder()+ " Fields" + screens.getFields());
			fieldsList=screens.getFields();

			nameTab=screens.getName();
			//waitsUtil.clickNodeAssert("#"+nameTab);


			for(String field: fieldsList)
			{
				try {
					for (int i = 0; i < rootSchema.getSchema().size(); i++)
					{
						schema = rootSchema.getSchema().get(i);
						if(!field.equals(schema.getId())) continue;
						try {
							if(field.contains("proofOf")&&schema.isRequired())
							{	if(flagproofOf) 
							{	scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("proofscroll")));
								
								flagproofOf=false;
							}
							}else if(field.contains("Biometrics")) 
							{	if(flagBiometrics) { 
							}
							}
							else	if(field.contains("consent"))
							{
								scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("consentscroll")));
							}
							else if(schema.isRequired())
							{
								scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection1")));
							}
						}catch(Exception e)
						{logger.error(e.getMessage());
						}
						logger.info("Automaiton Script - id="+i + "=" +schema.getId() + "\tSchemaControlType=" + schema.getControlType());
						contolType(schema,JsonIdentity,scenario);
					}
				}
				catch(Exception e)
				{
					logger.error(e.getMessage());
				}
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
	public WebViewDocument scemaRegistrationUpdate(String JsonIdentity,String scenario)  {

		/**
		 *  convert jsonFromSchema intoJava
		 */

		try {
			jsonFromSchema=getSchemaJsonTxt(JsonIdentity);
			rootSchema = JsonUtil.convertJsonintoJava(jsonFromSchema, Root.class);
			logger.info(rootSchema.getIdVersion()); logger.info("Automaiton Script - Printing Input file Json" + JsonIdentity);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}


		/**
		 * sortSchemaScreen Based on Order
		 * 
		 * Set Array index Based on the Order of the screens
		 * 
		 */
		unOrderedScreensList=rootSchema.getScreens();



		if(!unOrderedScreensList.isEmpty())
		{
			orderedScreensList=sortSchemaScreen(unOrderedScreensList);
		}
		else
		{orderedScreensList=singleSchemaScreen(rootSchema);
		}
		for(int k=0;k<orderedScreensList.size();k++)
		{	
			screens=orderedScreensList.get(k);

			logger.info("Order" + screens.getOrder()+ " Fields" + screens.getFields());
			fieldsList=screens.getFields();

			nameTab=screens.getName();
			//if(k>0) {
			waitsUtil.clickNodeAssert("#"+nameTab+"_tab");
			//waitsUtil.clickNodeAssert("#"+nameTab);
			robot.moveTo("#"+nameTab);
			//}
			for(String field: fieldsList)
			{
				try {
					for (int i = 0; i < rootSchema.getSchema().size(); i++)
					{
						schema = rootSchema.getSchema().get(i);
						if(!field.equals(schema.getId())) continue;
						try {
							if(schema.getGroup().equals("Documents")&&schema.isInputRequired())
							{	if(flagproofOf) //22
							{
//								scrollVerticalDirection2(i,schema);
//								scrollVerticalDirection2(i,schema);
//								scrollVerticalDirection2(i,schema);
//								scrollVerticalDirection2(i,schema);
//								scrollVerticalDirection2(i,schema);
//								scrollVerticalDirection2(i,schema);
//								scrollVerticalDirection2(i,schema);
//								scrollVerticalDirection2(i,schema);
//								flagproofOf=false;
							}
							}else if(field.contains("Biometrics")) 
							{	if(flagBiometrics) { //16
								//								scrollVerticalDirection2(i,schema);
								//								Thread.sleep(400);
								//								flagBiometrics=false;
							}
							}
							else	
								if(schema.getGroup().equals("consent")&&schema.isInputRequired())
							{
								scrollVerticalDirection2(i,schema);
								scrollVerticalDirection2(i,schema);
								scrollVerticalDirection2(i,schema);
								scrollVerticalDirection2(i,schema);

							}
							else if(schema.isInputRequired())
							{
								//scrollVerticalDirection1(i,schema); 
								scrollVerticalDirection(i,schema);
							}

						}catch(Exception e)
						{logger.error(e.getMessage());
						}
						System.out.println("Automaiton Script - id="+i + "=" +schema.getId() + "\tSchemaControlType=" + schema.getControlType());
						contolTypeUpdate(schema,JsonIdentity,scenario);
					}
				}
				catch(Exception e)
				{
					logger.error(e.getMessage());
				}
			}
		}
		return webViewDocument;

	}
	private void scrollVerticalDirection2(int i,Schema schema)  {

		try {
			robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")), VerticalDirection.DOWN);

		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	private void scrollVerticalDirectioncount(int scrollcount )  {

		try {
			robot.scroll(scrollcount, VerticalDirection.DOWN);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	private void scrollVerticalDirection(int i,Schema schema)  {
		if(schema.getAlignmentGroup()==null)
		{
			try {
				robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection1")), VerticalDirection.DOWN);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			
			try {
				if(!allignmentgroupMap.containsKey(schema.getAlignmentGroup().toString()))
					
					{allignmentgroupMap.put(schema.getAlignmentGroup().toString(),1);
					
				robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")), VerticalDirection.DOWN);
			}} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
			logger.error(e.getMessage());
		} 

	}


	public String getSchemaJsonTxt(String JsonIdentity)
	{
		String jsonFromSchema = null;
		try {
			schemaJsonFileVersion=JsonUtil.JsonObjDoubleParsing(JsonIdentity,"IDSchemaVersion");
			schemaJsonFilePath = System.getProperty("user.dir")+"\\SCHEMA_"+schemaJsonFileVersion+".json";
			jsonFromSchema = Files.readString(Paths.get(schemaJsonFilePath));
			logger.info("Automaiton Script - Printing jsonFromSchema" + jsonFromSchema);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return jsonFromSchema;
	}

	/**
	 * sortSchemaScreen Based on Order
	 * @param unsortedList
	 * @return
	 */


	public List<Screens> sortSchemaScreen(List<Screens> unsortedList)
	{
		Map<Integer,Integer> m=new HashMap<Integer,Integer>();	
		List<Screens> sortList=new LinkedList<Screens>();
		for(int kk=0;kk<unsortedList.size();kk++)
		{
			m.put(unsortedList.get(kk).getOrder(),kk);

		}
		System.out.println(m);



		TreeMap<Integer,Integer> tm=new  TreeMap<Integer,Integer> (m);  
		Iterator itr=tm.keySet().iterator();               
		while(itr.hasNext())    
		{    
			int key=(int)itr.next();  
			System.out.println("Order:  "+key+"     Index:   "+m.get(key)); 
			sortList.add(rootSchema.getScreens().get(m.get(key)));
		}    

		System.out.println(tm);
		System.out.println(sortList);

		return sortList;

	}

	public List<Screens> singleSchemaScreen(Root rootSchema)
	{Screens scn = new Screens();
	//Label lab=new Label();
	List<String> fieldList=new LinkedList<String>();
	List<Screens> screenList=new LinkedList<Screens>();
	HashMap<String, String> label = new HashMap<>();
	String[] listLang=null;;
	try {
		listLang = PropertiesUtil.getKeyValue("langcode").split("@@");
	} catch (IOException e) {
		logger.error(e.getMessage());
	}

	try
	{
		for (int i = 0; i < rootSchema.getSchema().size(); i++) {
			schema = rootSchema.getSchema().get(i);
			fieldList.add(schema.getId());
		}

		scn.setOrder(0);
		scn.setName("Resident_Information");

		label.put(listLang[0],"Resident_Information");
		scn.setLabel(label);
		scn.setCaption(label);

		scn.setFields(fieldList);
		scn.setLayoutTemplate(null);
		scn.setPreRegFetchRequired(true);
		scn.setActive(false);
		System.out.println(scn.toString() );
		screenList.add(scn);

		System.out.println(screenList.toString());
	}
	catch(Exception e)
	{
		logger.error(e.getMessage());
	}
	return screenList;
	}

	public List<String> getupdateUINAttributes(String JsonIdentity)
	{
		List<String> updateUINAttributes=null;
		try {
			updateUINAttributes=JsonUtil.JsonObjArrayListParsing(JsonIdentity, "updateUINAttributes");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return updateUINAttributes;
	}

	public void getTextboxKeyValueUpdate(String id,String JsonIdentity,String key,Schema schema,Boolean trans,String scenario)
	{	
		for(String uinlist:getupdateUINAttributes(JsonIdentity)) {
			if(schema.getGroup().equals(uinlist))
			{
				if(scenario.contains("child"))
					getTextboxKeyValueChild(id,JsonIdentity,key,schema.isTransliterate(),scenario);
				else 
					getTextboxKeyValue(id,JsonIdentity,key,schema.isTransliterate(),scenario);

			}
		}
	}



	public void getTextboxKeyValueChild(String id,String JsonIdentity,String key,Boolean trans,String scenario)
	{
		logger.info("Schema Control Type textbox");
		mapValue=null;
		if((schema.isRequired() && schema.isInputRequired()) || (schema.getRequiredOn().get(0).getExpr().contains("identity.isChild"))) 
		{	if(schema.getType().contains("simpleType"))
		{
			try {
				mapValue=JsonUtil.JsonObjSimpleParsing(JsonIdentity,key);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Set<String> keys = mapValue.keySet();
			for(String ky:keys)
			{
				String idk=id+ky;
				String v=mapValue.get(ky);
				setTextFieldsChild(idk,v);
				if(trans==true) return;
			}
		}
		else
		{
			value=null;
			try {
				value = JsonUtil.JsonObjParsing(JsonIdentity,key);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				String[] listLang=PropertiesUtil.getKeyValue("langcode").split("@@");

				setTextFields(id+listLang[0],value);
			} catch (IOException e) {

				logger.error(e.getMessage());
			}
		}
		}

	}

		
	public void biometricsAuth(Schema schema,String scenario,String id,String identity)
	{
		try {
			if(
					scenario.contains("update") &&schema.subType.equalsIgnoreCase("applicant-auth"))
					
			{
				System.out.println("");
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				//16

				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				//12
				Thread.sleep(400);
				biometricUploadPage.newRegbioUpload(schema.getSubType(),biometricUploadPage.bioAuthAttributeList(identity),id,identity);


			}

		}catch(Exception e)
		{
			logger.error(e.getMessage());
		}
		}
	
	public void biometricsupdate(Schema schema,String scenario,String id,String identity)
	{
		try {
			if(schema.isInputRequired() && schema.subType.equalsIgnoreCase("applicant"))
			{
				if(scenario.contains("updatechild")&&schema.getBioAttributes().contains("face")
						|| (getupdateUINAttributes(identity).contains("GuardianDetails")&&schema.getBioAttributes().contains("face")))
				{	
					biometricUploadPage.newRegbioUpload(schema.getSubType(),"face",id,identity);
				}
			else {
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);

					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					scrollVerticalDirection2(0,schema);
					Thread.sleep(400);
					biometricUploadPage.newRegbioUpload(schema.getSubType(),schema.getBioAttributes(),id,identity);
				}
			}
			else if(
					schema.subType.equalsIgnoreCase("introducer")&&
					scenario.contains("child")&&
					(
							((schema.getRequiredOn().get(0).getExpr().contains("identity.isChild"))&&
									(schema.getRequiredOn().get(0).getExpr().contains("identity.isNew")))||
							(schema.getRequiredOn().get(0).getExpr().contains("identity.isUpdate") && (getupdateUINAttributes(identity).contains("GuardianDetails")))
							))

			{
				System.out.println();
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				//16

				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				scrollVerticalDirection2(0,schema);
				//12
				Thread.sleep(400);
				biometricUploadPage.newRegbioUpload(schema.getSubType(),schema.getBioAttributes(),id,identity);


			}
			
		}catch(Exception e)
		{
			logger.error(e.getMessage());
		}}


	
	public void biometrics(Schema schema,String scenario,String id,String identity,String age)
	{
		try {
			if(schema.isInputRequired() && schema.subType.equalsIgnoreCase("applicant"))
			{
				//16
				//				if ((getRegistrationDTo().isChild() && APPLICANT_SUBTYPE.equals(subType) && requiredAttributes.contains("face"))
				//						|| (getRegistrationDTo().getRegistrationCategory().equalsIgnoreCase(RegistrationConstants.PACKET_TYPE_UPDATE)
				//						&& getRegistrationDTo().getUpdatableFieldGroups().contains("GuardianDetails")
				//						&& APPLICANT_SUBTYPE.equals(subType) && requiredAttributes.contains("face"))) {
				//					return Arrays.asList("face"); // Only capture face
				//				

				
				if(scenario.contains("child")&&schema.getBioAttributes().contains("face"))
						{	
					scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("childbioscroll")));
					
					biometricUploadPage.newRegbioUpload(schema.getSubType(),"face",id,identity);
				}
			else {
				
					scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));
					
					Thread.sleep(400);
					biometricUploadPage.newRegbioUpload(schema.getSubType(),schema.getBioAttributes(),id,identity);
				}
			}
			else if(
					schema.subType.equalsIgnoreCase("introducer")&& (!scenario.contains("lost")) &&
					scenario.contains("child")&&
					(
							((schema.getRequiredOn().get(0).getExpr().contains("identity.isChild"))&&
									(schema.getRequiredOn().get(0).getExpr().contains("identity.isNew")))||
							(schema.getRequiredOn().get(0).getExpr().contains("identity.isUpdate") && (getupdateUINAttributes(identity).contains("GuardianDetails")))
							))

			{
				scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));
				
				Thread.sleep(400);
				biometricUploadPage.newRegbioUpload(schema.getSubType(),schema.getBioAttributes(),id,identity);


			}
			
		}catch(Exception e)
		{
			logger.error(e.getMessage());
		}}

	public void fileuploadUpdate(Schema schema,String JsonIdentity,String key,String id,String scenario)
	{	if(schema.getGroup().equals("Documents")) {
		scrollVerticalDirection2(0,schema);
		scrollVerticalDirection2(0,schema);
		scrollVerticalDirection2(0,schema);
		scrollVerticalDirection2(0,schema);
		scrollVerticalDirection2(0,schema);
		scrollVerticalDirection2(0,schema);
		
		documentUploadPage.documentDropDownScan(schema, id, JsonIdentity, key);
	}
	}

	public void fileupload(Schema schema,String JsonIdentity,String key,String id,String scenario)
	{
		try {
			if(schema.isInputRequired() && schema.isRequired() && (schema.getRequiredOn()).get(0).getExpr().contains("identity.isNew"))
				documentUploadPage.documentDropDownScan(schema, id, JsonIdentity, key);
			else if(scenario.contains("child")&&(schema.getRequiredOn().get(0).getExpr().contains("identity.isChild"))&&(schema.getRequiredOn().get(0).getExpr().contains("identity.isNew")))
				documentUploadPage.documentDropDownScan(schema, id, JsonIdentity, key);
			
				
		}catch(Exception e)
		{
			logger.error(e.getMessage());

		}

	}

	public void dropdownUpdate(String id,String JsonIdentity,String key) {
		for(String uinlist:getupdateUINAttributes(JsonIdentity)) {
			if(schema.getGroup().equals(uinlist))
			{
				dropdown(id,JsonIdentity,key);
			}}
	}
	public void dropdown(String id,String JsonIdentity,String key) {
		GenericDto dto=new GenericDto();
		try {
			mapDropValue=null;
			if(schema.getType().contains("simpleType"))
			{
				mapDropValue=JsonUtil.JsonObjSimpleParsing(JsonIdentity,key);
				Set<String> dropkeys = mapDropValue.keySet();
				for(String ky:dropkeys)
				{	dto.setCode(ky);
				dto.setLangCode(ky);
				dto.setName(mapDropValue.get(ky));
				user_selects_combo_item(id,dto);
				break;
				}
			}
			else
			{
				String val=JsonUtil.JsonObjParsing(JsonIdentity, key);
				dto.setName(val);
				user_selects_combo_item(id,dto);

			}}catch(Exception e)
		{
				logger.error(e.getMessage());
		}
	}

	public void setageDate(String id,String JsonIdentity,String key)
	{
		String dateofbirth[] = null;
		try {
			dateofbirth = JsonUtil.JsonObjParsing(JsonIdentity,key).split("/");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setTextFields(id+"ddTextField",dateofbirth[2]);
		setTextFields(id+"mmTextField",dateofbirth[1]);
		setTextFields(id+"yyyyTextField",dateofbirth[0]);
		age = getTextFields(id+"ageFieldTextField");
	}
	

	public void checkSelection(String id,String JsonIdentity,String key)
	{
		String flag = "N";
		try {
			flag = JsonUtil.JsonObjParsing(JsonIdentity,key);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
if(flag.equalsIgnoreCase("Y"))
	{
	
	waitsUtil.clickNodeAssert(id);
	
	}}
	
	public void setageDateUpdate(String id,String JsonIdentity,String key,String scenario)
	{
		for(String uinlist:getupdateUINAttributes(JsonIdentity)) {
			if(schema.getGroup().equals(uinlist))
			{				setageDate(id,JsonIdentity,key);
			}}}
	public void contolType(Schema schema,String JsonIdentity,String scenario)
	{
		String id="#"+schema.getId(); 
		String key=schema.getId(); 
		try {
			switch (schema.getControlType())
			{
			case "html":
				logger.info("Read Consent");
				break;
			case "textbox":
				if(scenario.contains("child"))
					getTextboxKeyValueChild(id,JsonIdentity,key,schema.isTransliterate(),scenario);
				else
					getTextboxKeyValue(id,JsonIdentity,key,schema.isTransliterate(),scenario);
				break;
			case "ageDate":
					setageDate(id,JsonIdentity,key);
				break;

			case "dropdown": 
					dropdown(id,JsonIdentity,key);
				break;
			case "checkbox":
						checkSelection(id,JsonIdentity,key);
				break;
			case "fileupload":
					fileupload(schema,JsonIdentity,key,id,scenario);
				break;
			case "biometrics":
					biometrics(schema,scenario,id,JsonIdentity,age);
				break;

			}

		}
		catch(Exception e )
		{
			logger.error(e.getMessage());
		}
	}


	public void contolTypeUpdate(Schema schema,String JsonIdentity,String scenario)
	{
		String id="#"+schema.getId(); 
		String key=schema.getId(); 
		try {
			switch (schema.getControlType())
			{
			case "html":
				logger.info("Read Consent");
				break;
			case "textbox":
				getTextboxKeyValueUpdate(id,JsonIdentity,key,schema,schema.isTransliterate(),scenario);
				break;
			case "ageDate":
				if(getupdateUINAttributes(JsonIdentity).contains("DateOfBirth"))
				{
					setageDateUpdate(id,JsonIdentity,key,scenario);
					
				}
				break;

			case "dropdown": 
				dropdownUpdate(id,JsonIdentity,key);
				break;
			case "checkbox":
				if(getupdateUINAttributes(JsonIdentity).contains("consent"))
				{
					waitsUtil.clickNodeAssert(id);
							}

				break;
			case "fileupload":
				if(getupdateUINAttributes(JsonIdentity).contains("Documents"))
				{//Any Doc
					fileuploadUpdate(schema,JsonIdentity,key,id,scenario);
				}else
				{
					//Based on demo details
					//fileupload(schema,JsonIdentity,key,id,scenario);
				}
				break;
			case "biometrics":
				if(getupdateUINAttributes(JsonIdentity).contains("Biometrics") &&schema.getGroup().equals("Biometrics"))
				{  // All Bio-- IRIS(U) <--- Remaining() based on age
					biometricsupdate(schema,scenario,id,JsonIdentity);
				}else if( !getupdateUINAttributes(JsonIdentity).contains("Biometrics") && schema.getGroup()==null && !getupdateUINAttributes(JsonIdentity).contains("GuardianDetails"))
				{ //Single ---> AUTH
					biometricsAuth(schema,scenario,id,JsonIdentity);
					
				}
				else if( !getupdateUINAttributes(JsonIdentity).contains("Biometrics") && schema.getGroup()==null && getupdateUINAttributes(JsonIdentity).contains("GuardianDetails"))
				{//Only Introducer
					biometricsupdate(schema,scenario,id,JsonIdentity);
					
				}
				break;
				
			}
			
		}
		catch(Exception e )
		{
			logger.error(e.getMessage());
		}
	}



	private void getTextboxKeyValue(String id, String JsonIdentity, String key, boolean transliterate,
			String scenario) {
		logger.info("Schema Control Type textbox");
		mapValue=null;
		if(schema.isRequired() && schema.isInputRequired())
		{	if(schema.getType().contains("simpleType"))
		{
			try {
				mapValue=JsonUtil.JsonObjSimpleParsing(JsonIdentity,key);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Set<String> keys = mapValue.keySet();
			for(String ky:keys)
			{
				String idk=id+ky;
				String v=mapValue.get(ky);
				setTextFields(idk,v);
				if(transliterate==true) return;
			}
		}

		else
		{
			value=null;
			try {
				value = JsonUtil.JsonObjParsing(JsonIdentity,key);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				String[] listLang=PropertiesUtil.getKeyValue("langcode").split("@@");

				setTextFields(id+listLang[0],value);
			} catch (IOException e) {

				logger.error(e.getMessage());
			}
		}
		}

	}
	public WebViewDocument scemaGroupUpdate(String JsonIdentity, String scenario) {

		/**
		 *  convert jsonFromSchema intoJava
		 */

		try {
			jsonFromSchema=getSchemaJsonTxt(JsonIdentity);
			rootSchema = JsonUtil.convertJsonintoJava(jsonFromSchema, Root.class);
			logger.info(rootSchema.getIdVersion()); logger.info("Automaiton Script - Printing Input file Json" + JsonIdentity);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}


		/**
		 * sortSchemaScreen Based on Order
		 * 
		 * Set Array index Based on the Order of the screens
		 * 
		 */
		unOrderedScreensList=rootSchema.getScreens();



		if(!unOrderedScreensList.isEmpty())
		{
			orderedScreensList=sortSchemaScreen(unOrderedScreensList);
		}
		else
		{orderedScreensList=singleSchemaScreen(rootSchema);
		}
		for(int k=0;k<orderedScreensList.size();k++)
		{	
			if(k>0) buttons.clicknextBtn();	

			screens=orderedScreensList.get(k);

			logger.info("Order" + screens.getOrder()+ " Fields" + screens.getFields());
			fieldsList=screens.getFields();

			nameTab=screens.getName();
			//waitsUtil.clickNodeAssert("#"+nameTab);

			for(String field: fieldsList)
			{
				try {
					for (int i = 0; i < rootSchema.getSchema().size(); i++)
					{
						schema = rootSchema.getSchema().get(i);
						if(!field.equals(schema.getId())) continue;
						try {
							if(field.contains("proofOf")&&schema.isRequired())
							{	if(flagproofOf) 
							{	scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("proofscroll")));
								
								flagproofOf=false;
							}
							}else if(field.contains("Biometrics")) 
							{	if(flagBiometrics) { 
							}
							}
							else	if(field.contains("consent"))
							{
								scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("consentscroll")));
							}
							else if(schema.isRequired())
							{
								scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection1")));
							}
						}catch(Exception e)
						{logger.error(e.getMessage());
						}
						logger.info("Automaiton Script - id="+i + "=" +schema.getId() + "\tSchemaControlType=" + schema.getControlType());
						contolType(schema,JsonIdentity,scenario);
					}
				}
				catch(Exception e)
				{
					logger.error(e.getMessage());
				}
			}
		}
		return webViewDocument;

	}
}




