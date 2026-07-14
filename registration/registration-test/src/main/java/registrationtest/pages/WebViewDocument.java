package registrationtest.pages;



import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.testfx.api.FxRobot;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import registrationtest.controls.Alerts;
import registrationtest.controls.Buttons;
import registrationtest.pojo.output.RID;
import registrationtest.utility.ExtentReportUtil;
import registrationtest.utility.JsonUtil;
import registrationtest.utility.RobotActions;
import registrationtest.utility.WaitsUtil;

public class WebViewDocument {
    private static final org.slf4j.Logger logger= org.slf4j.LoggerFactory.getLogger(WebViewDocument.class);

    FxRobot robot;
    Stage applicationPrimaryStage;
    Scene scene;
    Node node;
    ImageView newRegImage;
    WebEngine webengine;
    WebView mywebview;
    String[] RID, RIDDateTime, firstName;
    String f2;
    final RID rid = new RID();
    WaitsUtil waitsUtil;
    Buttons buttons;
    Alerts alerts;
    RobotActions robotActions;
    String webView = "#webView";

    public WebViewDocument(FxRobot robot, Stage applicationPrimaryStage, Scene scene) {
        logger.info("WebViewDocument Constructor");
        this.robot = robot;
        this.applicationPrimaryStage = applicationPrimaryStage;
        this.scene = scene;
    }

    public WebViewDocument(FxRobot robot) {
        logger.info("WebViewDocument Constructor");
        this.robot = robot;
        waitsUtil = new WaitsUtil(robot);
    }

	public RID acceptPreview(String scenario) {

		CountDownLatch latch = new CountDownLatch(1);

		try {

			Platform.runLater(() -> {
				try {

					javafx.scene.web.WebView mywebview = waitsUtil.lookupById(webView);

					rid.setWebviewPreview(mywebview.getEngine().executeScript("document.body.innerHTML;"));

					String registrationID = (String) mywebview.getEngine()
							.executeScript("document.body.getElementsByTagName('td')[0].innerHTML;");

					RID = registrationID.split("<br>");

					String registrationIDDateTime;

					if (scenario.contains("update")) {
						registrationIDDateTime = (String) mywebview.getEngine()
								.executeScript("document.body.getElementsByTagName('td')[2].innerHTML;");
					} else {
						registrationIDDateTime = (String) mywebview.getEngine()
								.executeScript("document.body.getElementsByTagName('td')[1].innerHTML;");
					}

					RIDDateTime = registrationIDDateTime.split("<br>");

				} finally {
					latch.countDown(); // ✅ signal completion
				}
			});

			// ✅ WAIT until FX thread finishes (NO sleep!)
			latch.await();

		} catch (Exception e) {
			logger.error("", e);
		}

		return new RID(RID[1], RIDDateTime[1], rid.getWebviewPreview(), rid.getWebviewPreview());
	}

