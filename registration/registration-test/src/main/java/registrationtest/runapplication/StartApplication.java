package registrationtest.runapplication;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.testfx.api.FxRobotContext;

import io.mosip.registration.controller.Initialization;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import registrationtest.utility.PropertiesUtil;

/***
 * 
 * @author Neeharika.Garg
 * 
 *         First Launch invokes start method and this will launch Registration
 *         Client
 * 
 */

public class StartApplication extends Application {
    private static final Logger logger = LogManager.getLogger(StartApplication.class);

    static Stage primaryStage;
    static ApplicationContext applicationContext;
    FxRobotContext context;
    Scene scene;

    public void start(Stage primaryStage) {

        try {
            io.mosip.registration.context.ApplicationContext.getInstance();
            System.out.println(System.getProperty("mosip.hostname"));

            // applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
            applicationContext = Initialization.createApplicationContext();
            logger.info("Automaiton Script - ApplicationContext has taken");

            Initialization initialization = new Initialization();
            Initialization.setApplicationContext(applicationContext);
            StartApplication.primaryStage = primaryStage;
            initialization.start(primaryStage);
            this.primaryStage = primaryStage;

        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public static void main(String[] args) {
        try {

            System.setProperty("java.net.useSystemProxies", "true");
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("derby.ui.codeset", "UTF-8");
            System.setProperty("client.upgrade.server.url", PropertiesUtil.getKeyValue("mosip.upgradeserver"));
            System.setProperty("mosip.hostname", PropertiesUtil.getKeyValue("mosip.hostname"));

            launch(args);
        } catch (Exception e) {
            logger.error("", e);
        }
    }
}
