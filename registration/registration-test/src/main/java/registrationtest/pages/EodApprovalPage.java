package registrationtest.pages;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import registrationtest.utility.WaitsUtil;

public class EodApprovalPage {

	private static final Logger logger = LogManager.getLogger(EodApprovalPage.class); 
	FxRobot robot;
	WaitsUtil waitsUtil;
	String filterField="#filterField";
	String approvalBtn="#approvalBtn";
	String authenticateBtn="#authenticateBtn";
	EodApprovalPage(FxRobot robot)
	{
		logger.info("EodApprovalPage Constructor");
		this.robot=robot;
	waitsUtil=new WaitsUtil(robot);
	}
	
	public void clickOnfilterField()
	{
		logger.info("clickOnfilterField ");
		waitsUtil.clickNodeAssert( filterField);
	}

	public void enterFilterDetails(String rid) {
		logger.info("enterFilterDetails RID " + rid);
		// TODO Auto-generated method stub
		robot.write(rid);
	}

	public void clickOnApprovalBtn()
	{
		logger.info("clickOnApprovalBtn ");
		waitsUtil.clickNodeAssert( approvalBtn);
	}
	
	public AuthenticationPage clickOnAuthenticateBtn()
	{
		logger.info("clickOnAuthenticateBtn ");
		waitsUtil.clickNodeAssert( authenticateBtn);
		return new AuthenticationPage(robot);
	}
	
	
	
}
