package registrationtest.pages;

import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import javafx.scene.control.TextField;

//import com.itextpdf.text.log.SysoCounter;

import javafx.scene.input.KeyCode;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.RobotActions;
import registrationtest.utility.WaitsUtil;

public class UploadPacketPage {
	private static final Logger logger = LogManager.getLogger(UploadPacketPage.class); 
	
	FxRobot robot;
	WaitsUtil waitsUtil;
	TextField filterField;
	String filterField1="#filterField";
	String approvalBtn="#approvalBtn";
	String authenticateBtn="#authenticateBtn";
	String uploaded="PUSHED";
	RobotActions robotActions;
	UploadPacketPage(FxRobot robot)
	{
		logger.info("UploadPacketPage Constructor");
		this.robot=robot;
	waitsUtil=new WaitsUtil(robot);
	robotActions=new RobotActions(robot);
	}
	
	public void clickOnfilterField(FxRobot robot)
	{
		logger.info("clickOnfilterField");
		waitsUtil.clickNodeAssert( filterField1);
	}

	public void enterFilterDetails(String rid) {
		// TODO Auto-generated method stub
		logger.info("enter rid Details" + rid);
		robot.write(rid);
	}

	public void clickOnApprovalBtn(FxRobot robot)
	{
		logger.info("clickOnApprovalBtn");
		waitsUtil.clickNodeAssert( approvalBtn);
	}
	
	public AuthenticationPage clickOnAuthenticateBtn(FxRobot robot)
	{
		logger.info("clickOnAuthenticateBtn");
		waitsUtil.clickNodeAssert( authenticateBtn);
		return new AuthenticationPage(robot);
	}

	public void selectPacket(String rid) {
		
		logger.info("selectPacket" + rid);
		filterField=waitsUtil.lookupByIdTextField(filterField1, robot);
		filterField.setText(rid);
		
		waitsUtil.clickNodeAssert( rid);
		robot.press(KeyCode.TAB).release(KeyCode.TAB);
		robot.press(KeyCode.TAB).release(KeyCode.TAB);
		robot.press(KeyCode.SPACE).release(KeyCode.SPACE);
		
		
		
	}

	public Boolean verifyPacketUpload(String rid) {
		Boolean result=false;
		logger.info("verifyPacketUpload" + rid);
		try {
			Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("PacketUploadTimeWait")));
			waitsUtil.clickNodeAssert( uploaded);
			
			waitsUtil.clickNodeAssert( rid);
			result=true;
			robotActions.closeWindow();
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.info("Failure Unable to upload");
			result=false;
			e.printStackTrace();
		} 
		
		
		return result;

	}
	
	
	
}
