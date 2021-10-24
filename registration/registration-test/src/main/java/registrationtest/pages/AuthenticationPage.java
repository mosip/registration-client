package registrationtest.pages;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import registrationtest.controls.Buttons;
import registrationtest.utility.WaitsUtil;

public class AuthenticationPage {

	private static final Logger logger = LogManager.getLogger(AuthenticationPage.class); 
	FxRobot robot;
	WaitsUtil waitsUtil;
	String AuthenticationImg="#AuthenticationImg";
	String password="#password";
	String username="#username";
	String submitbtn="#submitbtn";

	public AuthenticationPage(FxRobot robot)
	{
		logger.info("Constructor AuthenticationPage ");
		this.robot=robot;
		waitsUtil=new WaitsUtil(robot);
		//waitsUtil.clickNodeAssert(robot, AuthenticationImg);
		 
	}
	
	public void enterPassword(String pwd)
	{
		logger.info("enterPassword");
		waitsUtil.clickNodeAssert( password);
		robot.write(pwd);
	}
	
	public void enterUserName(String userid)
	{
		logger.info("enterUserName");
		waitsUtil.clickNodeAssert(username);
		robot.write(userid);
	}
	
	public void clicksubmitBtn()
	{logger.info("clicksubmitBtn");
	
		waitsUtil.clickNodeAssert(submitbtn);
	}

}
