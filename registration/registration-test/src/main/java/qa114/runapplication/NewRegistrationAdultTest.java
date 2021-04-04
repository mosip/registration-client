package qa114.runapplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.testfx.api.FxRobot;

import com.aventstack.extentreports.ExtentReporter;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;

import qa114.pojo.output.RID;
import qa114.testcases.*;
import qa114.utility.ExtentReportUtil;
import qa114.utility.PropertiesUtil;
import qa114.utility.RobotActions;
import javafx.application.Application;
import javafx.application.Platform;

import org.apache.log4j.BasicConfigurator;  
import org.apache.log4j.LogManager;  
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;  


public class NewRegistrationAdultTest{

	private static final Logger logger = LogManager.getLogger(NewRegistrationAdultTest.class);  
	static FxRobot robot;
	static String[] Strinrid;

	public static void invokeRegClientNewReg(String jsonIdentity,
			HashMap<String, String> documentUpload,
			String operatorId,String operatorPwd,
			String supervisorId,
			String supervisorPwd,Double schemaVersion,String targetUrl
			) 
	{
		LoginNewRegLogout lg=new LoginNewRegLogout();  

		Thread thread = new Thread() { 
			@Override
			public void run() {
				try {

					logger.info("thread inside calling testcase"); 

					Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ApplicationLaunchTimeWait"))); 
					robot=new FxRobot();

					RID rid=lg.newRegistrationAdult(robot,operatorId, operatorPwd,supervisorId,supervisorPwd,
							StartApplication.primaryStage,jsonIdentity,
							documentUpload,schemaVersion);

					logger.info("RID RESULTS-"+ rid.result +"\t"+ rid.ridDateTime +"\t"+ rid.rid);
					ExtentReportUtil.reports.flush();

				} catch (Exception e) {

					logger.error(e.getMessage());
				}	
			}

		};

		thread.start();

		String args[]= {};
		Application.launch(StartApplication.class, args); 



	}
	public static void invokeRegClient(
			String operatorId,String operatorPwd,String targetUrl
			) 
	{
		LoginNewRegLogout lg=new LoginNewRegLogout();  
		Thread thread = new Thread() { 
			@Override
			public void run() {
				try {

						logger.info("thread inside calling testcase"); 

						Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ApplicationLaunchTimeWait"))); 
						robot=new FxRobot();
						boolean flag=lg.initialRegclientSet(robot, operatorId, operatorPwd, StartApplication.primaryStage);
						if(flag==true)
							logger.info("Initial setup Done");
						else
							logger.info("Initial setup not required");
						ExtentReportUtil.reports.flush();

					} catch (Exception e) {
						logger.error(e.getMessage());
					}

			}};

				thread.start();

				String args[]= {};
				Application.launch(StartApplication.class, args); 
			}

			public static void main(String[] args)  { 
				///TBD
//				System.setProperty("testfx.robot", "glass");
//			    System.setProperty("testfx.headless", "true");
//			    System.setProperty("prism.order", "sw");
//			    System.setProperty("prism.text", "t2k");
//			    System.setProperty("java.awt.headless", "true");
//			    
			    ///////TBD
				
				try {
				String targetUrl=PropertiesUtil.getKeyValue("mosip.upgradeserver");
				String hostName=targetUrl.substring(targetUrl.indexOf("//")+2);

				System.setProperty("java.net.useSystemProxies", "true");
				System.setProperty("file.encoding", "UTF-8");
				System.setProperty("derby.ui.codeset", "UTF-8");

				System.setProperty("mosip.upgradeserver",targetUrl+"/");

				System.setProperty("mosip.hostname",hostName);

				System.setProperty("jdbc.drivers","org.apache.derby.jdbc.EmbeddedDriver");

				//	invokeRegClient(PropertiesUtil.getKeyValue("operatorId"), 
				//		PropertiesUtil.getKeyValue("operatorPwd"),PropertiesUtil.getKeyValue("mosip.upgradeserver"));

				invokeRegClientNewReg(readJsonFileText(),readMapDocumentValues(),
						PropertiesUtil.getKeyValue("operatorId"), 
						PropertiesUtil.getKeyValue("operatorPwd"),
						PropertiesUtil.getKeyValue("supervisorUserid"), 
						PropertiesUtil.getKeyValue("supervisorUserpwd"),
						Double.parseDouble(PropertiesUtil.getKeyValue("schemaversion")),
						PropertiesUtil.getKeyValue("mosip.upgradeserver"));
				}catch(Exception e)
				{
					logger.error(e.getMessage());
				}
				System.exit(0);	
			}

			public static String readJsonFileText()
			{
				String jsonTxt=null;
				try {
				File f = new File(System.getProperty("user.dir")+PropertiesUtil.getKeyValue("IDJsonPath"));
				
				if (f.exists()){
					InputStream is = new FileInputStream(f);
					jsonTxt = IOUtils.toString(is, "UTF-8");
					System.out.println(jsonTxt);
					logger.info("readJsonFileText");

				}}
				catch(Exception e)
				{
					logger.error(e.getMessage());
				}
				return jsonTxt;
			}


			public static HashMap<String, String> readMapDocumentValues()
			{
				HashMap<String, String> map=new HashMap<String, String>();
				map.put("proofOfConsent","Registration Form");
				map.put("proofOfAddress","Barangay ID");
				map.put("proofOfIdentity","Postal ID");

				return map;

			}

		}



		/*
String hostName=targetUrl.substring(targetUrl.indexOf("//")+2);

System.setProperty("java.net.useSystemProxies", "true");
System.setProperty("file.encoding", "UTF-8");
System.setProperty("derby.ui.codeset", "UTF-8");
System.setProperty("mosip.upgradeserver",targetUrl+"/");
System.setProperty("mosip.hostname",hostName);
System.setProperty("jdbc.drivers","org.apache.derby.jdbc.EmbeddedDriver");
//if (Boolean.getBoolean("headless")) {
    System.setProperty("testfx.robot", "glass");
    System.setProperty("testfx.headless", "true");
    System.setProperty("prism.order", "sw");
    System.setProperty("prism.text", "t2k");
    System.setProperty("java.awt.headless", "true");
//}
		 */


