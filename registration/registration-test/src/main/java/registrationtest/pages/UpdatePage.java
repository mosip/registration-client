package registrationtest.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import javafx.scene.control.TextField;

import registrationtest.utility.JsonUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class UpdatePage {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(UpdatePage.class);
    String alertImage = "#alertImage";
    String exit = "#exit";
    String success = "#context";
    String cancel = "#cancel";
    String confirm = "#confirm";
    String uinId = "#uinId";

    WaitsUtil waitsUtil;
    FxRobot robot;

    /**
     * Alerts Constuctor
     * 
     * @param robot
     */
    public UpdatePage(FxRobot robot) {
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
        logger.info("In UpdatePage Constructor");
    }

    /**
     * Enter uinId -
     */
    public void enterUinId(String uinNumber) {
        TextField useruin = waitsUtil.waitForNode(uinId, TextField.class);
        useruin.setText(uinNumber);
        logger.info("enterUinId");
    }

    public void selectRadioButton(FxRobot robot, String JsonIdentity) {
        List<String> updateUINAttributes = null;
        try {
            updateUINAttributes = JsonUtil.JsonObjArrayListParsing(JsonIdentity, "updateUINAttributes");
        } catch (Exception e) {
            logger.error("", e);
        }
        if (updateUINAttributes == null || updateUINAttributes.isEmpty()) {
            return;
        }
        Set<String> clicked = new HashSet<>();
        for (String attr : updateUINAttributes) {
            for (String checkboxId : presentCheckboxIds(attr)) {
                if (clicked.add(checkboxId)) {
                    waitsUtil.clickNodeAssert("#" + checkboxId);
                }
            }
        }
    }

    /**
     * Name groups differ per environment: some UI specs have one "FullName" group, others "FirstName" and
     * "LastName". Uses whichever variant is on screen, falling back to the configured id.
     */
    private List<String> presentCheckboxIds(String attribute) {
        List<String> configured = resolveCheckboxIds(attribute);
        List<String> alternative = nameGroupAlternative(attribute);
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            if (allPresent(configured)) {
                return configured;
            }
            if (!alternative.isEmpty() && allPresent(alternative)) {
                logger.info("Update attribute {} shown as {}", attribute, alternative);
                return alternative;
            }
            WaitForAsyncUtils.sleep(200, TimeUnit.MILLISECONDS);
        }
        return configured;
    }

    private boolean allPresent(List<String> ids) {
        for (String id : ids) {
            if (robot.lookup("#" + id).tryQuery().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private List<String> nameGroupAlternative(String attribute) {
        if ("FullName".equalsIgnoreCase(attribute)) {
            return Arrays.asList("FirstName", "LastName");
        }
        if ("FirstName".equalsIgnoreCase(attribute) || "LastName".equalsIgnoreCase(attribute)) {
            return Arrays.asList("FullName");
        }
        return new ArrayList<>();
    }

    private List<String> resolveCheckboxIds(String attribute) {
        try {
            String mapped = PropertiesUtil.getKeyValue(attribute);
            if (mapped != null && !mapped.isBlank() && !mapped.equals(attribute)) {
                return Arrays.asList(mapped);
            }
        } catch (Exception e) {
            logger.debug("No config mapping for update attribute {}", attribute);
        }
        return new ArrayList<>(Arrays.asList(attribute));
    }

}
