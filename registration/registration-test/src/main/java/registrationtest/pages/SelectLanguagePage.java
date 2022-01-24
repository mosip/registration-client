package registrationtest.pages;

import java.io.IOException;


import org.testfx.api.FxRobot;

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

        for (String code : langCodeList) {
            waitsUtil.clickNodeAssert("#" + code);
        }
    }
}
