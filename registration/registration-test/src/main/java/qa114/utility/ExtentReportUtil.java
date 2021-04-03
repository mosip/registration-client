package qa114.utility;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;


public class ExtentReportUtil {
private static final Logger logger = LogManager.getLogger(ExtentReportUtil.class); 
	
	  public static String REPORTPATH=System.getProperty("user.dir") +"/test-output/extentReport.html";
	  public static ExtentHtmlReporter htmlReporter;
	  public static ExtentReports reports;
	  public static ExtentTest test;
	  public static ExtentTest step1,step2,step3,step4,step5,step6;
	  
	  
	  public static void ExtentSetting()
	  {
		  htmlReporter=new ExtentHtmlReporter(REPORTPATH);

		  reports=new ExtentReports();
		  reports.attachReporter(htmlReporter);
		  
		  
	  }
	
	
}
