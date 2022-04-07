package registrationtest.pages;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.testfx.api.FxRobot;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import registrationtest.controls.Buttons;
import registrationtest.utility.JsonUtil;
import registrationtest.utility.PropertiesUtil;
import registrationtest.utility.WaitsUtil;

public class BiometricUploadPage {

    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(BiometricUploadPage.class);
    FxRobot robot;
    WaitsUtil waitsUtil;
    String BiometricDetail = "#BiometricDetail";
    String scanBtn = "canBtn";
    String IRIS_DOUBLE = "IRIS_DOUBLE";
    String FINGERPRINT_SLAB_RIGHT = "FINGERPRINT_SLAB_RIGHT";
    String FINGERPRINT_SLAB_LEFT = "FINGERPRINT_SLAB_LEFT";
    String FINGERPRINT_SLAB_THUMBS = "FINGERPRINT_SLAB_THUMBS";
    String FACE = "FACE";
    String EXCEPTION_PHOTO = "EXCEPTION_PHOTO";
    String alertImage = "#alertImage";
    String exit = "#exit";
    String success = "#context";
    Buttons buttons;

    String thresholdScoreLabel = "hresholdScoreLabel";
    String qualityScore = "ualityScore";
    String attemptSlap = "ttemptSlap";
    BioCorrectionPage bioCorrectionPage;

    public BiometricUploadPage(FxRobot robot) {
        logger.info("BiometricUploadPage Constructor");

        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
        buttons = new Buttons(robot);
        bioCorrectionPage = new BioCorrectionPage(robot);
        // waitsUtil.clickNodeAssert( BiometricDetail);

    }

    public int getThresholdScoreLabel(String idBioType) {
        idBioType=idBioType.equals("#")?idBioType.concat("t"):idBioType.concat("T");
        Label thresholdScore = waitsUtil.lookupByIdLabel(idBioType + thresholdScoreLabel, robot);
        String val = thresholdScore.getText();
        val = val.replace("%", "");
        if (val.equalsIgnoreCase(""))
            val = "0";
        return Integer.parseInt(val);

    }

    public void setThresholdScoreLabel(String thresholdScoreLabel) {
        this.thresholdScoreLabel = thresholdScoreLabel;
    }

    public int getQualityScore(String idBioType) {

        idBioType=idBioType.equals("#")?idBioType.concat("q"):idBioType.concat("Q");
        
        Label score = waitsUtil.lookupByIdLabel(idBioType + qualityScore, robot);
        String val = score.getText();
        val = val.replace("%", "");
        if (val.equalsIgnoreCase(""))
            val = "0";
        return Integer.parseInt(val);
    }

    public void setQualityScore(String qualityScore) {
        this.qualityScore = qualityScore;
    }

    public int getAttemptSlap(String idBioType) {

        idBioType=idBioType.equals("#")?idBioType.concat("a"):idBioType.concat("A");
        
        Label slap = waitsUtil.lookupByIdLabel(idBioType + attemptSlap, robot);
        String val = slap.getText();
        val = val.replace("%", "");
        if (val.equalsIgnoreCase(""))
            val = "0";
        return Integer.parseInt(val);
    }

    public void setAttemptSlap(String attemptSlap) {
        this.attemptSlap = attemptSlap;
    }

    public void exceptionsIrisDouble(String idBioType, String idModality, String jsonContent, String subType) {

        logger.info("  Bio attributes upload with List");

        List<String> listException = null;
        Boolean flag = false;
        try {
            listException = exceptionList(jsonContent);
            // IRIS DOUBLE
            clickModality(idBioType,idModality);
           
            Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("wait1")));
            
            
            if (listException.contains("leftEye") && listException.contains("rightEye")) {
                waitsUtil.clickNodeAssert("#leftEye");
                waitsUtil.clickNodeAssert("#rightEye");
                flag = true;
            } else if (listException.contains("leftEye")) {
                waitsUtil.clickNodeAssert("#leftEye");
            } else if (listException.contains("rightEye")) {
                waitsUtil.clickNodeAssert("#rightEye");
            }
            if (flag == false)
                clickScanBtn(idBioType, jsonContent, idModality);
        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void exceptionsFingerPrintSlabThumbs(String idBioType, String idModality, String jsonContent,
            String subType) {

        logger.info("  Bio attributes upload with List");

        List<String> listException = null;
        Boolean flag = false;
        try {
            listException = exceptionList(jsonContent);
            // FINGERPRINT_SLAB_THUMBS
            clickModality(idBioType,idModality);
            
            Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("wait1")));
            

