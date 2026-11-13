package registrationtest.pages;

import java.io.IOException;

import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class SelectLanguagePage {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(SelectLanguagePage.class);

    FxRobot robot;
    String code;
    String[] langCodeList;
    WaitsUtil waitsUtil;

    public SelectLanguagePage(FxRobot robot) {
        logger.info("LoginPage Constructor");

        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
    }

    public void selectLang() {
        try {
            code = PropertiesUtil.getKeyValue("langcode");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        langCodeList = code.split("@@");

        // Index 0 is the application language and is already selected. Clicking a
        // selected box again clears it, which keeps Submit disabled.
        for (int index = 1; index < langCodeList.length; index++) {
            selectAdditionalLanguage("#" + langCodeList[index].trim());
        }
    }

    private void selectAdditionalLanguage(String id) {
        for (int attempt = 0; attempt < 3; attempt++) {
            Node node = waitsUtil.waitForNode(id);
            if (node instanceof CheckBox && ((CheckBox) node).isSelected()) {
                return;
            }
            waitsUtil.clickNodeAssert(id);
            WaitForAsyncUtils.waitForFxEvents();
            node = waitsUtil.waitForNode(id);
            if (node instanceof CheckBox && ((CheckBox) node).isSelected()) {
                return;
            }
        }
        throw new IllegalStateException("Language " + id + " stayed unselected");
    }
}
