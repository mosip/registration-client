package registrationtest.pages;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import javafx.application.Platform;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import registrationtest.pojo.schema.Schema;
import registrationtest.utility.JsonUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class DocumentUploadPage {
	private static final Logger logger = LogManager.getLogger(DocumentUploadPage.class); 

	FxRobot robot;
	WaitsUtil waitsUtil;
	String captureBtn="#captureBtn";
	String success="Success";
	String saveBtn="#saveBtn";
	String UploadDocImg="#UploadDocImg";

	String docPreviewImgViewPane="#docPreviewImgViewPane";
	DocumentCategoryDto documentCategoryDto;

	DocumentUploadPage(FxRobot robot)
	{
		logger.info("In DocumentUploadPage Constructor");
		this.robot=robot;
		waitsUtil=new WaitsUtil(robot);
		//waitsUtil.clickNodeAssert(robot, UploadDocImg);

		documentCategoryDto=new DocumentCategoryDto();


	}
	public void selectDocumentScan()
	{	logger.info("In selectDocumentScan");
	try {


		waitsUtil.clickNodeAssert( captureBtn);
		waitsUtil.clickNodeAssert (success);

 		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);

		waitsUtil.clickNodeAssert( saveBtn);

		//robot.press(KeyCode.SPACE).release(KeyCode.SPACE);
	}catch(Exception e)
	{
		logger.error(e.getMessage());
	}

	}


	public void user_selects_combo_item(FxRobot robot,String comboBoxId, DocumentCategoryDto dto)  {

		try {


			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					ComboBox comboBox= waitsUtil.lookupById(comboBoxId);

					comboBox.getSelectionModel().select(dto); 

				}});

			Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ComboItemTimeWait"))); 

		} catch (InterruptedException | NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
		logger.info( comboBoxId +  dto +"CHOOSEN" );
	}

	public void documentScan( String testdata, Schema schema, String id) 

	{
		try {

			documentCategoryDto.setName(testdata);
			documentCategoryDto.setCode(schema.getSubType());
			documentCategoryDto.setLangCode("eng");
			user_selects_combo_item(robot,id,documentCategoryDto);

			String scanBtn=id+"button";

			Button scanButton = waitsUtil.lookupByIdButton(scanBtn,robot);

			//waitsUtil.lookupById(docPreviewImgViewPane);

			robot.clickOn(scanButton);
			selectDocumentScan();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
	}

	public void documentDropDownScan(Schema schema,String id,String JsonIdentity,String key,boolean trans) {
		LinkedHashMap<String,String> mapDropValue;
	
		try {
			mapDropValue=null;
			if(schema.getType().contains("documentType"))
			{
				mapDropValue=JsonUtil.JsonObjSimpleParsing(JsonIdentity,key);
				Set<String> dropkeys = mapDropValue.keySet();
				for(String ky:dropkeys)
				{

					documentCategoryDto.setCode(ky);
					documentCategoryDto.setLangCode(ky);
					documentCategoryDto.setName(mapDropValue.get(ky));
					user_selects_combo_item(robot,id,documentCategoryDto);
					String scanBtn=id+"button";

					Button scanButton = waitsUtil.lookupByIdButton(scanBtn,robot);

					//waitsUtil.lookupById(docPreviewImgViewPane);

					robot.clickOn(scanButton);
					selectDocumentScan();
					break;
				}
			}
			else if (trans==false){

				mapDropValue=JsonUtil.JsonObjSimpleParsingnoTranslate(JsonIdentity,key);
				Set<String> dropkeys = mapDropValue.keySet();
				for(String ky:dropkeys)
				{

					documentCategoryDto.setCode(ky);
					documentCategoryDto.setLangCode(ky);
					documentCategoryDto.setName(mapDropValue.get(ky));
					user_selects_combo_item(robot,id,documentCategoryDto);
					String scanBtn=id+"button";

					Button scanButton = waitsUtil.lookupByIdButton(scanBtn,robot);

					//waitsUtil.lookupById(docPreviewImgViewPane);

					robot.clickOn(scanButton);
					selectDocumentScan();
					break;
				}
			}
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
	}

}

