package io.mosip.registration.controller;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.device.BiometricsController;
import io.mosip.registration.controller.device.ScanPopUpViewController;
import io.mosip.registration.controller.eodapproval.RegistrationApprovalController;
import io.mosip.registration.controller.reg.AlertController;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.controller.reg.HeaderController;
import io.mosip.registration.controller.reg.HomeController;
import io.mosip.registration.controller.reg.PacketHandlerController;
import io.mosip.registration.controller.reg.RegistrationPreviewController;
import io.mosip.registration.controller.reg.UserOnboardParentController;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.schema.ProcessSpecDto;
import io.mosip.registration.dto.schema.SchemaDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RemapException;
import io.mosip.registration.mdm.dto.MDMRequestDto;
import io.mosip.registration.scheduler.SchedulerUtil;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.service.remap.CenterMachineReMapService;
import io.mosip.registration.service.security.AuthenticationService;
import io.mosip.registration.service.sync.SyncStatusValidatorService;
import io.mosip.registration.util.common.PageFlow;
import io.mosip.registration.util.restclient.AuthTokenUtilService;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;
import javafx.animation.PauseTransition;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;


/**
 * Base class for all controllers.
 *
 * @author Sravya Surampalli
 * @since 1.0.0
 */

@Component
public class BaseController {

	private static final Logger LOGGER = AppConfig.getLogger(BaseController.class);
	private static final String ALERT_STAGE = "alertStage";
	private static final String TEMPLATE = "/%s/%s";

	@FXML
	public Text scanningMsg;

	@Autowired
	private SyncStatusValidatorService syncStatusValidatorService;
	@Autowired
	protected AuditManagerService auditFactory;
	@Autowired
	protected GlobalParamService globalParamService;

	@Autowired
	protected ServiceDelegateUtil serviceDelegateUtil;

	@Autowired
	protected FXComponents fXComponents;

	@Autowired
	public RegistrationPreviewController registrationPreviewController;

	@Autowired
	private BiometricsController guardianBiometricsController;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private UserOnboardService userOnboardService;

	@Autowired
	private CenterMachineReMapService centerMachineReMapService;

	@Autowired
	private PacketHandlerController packetHandlerController;

	@Autowired
	private HeaderController headerController;

	@Autowired
	private HomeController homeController;

	@Autowired
	private AlertController alertController;

	@Autowired
	private ScanPopUpViewController scanPopUpViewController;

	@Autowired
	private RegistrationApprovalController registrationApprovalController;

	@Autowired
	protected PageFlow pageFlow;

	@Autowired
	private UserOnboardParentController userOnboardParentController;

	@Autowired
	private RestartController restartController;

	@Autowired
	private IdentitySchemaService identitySchemaService;

	@Autowired
	protected BaseService baseService;

	@Autowired
	private AuthTokenUtilService authTokenUtilService;

	@Autowired
	private BioService bioService;

	@Autowired
	private DocumentScanController documentScanController;

	protected ApplicationContext applicationContext = ApplicationContext.getInstance();

	public Text getScanningMsg() {
		return scanningMsg;
	}

	public void setScanningMsg(String message) {
		scanningMsg.setText(message);
	}

	protected Scene scene;

	private List<String> pageDetails = new ArrayList<>();

	private Stage alertStage;

	private static boolean isAckOpened = false;

	private static TreeMap<String, String> mapOfbiometricSubtypes = new TreeMap<>();

	public static TreeMap<String, String> getMapOfbiometricSubtypes() {
		return mapOfbiometricSubtypes;
	}

	private static HashMap<String, String> labelMap = new HashMap<>();

	public static String getFromLabelMap(String key) {
		return labelMap.get(key);
	}

	public static void putIntoLabelMap(String key, String value) {
		labelMap.put(key, value);
	}

	private static List<String> ALL_BIO_ATTRIBUTES = null;

	@Value("${mosip.registration.images.theme:}")
	private String imagesTheme;

	@Value("${mosip.registration.css.theme:}")
	private String cssTheme;

	static {
		ALL_BIO_ATTRIBUTES = new ArrayList<String>();
		ALL_BIO_ATTRIBUTES.addAll(RegistrationConstants.leftHandUiAttributes);
		ALL_BIO_ATTRIBUTES.addAll(RegistrationConstants.rightHandUiAttributes);
		ALL_BIO_ATTRIBUTES.addAll(RegistrationConstants.twoThumbsUiAttributes);
		ALL_BIO_ATTRIBUTES.addAll(RegistrationConstants.eyesUiAttributes);
		ALL_BIO_ATTRIBUTES.add(RegistrationConstants.FACE_EXCEPTION);
	}

	/**
	 * @return the alertStage
	 */
	public Stage getAlertStage() {
		return alertStage;
	}

