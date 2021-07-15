package io.mosip.registration.controller.auth;

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
import io.mosip.registration.constants.ProcessNames;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.device.Streamer;
import io.mosip.registration.controller.reg.PacketHandlerController;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.OSIDataDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.UserDTO;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * Class for Operator Authentication
 *
 *
 *
 *
 */
@Controller
public class AuthenticationController extends BaseController implements Initializable {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(AuthenticationController.class);

	@FXML
	private AnchorPane temporaryLogin;
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
	private GridPane operatorAuthenticationPane;
	@FXML
	private Button operatorAuthContinue;
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
	private ImageView continueImageView;
	@FXML
	private ImageView rightHandImageView;
	@FXML
	private Button backBtn;
	@FXML
	private ImageView backImageView;	
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
	private Label authCounter;
	@FXML
	private Button irisScanButton;
	@FXML
	private Button faceScanButton;
	@FXML
	private Button fingerPrintScanButton;
	@FXML
	private ImageView faceImage;

	@Autowired
	private PacketHandlerController packetHandlerController;

	@Autowired
	private RegistrationController registrationController;

	@Autowired
	private OTPManager otpManager;

	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private LoginService loginService;

	@Autowired
	private Validations validations;
	
	private boolean isReviewer = false;

	private List<String> userAuthenticationTypeList;

	private List<String> userAuthenticationTypeListValidation;

	private List<String> userAuthenticationTypeListSupervisorValidation;

	private int authCount = 0;

	private String userNameField;

	@Autowired
	private BioService bioService;

	private int fingerPrintAuthCount;
	private int irisPrintAuthCount;
	private int facePrintAuthCount;

	@Autowired
	private Streamer streamer;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setImageOnHover();