            if (listException.contains("leftThumb") && listException.contains("rightThumb")) {
                waitsUtil.clickNodeAssert("#leftThumb");
                waitsUtil.clickNodeAssert("#rightThumb");
                flag = true;
            } else if (listException.contains("leftThumb")) {
                waitsUtil.clickNodeAssert("#leftThumb");
            } else if (listException.contains("rightThumb")) {
                waitsUtil.clickNodeAssert("#rightThumb");
            }
            if (flag == false)
                clickScanBtn(idBioType, jsonContent, idModality);
        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void exceptionsFingerPrintSlabRight(String idBioType, String idModality, String jsonContent,
            String subType) {

        logger.info("  Bio attributes upload with List");

        List<String> listException = null;
        Boolean flag = false;
        try {
            listException = exceptionList(jsonContent);
            // FINGERPRINT_SLAB_RIGHT
            clickModality(idBioType,idModality);
          
            Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("wait1")));
            
            
            if (listException.contains("rightIndex") && listException.contains("rightLittle")
                    && listException.contains("rightRing") && listException.contains("rightMiddle")) {
                waitsUtil.clickNodeAssert("#rightIndex");
                waitsUtil.clickNodeAssert("#rightLittle");
                waitsUtil.clickNodeAssert("#rightRing");
                waitsUtil.clickNodeAssert("#rightMiddle");
                flag = true;
            } else {
                if (listException.contains("rightIndex")) {
                    waitsUtil.clickNodeAssert("#rightIndex");
                }
                if (listException.contains("rightLittle")) {
                    waitsUtil.clickNodeAssert("#rightLittle");
                }
                if (listException.contains("rightRing")) {
                    waitsUtil.clickNodeAssert("#rightRing");
                }
                if (listException.contains("rightMiddle")) {
                    waitsUtil.clickNodeAssert("#rightMiddle");
                }
            }
            if (flag == false)
                clickScanBtn(idBioType, jsonContent, idModality);
        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public List<String> bioAuthAttributeList(String identity) {
        List<String> bioAuthAttList = new LinkedList<String>();
        String bioAuthAttributes = null;
        try {
            bioAuthAttributes = PropertiesUtil.getKeyValue("bioAuthAttributes");

            bioAuthAttList = JsonUtil.JsonObjArrayListParsing(identity, bioAuthAttributes);
        } catch (Exception e) {
            logger.error("", e);
        }

        return bioAuthAttList;
    }

    public List<String> bioAttributeList(String identity) {
        List<String> bioAttList = new LinkedList<String>();
        String bioAttributes = null;
        try {
            bioAttributes = PropertiesUtil.getKeyValue("bioAttributes");
            bioAttList = JsonUtil.JsonObjArrayListParsing(identity, bioAttributes);
        } catch (Exception e) {
            logger.error("", e);
        }

        return bioAttList;
    }

    public List<String> infantBioAttributeList(String identity) {
        List<String> bioAttList = new LinkedList<String>();
        String bioAttributes = null;
        try {
            bioAttributes = PropertiesUtil.getKeyValue("infantBioAttributes");
            bioAttList = JsonUtil.JsonObjArrayListParsing(identity, bioAttributes);
        } catch (Exception e) {
            logger.error("", e);
        }

        return bioAttList;
    }

