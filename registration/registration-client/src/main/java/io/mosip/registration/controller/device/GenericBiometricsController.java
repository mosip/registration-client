package io.mosip.registration.controller.device;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.face.FaceDecoder;
import io.mosip.biometrics.util.finger.FingerDecoder;
import io.mosip.biometrics.util.iris.IrisDecoder;
import io.mosip.registration.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

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
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.GenericController;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.dao.UserDetailDAO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.entity.UserBiometric;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.mdm.dto.Biometric;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.enums.Modality;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.util.control.impl.BiometricFxControl;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * {@code GenericBiometricsController} is to capture and display the captured
 * biometrics of Guardian
 *
 * @author Sravya Surampalli
 * @since 1.0
 */
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Controller
public class GenericBiometricsController extends BaseController {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(GenericBiometricsController.class);

	@Value("${mosip.registration.biometric.stage.width:1200}")
	private int width;

	@Value("${mosip.registration.biometric.stage.height:620}")
	private int height;

	@FXML
	private GridPane biometricBox;

	@FXML
	private GridPane retryBox;

	/*@FXML
	private ComboBox<BiometricAttributeDto> biometricTypecombo;*/

	@FXML
	private ImageView biometricImage;

	@FXML
	private Label qualityScore;

	@FXML
	private Label attemptSlap;

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

/*	@FXML
	private Button continueBtn;*/

	@FXML
	private Label guardianBiometricsLabel;

	/*@FXML
	private ImageView backImageView;*/
	
	@FXML
	private ImageView scanImageView;	
	@FXML
	private ImageView closeButtonImageView;
	
	/*public Label getGuardianBiometricsLabel() {
		return guardianBiometricsLabel;
	}

	public void setGuardianBiometricsLabel(Label guardianBiometricsLabel) {
		this.guardianBiometricsLabel = guardianBiometricsLabel;
	}*/

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

	/** The registration controller. */
	@Autowired
	private RegistrationController registrationController;

	/** The iris facade. */
	@Autowired
	private BioService bioService;

	@Autowired
	private BaseService baseService;

	private String bioValue;

	private FXUtils fxUtils;

	public ImageView getBiometricImage() {
		return biometricImage;
	}

	public Button getScanBtn() {
		return scanBtn;
	}

	/*public Button getContinueBtn() {
		return continueBtn;
	}

	public Label getPhotoAlert() {
		return photoAlert;
	}

	public GridPane getBiometricPane() {
		return biometricPane;
	}*/

	private String currentSubType;

	@FXML
	private GridPane checkBoxPane;

	private ResourceBundle applicationLabelBundle;

	private Modality currentModality;

	@Autowired
	private MosipDeviceSpecificationFactory deviceSpecificationFactory;

	/*@FXML
	private GridPane backButton;

	@FXML
	private GridPane gheaderfooter;*/

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

	@Autowired
	private DocumentScanController documentScanController;

	@Autowired
	private GenericController genericController;

	/*@Autowired
	private UserOnboardService userOnboardService;

	@Autowired
	private DocScannerFacade docScannerFacade;*/

	private Service<List<BiometricsDto>> rCaptureTaskService;

	private Service<MdmBioDevice> deviceSearchTask;

	public Modality getCurrentModality() {
		return currentModality;
	}

	private Node exceptionVBox;

	private BiometricFxControl fxControl;

	private List<String> configBioAttributes;

	private List<String> nonConfigBioAttributes;

	private VBox exceptionImgVBox;

	private Stage biometricPopUpStage;

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

	/*
	 * (non-Javadoc)
	 *
	 * @see javafx.fxml.Initializable#initialize(java.net.URL,
	 * java.util.ResourceBundle)
	 */
	@FXML
	public void initialize() {
		LOGGER.info("Loading of Guardian Biometric screen started");
		applicationLabelBundle = applicationContext.getBundle(applicationContext.getApplicationLanguage(),
				RegistrationConstants.LABELS);
		setImage(scanImageView	, RegistrationConstants.SCAN_IMG);
		setImage(closeButtonImageView	, RegistrationConstants.CLOSE_IMG);
	}

	public boolean isBiometricExceptionProofCollected() {
		boolean flag = false;
		if(getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getDocuments() != null) {
			flag = getRegistrationDTOFromSession().getDocuments()
					.values()
					.stream()
					.anyMatch(doc -> doc.getCategory().equalsIgnoreCase(RegistrationConstants.POE_DOCUMENT));
		}
		LOGGER.debug("Is Biometric proof of exception collected ? {}", flag);
		return flag;
	}

	private void setScanButtonVisibility(boolean isAllExceptions) {
		scanBtn.setDisable(isAllExceptions);
	}

