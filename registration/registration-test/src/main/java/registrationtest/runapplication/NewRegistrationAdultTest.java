package registrationtest.runapplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.testfx.api.FxRobot;
import org.testfx.osgi.service.TestFx;
import org.testfx.util.WaitForAsyncUtils;

import com.aventstack.extentreports.ExtentReporter;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;

import registrationtest.pojo.output.RID;
import registrationtest.testcases.*;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.RobotActions;
import javafx.application.Application;
import javafx.application.Platform;

import org.apache.log4j.BasicConfigurator;  
import org.apache.log4j.LogManager;  
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import static org.testfx.assertions.api.Assertions.assertThat;
import static org.testfx.api.FxAssert.assertContext;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isNull;

public class NewRegistrationAdultTest{

	private static final Logger logger = LogManager.getLogger(NewRegistrationAdultTest.class);  
	static FxRobot robot;
	static String[] Strinrid;
	static RID rid1,rid2,rid3,rid4;
	static String[] scenario;
	
	public static void invokeRegClientNewReg(
		
			String operatorId,String operatorPwd,
			String supervisorId,
			String supervisorPwd
			) 
	{
		LoginNewRegLogout loginNewRegLogout=new LoginNewRegLogout();  
		LostUINLogout lostUINLogout=new LostUINLogout();
		ChildNewReg childNewReg=new ChildNewReg(); 
		ChildLostUIN childLostUIN=new ChildLostUIN();

		Thread thread = new Thread() { 
			@Override
			public void run() {
				try {

					logger.info("thread inside calling testcase"); 

					Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ApplicationLaunchTimeWait"))); 
					robot=new FxRobot();
					ExtentReportUtil.ExtentSetting();
				
					scenario=PropertiesUtil.getKeyValue("scenario").split("@@");
					for(String sc : scenario )
					{
						switch(sc) {
						case "adult": 
							rid1=loginNewRegLogout.newRegistrationAdult(robot,operatorId, operatorPwd,supervisorId,supervisorPwd,
							StartApplication.primaryStage,readJsonFileText("path.idjson.adult"),
							sc,rid1);
					logger.info("RID RESULTS-"+ rid1.result +"\t"+ rid1.ridDateTime +"\t"+ rid1.rid + "\t "+ rid1.firstName );
					ExtentReportUtil.reports.flush();
					break;
						case "lostadult":
							
					 rid2=lostUINLogout.LostUINAdult(robot,operatorId, operatorPwd,supervisorId,supervisorPwd,
							StartApplication.primaryStage,readJsonFileText("path.idjson.adult"),
							rid1,sc);
					logger.info("RID RESULTS-"+ rid2.result +"\t"+ rid2.ridDateTime +"\t"+ rid2.rid);
					ExtentReportUtil.reports.flush();
					break;
						case "child":
							
					 rid3=childNewReg.newRegistrationChild(robot,operatorId, operatorPwd,supervisorId,supervisorPwd,
							StartApplication.primaryStage,readJsonFileText("path.idjson.child"),
							rid1,sc);
					logger.info("RID RESULTS-"+ rid3.result +"\t"+ rid3.ridDateTime +"\t"+ rid3.rid);
					ExtentReportUtil.reports.flush();
					break;
						case "lostchild":
							
							 rid4=childLostUIN.newRegistrationChildLost(robot,operatorId, operatorPwd,supervisorId,supervisorPwd,
									StartApplication.primaryStage,readJsonFileText("path.idjson.child"),
									rid1,sc);
							logger.info("RID RESULTS-"+ rid4.result +"\t"+ rid4.ridDateTime +"\t"+ rid4.rid);
							ExtentReportUtil.reports.flush();
							break;
						
						}
					}
				System.exit(0);	
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
				try {
				System.setProperty("java.net.useSystemProxies", "true");
				System.setProperty("file.encoding", "UTF-8");
				System.setProperty("derby.ui.codeset", "UTF-8");
				System.setProperty("jdbc.drivers","org.apache.derby.jdbc.EmbeddedDriver");
				System.setProperty("mosip.hostname",PropertiesUtil.getKeyValue("mosip.hostname"));
				
				invokeRegClientNewReg(
						PropertiesUtil.getKeyValue("operatorId"), 
						PropertiesUtil.getKeyValue("operatorPwd"),
						PropertiesUtil.getKeyValue("supervisorUserid"), 
						PropertiesUtil.getKeyValue("supervisorUserpwd"));
				}catch(Exception e)
				{
					logger.error(e.getMessage());
				}
				
			}

			
			
			
			public static String readJsonFileText(String key)
			{
				String jsonTxt=null;
				try {
				File f = new File(System.getProperty("user.dir")+PropertiesUtil.getKeyValue(key));
				
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