    public List<String> exceptionList(String identity) {
        List<String> listException = new LinkedList<String>();
        String bioExceptionAttributes = null;
        try {
            bioExceptionAttributes = PropertiesUtil.getKeyValue("bioExceptionAttributes");

            listException = JsonUtil.JsonObjArrayListParsing(identity, bioExceptionAttributes);
        } catch (Exception e) {
            logger.error("", e);
        }

        return listException;
    }

    public void exceptionsFingerPrintSlabLeft(String idBioType, String idModality, String jsonContent, String subType) {

        logger.info("exceptionsFingerPrintSlabLeft");

        List<String> listException = null;
        Boolean flag = false;
        try {
            listException = exceptionList(jsonContent);
            // FINGERPRINT_SLAB_LEFT
            clickModality(idBioType,idModality);
           
            Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("wait1")));
           
            if (listException.contains("leftIndex") && listException.contains("leftLittle")
                    && listException.contains("leftRing") && listException.contains("leftMiddle")) {
                waitsUtil.clickNodeAssert("#leftIndex");
                waitsUtil.clickNodeAssert("#leftLittle");
                waitsUtil.clickNodeAssert("#leftRing");
                waitsUtil.clickNodeAssert("#leftMiddle");
                flag = true;
            } else {
                if (listException.contains("leftIndex")) {
                    waitsUtil.clickNodeAssert("#leftIndex");
                }
                if (listException.contains("leftLittle")) {
                    waitsUtil.clickNodeAssert("#leftLittle");
                }
                if (listException.contains("leftRing")) {
                    waitsUtil.clickNodeAssert("#leftRing");
                }
                if (listException.contains("leftMiddle")) {
                    waitsUtil.clickNodeAssert("#leftMiddle");
                }
            }
            if (flag == false)
                clickScanBtn(idBioType, jsonContent, idModality);
        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void modalityScoreAttempt(String idBioType, String jsonContent, String idModality) {
        String type = null; // “Biometric Device”, “Fingerprint”, “Face”, “Iris”
        int bioCapattempvalue = 0;
        if (idModality.toLowerCase().contains("iris"))
            type = "Iris";
        else if (idModality.toLowerCase().contains("fingerprint"))
            type = "Finger";
        else if (idModality.toLowerCase().contains("face"))
            type = "Face";
        else
            type = "Biometric Device";

        try {
            bioCapattempvalue = Integer.parseInt(PropertiesUtil.getKeyValue("bioCaptureAttempts"));

        } catch (NumberFormatException e) {
            logger.error("", e);
        } catch (IOException e) {
            logger.error("", e);
        }

        try {
            bioCorrectionPage.setMDSprofile(type, JsonUtil.JsonObjParsing(jsonContent, "profile"));
        } catch (Exception e) {
            logger.error("", e);
        }
        try {
            for (int i = 1; i <= bioCapattempvalue; i++) {
                bioCorrectionPage.setMDSscore(type, JsonUtil.JsonObjParsing(jsonContent, "score" + i));

             
                String idBioTypeScan=idBioType.equals("#")?idBioType.concat("s"):idBioType.concat("S");
                clickScanBasedModality(idBioTypeScan + scanBtn);
                
                try {
                    Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("wait1")));
                } catch (InterruptedException e) {
                    logger.error("", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("", e);
                }
                waitsUtil.clickNodeAssert(alertImage);

                waitsUtil.clickNodeAssert(success);
                waitsUtil.clickNodeAssert(exit);

                
                logger.info(idBioType + idModality + " ATTEMPT " + getAttemptSlap(idBioType));
                logger.info(idBioType + idModality + " SCORE " + getQualityScore(idBioType));
                logger.info(idBioType + idModality + " THRESHOLD " + getThresholdScoreLabel(idBioType));

                if (getQualityScore(idBioType) >= getThresholdScoreLabel(idBioType))
                    break;

            }
        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    public void clickScanBtn(String id, String identityJson, String idModality) {
        // Adding Logic for Bio quality capture based on threshold
        modalityScoreAttempt(id, identityJson, idModality);

        /*
         * waitsUtil.clickNodeAssert(id + scanBtn); try {
         * Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("wait6"))); } catch
         * (Exception e) { // TODO Auto-generated catch block e.printStackTrace(); }
         * waitsUtil.clickNodeAssert(alertImage);
         * 
         * waitsUtil.clickNodeAssert(success); waitsUtil.clickNodeAssert(exit);
         */
    }

    public void bioScan(String idBioType, String idModality, String jsonContent) {
        try {
            logger.info("bioScan");
            clickModality(idBioType,idModality);
            Thread.sleep(Long.parseLong(PropertiesUtil.getKeyValue("wait1")));
            
            clickScanBtn(idBioType, jsonContent, idModality);
        } catch (InterruptedException e) {
            logger.error("", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("", e);
        }

    }

    private void clickModality(String idBioType,String idModality) {
       
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("clickModality = " + idModality);
                    if(!idBioType.equals("#"))
                    {
                Button button = waitsUtil.lookupById(idModality+"Button");
                //robot.moveTo(button);
                button.fire();
                    }
                    else
                    {
                        waitsUtil.clickNodeAssert(idModality);
                    }
                  
                } catch (Exception e) {
                    logger.error("", e);
                }

            }
        });
    }

    
    private void clickScanBasedModality(String idModality) {
                  logger.info("clickModality = " + idModality);
                
                     waitsUtil.clickNodeAssert(idModality);
                             
    }
    
    private void clickScanBasedModality1(String idModality) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("clickModality = " + idModality);
                 /*   HBox hbox = waitsUtil.lookupById(idModality);

                    Optional<Node> opNode = hbox.getChildren().stream()
                            .filter(node -> (node instanceof Button)
                                    && ((Button) node).getId().equalsIgnoreCase(idModality.replace("#", "") + "Button"))
                            .findFirst();
                    if (opNode.isPresent()) {
                        ((Button) opNode.get()).fire();
                    }
                    */

                     Button button = waitsUtil.lookupById(idModality);
                     robot.moveTo(button);
                     button.fire();
                     
                   
                } catch (Exception e) {
                    logger.error("", e);
                }

            }
        });
        
        
      
        
    }
    
    
    /**
     * Bio attributes in list
     * 
     * @param list
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public void newRegbioUpload(String subtype, List<String> list, String id, String identity, String ageGroup) {
        try

        {
            logger.info("Bio attributes upload with List");

            List<String> listException = null;
            try {
                listException = exceptionList(identity);
                logger.info("listException Click" + listException);
            } catch (Exception e) {
                logger.error("", e);
            }
            if (listException.isEmpty() || ageGroup.equalsIgnoreCase("INFANT")) {
                if (list.contains(PropertiesUtil.getKeyValue("leftEye"))
                        || list.contains(PropertiesUtil.getKeyValue("rightEye")))

                    bioScan(id, id + IRIS_DOUBLE, identity);

                if (list.contains(PropertiesUtil.getKeyValue("rightIndex"))
                        || list.contains(PropertiesUtil.getKeyValue("rightLittle"))
                        || list.contains(PropertiesUtil.getKeyValue("rightRing"))
                        || list.contains(PropertiesUtil.getKeyValue("rightMiddle")))

                    bioScan(id, id + FINGERPRINT_SLAB_RIGHT, identity);

                if (list.contains(PropertiesUtil.getKeyValue("leftIndex"))
                        || list.contains(PropertiesUtil.getKeyValue("leftLittle"))
                        || list.contains(PropertiesUtil.getKeyValue("leftRing"))
                        || list.contains(PropertiesUtil.getKeyValue("leftMiddle")))

                    bioScan(id, id + FINGERPRINT_SLAB_LEFT, identity);

                if (list.contains(PropertiesUtil.getKeyValue("leftThumb"))
                        || list.contains(PropertiesUtil.getKeyValue("rightThumb")))
                    bioScan(id, id + FINGERPRINT_SLAB_THUMBS, identity);

                if (list.contains(PropertiesUtil.getKeyValue("face")))
                    bioScan(id, id + FACE, identity);

            } else if (listException.contains("leftEye") || listException.contains("rightEye")
                    || listException.contains("rightIndex") || listException.contains("rightLittle")
                    || listException.contains("rightRing") || listException.contains("rightMiddle")
                    || listException.contains("leftIndex") || listException.contains("leftLittle")
                    || listException.contains("leftRing") || listException.contains("leftMiddle") ||

                    listException.contains("leftThumb") || listException.contains("rightThumb")) {

                if (list.contains(PropertiesUtil.getKeyValue("leftEye"))
                        || list.contains(PropertiesUtil.getKeyValue("rightEye")))
                    exceptionsIrisDouble(id, id + IRIS_DOUBLE, identity, subtype);

                if (list.contains(PropertiesUtil.getKeyValue("rightIndex"))
                        || list.contains(PropertiesUtil.getKeyValue("rightLittle"))
                        || list.contains(PropertiesUtil.getKeyValue("rightRing"))
                        || list.contains(PropertiesUtil.getKeyValue("rightMiddle")))
                    exceptionsFingerPrintSlabRight(id, id + FINGERPRINT_SLAB_RIGHT, identity, subtype);

                if (list.contains(PropertiesUtil.getKeyValue("leftIndex"))
                        || list.contains(PropertiesUtil.getKeyValue("leftLittle"))
                        || list.contains(PropertiesUtil.getKeyValue("leftRing"))
                        || list.contains(PropertiesUtil.getKeyValue("leftMiddle")))
                    exceptionsFingerPrintSlabLeft(id, id + FINGERPRINT_SLAB_LEFT, identity, subtype);

                if (list.contains(PropertiesUtil.getKeyValue("leftThumb"))
                        || list.contains(PropertiesUtil.getKeyValue("rightThumb")))
                    exceptionsFingerPrintSlabThumbs(id, id + FINGERPRINT_SLAB_THUMBS, identity, subtype);

                if (list.contains(PropertiesUtil.getKeyValue("face")))
                    bioScan(id, id + FACE, identity);

                if (subtype.equals("applicant") && (!id.equals("#")))
                    bioScan(id, id + EXCEPTION_PHOTO, identity);

            }
        } catch (Exception e) {
            logger.error("", e);
        }

    }

    public void infantbioUploadTBD(String idmod, List<String> list, String id, String identity) {
        // TODO Auto-generated method stub
        try {

            if (list.contains(PropertiesUtil.getKeyValue("leftEye"))
                    || list.contains(PropertiesUtil.getKeyValue("rightEye")))

                bioScan(id, id + IRIS_DOUBLE, identity);

            if (list.contains(PropertiesUtil.getKeyValue("rightIndex"))
                    || list.contains(PropertiesUtil.getKeyValue("rightLittle"))
                    || list.contains(PropertiesUtil.getKeyValue("rightRing"))
                    || list.contains(PropertiesUtil.getKeyValue("rightMiddle")))

                bioScan(id, id + FINGERPRINT_SLAB_RIGHT, identity);

            if (list.contains(PropertiesUtil.getKeyValue("leftIndex"))
                    || list.contains(PropertiesUtil.getKeyValue("leftLittle"))
                    || list.contains(PropertiesUtil.getKeyValue("leftRing"))
                    || list.contains(PropertiesUtil.getKeyValue("leftMiddle")))

                bioScan(id, id + FINGERPRINT_SLAB_LEFT, identity);

            if (list.contains(PropertiesUtil.getKeyValue("leftThumb"))
                    || list.contains(PropertiesUtil.getKeyValue("rightThumb")))
                bioScan(id, id + FINGERPRINT_SLAB_THUMBS, identity);

            if (list.contains(PropertiesUtil.getKeyValue("face")))
                bioScan(id, id + FACE, identity);

        } catch (IOException e) {
            logger.error("", e);
        }
    }

}