	/**
	 * Adding events to the stage.
	 *
	 * @return the stage
	 */
	protected Stage getStage() {
		EventHandler<Event> event = new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				SchedulerUtil.setCurrentTimeToStartTime();
			}
		};
		fXComponents.getStage().addEventHandler(EventType.ROOT, event);
		return fXComponents.getStage();
	}

	/**
	 * Load screen.
	 *
	 * @param screen the screen
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected void loadScreen(String screen) throws IOException {
		getScene(getRoot(screen));
	}

	protected Parent getRoot(String screen) throws IOException {
		return BaseController.load(getClass().getResource(screen), applicationContext.getApplicationLanguageLabelBundle());
	}

	/**
	 * Gets the scene.
	 *
	 * @param borderPane the border pane
	 * @return the scene
	 */
	protected Scene getScene(Parent borderPane) {
		scene = fXComponents.getScene();
		if (scene == null) {
			scene = new Scene(borderPane);
			fXComponents.setScene(scene);
		}
		scene.setRoot(borderPane);
		fXComponents.getStage().setScene(scene);
		scene.setNodeOrientation(applicationContext.isPrimaryLanguageRightToLeft() ?
				NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
		scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
		return scene;
	}

	/**
	 * Loading FXML files along with beans.
	 *
	 * @param <T> the generic type
	 * @param url the url
	 * @return T
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static <T> T load(URL url) throws IOException {
		String langCode = ApplicationContext.applicationLanguage();
		if (SessionContext.map() != null && !SessionContext.map().isEmpty() && SessionContext.map().containsKey(RegistrationConstants.REGISTRATION_DATA)) {
			RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.map()
					.get(RegistrationConstants.REGISTRATION_DATA);
			if (registrationDTO != null && registrationDTO.getSelectedLanguagesByApplicant() != null) {
				langCode = registrationDTO.getSelectedLanguagesByApplicant().get(0);
			}
		}
		FXMLLoader loader = new FXMLLoader(url, ApplicationContext.getBundle(langCode, RegistrationConstants.LABELS));
		loader.setControllerFactory(ClientApplication.getApplicationContext()::getBean);
		return loader.load();
	}

	public static <T> T loadWithNewInstance(URL url, Object controller) throws IOException {
		String langCode = ApplicationContext.applicationLanguage();
		if (SessionContext.map() != null && !SessionContext.map().isEmpty()) {
			RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.map()
					.get(RegistrationConstants.REGISTRATION_DATA);
			if (registrationDTO != null && registrationDTO.getSelectedLanguagesByApplicant() != null) {
				langCode = registrationDTO.getSelectedLanguagesByApplicant().get(0);
			}
		}
		FXMLLoader loader = new FXMLLoader(url,
				ApplicationContext.getBundle(langCode, RegistrationConstants.LABELS));
		loader.setController(controller);
		return loader.load();
	}

	/**
	 * Loading FXML files along with beans.
	 *
	 * @param <T>      the generic type
	 * @param url      the url
	 * @param resource the resource
	 * @return T
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static <T> T load(URL url, ResourceBundle resource) throws IOException {
		FXMLLoader loader = new FXMLLoader(url, resource);
		loader.setControllerFactory(ClientApplication.getApplicationContext()::getBean);
		return loader.load();
	}

	/**
	 * /* Alert creation with specified title, header, and context.
	 *
	 * @param title   alert title
	 * @param context alert context
	 */
	public void generateAlert(String title, String context) {
		try {
			closeAlreadyExistedAlert();
			alertStage = new Stage();
			Pane authRoot = BaseController.load(getClass().getResource(RegistrationConstants.ALERT_GENERATION));
			Scene scene = new Scene(authRoot);
			scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
			alertStage.initStyle(StageStyle.UNDECORATED);
			alertStage.setScene(scene);
			alertStage.initModality(Modality.WINDOW_MODAL);
			alertController.getAlertGridPane().setPrefHeight((double)context.length() / 2 + 110);
			if (scanPopUpViewController.getPopupStage() != null
					&& scanPopUpViewController.getPopupStage().isShowing()) {
				alertStage.initOwner(scanPopUpViewController.getPopupStage());
				alertTypeCheck(title, context, alertStage);
			} else if (registrationApprovalController.getPrimaryStage() != null
					&& registrationApprovalController.getPrimaryStage().isShowing()) {
				alertStage.initOwner(registrationApprovalController.getPrimaryStage());
				alertTypeCheck(title, context, alertStage);
			} else {
				alertStage.initOwner(fXComponents.getStage());
				alertTypeCheck(title, context, alertStage);
			}
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - ALERT - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - ALERT - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	/**
	 * /* Alert creation with specified title, header, and context.
	 *
	 * @param title   alert title
	 * @param context alert context
	 */
	protected boolean generateAlert(String title, String context, ToRun<Boolean> run, BaseController controller) {
		boolean isValid = false;
		try {
			closeAlreadyExistedAlert();
			alertStage = new Stage();
			Pane authRoot = BaseController.load(getClass().getResource(RegistrationConstants.ALERT_GENERATION));
			Scene scene = new Scene(authRoot);
			scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
			alertStage.initStyle(StageStyle.UNDECORATED);
			alertStage.setScene(scene);
			alertStage.initModality(Modality.WINDOW_MODAL);

			alertController.getAlertGridPane().setPrefHeight((double)context.length() / 2 + 110);
			controller.setScanningMsg(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.VALIDATION_MESSAGE));
			alertTypeCheck(title, context, alertStage);
			isValid = run.toRun();
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - ALERT - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		} catch (RuntimeException runtimeException) {
			LOGGER.error("REGISTRATION - ALERT - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
		return isValid;
	}

	private void alertTypeCheck(String title, String context, Stage alertStage) {
		if (!context.contains(RegistrationConstants.SUCCESS.toUpperCase()) && !context.contains(RegistrationConstants.ERROR.toUpperCase())) {
			if (SessionContext.isSessionContextAvailable()) {
				SessionContext.map().put(ALERT_STAGE, alertStage);
			}
			alertController.generateAlertResponse(title, context);
			alertStage.showAndWait();
		} else {
			if (SessionContext.isSessionContextAvailable()) {
				SessionContext.map().put(ALERT_STAGE, alertStage);
			}
			alertController.generateAlertResponse(title, context);
			alertStage.showAndWait();
		}
		alertController.alertWindowExit();
	}

	/**
	 * /* Alert creation with specified title, header, and context.
	 *
	 * @param title   the title
	 * @param context alert context
	 */
	protected void generateAlertLanguageSpecific(String title, String context) {
		generateAlert(title, RegistrationUIConstants.getMessageLanguageSpecific(context));
	}

	/**
	 * Alert specific for page navigation confirmation
	 *
	 * @return
	 */
	protected boolean pageNavigantionAlert() {
		if (!fXComponents.getScene().getRoot().getId().equals("mainBox") && SessionContext.map() != null && SessionContext.map()
				.containsKey(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ) && !SessionContext.map()
				.get(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ).equals(RegistrationConstants.ENABLE)) {

			Alert alert = createAlert(AlertType.CONFIRMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.INFORMATION),
					RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.ALERT_NOTE_LABEL), RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PAGE_NAVIGATION_MESSAGE),
					RegistrationConstants.PAGE_NAVIGATION_CONFIRM, RegistrationConstants.PAGE_NAVIGATION_CANCEL);

			alert.show();
			Rectangle2D screenSize = Screen.getPrimary().getVisualBounds();
			Double xValue = screenSize.getWidth() / 2 - alert.getWidth() + 250;
			Double yValue = screenSize.getHeight() / 2 - alert.getHeight();
			alert.hide();
			alert.setX(xValue);
			alert.setY(yValue);
			alert.showAndWait();
			/* Get Option from user */
			ButtonType result = alert.getResult();
			if (result == ButtonType.OK) {
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Alert creation with specified context.
	 *
	 * @param parentPane the parent pane
	 * @param id         the id
	 * @param context    alert context
	 */
	protected void generateAlert(Pane parentPane, String id, String context) {
		String type = "#TYPE#";
		if (id.contains(RegistrationConstants.ONTYPE)) {
			id = id.replaceAll(RegistrationConstants.UNDER_SCORE + RegistrationConstants.ONTYPE,
					RegistrationConstants.EMPTY);
		}

		String[] parts = id.split("__");
		if (parts.length > 1 && parts[1].matches(RegistrationConstants.DTAE_MONTH_YEAR_REGEX)) {
			id = parts[0] + "__" + RegistrationConstants.DOB;
			parentPane = (Pane) parentPane.getParent().getParent();
		}
		Label label = ((Label) (parentPane.lookup(RegistrationConstants.HASH + id + RegistrationConstants.MESSAGE)));
		if (label != null) {
			if (!(label.isVisible() && id.equals(RegistrationConstants.DOB))) {
				String[] split = context.split(type);
				label.setText(split[0]);
				label.setWrapText(true);
			}

			Tooltip tool = new Tooltip(context.contains(type) ? context.split(type)[0] : context);
			tool.getStyleClass().add(RegistrationConstants.TOOLTIP);
			label.setTooltip(tool);
			label.setVisible(true);
		}
	}

	/**
	 * Validate sync status.
	 *
	 * @return the response DTO
	 */
	protected ResponseDTO validateSyncStatus() {

		return syncStatusValidatorService.validateSyncStatus();
	}

	/**
	 * Validating Id for Screen Authorization.
	 *
	 * @param screenId the screenId
	 * @return boolean
	 */
	protected boolean validateScreenAuthorization(String screenId) {

		return SessionContext.userContext().getAuthorizationDTO().getAuthorizationScreenId().contains(screenId);
	}

	/**
	 * Regex validation with specified field and pattern.
	 *
	 * @param field        concerned field
	 * @param regexPattern pattern need to checked
	 * @return true, if successful
	 */
	protected boolean validateRegex(Control field, String regexPattern) {
		if (field instanceof TextField) {
			if (!((TextField) field).getText().matches(regexPattern))
				return true;
		} else {
			if (field instanceof PasswordField) {
				if (!((PasswordField) field).getText().matches(regexPattern))
					return true;
			}
		}
		return false;
	}

	/**
	 * {@code autoCloseStage} is to close the stage automatically by itself for a
	 * configured amount of time.
	 *
	 * @param stage the stage
	 */
	protected void autoCloseStage(Stage stage) {
		PauseTransition delay = new PauseTransition(Duration.seconds(5));
		delay.setOnFinished(event -> stage.close());
		delay.play();
	}


	/**
	 * Opens the home page screen.
	 */
	public void goToHomePage() {
		try {
			if (isAckOpened() || pageNavigantionAlert()) {
				setIsAckOpened(false);
				BaseController.load(getClass().getResource(RegistrationConstants.HOME_PAGE));
				if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
					clearOnboardData();
					clearRegistrationData();

				} else {
					SessionContext.map().put(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ,
							RegistrationConstants.ENABLE);
				}
			}

		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - REDIRECTHOME - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_HOME_PAGE));
		} catch (RuntimeException runtimException) {
			LOGGER.error("REGISTRATION - REDIRECTHOME - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					runtimException.getMessage() + ExceptionUtils.getStackTrace(runtimException));
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_HOME_PAGE));
		}
	}

	/**
	 * Opens the home page screen.
	 */
	public void goToSettingsFromRegistration() {
		try {
			if (isAckOpened() || pageNavigantionAlert()) {
				setIsAckOpened(false);
				if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
					clearOnboardData();
					clearRegistrationData();

				} else {
					SessionContext.map().put(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ,
							RegistrationConstants.ENABLE);
				}
			}

		} catch (RuntimeException runtimException) {
			LOGGER.error("REGISTRATION - REDIRECTHOME - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					runtimException.getMessage() + ExceptionUtils.getStackTrace(runtimException));
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_HOME_PAGE));
		}
	}

	/**
	 * Opens the home page screen.
	 */
	public void loadLoginScreen() {
		try {
			Parent root = load(getClass().getResource(RegistrationConstants.INITIAL_PAGE));
			getStage().setScene(getScene(root));
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - REDIRECTLOGIN - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
		}
	}

	/**
	 * This method is used clear all the new registration related mapm values and
	 * navigates to the home page.
	 */
	public void goToHomePageFromRegistration() {
		LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Going to home page");

		goToHomePage();

	}

	/**
	 * This method is used clear all the new onboard related mapm values and
	 * navigates to the home page.
	 */
	public void goToHomePageFromOnboard() {
		LOGGER.info(RegistrationConstants.REGISTRATION_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
				RegistrationConstants.APPLICATION_ID, "Going to home page");

		goToHomePage();
	}

	/**
	 * Clear registration data.
	 */
	protected void clearRegistrationData() {

		SessionContext.map().remove(RegistrationConstants.REGISTRATION_ISEDIT);
		SessionContext.map().remove(RegistrationConstants.REGISTRATION_PANE1_DATA);
		SessionContext.map().remove(RegistrationConstants.REGISTRATION_PANE2_DATA);
		SessionContext.map().remove(RegistrationConstants.REGISTRATION_AGE_DATA);
		SessionContext.map().remove(RegistrationConstants.REGISTRATION_DATA);
		SessionContext.map().remove(RegistrationConstants.IS_Child);
		SessionContext.map().remove(RegistrationConstants.DD);
		SessionContext.map().remove(RegistrationConstants.MM);
		SessionContext.map().remove(RegistrationConstants.YYYY);
		SessionContext.map().remove(RegistrationConstants.DOB_TOGGLE);
		SessionContext.map().remove(RegistrationConstants.UIN_UPDATE_DEMOGRAPHICDETAIL);
		SessionContext.map().remove(RegistrationConstants.UIN_UPDATE_DOCUMENTSCAN);
		SessionContext.map().remove(RegistrationConstants.UIN_UPDATE_FINGERPRINTCAPTURE);
		SessionContext.map().remove(RegistrationConstants.UIN_UPDATE_BIOMETRICEXCEPTION);
		SessionContext.map().remove(RegistrationConstants.UIN_UPDATE_FACECAPTURE);
		SessionContext.map().remove(RegistrationConstants.UIN_UPDATE_IRISCAPTURE);
		SessionContext.map().remove(RegistrationConstants.UIN_UPDATE_REGISTRATIONPREVIEW);
		SessionContext.map().remove(RegistrationConstants.UIN_UPDATE_OPERATORAUTHENTICATIONPANE);
		SessionContext.map().remove(RegistrationConstants.OLD_BIOMETRIC_EXCEPTION);
		SessionContext.map().remove(RegistrationConstants.NEW_BIOMETRIC_EXCEPTION);

		//guardianBiometricsController.clearCapturedBioData();
		guardianBiometricsController.clearBioCaptureInfo();

		SessionContext.userMap().remove(RegistrationConstants.TOGGLE_BIO_METRIC_EXCEPTION);
		SessionContext.userMap().remove(RegistrationConstants.IS_LOW_QUALITY_BIOMETRICS);
		SessionContext.map().remove(RegistrationConstants.DUPLICATE_FINGER);

		if (SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA) != null) {
			RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
			registrationDTO.clearRegistrationDto();
			SessionContext.map().remove(RegistrationConstants.REGISTRATION_DATA);
		}

		if(documentScanController.getScannedPages() != null) {
			documentScanController.getScannedPages().clear();
		}

		pageFlow.loadPageFlow();

	}

	/**
	 * Clear onboard data.
	 */
	protected void clearOnboardData() {
		SessionContext.map().put(RegistrationConstants.ONBOARD_USER_UPDATE, false);
		SessionContext.map().put(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ, RegistrationConstants.DISABLE);
		SessionContext.map().put(RegistrationConstants.ONBOARD_USER, false);
		SessionContext.map().remove(RegistrationConstants.USER_ONBOARD_DATA);
		SessionContext.map().remove(RegistrationConstants.OLD_BIOMETRIC_EXCEPTION);
		SessionContext.map().remove(RegistrationConstants.NEW_BIOMETRIC_EXCEPTION);
	}

	/**
	 * Load child.
	 *
	 * @param url the url
	 * @return the FXML loader
	 */
	public static FXMLLoader loadChild(URL url) {
		FXMLLoader loader = new FXMLLoader(url, ApplicationContext.getInstance()
				.getApplicationLanguageLabelBundle());
		loader.setControllerFactory(ClientApplication.getApplicationContext()::getBean);
		return loader;
	}

	/**
	 * Gets the finger print status.
	 *
	 */
	public void updateAuthenticationStatus() {

	}

	/**
	 * Scans documents.
	 *
	 * @param popupStage the stage
	 */
	public void scan(Stage popupStage) {

	}

	/**
	 * Convert bytes to image.
	 *
	 * @param imageBytes the image bytes
	 * @return the image
	 */
	public Image convertBytesToImage(byte[] imageBytes) {
		Image image = null;
		if (imageBytes != null) {
			image = new Image(new ByteArrayInputStream(imageBytes));
		}
		return image;
	}

	/**
	 * Online availability check.
	 *
	 * @return the timer
	 */
	protected Timer onlineAvailabilityCheck() {
		Timer timer = new Timer();
		fXComponents.setTimer(timer);
		return timer;
	}

	/**
	 * Stop timer.
	 */
	protected void stopTimer() {
		if (fXComponents.getTimer() != null) {
			fXComponents.getTimer().cancel();
			fXComponents.getTimer().purge();
			fXComponents.setTimer(null);
		}
	}

	/**
	 * Gets the timer.
	 *
	 * @return the timer
	 */
	public Timer getTimer() {
		return fXComponents.getTimer() == null ? onlineAvailabilityCheck() : fXComponents.getTimer();
	}

	/**
	 * to validate the password in case of password based authentication.
	 *
	 * @param username the username
	 * @param password the password
	 * @return the string
	 */
	protected String validatePwd(String username, String password) {

		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID, "Validating Password");

		AuthenticationValidatorDTO authenticationValidatorDTO = new AuthenticationValidatorDTO();
		authenticationValidatorDTO.setUserId(username);
		authenticationValidatorDTO.setPassword(password);

		try {
			return authenticationService.validatePassword(authenticationValidatorDTO) ? RegistrationConstants.SUCCESS :
					RegistrationConstants.FAILURE;
		} catch (RegBaseCheckedException e) {
			LOGGER.error("PWD login failed due to : ", e.getErrorCode());
			return e.getErrorCode();
		}
	}

	/**
	 * Clear all values.
	 */
	protected void clearAllValues() {
		guardianBiometricsController.clearCapturedBioData();
	}

	/**
	 * Gets the registration DTO from session.
	 *
	 * @return the registration DTO from session
	 */
	public RegistrationDTO getRegistrationDTOFromSession() {
		RegistrationDTO registrationDTO = null;
		if (SessionContext.map() != null && !SessionContext.map().isEmpty()) {
			registrationDTO = (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
		}
		return registrationDTO;

	}

	/**
	 * to return to the next page based on the current page and action for User
	 * Onboarding.
	 *
	 * @param currentPage - Id of current Anchorpane
	 * @param action      - action to be performed previous/next
	 * @return id of next Anchorpane
	 */
	protected String getOnboardPageDetails(String currentPage, String action) {

		LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
				"Updating OnBoard flow based on visibility and returning next page details");

		LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
				"Returning Next page by action " + action);

		String page = null;

		if (action.equalsIgnoreCase(RegistrationConstants.NEXT)) {
			/** Get Next Page if action is "NEXT" */
			page = pageFlow.getNextOnboardPage(currentPage);
		} else if (action.equalsIgnoreCase(RegistrationConstants.PREVIOUS)) {

			/** Get Previous Page if action is "PREVIOUS" */
			page = pageFlow.getPreviousOnboardPage(currentPage);
		}

		page = saveDetails(currentPage, page);
		return page;

	}

	/**
	 * to return to the next page based on the current page and action for New
	 * Registration.
	 *
	 * @param currentPage - Id of current Anchorpane
	 * @param action      - action to be performed previous/next
	 * @return id of next Anchorpane
	 */
	protected String getPageByAction(String currentPage, String action) {

		LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
				"Returning Next page by action " + action);

		String page = null;

		if (action.equalsIgnoreCase(RegistrationConstants.NEXT)) {
			/** Get Next Page if action is "NEXT" */
			page = pageFlow.getNextRegPage(currentPage);
		} else if (action.equalsIgnoreCase(RegistrationConstants.PREVIOUS)) {

			/** Get Previous Page if action is "PREVIOUS" */
			page = pageFlow.getPreviousRegPage(currentPage);
		}

		page = saveDetails(currentPage, page);
		return page;

	}

	/**
	 * to return to the next page based on the current page and action.
	 *
	 * @param pageList    - List of Anchorpane Ids
	 * @param currentPage - Id of current Anchorpane
	 * @param action      - action to be performed previous/next
	 * @return id of next Anchorpane
	 */
	private String getReturnPage(List<String> pageList, String currentPage, String action) {

		LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
				"Fetching the next page based on action");

		String returnPage = "";

		if (action.equalsIgnoreCase(RegistrationConstants.NEXT)) {

			LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
					"Fetching the next page based from list of ids for Next action");

			returnPage = pageList.get((pageList.indexOf(currentPage)) + 1);
		} else if (action.equalsIgnoreCase(RegistrationConstants.PREVIOUS)) {

			LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
					"Fetching the next page based from list of ids for Previous action");

			returnPage = pageList.get((pageList.indexOf(currentPage)) - 1);
		}

		saveDetails(currentPage, returnPage);

		LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
				"Returning the corresponding next page based on given action" + returnPage);

		pageDetails.clear();
		return returnPage;
	}

	private String saveDetails(String currentPage, String returnPage) {
		if (RegistrationConstants.REGISTRATION_PREVIEW.equalsIgnoreCase(returnPage)) {

			LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
					"Invoking Save Detail before redirecting to Preview");

			registrationPreviewController.setUpPreviewContent();

			LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
					"Details saved and content of preview is set");
		} else if (RegistrationConstants.ONBOARD_USER_SUCCESS.equalsIgnoreCase(returnPage)) {

			LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID, "Validating User Onboard data");

			if (executeUserOnboardTask(userOnboardService.getAllBiometrics())) {
				returnPage = RegistrationConstants.EMPTY;
			} else {
				returnPage = currentPage;
			}
		}

		return returnPage;
	}

	private boolean executeUserOnboardTask(List<BiometricsDto> allBiometrics) {
		AtomicBoolean returnPage = new AtomicBoolean(false);
		userOnboardParentController.getParentPane().setDisable(true);
		userOnboardParentController.getProgressIndicatorParentPane().setVisible(true);
		userOnboardParentController.getProgressIndicator().setVisible(true);

		Service<ResponseDTO> taskService = new Service<ResponseDTO>() {
			@Override
			protected Task<ResponseDTO> createTask() {
				return new Task<ResponseDTO>() {
					/*
					 * (non-Javadoc)
					 *
					 * @see javafx.concurrent.Task#call()
					 */
					@Override
					protected ResponseDTO call() {
						try {
							return userOnboardService.validateWithIDAuthAndSave(allBiometrics);
						} catch (RegBaseCheckedException checkedException) {
							LOGGER.error(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
									ExceptionUtils.getStackTrace(checkedException));
						}
						return null;
					}
				};
			}
		};

		userOnboardParentController.getProgressIndicator().progressProperty().bind(taskService.progressProperty());
		taskService.start();
		taskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				ResponseDTO response = taskService.getValue();
				if (response != null && response.getErrorResponseDTOs() != null
						&& response.getErrorResponseDTOs().get(0) != null) {
					LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
							"Displaying Alert if validation is not success");

					generateAlertLanguageSpecific(RegistrationConstants.ERROR,
							response.getErrorResponseDTOs().get(0).getMessage());
					returnPage.set(false);
				} else if (response != null && response.getSuccessResponseDTO() != null) {

					LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
							"User Onboard is success and clearing Onboard data");

					clearOnboardData();
					SessionContext.map().put(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ,
							RegistrationConstants.ENABLE);
					goToHomePage();
					onboardAlertMsg();

					LOGGER.info(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
							"Redirecting to Home page after success onboarding");
					returnPage.set(true);
				}
				userOnboardParentController.getParentPane().setDisable(false);
				userOnboardParentController.getProgressIndicatorParentPane().setVisible(false);
				userOnboardParentController.getProgressIndicator().setVisible(false);

				LOGGER.info(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
						"Onboarded User biometrics validation and insertion done");
			}
		});
		return returnPage.get();
	}

	/**
	 * to navigate to the next page based on the current page.
	 *
	 * @param pageId     - Parent Anchorpane where other panes are included
	 * @param notTosShow - Id of Anchorpane which has to be hidden
	 * @param show       - Id of Anchorpane which has to be shown
	 *
	 */
	protected void getCurrentPage(Pane pageId, String notTosShow, String show) {
		LOGGER.info("Pane : {}, Navigating from current page {} to show : {}",
				pageId == null ? "null" : pageId.getId(), notTosShow, show);

		if (pageId != null) {
			if (notTosShow != null) {
				((Pane) pageId.lookup(RegistrationConstants.HASH + notTosShow)).setVisible(false);
			}
			if (show != null) {
				((Pane) pageId.lookup(RegistrationConstants.HASH + show)).setVisible(true);
			}
		}

		LOGGER.info("Navigated to next page >> {}", show);
	}

	public void remapMachine() {
		String message = RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.REMAP_MESSAGE);

		if (isPacketsPendingForEODOrReRegister()) {
			message += RegistrationConstants.NEW_LINE + RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.REMAP_EOD_PROCESS_MESSAGE);
		}
		generateAlert(RegistrationConstants.ALERT_INFORMATION, message);

		disableHomePage(true);

		Service<String> service = new Service<String>() {
			@Override
			protected Task<String> createTask() {
				return new Task<String>() {

					@Override
					protected String call() throws RemapException {

						packetHandlerController.getProgressIndicator().setVisible(true);

						for (int i = 1; i <= 4; i++) {
							/* starts the remap process */
							centerMachineReMapService.handleReMapProcess(i);
							this.updateProgress(i, 4);
						}
						LOGGER.info("BASECONTROLLER_REGISTRATION CENTER MACHINE REMAP : ", APPLICATION_NAME,
								APPLICATION_ID, "center remap process completed");
						return null;
					}
				};
			}
		};
		packetHandlerController.getProgressIndicator().progressProperty().bind(service.progressProperty());

		service.restart();

		service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				handleRemapResponse(service, true);
			}
		});
		service.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				handleRemapResponse(service, false);
			}
		});
	}

	private void handleRemapResponse(Service<String> service, boolean isSuccess) {
		service.reset();
		disableHomePage(false);
		packetHandlerController.getProgressIndicator().setVisible(false);

		if (isSuccess) {
			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.REMAP_PROCESS_SUCCESS));
			headerController.logoutCleanUp();
		} else {
			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.REMAP_PROCESS_STILL_PENDING));
		}
	}

	private void disableHomePage(boolean isDisabled) {

		if (null != homeController.getMainBox())
			homeController.getMainBox().setDisable(isDisabled);

	}

	/**
	 * Checks if is packets pending for EOD.
	 *
	 * @return true, if is packets pending for EOD
	 */
	protected boolean isPacketsPendingForEOD() {

		return centerMachineReMapService.isPacketsPendingForEOD();
	}

	protected boolean isPacketsPendingForEODOrReRegister() {

		return isPacketsPendingForEOD() || isPacketsPendingForReRegister();
	}

	/**
	 * Checks if is packets pending for ReRegister.
	 *
	 * @return true, if is packets pending for ReRegister
	 */
	protected boolean isPacketsPendingForReRegister() {

		return centerMachineReMapService.isPacketsPendingForReRegister();
	}

	/**
	 * Popup statge.
	 *
	 */
	public void onboardAlertMsg() {
		packetHandlerController.getUserOnboardMessage().setVisible(true);
		fXComponents.getStage().addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (packetHandlerController.getUserOnboardMessage().isVisible()) {
					packetHandlerController.getUserOnboardMessage().setVisible(false);
				}
			}
		});
	}

	/**
	 * Create alert with given title, header and context.
	 *
	 * @param alertType type of alert
	 * @param title     alert's title
	 * @param header    alert's header
	 * @param context   alert's context
	 * @return alert
	 */
	protected Alert createAlert(AlertType alertType, String title, String header, String context,
								String confirmButtonText, String cancelButtonText) {

		Alert alert = new Alert(alertType);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(
				context.replaceAll(RegistrationConstants.SPLITTER + (RegistrationConstants.SUCCESS.toUpperCase()), ""));
		alert.setGraphic(null);
		alert.setResizable(true);
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.getDialogPane().setMinWidth(500);
		alert.getDialogPane().getStylesheets()
				.add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
		Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
		okButton.setText(RegistrationUIConstants.getMessageLanguageSpecific(confirmButtonText));
		okButton.setId("confirm");

		if (alertType == Alert.AlertType.CONFIRMATION) {
			Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
			cancelButton.setText(RegistrationUIConstants.getMessageLanguageSpecific(cancelButtonText));
			cancelButton.setId("cancel");
		}
		alert.initStyle(StageStyle.UNDECORATED);
		alert.initModality(Modality.WINDOW_MODAL);
		alert.initOwner(fXComponents.getStage());
		if (SessionContext.isSessionContextAvailable()) {
			SessionContext.map().put("alert", alert);
		}
		return alert;
	}

	/**
	 * Gets the value from application context.
	 *
	 * @param key the key
	 * @return the value from application context
	 */
	public String getValueFromApplicationContext(String key) {
		LOGGER.debug("Fetching value from application Context {}", key);
		return ApplicationContext.getStringValueFromApplicationMap(key);
	}

	/**
	 * Gets the quality score.
	 *
	 * @param qualityScore the quality score
	 * @return the quality score
	 */
	protected String getQualityScoreText(Double qualityScore) {
		return String.valueOf(Math.round(qualityScore)).concat(RegistrationConstants.PERCENTAGE);
	}

	/**
	 * Updates the Page Flow
	 *
	 * @param pageId id of the page
	 * @param val    value to be set
	 */
	protected void updatePageFlow(String pageId, boolean val) {
		LOGGER.info("Updating page flow to navigate next or previous, pageId:{}, {}", pageId, val);
		pageFlow.updateRegMap(pageId, RegistrationConstants.VISIBILITY, val);
	}

	protected void restartApplication() {

		generateAlert(RegistrationConstants.SUCCESS.toUpperCase(), RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.RESTART_APPLICATION));
		restartController.restart();

	}

	/**
	 * Restricts the re-ordering of the columns in {@link TableView}. This is
	 * generic method.
	 *
	 * @param table the instance of {@link TableView} for which re-ordering of
	 *              columns had to be restricted
	 */
	protected void disableColumnsReorder(TableView<?> table) {
		if (table != null) {
			table.widthProperty().addListener((source, oldWidth, newWidth) -> {
				javafx.scene.control.skin.TableHeaderRow header = (javafx.scene.control.skin.TableHeaderRow) table
						.lookup("TableHeaderRow");
			});
		}
	}

	public void closeAlreadyExistedAlert() {
		if (SessionContext.isSessionContextAvailable() && SessionContext.map() != null
				&& SessionContext.map().get(ALERT_STAGE) != null) {
			Stage alertStageFromSession = (Stage) SessionContext.map().get(ALERT_STAGE);
			alertStageFromSession.close();

		}
	}

	public interface ToRun<T> {
		public T toRun();
	}

	/**
	 * Check of wheteher operator was in acknowledgement page
	 *
	 * @return true or false if acknowledge page was opened
	 */
	protected boolean isAckOpened() {
		return isAckOpened;
	}

	/**
	 * Set the operator was in acknowledgement page
	 */
	protected void setIsAckOpened(boolean isAckOpened) {
		BaseController.isAckOpened = isAckOpened;
	}

	public SchemaDto getLatestSchema() {
		try {
			return identitySchemaService.getIdentitySchema(identitySchemaService.getLatestEffectiveSchemaVersion());
		} catch (RegBaseCheckedException exception) {
			LOGGER.error(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
		}
		return null;
	}

	public ProcessSpecDto getProcessSpec(String processId, double idVersion) {
		try {
			return identitySchemaService.getProcessSpecDto(processId, idVersion);
		} catch (RegBaseCheckedException exception) {
			LOGGER.error(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
		}
		return null;
	}

	public List<UiFieldDTO> getAllFields(String processId, double idVersion) {
		try {
			return identitySchemaService.getAllFieldSpec(processId, idVersion);
		} catch (RegBaseCheckedException exception) {
			LOGGER.error(LoggerConstants.LOG_REG_BASE, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
		}
		return null;
	}

	public SimpleEntry<String, List<String>> getValue(String bio, List<String> attributes) {
		SimpleEntry<String, List<String>> entry = new SimpleEntry<String, List<String>>(bio, attributes);
		return entry;
	}

	protected List<String> getContainsAllElements(List<String> source, List<String> target) {
		if (target != null) {
			return source.stream().filter(target::contains).collect(Collectors.toList());
		}
		return new ArrayList<String>();
	}

	protected void updateByAttempt(double qualityScore, Image streamImage, double thresholdScore,
								   ImageView streamImagePane, Label qualityText, ProgressBar progressBar) {

		String qualityScoreLabelVal = getQualityScoreText(qualityScore);

		if (qualityScoreLabelVal != null) {
			// Set Stream image
			streamImagePane.setImage(streamImage);

			// Quality Label
			qualityText.setText(qualityScoreLabelVal);

			// Progress Bar
			progressBar.setProgress(qualityScore / 100);

			if (qualityScore >= thresholdScore) {
				progressBar.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_RED);
				progressBar.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_GREEN);
			} else {
				progressBar.getStyleClass().removeAll(RegistrationConstants.PROGRESS_BAR_GREEN);
				progressBar.getStyleClass().add(RegistrationConstants.PROGRESS_BAR_RED);
			}
		}
	}

	public Map<Entry<String, String>, Map<String, List<List<String>>>> getOnboardUserMap() {
		Map<Entry<String, String>, Map<String, List<List<String>>>> mapToProcess = new HashMap<>();

		Map<String, String> labels = new HashMap<>();
		labels.put("OPERATOR", "OPERATOR");

		Object value = ApplicationContext.map().get(RegistrationConstants.OPERATOR_ONBOARDING_BIO_ATTRIBUTES);
		List<String> attributes = (value != null) ? Arrays.asList(((String) value).split(","))
				: new ArrayList<String>();
		HashMap<String, List<List<String>>> subMap = new HashMap<String, List<List<String>>>();
		subMap.put(io.mosip.registration.enums.Modality.FINGERPRINT_SLAB_LEFT.name(),
				Arrays.asList(ListUtils.intersection(io.mosip.registration.enums.Modality.FINGERPRINT_SLAB_LEFT.getAttributes(), attributes),
						ListUtils.subtract(io.mosip.registration.enums.Modality.FINGERPRINT_SLAB_LEFT.getAttributes(), attributes)));
		subMap.put(io.mosip.registration.enums.Modality.FINGERPRINT_SLAB_RIGHT.name(),
				Arrays.asList(ListUtils.intersection(io.mosip.registration.enums.Modality.FINGERPRINT_SLAB_RIGHT.getAttributes(), attributes),
						ListUtils.subtract(io.mosip.registration.enums.Modality.FINGERPRINT_SLAB_RIGHT.getAttributes(), attributes)));
		subMap.put(io.mosip.registration.enums.Modality.FINGERPRINT_SLAB_THUMBS.name(),
				Arrays.asList(ListUtils.intersection(io.mosip.registration.enums.Modality.FINGERPRINT_SLAB_THUMBS.getAttributes(), attributes),
						ListUtils.subtract(io.mosip.registration.enums.Modality.FINGERPRINT_SLAB_THUMBS.getAttributes(), attributes)));
		subMap.put(io.mosip.registration.enums.Modality.IRIS_DOUBLE.name(),
				Arrays.asList(ListUtils.intersection(io.mosip.registration.enums.Modality.IRIS_DOUBLE.getAttributes(), attributes),
						ListUtils.subtract(io.mosip.registration.enums.Modality.IRIS_DOUBLE.getAttributes(), attributes)));
		subMap.put(io.mosip.registration.enums.Modality.FACE.name(),
				Arrays.asList(ListUtils.intersection(io.mosip.registration.enums.Modality.FACE.getAttributes(), attributes),
						ListUtils.subtract(io.mosip.registration.enums.Modality.FACE.getAttributes(), attributes)));

		for (Entry<String, String> entry : labels.entrySet()) {
			mapToProcess.put(entry, subMap);
		}
		return mapToProcess;
	}

	public String getCssName() {
		return cssTheme == null || cssTheme.isBlank() ? "application.css" : String.format("application-%s.css", cssTheme);
	}

	protected String getLocalZoneTime(String time) {
		try {
			String formattedTime = Timestamp.valueOf(time).toLocalDateTime()
					.format(DateTimeFormatter.ofPattern(RegistrationConstants.UTC_PATTERN));
			LocalDateTime dateTime = DateUtils.parseUTCToLocalDateTime(formattedTime);
			return dateTime
					.format(DateTimeFormatter.ofPattern(RegistrationConstants.ONBOARD_LAST_BIOMETRIC_UPDTAE_FORMAT));
		} catch (RuntimeException exception) {
			LOGGER.error("REGISTRATION - ALERT - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
			return time + RegistrationConstants.UTC_APPENDER;
		}

	}

	public boolean proceedOnAction(String job) {

		if (!serviceDelegateUtil.isNetworkAvailable()) {
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_INTERNET_CONNECTION));
			return false;
		}

		if (!authTokenUtilService.hasAnyValidToken()) {
			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.USER_RELOGIN_REQUIRED));
			return false;
		}

		try {
			switch (job) {
				case "MS":
					baseService.proceedWithMasterAndKeySync(null);
					break;
				case "PS":
					baseService.proceedWithPacketSync();
					break;
				case "RM":
					baseService.proceedWithMachineCenterRemap();
					break;
				case "OU":
					baseService.proceedWithOperatorOnboard();
					break;
				default:
					baseService.proceedWithMasterAndKeySync(job);
					break;
			}
		} catch (PreConditionCheckException ex) {
			ResourceBundle resourceBundle = applicationContext.getApplicationLanguageMessagesBundle();

			generateAlert(RegistrationConstants.ERROR,
					resourceBundle.containsKey(ex.getErrorCode()) ? resourceBundle.getString(ex.getErrorCode())
							: ex.getErrorCode());
			return false;
		}
		return true;
	}

	public boolean proceedOnRegistrationAction() {
		try {
			baseService.proceedWithRegistration();
		} catch (PreConditionCheckException ex) {

			ResourceBundle resourceBundle = applicationContext.getApplicationLanguageMessagesBundle();

			generateAlert(RegistrationConstants.ERROR,
					resourceBundle.containsKey(ex.getErrorCode()) ? resourceBundle.getString(ex.getErrorCode())
							: ex.getErrorCode());
			return false;
		}
		return true;
	}

	public boolean proceedOnReRegistrationAction() {
		try {
			baseService.proceedWithReRegistration();
		} catch (PreConditionCheckException ex){
			ResourceBundle resourceBundle = applicationContext.getApplicationLanguageMessagesBundle();

			generateAlert(RegistrationConstants.ERROR,
					resourceBundle.containsKey(ex.getErrorCode()) ? resourceBundle.getString(ex.getErrorCode())
							: ex.getErrorCode());
			return false;
		}
		return true;
	}

	protected List<GenericDto> getConfiguredLanguages() {
		List<GenericDto> languages = new ArrayList<>();
		for (String langCode : getConfiguredLangCodes()) {
			ResourceBundle bundle = ApplicationContext.getBundle(langCode, RegistrationConstants.LABELS);
			languages.add(new GenericDto(langCode, bundle != null ? bundle.getString("language") : langCode, langCode));
		}
		return languages;
	}

	protected List<GenericDto> getConfiguredLanguagesForLogin() {
		List<GenericDto> languages = new ArrayList<>();
		for (String langCode : getConfiguredLangCodes()) {
			ResourceBundle bundle = ApplicationContext.getBundle(langCode, RegistrationConstants.LABELS);
			if (bundle != null) {
				languages.add(new GenericDto(langCode, bundle.getString("language"), langCode));
			}
		}
		return languages;
	}

	protected List<GenericDto> getConfiguredLanguages(List<String> langCodes) {
		ResourceBundle resourceBundle = ResourceBundle.getBundle(RegistrationConstants.LABELS, Locale.getDefault());
		List<GenericDto> languages = new ArrayList<>();
		for(String langCode : langCodes) {
			languages.add(new GenericDto(langCode,
					resourceBundle.containsKey(langCode) ? resourceBundle.getString(langCode) : langCode,
					langCode));
		}
		return languages;
	}

	protected List<String> getConfiguredLangCodes() {
		try {
			return baseService.getConfiguredLangCodes();
		} catch (PreConditionCheckException e) {
			generateAlert(RegistrationConstants.ERROR, "Both Mandatory and Optional languages not configured");
		}
		return Collections.EMPTY_LIST;
	}


	public void setImage(ImageView imageView, String imageName) {

		if (imageView != null) {
			Image image;
			try {
				image = getImage(imageName, false);
				if (image != null) {

					imageView.setImage(image);
				}
			} catch (RegBaseCheckedException e) {
				LOGGER.error("Exception while Getting Image");
			}

		}
	}

	public Image getImage(String imageName, boolean canDefault) throws RegBaseCheckedException {

		if (imageName == null || imageName.isEmpty()) {
			throw new RegBaseCheckedException();
		}


		try {

			return getImage(getImageFilePath(getConfiguredFolder(),imageName));
		} catch (RegBaseCheckedException exception) {

			if(canDefault) {
				return getImage(getImageFilePath(RegistrationConstants.IMAGES,imageName));
			} else {
				throw exception;
			}
		}


	}

	private Image getImage(String uri) throws RegBaseCheckedException {
		try {
			return  new Image(getClass().getResourceAsStream(uri));
		} catch (Exception exception) {
			LOGGER.error("Exception while Getting Image "+ uri, exception);
			throw new RegBaseCheckedException();
		}
	}

	private String getConfiguredFolder() {
		return RegistrationConstants.IMAGES.concat(imagesTheme !=null && !imagesTheme.isBlank() ? "_"+imagesTheme : "");
	}

	public String getImageFilePath(String configFolder,String imageName) {
		String[] names = imageName.split("\\/|\\\\");
		return String.format(TEMPLATE, configFolder, String.join("/", names));
	}

	public String getImagePath(String imageName, boolean canDefault) throws RegBaseCheckedException {
		if (imageName == null || imageName.isEmpty()) {
			throw new RegBaseCheckedException();
		}
		return getImageFilePath(getConfiguredFolder(),imageName);
	}

	public void changeNodeOrientation(Node node) {
		if (node != null && applicationContext.isPrimaryLanguageRightToLeft()) {
			node.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
		}
	}

	public Image getImage(BufferedImage bufferedImage) {
		WritableImage wr = null;
		if (bufferedImage != null) {
			wr = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
			PixelWriter pw = wr.getPixelWriter();
			for (int x = 0; x < bufferedImage.getWidth(); x++) {
				for (int y = 0; y < bufferedImage.getHeight(); y++) {
					pw.setArgb(x, y, bufferedImage.getRGB(x, y));
				}
			}
		}
		return wr;
	}

	/**
	 * This method will remove the auth method from list
	 *
	 * @param authList    authentication list
	 * @param flag configuration flag
	 * @param authCode    auth mode
	 */
	protected void removeAuthModes(List<String> authList, String flag, String authCode) {

		LOGGER.info(LoggerConstants.LOG_REG_AUTH, APPLICATION_NAME, APPLICATION_ID,
				"Ignoring FingerPrint, Iris, Face Authentication if the configuration is off");

		authList.removeIf(auth -> authList.size() > 1 && RegistrationConstants.DISABLE.equalsIgnoreCase(flag)
				&& auth.equalsIgnoreCase(authCode));
	}

	protected boolean haveToSaveAuthToken(String userId) {
		return SessionContext.userId().equals(userId);
	}

	/**
	 * to capture and validate the fingerprint for authentication
	 *
	 * @param userId - username entered in the textfield
	 * @return true/false after validating fingerprint
	 * @throws IOException
	 * @throws RegBaseCheckedException
	 */
	protected boolean captureAndValidateFP(String userId, boolean isPacketAuth, boolean isReviewer)
			throws RegBaseCheckedException, IOException {
		String authSlab = io.mosip.registration.context.ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.AUTH_FINGERPRINT_SLAB);
		if(authSlab == null) { authSlab = RegistrationConstants.FINGERPRINT_SLAB_LEFT; }

		MDMRequestDto mdmRequestDto = new MDMRequestDto(authSlab, null,
				"Registration",
				io.mosip.registration.context.ApplicationContext
						.getStringValueFromApplicationMap(RegistrationConstants.SERVER_ACTIVE_PROFILE),
				io.mosip.registration.context.ApplicationContext
						.getIntValueFromApplicationMap(RegistrationConstants.CAPTURE_TIME_OUT),
				1, io.mosip.registration.context.ApplicationContext.getIntValueFromApplicationMap(
				RegistrationConstants.FINGERPRINT_AUTHENTICATION_THRESHOLD));

		List<BiometricsDto> biometrics = bioService.captureModalityForAuth(mdmRequestDto);
		boolean fpMatchStatus = authenticationService.authValidator(userId, SingleType.FINGER.value(), biometrics);
		if (fpMatchStatus && isPacketAuth) {
			addOperatorBiometrics(biometrics, isReviewer);
		}
		return fpMatchStatus;
	}

	/**
	 * to capture and validate the iris for authentication
	 *
	 * @param userId - username entered in the textfield
	 * @return true/false after validating iris
	 * @throws IOException
	 */
	protected boolean captureAndValidateIris(String userId, boolean isPacketAuth, boolean isReviewer) throws RegBaseCheckedException, IOException {
		MDMRequestDto mdmRequestDto = new MDMRequestDto(RegistrationConstants.IRIS_DOUBLE, null, "Registration",
				io.mosip.registration.context.ApplicationContext
						.getStringValueFromApplicationMap(RegistrationConstants.SERVER_ACTIVE_PROFILE),
				io.mosip.registration.context.ApplicationContext
						.getIntValueFromApplicationMap(RegistrationConstants.CAPTURE_TIME_OUT),
				2, io.mosip.registration.context.ApplicationContext
				.getIntValueFromApplicationMap(RegistrationConstants.IRIS_AUTHENTICATION_THRESHOLD));
		List<BiometricsDto> biometrics = bioService.captureModalityForAuth(mdmRequestDto);

		boolean match = authenticationService.authValidator(userId, SingleType.IRIS.value(), biometrics);
		if (match && isPacketAuth) {
			addOperatorBiometrics(biometrics, isReviewer);
		}
		return match;
	}

	/**
	 * to capture and validate the iris for authentication
	 *
	 * @param userId - username entered in the textfield
	 * @return true/false after validating face
	 * @throws IOException
	 * @throws RegBaseCheckedException
	 */
	protected boolean captureAndValidateFace(String userId, boolean isPacketAuth, boolean isReviewer) throws RegBaseCheckedException, IOException {
		MDMRequestDto mdmRequestDto = new MDMRequestDto(RegistrationConstants.FACE_FULLFACE, null, "Registration",
				io.mosip.registration.context.ApplicationContext
						.getStringValueFromApplicationMap(RegistrationConstants.SERVER_ACTIVE_PROFILE),
				io.mosip.registration.context.ApplicationContext
						.getIntValueFromApplicationMap(RegistrationConstants.CAPTURE_TIME_OUT),
				1, io.mosip.registration.context.ApplicationContext
				.getIntValueFromApplicationMap(RegistrationConstants.FACE_AUTHENTICATION_THRESHOLD));

		List<BiometricsDto> biometrics = bioService.captureModalityForAuth(mdmRequestDto);

		boolean match = authenticationService.authValidator(userId, SingleType.FACE.value(), biometrics);
		if (match && isPacketAuth) {
			addOperatorBiometrics(biometrics, isReviewer);
		}
		return match;
	}

	private void addOperatorBiometrics(List<BiometricsDto> biometrics, boolean isReviewer) {
		if (isReviewer) {
			RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.getInstance().getMapObject()
					.get(RegistrationConstants.REGISTRATION_DATA);
			registrationDTO.addSupervisorBiometrics(biometrics);
		} else {
			RegistrationDTO registrationDTO = (RegistrationDTO) SessionContext.getInstance().getMapObject()
					.get(RegistrationConstants.REGISTRATION_DATA);
			registrationDTO.addOfficerBiometrics(biometrics);
		}
	}

	protected void showAlertAndLogout() {
		/* Generate alert */
		Alert logoutAlert = createAlert(AlertType.INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SYNC_SUCCESS),RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.ALERT_NOTE_LABEL),
				RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.LOGOUT_ALERT),
				RegistrationConstants.OK_MSG, null);

		logoutAlert.show();
		Rectangle2D screenSize = Screen.getPrimary().getVisualBounds();
		Double xValue = screenSize.getWidth()/2 - logoutAlert.getWidth() + 250;
		Double yValue = screenSize.getHeight()/2 - logoutAlert.getHeight();
		logoutAlert.hide();
		logoutAlert.setX(xValue);
		logoutAlert.setY(yValue);
		logoutAlert.showAndWait();

		/* Get Option from user */
		ButtonType result = logoutAlert.getResult();
		if (result == ButtonType.OK) {
			headerController.logout();
		}
	}
}
