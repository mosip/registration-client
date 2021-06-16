package registrationtest.controls;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import javafx.scene.control.Button;
import registrationtest.runapplication.NewRegistrationAdultTest;
import registrationtest.utility.WaitsUtil;

public class Alerts {

	private static final Logger logger = LogManager.getLogger(Alerts.class); 
	String alertImage="#alertImage";
	String exit="#exit";
	String success="#context";
	String cancel="#cancel";
	String confirm="#confirm";

	WaitsUtil waitsUtil;
	FxRobot robot;
	/**
	 * Alerts Constuctor
	 * @param robot
	 */
	public Alerts(FxRobot robot)
	{
		this.robot=robot;
		waitsUtil=new WaitsUtil(robot);
		logger.info("In Alerts Constructor");
	}

	/**
	 * clickAlertImage - For example Tick , Cross
	 */
	public void clickAlertImage()
	{
		waitsUtil.clickNodeAssert(alertImage);
		logger.info("clickAlertImage");
	}

	/**
	 * clickAlertexit - Close X
	 */
	public void clickAlertexit()
	{
		waitsUtil.clickNodeAssert(exit);
		logger.info("clickAlertexit");
	}

	/**
	 * clickAlertSuccess - Success Text
	 */
	public void clickAlertSuccess()
	{
		waitsUtil.clickNodeAssert(success);
		logger.info("clickAlertSuccess");
	}
	
	/**
	 * clickAlertcancel - cancel Text
	 */
	public void clickAlertCancel()
	{
		waitsUtil.clickNodeAssert(cancel);
		logger.info("clickAlertCancel");
	}

	/**
	 * clickAlertConfirm - confirm Text
	 */
	public void clickAlertConfirm()
	{
		waitsUtil.clickNodeAssert(confirm);
		logger.info("clickAlertConfirm");
	}
	
	
}
