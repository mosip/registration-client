package qa114.pages;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import qa114.utility.WaitsUtil;

public class EodApprovalPage {

	private static final Logger logger = LogManager.getLogger(EodApprovalPage.class); 
	FxRobot robot;
	WaitsUtil waitsUtil;
	String filterField="#filterField";
	String approvalBtn="#approvalBtn";
	String authenticateBtn="#authenticateBtn";
	EodApprovalPage(FxRobot robot)
	{this.robot=robot;
	waitsUtil=new WaitsUtil(robot);
	}
	
	public void clickOnfilterField()
	{
		waitsUtil.clickNodeAssert( filterField);
	}

	public void enterFilterDetails(String rid) {
		// TODO Auto-generated method stub
		robot.write(rid);
	}

	public void clickOnApprovalBtn()
	{
		waitsUtil.clickNodeAssert( approvalBtn);
	}
	
	public AuthenticationPage clickOnAuthenticateBtn()
	{
		waitsUtil.clickNodeAssert( authenticateBtn);
		return new AuthenticationPage(robot);
	}
	
	
	
}
