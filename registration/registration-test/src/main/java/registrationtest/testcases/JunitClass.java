package registrationtest.testcases;

import java.io.IOException;

import com.sun.javafx.application.LauncherImpl;
import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.preloader.ClientPreLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Application;
import registrationtest.utility.PropertiesUtil;

public class JunitClass {

    @BeforeAll
    public void testcase() {
        String args[] = {};

        LauncherImpl.launchApplication(ClientApplication.class, ClientPreLoader.class, args);
    }

    @Test
    public void testcase1() {
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("derby.ui.codeset", "UTF-8");

        try {
            System.setProperty("mosip.upgradeserver", PropertiesUtil.getKeyValue("mosip.upgradeserver"));

            System.setProperty("mosip.hostname", PropertiesUtil.getKeyValue("mosip.hostname"));

            System.setProperty("jdbc.drivers", "org.apache.derby.jdbc.EmbeddedDriver");

          //  RegistrationMain.invokeRegClient(PropertiesUtil.getKeyValue("operatorId"),
                //    PropertiesUtil.getKeyValue("operatorPwd"), PropertiesUtil.getKeyValue("mosip.upgradeserver"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /*
     * @Test public void testcase2() {
     * System.setProperty("java.net.useSystemProxies", "true");
     * System.setProperty("file.encoding", "UTF-8");
     * System.setProperty("derby.ui.codeset", "UTF-8");
     * 
     * 
     * try { RegistrationMain.invokeRegClient(
     * PropertiesUtil.getKeyValue("operatorId"),
     * PropertiesUtil.getKeyValue("operatorPwd"),
     * PropertiesUtil.getKeyValue("supervisorUserid"),
     * PropertiesUtil.getKeyValue("supervisorUserpwd")); } catch
     * (NumberFormatException | JSONException | IOException e) { // TODO
     * Auto-generated catch block e.printStackTrace(); } }
     */
}
