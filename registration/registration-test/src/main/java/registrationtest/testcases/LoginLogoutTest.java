package registrationtest.testcases;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.javafx.application.LauncherImpl;
import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.preloader.ClientPreLoader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.api.FxRobot;
import org.testfx.api.FxRobotContext;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import registrationtest.pages.HomePage;
import registrationtest.pages.LoginPage;
import registrationtest.pojo.schema.Root;
import registrationtest.pojo.schema.Schema;
import registrationtest.utility.PropertiesUtil;

/***
 * 
 * @author Neeharika.Garg Login and Logout RegClient Steps Run this using Junit
 *         First start method invokes and this will launch Registration Client
 *         and through dependency injection
 * 
 *         Fxrobot will take control of primary stage and perform keyboard and
 *         mouse driven activities.
 *
 */

public class LoginLogoutTest {

    private static final Logger logger = LoggerFactory.getLogger(LoginLogoutTest.class);

    static FxRobot robot;
    Schema schema;
    Root root;
    Scene scene;
    Node node;
    Boolean flagContinueBtnFileUpload = true;
    Boolean flagContinueBtnBioUpload = true;
    private static Stage applicationPrimaryStage;

    LoginPage loginPage;
    HomePage homePage;
    PropertiesUtil propertiesUtil;
    FxRobotContext context;
    Boolean result = false;
    @FXML
    private WebView webView;

    @ParameterizedTest
    @CsvFileSource(resources = "/login.csv", numLinesToSkip = 1)
    void loginlogout(String userid, String password) throws Exception {

        // Set FxRobotContext
        // robot=new FxRobot();

        loginPage = new LoginPage(robot);

        // Load Login screen

        loginPage.loadLoginScene(applicationPrimaryStage);

        // Enter userid and password
        loginPage.setUserId(userid);

        homePage = loginPage.setPassword(password);

        // Logout Regclient
        loginPage.logout();

        assertTrue(result, "TestCase Failed");
        // return result;

    }

    public static void main(String[] args) throws InterruptedException { // 1

        robot = new FxRobot();
        LoginLogoutTest lg = new LoginLogoutTest();

        Thread thread4 = new Thread() { // 3 parallel running
            @Override
            public void run() {
                try {
                    System.out.println("thread4 inside calling testcase");
                    Thread.sleep(120000); // Needed
                    robot = new FxRobot();
                    LoginLogoutTest lg = new LoginLogoutTest();

                    lg.loginlogout("", "");
                } catch (InterruptedException e) {
                    logger.error("", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                	logger.error("", e);
				}
            }

        };

        thread4.start();
        LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);// 2 infine loop never yeild

    }
}
