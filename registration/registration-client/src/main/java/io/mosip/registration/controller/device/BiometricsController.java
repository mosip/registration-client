package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_BIOMETRIC_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.face.FaceDecoder;
import io.mosip.biometrics.util.finger.FingerDecoder;
import io.mosip.biometrics.util.iris.IrisDecoder;
import io.mosip.commons.packet.dto.packet.BiometricsException;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.PurposeType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BIR.BIRBuilder;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.core.bioapi.exception.BiometricException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.reg.UserOnboardParentController;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.enums.Modality;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.dto.Biometric;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.service.operator.UserOnboardService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * {@code GuardianBiometricscontroller} is to capture and display the captured
 * biometrics of Guardian
 *
 * @author Sravya Surampalli
 * @since 1.0
 */
@Controller
public class BiometricsController extends BaseController /* implements Initializable */ {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(BiometricsController.class);

	@FXML
	private GridPane biometricBox;

	@FXML
	private GridPane retryBox;

	@FXML
	private Label biometricType;

	@FXML
	private ImageView biometricImage;

	@FXML
	private Label qualityScore;

	@FXML
	private Label attemptSlap;

	@FXML
	private Label attemptsLabel;

	@FXML
	private Label attemptsLabel1;

	@FXML
	private Label exceptionsLabel;

	@FXML
	private Label thresholdScoreLabel;

	@FXML
	private Label thresholdLabel;

	@FXML
	private GridPane biometricPane;

	@FXML
	private Button scanBtn;

	@FXML
	private ProgressBar bioProgress;

	@FXML
	private Label qualityText;

	@FXML
	private ColumnConstraints thresholdPane1;

	@FXML
	private ColumnConstraints thresholdPane2;

	@FXML
	private HBox bioRetryBox;

	@FXML
	private Button continueBtn;

	@FXML
	private ImageView backImageView;

	@FXML
	private ImageView scanImageView;

	@FXML
	private ImageView rightArrowImgView;

	@FXML
	private GridPane thresholdBox;

	@FXML
	private Label photoAlert;

	@Autowired
	private Streamer streamer;

	@FXML
	private Label registrationNavlabel;

	@FXML
	private Label captureTimeValue;

	@FXML
	private GridPane biometric;

	/** The iris facade. */
	@Autowired
	private BioService bioService;

	private static Map<String, byte[]> STREAM_IMAGES = new HashMap<String, byte[]>();

	private static Map<String, Double> BIO_SCORES = new HashMap<String, Double>();

	public ImageView getBiometricImage() {
		return biometricImage;
	}

	public Button getScanBtn() {
		return scanBtn;
	}

	public Button getContinueBtn() {
		return continueBtn;
	}

	public Label getPhotoAlert() {
		return photoAlert;
	}

	public GridPane getBiometricPane() {
		return biometricPane;
	}

	private String currentSubType;

	@FXML
	private GridPane checkBoxPane;

	private ResourceBundle applicationLabelBundle;

	private String currentModality;

	private int currentPosition = -1;

	private int sizeOfLeftGridPaneImageList = -1;

	private HashMap<String, HashMap<String, VBox>> exceptionMap;

	private HashMap<String, GridPane> leftHandImageBoxMap;

	private HashMap<String, List<String>> currentMap;

	private static final String AND_OPERATOR = " && ";
	private static final String OR_OPERATOR = " || ";

	@Autowired
	private UserOnboardService userOnboardService;

	@Autowired
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	private boolean isUserOnboardFlag = false;

	@FXML
	private GridPane backButton;

	@FXML
	private GridPane gheaderfooter;

	@Autowired
	private UserOnboardParentController userOnboardParentController;

	@Autowired
	private BioAPIFactory bioAPIFactory;

	@Autowired
	private UserDetailDAO userDetailDAO;

	@FXML
	private GridPane leftPanelImageGridPane;

	@FXML
	private Label subTypeLabel;

	@FXML
	private GridPane parentProgressPane;

	private Service<List<BiometricsDto>> rCaptureTaskService;

	private Service<MdmBioDevice> deviceSearchTask;

	public void stopRCaptureService() {
		if (rCaptureTaskService != null && rCaptureTaskService.isRunning()) {
			rCaptureTaskService.cancel();
		}
	}

	public void stopDeviceSearchService() {
		if (deviceSearchTask != null && deviceSearchTask.isRunning()) {
			deviceSearchTask.cancel();
		}
	}

	private Node exceptionVBox;

	private String loggerClassName = LOG_REG_BIOMETRIC_CONTROLLER;

