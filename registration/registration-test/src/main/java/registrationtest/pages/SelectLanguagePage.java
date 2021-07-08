package registrationtest.pages;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class SelectLanguagePage {
private static final Logger logger = LogManager.getLogger(SelectLanguagePage.class); 
	
	FxRobot robot;
	String code;
	String[] langCodeList;
	WaitsUtil waitsUtil;
	
	
	public SelectLanguagePage(FxRobot robot)
	{
		logger.info("LoginPage Constructor");
		
		this.robot=robot;
		waitsUtil=new WaitsUtil(robot);
	}
	
	
	public void selectLang()
	{	try {
		code=PropertiesUtil.getKeyValue("langcode");
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		langCodeList=code.split("@@");
		
		for(String code:langCodeList)
		{
			waitsUtil.clickNodeAssert("#"+code);
		}
	}
}
