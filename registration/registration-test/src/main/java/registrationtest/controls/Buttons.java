package registrationtest.controls;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import javafx.scene.control.Button;
import registrationtest.utility.WaitsUtil;

public class Buttons {

	private static final Logger logger = LogManager.getLogger(Buttons.class); 
	String continueBtn="#continueBtn",
			backBtn="#backBtn",
			Confirm="Confirm",
			uploadBtn="#uploadBtn";


	WaitsUtil waitsUtil;
	FxRobot robot;
	/**
	 * {@summary} Buttons Constructor
	 * @param robot
	 */
	public Buttons(FxRobot robot)
	{
		logger.info("In Button Constructor");
		this.robot=robot;
		waitsUtil=new WaitsUtil(robot);
		
	}

	/**
	 * {@summary}  - Continue Button Click
	 */
	public void clickContinueBtn()
	{
		logger.info("clickContinueBtn");
		waitsUtil.clickNodeAssert( continueBtn);
		
	}

	/**
	 * {@summary} Back Button Click
	 */
	public void clickBackBtn()
	{
		logger.info("clickBackBtn");
		waitsUtil.clickNodeAssert( backBtn);
		
	}

	/**
	 * Confirm Button Click
	 */
	public void clickConfirmBtn()
	{
		logger.info("clickConfirmBtn");
		waitsUtil.clickNodeAssert( Confirm);
	}
	
	/**
	 * Upload Button Click
	 */
	public void clickuploadBtn()
	{
		logger.info("clickuploadBtn");
		waitsUtil.clickNodeAssert( uploadBtn);
	}
}
