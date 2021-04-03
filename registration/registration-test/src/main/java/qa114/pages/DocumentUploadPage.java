package qa114.pages;

import java.util.concurrent.TimeoutException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import qa114.pojo.schema.Schema;
import qa114.pojo.testdata.TestData;
import qa114.utility.WaitsUtil;

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
	public void selectDocumentScan() throws InterruptedException, TimeoutException
	{	logger.info("In DocumentUploadPage Constructor");
		waitsUtil.clickNodeAssert( captureBtn);
		waitsUtil.clickNodeAssert (success);

		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);

		waitsUtil.clickNodeAssert( saveBtn);

		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);

	}


	public void user_selects_combo_item(FxRobot robot,String comboBoxId, DocumentCategoryDto dto) throws InterruptedException {

		

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				ComboBox comboBox= waitsUtil.lookupById(comboBoxId);

				comboBox.getSelectionModel().select(dto); 

			}});

		Thread.sleep(800);
		logger.info( comboBoxId +  dto +"CHOOSEN" );
	}

	public void documentScan( String testdata, Schema schema, String id) throws InterruptedException, TimeoutException

	{

		documentCategoryDto.setName(testdata);
		documentCategoryDto.setCode(schema.getSubType());

		user_selects_combo_item(robot,id,documentCategoryDto);

		String scanBtn="#"+schema.getSubType();

		Button scanButton = waitsUtil.lookupByIdButton(scanBtn,robot);

		//waitsUtil.lookupById(docPreviewImgViewPane);

		robot.clickOn(scanButton);
		selectDocumentScan();

	}
}