    public RID getacknowledgement(String scenario) {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.runLater(() -> {
                try {
                    javafx.scene.web.WebView mywebview = waitsUtil.lookupById(webView);

                    rid.setWebViewAck(mywebview.getEngine().executeScript("document.body.innerHTML;"));

                    String registrationId = (String) mywebview.getEngine()
                            .executeScript("document.body.getElementsByTagName('td')[1].innerHTML;");

                    RID = registrationId.split("<br>");

                    int dateTimeCellIndex = scenario.contains("update") ? 3 : 2;
                    String registrationIdDateTime = (String) mywebview.getEngine()
                            .executeScript("document.body.getElementsByTagName('td')[" + dateTimeCellIndex
                                    + "].innerHTML;");
                    RIDDateTime = registrationIdDateTime.split("<br>");
                } finally {
                    latch.countDown();
                }
            });
            latch.await();
        } catch (Exception e) {
            logger.error("", e);
        }
        return new RID(RID[1], RIDDateTime[1], rid.getWebViewAck(), rid.getWebViewAck());
    }

    public void verifyAcknowledgementPrint() {
        buttons = new Buttons(robot);
        alerts = new Alerts(robot);
        robotActions = new RobotActions(robot);

        ExtentReportUtil.test1.info("Verify Print option on Acknowledgement screen");
        clickPrintAndVerifySuccess();
        ExtentReportUtil.test1.pass("Acknowledgement print initiated successfully");

        ExtentReportUtil.test1.info("Verify acknowledgement reprint via Print option");
        for (int attempt = 2; attempt <= 3; attempt++) {
            ExtentReportUtil.test1.info("Acknowledgement reprint attempt " + attempt);
            clickPrintAndVerifySuccess();
        }
        ExtentReportUtil.test1.pass("Acknowledgement reprint verified for multiple attempts");
    }

    private void clickPrintAndVerifySuccess() {
        robotActions.dismissNativePrintDialogAsync();
        buttons.clickPrintBtn();
        alerts.verifyAndDismissPrintSuccessAlert();
    }

    private static final List<String> ALL_PREVIEW_CHECKS = Arrays.asList("applicationId", "demographics", "documents",
            "biometrics", "faceBiometric", "introducer", "uin", "authBiometrics", "additionalInfoRequestId",
            "bioExceptions", "qrCode", "previewEdit");

    private static final List<String> ALL_ACK_CHECKS = Arrays.asList("qrCode", "applicationId", "uin", "dateTime",
            "demographics", "documents", "biometrics", "authBiometrics", "bioExceptions", "additionalInfoRequestId");

    public void verifyPreviewScreen(String jsonContent, String process, String ageGroup, RID rid) {
        List<String> previewChecks = resolvePreviewChecks(jsonContent, process, ageGroup);
        if (previewChecks.isEmpty()) {
            return;
        }

        buttons = new Buttons(robot);
        String html = getPreviewHtml();
        assertNotNull(html, "Preview HTML content is empty");

        ExtentReportUtil.test1.info("Verify preview screen checks: " + previewChecks);
        for (String check : previewChecks) {
            if (!isPreviewCheckApplicable(check, jsonContent, process, ageGroup)) {
                logger.info("Skipping non-applicable preview check: {}", check);
                continue;
            }
            runPreviewCheck(check, html, jsonContent, process, ageGroup, rid);
        }
        ExtentReportUtil.test1.pass("Preview screen verification passed");
    }

    public void verifyAcknowledgementScreen(String jsonContent, String process, String ageGroup, RID rid) {
        List<String> ackChecks = resolveAckChecks(jsonContent, process, ageGroup);
        if (ackChecks.isEmpty()) {
            return;
        }

        String html = rid != null && rid.getWebViewAck() != null ? String.valueOf(rid.getWebViewAck()) : null;
        if (html == null || html.isBlank()) {
            html = getAckHtml();
        }
        assertNotNull(html, "Acknowledgement HTML content is empty");

        ExtentReportUtil.test1.info("Verify acknowledgement slip checks: " + ackChecks);
        for (String check : ackChecks) {
            if (!isAckCheckApplicable(check, jsonContent, process, ageGroup)) {
                logger.info("Skipping non-applicable acknowledgement check: {}", check);
                continue;
            }
            runAckCheck(check, html, jsonContent, process, ageGroup, rid);
        }
        ExtentReportUtil.test1.pass("Acknowledgement slip verification passed");
    }

    public void verifyEodApplicationDetails(String jsonContent, String process, String ageGroup, RID rid,
            List<String> viewTests) {
        if (viewTests == null || viewTests.isEmpty()) {
            return;
        }

        String html = getAckHtml();
        assertNotNull(html, "Pending Approval application details HTML content is empty");

        ExtentReportUtil.test1.info("Verify Pending Approval application details: " + viewTests);
        for (String check : viewTests) {
            if (!isEodCheckApplicable(check, jsonContent, process, ageGroup)) {
                logger.info("Skipping non-applicable Pending Approval check: {}", check);
                continue;
            }
            runEodCheck(check, html, jsonContent, process, ageGroup, rid);
        }
        ExtentReportUtil.test1.pass("Pending Approval application details verification passed");
    }

    private boolean isEodCheckApplicable(String check, String jsonContent, String process, String ageGroup) {
        switch (check.toLowerCase()) {
            case "qrcode":
                return !"update".equalsIgnoreCase(process) && !"biocorrection".equalsIgnoreCase(process);
            case "authbiometrics":
                return hasIdentityArray(jsonContent, "bioAuthAttributes")
                        || "update".equalsIgnoreCase(process);
            default:
                return isAckCheckApplicable(check, jsonContent, process, ageGroup);
        }
    }

    private void runEodCheck(String check, String html, String jsonContent, String process, String ageGroup,
            RID rid) {
        switch (check.toLowerCase()) {
            case "demographics":
                verifyEodDemographics(html, jsonContent);
                break;
            case "authbiometrics":
                verifyEodAuthBiometrics(html);
                break;
            default:
                runAckCheck(check, html, jsonContent, process, ageGroup, rid);
        }
    }

    private void verifyEodDemographics(String html, String jsonContent) {
        String plainText = stripHtml(html);
        assertTrue(html.contains("data:image"),
                "Applicant photo not displayed in Pending Approval details");
        assertTrue(plainText.toLowerCase().contains("dob") || plainText.matches("(?s).*\\d{4}/\\d{2}/\\d{2}.*"),
                "Date of birth not displayed in Pending Approval details");
        verifyDemoFieldIfPresent(html, jsonContent, "firstName", "First name");
        verifyDemoFieldIfPresent(html, jsonContent, "lastName", "Last name");
        ExtentReportUtil.test1.info("Pending Approval demographic details verified");
    }

    private void verifyDemoFieldIfPresent(String html, String jsonContent, String field, String label) {
        try {
            LinkedHashMap<String, String> values = JsonUtil.JsonObjSimpleParsing(jsonContent, field);
            String engValue = values.get("eng");
            if (engValue != null && !engValue.isBlank() && stripHtml(html).contains(engValue)) {
                ExtentReportUtil.test1.info(label + " verified in Pending Approval details");
            }
        } catch (Exception e) {
            logger.debug("Optional demographic field {} not verified: {}", field, e.getMessage());
        }
    }

    private void verifyEodAuthBiometrics(String html) {
        assertTrue(html.contains("data:image/jpeg") || html.toLowerCase().contains("biometric")
                        || html.toLowerCase().contains("authentication"),
                "Authentication biometric details not displayed in Pending Approval view");
        ExtentReportUtil.test1.info("Authentication biometric section verified in Pending Approval details");
    }

    private List<String> resolveAckChecks(String jsonContent, String process, String ageGroup) {
        List<String> configured = JsonUtil.getOptionalIdentityArrayList(jsonContent, "ackTests");
        if (configured != null && !configured.isEmpty()) {
            if (configured.stream().anyMatch(check -> "ALL".equalsIgnoreCase(check))) {
                return ALL_ACK_CHECKS;
            }
            return configured;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unused")
    private List<String> defaultAckChecksForProcess(String process, String ageGroup) {
        if (process == null) {
            return new ArrayList<>();
        }

        switch (process.toLowerCase()) {
            case "new":
                return Arrays.asList("qrCode", "applicationId", "dateTime", "demographics", "documents", "biometrics");
            case "lost":
                return Arrays.asList("qrCode", "applicationId", "uin", "dateTime", "demographics", "biometrics");
            case "update":
                return Arrays.asList("applicationId", "uin", "dateTime", "demographics", "documents", "authBiometrics");
            case "biocorrection":
                return Arrays.asList("additionalInfoRequestId", "uin", "dateTime", "biometrics");
            default:
                return new ArrayList<>();
        }
    }

    private boolean isAckCheckApplicable(String check, String jsonContent, String process, String ageGroup) {
        switch (check.toLowerCase()) {
            case "qrcode":
                return !"update".equalsIgnoreCase(process) && !"biocorrection".equalsIgnoreCase(process);
            case "uin":
                return !"new".equalsIgnoreCase(process);
            case "authbiometrics":
                return hasIdentityArray(jsonContent, "bioAuthAttributes")
                        || "update".equalsIgnoreCase(process);
            case "additionalinforequestid":
                return JsonUtil.getOptionalIdentityValue(jsonContent, "additionalInfoRequestId") != null
                        || "biocorrection".equalsIgnoreCase(process);
            case "bioexceptions":
                return hasIdentityArray(jsonContent, "bioExceptionAttributes");
            case "documents":
                return hasIdentityArray(jsonContent, "documentUploadAttributes")
                        || JsonUtil.getOptionalIdentityValue(jsonContent, "proofOfAddress") != null;
            case "demographics":
                return !isBioOnlyUpdate(jsonContent, process);
            default:
                return true;
        }
    }

    private boolean isBioOnlyUpdate(String jsonContent, String process) {
        if (!"update".equalsIgnoreCase(process)) {
            return false;
        }
        try {
            List<String> updateAttrs = JsonUtil.JsonObjArrayListParsing(jsonContent, "updateUINAttributes");
            if (updateAttrs == null) {
                return false;
            }
            return updateAttrs.stream().anyMatch(attr -> "Biometrics".equalsIgnoreCase(attr))
                    && updateAttrs.stream().noneMatch(attr -> "FullName".equalsIgnoreCase(attr)
                            || "Documents".equalsIgnoreCase(attr) || "Address".equalsIgnoreCase(attr));
        } catch (Exception e) {
            return false;
        }
    }

    private void runAckCheck(String check, String html, String jsonContent, String process, String ageGroup,
            RID rid) {
        switch (check.toLowerCase()) {
            case "qrcode":
                verifyQrCode(html);
                break;
            case "applicationid":
                verifyApplicationIdAndDateTime(rid, html);
                break;
            case "datetime":
                verifyDateTime(rid, html);
                break;
            case "demographics":
                verifyDemographics(html, jsonContent);
                break;
            case "documents":
                verifyDocuments(html, jsonContent);
                break;
            case "biometrics":
                verifyApplicantBiometrics(html);
                break;
            case "facebiometric":
                verifyFaceBiometric(html, jsonContent);
                break;
            case "introducer":
                verifyIntroducerDetails(html, jsonContent);
                break;
            case "uin":
                verifyUinOnAck(html, jsonContent);
                break;
            case "authbiometrics":
                verifyAuthBiometrics(html, jsonContent);
                break;
            case "bioexceptions":
                verifyBioExceptions(html, jsonContent);
                break;
            case "additionalinforequestid":
                verifyAdditionalInfoRequestId(html, jsonContent);
                break;
            default:
                logger.warn("Unknown acknowledgement check: {}", check);
        }
    }

    private String getAckHtml() {
        AtomicReference<String> htmlRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.runLater(() -> {
                try {
                    WebView ackWebView = findVisibleWebView();
                    htmlRef.set((String) ackWebView.getEngine().executeScript("document.body.innerHTML;"));
                } finally {
                    latch.countDown();
                }
            });
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out while reading acknowledgement HTML");
            }
        } catch (Exception e) {
            logger.error("Failed to read acknowledgement HTML", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return htmlRef.get();
    }

    private WebView findVisibleWebView() {
        Set<Node> nodes = robot.lookup(webView).queryAll();
        for (Node node : nodes) {
            if (node instanceof WebView && node.isVisible()) {
                return (WebView) node;
            }
        }
        return waitsUtil.lookupById(webView);
    }

    private void verifyQrCode(String html) {
        assertTrue(html.contains("data:image/png") || html.toLowerCase().contains("qr"),
                "QR code not displayed on acknowledgement slip");
        ExtentReportUtil.test1.info("QR code verified on acknowledgement slip");
    }

    private void verifyDateTime(RID rid, String html) {
        assertTrue(rid.ridDateTime != null && !rid.ridDateTime.trim().isEmpty(),
                "Date/time missing on acknowledgement slip");
        verifyHtmlContains(html, rid.ridDateTime.trim(), "Date/time");
    }

    private void verifyUinOnAck(String html, String jsonContent) {
        String uin = JsonUtil.getOptionalIdentityValue(jsonContent, "UIN");
        if (uin != null && !uin.isBlank()) {
            verifyHtmlContains(html, uin, "UIN");
            return;
        }
        assertTrue(stripHtml(html).toLowerCase().contains("uin"), "UIN not displayed on acknowledgement slip");
        ExtentReportUtil.test1.info("UIN section verified on acknowledgement slip");
    }

    private void verifyBioExceptions(String html, String jsonContent) {
        try {
            List<String> exceptionAttributes = JsonUtil.JsonObjArrayListParsing(jsonContent, "bioExceptionAttributes");
            assertTrue(exceptionAttributes != null && !exceptionAttributes.isEmpty(),
                    "Bio exception attributes missing in test data");
            assertTrue(html.contains("data:image") || html.toLowerCase().contains("exception")
                    || html.toLowerCase().contains("cross"),
                    "Biometric exceptions not displayed");
            ExtentReportUtil.test1.info("Biometric exceptions verified: " + exceptionAttributes);
        } catch (Exception e) {
            logger.error("Failed to verify biometric exceptions", e);
            throw new AssertionError("Biometric exception verification failed", e);
        }
    }

    private List<String> resolvePreviewChecks(String jsonContent, String process, String ageGroup) {
        List<String> configured = JsonUtil.getOptionalIdentityArrayList(jsonContent, "previewTests");
        if (configured != null && !configured.isEmpty()) {
            if (configured.stream().anyMatch(check -> "ALL".equalsIgnoreCase(check))) {
                return ALL_PREVIEW_CHECKS;
            }
            return configured;
        }

        List<String> defaults = new ArrayList<>(defaultChecksForProcess(process, ageGroup));
        if ("Y".equalsIgnoreCase(JsonUtil.getOptionalIdentityValue(jsonContent, "verifyPreviewEdit"))
                && !defaults.contains("previewEdit")) {
            defaults.add("previewEdit");
        }
        return defaults;
    }

    private List<String> defaultChecksForProcess(String process, String ageGroup) {
        if (process == null) {
            return new ArrayList<>();
        }

        String age = ageGroup != null ? ageGroup.toUpperCase() : "";
        switch (process.toLowerCase()) {
            case "new":
                if ("INFANT".equals(age) || "MINOR".equals(age)) {
                    return Arrays.asList("applicationId", "demographics", "documents", "biometrics", "introducer");
                }
                return Arrays.asList("applicationId", "demographics", "documents", "biometrics");
            case "lost":
                if ("INFANT".equals(age)) {
                    return Arrays.asList("applicationId", "demographics", "faceBiometric");
                }
                return Arrays.asList("applicationId", "demographics", "biometrics");
            case "update":
                if ("INFANT".equals(age) || "MINOR".equals(age)) {
                    return Arrays.asList("applicationId", "uin", "demographics", "documents", "authBiometrics",
                            "introducer");
                }
                return Arrays.asList("applicationId", "uin", "demographics", "documents", "authBiometrics");
            case "biocorrection":
                return Arrays.asList("applicationId", "additionalInfoRequestId", "biometrics");
            default:
                return new ArrayList<>();
        }
    }

    private boolean isPreviewCheckApplicable(String check, String jsonContent, String process, String ageGroup) {
        switch (check.toLowerCase()) {
            case "previewedit":
                return "Y".equalsIgnoreCase(JsonUtil.getOptionalIdentityValue(jsonContent, "verifyPreviewEdit"))
                        || hasPreviewTestsFlag(jsonContent, "ALL")
                        || hasPreviewTestsFlag(jsonContent, "previewEdit");
            case "uin":
                return JsonUtil.getOptionalIdentityValue(jsonContent, "UIN") != null;
            case "authbiometrics":
                return hasIdentityArray(jsonContent, "bioAuthAttributes");
            case "additionalinforequestid":
                return JsonUtil.getOptionalIdentityValue(jsonContent, "additionalInfoRequestId") != null;
            case "introducer":
                String age = ageGroup != null ? ageGroup.toUpperCase() : "";
                return "INFANT".equals(age) || "MINOR".equals(age);
            case "facebiometric":
                return "lost".equalsIgnoreCase(process) && "INFANT".equalsIgnoreCase(ageGroup);
            case "bioexceptions":
                return hasIdentityArray(jsonContent, "bioExceptionAttributes");
            case "qrcode":
                return "new".equalsIgnoreCase(process) || "lost".equalsIgnoreCase(process);
            case "documents":
                return hasIdentityArray(jsonContent, "documentUploadAttributes")
                        || JsonUtil.getOptionalIdentityValue(jsonContent, "proofOfAddress") != null;
            default:
                return true;
        }
    }

    private boolean hasPreviewTestsFlag(String jsonContent, String flag) {
        List<String> configured = JsonUtil.getOptionalIdentityArrayList(jsonContent, "previewTests");
        return configured != null && configured.stream().anyMatch(check -> flag.equalsIgnoreCase(check));
    }

    private boolean hasIdentityArray(String jsonContent, String field) {
        try {
            List<String> values = JsonUtil.JsonObjArrayListParsing(jsonContent, field);
            return values != null && !values.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void runPreviewCheck(String check, String html, String jsonContent, String process, String ageGroup,
            RID rid) {
        switch (check.toLowerCase()) {
            case "applicationid":
                verifyApplicationIdAndDateTime(rid, html);
                break;
            case "demographics":
                verifyDemographics(html, jsonContent);
                break;
            case "documents":
                verifyDocuments(html, jsonContent);
                break;
            case "biometrics":
                verifyApplicantBiometrics(html);
                break;
            case "facebiometric":
                verifyFaceBiometric(html, jsonContent);
                break;
            case "introducer":
                verifyIntroducerDetails(html, jsonContent);
                break;
            case "uin":
                verifyUin(html, jsonContent);
                break;
            case "authbiometrics":
                verifyAuthBiometrics(html, jsonContent);
                break;
            case "additionalinforequestid":
                verifyAdditionalInfoRequestId(html, jsonContent);
                break;
            case "bioexceptions":
                verifyBioExceptions(html, jsonContent);
                break;
            case "qrcode":
                verifyQrCode(html);
                break;
            case "previewedit":
                verifyPreviewNotEditable();
                verifyBackNavigationAllowsEdit();
                break;
            default:
                logger.warn("Unknown preview check: {}", check);
        }
    }

    private String getPreviewHtml() {
        AtomicReference<String> htmlRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.runLater(() -> {
                try {
                    WebView previewWebView = waitsUtil.lookupById(webView);
                    htmlRef.set((String) previewWebView.getEngine().executeScript("document.body.innerHTML;"));
                } finally {
                    latch.countDown();
                }
            });
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out while reading preview HTML");
            }
        } catch (Exception e) {
            logger.error("Failed to read preview HTML", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return htmlRef.get();
    }

    private void verifyApplicationIdAndDateTime(RID rid, String html) {
        assertNotNull(rid, "Preview RID is null");
        assertTrue(rid.rid != null && !rid.rid.trim().isEmpty(), "Application ID missing on preview screen");
        verifyHtmlContains(html, rid.rid.trim(), "Application ID");

        assertTrue(rid.ridDateTime != null && !rid.ridDateTime.trim().isEmpty(),
                "Date/time missing on preview screen");
        verifyHtmlContains(html, rid.ridDateTime.trim(), "Date/time");
    }

    private void verifyUin(String html, String jsonContent) {
        try {
            String uin = JsonUtil.JsonObjParsing(jsonContent, "UIN");
            verifyHtmlContains(html, uin, "UIN");
        } catch (Exception e) {
            logger.error("Failed to verify UIN on preview", e);
            throw new AssertionError("UIN verification failed", e);
        }
    }

    private void verifyAdditionalInfoRequestId(String html, String jsonContent) {
        String requestId = JsonUtil.getOptionalIdentityValue(jsonContent, "additionalInfoRequestId");
        assertTrue(requestId != null && !requestId.isBlank(),
                "Additional Info Request ID - expected value missing in test data");

        String plainText = stripHtml(html);
        String applicationId = requestId;
        int suffixIndex = requestId.indexOf("-BIOMETRIC");
        if (suffixIndex > 0) {
            applicationId = requestId.substring(0, suffixIndex);
        }

        assertTrue(plainText.contains(applicationId.trim()) || plainText.contains(requestId.trim()),
                "Additional Info Request ID not found in preview. Expected application id: " + applicationId);
        ExtentReportUtil.test1.info("Additional Info Request ID verified on preview: " + applicationId);
    }

    private void verifyDemographics(String html, String jsonContent) {
        verifyDemoField(html, jsonContent, "fullName", "Full name", true);
        verifyDemoField(html, jsonContent, "gender", "Gender", false);
        verifyDemoField(html, jsonContent, "city", "City", false);
    }

    private void verifyDemoField(String html, String jsonContent, String field, String label) {
        verifyDemoField(html, jsonContent, field, label, false);
    }

    private void verifyDemoField(String html, String jsonContent, String field, String label,
            boolean allowPrefixMatch) {
        try {
            LinkedHashMap<String, String> values = JsonUtil.JsonObjSimpleParsing(jsonContent, field);
            String engValue = values.get("eng");
            if (engValue != null && !engValue.isBlank()) {
                verifyHtmlContains(html, engValue, label, allowPrefixMatch);
            }
        } catch (Exception e) {
            logger.debug("Skipping demographic field {}: {}", field, e.getMessage());
        }
    }

    private void verifyDocuments(String html, String jsonContent) {
        String plainText = stripHtml(html);
        assertTrue(plainText.toLowerCase().contains("document") || plainText.toLowerCase().contains("proof")
                || html.contains("POI") || html.contains("POA") || html.contains("POB")
                || html.contains("data:image"),
                "Documents section not displayed on preview");

        try {
            List<String> uploadedDocs = JsonUtil.JsonObjArrayListParsing(jsonContent, "documentUploadAttributes");
            if (uploadedDocs != null && !uploadedDocs.isEmpty()) {
                List<String> missingDocs = new ArrayList<>();
                for (String docType : uploadedDocs) {
                    if (html.contains(docType) || hasDocumentLabel(html, jsonContent, docType)) {
                        ExtentReportUtil.test1.info("Document verified on preview: " + docType);
                    } else {
                        logger.info("Document type not visible on preview: {}", docType);
                        missingDocs.add(docType);
                    }
                }
                assertTrue(missingDocs.isEmpty(), "Uploaded documents missing on preview: " + missingDocs);
                verifyExcludedDocumentsNotOnPreview(html, uploadedDocs);
                return;
            }
        } catch (Exception e) {
            logger.debug("Falling back to document label verification", e);
        }

        verifyDocumentLabel(html, jsonContent, "proofOfIdentity", "Proof of identity document");
        verifyDocumentLabel(html, jsonContent, "proofOfAddress", "Proof of address document");
    }

    private boolean hasDocumentLabel(String html, String jsonContent, String docType) {
        String field = documentFieldForType(docType);
        if (field == null) {
            return false;
        }
        try {
            LinkedHashMap<String, String> values = JsonUtil.JsonObjSimpleParsing(jsonContent, field);
            for (String value : values.values()) {
                if (value != null && !value.isBlank() && stripHtml(html).contains(value)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not resolve document label for {}", docType, e);
        }
        return false;
    }

    private void verifyDocumentLabel(String html, String jsonContent, String field, String label) {
        try {
            LinkedHashMap<String, String> values = JsonUtil.JsonObjSimpleParsing(jsonContent, field);
            for (String value : values.values()) {
                if (value != null && !value.isBlank() && stripHtml(html).contains(value)) {
                    ExtentReportUtil.test1.info(label + " verified on preview");
                    return;
                }
            }
            throw new AssertionError(label + " not found on preview");
        } catch (Exception e) {
            logger.debug("Skipping document field {}: {}", field, e.getMessage());
        }
    }

    private String documentFieldForType(String docType) {
        switch (docType) {
            case "POI":
                return "proofOfIdentity";
            case "POA":
                return "proofOfAddress";
            case "POB":
                return "proofOfDateOfBirth";
            case "POR":
                return "proofOfRelationship";
            case "POE":
                return "proofOfException";
            default:
                return null;
        }
    }

    private void verifyExcludedDocumentsNotOnPreview(String html, List<String> uploadedDocs) {
        if (uploadedDocs == null || uploadedDocs.contains("POE")) {
            return;
        }
        String plainText = stripHtml(html).toLowerCase();
        assertTrue(!plainText.contains("poe") && !plainText.contains("exception proof")
                        && !plainText.contains("proof of exception"),
                "POE document should not be displayed on preview when not uploaded");
        ExtentReportUtil.test1.info("Verified POE document is not shown on preview");
    }

    private void verifyApplicantBiometrics(String html) {
        assertTrue(html.contains("data:image/jpeg") || html.toLowerCase().contains("biometric"),
                "Applicant biometric details not displayed on preview");
        ExtentReportUtil.test1.info("Applicant biometric details verified on preview");
    }

    private void verifyFaceBiometric(String html, String jsonContent) {
        verifyApplicantBiometrics(html);
        try {
            List<String> bioAttributes = JsonUtil.JsonObjArrayListParsing(jsonContent, "bioAttributes");
            assertTrue(bioAttributes != null && bioAttributes.size() == 1 && bioAttributes.contains("face"),
                    "Infant lost preview should capture face biometric only");
        } catch (Exception e) {
            logger.warn("Could not verify infant bio attributes from JSON", e);
        }
        ExtentReportUtil.test1.info("Infant face biometric verified on preview");
    }

    private void verifyAuthBiometrics(String html, String jsonContent) {
        try {
            List<String> authAttributes = JsonUtil.JsonObjArrayListParsing(jsonContent, "bioAuthAttributes");
            if (authAttributes != null && !authAttributes.isEmpty()) {
                for (String attribute : authAttributes) {
                    assertTrue(html.toLowerCase().contains(attribute.toLowerCase())
                            || html.contains("data:image/jpeg"),
                            "Auth biometric not found on preview: " + attribute);
                }
                ExtentReportUtil.test1.info("Auth biometrics verified on preview: " + authAttributes);
            }
        } catch (Exception e) {
            logger.error("Failed to verify auth biometrics on preview", e);
            throw new AssertionError("Auth biometric verification failed", e);
        }
        verifyApplicantBiometrics(html);
    }

    private void verifyIntroducerDetails(String html, String jsonContent) {
        verifyDemoField(html, jsonContent, "introducerName", "Introducer name");
        assertTrue(html.toLowerCase().contains("introducer") || html.contains("data:image/jpeg"),
                "Introducer biometric details not displayed on preview");
        ExtentReportUtil.test1.info("Introducer details verified on preview");
    }

    private void verifyHtmlContains(String html, String value, String description) {
        verifyHtmlContains(html, value, description, false);
    }

    private void verifyHtmlContains(String html, String value, String description, boolean allowPrefixMatch) {
        assertTrue(value != null && !value.isBlank(), description + " - expected value missing in test data");
        String plainText = stripHtml(html);
        boolean found = plainText.contains(value.trim());
        if (!found && allowPrefixMatch) {
            found = containsAsPrefix(plainText, value.trim());
        }
        assertTrue(found, description + " not found in preview. Expected: " + value);
        ExtentReportUtil.test1.info(description + " verified on preview");
    }

    private boolean containsAsPrefix(String plainText, String value) {
        for (String token : plainText.split("\\s+")) {
            if (token.startsWith(value)) {
                return true;
            }
        }
        return false;
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private void verifyPreviewNotEditable() {
        AtomicReference<Boolean> readOnlyRef = new AtomicReference<>(false);
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.runLater(() -> {
                try {
                    WebView previewWebView = waitsUtil.lookupById(webView);
                    Boolean isReadOnly = (Boolean) previewWebView.getEngine().executeScript(
                            "(function() {"
                                    + "var els = document.querySelectorAll('input, textarea, select, [contenteditable=\"true\"]');"
                                    + "for (var i = 0; i < els.length; i++) {"
                                    + "if (!els[i].readOnly && !els[i].disabled) return false;"
                                    + "}"
                                    + "return true;"
                                    + "})()");
                    readOnlyRef.set(Boolean.TRUE.equals(isReadOnly));
                } finally {
                    latch.countDown();
                }
            });
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out while checking preview edit restriction");
            }
        } catch (Exception e) {
            logger.error("Failed to verify preview edit restriction", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AssertionError("Preview edit restriction check failed", e);
        }

        assertTrue(Boolean.TRUE.equals(readOnlyRef.get()),
                "Preview screen should not allow editing captured data");
        ExtentReportUtil.test1.info("Preview screen is read-only");
    }

    private void verifyBackNavigationAllowsEdit() {
        ExtentReportUtil.test1.info("Verify back navigation from preview allows editing on previous screen");
        navigateToPreviousRegistrationScreen();
        assertTrue(isPreviousScreenEditable(), "Previous screen should allow data editing");
        ExtentReportUtil.test1.info("Previous screen is editable after navigating back from preview");

        buttons.clickNextBtn();
        waitsUtil.waitForNodePresent(webView);
        ExtentReportUtil.test1.info("Navigated back to preview screen successfully");
    }

    private void navigateToPreviousRegistrationScreen() {
        String[] tabs = { "#Biometrics_tab", "#Documents_tab", "#DemoDetails_tab", "#GuardianDetails_tab",
                "#IntroducerDetails_tab" };
        for (String tab : tabs) {
            try {
                waitsUtil.clickIfPresent(tab);
                if (isPreviousScreenEditable()) {
                    return;
                }
            } catch (Exception e) {
                logger.debug("Tab not available for back navigation: {}", tab);
            }
        }
        throw new AssertionError("Could not navigate to an editable screen from preview");
    }

    private boolean isPreviousScreenEditable() {
        String[] editableNodes = { "#individualBiometricsScanBtn", "#fullName", "#proofOfAddress",
                "#introducerBiometricsScanBtn" };
        for (String nodeId : editableNodes) {
            Node editableNode = robot.lookup(nodeId).tryQuery().orElse(null);
            if (editableNode != null
                    && editableNode.isVisible()
                    && !editableNode.isDisable()
                    && (!(editableNode instanceof TextInputControl)
                            || ((TextInputControl) editableNode).isEditable())) {
                return true;
            }
        }
        return false;
    }

}
