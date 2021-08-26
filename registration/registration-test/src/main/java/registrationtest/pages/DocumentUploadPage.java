package registrationtest.pages;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testfx.api.FxRobot;

import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import registrationtest.pojo.schema.Schema;
import registrationtest.utility.JsonUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class DocumentUploadPage {
    private static final Logger logger = LogManager.getLogger(DocumentUploadPage.class);

    FxRobot robot;
    WaitsUtil waitsUtil;
    String captureBtn = "#captureBtn";
    String success = "#context";
    String saveBtn = "#saveBtn";
    String UploadDocImg = "#UploadDocImg";

    String docPreviewImgViewPane = "#docPreviewImgViewPane";
    // DocumentCategoryDto documentCategoryDto;

    DocumentUploadPage(FxRobot robot) {
        logger.info("In DocumentUploadPage Constructor");
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
        // waitsUtil.clickNodeAssert(robot, UploadDocImg);

        // documentCategoryDto=new DocumentCategoryDto();

    }

    public void selectDocumentScan() {
        logger.info("In selectDocumentScan");
        try {

            waitsUtil.clickNodeAssert(captureBtn);
            waitsUtil.clickNodeAssert(success);

            robot.press(KeyCode.SPACE).release(KeyCode.SPACE);

            waitsUtil.clickNodeAssert(saveBtn);

            // robot.press(KeyCode.SPACE).release(KeyCode.SPACE);
        } catch (Exception e) {
            logger.error("", e);
        }

    }

    public void user_selects_combo_itemdoc(String comboBoxId, String val) {
        try {
            Platform.runLater(new Runnable() {
                public void run() {

                    ComboBox comboBox = waitsUtil.lookupById(comboBoxId);

                    // comboBox.getSelectionModel().select(dto);
                    Optional<DocumentCategoryDto> op = comboBox.getItems().stream()
                            .filter(i -> ((DocumentCategoryDto) i).getName().equalsIgnoreCase(val)).findFirst();
                    if (op.isEmpty())
                        comboBox.getSelectionModel().selectFirst();
                    else
                        comboBox.getSelectionModel().select(op.get());

                    try {
                        Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ComboItemTimeWait")));
                    } catch (Exception e) {

                        logger.error("", e);
                    }
                }
            });
        } catch (Exception e) {

            logger.error("", e);
        }

    }

    public void documentDropDownScan(Schema schema, String id, String JsonIdentity, String key) {
        try {
            if (schema.getType().contains("documentType")) {
                LinkedHashMap<String, String> mapDropValue = JsonUtil.JsonObjSimpleParsing(JsonIdentity, key);
                Set<String> dropkeys = mapDropValue.keySet();
                for (String ky : dropkeys) {
                    user_selects_combo_itemdoc(id, mapDropValue.get(ky));
                    String scanBtn = id + "button";

                    Button scanButton = waitsUtil.lookupByIdButton(scanBtn, robot);
                    robot.moveTo(scanButton);
                    robot.clickOn(scanButton);
                    selectDocumentScan();
                    break;
                }
            }
        } catch (Exception e) {

            logger.error("", e);
        }
    }

    public List<String> documentUploadAttributeList(String identity) {
        List<String> documentUploadAttList = new LinkedList<String>();
        String documentUploadAttributes = null;
        try {
            documentUploadAttributes = PropertiesUtil.getKeyValue("documentUploadAttributes");

            documentUploadAttList = JsonUtil.JsonObjArrayListParsing(identity, documentUploadAttributes);
        } catch (Exception e) {
            logger.error("", e);
        }

        return documentUploadAttList;
    }

}
