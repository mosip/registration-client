package io.mosip.registration.controller.eodapproval;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.device.Streamer;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.UserDTO;
import io.mosip.registration.enums.Role;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.service.login.LoginService;
import io.mosip.registration.service.security.AuthenticationService;
import io.mosip.registration.util.common.OTPManager;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

@Controller
public class EODAuthenticationController extends BaseController implements Initializable {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(EODAuthenticationController.class);

	@FXML
	private GridPane pwdBasedLogin;
	@FXML
	private GridPane otpBasedLogin;
	@FXML
	private GridPane fingerprintBasedLogin;
	@FXML
	private GridPane irisBasedLogin;
	@FXML
	private GridPane faceBasedLogin;
	@FXML
	private GridPane errorPane;
	@FXML
	private Label errorLabel;
	@FXML
	private Label errorText1;
	@FXML
	private Label errorText2;
	@FXML
	private Label otpValidity;
	@FXML
	private TextField fpUserId;
	@FXML
	private TextField irisUserId;
	@FXML
	private TextField faceUserId;
	@FXML
	private TextField username;
	@FXML
	private TextField password;
	@FXML
	private TextField otpUserId;
	@FXML
	private TextField otp;
	@FXML
	private Label otpLabel;
	@FXML
	private Label fpLabel;
	@FXML
	private Label irisLabel;
	@FXML
	private Label photoLabel;
	@FXML
	private Label pwdLabel;
	@FXML
	private Button getOTP;
	@FXML
	private ImageView irisImageView;
	@FXML
	private ImageView documentCloseImgPwdBasedLogin;
	@FXML
	private ImageView documentCloseImgOtp;
	@FXML
	private ImageView documentCloseImgFp;
	@FXML
	private ImageView rightHandImgFpUserId;
	@FXML
	private ImageView fpScanImageView;
	@FXML
	private ImageView documentCloseImgVwAuthpageTitle;
	@FXML
	private ImageView irisScanImageView;
	@FXML
	private ImageView exitWindowImgView;
	@FXML
	private ImageView faceStreamImageView;
	@FXML
	private ImageView faceScanImageView;
	@FXML
	private ImageView exitWindowImgVwAuthPageTitle;
	@FXML
	private GridPane progressIndicatorPane;
	@FXML
	private ProgressIndicator progressIndicator;
	@FXML
	private ImageView faceImage;
	@FXML
	private GridPane authenticaion;

	@Autowired
	private OTPManager otpManager;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private LoginService loginService;

	@Autowired
	private Validations validations;

	@Autowired
	private Streamer streamer;

	@Autowired
	private BaseController baseController;

	@Autowired
	private BioService bioService;

	private List<String> userAuthenticationTypeList;

	private int authCount = 0;

	private String userNameField;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setImage(irisImageView, RegistrationConstants.EYE_IMG);
		setImage(faceImage, RegistrationConstants.PHOTO_IMG);
		setImage(documentCloseImgPwdBasedLogin, RegistrationConstants.CLOSE_IMG);
		setImage(documentCloseImgOtp, RegistrationConstants.CLOSE_IMG);
		setImage(documentCloseImgFp, RegistrationConstants.CLOSE_IMG);
		setImage(rightHandImgFpUserId, RegistrationConstants.RIGHT_HAND_IMG);
		setImage(fpScanImageView, RegistrationConstants.SCAN_IMG);
		setImage(documentCloseImgVwAuthpageTitle, RegistrationConstants.CLOSE_IMG);
		setImage(irisScanImageView, RegistrationConstants.SCAN_IMG);
		setImage(exitWindowImgView, RegistrationConstants.CLOSE_IMG);
		setImage(faceStreamImageView, RegistrationConstants.SCAN_IMG);
		setImage(faceScanImageView, RegistrationConstants.SCAN_IMG);
		setImage(exitWindowImgVwAuthPageTitle, RegistrationConstants.CLOSE_IMG);

