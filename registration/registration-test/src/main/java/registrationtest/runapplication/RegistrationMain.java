package registrationtest.runapplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Set;

import com.sun.javafx.application.LauncherImpl;
import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.preloader.ClientPreLoader;
import org.apache.commons.io.IOUtils;
import org.testfx.api.FxRobot;
import com.aventstack.extentreports.Status;
import registrationtest.pojo.output.RID;
import registrationtest.testcases.*;
import registrationtest.utility.DateUtil;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.HeadlessMode;
import registrationtest.utility.JsonUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;



public class RegistrationMain {

    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(RegistrationMain.class);
    static FxRobot robot;
    static String[] Strinrid;
    static RID rid1, rid2, rid3, rid4, rid5, rid6;
    static String process, ageGroup;

    public static void invokeRegClient(String operatorId, String operatorPwd, String supervisorId, String supervisorPwd,
        String reviewerUserid, String reviewerpwd) {
        NewReg loginNewRegLogout = new NewReg();
        LostReg lostUINLogout = new LostReg();
        UpdateReg updatereg = new UpdateReg();
        ManualReg manualReg = new ManualReg();
        WaitsUtil waitsUtil = new WaitsUtil();
        BioCorrectionReg bioCorrection = new BioCorrectionReg();
        Boolean flag;
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {

                    //loop until application primary stage is not loaded
                    while (ClientApplication.getPrimaryStage() == null) {Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ApplicationLaunchTimeWait")));
}

                    logger.info("Application primary stage setup completed "+DateUtil.getDateTime());
                    Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ApplicationLaunchTimeWait")));

                    robot = new FxRobot();
                    keepClientInFront(ClientApplication.getPrimaryStage());
                    ExtentReportUtil.ExtentSetting();

                    LinkedHashMap<String, String> map = readJsonFileText(
                            readFolderJsonList(PropertiesUtil.getKeyValue("datadir")));
                    

                    String manualFlag = PropertiesUtil.getKeyValue("manual");
                    if (manualFlag.equalsIgnoreCase("Y")) {
                        manualReg.manualRegistration(robot, operatorId, operatorPwd, supervisorId, supervisorPwd,
                                ClientApplication.getPrimaryStage(), ClientApplication.getApplicationContext());
                    }

                    Set<String> fileNameSet = map.keySet();
                    for (String fileName : fileNameSet) {
                        String jsonContent=null,process = null;
                       
                            jsonContent = map.get(fileName);
                             process = PropertiesUtil.getKeyValue("process");
                            process = JsonUtil.JsonObjParsing(jsonContent, process);
                       
                        try {
                            switch (process) {
                            case "New":
                                rid1 = null;
                                ageGroup = JsonUtil.JsonObjParsing(jsonContent, "ageGroup");
                                rid1 = loginNewRegLogout.newRegistration(robot, operatorId, operatorPwd, supervisorId,
                                        supervisorPwd, ClientApplication.getPrimaryStage(), jsonContent, process, ageGroup,
                                        fileName, ClientApplication.getApplicationContext());
                                if (rid1 != null) {
                                    logger.info("RID RESULTS-" + rid1.result + "\t" + rid1.ridDateTime + "\t" + rid1.rid);
                                }
                                ExtentReportUtil.reports.flush();
                                break;
                            case "Lost":
                                rid2 = null;
                                ageGroup = JsonUtil.JsonObjParsing(jsonContent, "ageGroup");
                                rid2 = lostUINLogout.lostRegistration(robot, operatorId, operatorPwd, supervisorId,
                                        supervisorPwd, ClientApplication.getPrimaryStage(), jsonContent, process, ageGroup,
                                        fileName, ClientApplication.getApplicationContext());
                                if (rid2 != null) {
                                    logger.info("RID RESULTS-" + rid2.result + "\t" + rid2.ridDateTime + "\t" + rid2.rid);
                                }
                                ExtentReportUtil.reports.flush();
                                break;
                            case "Update":
                                rid5 = null;
                                ageGroup = JsonUtil.JsonObjParsing(jsonContent, "ageGroup");
                                rid5 = updatereg.updateRegistration(robot, operatorId, operatorPwd, supervisorId,
                                        supervisorPwd, ClientApplication.getPrimaryStage(), jsonContent, process, ageGroup,
                                        fileName, ClientApplication.getApplicationContext());
                                if (rid5 != null) {
                                    logger.info("RID RESULTS-" + rid5.result + "\t" + rid5.ridDateTime + "\t" + rid5.rid);
                                }
                                ExtentReportUtil.reports.flush();
                                break;
                            case "bioCorrection":
                                rid1 = null;
                                ageGroup = JsonUtil.JsonObjParsing(jsonContent, "ageGroup");
                                try {
                                    jsonContent = BioCorrectionSeed.prepare(robot, operatorId, operatorPwd,
                                            supervisorId, supervisorPwd, ClientApplication.getPrimaryStage(),
                                            jsonContent, fileName, ClientApplication.getApplicationContext());
                                } catch (Exception e) {
                                    logger.error("BIOMETRIC_CORRECTION request could not be prepared", e);
                                    ExtentReportUtil.test1 = ExtentReportUtil.reports.createTest(
                                            "New Registration Scenario : " + process + " FileName : " + fileName);
                                    ExtentReportUtil.test1.log(Status.FAIL,
                                            "BIOMETRIC_CORRECTION request could not be prepared: " + e.getMessage());
                                    ExtentReportUtil.reports.flush();
                                    break;
                                }
                                rid1 = bioCorrection.bioCorrection(robot, operatorId, operatorPwd, supervisorId,
                                        supervisorPwd, ClientApplication.getPrimaryStage(), jsonContent, process, ageGroup,
                                        fileName, ClientApplication.getApplicationContext());
                                if (rid1 != null) {
                                    logger.info("RID RESULTS-" + rid1.result + "\t" + rid1.ridDateTime + "\t" + rid1.rid);
                                }
                                ExtentReportUtil.reports.flush();
                                break;
                            case "InitialLaunch":
                                Boolean flag = false;
                                flag = loginNewRegLogout.initialRegclientSet(robot, operatorId, operatorPwd,fileName,
                                        ClientApplication.getPrimaryStage());
                                logger.info("Operator Onboarding status=" + flag);
                                ExtentReportUtil.reports.flush();
                                break;
                            case "OperatorOnboard":
                                 loginNewRegLogout.operatorOnboard(robot, operatorId, operatorPwd,jsonContent,fileName,
                                         ClientApplication.getPrimaryStage());
                                logger.info("Operator Onboarding");
                                ExtentReportUtil.reports.flush();
                                break;
                            case "ReviewerOnboard":
                                
                                loginNewRegLogout.operatorOnboard(robot, reviewerUserid,
                                        reviewerpwd, jsonContent,fileName,ClientApplication.getPrimaryStage());
                                logger.info("Operator Onboarding ");
                                ExtentReportUtil.reports.flush();
                                break;
                            default:
                                logger.info("Choose correct process for automation or go with manual flow");

                            }
                        } catch (Throwable e) {

                            logger.error("", e);

                            ExtentReportUtil.test1.log(Status.FAIL, "TESTCASE FAIL");
                            ExtentReportUtil.reports.flush();
                            waitsUtil.capture();
                        }

                    }

                    if (!manualFlag.equalsIgnoreCase("Y"))
                        System.exit(0);
                } catch (InterruptedException e) {
                    logger.error("", e);
                    ExtentReportUtil.reports.flush();
                    waitsUtil.capture();
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                	logger.error("", e);
                    ExtentReportUtil.reports.flush();
                    waitsUtil.capture();
				}
            }

        };

        thread.start();

        String args[] = {};
        LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);
    }
    
    public static void invokeRegClientInitialLaunch(String operatorId, String operatorPwd, String supervisorId, String supervisorPwd,
			String reviewerUserid, String reviewerpwd) {
		NewReg loginNewRegLogout = new NewReg();
		ManualReg manualReg = new ManualReg();
		WaitsUtil waitsUtil = new WaitsUtil();
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {

					// loop until application primary stage is not loaded
					while (ClientApplication.getPrimaryStage() == null) {
						Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ApplicationLaunchTimeWait")));
					}

					logger.info("Application primary stage setup completed " + DateUtil.getDateTime());
					Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ApplicationLaunchTimeWait")));

					robot = new FxRobot();
					keepClientInFront(ClientApplication.getPrimaryStage());
					ExtentReportUtil.ExtentSetting();

					String manualFlag = PropertiesUtil.getKeyValue("manual");
					if (manualFlag.equalsIgnoreCase("Y")) {
						manualReg.manualRegistration(robot, operatorId, operatorPwd, supervisorId, supervisorPwd,
								ClientApplication.getPrimaryStage(), ClientApplication.getApplicationContext());
					}

					try {

						Boolean flag = false;
						flag = loginNewRegLogout.initialRegclientSet(robot, operatorId, operatorPwd, "1initallaunch",
								ClientApplication.getPrimaryStage());
						logger.info("Operator Onboarding status=" + flag);
						ExtentReportUtil.reports.flush();

					} catch (Exception e) {

						logger.error("", e);

						ExtentReportUtil.test1.log(Status.FAIL, "TESTCASE FAIL");
						ExtentReportUtil.reports.flush();
						waitsUtil.capture();
					}

					if (!manualFlag.equalsIgnoreCase("Y"))
						System.exit(0);
				}  catch (Exception e) {
					logger.error("", e);
					ExtentReportUtil.reports.flush();
					waitsUtil.capture();
				}
			}

		};
		thread.start();
		String args[] = {};
		LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);
	}


    public static void main(String[] args) {
        try {
            System.setProperty("java.net.useSystemProxies", "true");
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("derby.ui.codeset", "UTF-8");
            System.setProperty("jdbc.drivers", "org.apache.derby.jdbc.EmbeddedDriver");
            HeadlessMode.configure();
            Runtime.getRuntime().addShutdownHook(new Thread(MockMdsServer::stop, "mock-mds-stop"));
            MockMdsServer.start();

            if ("initiallaunch".equalsIgnoreCase(System.getProperty("module.run")))  {
            	PropertiesUtil.deleteFolder(System.getProperty("user.dir")+"/logs");
            	PropertiesUtil.deleteFolder(System.getProperty("user.dir")+"/db");
            	// Only the DB secret goes with the DB; the machine key pair in .mosipkeys must survive
            	// because runSetup.bat registered this machine on the server with those keys.
            	PropertiesUtil.deleteFolder(System.getProperty("user.dir")+"/.mosipkeys/db.conf");
            	invokeRegClientInitialLaunch(PropertiesUtil.getKeyValue("operatorId"), PropertiesUtil.getKeyValue("operatorPwd"),
						PropertiesUtil.getKeyValue("supervisorUserid"), PropertiesUtil.getKeyValue("supervisorUserpwd"),
						PropertiesUtil.getKeyValue("reviewerUserid"), PropertiesUtil.getKeyValue("reviewerpwd"));
				return;
            }   

            invokeRegClient(PropertiesUtil.getKeyValue("operatorId"), PropertiesUtil.getKeyValue("operatorPwd"),
                    PropertiesUtil.getKeyValue("supervisorUserid"), PropertiesUtil.getKeyValue("supervisorUserpwd"),
                    PropertiesUtil.getKeyValue("reviewerUserid"), PropertiesUtil.getKeyValue("reviewerpwd"));
        } catch (Exception e) {
            logger.error("", e);
        }

    }

    // FxRobot sends real OS mouse/keyboard events, which go to whichever window is on top; if another window
    // (IDE, notifications) comes to the front, the input never reaches the client.
    private static void keepClientInFront(Stage stage) {
        if (HeadlessMode.isEnabled()) {
            return;
        }
        Platform.runLater(() -> {
            stage.setAlwaysOnTop(true);
            stage.toFront();
        });
    }

    public static String[] readFolderJsonList(String key) {
        String contents[] = null;
        try {
            File directoryPath = new File(System.getProperty("user.dir") + key);

            if (directoryPath.exists()) {
                contents = directoryPath.list();
                // Linux directory order is not alphabetical. The onboard files are prefixed so
                // they run before registration, which is what a Windows listing already does.
                if (contents != null) {
                    java.util.Arrays.sort(contents);
                }
                System.out.println("List of files and directories in the specified directory:");
                for (int i = 0; i < contents.length; i++) {
                    System.out.println(contents[i]);
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return contents;
    }

    public static LinkedHashMap<String, String> readJsonFileText(String[] documentList) {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        String jsonTxt = null;

        try {
            for (String doc : documentList) {
                File f = new File(System.getProperty("user.dir") + PropertiesUtil.getKeyValue("datadir") + doc);

                if (f.exists()) {
                    InputStream is = new FileInputStream(f);
                    jsonTxt = IOUtils.toString(is, "UTF-8");
                    //System.out.println(jsonTxt);
                    logger.info("readJsonFileText");

                    map.put(doc, jsonTxt);

                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return map;
    }

}

/*
 * String hostName=targetUrl.substring(targetUrl.indexOf("//")+2);
 * 
 * System.setProperty("java.net.useSystemProxies", "true");
 * System.setProperty("file.encoding", "UTF-8");
 * System.setProperty("derby.ui.codeset", "UTF-8");
 * System.setProperty("mosip.upgradeserver",targetUrl+"/");
 * System.setProperty("mosip.hostname",hostName);
 * System.setProperty("jdbc.drivers","org.apache.derby.jdbc.EmbeddedDriver");
 * //if (Boolean.getBoolean("headless")) { System.setProperty("testfx.robot",
 * "glass"); System.setProperty("testfx.headless", "true");
 * System.setProperty("prism.order", "sw"); System.setProperty("prism.text",
 * "t2k"); System.setProperty("java.awt.headless", "true"); //}
 */