	/*
	 * (non-Javadoc)
	 *
	 * @see javafx.fxml.Initializable#initialize(java.net.URL,
	 * java.util.ResourceBundle)
	 */
	@FXML
	public void initialize() {
		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Loading of Guardian Biometric screen started");

		setImage(scanImageView, RegistrationConstants.SCAN_IMG);
		setImage(backImageView, RegistrationConstants.ARROW_LEFT_IMG);
		setImage(rightArrowImgView, RegistrationConstants.ARROW_RIGHT_IMG);

		currentMap = new LinkedHashMap<>();
		leftHandImageBoxMap = new HashMap<>();
		exceptionMap = new HashMap<>();

		backButton.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(backImageView, RegistrationConstants.BACK_FOCUSED_IMG);
			} else {
				setImage(backImageView, RegistrationConstants.ARROW_LEFT_IMG);
			}
		});

		applicationLabelBundle = applicationContext.getBundle(applicationContext.getApplicationLanguage(),
				RegistrationConstants.LABELS);

	}

	public void populateBiometricPage(boolean isUserOnboard, boolean isGoingBack) {
		LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"populateBiometricPage invoked, isUserOnboard : " + isUserOnboard);

		if(isUserOnboard) {
			attemptsLabel.setVisible(false);
			bioRetryBox.setVisible(false);
			attemptsLabel1.setVisible(false);
			exceptionsLabel.setVisible(false);
			attemptSlap.setVisible(false);
		}

		isUserOnboardFlag = isUserOnboard;

		Map<Entry<String, String>, Map<String, List<List<String>>>> mapToProcess = getOnboardUserMap();

		if (mapToProcess.isEmpty()) {
			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"populateBiometricPage mapToProcess is EMTPY");
			updatePageFlow(RegistrationConstants.GUARDIAN_BIOMETRIC, false);
			return;
		}

		//removeInapplicableCapturedData(mapToProcess);

		if (isUserOnboard) {
			registrationNavlabel.setVisible(false);
			backButton.setVisible(false);
			gheaderfooter.setVisible(false);
			continueBtn.setText(applicationContext.getApplicationLanguageLabelBundle().getString("save"));
		} else {
			registrationNavlabel.setVisible(true);
			backButton.setVisible(true);
			gheaderfooter.setVisible(true);
			continueBtn.setText(applicationContext.getApplicationLanguageLabelBundle().getString("continue"));
		}

		checkBoxPane.getChildren().clear();
		leftPanelImageGridPane.getChildren().clear();

		// comboBoxMap.clear();
		// checkBoxMap.clear();
		currentMap.clear();
		leftHandImageBoxMap.clear();
		exceptionMap.clear();

		leftPanelImageGridPane.setAlignment(Pos.TOP_LEFT);
		leftPanelImageGridPane.setPadding(new Insets(70, 100, 100, 70)); // margins around the whole grid
		// (top/right/bottom/left)
		for (Entry<Entry<String, String>, Map<String, List<List<String>>>> subType : mapToProcess.entrySet()) {

			int rowIndex = 0;
			// leftPanelImageGridPane.getChildren().add(new Label("Biometrics"));
			GridPane gridPane = getGridPane(subType.getKey());

			// ComboBox<Entry<String, String>> comboBox = buildComboBox(subType.getKey());
			HashMap<String, VBox> subMap = new HashMap<>();
			currentMap.put(subType.getKey().getKey(), new ArrayList<String>());
			for (Entry<String, List<List<String>>> biometric : subType.getValue().entrySet()) {
				List<List<String>> listOfCheckBoxes = biometric.getValue();
				currentMap.get(subType.getKey().getKey()).addAll(listOfCheckBoxes.get(0));
				if (!listOfCheckBoxes.get(0).isEmpty()) {

					// comboBox.getItems().add(new SimpleEntry<String, String>(biometric.getKey(),
					// applicationLabelBundle.getString(biometric.getKey())));

					gridPane.add(getImageVBox(biometric.getKey(), subType.getKey().getKey(), listOfCheckBoxes.get(0)),
							1, rowIndex);

					rowIndex++;

					// gridPane.getChildren().add(getImageVBox(biometric.getKey()));
					if (!listOfCheckBoxes.get(0).get(0).equals("face")) {

						VBox vboxForCheckBox = new VBox();
						vboxForCheckBox.setSpacing(5);
						Label checkBoxTitle = new Label();
						checkBoxTitle.setText(applicationLabelBundle.getString("exceptionCheckBoxPaneLabel"));
						vboxForCheckBox.getChildren().addAll(checkBoxTitle);
						checkBoxTitle.getStyleClass().add("demoGraphicFieldLabel");

						vboxForCheckBox.getChildren().add(getExceptionImagePane(biometric.getKey(),
								listOfCheckBoxes.get(0), listOfCheckBoxes.get(1), subType.getKey().getKey()));

						vboxForCheckBox.setVisible(false);
						vboxForCheckBox.setManaged(false);

						checkBoxPane.add(vboxForCheckBox, 0, 0);

						// checkBoxPane.setVisible(true);

						// checkBoxMap.put(subType.getKey().getKey(), subMap);
						subMap.put(biometric.getKey(), vboxForCheckBox);

						exceptionMap.put(subType.getKey().getKey(), subMap);

					}
				}
			}

			if (isApplicant(subType.getKey().getKey()) && rowIndex > 1) {

				gridPane.add(
						getExceptionImageVBox(RegistrationConstants.EXCEPTION_PHOTO, subType.getKey().getKey(), null),
						1, rowIndex);

				if (rowIndex >= 5) {

					gridPane.setPadding(new Insets(100, 10, 10, 10));
				}

			}
		}

		initializeState(isGoingBack);
	}

	private VBox getExceptionImageVBox(String modality, String key, Object object) {

		VBox vBox = new VBox();

		vBox.setAlignment(Pos.BASELINE_LEFT);
		vBox.setId(modality);

		HBox hBox = new HBox();
		// hBox.setAlignment(Pos.BOTTOM_RIGHT);
		Image image = null;

		/*if (hasApplicantBiometricException()) {
			vBox.setVisible(true);

			if (getRegistrationDTOFromSession().getDocuments().containsKey("proofOfException")) {
				byte[] documentBytes = getRegistrationDTOFromSession().getDocuments().get("proofOfException")
						.getDocument();
				image = convertBytesToImage(documentBytes);

			}
		} else {*/
		vBox.setVisible(false);
		//}

		ImageView imageView;
		try {
			imageView = new ImageView(getImage(getImageIconPath(modality),true));
			imageView.setFitHeight(80);
			imageView.setFitWidth(85);

			Tooltip tooltip = new Tooltip(applicationLabelBundle.getString(modality));
			tooltip.getStyleClass().add(RegistrationConstants.TOOLTIP_STYLE);
			// Tooltip.install(hBox, tooltip);
			hBox.setOnMouseEntered(event -> tooltip.show(hBox, event.getScreenX(), event.getScreenY() + 15));
			hBox.setOnMouseExited(event -> tooltip.hide());
			hBox.getChildren().add(imageView);
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error("Error while getting image");
		}


		vBox.getChildren().add(hBox);

		// vBox.getChildren().add(imageView);

		vBox.setOnMouseClicked((event) -> {
			displayExceptionBiometric(vBox.getId());
		});

		vBox.setFillWidth(true);
		vBox.setMinWidth(100);

		// vBox.setMinHeight(100);
		vBox.getStyleClass().add(RegistrationConstants.BIOMETRICS_DISPLAY);
		// vBox.setBorder(new Border(
		// new BorderStroke(Color.PINK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
		// BorderWidths.FULL)));

		exceptionVBox = vBox;
		return vBox;
	}

	private void displayExceptionBiometric(String modality) {

		retryBox.setVisible(true);
		biometricBox.setVisible(true);
		biometricType.setText(applicationLabelBundle.getString(modality));

		disableLastCheckBoxSection();
		this.currentModality = modality;
		enableCurrentCheckBoxSection();

		setScanButtonVisibility(false, scanBtn);
		// Get the stream image from Bio ServiceImpl and load it in the image pane

		clearBioLabels();

		clearRetryAttemptsBox();

		thresholdScoreLabel.setText(RegistrationConstants.HYPHEN);

		thresholdPane1.setPercentWidth(Double.parseDouble("0"));
		thresholdPane2.setPercentWidth(100.00 - (Double.parseDouble("0")));

		thresholdLabel.setText(
				RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.THRESHOLD).concat("  ").concat("0").concat(RegistrationConstants.PERCENTAGE));

		/*if (hasApplicantBiometricException()
				&& getRegistrationDTOFromSession().getDocuments().containsKey("proofOfException")) {

			DocumentDto documentDto = getRegistrationDTOFromSession().getDocuments().get("proofOfException");

			Image image = convertBytesToImage(documentDto.getDocument());
			biometricImage.setImage(image);

			addImageInUIPane(currentSubType, currentModality, convertBytesToImage(documentDto.getDocument()), true);

		} else {*/
		try {
			biometricImage.setImage(getImage(getImageIconPath(modality),true));
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Exception while getting image",exception);
		}

		addImageInUIPane(currentModality, currentModality, null, false, false);
		//}
	}

	private void initializeState(boolean isGoingBack) {

		sizeOfLeftGridPaneImageList = leftHandImageBoxMap.size();

		if (sizeOfLeftGridPaneImageList > 0) {
			if (isGoingBack) {
				currentPosition = leftHandImageBoxMap.size() - 1;
				currentSubType = getListOfBiometricSubTypes().get(currentPosition);
			} else {
				currentPosition = 0;
				currentSubType = getListOfBiometricSubTypes().get(currentPosition);
			}
		}

		currentSubType = getListOfBiometricSubTypes().get(currentPosition);
		enableGridPane(findImageListGridPane());
		biometricBox.setVisible(false);
		retryBox.setVisible(false);
		refreshContinueButton();
		displaycurrentUiElements();
	}

	private GridPane findImageListGridPane() {

		return leftHandImageBoxMap.get(getListOfBiometricSubTypes().get(currentPosition));
	}

	private void enableGridPane(GridPane gridPane) {
		if (gridPane != null) {
			gridPane.setVisible(true);
			gridPane.setManaged(true);

			if (!isUserOnboardFlag) {
				subTypeLabel.setText(getMapOfbiometricSubtypes().get(currentSubType));
			} else {

				subTypeLabel.setText(applicationContext.getApplicationLanguageLabelBundle()
						.getString(RegistrationUIConstants.ONBOARD_USER_TITLE));
			}

		}
	}

	private void disableGridPane(GridPane gridPane) {
		if (gridPane != null) {
			gridPane.setVisible(false);
			gridPane.setManaged(false);
			subTypeLabel.setText(RegistrationConstants.EMPTY);

		}
	}


	private void setScanButtonVisibility(boolean isAllExceptions, Button scanBtn2) {
		scanBtn.setDisable(isAllExceptions);

	}

	private boolean isAllExceptions(List<Node> exceptionImageViews) {
		boolean isAllExceptions = true;
		if (exceptionImageViews != null && !exceptionImageViews.isEmpty()) {

			for (Node exceptionImageView : ((Pane) exceptionImageViews.get(1)).getChildren()) {
				if (exceptionImageView instanceof ImageView && exceptionImageView.getId() != null
						&& !exceptionImageView.getId().isEmpty()) {
					isAllExceptions = ((ImageView) exceptionImageView).getOpacity() == 1 ? isAllExceptions
							? isBiometricExceptionAvailable(currentSubType, exceptionImageView.getId())
							: isAllExceptions : false;
				}
			}
		}

		return isAllExceptions;
	}

	private void goToNext() {
		if (currentPosition + 1 < sizeOfLeftGridPaneImageList) {

			disableGridPane(findImageListGridPane());

			++currentPosition;

			currentSubType = getListOfBiometricSubTypes().get(currentPosition);
			enableGridPane(findImageListGridPane());

			refreshContinueButton();

			displaycurrentUiElements();

		}
	}

	private void displaycurrentUiElements() {
		try {

			GridPane gridPane = findImageListGridPane();

			if (gridPane.getChildren() != null) {

				displayBiometric(gridPane.getChildren().get(0).getId());
			}
			// ComboBox<Object> comboBox = (ComboBox<Object>)
			// findComboBox().getChildren().get(1);
			//
			// comboBox.setValue((SimpleEntry<String, String>) comboBox.getItems().get(0));
		} catch (NullPointerException | ClassCastException exception) {
			LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));

		}

	}

	private void goToPrevious() {
		if (currentPosition > 0) {
			disableGridPane(findImageListGridPane());
			currentPosition--;

			currentSubType = getListOfBiometricSubTypes().get(currentPosition);
			enableGridPane(findImageListGridPane());

			refreshContinueButton();

			displaycurrentUiElements();
		}
	}

	/**
	 * Displays biometrics
	 *
	 * @param modality the modality for displaying biometrics
	 */
	private void displayBiometric(String modality) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Displaying biometrics to capture");

		retryBox.setVisible(true);
		biometricBox.setVisible(true);
		biometricType.setText(applicationLabelBundle.getString(modality));

		disableLastCheckBoxSection();
		this.currentModality = modality;
		enableCurrentCheckBoxSection();

		// get List of captured Biometrics based on nonExceptionBio Attributes
		List<BiometricsDto> capturedBiometrics = null;

		// if (!isFace(modality)) {
		List<Node> checkBoxNodes = getCheckBoxes(currentSubType, currentModality);

		List<String> exceptionBioAttributes = null;
		List<String> nonExceptionBioAttributes = isFace(modality) ? RegistrationConstants.faceUiAttributes : null;

		if (!checkBoxNodes.isEmpty()) {
			for (Node node : ((Pane) checkBoxNodes.get(1)).getChildren()) {
				if (node instanceof ImageView) {
					ImageView imageView = (ImageView) node;
					String bioAttribute = imageView.getId();
					if (bioAttribute != null && !bioAttribute.isEmpty()) {
						if (imageView.getOpacity() == 1) {
							exceptionBioAttributes = exceptionBioAttributes != null ? exceptionBioAttributes
									: new LinkedList<String>();
							exceptionBioAttributes.add(bioAttribute);
						} else {
							nonExceptionBioAttributes = nonExceptionBioAttributes != null ? nonExceptionBioAttributes
									: new LinkedList<String>();
							nonExceptionBioAttributes.add(bioAttribute);
						}
					}
				}
			}
		}

		if (nonExceptionBioAttributes != null) {
			capturedBiometrics = getBiometrics(currentSubType, nonExceptionBioAttributes);
		}

		/*
		 * } else { capturedBiometrics = getBiometrics(currentSubType,
		 * Arrays.asList(RegistrationConstants.faceUiAttributes.get(0))); }
		 */

		updateBiometric(modality, getImageIconPath(modality), getBiometricThreshold(modality), getRetryCount(modality));

		if (capturedBiometrics != null && !capturedBiometrics.isEmpty()) {

			loadBiometricsUIElements(capturedBiometrics, currentSubType, modality);
		}

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Parent/Guardian Biometrics captured");
	}

	private String getRetryCount(String modality) {
		// modality = modality.toLowerCase();

		String irisRetryCount = null;

		if (modality != null) {
			switch (modality) {

				case RegistrationConstants.FACE:
					irisRetryCount = RegistrationConstants.FACE_RETRY_COUNT;
					break;
				case RegistrationConstants.IRIS_DOUBLE:
					irisRetryCount = RegistrationConstants.IRIS_RETRY_COUNT;
					break;

				case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
					irisRetryCount = RegistrationConstants.FINGERPRINT_RETRIES_COUNT;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
					irisRetryCount = RegistrationConstants.FINGERPRINT_RETRIES_COUNT;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
					irisRetryCount = RegistrationConstants.FINGERPRINT_RETRIES_COUNT;
					break;

			}
		}

		return irisRetryCount;

	}

	private String getBiometricThreshold(String modality) {
		// modality = modality.toLowerCase();

		String biomnetricThreshold = null;
		if (modality != null) {
			switch (modality) {

				case RegistrationConstants.FACE:
					biomnetricThreshold = RegistrationConstants.FACE_THRESHOLD;
					break;
				case RegistrationConstants.IRIS_DOUBLE:
					biomnetricThreshold = RegistrationConstants.IRIS_THRESHOLD;
					break;

				case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
					biomnetricThreshold = RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
					biomnetricThreshold = RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
					biomnetricThreshold = RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD;
					break;

			}
		}

		return biomnetricThreshold;

	}

	private String getImageIconPath(String modality) {
		String imageIconPath = RegistrationConstants.DEFAULT_EXCEPTION_IMG;

		if (modality != null) {
			switch (modality) {
				case RegistrationConstants.FACE:
					imageIconPath = RegistrationConstants.FACE_IMG;
					break;
				case RegistrationConstants.IRIS_DOUBLE:
					imageIconPath = RegistrationConstants.DOUBLE_IRIS_IMG;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
					imageIconPath = RegistrationConstants.RIGHTPALM_IMG;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
					imageIconPath = RegistrationConstants.LEFTPALM_IMG;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
					imageIconPath = RegistrationConstants.THUMB_IMG;
					break;
			}
		}
		return imageIconPath;
	}

	private void enableCurrentCheckBoxSection() {

		if (exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)) != null && exceptionMap
				.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality) != null) {
			exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
					.setVisible(true);
			exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
					.setManaged(true);
			checkBoxPane.setVisible(true);
			checkBoxPane.setManaged(true);

		}

	}

	private void disableLastCheckBoxSection() {
		checkBoxPane.setVisible(false);
		if (currentPosition != -1) {
			if (this.currentModality != null
					&& exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)) != null && exceptionMap
					.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality) != null) {
				exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
						.setVisible(false);
				exceptionMap.get(getListOfBiometricSubTypes().get(currentPosition)).get(this.currentModality)
						.setManaged(false);
			}
		}

	}

	/**
	 * This method will allow to scan and upload documents
	 */
	@Override
	public void scan(Stage popupStage) {
		if (isExceptionPhoto(currentModality)) {
			try {
				//saveProofOfExceptionDocument(byteArray);
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS));

				//scanPopUpViewController.getPopupStage().close();

			} catch (Exception exception) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR));

				LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Error while capturing exception photo : " + ExceptionUtils.getStackTrace(exception));

			}

		}
	}


	/**
	 * Scan the biometrics
	 *
	 * @param event the event for scanning biometrics
	 */
	@FXML
	private void scan(ActionEvent event) {
		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Displaying Scan popup for capturing biometrics");

		auditFactory.audit(getAuditEventForScan(currentModality), Components.REG_BIOMETRICS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		biometric.setDisable(true);

		deviceSearchTask = new Service<MdmBioDevice>() {
			@Override
			protected Task<MdmBioDevice> createTask() {
				return new Task<MdmBioDevice>() {
					/*
					 * (non-Javadoc)
					 *
					 * @see javafx.concurrent.Task#call()
					 */
					@Override
					protected MdmBioDevice call() throws RegBaseCheckedException {

						LOGGER.info("device search request started {}" , System.currentTimeMillis());

						String modality = isFace(currentModality) || isExceptionPhoto(currentModality)
								? RegistrationConstants.FACE_FULLFACE : currentModality;

						MdmBioDevice bioDevice =deviceSpecificationFactory.getDeviceInfoByModality(modality);

						if (deviceSpecificationFactory.isDeviceAvailable(bioDevice)) {
							return bioDevice;
						} else {
							throw new RegBaseCheckedException(
									RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorCode(),
									RegistrationExceptionConstants.MDS_BIODEVICE_NOT_FOUND.getErrorMessage());
						}

					}
				};
			}
		};
		deviceSearchTask.start();
		// mdmBioDevice = null;

		deviceSearchTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {

				MdmBioDevice mdmBioDevice = deviceSearchTask.getValue();

				try {


					// Disable Auto-Logout
					SessionContext.setAutoLogout(false);

					if (mdmBioDevice == null) {
						generateAlert(RegistrationConstants.ERROR,  RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND));

						return;
					}

					// Start Stream
					//setPopViewControllerMessage(true, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.STREAMING_PREP_MESSAGE));

					InputStream urlStream = bioService.getStream(mdmBioDevice,
							isFace(currentModality) ? RegistrationConstants.FACE_FULLFACE : currentModality);

					boolean isStreamStarted = urlStream != null && urlStream.read() != -1;
					if (!isStreamStarted) {
						LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								"URL Stream was null for : " + System.currentTimeMillis());
						deviceSpecificationFactory.initializeDeviceMap(true);
						streamer.setUrlStream(null);
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.STREAMING_ERROR));

						return;
					}

					rCaptureTaskService();

					streamer.startStream(urlStream, biometricImage, biometricImage);

				} catch (RegBaseCheckedException | IOException exception) {
					LOGGER.error("Error while streaming : ", exception);
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.STREAMING_ERROR));

					// Enable Auto-Logout
					SessionContext.setAutoLogout(true);
					streamer.setUrlStream(null);
					biometric.setDisable(false);
				}

			}

		});

		// mdmBioDevice = null;
		deviceSearchTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {

				LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Exception while finding bio device");
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND));
				streamer.setUrlStream(null);
				biometric.setDisable(false);
			}
		});

	}

	private boolean isFace(String currentModality) {
		return currentModality != null && currentModality.toUpperCase().contains(RegistrationConstants.FACE.toUpperCase());
	}

	private List<String> getSelectedExceptionsByBioType(String subType, String modality)
			throws RegBaseCheckedException {
		List<String> selectedExceptions = new LinkedList<String>();

		// Get List of check boxes using the grid pane
		List<Node> paneList = getCheckBoxes(subType, modality);
		if (paneList != null && !paneList.isEmpty()) {

			Pane pane = (Pane) paneList.get(1);

			for (Node exceptionImage : pane.getChildren()) {
				if (exceptionImage instanceof ImageView && exceptionImage.getId() != null
						&& !exceptionImage.getId().isEmpty()) {
					ImageView image = (ImageView) exceptionImage;
					if (image.getOpacity() == 1) {

						selectedExceptions.add(image.getId());

					}
				}
			}
		}
		return selectedExceptions;
	}

	private List<Node> getCheckBoxes(String subType, String modality) {

		List<Node> exceptionCheckBoxes = new ArrayList<>();
		if (exceptionMap.get(subType) != null && exceptionMap.get(subType).get(modality) != null) {
			exceptionCheckBoxes = exceptionMap.get(subType).get(modality).getChildren();
		}
		return exceptionCheckBoxes;

	}

	public void rCaptureTaskService() {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Capture request called" + System.currentTimeMillis());

		rCaptureTaskService = new Service<List<BiometricsDto>>() {
			@Override
			protected Task<List<BiometricsDto>> createTask() {
				return new Task<List<BiometricsDto>>() {
					/*
					 * (non-Javadoc)
					 *
					 * @see javafx.concurrent.Task#call()
					 */
					@Override
					protected List<BiometricsDto> call() throws RegBaseCheckedException, IOException {

						LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								"Capture request started" + System.currentTimeMillis());

						currentSubType = getListOfBiometricSubTypes().get(currentPosition);
						return rCapture(currentSubType, currentModality);

					}
				};
			}
		};
		rCaptureTaskService.start();

		rCaptureTaskService.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {

				LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "RCapture task failed");
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR));

				LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Enabling LogOut");
				// Enable Auto-Logout
				SessionContext.setAutoLogout(true);

				LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Setting URL Stream as null");
				streamer.setUrlStream(null);
				biometric.setDisable(false);
			}
		});
		rCaptureTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {

				LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"RCapture task was successful");
				try {
					List<BiometricsDto> mdsCapturedBiometricsList = rCaptureTaskService.getValue();
					boolean isValidBiometric = isValidBiometric(mdsCapturedBiometricsList);
					LOGGER.debug("biometrics captured from mock/real MDM was valid : {}", isValidBiometric);

					if(!isValidBiometric) {// if any above checks failed show alert capture failure
						generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_FAILURE));
						return;
					}

					LOGGER.debug("Started local de-dup validation");// validate local de-dup check
					if(identifyInLocalGallery(mdsCapturedBiometricsList,
							Biometric.getSingleTypeByModality(isFace(currentModality) || isExceptionPhoto(currentModality) ?
									"FACE_FULL FACE" : currentModality).value())) {
						LOGGER.info("*** Local DE_DUPE validation -- found local match !! ");
						generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.LOCAL_DEDUP_CHECK_FAILED));
						return;
					}

					List<BiometricsDto> registrationDTOBiometricsList = new LinkedList<>();

					List<String> exceptionBioAttributes = getSelectedExceptionsByBioType(currentSubType,
							currentModality);
					// save to registration DTO
					for (BiometricsDto biometricDTO : mdsCapturedBiometricsList) {
						LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								"BiometricDTO captured from mock/real MDM >>> "
										+ biometricDTO.getBioAttribute());

						if (!exceptionBioAttributes.isEmpty()
								&& exceptionBioAttributes.contains(biometricDTO.getBioAttribute())) {
							LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
									"As bio atrribute marked as exception not storing into registration DTO : "
											+ biometricDTO.getBioAttribute());
							continue;
						} else {
							biometricDTO.setSubType(currentSubType);
							registrationDTOBiometricsList.add(biometricDTO);
						}
					}

					LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"started Saving filtered biometrics into registration DTO");
					registrationDTOBiometricsList = saveCapturedBiometricData(currentSubType,
							registrationDTOBiometricsList);

					LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"Completed Saving filtered biometrics into registration DTO");

					if (registrationDTOBiometricsList.isEmpty()) {
						// request response mismatch
						generateAlert(RegistrationConstants.ALERT_INFORMATION,
								RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_FAILURE));
					}
					LOGGER.info("Adding streaming images and bio scores into local map");

					addStreamImageAndScoreToCache(registrationDTOBiometricsList, currentModality, registrationDTOBiometricsList.get(0).getNumOfRetries());

					LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"using captured response fill the fields like quality score and progress bar,,etc,.. UI");

					loadBiometricsUIElements(registrationDTOBiometricsList, currentSubType,
							currentModality);

					refreshContinueButton();

					generateAlert(RegistrationConstants.ALERT_INFORMATION,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS));
					biometric.setDisable(false);
				} catch (Exception exception) {
					LOGGER.error("Exception while getting the scanned biometrics for operator onboarding", exception);
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR));
				} finally {
					// Enable Auto-Logout
					SessionContext.setAutoLogout(true);
					streamer.setUrlStream(null);
					biometric.setDisable(false);
				}
			}
		});
		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Scan process ended for capturing biometrics");

	}

	private void addStreamImageAndScoreToCache(List<BiometricsDto> biometricsDtos, String modalityName, int retry) throws Exception {
		try {
			double score = 0;
			switch (modalityName) {
				case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
				case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
				case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
					for(BiometricsDto dto : biometricsDtos) {
						ConvertRequestDto convertRequestDto = new ConvertRequestDto();
						convertRequestDto.setVersion("ISO19794_4_2011");
						convertRequestDto.setInputBytes(dto.getAttributeISO());
						STREAM_IMAGES.put(String.format("%s_%s_%s",
								currentSubType, dto.getBioAttribute(), String.valueOf(retry)),
								FingerDecoder.convertFingerISOToImageBytes(convertRequestDto));
						score += dto.getQualityScore();
					}
					BIO_SCORES.put(String.format("%s_%s_%s",
							currentSubType, modalityName, String.valueOf(retry)),
							score / biometricsDtos.size());
					break;
				case RegistrationConstants.IRIS_DOUBLE:
					for(BiometricsDto dto : biometricsDtos) {
						ConvertRequestDto convertRequestDto = new ConvertRequestDto();
						convertRequestDto.setVersion("ISO19794_6_2011");
						convertRequestDto.setInputBytes(dto.getAttributeISO());
						STREAM_IMAGES.put(String.format("%s_%s_%s",
								currentSubType, dto.getBioAttribute(), String.valueOf(retry)),
								IrisDecoder.convertIrisISOToImageBytes(convertRequestDto));
						score += dto.getQualityScore();
					}
					BIO_SCORES.put(String.format("%s_%s_%s",
							currentSubType, modalityName, String.valueOf(retry)),
							score / biometricsDtos.size());
					break;

				case RegistrationConstants.FACE:
					BiometricsDto faceDto = biometricsDtos.toArray(new BiometricsDto[0])[0];
					ConvertRequestDto convertRequestDto = new ConvertRequestDto();
					convertRequestDto.setVersion("ISO19794_5_2011");
					convertRequestDto.setInputBytes(faceDto.getAttributeISO());
					STREAM_IMAGES.put(String.format("%s_%s_%s",
							currentSubType, faceDto.getBioAttribute(), String.valueOf(retry)),
							FaceDecoder.convertFaceISOToImageBytes(convertRequestDto));
					BIO_SCORES.put(String.format("%s_%s_%s",
							currentSubType, modalityName, String.valueOf(retry)),
							faceDto.getQualityScore());
					break;
			}
		} catch (Exception exception) {
			LOGGER.error("Failed to extract image from ISO", exception);
			throw exception;
		}
	}

	private boolean isValidBiometric(List<BiometricsDto> mdsCapturedBiometricsList) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Validating captured biometrics");

		boolean isValid = mdsCapturedBiometricsList != null && !mdsCapturedBiometricsList.isEmpty();

		if (isValid) {
			for (BiometricsDto biometricsDto : mdsCapturedBiometricsList) {
				if (biometricsDto.getBioAttribute() == null
						|| biometricsDto.getBioAttribute().equalsIgnoreCase(RegistrationConstants.JOB_UNKNOWN)) {

					LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"Unknown bio attribute identified in captured biometrics");

					isValid = false;
					break;
				}
			}
		}

		return isValid;
	}

	private List<BiometricsDto> rCapture(String subType, String modality) throws RegBaseCheckedException, IOException {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Finding exception bio attributes" + System.currentTimeMillis());

		List<String> exceptionBioAttributes = new LinkedList<>();
		if (isExceptionPhoto(modality)) {

			if (getRegistrationDTOFromSession() != null) {
				for (Entry<String, BiometricsException> bs : getRegistrationDTOFromSession().getBiometricExceptions()
						.entrySet()) {

					if (isApplicant(bs.getValue().getIndividualType())) {

						exceptionBioAttributes.add(bs.getValue().getMissingBiometric());
					}

				}
			}

		} else {
			exceptionBioAttributes = getSelectedExceptionsByBioType(currentSubType, currentModality);
		}
		// Check count
		int count = 1;

		MDMRequestDto mdmRequestDto = new MDMRequestDto(
				isFace(modality) || isExceptionPhoto(modality) ? RegistrationConstants.FACE_FULLFACE : modality,
				exceptionBioAttributes.toArray(new String[0]), "Registration",
				io.mosip.registration.context.ApplicationContext.getStringValueFromApplicationMap(
						RegistrationConstants.SERVER_ACTIVE_PROFILE),
				Integer.valueOf(getCaptureTimeOut()), count,
				getThresholdScoreInInt(getThresholdKeyByBioType(Modality.valueOf(modality))));

		LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"exceptionBioAttributes passed to mock/real MDM >>> " + exceptionBioAttributes);

		return bioService.captureModality(mdmRequestDto);

	}

	public String getThresholdKeyByBioType(Modality modality) {
		if(modality == null)
			return RegistrationConstants.EMPTY;

		switch (modality) {
			case FINGERPRINT_SLAB_LEFT:
				return RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD;
			case FINGERPRINT_SLAB_RIGHT:
				return RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD;
			case FINGERPRINT_SLAB_THUMBS:
				return RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD;
			case IRIS_DOUBLE:
				return RegistrationConstants.IRIS_THRESHOLD;
			case FACE:
				return RegistrationConstants.FACE_THRESHOLD;
			case EXCEPTION_PHOTO:
				return RegistrationConstants.EMPTY;
		}
		return RegistrationConstants.EMPTY;
	}

	private boolean isApplicant(String subType) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Checking subType is whether applicant or not");
		return subType != null && subType.equalsIgnoreCase(RegistrationConstants.APPLICANT);
	}

	private boolean isExceptionPhoto(String modality) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Checking subType is whether exception photo or not");
		return modality != null && modality.equalsIgnoreCase(RegistrationConstants.EXCEPTION_PHOTO);
	}

	private AuditEvent getAuditEventForScan(String modality) {
		AuditEvent auditEvent = AuditEvent.REG_DOC_NEXT;
		if (modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)) {
			auditEvent = AuditEvent.REG_BIO_RIGHT_SLAP_SCAN;
		} else if (modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_LEFT)) {
			auditEvent = AuditEvent.REG_BIO_LEFT_SLAP_SCAN;
		} else if (modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)) {
			auditEvent = AuditEvent.REG_BIO_THUMBS_SCAN;
		} else if (modality.equalsIgnoreCase(RegistrationConstants.IRIS_DOUBLE)) {
			auditEvent = AuditEvent.REG_BIO_IRIS_SCAN;
		} else if (modality.equalsIgnoreCase(RegistrationConstants.FACE)) {
			auditEvent = AuditEvent.REG_BIO_FACE_CAPTURE;
		}
		return auditEvent;
	}

	private List<BiometricsDto> saveCapturedBiometricData(String subType, List<BiometricsDto> biometrics) {
		LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"saveCapturedBiometricData invoked size >> " + biometrics.size());
		//if (isUserOnboardFlag) {
		List<BiometricsDto> savedCaptures = new ArrayList<>();
		for (BiometricsDto value : biometrics) {
			LOGGER.debug(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"updating userOnboard biometric data >> " + value.getModalityName());
			savedCaptures.add(userOnboardService.addOperatorBiometrics(subType, value.getBioAttribute(), value));

		}
		return savedCaptures;
		/*} else {

			Map<String, BiometricsDto> biometricsMap = new LinkedHashMap<>();

			for (BiometricsDto biometricsDto : biometrics) {

				LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Adding registration biometric data >> " + biometricsDto.getBioAttribute());
				biometricsMap.put(biometricsDto.getBioAttribute(), biometricsDto);
			}

			return getRegistrationDTOFromSession().addAllBiometrics(subType, Modality.valueOf(currentModality), biometricsMap,
					getThresholdScoreInDouble(getThresholdKeyByBioType(Modality.valueOf(currentModality))),
					getMaxRetryByModality(currentModality));
		}*/
	}

	private void loadBiometricsUIElements(List<BiometricsDto> biometricDTOList, String subType, String modality) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating progress Bar,Text and attempts Box in UI");

		int retry = biometricDTOList.get(0).getNumOfRetries();

		double averageQualityScore = getAverageQualityScore(biometricDTOList);
		int thresholdScore = getThresholdScoreInInt(getThresholdKeyByBioType(Modality.valueOf(modality)));

		setCapturedValues(averageQualityScore, retry, thresholdScore);

		// Get the stream image from Bio ServiceImpl and load it in the image pane
		biometricImage.setImage(getBioStreamImage(subType, Modality.valueOf(modality), retry));

		boolean retryNeeded = averageQualityScore >= thresholdScore ? false : true;
		addImageInUIPane(subType, modality, getBioStreamImage(subType, Modality.valueOf(modality), retry), true, retryNeeded);
	}

	private double getAverageQualityScore(List<BiometricsDto> biometricDTOList) {

		double qualityScore = 0;
		if (biometricDTOList != null && !biometricDTOList.isEmpty()) {

			for (BiometricsDto biometricDTO : biometricDTOList) {
				qualityScore += biometricDTO.getQualityScore();
			}

			qualityScore = qualityScore / biometricDTOList.size();
		}
		return qualityScore;
	}

	private int getThresholdScoreInInt(String thresholdKey) {
		/* Get Configued threshold score for bio type */

		String thresholdScore = getValueFromApplicationContext(thresholdKey);

		return thresholdScore != null ? Integer.valueOf(thresholdScore) : 0;
	}

	private String getCaptureTimeOut() {

		/* Get Configued capture timeOut */
		return getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT);
	}

	/**
	 * Navigating to previous section
	 *
	 * @param event the event for navigating to previous section
	 */
	@FXML
	private void previous(ActionEvent event) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Navigates to previous section");

		if (currentPosition != 0) {
			disableLastCheckBoxSection();
			goToPrevious();
			return;
		}

	}

	/**
	 * Navigating to next section
	 *
	 * @param event the event for navigating to next section
	 */
	@FXML
	private void next(ActionEvent event) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Navigates to next section");

		if (currentPosition != sizeOfLeftGridPaneImageList - 1) {
			disableLastCheckBoxSection();
			goToNext();
			return;
		}

		if (isUserOnboardFlag) {
			userOnboardParentController.showCurrentPage(RegistrationConstants.GUARDIAN_BIOMETRIC,
					getOnboardPageDetails(RegistrationConstants.GUARDIAN_BIOMETRIC, RegistrationConstants.NEXT));
		}

		initializeState(false);

	}

	/**
	 * Updating biometrics
	 *
	 * @param bioType            biometric type
	 * @param bioImage           biometric image
	 * @param biometricThreshold threshold of biometric
	 * @param retryCount         retry count
	 */
	private void updateBiometric(String bioType, String bioImage, String biometricThreshold, String retryCount) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating biometrics and clearing previous data");

		try {
			biometricImage.setImage(getImage(bioImage,true));
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Exception while getting image",exception);
		}

		String threshold = null;
		if (biometricThreshold != null) {
			threshold = getValueFromApplicationContext(biometricThreshold);
		}

		double qualityScore = threshold == null || threshold.isEmpty() ? 0 : Double.parseDouble(threshold);

		thresholdScoreLabel.setText(getQualityScoreText(qualityScore));
		createQualityBox(retryCount, biometricThreshold);

		clearBioLabels();
		if (!isFace(currentModality)) {
			setScanButtonVisibility(isAllExceptions(getCheckBoxes(currentSubType, currentModality)), scanBtn);
		} else {
			setScanButtonVisibility(false, scanBtn);
		}

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updated biometrics and cleared previous data");
	}

	private void clearBioLabels() {
		clearCaptureData();
		biometricPane.getStyleClass().clear();
		biometricPane.getStyleClass().add(RegistrationConstants.BIOMETRIC_PANES_SELECTED);
		qualityScore.setText(RegistrationConstants.HYPHEN);
		attemptSlap.setText(RegistrationConstants.HYPHEN);
		// duplicateCheckLbl.setText(RegistrationConstants.EMPTY);

		retryBox.setVisible(true);
		biometricBox.setVisible(true);

		bioProgress.setProgress(0);
		qualityText.setText("");

	}

	/**
	 * Updating captured values
	 *
	 * @param qltyScore      Qulaity score
	 * @param retry          retrycount
	 * @param thresholdValue threshold value
	 */
	private void setCapturedValues(double qltyScore, int retry, double thresholdValue) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating captured values of biometrics");

		biometricPane.getStyleClass().clear();
		biometricPane.getStyleClass().add(RegistrationConstants.FINGERPRINT_PANES_SELECTED);
		qualityScore.setText(getQualityScoreText(qltyScore));
		attemptSlap.setText(String.valueOf(retry));

		bioProgress.setProgress(
				Double.valueOf(getQualityScoreText(qltyScore).split(RegistrationConstants.PERCENTAGE)[0]) / 100);
		qualityText.setText(getQualityScoreText(qltyScore));

		retry = retry == 0 ? 1 : retry;
		if (Double.valueOf(getQualityScoreText(qltyScore).split(RegistrationConstants.PERCENTAGE)[0]) >= thresholdValue) {
			clearAttemptsBox(RegistrationConstants.QUALITY_LABEL_GREEN, retry);
			bioProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_RED);
			bioProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_GREEN);
			qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_RED);
			qualityText.getStyleClass().add(RegistrationConstants.LABEL_GREEN);
		} else {
			clearAttemptsBox(RegistrationConstants.QUALITY_LABEL_RED, retry);
			bioProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_GREEN);
			bioProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_RED);
			qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_GREEN);
			qualityText.getStyleClass().add(RegistrationConstants.LABEL_RED);
		}

		if (retry == getMaxRetryByModality(currentModality)) {
			scanBtn.setDisable(true);
		} else {
			scanBtn.setDisable(false);
		}

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updated captured values of biometrics");
	}

	private int getMaxRetryByModality(String currentModality) {
		String key = getMaxRetryKeyByModality(currentModality);

		String val = getValueFromApplicationContext(key);
		return val != null ? Integer.parseInt(val) : 0;
	}

	private String getMaxRetryKeyByModality(String modality) {
		return modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_RIGHT)
				? RegistrationConstants.FINGERPRINT_RETRIES_COUNT
				: modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_LEFT)
				? RegistrationConstants.FINGERPRINT_RETRIES_COUNT
				: modality.equalsIgnoreCase(RegistrationConstants.FINGERPRINT_SLAB_THUMBS)
				? RegistrationConstants.FINGERPRINT_RETRIES_COUNT
				: modality.equalsIgnoreCase(RegistrationConstants.IRIS_DOUBLE)
				? RegistrationConstants.IRIS_RETRY_COUNT
				: modality.equalsIgnoreCase(RegistrationConstants.FACE)
				? RegistrationConstants.FACE_RETRY_COUNT
				: modality;
	}

	/**
	 * Updating captured values
	 *
	 * @param retryCount         retry count
	 * @param biometricThreshold threshold value
	 */
	private void createQualityBox(String retryCount, String biometricThreshold) {
		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updating Quality and threshold values of biometrics");

		final EventHandler<MouseEvent> mouseEventHandler = new EventHandler<MouseEvent>() {
			public void handle(final MouseEvent mouseEvent) {

				LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
						"Mouse Event by attempt Started");

				String eventString = mouseEvent.toString();
				int index = eventString.indexOf(RegistrationConstants.RETRY_ATTEMPT_ID);

				if (index == -1) {
					String text = "Text[text=";
					index = eventString.indexOf(text) + text.length() + 1;

				} else {
					index = index + RegistrationConstants.RETRY_ATTEMPT_ID.length();
				}
				try {

					// updateByAttempt(modality,
					// Character.getNumericValue(eventString.charAt(index)), biometricImage,
					// qualityText, bioProgress, qualityScore);

					int attempt = Character.getNumericValue(eventString.charAt(index));

					double qualityScoreVal = getBioScores(currentSubType, currentModality, attempt);
					if (qualityScoreVal != 0) {
						updateByAttempt(qualityScoreVal, getBioStreamImage(currentSubType, Modality.valueOf(currentModality), attempt),
								getThresholdScoreInInt(getThresholdKeyByBioType(Modality.valueOf(currentModality))), biometricImage,
								qualityText, bioProgress);
					}

					LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							"Mouse Event by attempt Ended. modality : " + currentModality);

				} catch (RuntimeException runtimeException) {
					LOGGER.error(LOG_REG_FINGERPRINT_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
							runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));

				}
			}

		};

		bioRetryBox.getChildren().clear();
		// if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER))
		// {

		String retryCountVal = getValueFromApplicationContext(retryCount);

		retryCountVal = retryCountVal == null || retryCountVal.isEmpty() ? "0" : retryCountVal;
		for (int retry = 0; retry < Integer.parseInt(retryCountVal); retry++) {
			Label label = new Label();
			label.getStyleClass().add(RegistrationConstants.QUALITY_LABEL_GREY);
			label.setId(RegistrationConstants.RETRY_ATTEMPT_ID + (retry + 1));
			label.setVisible(true);
			label.setText(String.valueOf(retry + 1));
			label.setAlignment(Pos.CENTER);

			bioRetryBox.getChildren().add(label);
		}
		bioRetryBox.setOnMouseClicked(mouseEventHandler);

		String threshold = biometricThreshold != null ? getValueFromApplicationContext(biometricThreshold) : "0";

		thresholdLabel.setAlignment(Pos.CENTER);

		double thresholdValDouble = threshold != null && !threshold.isEmpty() ? Double.parseDouble(threshold) : 0;
		thresholdLabel.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.THRESHOLD).concat("  ").concat(String.valueOf(thresholdValDouble))
				.concat(RegistrationConstants.PERCENTAGE));
		thresholdPane1.setPercentWidth(thresholdValDouble);
		thresholdPane2.setPercentWidth(100.00 - (thresholdValDouble));
		// }

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Updated Quality and threshold values of biometrics");

	}

	/**
	 * Clear attempts box.
	 *
	 * @param styleClass the style class
	 * @param retries    the retries
	 */
	private void clearAttemptsBox(String styleClass, int retries) {
		for (int retryBox = 1; retryBox <= retries; retryBox++) {

			Node node = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retryBox);

			if (node != null) {
				node.getStyleClass().clear();
				node.getStyleClass().add(styleClass);
			}
		}

		boolean nextRetryFound = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + ++retries) != null;
		while (nextRetryFound) {

			Node node = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retries);

			if (node != null) {
				node.getStyleClass().clear();
				node.getStyleClass().add(RegistrationConstants.QUALITY_LABEL_GREY);
			}
			nextRetryFound = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + ++retries) != null;
		}
	}

	public void clearRetryAttemptsBox() {

		int attempt = 1;
		boolean retryBoxFound = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + attempt) != null;

		while (retryBoxFound) {
			bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + attempt).getStyleClass().clear();
			bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + attempt).getStyleClass()
					.add(RegistrationConstants.QUALITY_LABEL_GREY);
			++attempt;
			retryBoxFound = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + attempt) != null;

		}
	}

	/**
	 * Clear captured data
	 *
	 */
	private void clearCaptureData() {

		clearUiElements();

	}

	private void clearUiElements() {

		retryBox.setVisible(false);
		biometricBox.setVisible(false);

	}

	/**
	 * Clear Biometric data
	 */
	public void clearCapturedBioData() {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Clearing the captured biometric data");

		// clearAllBiometrics();

		/*if (getRegistrationDTOFromSession() != null && (getRegistrationDTOFromSession().isUpdateUINChild()
				|| (SessionContext.map().get(RegistrationConstants.IS_Child) != null
						&& (Boolean) SessionContext.map().get(RegistrationConstants.IS_Child)))) {
			getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					.setFingerprintDetailsDTO(new ArrayList<>());

			getRegistrationDTOFromSession().getBiometricDTO().getIntroducerBiometricDTO()
					.setIrisDetailsDTO(new ArrayList<>());
		}*/
		// duplicateCheckLbl.setText(RegistrationConstants.EMPTY);
		clearCaptureData();
		biometricBox.setVisible(false);
		retryBox.setVisible(false);

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Cleared the captured biometric data");

	}

	/*private void addRegistrationStreamImage(String subType, String modality, int attempt, byte[] streamImage) {
		if (getRegistrationDTOFromSession() != null) {
			getRegistrationDTOFromSession().streamImages.put(String.format("%s_%s_%s", subType, modality, attempt),
					streamImage);
		}

	}*/

	public Image getBioStreamImage(String subType, Modality modality, int attempt) {
		List<byte[]> images = new LinkedList<>();
		for(String attribute : modality.getAttributes()) {
			byte[] image = STREAM_IMAGES.get(String.format("%s_%s_%s", subType,
					attribute, attempt));
			images.add(image);
		}

		try {
			if(images.stream().allMatch( b -> b == null))
				return getImage(getImageIconPath(modality.name()), false);

			switch (modality) {
				case FACE:
				case EXCEPTION_PHOTO:
					return images.get(0) == null ? getImage(getImageIconPath(modality.name()), false) :
							new Image(new ByteArrayInputStream(images.get(0)));
				case IRIS_DOUBLE:
				case FINGERPRINT_SLAB_THUMBS:
					return getImage(baseService.concatImages(images.get(0), images.get(1), getImagePath(RegistrationConstants.CROSS_IMG, true)));
				case FINGERPRINT_SLAB_LEFT:
				case FINGERPRINT_SLAB_RIGHT:
					return getImage(baseService.concatImages(images.get(0), images.get(1), images.get(2), images.get(3), getImagePath(RegistrationConstants.CROSS_IMG, true)));
			}
		} catch (RegBaseCheckedException e) {
			LOGGER.error("Failed to get stream image for modality "+ modality, e);
		}
		return null;
	}

	public void refreshContinueButton() {
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "refreshContinueButton invoked");

		if (getListOfBiometricSubTypes().isEmpty()) {

			LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
					RegistrationConstants.APPLICATION_NAME,
					"refreshContinueButton NON of the BIOMETRIC FIELD IS ENABLED");
			return;
		}

		String currentSubType = getListOfBiometricSubTypes().get(currentPosition);

		List<String> bioAttributes = currentMap.get(currentSubType);

		List<String> leftHandBioAttributes = getContainsAllElements(RegistrationConstants.leftHandUiAttributes,
				bioAttributes);
		List<String> rightHandBioAttributes = getContainsAllElements(RegistrationConstants.rightHandUiAttributes,
				bioAttributes);
		List<String> twoThumbsBioAttributes = getContainsAllElements(RegistrationConstants.twoThumbsUiAttributes,
				bioAttributes);
		List<String> faceBioAttributes = getContainsAllElements(RegistrationConstants.faceUiAttributes, bioAttributes);
		List<String> irisBioAttributes = getContainsAllElements(RegistrationConstants.eyesUiAttributes, bioAttributes);

		String operator = AND_OPERATOR;
		boolean considerExceptionAsCaptured = OR_OPERATOR.equals(operator) ? false : true;

		Map<String, Boolean> capturedDetails = new HashMap<String, Boolean>();
		capturedDetails = setCapturedDetailsMap(capturedDetails, leftHandBioAttributes,
				isBiometricsCaptured(currentSubType, leftHandBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.LEFTSLAP_FINGERPRINT_THRESHOLD),
						considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, rightHandBioAttributes,
				isBiometricsCaptured(currentSubType, rightHandBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.RIGHTSLAP_FINGERPRINT_THRESHOLD),
						considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, twoThumbsBioAttributes,
				isBiometricsCaptured(currentSubType, twoThumbsBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.THUMBS_FINGERPRINT_THRESHOLD),
						considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, irisBioAttributes,
				isBiometricsCaptured(currentSubType, irisBioAttributes,
						getThresholdScoreInInt(RegistrationConstants.IRIS_THRESHOLD), considerExceptionAsCaptured));
		capturedDetails = setCapturedDetailsMap(capturedDetails, faceBioAttributes, isBiometricsCaptured(currentSubType,
				faceBioAttributes, getThresholdScoreInInt(RegistrationConstants.FACE), considerExceptionAsCaptured));

		String expression = String.join(operator, bioAttributes);
		boolean result = MVEL.evalToBoolean(expression, capturedDetails);

		if (result && considerExceptionAsCaptured) {

			if (hasApplicantBiometricException()) {

				result = getRegistrationDTOFromSession().getDocuments().containsKey("proofOfException");

			}

		}
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "capturedDetails >> " + capturedDetails);
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,

				RegistrationConstants.APPLICATION_NAME, "Expression >> " + expression + " :: result >> " + result);
		if (result) {
			auditFactory.audit(AuditEvent.REG_BIO_CAPTURE_NEXT, Components.REG_BIOMETRICS, SessionContext.userId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		}
		continueBtn.setDisable(result ? false : true);
	}

	private boolean hasApplicantBiometricException() {
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME, "Checking whether applicant has biometric exceptions");

		boolean hasApplicantBiometricException = false;
		if (getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getBiometricExceptions() != null
				&& !getRegistrationDTOFromSession().getBiometricExceptions().isEmpty()) {
			for (Entry<String, BiometricsException> bs : getRegistrationDTOFromSession().getBiometricExceptions()
					.entrySet()) {
				if (isApplicant(bs.getValue().getIndividualType())) {

					LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton",
							RegistrationConstants.APPLICATION_ID, RegistrationConstants.APPLICATION_NAME,
							"Applicant biometric exception found");

					hasApplicantBiometricException = true;
					break;
				}
			}
		}

		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME,
				"Completed checking whether applicant has biometric exceptions : " + hasApplicantBiometricException);

		return hasApplicantBiometricException;
	}


	private Map<String, Boolean> setCapturedDetailsMap(Map<String, Boolean> capturedDetails, List<String> bioAttributes,
													   boolean isBiometricsCaptured) {

		for (String bioAttribute : bioAttributes) {
			capturedDetails.put(bioAttribute, isBiometricsCaptured);
		}
		return capturedDetails;
	}

	private boolean isBiometricsCaptured(String subType, List<String> bioAttributes, int thresholdScore,
										 boolean considerExceptionAsCaptured) {
		// if no bio attributes then it is not configured to be captured
		if (bioAttributes == null || bioAttributes.isEmpty())
			return false;

		boolean isCaptured = false, isForceCaptured = false;
		int qualityScore = 0, exceptionBioCount = 0;

		for (String bioAttribute : bioAttributes) {

			BiometricsDto biometricDTO = getBiometrics(subType, bioAttribute);
			if (biometricDTO != null) {
				/* Captures check */
				qualityScore += biometricDTO.getQualityScore();
				isCaptured = true;
				isForceCaptured = biometricDTO.isForceCaptured();

			} else if (isBiometricExceptionAvailable(subType, bioAttribute)) {
				/* Exception bio check */
				isCaptured = true;
				++exceptionBioCount;

			} else {
				isCaptured = false;
				break;
			}
		}

		if (isCaptured && !isForceCaptured) {
			if (bioAttributes.size() == exceptionBioCount)
				isCaptured = considerExceptionAsCaptured ? true : false;
			else
				isCaptured = (qualityScore / (bioAttributes.size() - exceptionBioCount)) >= thresholdScore;
		}
		LOGGER.debug("REGISTRATION - BIOMETRICS - refreshContinueButton", RegistrationConstants.APPLICATION_ID,
				RegistrationConstants.APPLICATION_NAME,
				"isBiometricsCaptured invoked  subType >> " + subType + " bioAttributes >> " + bioAttributes
						+ " exceptionBioCount >> " + exceptionBioCount + " isCaptured >> " + isCaptured);
		return isCaptured;
	}

	private BiometricsDto getBiometrics(String subType, String bioAttribute) {
		//if (isUserOnboardFlag) {
		List<BiometricsDto> list = userOnboardService.getBiometrics(subType, Arrays.asList(bioAttribute));
		return !list.isEmpty() ? list.get(0) : null;
		/*} else
			return getRegistrationDTOFromSession().getBiometric(subType, bioAttribute);*/
	}

	private List<BiometricsDto> getBiometrics(String subType, List<String> bioAttribute) {
		//if (isUserOnboardFlag) {
		List<BiometricsDto> list = userOnboardService.getBiometrics(subType, bioAttribute);
		return list;
		/*} else
			return getRegistrationDTOFromSession().getBiometric(subType, bioAttribute);*/
	}

	private boolean isBiometricExceptionAvailable(String subType, String bioAttribute) {
		//if (isUserOnboardFlag)
		return userOnboardService.isBiometricException(subType, bioAttribute);
		/*else
			return getRegistrationDTOFromSession().isBiometricExceptionAvailable(subType, bioAttribute);*/
	}

	public void addBioScores(String subType, String modality, String attempt, double qualityScore) {

		BIO_SCORES.put(String.format("%s_%s_%s", subType, modality, attempt), qualityScore);
	}

	public double getBioScores(String subType, String modality, int attempt) {

		double qualityScore = 0.0;
		try {
			qualityScore = BIO_SCORES.get(String.format("%s_%s_%s", subType, modality, attempt));
		} catch (NullPointerException nullPointerException) {
			LOGGER.error(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(nullPointerException));

		}

		return qualityScore;
	}

	public void clearBioCaptureInfo() {

		BIO_SCORES.clear();
		STREAM_IMAGES.clear();
	}

	private List<String> getListOfBiometricSubTypes() {
		return new ArrayList<String>(currentMap.keySet());
	}

	private boolean identifyInLocalGallery(List<BiometricsDto> biometrics, String modality) {
		if(RegistrationConstants.DISABLE.equalsIgnoreCase((String) ApplicationContext.map()
				.getOrDefault(RegistrationConstants.DEDUPLICATION_ENABLE_FLAG, RegistrationConstants.DISABLE))) {
			LOGGER.info("DEDUPLICATION_ENABLE_FLAG disabled, hence returning false by default");
			return false;
		}

		BiometricType biometricType = BiometricType.fromValue(modality);
		Map<String, List<BIR>> gallery = new HashMap<>();
		List<UserBiometric> userBiometrics = userDetailDAO.findAllActiveUsersExceptCurrentUser(biometricType.value(),
				SessionContext.userContext().getUserId());
		if (userBiometrics.isEmpty())
			return false;

		userBiometrics.forEach(userBiometric -> {
			String userId = userBiometric.getUserBiometricId().getUsrId();
			gallery.computeIfAbsent(userId, k -> new ArrayList<BIR>())
					.add(buildBir(userBiometric.getBioIsoImage(), biometricType));
		});

		List<BIR> sample = new ArrayList<>(biometrics.size());
		biometrics.forEach(biometricDto -> {
			sample.add(buildBir(biometricDto.getAttributeISO(), biometricType));
		});

		try {
			Map<String, Boolean> result = bioAPIFactory.getBioProvider(biometricType, BiometricFunction.MATCH)
					.identify(sample, gallery, biometricType, null);
			return result.entrySet().stream().anyMatch(e -> e.getValue() == true);
		} catch (BiometricException e) {
			LOGGER.error("Failed to dedupe check >> ", e);
		}
		return false;
	}

	private BIR buildBir(byte[] biometricImageISO, BiometricType modality) {
		return new BIRBuilder().withBdb(biometricImageISO)
				.withBdbInfo(new BDBInfo.BDBInfoBuilder().withFormat(new RegistryIDType())
						.withType(Collections.singletonList(modality))
						.withPurpose(PurposeType.IDENTIFY).build())
				.build();
	}

	private VBox getImageVBox(String modality, String subtype, List<String> configBioAttributes) {

		VBox vBox = new VBox();

		vBox.setAlignment(Pos.BASELINE_LEFT);
		vBox.setId(modality);

		// Create Label with modality
		// Label label = new Label();
		// label.setText(applicationLabelBundle.getString(modality));
		// vBox.getChildren().add(label);

		HBox hBox = new HBox();
		// hBox.setAlignment(Pos.BOTTOM_RIGHT);

		// Image modalityImage = getImage(modality);
		List<BiometricsDto> biometricsDtos = getBiometrics(currentSubType, configBioAttributes);

		Image image = null;
		if (biometricsDtos != null && !biometricsDtos.isEmpty()) {
			image = getBioStreamImage(subtype, Modality.valueOf(modality), biometricsDtos.get(0).getNumOfRetries());
		}

		try {
			ImageView imageView = new ImageView(
					image != null ? image :getImage(getImageIconPath(modality),true));
			imageView.setFitHeight(80);
			imageView.setFitWidth(85);
			Tooltip tooltip = new Tooltip(applicationLabelBundle.getString(modality));
			tooltip.getStyleClass().add(RegistrationConstants.TOOLTIP_STYLE);
			// Tooltip.install(hBox, tooltip);
			hBox.setOnMouseEntered(event -> tooltip.show(hBox, event.getScreenX(), event.getScreenY() + 15));
			hBox.setOnMouseExited(event -> tooltip.hide());
			hBox.getChildren().add(imageView);

			boolean isAllExceptions = true;
			for (String configBioAttribute : configBioAttributes) {

				isAllExceptions = isBiometricExceptionAvailable(currentSubType, configBioAttribute) ? isAllExceptions
						: false;

				if (!isAllExceptions) {
					break;
				}
			}
			if (image != null || isAllExceptions) {
				if (hBox.getChildren().size() == 1) {

					String imageName = isAllExceptions ? RegistrationConstants.EXCLAMATION_IMG : RegistrationConstants.TICK_CIRICLE_IMG;

					ImageView tickImageView = new ImageView(getImage(imageName,true));
					tickImageView.setFitWidth(40);
					tickImageView.setFitHeight(40);
					hBox.getChildren().add(tickImageView);
				}
			}

			vBox.getChildren().add(hBox);

			// vBox.getChildren().add(imageView);

			vBox.setOnMouseClicked((event) -> {
				displayBiometric(vBox.getId());
			});

			vBox.setFillWidth(true);
			vBox.setMinWidth(100);

			// vBox.setMinHeight(100);
			vBox.getStyleClass().add(RegistrationConstants.BIOMETRICS_DISPLAY);
			// vBox.setBorder(new Border(
			// new BorderStroke(Color.PINK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
			// BorderWidths.FULL)));
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Error while getting image");
		}

		return vBox;
	}

	private void addImageInUIPane(String subType, String modality, Image uiImage, boolean isCaptured, boolean retryNeeded) {
		for (GridPane gridPane : leftHandImageBoxMap.values()) {
			if (gridPane.getId().equals(subType)) {

				for (Node node : gridPane.getChildren()) {

					if (node.getId().equalsIgnoreCase(modality)) {
						VBox vBox = (VBox) node;
						HBox hBox = (HBox) vBox.getChildren().get(0);
						// hBox.getChildren().clear();
						try {
							((ImageView) (hBox.getChildren().get(0))).setImage(uiImage != null ? uiImage
									: getImage(getImageIconPath(modality),true));
						} catch (RegBaseCheckedException e) {
							LOGGER.error("Error While getting image");
						}

						if (isCaptured) {
							if (hBox.getChildren().size() > 1) {
								hBox.getChildren().remove(1);
							}
							String imageName = (uiImage == null || retryNeeded) ? RegistrationConstants.EXCLAMATION_IMG : RegistrationConstants.TICK_CIRICLE_IMG;

							try {
								ImageView imageView = new ImageView(getImage(imageName,true));
								imageView.setFitWidth(40);
								imageView.setFitHeight(40);
								hBox.getChildren().add(imageView);
							} catch (RegBaseCheckedException e) {
								LOGGER.error("Error While getting image");
							}
						} else {

							if (hBox.getChildren().size() > 1) {
								hBox.getChildren().remove(1);
							}
						}

					}
				}

			}
		}

	}

	private GridPane getGridPane(Entry<String, String> subMapKey) {

		GridPane gridPane = new GridPane();

		gridPane.setId(subMapKey.getKey());
		Label label = new Label(subMapKey.getValue());
		label.getStyleClass().add("paneHeader");

		leftPanelImageGridPane.add(gridPane, 1, 1);

		leftHandImageBoxMap.put(subMapKey.getKey(), gridPane);

		disableGridPane(gridPane);

		return gridPane;

	}

	private Pane getExceptionImagePane(String modality, List<String> configBioAttributes,
									   List<String> nonConfigBioAttributes, String subType) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Getting exception image pane for modality : " + modality);

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Getting exception image pane for modality from BiometricsExceptionController: " + modality);

		Pane exceptionImagePane = getExceptionImagePane(modality);

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Completed of getting exception image pane");

		if (exceptionImagePane != null) {
			addExceptionsUiPane(exceptionImagePane, configBioAttributes, nonConfigBioAttributes, modality, subType);

			exceptionImagePane.setVisible(true);
			exceptionImagePane.setManaged(true);
		}
		return exceptionImagePane;

	}

	public void updateBiometricData(ImageView clickedImageView, List<ImageView> bioExceptionImagesForSameModality) {
		clearBIOCache(currentSubType, clickedImageView.getId());
		if (clickedImageView.getOpacity() == 0) {
			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Marking exceptions for biometrics");

			auditFactory.audit(AuditEvent.REG_BIO_EXCEPTION_MARKING, Components.REG_BIOMETRICS, SessionContext.userId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			clickedImageView.setOpacity(1);
			userOnboardService.addOperatorBiometricException(currentSubType, clickedImageView.getId());
		} else {
			LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Removing exceptions for biometrics");

			auditFactory.audit(AuditEvent.REG_BIO_EXCEPTION_REMOVING, Components.REG_BIOMETRICS,
					SessionContext.userId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			clickedImageView.setOpacity(0);
			if (isUserOnboardFlag)
				userOnboardService.removeOperatorBiometricException(currentSubType, clickedImageView.getId());
		}

		if (hasApplicantBiometricException()) {
			setBiometricExceptionVBox(true);
		} else {
			if (getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getDocuments() != null) {
				getRegistrationDTOFromSession().getDocuments().remove("proofOfException");
			}
			addImageInUIPane(RegistrationConstants.APPLICANT, RegistrationConstants.EXCEPTION_PHOTO, null, false, false);
			setBiometricExceptionVBox(false);
		}

		boolean isAllMarked = true;
		for (ImageView exceptionImageView : bioExceptionImagesForSameModality) {
			if (isUserOnboardFlag)
				userOnboardService.removeOperatorBiometrics(currentSubType, exceptionImageView.getId());
			if (isAllMarked) {
				isAllMarked = isAllMarked && exceptionImageView.getOpacity() == 1 ? true : false;
			}
		}

		displayBiometric(currentModality);
		addImageInUIPane(currentSubType, currentModality, null, isAllMarked, false);
		setScanButtonVisibility(isAllMarked, scanBtn);
		refreshContinueButton();
	}

	private void clearBIOCache(String subType, String bioAttribute) {
		Modality modality = Modality.getModality(bioAttribute);
		List<String> keys = new ArrayList<>();
		keys.addAll(STREAM_IMAGES.keySet());
		keys.addAll(BIO_SCORES.keySet());

		for(String attr : modality.getAttributes()) {
			keys.stream()
					.filter( k -> k.startsWith(String.format("%s_%s", subType, attr)))
					.forEach( k -> {
						STREAM_IMAGES.remove(k);
						BIO_SCORES.remove(k);
					});
		}
	}

	private void setBiometricExceptionVBox(boolean visible) {
		if (exceptionVBox != null) {
			// exceptionVBox.setVisible(disable);
			exceptionVBox.setVisible(visible);

		}

	}

	private void addExceptionsUiPane(Pane pane, List<String> configBioAttributes, List<String> nonConfigBioAttributes,
									 String modality, String subType) {

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"started adding exception images in ui pane");

		if (pane != null) {
			List<Node> nodes = pane.getChildren();

			if (nodes != null && !nodes.isEmpty()) {
				for (Node node : nodes) {

					if (configBioAttributes.contains(node.getId())) {
						LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								"Not marked exception image : " + node.getId() + " as default");

						node.setVisible(true);
						node.setDisable(false);
						node.setOpacity(isBiometricExceptionAvailable(subType, node.getId()) ? 1 : 0);

					}
					if (nonConfigBioAttributes.contains(node.getId())) {
						LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
								"Marked exception image : " + node.getId() + " as default");

						node.setVisible(true);
						node.setDisable(true);
						node.setOpacity(1.0);

						//if (isUserOnboardFlag)
						userOnboardService.addOperatorBiometricException(currentSubType, node.getId());
						/*else
							getRegistrationDTOFromSession().addBiometricException(currentSubType, node.getId(),
									node.getId(), "Temporary", "Temporary");*/
					}
				}
			}
		}

		LOGGER.info(LOG_REG_BIOMETRIC_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Completed adding exception images in ui pane");

	}

	private Pane getExceptionImagePane(String modality) {

		Pane exceptionImagePane = null;

		if (isExceptionPhoto(modality)) {
			return null;
		}

		if (modality != null) {
			switch (modality) {

				case RegistrationConstants.FACE:
					exceptionImagePane = null;
					break;
				case RegistrationConstants.IRIS_DOUBLE:
					exceptionImagePane = getTwoIrisSlabExceptionPane(modality);
					break;

				case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
					exceptionImagePane = getRightSlabExceptionPane(modality);
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
					exceptionImagePane = getLeftSlabExceptionPane(modality);
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
					exceptionImagePane = getTwoThumbsSlabExceptionPane(modality);
					break;

			}
		}

		return exceptionImagePane;
	}

	private Pane getLeftSlabExceptionPane(String modality) {
		LOGGER.debug("Preparing Left Slab Exception Image ");

		Pane pane = new Pane();
		pane.setId(modality);
		pane.setPrefHeight(200);
		pane.setPrefWidth(200);

		ImageView topImageView = getImageView(null, RegistrationConstants.LEFTPALM_IMG, 144, 163, 6, 6, true, true,
				false);

		// Left Middle

		ImageView leftMiddleImageView = getImageView("leftMiddle", RegistrationConstants.LEFTMIDDLE_IMG, 92, 27,
				70, 41, true, true, true);
		ImageView leftIndexImageView = getImageView("leftIndex", RegistrationConstants.LEFTINDEX_IMG, 75, 28, 97,
				55, true, true, true);
		ImageView leftRingImageView = getImageView("leftRing", RegistrationConstants.LEFTRING_IMG, 75, 28, 45, 55,
				true, true, true);
		ImageView leftLittleImageView = getImageView("leftLittle", RegistrationConstants.LEFTLITTLE_IMG, 49, 26,
				19, 82, true, true, true);

		pane.getChildren().add(topImageView);
		pane.getChildren().add(leftMiddleImageView);
		pane.getChildren().add(leftIndexImageView);
		pane.getChildren().add(leftRingImageView);
		pane.getChildren().add(leftLittleImageView);

		LOGGER.debug("Completed Preparing Left Slap Exception Image");

		return pane;
	}

	private Pane getRightSlabExceptionPane(String modality) {
		LOGGER.debug("Preparing Right Slab Exception Image ");
		Pane pane = new Pane();
		pane.setId(modality);
		pane.setPrefHeight(200);
		pane.setPrefWidth(200);

		ImageView topImageView = getImageView(null, RegistrationConstants.RIGHTPALM_IMG, 144, 163, 3, 4, true,
				true, false);

		// Left Middle

		ImageView middleImageView = getImageView("rightMiddle", RegistrationConstants.LEFTMIDDLE_IMG, 92, 30, 72,
				37, true, true, true);
		ImageView ringImageView = getImageView("rightRing", RegistrationConstants.LEFTRING_IMG, 82, 27, 99, 54,
				true, true, true);
		ImageView indexImageView = getImageView("rightIndex", RegistrationConstants.LEFTINDEX_IMG, 75, 30, 46, 54,
				true, true, true);

		ImageView littleImageView = getImageView("rightLittle", RegistrationConstants.LEFTLITTLE_IMG, 57, 28, 125,
				75, true, true, true);

		pane.getChildren().add(topImageView);
		pane.getChildren().add(middleImageView);
		pane.getChildren().add(ringImageView);
		pane.getChildren().add(indexImageView);
		pane.getChildren().add(littleImageView);
		LOGGER.debug("Completed Preparing Right Slab Exception Image ");

		return pane;
	}

	private Pane getTwoThumbsSlabExceptionPane(String modality) {
		LOGGER.info("Preparing Two Thumbs Exception Image ");
		Pane pane = new Pane();
		pane.setId(modality);
		pane.setPrefHeight(200);
		pane.setPrefWidth(200);

		ImageView topImageView = getImageView(null, RegistrationConstants.THUMB_IMG, 144, 171, 18, 22, true, true,
				false);

		ImageView left = getImageView("leftThumb", RegistrationConstants.LEFTTHUMB_IMG, 92, 30, 60, 46, true, true,
				true);
		ImageView right = getImageView("rightThumb", RegistrationConstants.LEFTTHUMB_IMG, 99, 30, 118, 46, true,
				true, true);

		pane.getChildren().add(topImageView);
		pane.getChildren().add(left);
		pane.getChildren().add(right);
		LOGGER.info("Completed Preparing Two Thumbs Exception Image ");
		return pane;
	}

	private Pane getTwoIrisSlabExceptionPane(String modality) {
		LOGGER.debug("Preparing Two Iris Exception Image ");
		Pane pane = new Pane();
		pane.setId(modality);
		pane.setPrefHeight(200);
		pane.setPrefWidth(200);
		ImageView topImageView = getImageView(null, RegistrationConstants.DOUBLE_IRIS_WITH_INDICATORS_IMG, 144, 189.0, 7, 4, true,
				true, false);

		ImageView rightImageView = getImageView("rightEye", RegistrationConstants.RIGHTEYE_IMG, 43, 48, 125, 54,
				true, true, true);
		ImageView leftImageView = getImageView("leftEye", RegistrationConstants.LEFTEYE_IMG, 43, 48, 35, 54, true,
				true, true);

		pane.getChildren().add(topImageView);
		pane.getChildren().add(rightImageView);
		pane.getChildren().add(leftImageView);

		LOGGER.debug("Completed Preparing Two Iris Exception Image ");
		return pane;
	}

	private ImageView getImageView(String id, String imageName, double fitHeight, double fitWidth, double layoutX,
								   double layoutY, boolean pickOnBounds, boolean preserveRatio, boolean hasActionEvent) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Started Preparing exception image view for : " + id);

		ImageView imageView = null;
		try {
			imageView = new ImageView(getImage(imageName,true));
			if (id != null) {
				imageView.setId(id);
			}
			imageView.setFitHeight(fitHeight);
			imageView.setFitWidth(fitWidth);
			imageView.setLayoutX(layoutX);
			imageView.setLayoutY(layoutY);
			imageView.setPickOnBounds(pickOnBounds);
			imageView.setPreserveRatio(preserveRatio);

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Is Action required : " + hasActionEvent);

			if (hasActionEvent) {
				imageView.setOnMouseClicked((event) -> {

					LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
							"Action event triggered on click of exception image");
					addException(event);
				});

			}

			LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
					"Completed Preparing exception image view for : " + id);
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Exception while getting image",exception);
		}


		return imageView;

	}


	public void addException(MouseEvent event) {

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Clicked on exception Image");

		ImageView exceptionImage = (ImageView) event.getSource();

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"Clicked on exception Image : " + exceptionImage.getId());

		Pane pane = (Pane) exceptionImage.getParent();

		List<ImageView> paneExceptionBioAttributes = new LinkedList<>();
		for (Node node : pane.getChildren()) {
			if (node instanceof ImageView && node.getId() != null && !node.getId().isEmpty()) {

				paneExceptionBioAttributes.add((ImageView) node);
			}
		}
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID,
				"All exception images for same modality" + paneExceptionBioAttributes);

		updateBiometricData(exceptionImage, paneExceptionBioAttributes);

		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Add or remove exception completed");

	}


}