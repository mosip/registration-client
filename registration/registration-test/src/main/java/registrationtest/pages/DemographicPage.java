package registrationtest.pages;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.mastersync.GenericDto;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import com.itextpdf.text.log.SysoCounter;

import javafx.application.Platform;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import registrationtest.controls.Buttons;
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
	TextField demoTextField,demoTextField2;
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

	Boolean flag=false;
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


	public void setTextFields(String id,String idSchema) {
		flag=false;
		Platform.runLater(new Runnable() {
			@Override
			public void run() {



				logger.info(" setTextFields in " + id +" " + idSchema );

				try {
					demoTextField= waitsUtil.lookupById(id);
					assertNotNull(demoTextField, id+" not present");

					try {
						if(demoTextField.isEditable() && demoTextField.isVisible() )
						{
							String makeUniqueEntry[]=PropertiesUtil.getKeyValue("makeUniqueEntry").split("@@");
							for(String uniqueid:makeUniqueEntry)
							{
								if(id.contains(uniqueid))
								{	
									demoTextField.setText(idSchema+DateUtil.getDateTime());
								flag=true;

								}
							}
						}
					}catch(Exception e)
					{
						logger.error("Invalid makeUniqueEntry or empty", e);

					}
					if(flag==false)
					{
						demoTextField.setText(idSchema);
					}
				}

				catch(Exception e)
				{
					logger.error("",e);
				}

			}
		});	
	}

	public void setTextFieldsChild(String id,String idSchema) {
		logger.info(" setTextFields in " + id +" " + idSchema );
		flag=false;
		Platform.runLater(new Runnable() {
			@Override
			public void run() {

				try {
					demoTextField= waitsUtil.lookupById(id);
					assertNotNull(demoTextField, id+" not present");

					try {
						String makeUniqueEntry[]=PropertiesUtil.getKeyValue("makeUniqueEntry").split("@@");

						for(String uniqueid:makeUniqueEntry)
						{
							if(id.contains(uniqueid))
							{	demoTextField.setText(idSchema+DateUtil.getDateTime());
							flag=true;

							}
						}
					}catch(Exception e)
					{
						logger.error("",e);
					}

					if(flag==false)
					{
						demoTextField.setText(idSchema);
					}
				}
				catch(Exception e)
				{
					logger.error("",e);
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
	public WebViewDocument screensFlow(String JsonIdentity,String flow,String ageGroup)  {

		/**
		 *  convert jsonFromSchema intoJava
		 */

		try {
			jsonFromSchema=getSchemaJsonTxt(JsonIdentity);
			rootSchema = JsonUtil.convertJsonintoJava(jsonFromSchema, Root.class);
			logger.info(rootSchema.getIdVersion()); logger.info("Automaiton Script - Printing Input file Json" + JsonIdentity);
		} catch (Exception e) {
			logger.error("",e);
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
			waitsUtil.clickNodeAssert("#"+nameTab+"_tab");
			//waitsUtil.clickNodeAssert("#"+nameTab);
			robot.moveTo("#"+nameTab);


			for(String field: fieldsList)
			{
				try {
					for (int i = 0; i < rootSchema.getSchema().size(); i++)
					{
						schema = rootSchema.getSchema().get(i);
						if(!field.equals(schema.getId())) continue;
						try {
							if(schema.getGroup().equals(PropertiesUtil.getKeyValue("Documents"))&&schema.isInputRequired())
							{	if(flagproofOf) 
							{	scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("proofscroll")));
							flagproofOf=false;
							}
							}else if(schema.getGroup().equals(PropertiesUtil.getKeyValue("Biometrics"))) 
							{	if(flagBiometrics) { 
							}
							}
							else	if(schema.getGroup().equals(PropertiesUtil.getKeyValue("consent"))&&schema.isInputRequired())
							{
								scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("consentscroll")));
							}
							else  if(schema.isInputRequired()&&schema.isRequired())
							{
								scrollVerticalDirection(i,schema);
							}
							//							else  if(!schema.isRequired()&&(scenario.contains("child")))
							//							{
							//								scrollVerticalDirection(i,schema);
							//							}
						}catch(Exception e)
						{logger.error("",e);
						}
						logger.info("Automaiton Script - id="+i + "=" +schema.getId() + "\tSchemaControlType=" + schema.getControlType());
						if(flow.equalsIgnoreCase("Update"))
						{
							contolTypeUpdate(schema,JsonIdentity,flow,ageGroup);
						}
						else
						{
							contolType(schema,JsonIdentity,flow,ageGroup);
						}
					}
				}
				catch(Exception e)
				{
					logger.error("",e);
				}
			}
		}
		return webViewDocument;

	}


	private void scrollVerticalDirection2(int i,Schema schema)  {

		try {
			robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")), VerticalDirection.DOWN);

		} catch (NumberFormatException e) {
			logger.error("",e);
		} catch (IOException e) {
			logger.error("",e);
		}

	}
	private void scrollVerticalDirectioncount(int scrollcount )  {

		try {
			robot.scroll(scrollcount, VerticalDirection.DOWN);

		} catch (Exception e) {
			logger.error("",e);
		}

	}

	private void scrolltillnode(int scrollcount )  {

		try {
			robot.scroll(scrollcount, VerticalDirection.DOWN);

		} catch (Exception e) {
			logger.error("",e);
		}

	}



	private void  scrollVerticalDirection(int i,Schema schema)  {
		String[] lang=null;
		try {
			lang = PropertiesUtil.getKeyValue("langcode").split("@@");
		} catch (IOException e) {
			logger.error("",e);
		}

		if(schema.getAlignmentGroup()==null)
		{
			try {
				if(schema.getControlType().equals("textbox"))
				{
					for(String s:lang)
						robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")), VerticalDirection.DOWN);
				}
				else
					robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")), VerticalDirection.DOWN);

			}  catch (Exception e) {
				logger.error("",e);
			}
		}
		else
		{

			try {
				if(!allignmentgroupMap.containsKey(schema.getAlignmentGroup().toString()))
				{
					allignmentgroupMap.put(schema.getAlignmentGroup().toString(),1);
					if(schema.getControlType().equals("textbox"))
					{
						for(String s:lang)
						{
							robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")), VerticalDirection.DOWN);
						}
					}
					else
					{
						robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")), VerticalDirection.DOWN);

					}
				}
				else
				{
					allignmentgroupMap.put(schema.getAlignmentGroup().toString(),allignmentgroupMap.get(schema.getAlignmentGroup().toString())+1);
					int key1=allignmentgroupMap.get(schema.getAlignmentGroup().toString());
					if(schema.getControlType().equals("textbox")&&(key1%2)==1 && key1>2 )
					{
						for(String s:lang)
						{
							robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")), VerticalDirection.DOWN);
						}
					}else
						if(schema.getControlType().equals("dropdown")&&(key1%4)==1 && key1>4 )
						{
							for(String s:lang)
							{
								robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")), VerticalDirection.DOWN);
							}
						}

				}



			} catch (Exception e) {
				logger.error("",e);
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
			logger.error("",e);
		} 

	}
	
	public void user_selects_combo_item1(String comboBoxId, String dto)  {
		Thread taskThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						ComboBox comboBox= waitsUtil.lookupById(comboBoxId);

						robot.clickOn(comboBox);
						robot.clickOn(dto);
						//comboBox.getSelectionModel().select(dto); 
						
					}}); 
			}});



		taskThread.start();
		try {
			taskThread.join();
			Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ComboItemTimeWait")));
		} catch (NumberFormatException | InterruptedException | IOException e) {
			logger.error("",e);
		} 

	}


	public String getSchemaJsonTxt(String JsonIdentity)
	{
		String jsonFromSchema = null;
		try {
			//schemaJsonFileVersion=JsonUtil.JsonObjDoubleParsing(JsonIdentity,"IDSchemaVersion");

			schemaJsonFileVersion=Double.parseDouble(PropertiesUtil.getKeyValue("IDSchemaVersion"));
			schemaJsonFilePath = System.getProperty("user.dir")+"\\SCHEMA_"+schemaJsonFileVersion+".json";
			jsonFromSchema = Files.readString(Paths.get(schemaJsonFilePath));
			logger.info("Automaiton Script - Printing jsonFromSchema" + jsonFromSchema);
		} catch (Exception e) {
			logger.error("",e);
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
		logger.error("",e);
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
		logger.error("",e);

	}
	return screenList;
	}

	public List<String> getupdateUINAttributes(String JsonIdentity)
	{
		List<String> updateUINAttributes=null;
		try {
			updateUINAttributes=JsonUtil.JsonObjArrayListParsing(JsonIdentity, "updateUINAttributes");
		} catch (Exception e) {
			logger.error("",e);
		}
		return updateUINAttributes;
	}

	public void getTextboxKeyValueUpdate(String id,String JsonIdentity,String key,Schema schema,Boolean trans,String scenario,String ageGroup)
	{	
		for(String uinlist:getupdateUINAttributes(JsonIdentity)) {
			if(schema.getGroup().equals(uinlist))
			{
				if(ageGroup.equalsIgnoreCase("INFANT")||ageGroup.equalsIgnoreCase("MINOR"))
					getTextboxKeyValueChild1(id,JsonIdentity,key,schema.isTransliterate(),scenario);
				else 
					getTextboxKeyValue1(id,JsonIdentity,key,schema.isTransliterate(),scenario);

			}
		}
	}


	public void getTextboxKeyValueChild1(String id,String JsonIdentity,String key,Boolean trans,String scenario)
	{
		logger.info("Schema Control Type textbox");
		mapValue=null;
		if(schema.isInputRequired())
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
				logger.error("",e);
			}
			try {
				String[] listLang=PropertiesUtil.getKeyValue("langcode").split("@@");

				setTextFields(id+listLang[0],value);
			} catch (IOException e) {

				logger.error("",e);
			}
		}
		}

	}



	public void getTextboxKeyValueChild(String id,String JsonIdentity,String key,Boolean trans,String scenario)
	{
		logger.info("Schema Control Type textbox");
		mapValue=null;
		if(schema.isInputRequired()){	if(schema.getType().contains("simpleType"))
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
				logger.error("",e);
			}
			try {
				String[] listLang=PropertiesUtil.getKeyValue("langcode").split("@@");

				setTextFields(id+listLang[0],value);
			} catch (IOException e) {

				logger.error("",e);
			}
		}
		}

	}


	public void biometricsAuth(Schema schema,String scenario,String id,String identity)
	{
		try {
			if(
					scenario.equalsIgnoreCase("Update") &&schema.subType.equalsIgnoreCase("applicant-auth"))

			{
				scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));

				System.out.println("");
				Thread.sleep(400);
				biometricUploadPage.newRegbioUpload(schema.getSubType(),biometricUploadPage.bioAuthAttributeList(identity),id,identity);


			}

		}catch(Exception e)
		{
			logger.error("",e);
		}
	}
	public List<String> fetchbioAttr(Schema schema,String agegroup,String process)
	{
		List<String> bioattributes=null;
		try {
		for(int index = 0;index<schema.getConditionalBioAttributes().size();index++)
		{
			if(schema.getConditionalBioAttributes().get(index).getAgeGroup().equalsIgnoreCase(agegroup)
					&&
					(schema.getConditionalBioAttributes().get(index).getProcess().equalsIgnoreCase("ALL")||
							schema.getConditionalBioAttributes().get(index).getProcess().equalsIgnoreCase(process)
							))
				bioattributes=schema.getConditionalBioAttributes().get(index).getBioAttributes();
		}}
		catch(Exception e)
		{
			logger.error("",e);
		}

		if(bioattributes==null)
			bioattributes=schema.getBioAttributes();

		return bioattributes;
	}

	public void biometrics(Schema schema,String scenario,String id,String identity)
	{
		try {
			RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
			
//			int age=registrationDTO.getAge();
			String ageGroup= JsonUtil.JsonObjParsing(identity,"ageGroup");
			String process= JsonUtil.JsonObjParsing(identity,"process");
			List<String> bioattributes=null;

			if(schema.isInputRequired() && schema.subType.equalsIgnoreCase("applicant"))
			{
				if(ageGroup.equalsIgnoreCase("INFANT"))
				{	
					scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("childbioscroll")));
					//bioattributes=fetchbioAttr(schema,ageGroup,process);
					biometricUploadPage.newRegbioUpload(schema.getSubType(),biometricUploadPage.bioAttributeList(identity),id,identity);
				}
				else {
					if(ageGroup.equalsIgnoreCase("MINOR"))
					{
					scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("childbioscroll")));
					//bioattributes=fetchbioAttr(schema,ageGroup,process);
					biometricUploadPage.newRegbioUpload(schema.getSubType(),biometricUploadPage.bioAttributeList(identity),id,identity);
					}
					else if(ageGroup.equalsIgnoreCase("ADULT"))
					{
						scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));
						Thread.sleep(400);
					//	bioattributes=fetchbioAttr(schema,ageGroup,process);
						
						biometricUploadPage.newRegbioUpload(schema.getSubType(),biometricUploadPage.bioAttributeList(identity),id,identity); 
					}}
			}
			else if(
					schema.subType.equalsIgnoreCase("introducer") && (ageGroup.equals("INFANT")||ageGroup.equals("MINOR")))
			{

				if(ageGroup.equalsIgnoreCase("INFANT")) {
					scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));
					scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));
				}
				else
					scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));

				Thread.sleep(400);
				biometricUploadPage.newRegbioUpload(schema.getSubType(),biometricUploadPage.bioAuthAttributeList(identity),id,identity);

			}

		}catch(Exception e)
		{
			logger.error("",e);
		}}

	public void fileuploadUpdate(Schema schema,String JsonIdentity,String key,String id,String scenario)
	{	
		for(String uinlist:getupdateUINAttributes(JsonIdentity)) {
			if(schema.getGroup().equals(uinlist))
			{
				try {
					if(schema.isInputRequired() && (schema.getRequiredOn()).get(0).getExpr().contains("identity.isUpdate"))
						documentUploadPage.documentDropDownScan(schema, id, JsonIdentity, key);
					else if(schema.getRequiredOn().get(0).getExpr().contains("identity.isUpdate") &&
							(schema.getRequiredOn().get(0).getExpr().contains("identity.updatableFieldGroups contains '"+uinlist+"'")||
									schema.getRequiredOn().get(0).getExpr().contains("identity.updatableFields contains '"+schema.getId()+"'")	)
							)
						documentUploadPage.documentDropDownScan(schema, id, JsonIdentity, key);
				}catch(Exception e)
				{
					logger.error("",e);

				}
			}}


	}

	public void fileupload(Schema schema,String JsonIdentity,String key,String id,String scenario,String ageGroup)
	{
		try {
			if(schema.isInputRequired() && ( schema.isRequired() || (schema.getRequiredOn()).get(0).getExpr().contains("identity.isNew")))
				documentUploadPage.documentDropDownScan(schema, id, JsonIdentity, key);
			else if(
					(ageGroup.equalsIgnoreCase("MINOR")||ageGroup.equalsIgnoreCase("INFANT"))
					&&(schema.getRequiredOn().get(0).getExpr().contains("identity.isNew"))
					)
				documentUploadPage.documentDropDownScan(schema, id, JsonIdentity, key);

		}catch(Exception e)
		{
			logger.error("",e);

		}

	}

	public void dropdownUpdate(String id,String JsonIdentity,String key) {
		for(String uinlist:getupdateUINAttributes(JsonIdentity)) {
			if(schema.getGroup().equals(uinlist))
			{
				dropdown(id,JsonIdentity,key);
			}}
	}
	public void checkboxUpdate(String id,String JsonIdentity,String key) {
		for(String uinlist:getupdateUINAttributes(JsonIdentity)) {
			if(schema.getGroup().equals(uinlist))
			{

				checkSelection(id,JsonIdentity,key);
			}}
	}
	
	public void dropdown1(String id,String JsonIdentity,String key) {
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
				logger.error("",e);
		}
	}
	