		setImage(backImageView, RegistrationConstants.ARROW_LEFT_IMG);
		setImage(continueImageView, RegistrationConstants.ARROW_RIGHT_IMG);
		setImage(rightHandImageView, RegistrationConstants.RIGHT_HAND_IMG);
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
		otpValidity.setText(RegistrationUIConstants.getMessageLanguageSpecific("OTP_VALIDITY") + " " + minutes + ":" + seconds);
		stopTimer();
	}

	/**
	 * to generate OTP in case of OTP based authentication
	 */
	public void generateOtp() {
		auditFactory.audit(isReviewer ? AuditEvent.REG_REVIEWER_AUTH_GET_OTP : AuditEvent.REG_OPERATOR_AUTH_GET_OTP,
				Components.REG_OS_AUTH, otpUserId.getText(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		LOGGER.info("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
				"Generate OTP for OTP based Authentication");

		if (!otpUserId.getText().isEmpty()) {
			// Response obtained from server
			ResponseDTO responseDTO = null;

			// Service Layer interaction
			responseDTO = otpManager.getOTP(otpUserId.getText());
			if (responseDTO.getSuccessResponseDTO() != null) {
				// Enable submit button
				// Generate alert to show OTP
				getOTP.setVisible(false);
				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.OTP_GENERATION_SUCCESS_MESSAGE));
			} else if (responseDTO.getErrorResponseDTOs() != null) {
				// Generate Alert to show INVALID USERNAME
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.OTP_GENERATION_ERROR_MESSAGE));
			}
		} else {
			// Generate Alert to show username field was empty
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.USERNAME_FIELD_EMPTY));
		}
	}

	/**
	 * to validate OTP in case of OTP based authentication
	 */
	public void validateOTP() {
		auditFactory.audit(
				isReviewer ? AuditEvent.REG_REVIEWER_AUTH_SUBMIT_OTP : AuditEvent.REG_OPERATOR_AUTH_SUBMIT_OTP,
				Components.REG_OS_AUTH,
				otpUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : otpUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		LOGGER.info("Validating OTP for OTP based Authentication");

		if (validations.validateTextField(operatorAuthenticationPane, otp, otp.getId(), true,ApplicationContext.applicationLanguage())) {
			if (isReviewer) {
				if (!otpUserId.getText().isEmpty()) {
					if (fetchUserRole(otpUserId.getText())) {
						if (null != authenticationService.authValidator(RegistrationConstants.OTP, otpUserId.getText(),
								otp.getText(), haveToSaveAuthToken(otpUserId.getText()))) {
							userAuthenticationTypeListValidation.remove(0);
							userNameField = otpUserId.getText();
							getOSIData().setSupervisorID(userNameField);
							getOSIData().setSuperviorAuthenticatedByPIN(true);
							loadNextScreen();
						} else {
							generateAlert(RegistrationConstants.ERROR,
									RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.OTP_VALIDATION_ERROR_MESSAGE));
						}
					} else {
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.REVIEWER_NOT_AUTHORIZED));
					}
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.USERNAME_FIELD_EMPTY));
				}
			} else {
				if (null != authenticationService.authValidator(RegistrationConstants.OTP, otpUserId.getText(),
						otp.getText(), haveToSaveAuthToken(otpUserId.getText()))) {
					getOSIData().setOperatorAuthenticatedByPIN(true);
					userAuthenticationTypeListValidation.remove(0);
					loadNextScreen();
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.OTP_VALIDATION_ERROR_MESSAGE));
				}
			}
		}
	}

	public void validatePwd() {
		auditFactory.audit(
				isReviewer ? AuditEvent.REG_REVIEWER_AUTH_PASSWORD : AuditEvent.REG_OPERATOR_AUTH_PASSWORD,
				Components.REG_OS_AUTH,
				username.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : username.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		String status = RegistrationConstants.EMPTY;
		if (isReviewer) {
			if (!username.getText().isEmpty()) {
				if (fetchUserRole(username.getText())) {
					if (password.getText().isEmpty()) {
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PWORD_FIELD_EMPTY));
					} else {
						status = validatePwd(username.getText(), password.getText());
						if (RegistrationConstants.SUCCESS.equals(status)) {
							userAuthenticationTypeListValidation.remove(0);
							userNameField = username.getText();
							getOSIData().setSupervisorID(userNameField);
							getOSIData().setSuperviorAuthenticatedByPassword(true);
							loadNextScreen();
						} else if (RegistrationConstants.FAILURE.equals(status)) {
							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHENTICATION_FAILURE));
						} else if(RegistrationConstants.CREDS_NOT_FOUND.equals(status)) {
							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.CREDENTIALS_NOT_FOUND));
						}
					}
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.REVIEWER_NOT_AUTHORIZED));
				}
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.USERNAME_FIELD_EMPTY));
			}
		} else {
			if (!username.getText().isEmpty()) {
				if (password.getText().isEmpty()) {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PWORD_FIELD_EMPTY));
				} else {
					status = validatePwd(username.getText(), password.getText());
					if (RegistrationConstants.SUCCESS.equals(status)) {
						userAuthenticationTypeListValidation.remove(0);
						userNameField = username.getText();
						getOSIData().setOperatorID(userNameField);
						getOSIData().setOperatorAuthenticatedByPassword(true);
						loadNextScreen();
					} else if (RegistrationConstants.FAILURE.equals(status)) {
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHENTICATION_FAILURE));
					}
				}
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.USERNAME_FIELD_EMPTY));
			}
		}
	}

	/**
	 * to validate the fingerprint in case of fingerprint based authentication
	 */
	public void validateFingerprint() {
		auditFactory.audit(
				isReviewer ? AuditEvent.REG_REVIEWER_AUTH_FINGERPRINT : AuditEvent.REG_OPERATOR_AUTH_FINGERPRINT,
				Components.REG_OS_AUTH,
				fpUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : fpUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		authCounter = new Label();

		LOGGER.info("Validating Fingerprint for Fingerprint based Authentication");

		if (isReviewer) {
			if (!fpUserId.getText().isEmpty()) {
				if (fetchUserRole(fpUserId.getText())) {
					executeFPValidationTask(fpUserId.getText(), operatorAuthenticationPane);
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.REVIEWER_NOT_AUTHORIZED));
				}
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.USERNAME_FIELD_EMPTY));
			}
		} else {
			executeFPValidationTask(fpUserId.getText(), operatorAuthenticationPane);
		}
		authCounter.setText(++fingerPrintAuthCount + "");
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
							return captureAndValidateFP(userId, true, isReviewer);
						} catch (RegBaseCheckedException | IOException exception) {
							LOGGER.error("AuthenticationController", APPLICATION_NAME, APPLICATION_ID,
									"Exception while getting the scanned biometrics for user authentication: caused by "
											+ ExceptionUtils.getStackTrace(exception));
							generateAlert(RegistrationConstants.ERROR,
									RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR));
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
					userAuthenticationTypeListValidation.remove(0);
					if (isReviewer) {
						userNameField = fpUserId.getText();
						getOSIData().setSupervisorID(userNameField);
					}

					if (operatorAuthContinue != null) {
						// TODO Enable continue button
						operatorAuthContinue.setDisable(false);
					}
					if (fingerPrintScanButton != null) {
						fingerPrintScanButton.setDisable(true);
					}
					generateAlert(RegistrationConstants.ALERT_INFORMATION,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS));

					loadNextScreen();
				} else {
					if (operatorAuthContinue != null) {
						operatorAuthContinue.setDisable(true);
					}
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.FINGER_PRINT_MATCH));
				}

			}
		});

	}

	/**
	 * to validate the iris in case of iris based authentication
	 */
	public void validateIris() {
		auditFactory.audit(isReviewer ? AuditEvent.REG_REVIEWER_AUTH_IRIS : AuditEvent.REG_OPERATOR_AUTH_IRIS,
				Components.REG_OS_AUTH,
				irisUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : irisUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		authCounter = new Label();

		LOGGER.info("Validating Iris for Iris based Authentication");

		if (isReviewer) {
			if (!irisUserId.getText().isEmpty()) {
				if (fetchUserRole(irisUserId.getText())) {
					executeIrisValidationTask(irisUserId.getText(), operatorAuthenticationPane);
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.REVIEWER_NOT_AUTHORIZED));
				}
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.USERNAME_FIELD_EMPTY));
			}
		} else {
			executeIrisValidationTask(irisUserId.getText(), operatorAuthenticationPane);
		}
		authCounter.setText(++irisPrintAuthCount + "");
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
							return captureAndValidateIris(userId, true, isReviewer);
						} catch (RegBaseCheckedException | IOException exception) {
							LOGGER.error("AuthenticationController", APPLICATION_NAME, APPLICATION_ID,
									"Exception while getting the scanned biometrics for user authentication: caused by "
											+ ExceptionUtils.getStackTrace(exception));
							generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND));
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
					userAuthenticationTypeListValidation.remove(0);
					if (isReviewer) {
						userNameField = irisUserId.getText();
						getOSIData().setSupervisorID(userNameField);
					}
					if (operatorAuthContinue != null) {
						operatorAuthContinue.setDisable(false);
					}
					if (irisScanButton != null) {
						irisScanButton.setDisable(true);
					}
					generateAlert(RegistrationConstants.ALERT_INFORMATION,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS));
					loadNextScreen();
				} else {
					if (operatorAuthContinue != null) {
						operatorAuthContinue.setDisable(true);
					}
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.IRIS_MATCH));
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
		auditFactory.audit(isReviewer ? AuditEvent.REG_REVIEWER_AUTH_FACE : AuditEvent.REG_OPERATOR_AUTH_FACE,
				Components.REG_OS_AUTH,
				faceUserId.getText().isEmpty() ? RegistrationConstants.AUDIT_DEFAULT_USER : faceUserId.getText(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		authCounter = new Label();

		LOGGER.info("Validating Face for Face based Authentication");

		if (isReviewer) {
			if (!faceUserId.getText().isEmpty()) {
				if (fetchUserRole(faceUserId.getText())) {
					executeFaceValidationTask(faceUserId.getText(), operatorAuthenticationPane);
				} else {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.REVIEWER_NOT_AUTHORIZED));
				}
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.USERNAME_FIELD_EMPTY));
			}
		} else {
			executeFaceValidationTask(faceUserId.getText(), operatorAuthenticationPane);
		}

		authCounter.setText(++facePrintAuthCount + "");
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
							return captureAndValidateFace(userId, true, isReviewer);
						} catch (RegBaseCheckedException | IOException exception) {
							LOGGER.error("Exception while getting the scanned biometrics for user authentication: caused by "
											+ ExceptionUtils.getStackTrace(exception));
							generateAlert(RegistrationConstants.ERROR,
									RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_SCANNING_ERROR));
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
					userAuthenticationTypeListValidation.remove(0);
					if (isReviewer) {
						userNameField = faceUserId.getText();
						getOSIData().setSupervisorID(userNameField);
					}

					if (operatorAuthContinue != null) {
						operatorAuthContinue.setDisable(false);
					}
					if (faceScanButton != null) {
						faceScanButton.setDisable(true);
					}
					generateAlert(RegistrationConstants.ALERT_INFORMATION,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_CAPTURE_SUCCESS));
					loadNextScreen();

				} else {
					if (operatorAuthContinue != null) {
						operatorAuthContinue.setDisable(true);
					}
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.FACE_MATCH));
				}

			}
		});
	}

	/**
	 * to get the configured modes of authentication
	 *
	 * @throws RegBaseCheckedException
	 */
	private void getAuthenticationModes(String authType) throws RegBaseCheckedException {
		LOGGER.info("Loading configured modes of authentication");

		Set<String> roleSet = new HashSet<>(SessionContext.userContext().getRoles());
		
		userAuthenticationTypeList = loginService.getModesOfLogin(authType, roleSet, isReviewer);
		userAuthenticationTypeListValidation = loginService.getModesOfLogin(authType, roleSet, isReviewer);
		userAuthenticationTypeListSupervisorValidation = loginService.getModesOfLogin(authType, roleSet, isReviewer);

		if (userAuthenticationTypeList.isEmpty()) {
			isReviewer = false;
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHENTICATION_ERROR_MSG));
		} else {
			LOGGER.info("Ignoring FingerPrint, Iris, Face Authentication if the configuration is off");

			String fingerprintDisableFlag = getValueFromApplicationContext(
					RegistrationConstants.FINGERPRINT_DISABLE_FLAG);
			String irisDisableFlag = getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG);
			String faceDisableFlag = getValueFromApplicationContext(RegistrationConstants.FACE_DISABLE_FLAG);

			removeAuthModes(userAuthenticationTypeList, fingerprintDisableFlag,
					RegistrationConstants.FINGERPRINT);
			removeAuthModes(userAuthenticationTypeList, irisDisableFlag, RegistrationConstants.IRIS);
			removeAuthModes(userAuthenticationTypeList, faceDisableFlag, RegistrationConstants.FACE);

			LOGGER.info(LoggerConstants.LOG_REG_AUTH, APPLICATION_NAME, APPLICATION_ID,
					"Ignoring FingerPrint, Iris, Face Supervisror Authentication if the configuration is off");

			removeAuthModes(userAuthenticationTypeListValidation, fingerprintDisableFlag,
					RegistrationConstants.FINGERPRINT);
			removeAuthModes(userAuthenticationTypeListValidation, irisDisableFlag, RegistrationConstants.IRIS);
			removeAuthModes(userAuthenticationTypeListValidation, faceDisableFlag, RegistrationConstants.FACE);

			removeAuthModes(userAuthenticationTypeListSupervisorValidation, fingerprintDisableFlag,
					RegistrationConstants.FINGERPRINT);
			removeAuthModes(userAuthenticationTypeListSupervisorValidation, irisDisableFlag,
					RegistrationConstants.IRIS);
			removeAuthModes(userAuthenticationTypeListSupervisorValidation, faceDisableFlag,
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
				if ((RegistrationConstants.DISABLE.equalsIgnoreCase(
						getValueFromApplicationContext(RegistrationConstants.FINGERPRINT_DISABLE_FLAG))
						&& authenticationType.equalsIgnoreCase(RegistrationConstants.FINGERPRINT))
						|| (RegistrationConstants.DISABLE.equalsIgnoreCase(
						getValueFromApplicationContext(RegistrationConstants.IRIS_DISABLE_FLAG))
						&& authenticationType.equalsIgnoreCase(RegistrationConstants.IRIS))
						|| (RegistrationConstants.DISABLE.equalsIgnoreCase(
						getValueFromApplicationContext(RegistrationConstants.FACE_DISABLE_FLAG))
						&& authenticationType.equalsIgnoreCase(RegistrationConstants.FACE))) {

					enableErrorPage();
					operatorAuthContinue.setDisable(true);
				} else {
					operatorAuthContinue.setDisable(false);
					loadAuthenticationScreen(authenticationType);
				}
			} else {
				if (!isReviewer) {
					/*
					 * Check whether the biometric exceptions are enabled and supervisor
					 * authentication is required
					 */
					if (!getRegistrationDTOFromSession().getBiometricExceptions().isEmpty()
							&& RegistrationConstants.ENABLE.equalsIgnoreCase(
							getValueFromApplicationContext(RegistrationConstants.SUPERVISOR_AUTH_CONFIG))) {
						authCount = 0;
						isReviewer = true;
						getAuthenticationModes(ProcessNames.EXCEPTION.getType());
					} else {
						submitRegistration();
					}
				} else {
					submitRegistration();
				}
			}
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("REGISTRATION - OPERATOR_AUTHENTICATION", APPLICATION_NAME, APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));
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
				operatorAuthContinue.setDisable(false);
				enableOTP();
				break;
			case RegistrationConstants.PWORD:
				operatorAuthContinue.setDisable(false);
				enablePWD();
				break;
			case RegistrationConstants.FINGERPRINT_UPPERCASE:
				fingerPrintScanButton.setDisable(false);
				operatorAuthContinue.setDisable(true);
				enableFingerPrint();
				break;
			case RegistrationConstants.IRIS:
				irisScanButton.setDisable(false);
				operatorAuthContinue.setDisable(true);
				enableIris();
				break;
			case RegistrationConstants.FACE:
				faceScanButton.setDisable(false);
				operatorAuthContinue.setDisable(true);
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
	private void enableErrorPage() {
		LOGGER.info("Enabling OTP based Authentication Screen in UI");

		pwdBasedLogin.setVisible(false);
		otpBasedLogin.setVisible(false);
		fingerprintBasedLogin.setVisible(false);
		irisBasedLogin.setVisible(false);
		faceBasedLogin.setVisible(false);
		errorPane.setVisible(true);
		errorPane.setDisable(false);
		errorText1.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_DISABLE_SCREEN_4));
		errorText1.setVisible(true);
		errorText2.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.BIOMETRIC_DISABLE_SCREEN_3));
		errorText1.setVisible(true);

		if (isReviewer) {
			errorLabel.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SUPERVISOR_VERIFICATION));
		}
	}

	/**
	 * to enable the OTP based authentication mode and disable rest of modes
	 */
	private void enableOTP() {
		LOGGER.info("Enabling OTP based Authentication Screen in UI");

		otpLabel.setText(
				ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("otpAuthentication"));
		otpBasedLogin.setVisible(true);
		otp.clear();
		otpUserId.clear();
		otpUserId.setEditable(false);
		if (isReviewer) {
			otpLabel.setText(
					ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("reviewerOtpAuth"));
			if (authCount > 1 && !userNameField.isEmpty()) {
				otpUserId.setText(userNameField);
			} else {
				otpUserId.setEditable(true);
			}
		} else {
			otpUserId.setText(SessionContext.userContext().getUserId());
		}
	}

	/**
	 * to enable the password based authentication mode and disable rest of modes
	 */
	private void enablePWD() {
		LOGGER.info("Enabling Password based Authentication Screen in UI");

		pwdLabel.setText(
				ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("pwdAuthentication"));
		pwdBasedLogin.setVisible(true);
		username.clear();
		password.clear();
		username.setEditable(false);
		if (isReviewer) {
			pwdLabel.setText(
					ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("reviewerPwdAuth"));
			if (authCount > 1 && !userNameField.isEmpty()) {
				username.setText(userNameField);
			} else {
				username.setEditable(true);
			}
		} else {
			username.setText(SessionContext.userContext().getUserId());
		}
	}

	/**
	 * to enable the fingerprint based authentication mode and disable rest of modes
	 */
	private void enableFingerPrint() {
		LOGGER.info("Enabling Fingerprint based Authentication Screen in UI");

		fpLabel.setText(ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("fpAuthentication"));
		fingerprintBasedLogin.setVisible(true);
		fpUserId.clear();
		fpUserId.setEditable(false);
		if (isReviewer) {
			fpLabel.setText(
					ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("reviewerFpAuth"));
			if (authCount > 1 && !userNameField.isEmpty()) {
				fpUserId.setText(userNameField);
			} else {
				fpUserId.setEditable(true);
			}
		} else {
			fpUserId.setText(SessionContext.userContext().getUserId());
		}
	}

	/**
	 * to enable the iris based authentication mode and disable rest of modes
	 */
	private void enableIris() {
		LOGGER.info("Enabling Iris based Authentication Screen in UI");

		irisLabel.setText(
				ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("irisAuthentication"));
		irisBasedLogin.setVisible(true);
		irisUserId.clear();
		irisUserId.setEditable(false);
		if (isReviewer) {
			irisLabel.setText(
					ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("reviewerIrisAuth"));
			if (authCount > 1 && !userNameField.isEmpty()) {
				irisUserId.setText(userNameField);
			} else {
				irisUserId.setEditable(true);
			}
		} else {
			irisUserId.setText(SessionContext.userContext().getUserId());
		}
	}

	/**
	 * to enable the face based authentication mode and disable rest of modes
	 */
	private void enableFace() {
		LOGGER.info("Enabling Face based Authentication Screen in UI");

		photoLabel.setText(
				ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("photoAuthentication"));
		faceBasedLogin.setVisible(true);
		faceUserId.clear();
		faceUserId.setEditable(false);
		if (isReviewer) {
			photoLabel.setText(
					ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("reviewerPhotoAuth"));
			if (authCount > 1 && !userNameField.isEmpty()) {
				faceUserId.setText(userNameField);
			} else {
				faceUserId.setEditable(true);
			}
		} else {
			faceUserId.setText(SessionContext.userContext().getUserId());
		}
	}

	

	/**
	 * to submit the registration after successful authentication
	 */
	public void submitRegistration() {
		LOGGER.info("Submit Registration after Operator Authentication");

		packetHandlerController.showReciept();
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

	public void initData(String authType) throws RegBaseCheckedException {
		authCount = 0;
		int otpExpirySeconds = Integer
				.parseInt((getValueFromApplicationContext(RegistrationConstants.OTP_EXPIRY_TIME)).trim());
		int minutes = otpExpirySeconds / 60;
		String seconds = String.valueOf(otpExpirySeconds % 60);
		seconds = seconds.length() < 2 ? "0" + seconds : seconds;
		otpValidity.setText(RegistrationUIConstants.getMessageLanguageSpecific("OTP_VALIDITY") + " " + minutes + ":" + seconds + " "
				+ RegistrationUIConstants.getMessageLanguageSpecific("MINUTES"));
		stopTimer();
		isReviewer = false;
		getAuthenticationModes(authType);
	}

	private OSIDataDTO getOSIData() {
		return getRegistrationDTOFromSession().getOsiDataDTO();
	}

	private void setImageOnHover() {
		backBtn.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(backImageView, RegistrationConstants.BACK_FOCUSED_IMG);
			} else {
				setImage(backImageView, RegistrationConstants.ARROW_LEFT_IMG);
			}
		});
	}

	public void goToPreviousPage() {
		auditFactory.audit(AuditEvent.REG_PREVIEW_BACK, Components.REG_PREVIEW, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		if (getRegistrationDTOFromSession().getSelectionListDTO() != null) {
			SessionContext.map().put(RegistrationConstants.UIN_UPDATE_OPERATORAUTHENTICATIONPANE, false);
			SessionContext.map().put(RegistrationConstants.UIN_UPDATE_REGISTRATIONPREVIEW, true);
			registrationController.showUINUpdateCurrentPage();
		} else {
			registrationController.showCurrentPage(RegistrationConstants.OPERATOR_AUTHENTICATION,
					getPageByAction(RegistrationConstants.OPERATOR_AUTHENTICATION, RegistrationConstants.PREVIOUS));
		}
	}

	public void goToNextPage() {
		if (userAuthenticationTypeListValidation.isEmpty()) {
			userAuthenticationTypeListValidation = userAuthenticationTypeListSupervisorValidation;
		}

		switch (userAuthenticationTypeListValidation.get(0).toUpperCase()) {
			case RegistrationConstants.OTP:
				validateOTP();
				break;
			case RegistrationConstants.PWORD:
				validatePwd();
				break;
			case RegistrationConstants.FINGERPRINT_UPPERCASE:
				loadNextScreen();
				break;
			case RegistrationConstants.IRIS:
				loadNextScreen();
				break;
			case RegistrationConstants.FACE:
				loadNextScreen();
				break;
			default:
		}
	}

	public void scan() {
		if (userAuthenticationTypeListValidation.isEmpty()) {
			userAuthenticationTypeListValidation = userAuthenticationTypeListSupervisorValidation;
		}

		switch (userAuthenticationTypeListValidation.get(0).toUpperCase()) {
			case RegistrationConstants.FINGERPRINT_UPPERCASE:
				validateFingerprint();
				break;
			case RegistrationConstants.IRIS:
				validateIris();
				break;
			case RegistrationConstants.FACE:
				validateFace();
				break;
			default:
		}
	}
	
	/**
	 * to check the role of reviewer in case of biometric exception
	 *
	 * @param userId - username entered by the reviewer in the authentication
	 *               screen
	 * @return boolean variable "true", if the person is authenticated as reviewer
	 *         or "false", if not
	 */
	private boolean fetchUserRole(String userId) {
		LOGGER.info("Fetching the user role in case of Reviewer Authentication");

		UserDTO userDTO = loginService.getUserDetail(userId);
		if (userDTO != null && !SessionContext.userId().equals(userId)) {
			return true;
		}
		return false;
	}

}