		int otpExpirySeconds = Integer
				.parseInt((getValueFromApplicationContext(RegistrationConstants.OTP_EXPIRY_TIME)).trim());
		int minutes = otpExpirySeconds / 60;
		String seconds = String.valueOf(otpExpirySeconds % 60);
		seconds = seconds.length() < 2 ? "0" + seconds : seconds;
		otpValidity.setText(
				RegistrationUIConstants.getMessageLanguageSpecific("OTP_VALIDITY") + " " + minutes + ":" + seconds);
		stopTimer();
	}

	/**
	 * Setting the init method to the Basecontroller
	 *
	 * @param parentControllerObj - Parent Controller name
	 * @param authType            - Authentication Type
	 * @throws RegBaseCheckedException
	 */
	public void init(BaseController parentControllerObj, String authType) throws RegBaseCheckedException {
		authCount = 0;
		baseController = parentControllerObj;
		getAuthenticationModes(authType);
	}

	/**
	 * to get the configured modes of authentication
	 *
	 * @throws RegBaseCheckedException
	 */
	private void getAuthenticationModes(String authType) throws RegBaseCheckedException {
		LOGGER.info("Loading configured modes of authentication");

		Set<String> roleSet = new HashSet<>(SessionContext.userContext().getRoles());

		userAuthenticationTypeList = loginService.getModesOfLogin(authType, roleSet);

		if (userAuthenticationTypeList.isEmpty()) {
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants
					.getMessageLanguageSpecific(RegistrationUIConstants.AUTHENTICATION_ERROR_MSG));
			throw new RegBaseCheckedException();
		} else {
			LOGGER.info("Ignoring FingerPrint, Iris, Face Authentication if the configuration is off");

			removeAuthModes(userAuthenticationTypeList,
					getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_DISABLE_FLAG),
					RegistrationConstants.FINGERPRINT);
			removeAuthModes(userAuthenticationTypeList,
					getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG),
					RegistrationConstants.IRIS);
			removeAuthModes(userAuthenticationTypeList,
					getValueFromApplicationContext(RegistrationConstants.FACE_DISABLE_FLAG),
					RegistrationConstants.FACE);

			loadNextScreen();
		}
	}

	/**
	 * to load the respective screen with respect to the list of configured
	 * authentication modes
	 */
	private void loadNextScreen() {
		LOGGER.info("Loading next authentication screen");
		try {
			if (!SessionContext.userMap().isEmpty()) {
				if (SessionContext.userMap().get(RegistrationConstants.IS_LOW_QUALITY_BIOMETRICS) == null) {
					SessionContext.userMap().put(RegistrationConstants.IS_LOW_QUALITY_BIOMETRICS, false);
				}
			}
			if (!userAuthenticationTypeList.isEmpty()) {
				authCount++;
				String authenticationType = String
						.valueOf(userAuthenticationTypeList.get(RegistrationConstants.PARAM_ZERO));
				if (authenticationType.equalsIgnoreCase(RegistrationConstants.OTP)) {
					getOTP.setVisible(true);
				}
				loadAuthenticationScreen(authenticationType);
			} else {
				baseController.updateAuthenticationStatus();
			}
		} catch (Exception exception) {
			LOGGER.error("Exception in loading the authentication screen", exception);
		}
	}

	/**
	 * to enable the respective authentication mode
	 *
	 * @param loginMode - name of authentication mode
	 */
	public void loadAuthenticationScreen(String loginMode) {
		LOGGER.info("Loading the respective authentication screen in UI >> " + loginMode);
		errorPane.setVisible(false);
		pwdBasedLogin.setVisible(false);
		otpBasedLogin.setVisible(false);
		fingerprintBasedLogin.setVisible(false);
		faceBasedLogin.setVisible(false);
		irisBasedLogin.setVisible(false);

		switch (loginMode.toUpperCase()) {
		case RegistrationConstants.OTP:
			enableOTP();
			break;
		case RegistrationConstants.PWORD:
			enablePWD();
			break;
		case RegistrationConstants.FINGERPRINT_UPPERCASE:
			enableFingerPrint();
			break;
		case RegistrationConstants.IRIS:
			enableIris();
			break;
		case RegistrationConstants.FACE:
			enableFace();
			break;
		default:
			enablePWD();
		}

		userAuthenticationTypeList.remove(RegistrationConstants.PARAM_ZERO);
	}

	/**
	 * to enable the OTP based authentication mode and disable rest of modes
	 */
	private void enableOTP() {
		LOGGER.info("Enabling OTP based Authentication Screen in UI");

		otpLabel.setText(
				ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("supervisorOtpAuth"));
		otpBasedLogin.setVisible(true);
		otp.clear();
		otpUserId.clear();
		otpUserId.setEditable(false);
		if (authCount > 1 && !userNameField.isEmpty()) {
			otpUserId.setText(userNameField);
		} else {
			otpUserId.setEditable(true);
		}
	}

	/**
	 * to enable the password based authentication mode and disable rest of modes
	 */
	private void enablePWD() {
		LOGGER.info("Enabling Password based Authentication Screen in UI");

		pwdLabel.setText(
				ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("supervisorPwdAuth"));
		pwdBasedLogin.setVisible(true);
		username.clear();
		password.clear();
		username.setEditable(false);
		if (authCount > 1 && !userNameField.isEmpty()) {
			username.setText(userNameField);
		} else {
			username.setEditable(true);
		}
	}

	/**
	 * to enable the fingerprint based authentication mode and disable rest of modes
	 */
	private void enableFingerPrint() {
		LOGGER.info("Enabling Fingerprint based Authentication Screen in UI");

		fpLabel.setText(
				ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("supervisorFpAuth"));
		fingerprintBasedLogin.setVisible(true);
		fpUserId.clear();
		fpUserId.setEditable(false);
		if (authCount > 1 && !userNameField.isEmpty()) {
			fpUserId.setText(userNameField);
		} else {
			fpUserId.setEditable(true);
		}
	}

	/**
	 * to enable the iris based authentication mode and disable rest of modes
	 */
	private void enableIris() {
		LOGGER.info("Enabling Iris based Authentication Screen in UI");

		irisLabel.setText(
				ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("supervisorIrisAuth"));
		irisBasedLogin.setVisible(true);
		irisUserId.clear();
		irisUserId.setEditable(false);
		if (authCount > 1 && !userNameField.isEmpty()) {
			irisUserId.setText(userNameField);
		} else {
			irisUserId.setEditable(true);
		}
	}

	/**
	 * to enable the face based authentication mode and disable rest of modes
	 */
	private void enableFace() {
		LOGGER.info("Enabling Face based Authentication Screen in UI");

		photoLabel.setText(
				ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("supervisorPhotoAuth"));
		faceBasedLogin.setVisible(true);
		faceUserId.clear();
		faceUserId.setEditable(false);
		if (authCount > 1 && !userNameField.isEmpty()) {
			faceUserId.setText(userNameField);
		} else {
			faceUserId.setEditable(true);
		}
	}

	/**
	 * to generate OTP in case of OTP based authentication
	 */
	public void generateOtp() {
		auditFactory.audit(AuditEvent.REG_SUPERVISOR_AUTH_GET_OTP, Components.REG_OS_AUTH, otpUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		LOGGER.info("Generate OTP for OTP based Authentication");

		if (!otpUserId.getText().isEmpty()) {
			// Response obtained from server
			ResponseDTO responseDTO = null;

			// Service Layer interaction
			responseDTO = otpManager.getOTP(otpUserId.getText());
			if (responseDTO.getSuccessResponseDTO() != null) {
				// Enable submit button
				// Generate alert to show OTP
				getOTP.setVisible(false);
				generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants
						.getMessageLanguageSpecific(RegistrationUIConstants.OTP_GENERATION_SUCCESS_MESSAGE));
			} else if (responseDTO.getErrorResponseDTOs() != null) {
				// Generate Alert to show INVALID USERNAME
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants
						.getMessageLanguageSpecific(RegistrationUIConstants.OTP_GENERATION_ERROR_MESSAGE));
			}
		} else {
			// Generate Alert to show username field was empty
			generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.USERNAME_FIELD_EMPTY));
		}
	}

	/**
	 * to validate OTP in case of OTP based authentication
	 */
	public void validateOTP() {
		auditFactory.audit(AuditEvent.REG_SUPERVISOR_AUTH_SUBMIT_OTP, Components.REG_OS_AUTH,
				otpUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : otpUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		LOGGER.info("Validating OTP for OTP based Authentication");

		if (validations.validateTextField(authenticaion, otp, otp.getId(), true,
				ApplicationContext.applicationLanguage())) {
			if (validateInput(otpUserId, null)) {
				if (null != authenticationService.authValidator(RegistrationConstants.OTP, otpUserId.getText(),
						otp.getText(), haveToSaveAuthToken(otpUserId.getText()))) {
					loadNextScreen();
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants
							.getMessageLanguageSpecific(RegistrationUIConstants.OTP_VALIDATION_ERROR_MESSAGE));
				}
			}
		}
	}

	public void validatePwd() {
		auditFactory.audit(AuditEvent.REG_SUPERVISOR_AUTH_PASSWORD,
				Components.REG_OS_AUTH,
				username.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : username.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		if (validateInput(username, password)) {
			String status = RegistrationConstants.EMPTY;
			status = validatePwd(username.getText(), password.getText());
			if (RegistrationConstants.SUCCESS.equals(status)) {
				loadNextScreen();
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants
						.getMessageLanguageSpecific(RegistrationUIConstants.AUTHENTICATION_FAILURE));
			}
		}
	}

	/**
	 * to validate the fingerprint in case of fingerprint based authentication
	 */
	public void validateFingerprint() {
		auditFactory.audit(AuditEvent.REG_SUPERVISOR_AUTH_FINGERPRINT,
				Components.REG_OS_AUTH,
				fpUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : fpUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		LOGGER.info("Validating Fingerprint for Fingerprint based Authentication");

		if (validateInput(fpUserId, null)) {
			executeFPValidationTask(fpUserId.getText(), fingerprintBasedLogin);
		}
	}

	private void executeFPValidationTask(String userId, GridPane pane) {
		pane.setDisable(true);
		progressIndicatorPane.setVisible(true);
		progressIndicator.setVisible(true);

		Service<Boolean> taskService = new Service<Boolean>() {
			@Override
			protected Task<Boolean> createTask() {
				return new Task<Boolean>() {
					/*
					 * (non-Javadoc)
					 *
					 * @see javafx.concurrent.Task#call()
					 */
					@Override
					protected Boolean call() {
						try {
							return captureAndValidateFP(userId, false, false);
						} catch (RegBaseCheckedException | IOException exception) {
							LOGGER.error("AuthenticationController", APPLICATION_NAME, APPLICATION_ID,
									"Exception while getting the scanned biometrics for user authentication: caused by "
											+ ExceptionUtils.getStackTrace(exception));
							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants
									.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR));
						}
						return false;
					}
				};
			}
		};

		progressIndicator.progressProperty().bind(taskService.progressProperty());
		taskService.start();
		taskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				pane.setDisable(false);
				progressIndicatorPane.setVisible(false);
				progressIndicator.setVisible(false);
				if (taskService.getValue()) {
					generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants
							.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS));

					loadNextScreen();
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants
							.getMessageLanguageSpecific(RegistrationUIConstants.FINGER_PRINT_MATCH));
				}

			}
		});

	}

	/**
	 * to validate the iris in case of iris based authentication
	 */
	public void validateIris() {
		auditFactory.audit(AuditEvent.REG_SUPERVISOR_AUTH_IRIS,
				Components.REG_OS_AUTH,
				irisUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : irisUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		LOGGER.info("Validating Iris for Iris based Authentication");

		if (validateInput(irisUserId, null)) {
			executeIrisValidationTask(irisUserId.getText(), irisBasedLogin);
		}
	}

	private void executeIrisValidationTask(String userId, GridPane pane) {
		pane.setDisable(true);
		progressIndicatorPane.setVisible(true);
		progressIndicator.setVisible(true);

		Service<Boolean> taskService = new Service<Boolean>() {
			@Override
			protected Task<Boolean> createTask() {
				return new Task<Boolean>() {
					/*
					 * (non-Javadoc)
					 *
					 * @see javafx.concurrent.Task#call()
					 */
					@Override
					protected Boolean call() {
						try {
							return captureAndValidateIris(userId, false, false);
						} catch (RegBaseCheckedException | IOException exception) {
							LOGGER.error("AuthenticationController", APPLICATION_NAME, APPLICATION_ID,
									"Exception while getting the scanned biometrics for user authentication: caused by "
											+ ExceptionUtils.getStackTrace(exception));
							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants
									.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND));
						}
						return false;
					}
				};
			}
		};

		progressIndicator.progressProperty().bind(taskService.progressProperty());
		taskService.start();
		taskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				pane.setDisable(false);
				progressIndicatorPane.setVisible(false);
				progressIndicator.setVisible(false);
				if (taskService.getValue()) {
					generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants
							.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS));
					loadNextScreen();
				} else {
					generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.IRIS_MATCH));
				}
			}
		});
	}

	@FXML
	private void startStream() {
		faceImage.setImage(null);
		try {
			streamer.startStream(bioService.getStream(RegistrationConstants.FACE_FULLFACE), faceImage, null);
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error(LoggerConstants.LOG_REG_LOGIN, APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
	}

	/**
	 * to validate the face in case of face based authentication
	 *
	 * @throws IOException
	 * @throws RegBaseCheckedException
	 */
	public void validateFace() {
		auditFactory.audit(AuditEvent.REG_SUPERVISOR_AUTH_FACE,
				Components.REG_OS_AUTH,
				faceUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : faceUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		
		LOGGER.info("Validating Face for Face based Authentication");

		if (validateInput(faceUserId, null)) {
			executeFaceValidationTask(faceUserId.getText(), faceBasedLogin);
		}
	}

	private void executeFaceValidationTask(String userId, GridPane pane) {
		pane.setDisable(true);
		progressIndicatorPane.setVisible(true);
		progressIndicator.setVisible(true);

		Service<Boolean> taskService = new Service<Boolean>() {
			@Override
			protected Task<Boolean> createTask() {
				return new Task<Boolean>() {
					/*
					 * (non-Javadoc)
					 *
					 * @see javafx.concurrent.Task#call()
					 */
					@Override
					protected Boolean call() {
						try {
							return captureAndValidateFace(userId, false, false);
						} catch (RegBaseCheckedException | IOException exception) {
							LOGGER.error("AuthenticationController", APPLICATION_NAME, APPLICATION_ID,
									"Exception while getting the scanned biometrics for user authentication: caused by "
											+ ExceptionUtils.getStackTrace(exception));
							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants
									.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR));
						}
						return false;
					}
				};
			}
		};

		progressIndicator.progressProperty().bind(taskService.progressProperty());
		taskService.start();
		taskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				pane.setDisable(false);
				progressIndicatorPane.setVisible(false);
				progressIndicator.setVisible(false);
				if (taskService.getValue()) {
					generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants
							.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS));
					loadNextScreen();

				} else {
					generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.FACE_MATCH));
				}

			}
		});
	}
	
	/**
	 * to check the role of supervisor in case of EOD authentication
	 *
	 * @param userId - username entered by the supervisor in the authentication
	 *               screen
	 * @return boolean variable "true", if the person is authenticated as supervisor
	 *         or "false", if not
	 */
	protected boolean fetchUserRole(String userId) {
		LOGGER.info("Fetching the user role in case of Supervisor Authentication");

		UserDTO userDTO = loginService.getUserDetail(userId);
		if (userDTO != null) {
			return userDTO.getUserRole().stream()
					.anyMatch(userRole -> Role.getSupervisorAuthRoles().contains(userRole.getRoleCode()));
		}
		return false;
	}
	
	/**
	 * event class to exit from authentication window. pop up window.
	 *
	 * @param event - the action event
	 */
	public void exitWindow(ActionEvent event) {
		Stage primaryStage = (Stage) ((Node) event.getSource()).getParent().getScene().getWindow();
		primaryStage.close();
	}
	
	private boolean validateInput(TextField userId, TextField pword) {
		boolean isValid = false;
		if (!userId.getText().isEmpty()) {
			isValid = true;
			if (!fetchUserRole(userId.getText())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants
						.getMessageLanguageSpecific(RegistrationUIConstants.USER_NOT_AUTHORIZED));
				return false;
			} else {
				userNameField = userId.getText();
			}
			if (pword != null && pword.getText().isEmpty()) {
				isValid = false;
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PWORD_FIELD_EMPTY));
			}
		} else {
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.USERNAME_FIELD_EMPTY));
		}
		return isValid;
	}
}