//	public void dropdown(String id,String JsonIdentity,String key) {
//		GenericDto dto=new GenericDto();
//		try {
//			mapDropValue=null;
//			if(schema.getType().contains("simpleType"))
//			{
//				mapDropValue=JsonUtil.JsonObjSimpleParsing(JsonIdentity,key);
//				Set<String> dropkeys = mapDropValue.keySet();
//				for(String ky:dropkeys)
//				{	//dto.setCode(ky);
//				//dto.setLangCode(ky);
//				dto.setName(mapDropValue.get(ky));
//				user_selects_combo_item(id,dto);
//				//user_selects_combo_item1(id,mapDropValue.get(ky));
//				break;
//				}
//			}
//			else
//			{
//				String val=JsonUtil.JsonObjParsing(JsonIdentity, key);
//				dto.setName(val);
//				user_selects_combo_item(id,dto);
//				//user_selects_combo_item1(id,val);
//			}}catch(Exception e)
//		{
//				logger.error("",e);
//		}
//	}
//	
	//With Code
	public void dropdown(String id,String JsonIdentity,String key) {
		GenericDto dto=new GenericDto();
		try {
			mapDropValue=null;
			//if(schema.getType().contains("simpleType"))
			{
				mapDropValue=JsonUtil.JsonObjSimpleParsingWithCode(JsonIdentity,key);
				Set<String> dropkeys = mapDropValue.keySet();
				for(String ky:dropkeys)
				{	
					

					String valcode=mapDropValue.get(ky);
					String valcodeArr[]=valcode.split("@@");
					dto.setLangCode(ky);
					dto.setName(valcodeArr[0]);
					dto.setCode(valcodeArr[1]);
					user_selects_combo_item(id,dto);
				break;
				}
			}
//			else
//			{
//				String val=JsonUtil.JsonObjParsing(JsonIdentity, key);
//				dto.setName(val);
//				dto.setCode("ara");
//				user_selects_combo_item(id,dto);
//
//			}
			}catch(Exception e)
		{
				logger.error("",e);
		}
	}

	public void setageDate(String id,String JsonIdentity,String key)
	{
		String dateofbirth= null;
		try {
			dateofbirth = JsonUtil.JsonObjParsing(JsonIdentity,key);
		} catch(Exception e)
		{
			logger.error("",e);
		}
		setTextFields(id+"ageFieldTextField",dateofbirth);

	}

	public void setdob(String id,String JsonIdentity,String key)
	{
		String dateofbirth[] = null;
		try {
			dateofbirth = JsonUtil.JsonObjParsing(JsonIdentity,key).split("/");
			System.out.println(dateofbirth);
		} catch(Exception e)
		{
			logger.error("",e);}
		setTextFields(id+"ddTextField",dateofbirth[2]);
		setTextFields(id+"mmTextField",dateofbirth[1]);
		setTextFields(id+"yyyyTextField",dateofbirth[0]);


	}

	public void checkSelection(String id,String JsonIdentity,String key)
	{
		String flag = "N";
		try {
			flag = JsonUtil.JsonObjParsing(JsonIdentity,key);
		} catch(Exception e)
		{
			logger.error("",e);		}
		if(flag.equalsIgnoreCase("Y"))
		{

			waitsUtil.clickNodeAssert(id);

		}}

	public void setageDateUpdate(String id,String JsonIdentity,String key,String scenario)
	{
		for(String uinlist:getupdateUINAttributes(JsonIdentity)) {
			if(schema.getGroup().equals(uinlist))
			{				setageDate(id, JsonIdentity, key);
			}}}
	
	
	public void setdobUpdate(String id,String JsonIdentity,String key)
	{
		for(String uinlist:getupdateUINAttributes(JsonIdentity)) {
			if(schema.getGroup().equals(uinlist))
			{				setdob(id, JsonIdentity, key);
			}}}
	
	public void contolType(Schema schema,String JsonIdentity,String scenario,String ageGroup)
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
//				if(ageGroup.equalsIgnoreCase("MINOR")||ageGroup.equalsIgnoreCase("INFANT"))
//					getTextboxKeyValueChild(id,JsonIdentity,key,schema.isTransliterate(),scenario);
//				else
//					getTextboxKeyValue(id,JsonIdentity,key,schema.isTransliterate(),scenario);
				
				getTextboxKeyValue(id,JsonIdentity,key,schema.isTransliterate(),scenario);
				break;
			case "ageDate":
				//setdob(id,JsonIdentity,key);
				setageDate(id,JsonIdentity,key);
				break;
			case "date":
				setdob(id,JsonIdentity,key);
				
				break;
			case "dropdown": 
				dropdown(id,JsonIdentity,key);
				break;
			case "checkbox":
				checkSelection(id,JsonIdentity,key);
				break;
			case "fileupload":
				fileupload(schema,JsonIdentity,key,id,scenario,ageGroup);
				break;
			case "biometrics":
				biometrics(schema,scenario,id,JsonIdentity);
				break;

			}

		}
		catch(Exception e )
		{
			logger.error("",e);
		}
	}


	public void contolTypeUpdate(Schema schema,String JsonIdentity,String scenario,String ageGroup)
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
				getTextboxKeyValueUpdate(id,JsonIdentity,key,schema,schema.isTransliterate(),scenario,ageGroup);
				break;
			case "ageDate":
				setageDateUpdate(id,JsonIdentity,key,scenario);
				break;
			case "date":
				setdobUpdate(id,JsonIdentity,key);
				
				break;
			case "dropdown": 
				dropdownUpdate(id,JsonIdentity,key);
				break;
			case "checkbox":
				checkboxUpdate(id,JsonIdentity,key);

				break;
			case "fileupload":
				fileuploadUpdate(schema,JsonIdentity,key,id,scenario);
				break;
			case "biometrics":
				if(getupdateUINAttributes(JsonIdentity).contains(schema.getGroup()))
				{  // All Bio-- IRIS(U) <--- Remaining() based on age
					//biometricsupdate(schema,scenario,id,JsonIdentity);
					biometrics(schema,scenario,id,JsonIdentity);
				}else if( !getupdateUINAttributes(JsonIdentity).contains(schema.getGroup()) 
						&& 
						!getupdateUINAttributes(JsonIdentity).contains(PropertiesUtil.getKeyValue("GuardianDetails"))
						)
				{ //Single ---> AUTH
					biometricsAuth(schema,scenario,id,JsonIdentity);

				}
				else if( !getupdateUINAttributes(JsonIdentity).contains(schema.getGroup()) 
						&& 
						getupdateUINAttributes(JsonIdentity).contains(PropertiesUtil.getKeyValue("GuardianDetails")))
				{//Only Introducer
					//biometricsupdate(schema,scenario,id,JsonIdentity);
					biometrics(schema,scenario,id,JsonIdentity);

				}
				break;

			}

		}
		catch(Exception e )
		{
			logger.error("",e);
		}
	}



	private void getTextboxKeyValue(String id, String JsonIdentity, String key, boolean transliterate,
			String scenario) {
		logger.info("Schema Control Type textbox");
		mapValue=null;
		if(schema.isInputRequired())
		{	if(schema.getType().contains("simpleType"))
		{
			try {
				mapValue=JsonUtil.JsonObjSimpleParsing(JsonIdentity,key);
			} catch(Exception e)
			{
				logger.error("",e);
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
			}catch(Exception e)
			{
				logger.error("",e);
			}
			try {
				String[] listLang=PropertiesUtil.getKeyValue("langcode").split("@@");

				setTextFields(id+listLang[0],value);
			} catch (IOException e) {

				logger.error("",e);
			}
		}
		}

	}

	private void getTextboxKeyValue1(String id, String JsonIdentity, String key, boolean transliterate,
			String scenario) {
		logger.info("Schema Control Type textbox");
		mapValue=null;
		if(schema.isInputRequired())
		{	if(schema.getType().contains("simpleType"))
		{
			try {
				mapValue=JsonUtil.JsonObjSimpleParsing(JsonIdentity,key);
			} catch(Exception e)
			{
				logger.error("",e);
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
			}catch(Exception e)
			{
				logger.error("",e);
			}
			try {
				String[] listLang=PropertiesUtil.getKeyValue("langcode").split("@@");

				setTextFields(id+listLang[0],value);
			} catch (IOException e) {

				logger.error("",e);
			}
		}
		}

	}

}




