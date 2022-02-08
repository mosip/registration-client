package registrationtest.pages;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import io.mosip.registration.dto.mastersync.GenericDto;

import org.testfx.api.FxRobot;

import javafx.application.Platform;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import registrationtest.controls.Buttons;
import registrationtest.pojo.schema.Root;
import registrationtest.pojo.schema.Schema;
import registrationtest.pojo.schema.Screens;
import registrationtest.utility.ComboBoxUtil;
import registrationtest.utility.DateUtil;
import registrationtest.utility.JsonUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class DemographicPage {

    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(DemographicPage.class);
    FxRobot robot;
    Stage applicationPrimaryStage;
    Scene scene;
    Screens screens;
    Node node;
    TextField demoTextField2;
    Schema schema;
    // Root rootSchema;
    Boolean flagContinueBtnDemograph = true;

    Boolean flagContinueBtnDocumentUpload = true;
    DocumentUploadPage documentUploadPage;
    BiometricUploadPage biometricUploadPage;
    BioCorrectionPage bioCorrectionPage;
    WaitsUtil waitsUtil;
    String DemoDetailsImg = "#DemoDetailsImg";
    WebViewDocument webViewDocument;
    Buttons buttons;
    String schemaJsonFilePath;
    double schemaJsonFileVersion;
    String nameTab = null;
    LinkedHashMap<String, String> mapValue;
    LinkedHashMap<String, String> mapDropValue;
    String value, text;
    String jsonFromSchema;
    // ProcessSchema processSchema;
    Root processSchema;
    ComboBoxUtil comboBoxUtil = new ComboBoxUtil();

    List<Screens> unOrderedScreensList, orderedScreensList;
    List<Schema> fieldsList;
    Boolean flagproofOf = true;
    Boolean flagBiometrics = true;
    LinkedHashMap<String, Integer> allignmentgroupMap;

    Boolean flag = false;

    public DemographicPage(FxRobot robot) {
        logger.info(" DemographicPage Constructor  ");

        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
        // waitsUtil.clickNodeAssert( DemoDetailsImg);
        documentUploadPage = new DocumentUploadPage(robot);
        biometricUploadPage = new BiometricUploadPage(robot);
        bioCorrectionPage = new BioCorrectionPage(robot);
        buttons = new Buttons(robot);
        webViewDocument = new WebViewDocument(robot);
        allignmentgroupMap = new LinkedHashMap<String, Integer>();
    }

    public void setTextFields1(String id, String idSchema) {
        flag = false;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                logger.info(" setTextFields in " + id + " " + idSchema);

                try {

                    TextField demoTextField = waitsUtil.lookupById(id);
                    assertNotNull(demoTextField, id + " not present");

                    if (demoTextField.isEditable() && demoTextField.getParent().getParent().isVisible()) {
                        logger.info(" textfield is visible ans setting the text in " + id + " " + idSchema);
                        demoTextField.setText(idSchema);

                    }

                }

                catch (Exception e) {
                    logger.error("", e);
                }

            }
        });
    }

    public void setTextFields(String id1, String id, String value) {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                logger.info(" setTextFields in " + id + " " + value);

                try {
                    TextField demoTextFieldvar = waitsUtil.lookupById(id);
                    assertNotNull(demoTextFieldvar, id + " not present");

                   
                    if (demoTextFieldvar.isEditable() && demoTextFieldvar.getParent().getParent().isVisible()) {
                        logger.info("id="+id + "value"+value);
                        String makeUniqueEntry = PropertiesUtil.getKeyValue("makeUniqueEntry");

                        Boolean appendDateTime = makeUniqueEntry == null ? false
                                : makeUniqueEntry.contains(id1.replace("#", ""));

                        // appendDateTime =
                        // Arrays.asList(makeUniqueEntry).stream().anyMatch(uniqueid->uniqueid.equals(id));

                        logger.info(" textfield is visible and setting the text in " + id + "= " + value);
                        demoTextFieldvar.setText(appendDateTime ? value + DateUtil.getDateTime() : value);

//                        try {
//                            Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("SetTextTimeWait")));
//                        } catch (Exception e) {
//
//                            logger.error("", e);
//                        }
                    }
                } catch (Exception e) {
                    logger.error("", e);
                }

            }
        });
    }

    public void setTextFieldswithNothread(String id1, String id, String value) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                logger.info(" setTextFields in " + id + " " + value);

                try {
                    TextField demoTextFieldvar = waitsUtil.lookupById(id);
                    assertNotNull(demoTextFieldvar, id + " not present");

                    if (demoTextFieldvar.isEditable() && demoTextFieldvar.getParent().getParent().isVisible()) {
                        String makeUniqueEntry = PropertiesUtil.getKeyValue("makeUniqueEntry");

                        Boolean appendDateTime = makeUniqueEntry == null ? false
                                : makeUniqueEntry.contains(id1.replace("#", ""));

                        // appendDateTime =
                        // Arrays.asList(makeUniqueEntry).stream().anyMatch(uniqueid->uniqueid.equals(id));

                        logger.info(" textfield is visible and setting the text in " + id + "= " + value);
                        demoTextFieldvar.setText(appendDateTime ? value + DateUtil.getDateTime() : value);

                        Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("SetTextTimeWait")));
                    }
                } catch (InterruptedException e) {
                    logger.error("", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        });
    }

    /**
     * {@summary} For New Registration adult
     * 
     * @param schemaVersion
     * @param JsonIdentity
     * @param documentUpload
     * @return
     */
    public WebViewDocument screensFlow(String JsonIdentity, String flow, String ageGroup) {

        /**
         * convert jsonFromSchema intoJava
         */

        try {
            jsonFromSchema = getProcessSchemaJsonTxt(flow);

            processSchema = JsonUtil.convertJsonintoJava(jsonFromSchema, Root.class);

           // logger.info("Automaiton Script - Printing Input file Json" + JsonIdentity);
        } catch (Exception e) {
            logger.error("", e);
        }

        /**
         * sortSchemaScreen Based on Order
         * 
         * Set Array index Based on the Order of the screens
         * 
         */
        unOrderedScreensList = processSchema.getScreens();

        if (!unOrderedScreensList.isEmpty()) {
            orderedScreensList = sortSchemaScreen(unOrderedScreensList);
        }

        // else
        // {
        // orderedScreensList=singleSchemaScreen(rootSchema);
        // }
        for (int k = 0; k < orderedScreensList.size(); k++) {
            screens = orderedScreensList.get(k);

            logger.info("Order" + screens.getOrder() + " Fields" + screens.getFields());
            fieldsList = screens.getFields();

            nameTab = screens.getName();
            try {
                Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("SetTextTimeWait")));
                logger.info("SetTextTimeWait Done");
            } catch (InterruptedException e) {
                logger.error("", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("", e);
            }
            waitsUtil.clickNodeAssert("#" + nameTab + "_tab");
            // waitsUtil.clickNodeAssert("#"+nameTab);
            robot.moveTo("#" + nameTab);

            for (Schema schema : fieldsList) {
                try {
                    // for (int i = 0; i < rootSchema.getSchema().size(); i++)
                    // {
                    // schema = rootSchema.getSchema().get(i);
                    // if(!field.equals(schema.getId())) continue;
                    try {
                        if (schema.getGroup().equals(PropertiesUtil.getKeyValue("Documents"))
                                && schema.isInputRequired()) {
                            if (flagproofOf) {
                                scrollVerticalDirectioncount(
                                        Integer.parseInt(PropertiesUtil.getKeyValue("proofscroll")));

                                flagproofOf = false;
                            }
                        } else if (schema.getGroup().equals(PropertiesUtil.getKeyValue("Biometrics"))) {
                            if (flagBiometrics) {
                            }
                        } else if (schema.getGroup().equals(PropertiesUtil.getKeyValue("consent"))
                                && schema.isInputRequired()) {
                            scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("consentscroll")));
                        }
                        // else if(schema.isInputRequired()&&schema.isRequired())
                        // {
                        // scrollVerticalDirection(i,schema);
                        // }
                        // else if(!schema.isRequired()&&(scenario.contains("child")))
                        // {
                        // scrollVerticalDirection(i,schema);
                        // }
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                    logger.info("Automaiton Script - id=" + "=" + schema.getId() + "\tSchemaControlType="
                            + schema.getControlType());
                    if (flow.equalsIgnoreCase("Update")) {
                        contolTypeUpdate(schema, JsonIdentity, flow, ageGroup);
                    } else {
                        contolType(schema, JsonIdentity, flow, ageGroup);
                    }
                }

                catch (Exception e) {
                    logger.error("", e);
                }
            }
        }
        return webViewDocument;

    }

    private void scrollVerticalDirection2(int i, Schema schema) {

        try {
            robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")),
                    VerticalDirection.DOWN);

        } catch (NumberFormatException e) {
            logger.error("", e);
        } catch (IOException e) {
            logger.error("", e);
        }

    }

    private void scrollVerticalDirectioncount(int scrollcount) {

        try {
            robot.scroll(scrollcount, VerticalDirection.DOWN);

        } catch (Exception e) {
            logger.error("", e);
        }

    }

    private void scrolltillnode(int scrollcount) {

        try {
            robot.scroll(scrollcount, VerticalDirection.DOWN);

        } catch (Exception e) {
            logger.error("", e);
        }

    }

    private void scrollVerticalDirection(int i, Schema schema) {
        String[] lang = null;
        try {
            lang = PropertiesUtil.getKeyValue("langcode").split("@@");
        } catch (IOException e) {
            logger.error("", e);
        }

        if (schema.getAlignmentGroup() == null) {
            try {
                if (schema.getControlType().equals("textbox")) {
                    for (String s : lang)
                        robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")),
                                VerticalDirection.DOWN);
                } else
                    robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")),
                            VerticalDirection.DOWN);

            } catch (Exception e) {
                logger.error("", e);
            }
        } else {

            try {
                if (!allignmentgroupMap.containsKey(schema.getAlignmentGroup().toString())) {
                    allignmentgroupMap.put(schema.getAlignmentGroup().toString(), 1);
                    if (schema.getControlType().equals("textbox")) {
                        for (String s : lang) {
                            robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")),
                                    VerticalDirection.DOWN);
                        }
                    } else {
                        robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")),
                                VerticalDirection.DOWN);

                    }
                } else {
                    allignmentgroupMap.put(schema.getAlignmentGroup().toString(),
                            allignmentgroupMap.get(schema.getAlignmentGroup().toString()) + 1);
                    int key1 = allignmentgroupMap.get(schema.getAlignmentGroup().toString());
                    if (schema.getControlType().equals("textbox") && (key1 % 2) == 1 && key1 > 2) {
                        for (String s : lang) {
                            robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")),
                                    VerticalDirection.DOWN);
                        }
                    } else if (schema.getControlType().equals("dropdown") && (key1 % 4) == 1 && key1 > 4) {
                        for (String s : lang) {
                            robot.scroll(Integer.parseInt(PropertiesUtil.getKeyValue("scrollVerticalDirection2")),
                                    VerticalDirection.DOWN);
                        }
                    }

                }

            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    public String getSchemaJsonTxt() {
        String jsonFromSchema = null;
        try {
            // schemaJsonFileVersion=JsonUtil.JsonObjDoubleParsing(JsonIdentity,"IDSchemaVersion");

            schemaJsonFileVersion = Double.parseDouble(PropertiesUtil.getKeyValue("IDSchemaVersion"));
            schemaJsonFilePath = System.getProperty("user.dir") + "/SCHEMA_" + schemaJsonFileVersion + ".json";
            jsonFromSchema = Files.readString(Paths.get(schemaJsonFilePath));
            logger.info("Automaiton Script - Printing jsonFromSchema" + jsonFromSchema);
        } catch (Exception e) {
            logger.error("", e);
        }
        return jsonFromSchema;
    }

    public String getProcessSchemaJsonTxt(String flow) {
        String jsonFromSchema = null;
        try {
            // schemaJsonFileVersion=JsonUtil.JsonObjDoubleParsing(JsonIdentity,"IDSchemaVersion");

           // schemaJsonFileVersion = Double.parseDouble(PropertiesUtil.getKeyValue("IDSchemaVersion"));
            schemaJsonFilePath = System.getProperty("user.dir") + File.separator + flow.toLowerCase() + "Process.json";
            jsonFromSchema = Files.readString(Paths.get(schemaJsonFilePath));
            logger.info("Automaiton Script - Printing jsonFromSchema" + jsonFromSchema);
        } catch (Exception e) {
            logger.error("", e);
        }
        return jsonFromSchema;
    }

    /**
     * sortSchemaScreen Based on Order
     * 
     * @param unsortedList
     * @return
     */

    public List<Screens> sortSchemaScreen(List<Screens> unsortedList) {
        Map<Integer, Integer> m = new HashMap<Integer, Integer>();
        List<Screens> sortList = new LinkedList<Screens>();
        for (int kk = 0; kk < unsortedList.size(); kk++) {
            m.put(unsortedList.get(kk).getOrder(), kk);

        }
        System.out.println(m);

        TreeMap<Integer, Integer> tm = new TreeMap<Integer, Integer>(m);
        Iterator itr = tm.keySet().iterator();
        while (itr.hasNext()) {
            int key = (int) itr.next();
            System.out.println("Order:  " + key + "     Index:   " + m.get(key));
            sortList.add(processSchema.getScreens().get(m.get(key)));
        }

        System.out.println(tm);
        System.out.println(sortList);

        return sortList;

    }

    // public List<Screens> singleSchemaScreen(Root rootSchema)
    // {Screens scn = new Screens();
    // //Label lab=new Label();
    // List<String> fieldList=new LinkedList<String>();
    // List<Screens> screenList=new LinkedList<Screens>();
    // HashMap<String, String> label = new HashMap<>();
    // String[] listLang=null;;
    // try {
    // listLang = PropertiesUtil.getKeyValue("langcode").split("@@");
    // } catch (IOException e) {
    // logger.error("",e);
    // }
    //
    // try
    // {
    // for (int i = 0; i < rootSchema.getSchema().size(); i++) {
    // schema = rootSchema.getSchema().get(i);
    // fieldList.add(schema.getId());
    // }
    //
    // scn.setOrder(0);
    // scn.setName("Resident_Information");
    //
    // label.put(listLang[0],"Resident_Information");
    // scn.setLabel(label);
    // scn.setCaption(label);
    //
    // scn.setFields(fieldList);
    // scn.setLayoutTemplate(null);
    // scn.setPreRegFetchRequired(true);
    // scn.setActive(false);
    // System.out.println(scn.toString() );
    // screenList.add(scn);
    //
    // System.out.println(screenList.toString());
    // }
    // catch(Exception e)
    // {
    // logger.error("",e);
    //
    // }
    // return screenList;
    // }

    public List<String> getupdateUINAttributes(String JsonIdentity) {
        List<String> updateUINAttributes = null;
        try {
            updateUINAttributes = JsonUtil.JsonObjArrayListParsing(JsonIdentity, "updateUINAttributes");
        } catch (Exception e) {
            logger.error("", e);
        }
        return updateUINAttributes;
    }

    public void getTextboxKeyValueUpdate(Schema schema, String id, String JsonIdentity, String key, Boolean trans,
            String scenario, String ageGroup) {
        for (String uinlist : getupdateUINAttributes(JsonIdentity)) {
            if (schema.getGroup().equals(uinlist)) {
                if (ageGroup.equalsIgnoreCase("INFANT") || ageGroup.equalsIgnoreCase("MINOR"))
                    getTextboxKeyValueChild1(schema, id, JsonIdentity, key, schema.isTransliterate(), scenario);
                else
                    getTextboxKeyValue1(schema, id, JsonIdentity, key, schema.isTransliterate(), scenario);

            }
        }
    }

    public void getTextboxKeyValueChild1(Schema schema, String id, String JsonIdentity, String key, Boolean trans,
            String scenario) {
        logger.info("Schema Control Type textbox");
        mapValue = null;
        if (schema.isInputRequired()) {
            if (schema.getType().contains("simpleType")) {
                try {
                    mapValue = JsonUtil.JsonObjSimpleParsing(JsonIdentity, key);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    logger.error("", e);
                }
                if (mapValue != null) {
                	Set<String> keys = mapValue.keySet();
                    for (String ky : keys) {
                        String idk = id + ky;
                        String v = mapValue.get(ky);
                        setTextFields(id, idk, v);
                        if (trans == true)
                            return;
                    }
                }
            } else {
                value = null;
                try {
                    value = JsonUtil.JsonObjParsing(JsonIdentity, key);
                } catch (Exception e) {
                    logger.error("", e);
                }
                try {
                    String[] listLang = PropertiesUtil.getKeyValue("langcode").split("@@");

                    setTextFields(id, id + listLang[0], value);
                } catch (IOException e) {

                    logger.error("", e);
                }
            }
        }

    }

    public void getTextboxKeyValueChild(Schema schema, String id, String JsonIdentity, String key, Boolean trans,
            String scenario) {
        logger.info("Schema Control Type textbox");
        mapValue = null;
        if (schema.isInputRequired()) {
            if (schema.getType().contains("simpleType")) {
                try {
                    mapValue = JsonUtil.JsonObjSimpleParsing(JsonIdentity, key);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    logger.error("", e);
                }
                if (mapValue != null) {
                	Set<String> keys = mapValue.keySet();
                    for (String ky : keys) {
                        String idk = id + ky;
                        String v = mapValue.get(ky);
                        setTextFields(id, idk, v);
                        if (trans == true)
                            return;
                    }
                }
            } else {
                value = null;
                try {
                    value = JsonUtil.JsonObjParsing(JsonIdentity, key);
                } catch (Exception e) {
                    logger.error("", e);
                }
                try {
                    String[] listLang = PropertiesUtil.getKeyValue("langcode").split("@@");

                    setTextFields(id, id + listLang[0], value);
                } catch (IOException e) {

                    logger.error("", e);
                }
            }
        }

    }

    public void biometricsAuth(Schema schema, String scenario, String id, String identity, String ageGroup) {
        try {
            if (scenario.equalsIgnoreCase("Update") && schema.subType.equalsIgnoreCase("applicant-auth")) {
                Node node = waitsUtil.lookupById(id);
                if (node.isVisible()) {
                    scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("authscroll")));

                    System.out.println("");
                    Thread.sleep(400);
                    biometricUploadPage.newRegbioUpload(schema.getSubType(),
                            biometricUploadPage.bioAuthAttributeList(identity), id, identity, ageGroup);
                }
            }

        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public List<String> fetchbioAttr(Schema schema, String agegroup, String process) {
        List<String> bioattributes = null;
        try {
            for (int index = 0; index < schema.getConditionalBioAttributes().size(); index++) {
                if (schema.getConditionalBioAttributes().get(index).getAgeGroup().equalsIgnoreCase(agegroup) && (schema
                        .getConditionalBioAttributes().get(index).getProcess().equalsIgnoreCase("ALL")
                        || schema.getConditionalBioAttributes().get(index).getProcess().equalsIgnoreCase(process)))
                    bioattributes = schema.getConditionalBioAttributes().get(index).getBioAttributes();
            }
        } catch (Exception e) {
            logger.error("", e);
        }

        if (bioattributes == null)
            bioattributes = schema.getBioAttributes();

        return bioattributes;
    }

    public void biometrics(Schema schema, String scenario, String id, String identity) {
        try {
            if (scenario.equalsIgnoreCase("bioCorrection")) {
                String additionalInfoRequestId = JsonUtil.JsonObjParsing(identity, "additionalInfoRequestId");
                bioCorrectionPage.setAdditionalInfoRequestId(additionalInfoRequestId);
            }
        } catch (Exception e) {
            logger.error("", e);
        }

        try {

            // RegistrationDTO registrationDTO = (RegistrationDTO)
            // SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);

            // int age=registrationDTO.getAge();
            String ageGroup = JsonUtil.JsonObjParsing(identity, "ageGroup");
            String process = JsonUtil.JsonObjParsing(identity, "process");
            List<String> bioattributes = null;

            if (schema.isInputRequired() && schema.subType.equalsIgnoreCase("applicant")) {
                if (ageGroup.equalsIgnoreCase("INFANT")) {
                    scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("childbioscroll")));
                    // bioattributes=fetchbioAttr(schema,ageGroup,process);
                    biometricUploadPage.newRegbioUpload(schema.getSubType(),
                            biometricUploadPage.bioAttributeList(identity), id, identity, ageGroup);
                } else {
                    if (ageGroup.equalsIgnoreCase("MINOR")) {
                        scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("childbioscroll")));
                        // bioattributes=fetchbioAttr(schema,ageGroup,process);
                        biometricUploadPage.newRegbioUpload(schema.getSubType(),
                                biometricUploadPage.bioAttributeList(identity), id, identity, ageGroup);
                    } else if (ageGroup.equalsIgnoreCase("ADULT")) {
                        scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));
                        Thread.sleep(400);
                        // bioattributes=fetchbioAttr(schema,ageGroup,process);

                        biometricUploadPage.newRegbioUpload(schema.getSubType(),
                                biometricUploadPage.bioAttributeList(identity), id, identity, ageGroup);
                    }
                }
            } else if (schema.subType.equalsIgnoreCase("introducer")
                    && (ageGroup.equals("INFANT") || ageGroup.equals("MINOR"))) {

                if (ageGroup.equalsIgnoreCase("INFANT")) {
                    scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));
                    scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));
                } else
                    scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));

                Thread.sleep(400);
                biometricUploadPage.newRegbioUpload(schema.getSubType(),
                        biometricUploadPage.bioAuthAttributeList(identity), id, identity, ageGroup);

            } else if (schema.subType.equalsIgnoreCase("applicant-auth") && (!ageGroup.equals("INFANT"))) {

                biometricsAuth(schema, scenario, id, identity, ageGroup);

            }

        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void authBiometrics(Schema schema, String scenario, String id, String identity) {
        try {
            // RegistrationDTO registrationDTO = (RegistrationDTO)
            // SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);

            // int age=registrationDTO.getAge();
            String ageGroup = JsonUtil.JsonObjParsing(identity, "ageGroup");
            String process = JsonUtil.JsonObjParsing(identity, "process");
            List<String> bioattributes = null;

            if (schema.subType.equalsIgnoreCase("applicant-auth") && (!ageGroup.equals("INFANT"))) {

                biometricsAuth(schema, scenario, id, identity, ageGroup);

            }

        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void introducerBiometrics(Schema schema, String scenario, String id, String identity) {
        try {
            // RegistrationDTO registrationDTO = (RegistrationDTO)
            // SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);

            // int age=registrationDTO.getAge();
            String ageGroup = JsonUtil.JsonObjParsing(identity, "ageGroup");
            String process = JsonUtil.JsonObjParsing(identity, "process");
            List<String> bioattributes = null;

            if (schema.subType.equalsIgnoreCase("introducer")
                    && (ageGroup.equals("INFANT") || ageGroup.equals("MINOR"))) {
                Node node = waitsUtil.lookupById(id);
                if (node.isVisible()) {
                    if (ageGroup.equalsIgnoreCase("INFANT")) {
                        scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));
                        scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));
                    } else
                        scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));

                    Thread.sleep(400);

                    biometricUploadPage.newRegbioUpload(schema.getSubType(),
                            biometricUploadPage.bioAuthAttributeList(identity), id, identity, ageGroup);
                }
            }

        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void applicantBiometrics(Schema schema, String scenario, String id, String identity) {
        try {
            // RegistrationDTO registrationDTO = (RegistrationDTO)
            // SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);

            // int age=registrationDTO.getAge();
            String ageGroup = JsonUtil.JsonObjParsing(identity, "ageGroup");
            String process = JsonUtil.JsonObjParsing(identity, "process");
            List<String> bioattributes = null;

            if (schema.isInputRequired() && schema.subType.equalsIgnoreCase("applicant")) {
                if (ageGroup.equalsIgnoreCase("INFANT")) {
                    scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("childbioscroll")));
                    // bioattributes=fetchbioAttr(schema,ageGroup,process);
                    biometricUploadPage.newRegbioUpload(schema.getId(),
                            biometricUploadPage.bioAttributeList(identity), id, identity, ageGroup);
                } else {
                    if (ageGroup.equalsIgnoreCase("MINOR")) {
                        scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("childbioscroll")));
                        // bioattributes=fetchbioAttr(schema,ageGroup,process);
                        biometricUploadPage.newRegbioUpload(schema.getSubType(),
                                biometricUploadPage.bioAttributeList(identity), id, identity, ageGroup);
                    } else if (ageGroup.equalsIgnoreCase("ADULT")) {
                        scrollVerticalDirectioncount(Integer.parseInt(PropertiesUtil.getKeyValue("bioscroll")));
                        Thread.sleep(400);
                        // bioattributes=fetchbioAttr(schema,ageGroup,process);

                        biometricUploadPage.newRegbioUpload(schema.getSubType(),
                                biometricUploadPage.bioAttributeList(identity), id, identity, ageGroup);
                    }
                }
            }

        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void fileupload(Schema schema, String JsonIdentity, String key, String id, String scenario,
            String ageGroup) {
        try {
            List<String> documentUploadAttList = documentUploadPage.documentUploadAttributeList(JsonIdentity);
            for (String doclist : documentUploadAttList) {
                if (schema.getSubType().equals(doclist)) {
                    documentUploadPage.documentDropDownScan(schema, id, JsonIdentity, key);
                }
            }

        } catch (Exception e) {
            logger.error("", e);

        }

    }

    public void dropdownUpdate(Schema schema, String id, String JsonIdentity, String key) {
        for (String uinlist : getupdateUINAttributes(JsonIdentity)) {
            if (schema.getGroup().equals(uinlist)) {
                dropdown(schema, id, JsonIdentity, key);
            }
        }
    }

    public void checkboxUpdate(Schema schema, String id, String JsonIdentity, String key) {
        for (String uinlist : getupdateUINAttributes(JsonIdentity)) {
            if (schema.getGroup().equals(uinlist)) {

                checkSelection(id, JsonIdentity, key);
            }
        }
    }

    public void buttonUpdateSelection(Schema schema, String id, String JsonIdentity, String key) {
        for (String uinlist : getupdateUINAttributes(JsonIdentity)) {
            if (schema.getGroup().equals(uinlist)) {

                buttonSelection(schema, id, JsonIdentity, key);
            }
        }
    }

    // public void dropdown(String id,String JsonIdentity,String key) {
    // GenericDto dto=new GenericDto();
    // try {
    // mapDropValue=null;
    // if(schema.getType().contains("simpleType"))
    // {
    // mapDropValue=JsonUtil.JsonObjSimpleParsing(JsonIdentity,key);
    // Set<String> dropkeys = mapDropValue.keySet();
    // for(String ky:dropkeys)
    // { //dto.setCode(ky);
    // //dto.setLangCode(ky);
    // dto.setName(mapDropValue.get(ky));
    // user_selects_combo_item(id,dto);
    // //user_selects_combo_item1(id,mapDropValue.get(ky));
    // break;
    // }
    // }
    // else
    // {
    // String val=JsonUtil.JsonObjParsing(JsonIdentity, key);
    // dto.setName(val);
    // user_selects_combo_item(id,dto);
    // //user_selects_combo_item1(id,val);
    // }}catch(Exception e)
    // {
    // logger.error("",e);
    // }
    // }
    //

    public void user_selects_combo_item(String comboBoxId, String val) {
        try {
            Platform.runLater(new Runnable() {
                public void run() {

                    ComboBox comboBox = waitsUtil.lookupById(comboBoxId);

                    // comboBox.getSelectionModel().select(dto);
                    Optional<GenericDto> op = comboBox.getItems().stream()
                            .filter(i -> ((GenericDto) i).getName().equalsIgnoreCase(val)).findFirst();
                    if (op.isEmpty())
                        comboBox.getSelectionModel().selectFirst();
                    else
                        comboBox.getSelectionModel().select(op.get());

                    try {
                        Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("ComboItemTimeWait")));
                    } catch (InterruptedException e) {
                        logger.error("", e);
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }
            });
        } catch (Exception e) {

            logger.error("", e);
        }

    }

    // With Code
    public void dropdown(Schema schema, String id, String JsonIdentity, String key) {
        GenericDto dto = new GenericDto();
        try {
            if (schema.getType().contains("simpleType")) {
                LinkedHashMap<String, String> mapDropValue = JsonUtil.JsonObjSimpleParsing(JsonIdentity, key);
                Set<String> dropkeys = mapDropValue.keySet();
                if (!dropkeys.isEmpty()) {

                    String valcode = mapDropValue.get(dropkeys.iterator().next());
                    // String valcodeArr[]=valcode.split("@@");
                    // dto.setLangCode(ky);
                    // dto.setName(valcodeArr[0]);
                    // dto.setCode(valcodeArr[1]);

                    // comboBoxUtil.user_selects_combo_item2(id,valcode);

                    user_selects_combo_item(id, valcode);
                }
            } else {
                String val = JsonUtil.JsonObjParsing(JsonIdentity, key);
                // dto.setName(val);
                // dto.setCode("ara");
                user_selects_combo_item(id, val);

            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void setageDate(String id, String JsonIdentity, String key) {
        String dateofbirth = null;
        try {
            dateofbirth = JsonUtil.JsonObjParsing(JsonIdentity, key);
        } catch (Exception e) {
            logger.error("", e);
        }
        setTextFields(id, id + "ageFieldTextField", dateofbirth);

    }

    public void setdob(String id, String JsonIdentity, String key) {
        String dateofbirth[] = null;
        try {
            dateofbirth = JsonUtil.JsonObjParsing(JsonIdentity, key).split("/");
            System.out.println(dateofbirth);
            setTextFields(id, id + "ddTextField", dateofbirth[2]);
            setTextFields(id, id + "mmTextField", dateofbirth[1]);
            setTextFields(id, id + "yyyyTextField", dateofbirth[0]);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void checkSelection(String id, String JsonIdentity, String key) {
        String flag = "N";
        try {
            flag = JsonUtil.JsonObjParsing(JsonIdentity, key);
        } catch (Exception e) {
            logger.error("", e);
        }
        if (flag.equalsIgnoreCase("Y")) {

            waitsUtil.clickNodeAssert(id);

        }
    }

    public void buttonSelection(Schema schema, String id, String JsonIdentity, String key) {

        try {
            text = null;
            mapDropValue = null;
            if (schema.getType().contains("simpleType")) {
                mapDropValue = JsonUtil.JsonObjSimpleParsing(JsonIdentity, key);
                Set<String> dropkeys = mapDropValue.keySet();
                if (!dropkeys.isEmpty()) {
                    text = mapDropValue.get(dropkeys.iterator().next());
                }
            }

            else
                text = JsonUtil.JsonObjParsing(JsonIdentity, key);

            if (text != null && !text.isEmpty()) {
                HBox hbox = waitsUtil.lookupById(id + "HBOX");

                Optional<Node> opNode = hbox.getChildren().stream()
                        .filter(node -> (node instanceof Button) && ((Button) node).getText().equalsIgnoreCase(text))
                        .findFirst();
                if (opNode.isPresent()) {
                    ((Button) opNode.get()).fire();
                }

            }
        } catch (Exception e) {
            logger.error("", e);
        }

    }

    public void setageDateUpdate(Schema schema, String id, String JsonIdentity, String key, String scenario) {
        for (String uinlist : getupdateUINAttributes(JsonIdentity)) {
            if (schema.getGroup().equals(uinlist)) {
                setageDate(id, JsonIdentity, key);
            }
        }
    }

    public void setdobUpdate(Schema schema, String id, String JsonIdentity, String key) {
        for (String uinlist : getupdateUINAttributes(JsonIdentity)) {
            if (schema.getGroup().equals(uinlist)) {
                setdob(id, JsonIdentity, key);
            }
        }
    }

    public void contolType(Schema schema, String JsonIdentity, String scenario, String ageGroup) {
        String id = "#" + schema.getId();
        String key = schema.getId();
        try {
            switch (schema.getControlType()) {
            case "html":
                logger.info("Read Consent");
                break;
            case "textbox":
                getTextboxKeyValue(schema, id, JsonIdentity, key, schema.isTransliterate(), scenario);
                break;
            case "ageDate":
                setageDate(id, JsonIdentity, key);

                break;
            case "date":
                setdob(id, JsonIdentity, key);

                break;
            case "dropdown":
                dropdown(schema, id, JsonIdentity, key);
                break;
            case "checkbox":
                checkSelection(id, JsonIdentity, key);
                break;
            case "fileupload":
                fileupload(schema, JsonIdentity, key, id, scenario, ageGroup);
                break;
            case "biometrics":
                biometrics(schema, scenario, id, JsonIdentity);
                break;
            case "button":

                buttonSelection(schema, id, JsonIdentity, key);
                break;

            }

        } catch (

        Exception e) {
            logger.error("", e);
        }
    }

    public void contolTypeUpdate(Schema schema, String JsonIdentity, String scenario, String ageGroup) {
        String id = "#" + schema.getId();
        String key = schema.getId();
        try {
            switch (schema.getControlType()) {
            case "html":
                logger.info("Read Consent");
                break;
            case "textbox":
                getTextboxKeyValueUpdate(schema, id, JsonIdentity, key, schema.isTransliterate(), scenario, ageGroup);
                break;
            case "ageDate":
                setageDateUpdate(schema, id, JsonIdentity, key, scenario);
                break;
            case "date":
                setdobUpdate(schema, id, JsonIdentity, key);

                break;
            case "dropdown":
                dropdownUpdate(schema, id, JsonIdentity, key);
                break;
            case "checkbox":
                checkboxUpdate(schema, id, JsonIdentity, key);

                break;
            case "fileupload":
                fileupload(schema, JsonIdentity, key, id, scenario, ageGroup);
                break;
            case "button":

                buttonUpdateSelection(schema, id, JsonIdentity, key);
                break;
            case "biometrics":
                if (getupdateUINAttributes(JsonIdentity).contains(PropertiesUtil.getKeyValue("Biometrics"))) { // All
                                                                                                               // Bio--
                                                                                                               // IRIS(U)
                                                                                                               // <---
                    applicantBiometrics(schema, scenario, id, JsonIdentity);
                } else { // Single ---> AUTH
                    authBiometrics(schema, scenario, id, JsonIdentity);

                }

                introducerBiometrics(schema, scenario, id, JsonIdentity);

                break;
            }

        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private void getTextboxKeyValue(Schema schema, String id, String JsonIdentity, String key, boolean transliterate,
            String scenario) {
        logger.info("Schema Control Type textbox");
        mapValue = null;
        if (schema.isInputRequired()) {
            if (schema.getType().contains("simpleType")) {
                try {
                    mapValue = JsonUtil.JsonObjSimpleParsing(JsonIdentity, key);
                } catch (Exception e) {
                    logger.error("", e);
                }
                if (mapValue != null) {
                	Set<String> keys = mapValue.keySet();
                    for (String ky : keys) {
                        String idk = id + ky;
                        String v = mapValue.get(ky);
                        setTextFields(id, idk, v);
                        if (transliterate == true)
                            return;
                    }
                }
            }

            else {
                value = null;
                try {
                    value = JsonUtil.JsonObjParsing(JsonIdentity, key);
                } catch (Exception e) {
                    logger.error("", e);
                }
                try {
                    String[] listLang = PropertiesUtil.getKeyValue("langcode").split("@@");

                    setTextFields(id, id + listLang[0], value);
                } catch (IOException e) {

                    logger.error("", e);
                }
            }
        }

    }

    private void getTextboxKeyValue1(Schema schema, String id, String JsonIdentity, String key, boolean transliterate,
            String scenario) {
        logger.info("Schema Control Type textbox");
        mapValue = null;
        if (schema.isInputRequired()) {
            if (schema.getType().contains("simpleType")) {
                try {
                    mapValue = JsonUtil.JsonObjSimpleParsing(JsonIdentity, key);
                } catch (Exception e) {
                    logger.error("", e);
                }
                if (mapValue != null) {
                	Set<String> keys = mapValue.keySet();
                    for (String ky : keys) {
                        String idk = id + ky;
                        String v = mapValue.get(ky);
                        setTextFields(id, idk, v);
                        if (transliterate == true)
                            return;
                    }
                }
            }

            else {
                value = null;
                try {
                    value = JsonUtil.JsonObjParsing(JsonIdentity, key);
                } catch (Exception e) {
                    logger.error("", e);
                }
                try {
                    String[] listLang = PropertiesUtil.getKeyValue("langcode").split("@@");

                    setTextFields(id, id + listLang[0], value);
                } catch (IOException e) {

                    logger.error("", e);
                }
            }
        }

    }

}
