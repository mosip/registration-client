
package registrationtest.utility;

import java.nio.file.Path;
import java.nio.file.Paths;




import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;

public class ExtentReportUtil {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(ExtentReportUtil.class);

    public static Path REPORTPATH = Paths.get(System.getProperty("user.dir"), "report",
            "extentReport" + DateUtil.getDateTime() + ".html");

    public static ExtentHtmlReporter htmlReporter;
    public static ExtentReports reports;
    public static ExtentTest test1, test2, test3, test4, test5, test6, test7;
    public static ExtentTest step1, step2, step3, step4, step5, step6, step7, step8, step9;

    public static void ExtentSetting() {
        htmlReporter = new ExtentHtmlReporter(REPORTPATH.toString());

        reports = new ExtentReports();
        reports.attachReporter(htmlReporter);

    }

}