	/**
	 * Displays biometrics
	 *
	 * @param modality the modality for displaying biometrics
	 */
	private void displayBiometric(Modality modality) {
		LOGGER.info("Displaying biometrics to capture for {}", modality);

		applicationLabelBundle = applicationContext.getBundle(applicationContext.getApplicationLanguage(), RegistrationConstants.LABELS);

		retryBox.setVisible(!isExceptionPhoto(modality));
		thresholdBox.setVisible(!isExceptionPhoto(modality));
		biometricBox.setVisible(true);
		checkBoxPane.getChildren().clear();

		// get List of captured Biometrics based on nonExceptionBio Attributes
		List<BiometricsDto> capturedBiometrics = null;
		List<String> nonExceptionBioAttributes = isFace(modality) ? RegistrationConstants.faceUiAttributes : null;
		if (!isFace(modality) && !isExceptionPhoto(modality)) {
			setExceptionImg();

			List<Node> checkBoxNodes = exceptionImgVBox.getChildren();

			List<String> exceptionBioAttributes = null;

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
								nonExceptionBioAttributes = nonExceptionBioAttributes != null
										? nonExceptionBioAttributes
										: new LinkedList<String>();
								nonExceptionBioAttributes.add(bioAttribute);
							}
						}
					}
				}
			}
		}

		if (nonExceptionBioAttributes != null) {
			capturedBiometrics = getBiometrics(currentSubType, nonExceptionBioAttributes);
		}

		updateBiometric(modality, getImageIconPath(modality), bioService.getMDMQualityThreshold(modality),
				bioService.getRetryCount(modality));

		loadBiometricsUIElements(currentSubType, modality);

		if(capturedBiometrics != null && !capturedBiometrics.isEmpty())
			fxControl.refreshModalityButton(modality);

		LOGGER.info("{} Biometrics captured", currentSubType);
	}

	private void setExceptionImg() {
		exceptionImgVBox = new VBox();
		exceptionImgVBox.setSpacing(5);
		Label checkBoxTitle = new Label();
		checkBoxTitle.setText(applicationLabelBundle.getString("exceptionCheckBoxPaneLabel"));
		exceptionImgVBox.setAlignment(Pos.CENTER);
		exceptionImgVBox.getChildren().addAll(checkBoxTitle);
		checkBoxTitle.getStyleClass().add("demoGraphicFieldLabel");

		exceptionImgVBox.getChildren().add(
				getExceptionImagePane(currentModality, configBioAttributes, nonConfigBioAttributes, currentSubType));

		exceptionImgVBox.setVisible(true);
		exceptionImgVBox.setManaged(true);

		checkBoxPane.add(exceptionImgVBox, 0, 0);
	}


	public String getImageIconPath(Modality modality) {
		if(modality == null)
			return null;

		String imageIconPath = null;
		switch (modality) {
			case FACE:
				imageIconPath = RegistrationConstants.FACE_IMG;
				break;
			case IRIS_DOUBLE:
				imageIconPath = RegistrationConstants.DOUBLE_IRIS_IMG;
				break;
			case FINGERPRINT_SLAB_RIGHT:
				imageIconPath = RegistrationConstants.RIGHTPALM_IMG;
				break;
			case FINGERPRINT_SLAB_LEFT:
				imageIconPath = RegistrationConstants.LEFTPALM_IMG;
				break;
			case FINGERPRINT_SLAB_THUMBS:
				imageIconPath = RegistrationConstants.THUMB_IMG;
				break;
			case EXCEPTION_PHOTO:
				imageIconPath = RegistrationConstants.DEFAULT_EXCEPTION_IMG;
				break;
		}
		return imageIconPath;
	}


	/**
	 * This method will allow to scan and upload documents
	 */
	@Override
	public void scan(Stage popupStage) {
		if (isExceptionPhoto(currentModality)) {
			try {
				byte[] byteArray = documentScanController.captureAndConvertBufferedImage();

				saveProofOfExceptionDocument(byteArray);
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS));

			} catch (Exception exception) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR));
				LOGGER.error("Error while capturing exception photo : ", exception);

			}

		}
	}

	private void saveProofOfExceptionDocument(byte[] byteArray) {
		DocumentDto documentDto = new DocumentDto();
		documentDto.setDocument(byteArray);
		documentDto.setType("EOP");
		documentDto.setFormat(RegistrationConstants.SCANNER_IMG_TYPE);
		documentDto.setCategory(RegistrationConstants.POE_DOCUMENT);
		documentDto.setOwner(RegistrationConstants.APPLICANT);
		documentDto.setValue(documentDto.getCategory().concat(RegistrationConstants.UNDER_SCORE).concat(documentDto.getType()));

		Optional<FxControl> result = GenericController.getFxControlMap().values().stream()
				.filter(control -> control.getUiSchemaDTO().getSubType().equals(RegistrationConstants.POE_DOCUMENT)).findFirst();

		if(result.isPresent()) {
			getRegistrationDTOFromSession().addDocument(result.get().getUiSchemaDTO().getId(), documentDto);
			LOGGER.info("Saving Proof of exception document into field : {}", result.get().getUiSchemaDTO().getId());
		}
	}

	public void deleteProofOfExceptionDocument() {
		Optional<FxControl> result = GenericController.getFxControlMap().values().stream()
				.filter(control -> control.getUiSchemaDTO().getSubType().equals(RegistrationConstants.POE_DOCUMENT)).findFirst();

		if(result.isPresent()) {
			getRegistrationDTOFromSession().removeDocument(result.get().getUiSchemaDTO().getId());
			LOGGER.info("Removing Proof of exception document into field : {}", result.get().getUiSchemaDTO().getId());
		}
	}

	/**
	 * Scan the biometrics
	 *
	 * @param event the event for scanning biometrics
	 */
	@FXML
	private void scan(ActionEvent event) {
		LOGGER.info("Displaying Scan popup for capturing biometrics");

		auditFactory.audit(getAuditEventForScan(currentModality.name()), Components.REG_BIOMETRICS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

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

						LOGGER.info("device search request started {}", System.currentTimeMillis());
						
						String modality = isFace(currentModality) || isExceptionPhoto(currentModality) ?
								 RegistrationConstants.FACE_FULLFACE : currentModality.name();
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

		deviceSearchTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				MdmBioDevice mdmBioDevice = deviceSearchTask.getValue();
				try {
					// Disable Auto-Logout
					SessionContext.setAutoLogout(false);

					if (mdmBioDevice == null) {
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND));
						return;
					}

					InputStream urlStream = bioService.getStream(mdmBioDevice,
							isFace(currentModality) ? RegistrationConstants.FACE_FULLFACE : currentModality.name());

					boolean isStreamStarted = urlStream != null && urlStream.read() != -1;
					if (!isStreamStarted) {
						LOGGER.info("URL Stream was null at : {} ", System.currentTimeMillis());
						deviceSpecificationFactory.init();
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.STREAMING_ERROR));
						return;
					}
					rCaptureTaskService();
					streamer.startStream(urlStream, biometricImage, biometricImage);

				} catch (RegBaseCheckedException | IOException exception) {

					LOGGER.error("Error while streaming : " + currentModality,  exception);
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.STREAMING_ERROR));

					// Enable Auto-Logout
					SessionContext.setAutoLogout(true);
				}
			}
		});

		deviceSearchTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND));
			}
		});

	}

	private boolean isFace(Modality currentModality) {
		return currentModality.equals(Modality.FACE);
	}

	private List<String> getSelectedExceptionsByBioType(String subType, Modality modality)
			throws RegBaseCheckedException {
		List<String> selectedExceptions = new LinkedList<String>();

		// get vbox holding label and exception marking Image
		if(checkBoxPane.getChildren().size() > 0) {
			Pane pane = (Pane) checkBoxPane.getChildren().get(0);
			pane = (Pane) pane.getChildren().get(1);
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


	public void rCaptureTaskService() {
		LOGGER.debug("Capture request called at : {}", System.currentTimeMillis());

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

						LOGGER.info("Capture request started {}", System.currentTimeMillis());
						return rCapture(currentSubType, currentModality);

					}
				};
			}
		};
		rCaptureTaskService.start();

		rCaptureTaskService.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				LOGGER.debug("RCapture task failed");
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR));

				LOGGER.debug("closing popup stage");
				//scanPopUpViewController.getPopupStage().close();

				LOGGER.debug("Enabling LogOut");
				// Enable Auto-Logout
				SessionContext.setAutoLogout(true);

				LOGGER.debug("Setting URL Stream as null");
				streamer.setUrlStream(null);
			}
		});

		rCaptureTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {

				LOGGER.debug("RCapture task was successful");
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
									"FACE_FULL FACE" : currentModality.name()).value())) {
						LOGGER.info("*** Local DE_DUPE validation -- found local match !! ");
						generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.LOCAL_DEDUP_CHECK_FAILED));
						return;
					}

					if (isExceptionPhoto(currentModality)) {
						LOGGER.info("started Saving Exception photo captured using MDS");
						ConvertRequestDto convertRequestDto = new ConvertRequestDto();
						convertRequestDto.setVersion("ISO19794_5_2011");
						convertRequestDto.setInputBytes(mdsCapturedBiometricsList.get(0).getAttributeISO());
						saveProofOfExceptionDocument(FaceDecoder.convertFaceISOToImageBytes(convertRequestDto));
						generateAlert(RegistrationConstants.ALERT_INFORMATION,
								RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS));
						fxControl.refreshModalityButton(currentModality);
						return;
					}

					getRegistrationDTOFromSession().ATTEMPTS.put(String.format("%s_%s", currentSubType, currentModality),
							getRegistrationDTOFromSession().ATTEMPTS.getOrDefault(String.format("%s_%s", currentSubType, currentModality), 0) + 1);
					List<String> exceptionBioAttributes = getSelectedExceptionsByBioType(currentSubType, currentModality);
					Map<String, BiometricsDto> biometricsMap = new LinkedHashMap<>();
					for (BiometricsDto biometricsDto : mdsCapturedBiometricsList) {
						if (exceptionBioAttributes.contains(biometricsDto.getBioAttribute())) {
							LOGGER.debug("As bio atrribute marked as exception, not storing into registration DTO : {}", biometricsDto.getBioAttribute());
							continue;
						}
						LOGGER.info("Adding registration biometric data >> {}", biometricsDto.getBioAttribute());
						biometricsDto.setSubType(currentSubType);
						biometricsDto.setNumOfRetries(getRegistrationDTOFromSession().ATTEMPTS.get(String.format("%s_%s", currentSubType, currentModality)));
						biometricsMap.put(biometricsDto.getBioAttribute(), biometricsDto);
					}
					fxControl.setData(biometricsMap);
					LOGGER.debug("Completed Saving filtered biometrics into registration DTO");
					addStreamImageAndScoreToCache(currentSubType, currentModality, biometricsMap.values(),
							getRegistrationDTOFromSession().ATTEMPTS.get(String.format("%s_%s", currentSubType, currentModality)));
					displayBiometric(currentModality);
					// if all the above check success show alert capture success
					generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS));
				} catch (Exception e) {
					LOGGER.error("Exception while getting the scanned biometrics for user registration",e);
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR));
				}
				SessionContext.setAutoLogout(true);	// Enable Auto-Logout
				streamer.setUrlStream(null);
			}
		});
		LOGGER.info("Scan process ended for capturing biometrics");
	}

	//TODO - onMissing attribute , pls use default image / blank image
	private void addStreamImageAndScoreToCache(String subType, Modality modalityName, Collection<BiometricsDto> biometricsDtos, int retry) throws Exception {
		try {
			double score = 0;
			switch (modalityName) {
				case FINGERPRINT_SLAB_LEFT:
				case FINGERPRINT_SLAB_RIGHT:
				case FINGERPRINT_SLAB_THUMBS:

					for(BiometricsDto dto : biometricsDtos) {
						ConvertRequestDto convertRequestDto = new ConvertRequestDto();
						convertRequestDto.setVersion("ISO19794_4_2011");
						convertRequestDto.setInputBytes(dto.getAttributeISO());
						getRegistrationDTOFromSession().BIO_CAPTURES.put(String.format("%s_%s_%s",
								subType, dto.getBioAttribute(), retry),
								FingerDecoder.convertFingerISOToImageBytes(convertRequestDto));
						score += dto.getQualityScore();
					}
					getRegistrationDTOFromSession().BIO_SCORES.put(String.format("%s_%s_%s",
							subType, modalityName.name(), retry),
							score / biometricsDtos.size());
					break;
				case IRIS_DOUBLE:
					for(BiometricsDto dto : biometricsDtos) {
						ConvertRequestDto convertRequestDto = new ConvertRequestDto();
						convertRequestDto.setVersion("ISO19794_6_2011");
						convertRequestDto.setInputBytes(dto.getAttributeISO());
						getRegistrationDTOFromSession().BIO_CAPTURES.put(String.format("%s_%s_%s",
								subType, dto.getBioAttribute(), retry),
								IrisDecoder.convertIrisISOToImageBytes(convertRequestDto));
						score += dto.getQualityScore();
					}
					getRegistrationDTOFromSession().BIO_SCORES.put(String.format("%s_%s_%s",
							subType, modalityName.name(), retry),
							score / biometricsDtos.size());
					break;

				case FACE:
					BiometricsDto faceDto = biometricsDtos.toArray(new BiometricsDto[0])[0];
					ConvertRequestDto convertRequestDto = new ConvertRequestDto();
					convertRequestDto.setVersion("ISO19794_5_2011");
					convertRequestDto.setInputBytes(faceDto.getAttributeISO());
					getRegistrationDTOFromSession().BIO_CAPTURES.put(String.format("%s_%s_%s",
							subType, modalityName.getAttributes().get(0), retry),
							FaceDecoder.convertFaceISOToImageBytes(convertRequestDto));
					getRegistrationDTOFromSession().BIO_SCORES.put(String.format("%s_%s_%s",
							subType, modalityName.name(), retry),
							faceDto.getQualityScore());
					break;
			}
		} catch (Exception exception) {
			LOGGER.error("Failed to extract image from ISO", exception);
			throw exception;
		}
	}

	private boolean isValidBiometric(List<BiometricsDto> mdsCapturedBiometricsList) {
		LOGGER.info("Validating captured biometrics");

		boolean isValid = mdsCapturedBiometricsList != null && !mdsCapturedBiometricsList.isEmpty();

		if (isValid) {
			for (BiometricsDto biometricsDto : mdsCapturedBiometricsList) {
				if (biometricsDto.getBioAttribute() == null
						|| biometricsDto.getBioAttribute().equalsIgnoreCase(RegistrationConstants.JOB_UNKNOWN)) {
					LOGGER.error("Unknown bio attribute identified in captured biometrics");
					isValid = false;
					break;
				}
			}
		}
		return isValid;
	}

	private List<BiometricsDto> rCapture(String subType, Modality modality) throws RegBaseCheckedException {
		LOGGER.info("Finding exception bio attributes");
		List<String> exceptionBioAttributes = new LinkedList<>();
		int count = 1;
		//if its exception photo, then we need to send all the exceptions that is marked to MDS
		//its the information provided to MDS
		if (isExceptionPhoto(modality)) {
			for (Entry<String, BiometricsException> bs : getRegistrationDTOFromSession().getBiometricExceptions()
					.entrySet()) {
				if (isApplicant(bs.getValue().getIndividualType())) {
					exceptionBioAttributes.add(bs.getValue().getMissingBiometric());
				}
			}
		}
		else {
			exceptionBioAttributes = getSelectedExceptionsByBioType(currentSubType, currentModality);
			count = modality.getAttributes().size() - exceptionBioAttributes.size();
		}

		MDMRequestDto mdmRequestDto = new MDMRequestDto(
				isFace(modality) || isExceptionPhoto(modality) ? RegistrationConstants.FACE_FULLFACE : modality.name(),
				exceptionBioAttributes.toArray(new String[0]), "Registration",
				io.mosip.registration.context.ApplicationContext.getStringValueFromApplicationMap(
						RegistrationConstants.SERVER_ACTIVE_PROFILE),
				Integer.valueOf(getCaptureTimeOut()), count, (int) bioService.getMDMQualityThreshold(modality));
		return bioService.captureModality(mdmRequestDto);

	}

	public boolean isApplicant(String subType) {
		boolean flag = subType != null && subType.equalsIgnoreCase(RegistrationConstants.APPLICANT);
		LOGGER.debug("checking isApplicant({}) ? {}", subType, flag);
		return flag;
	}

	public boolean isExceptionPhoto(Modality modality) {
		return modality != null && modality.equals(Modality.EXCEPTION_PHOTO);
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


	private void loadBiometricsUIElements(String subType, Modality modality) {
		LOGGER.debug("Updating progress Bar,Text and attempts Box in UI");

		int retry = getRegistrationDTOFromSession().ATTEMPTS.getOrDefault(String.format("%s_%s", subType, modality.name()), 0);

		setCapturedValues(getRegistrationDTOFromSession().BIO_SCORES.getOrDefault(String.format("%s_%s_%s",
				subType, modality.name(), retry), 0.0), retry, bioService.getMDMQualityThreshold(modality));

		// Get the stream image from Bio ServiceImpl and load it in the image pane
		biometricImage.setImage(getBioStreamImage(subType, modality, retry));
	}


	private String getCaptureTimeOut() {
		/* Get Configued capture timeOut */
		return getValueFromApplicationContext(RegistrationConstants.CAPTURE_TIME_OUT);
	}

	/**
	 * Updating biometrics
	 *
	 * @param bioType            biometric type
	 * @param bioImage           biometric image
	 * @param biometricThreshold threshold of biometric
	 * @param retryCount         retry count
	 */
	private void updateBiometric(Modality bioType, String bioImage, double biometricThreshold, int retryCount) {
		LOGGER.info("Updating biometrics and clearing previous data");

		bioValue = bioType.name();
		try {
			biometricImage.setImage(getImage(bioImage,true));
		} catch (RegBaseCheckedException e) {
			LOGGER.error("Error while getting image");
		}

		thresholdScoreLabel.setText(getQualityScoreText(biometricThreshold));
		createQualityBox(retryCount, biometricThreshold);

		clearBioLabels();
		setScanButtonVisibility((!isFace(currentModality) && !isExceptionPhoto(currentModality)) ?
				fxControl.isAllExceptions(currentModality) : false);

		LOGGER.info("Updated biometrics and cleared previous data");
	}

	private void clearBioLabels() {
		clearCaptureData();
		biometricPane.getStyleClass().clear();
		biometricPane.getStyleClass().add(RegistrationConstants.BIOMETRIC_PANES_SELECTED);
		qualityScore.setText(RegistrationConstants.HYPHEN);
		attemptSlap.setText(RegistrationConstants.HYPHEN);
		// duplicateCheckLbl.setText(RegistrationConstants.EMPTY);

		retryBox.setVisible(!isExceptionPhoto(currentModality));
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

		LOGGER.info("Updating captured values of biometrics");

		biometricPane.getStyleClass().clear();
		biometricPane.getStyleClass().add(RegistrationConstants.FINGERPRINT_PANES_SELECTED);
		qualityScore.setText(getQualityScoreText(qltyScore));
		attemptSlap.setText(String.valueOf(retry));

		bioProgress.setProgress(
				Double.valueOf(getQualityScoreText(qltyScore).split(RegistrationConstants.PERCENTAGE)[0]) / 100);
		qualityText.setText(getQualityScoreText(qltyScore));

		retry = retry == 0 ? 1 : retry;
		clearAttemptsBox(thresholdValue, retry);

		if (Double.valueOf(getQualityScoreText(qltyScore).split(RegistrationConstants.PERCENTAGE)[0]) >= thresholdValue) {
			bioProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_RED);
			bioProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_GREEN);
			qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_RED);
			qualityText.getStyleClass().add(RegistrationConstants.LABEL_GREEN);
		} else {
			bioProgress.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_GREEN);
			bioProgress.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_RED);
			qualityText.getStyleClass().removeAll(RegistrationConstants.LABEL_GREEN);
			qualityText.getStyleClass().add(RegistrationConstants.LABEL_RED);
		}

		scanBtn.setDisable(retry == bioService.getRetryCount(currentModality));
		LOGGER.info("Updated captured values of biometrics");
	}


	/**
	 * Updating captured values
	 *
	 * @param retryCount         retry count
	 * @param biometricThreshold threshold value
	 */
	private void createQualityBox(int retryCount, double biometricThreshold) {
		LOGGER.info("Updating Quality and threshold values of biometrics");

		final EventHandler<MouseEvent> mouseEventHandler = new EventHandler<MouseEvent>() {
			public void handle(final MouseEvent mouseEvent) {

				LOGGER.info("Mouse Event by attempt Started");

				String eventString = mouseEvent.toString();
				int index = eventString.indexOf(RegistrationConstants.RETRY_ATTEMPT_ID);

				if (index == -1) {
					String text = "Text[text=";
					index = eventString.indexOf(text) + text.length() + 1;

				} else {
					index = index + RegistrationConstants.RETRY_ATTEMPT_ID.length();
				}
				try {

					int attempt = Character.getNumericValue(eventString.charAt(index));
					for(Node node : bioRetryBox.getChildren()) {
						node.getStyleClass().removeAll(RegistrationConstants.QUALITY_LABEL_BLUE_BORDER);
					}
					Node node = bioRetryBox.getChildren().get(attempt - 1);
					node.getStyleClass().add(RegistrationConstants.QUALITY_LABEL_BLUE_BORDER);

					double qualityScoreVal = getBioScores(currentSubType, currentModality, attempt);
					if (qualityScoreVal != 0) {
						updateByAttempt(qualityScoreVal, getBioStreamImage(currentSubType, currentModality, attempt),
								bioService.getMDMQualityThreshold(currentModality), biometricImage,
								qualityText, bioProgress, qualityScore);
					}

					LOGGER.info("Mouse Event by attempt Ended. modality : {}", currentModality);

				} catch (RuntimeException runtimeException) {
					LOGGER.error("Error updating Quality and threshold values",runtimeException);
				}
			}
		};

		bioRetryBox.getChildren().clear();
		for (int retry = 0; retry < retryCount; retry++) {
			Label label = new Label();
			label.getStyleClass().add(RegistrationConstants.QUALITY_LABEL_GREY);
			label.setId(RegistrationConstants.RETRY_ATTEMPT_ID + (retry + 1));
			label.setVisible(true);
			label.setText(String.valueOf(retry + 1));
			label.setAlignment(Pos.CENTER);
			bioRetryBox.getChildren().add(label);
		}
		bioRetryBox.setOnMouseClicked(mouseEventHandler);
		thresholdLabel.setAlignment(Pos.CENTER);

		String langCode = ApplicationContext.applicationLanguage();
		if (getRegistrationDTOFromSession() != null && getRegistrationDTOFromSession().getSelectedLanguagesByApplicant() != null) {
			langCode = getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().get(0);
		}
		thresholdLabel.setText(applicationContext.getBundle(langCode, RegistrationConstants.LABELS).getString("threshold").concat("  ").concat(String.valueOf(biometricThreshold))
				.concat(RegistrationConstants.PERCENTAGE));
		thresholdPane1.setPercentWidth(biometricThreshold);
		thresholdPane2.setPercentWidth(100.00 - (biometricThreshold));
		LOGGER.info("Updated Quality and threshold values of biometrics");
	}

	/**
	 * Clear attempts box.
	 * @param threshold
	 * @param retries
	 */
	private void clearAttemptsBox(double threshold, int retries) {
		for (int retryBox = 1; retryBox <= retries; retryBox++) {

			Node node = bioRetryBox.lookup(RegistrationConstants.RETRY_ATTEMPT + retryBox);

			if (node != null) {
				node.getStyleClass().clear();
				node.getStyleClass().add(getBioScores(currentSubType, currentModality, retryBox) >= threshold ?
						RegistrationConstants.QUALITY_LABEL_GREEN :
						getRegistrationDTOFromSession().ATTEMPTS.getOrDefault(String.format("%s_%s", currentSubType, currentModality), 0) == 0 ?
								RegistrationConstants.QUALITY_LABEL_GREY : RegistrationConstants.QUALITY_LABEL_RED);
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

	/**
	 * Clear captured data
	 *
	 */
	private void clearCaptureData() {

		clearUiElements();

	}

	private void clearUiElements() {

		retryBox.setVisible(!isExceptionPhoto(currentModality));
		biometricBox.setVisible(false);

	}


	public Image getBioStreamImage(String subType, Modality modality, int attempt) {
		List<byte[]> images = new LinkedList<>();
		for(String attribute : modality.getAttributes()) {
			byte[] image = getRegistrationDTOFromSession().BIO_CAPTURES.get(String.format("%s_%s_%s", subType,
					attribute, attempt));
			images.add(image);
		}

		try {
			if(isExceptionPhoto(modality)) {
				Image exceptionImage = fxControl.getExceptionDocumentAsImage();
				return exceptionImage!=null?exceptionImage:getImage(fxControl.getImageIconPath(modality.name()), false);
			}

			if(images.stream().allMatch( b -> b == null))
				return getImage(fxControl.getImageIconPath(modality.name()), false);

			switch (modality) {
				case FACE:
				case EXCEPTION_PHOTO:
					return images.get(0) == null ? getImage(fxControl.getImageIconPath(modality.name()), false) :
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


	public boolean hasApplicantBiometricException() {
		LOGGER.debug("Checking whether applicant has biometric exceptions");

		boolean hasApplicantBiometricException = false;
		if (getRegistrationDTOFromSession() != null
				&& getRegistrationDTOFromSession().getBiometricExceptions() != null) {

			hasApplicantBiometricException = getRegistrationDTOFromSession().getBiometricExceptions().values()
					.stream()
					.anyMatch( be -> isApplicant(be.getIndividualType()));
		}

		LOGGER.debug("Completed checking whether applicant has biometric exceptions : {}" ,hasApplicantBiometricException);
		return hasApplicantBiometricException;
	}

	public List<BiometricsDto> getBiometrics(String subType, List<String> bioAttribute) {
		return getRegistrationDTOFromSession().getBiometric(subType, bioAttribute);
	}

	public boolean isBiometricExceptionAvailable(String subType, String bioAttribute) {
		return getRegistrationDTOFromSession().isBiometricExceptionAvailable(subType, bioAttribute);
	}

	public double getBioScores(String subType, Modality modality, int attempt) {
		double qualityScore = 0.0;
		try {
			qualityScore = getRegistrationDTOFromSession().BIO_SCORES.getOrDefault(String.format("%s_%s_%s", subType, modality, attempt),qualityScore);
		} catch (NullPointerException nullPointerException) {
			LOGGER.error("Error getting bioscore", nullPointerException);
		}
		return qualityScore;
	}

	private boolean identifyInLocalGallery(List<BiometricsDto> biometrics, String modality) {
		BiometricType biometricType = BiometricType.fromValue(modality);
		Map<String, List<BIR>> gallery = new HashMap<>();
		List<UserBiometric> userBiometrics = userDetailDAO.findAllActiveUsers(biometricType.value());
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
			LOGGER.error("Failed dedupe check >> ",e);
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


	private Pane getExceptionImagePane(Modality modality, List<String> configBioAttributes,
			List<String> nonConfigBioAttributes, String subType) {
		LOGGER.info("Getting exception image pane for modality : {}", modality);

		Pane exceptionImagePane = getExceptionImagePane(modality);

		if (exceptionImagePane != null) {
			addExceptionsUiPane(exceptionImagePane, configBioAttributes, nonConfigBioAttributes, modality, subType);

			exceptionImagePane.setVisible(true);
			exceptionImagePane.setManaged(true);
		}
		return exceptionImagePane;

	}

	public void updateBiometricData(ImageView clickedImageView, List<ImageView> bioExceptionImagesForSameModality) {
		//clearing all stored data for this modality, as seen changes in selection
		getRegistrationDTOFromSession().clearBIOCache(currentSubType, clickedImageView.getId());
		auditFactory.audit((clickedImageView.getOpacity() == 0) ? AuditEvent.REG_BIO_EXCEPTION_MARKING :
						AuditEvent.REG_BIO_EXCEPTION_REMOVING, Components.REG_BIOMETRICS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		if (clickedImageView.getOpacity() == 0) {
			LOGGER.info("Marking exceptions for biometrics {}",  clickedImageView.getId());
			clickedImageView.setOpacity(1);
		}else {
			LOGGER.info("Unmarking exceptions for biometrics {}",  clickedImageView.getId());
			clickedImageView.setOpacity(0);
		}

		boolean isAllMarked = true;
		for (ImageView exceptionImageView : bioExceptionImagesForSameModality) {
			if(exceptionImageView.getOpacity() == 1) {
				getRegistrationDTOFromSession().addBiometricException(currentSubType, exceptionImageView.getId(),
						exceptionImageView.getId(), "Temporary", "Temporary");
			}
			isAllMarked = isAllMarked && exceptionImageView.getOpacity() == 1 ? true : false;
		}

		displayBiometric(currentModality);
		setScanButtonVisibility(isAllMarked);
		fxControl.refreshModalityButton(currentModality);
	}


	private void addExceptionsUiPane(Pane pane, List<String> configBioAttributes, List<String> nonConfigBioAttributes,
			Modality modality, String subType) {

		if(pane == null || pane.getChildren() == null) {
			LOGGER.debug("Nothing to add in exception images ui pane");
			return;
		}

		LOGGER.debug("started adding exception images in ui pane");
		for (Node node : pane.getChildren()) {

			if (configBioAttributes.contains(node.getId())) {
				LOGGER.info("Not marked exception image : {} as default", node.getId());

				node.setVisible(true);
				node.setDisable(false);
				node.setOpacity(isBiometricExceptionAvailable(subType, node.getId()) ? 1 : 0);

			}
			if (nonConfigBioAttributes.contains(node.getId())) {
				LOGGER.info("Marked exception image : {} as default", node.getId());

				node.setVisible(true);
				node.setDisable(true);
				node.setOpacity(1.0);

				getRegistrationDTOFromSession().addBiometricException(currentSubType, node.getId(),
						node.getId(), "Temporary", "Temporary");
			}
		}
		LOGGER.debug("Completed adding exception images in ui pane");
	}


	private Pane getExceptionImagePane(Modality modality) {
		Pane exceptionImagePane = null;
		if(modality == null)
			return exceptionImagePane;

		switch (modality) {
			case FACE:
				exceptionImagePane = null;
				break;
			case IRIS_DOUBLE:
				exceptionImagePane = getTwoIrisSlabExceptionPane(modality);
				break;
			case FINGERPRINT_SLAB_RIGHT:
				exceptionImagePane = getRightSlabExceptionPane(modality);
				break;
			case FINGERPRINT_SLAB_LEFT:
				exceptionImagePane = getLeftSlabExceptionPane(modality);
				break;
			case FINGERPRINT_SLAB_THUMBS:
				exceptionImagePane = getTwoThumbsSlabExceptionPane(modality);
				break;
			case EXCEPTION_PHOTO:
				exceptionImagePane = null;
				break;
		}
		return exceptionImagePane;
	}

	private Pane getLeftSlabExceptionPane(Modality modality) {
		LOGGER.debug("Preparing Left Slab Exception Image ");

		Pane pane = new Pane();
		pane.setId(modality.name());
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

	private Pane getRightSlabExceptionPane(Modality modality) {
		LOGGER.debug("Preparing Right Slab Exception Image ");
		Pane pane = new Pane();
		pane.setId(modality.name());
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

	private Pane getTwoThumbsSlabExceptionPane(Modality modality) {
		LOGGER.info("Preparing Two Thumbs Exception Image ");
		Pane pane = new Pane();
		pane.setId(modality.name());
		pane.setPrefHeight(200);
		pane.setPrefWidth(200);

		ImageView topImageView = getImageView(null, RegistrationConstants.THUMB_IMG, 144, 171, 18, 22, true, true,
				false);

		ImageView left = getImageView("leftThumb", RegistrationConstants.LEFTTHUMB_IMG, 92, 30, 55, 37, true, true,
				true);
		ImageView right = getImageView("rightThumb", RegistrationConstants.LEFTTHUMB_IMG, 99, 30, 123, 38, true,
				true, true);

		pane.getChildren().add(topImageView);
		pane.getChildren().add(left);
		pane.getChildren().add(right);
		LOGGER.info("Completed Preparing Two Thumbs Exception Image ");
		return pane;
	}

	private Pane getTwoIrisSlabExceptionPane(Modality modality) {
		LOGGER.debug("Preparing Two Iris Exception Image ");
		Pane pane = new Pane();
		pane.setId(modality.name());
		pane.setPrefHeight(200);
		pane.setPrefWidth(200);
		ImageView topImageView = getImageView(null, RegistrationConstants.DOUBLE_IRIS_IMG, 144, 189.0, 7, 4, true,
				true, false);

		ImageView rightImageView = getImageView("rightEye", RegistrationConstants.RIGHTEYE_IMG, 43, 48, 118, 54,
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

		LOGGER.info("Started Preparing exception image view for : {}", id);

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

			LOGGER.info("Is Action required : {}", hasActionEvent);

			if (hasActionEvent) {
				imageView.setOnMouseClicked((event) -> {

					LOGGER.info("Action event triggered on click of exception image");
					addException(event);
				});

			}

			LOGGER.info("Completed Preparing exception image view for : {}",id);
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Exception while getting image",exception);
		}

		
		return imageView;

	}

	public void addException(MouseEvent event) {

		ImageView exceptionImage = (ImageView) event.getSource();

		LOGGER.info("Clicked on exception Image : {}", exceptionImage.getId());

		Pane pane = (Pane) exceptionImage.getParent();

		List<ImageView> paneExceptionBioAttributes = new LinkedList<>();
		for (Node node : pane.getChildren()) {
			if (node instanceof ImageView && node.getId() != null && !node.getId().isEmpty()) {

				paneExceptionBioAttributes.add((ImageView) node);
			}
		}
		LOGGER.info("All exception images for same modality {}", paneExceptionBioAttributes);

		updateBiometricData(exceptionImage, paneExceptionBioAttributes);

		LOGGER.info("Add or remove exception completed");

	}

	public void init(FxControl fxControl, String subType, Modality modality, List<String> configBioAttributes,
			List<String> nonConfigBioAttributes) throws IOException {

		this.fxControl = (BiometricFxControl) fxControl;
		this.currentSubType = subType;
		this.currentModality = modality;
		this.configBioAttributes = configBioAttributes;
		this.nonConfigBioAttributes = nonConfigBioAttributes;

		biometricPopUpStage = new Stage();
		biometricPopUpStage.initStyle(StageStyle.UNDECORATED);
		biometricPopUpStage.setResizable(false);
		Parent scanPopup = BaseController.load(getClass().getResource(RegistrationConstants.BIOMETRIC_FXML));

		Scene scene = new Scene(scanPopup, width, height);
		scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
		biometricPopUpStage.setScene(scene);
		biometricPopUpStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
		biometricPopUpStage.initOwner(fXComponents.getStage());
		biometricPopUpStage.show();

		displayBiometric(modality);
	}

	public void initializeWithoutStage(FxControl fxControl, String subType, Modality modality, List<String> configBioAttributes,
					 List<String> nonConfigBioAttributes) {

		this.scanBtn.setId(subType == null ? "ScanBtn" : subType+"ScanBtn");
		this.fxControl = (BiometricFxControl) fxControl;
		this.currentSubType = subType;
		this.currentModality = modality;
		this.configBioAttributes = configBioAttributes;
		this.nonConfigBioAttributes = nonConfigBioAttributes;
		displayBiometric(modality);
	}

	/**
	 * event class to exit from present pop up window.
	 * 
	 * @param event
	 */
	public void exitWindow(ActionEvent event) {

		LOGGER.info("Calling exit window to close the popup");

		biometricPopUpStage.close();

		LOGGER.info("Popup is closed");

	}

}
