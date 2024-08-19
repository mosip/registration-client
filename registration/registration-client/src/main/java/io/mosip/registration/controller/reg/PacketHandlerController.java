package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.LoggerConstants.PACKET_HANDLER;
import static io.mosip.registration.constants.RegistrationConstants.*;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.controller.GenericController;
import io.mosip.registration.dao.IdentitySchemaDao;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationApprovalDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SyncDataProcessDTO;
import io.mosip.registration.dto.schema.ProcessSpecDto;
import io.mosip.registration.entity.PreRegistrationList;
import io.mosip.registration.entity.ProcessSpec;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.enums.FlowType;
import io.mosip.registration.enums.Role;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.config.JobConfigurationService;
import io.mosip.registration.service.operator.UserOnboardService;
import io.mosip.registration.service.packet.PacketHandlerService;
import io.mosip.registration.service.packet.ReRegistrationService;
import io.mosip.registration.service.packet.RegistrationApprovalService;
import io.mosip.registration.service.sync.PreRegistrationDataSyncService;
import io.mosip.registration.service.template.TemplateService;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.acktemplate.TemplateGenerator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import lombok.NonNull;

/**
 * Class for Registration Packet operations
 * 
 * @author Sravya Surampalli
 * @since 1.0.0
 *
 */
@Controller
public class PacketHandlerController extends BaseController implements Initializable {

	private static final Logger LOGGER = AppConfig.getLogger(PacketHandlerController.class);

	@FXML
	private GridPane uploadRoot;

	@FXML
	private Label pendingApprovalCountLbl;

	@FXML
	private Label reRegistrationCountLbl;

	@FXML
	private Label lastBiometricTime;

	@FXML
	private Label lastPreRegPacketDownloadedTime;
	
	@FXML
	private ImageView inProgressImage;

	@FXML
	private Label lastSyncTime;

	@FXML
	private GridPane eodProcessGridPane;

	@FXML
	private VBox vHolder;

	@FXML
	public GridPane uinUpdateGridPane;

	@FXML
	public HBox userOnboardMessage;
	@FXML
	public ProgressIndicator progressIndicator;
	@FXML
	public GridPane progressPane;
	@FXML
	public ProgressBar syncProgressBar;
	@FXML
	private Label eodLabel;
	@FXML
	private GridPane syncDataPane;
	@FXML
	private ImageView syncDataImageView;
	@FXML
	private GridPane downloadPreRegDataPane;
	@FXML
	private ImageView downloadPreRegDataImageView;
	@FXML
	private GridPane updateOperatorBiometricsPane;
	@FXML
	private ImageView updateOperatorBiometricsImageView;
	@FXML
	private GridPane eodApprovalPane;
	@FXML
	private ImageView eodApprovalImageView;
	@FXML
	private GridPane reRegistrationPane;
	@FXML
	private ImageView reRegistrationImageView;
	@FXML
	private GridPane dashBoardPane;
	@FXML
	private GridPane uploadPacketPane;
	@FXML
	private GridPane centerRemapPane;
	@FXML
	private GridPane checkUpdatesPane;
	@FXML
	private ImageView viewReportsImageView;

	@FXML
	private Label versionValueLabel;

	@FXML
	private ImageView uploadPacketImageView;

	@FXML
	private ImageView remapImageView;

	@FXML
	private ImageView checkUpdatesImageView;

	@FXML
	private ImageView tickMarkImageView;

	@FXML
	private GridPane registrationGridPane;

	@Autowired
	private AckReceiptController ackReceiptController;

	@Autowired
	private TemplateService templateService;

	@Autowired
	private TemplateManagerBuilder templateManagerBuilder;

	@Autowired
	private TemplateGenerator templateGenerator;

	@Autowired
	private PreRegistrationDataSyncService preRegistrationDataSyncService;

	@Autowired
	private UserOnboardController userOnboardController;

	@Autowired
	private PacketHandlerService packetHandlerService;

	@Autowired
	private DashBoardController dashBoardController;

	@Autowired
	private RegistrationApprovalService registrationApprovalService;

	@Autowired
	private ReRegistrationService reRegistrationService;

	@Autowired
	private UserOnboardParentController userOnboardParentController;

	@Autowired
	private RegistrationController registrationController;

	@Autowired
	private UserOnboardService userOnboardService;

	@Autowired
	private JobConfigurationService jobConfigurationService;

	@Autowired
	private IdentitySchemaDao identitySchemaDao;

	@Autowired
	private SoftwareUpdateHandler softwareUpdateHandler;

	@Autowired
	private HeaderController headerController;

	@Autowired
	private GenericController genericController;
	
	@Autowired
	private LanguageSelectionController languageSelectionController;

	@SuppressWarnings("unchecked")
	public void setLastUpdateTime() {
		try {

			ResponseDTO responseDTO = jobConfigurationService.getLastCompletedSyncJobs();
			if (responseDTO.getSuccessResponseDTO() != null) {

				List<SyncDataProcessDTO> dataProcessDTOs = ((List<SyncDataProcessDTO>) responseDTO
						.getSuccessResponseDTO().getOtherAttributes().get(RegistrationConstants.SYNC_DATA_DTO));

				LinkedList<String> timestamps = new LinkedList<>();
				dataProcessDTOs.forEach(syncDataProcessDTO -> {

					if (!(jobConfigurationService.getUnTaggedJobs().contains(syncDataProcessDTO.getJobId())
							|| jobConfigurationService.getOfflineJobs().contains(syncDataProcessDTO.getJobId()))) {
						timestamps.add(syncDataProcessDTO.getLastUpdatedTimes());
					}
				});

				Optional<String> latestUpdateTime = timestamps.stream().sorted((timestamp1, timestamp2) -> Timestamp
						.valueOf(timestamp2).compareTo(Timestamp.valueOf(timestamp1))).findFirst();

				lastSyncTime.setText(getLocalZoneTime(latestUpdateTime.isPresent() ? latestUpdateTime.get() : null));

				setLastPreRegPacketDownloadedTime();
			}
		} catch (RuntimeException exception) {
			LOGGER.error("REGISTRATION - ALERT - BASE_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ExceptionUtils.getStackTrace(exception));
			lastSyncTime.setText("---");
		}
	}



	/**
	 * @return the userOnboardMsg
	 */
	public HBox getUserOnboardMessage() {
		return userOnboardMessage;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		versionValueLabel.setText(softwareUpdateHandler.getCurrentVersion());

		try {
			setImagesOnHover();

			setImage(syncDataImageView, RegistrationConstants.SYNC_IMG);	
			setImage(downloadPreRegDataImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
			setImage(uploadPacketImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);		
			setImage(remapImageView, RegistrationConstants.SYNC_IMG);
			setImage(checkUpdatesImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
			setImage(eodApprovalImageView, RegistrationConstants.PENDING_APPROVAL_IMG);
			setImage(reRegistrationImageView, RegistrationConstants.RE_REGISTRATION_IMG);		
			setImage(viewReportsImageView, RegistrationConstants.VIEW_REPORTS_IMG);
			setImage(tickMarkImageView, RegistrationConstants.TICK_IMG);
			setImage(updateOperatorBiometricsImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);

			if (!Role.hasSupervisorRole(SessionContext.userContext().getRoles())) {
				eodProcessGridPane.setVisible(false);
				eodLabel.setVisible(false);
			}
			setLastUpdateTime();
			pendingApprovalCountLbl.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_PENDING_APPLICATIONS));
			reRegistrationCountLbl.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_RE_REGISTER_APPLICATIONS));

			List<RegistrationApprovalDTO> pendingApprovalRegistrations = registrationApprovalService
					.getEnrollmentByStatus(RegistrationClientStatusCode.CREATED.getCode());

			List<PacketStatusDTO> reRegisterRegistrations = reRegistrationService.getAllReRegistrationPackets();

			if (!pendingApprovalRegistrations.isEmpty()) {
				pendingApprovalCountLbl
						.setText(pendingApprovalRegistrations.size() + " " + RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.APPLICATIONS));
			}
			if (!reRegisterRegistrations.isEmpty()) {
				reRegistrationCountLbl
						.setText(reRegisterRegistrations.size() + " " + RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.APPLICATIONS));
			}

			Timestamp ts = userOnboardService.getLastUpdatedTime(SessionContext.userId());
			if (ts != null) {
				lastBiometricTime
						.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.LAST_DOWNLOADED) + " " + getLocalZoneTime(ts.toString()));
			}

			loadRegistrationProcesses();

		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error("REGISTRATION - UI- Home Page Loading", APPLICATION_NAME, APPLICATION_ID,
					regBaseCheckedException.getMessage() + ExceptionUtils.getStackTrace(regBaseCheckedException));
		}
	}

	private void setImagesOnHover() {
		syncDataPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				
				setImage(syncDataImageView, RegistrationConstants.SYNC_DATA_FOCUSED_IMG);
			} else {
				
				setImage(syncDataImageView, RegistrationConstants.SYNC_IMG);
			}
		});
		downloadPreRegDataPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(downloadPreRegDataImageView, RegistrationConstants.DOWNLOAD_PREREG_FOCUSED_IMG);
			} else {

				setImage(downloadPreRegDataImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
			}
		});
		updateOperatorBiometricsPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(updateOperatorBiometricsImageView, RegistrationConstants.UPDATE_OP_BIOMETRICS_FOCUSED_IMG);
			} else {
				setImage(updateOperatorBiometricsImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);
			}
		});
		eodApprovalPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(eodApprovalImageView, RegistrationConstants.PENDING_APPROVAL_FOCUSED_IMG);
			} else {
				setImage(eodApprovalImageView, RegistrationConstants.PENDING_APPROVAL_IMG);
			}
		});
		reRegistrationPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(reRegistrationImageView, RegistrationConstants.RE_REGISTRATION_FOCUSED_IMG);	
			} else {
				setImage(reRegistrationImageView, RegistrationConstants.RE_REGISTRATION_IMG);	
			}
		});
		dashBoardPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(viewReportsImageView, RegistrationConstants.VIEW_REPORTS_FOCUSED_IMG);
			} else {
				setImage(viewReportsImageView, RegistrationConstants.VIEW_REPORTS_IMG);
			}
		});
		uploadPacketPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(uploadPacketImageView, RegistrationConstants.UPDATE_OP_BIOMETRICS_FOCUSED_IMG);
			} else {

				setImage(uploadPacketImageView, RegistrationConstants.UPDATE_OPERATOR_BIOMETRICS_IMG);
			}
		});
		centerRemapPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {

				setImage(remapImageView, RegistrationConstants.SYNC_DATA_FOCUSED_IMG);
			} else {

				setImage(remapImageView, RegistrationConstants.SYNC_IMG);
			}
		});
		checkUpdatesPane.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				setImage(checkUpdatesImageView, RegistrationConstants.DOWNLOAD_PREREG_FOCUSED_IMG);
			} else {
				setImage(checkUpdatesImageView, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
			}
		});
	}

	public void startRegistration(@NonNull String processId) {
		LOGGER.info("Creation of Registration Starting : {}", processId);
		try {
			double version = identitySchemaDao.getLatestEffectiveSchemaVersion();
			ProcessSpecDto processSpecDto = identitySchemaDao.getProcessSpec(processId, version);
			FlowType flowType = FlowType.valueOf(processSpecDto.getFlow());
			auditFactory.audit(flowType.getAuditEvent(), Components.NAVIGATION, SessionContext.userContext().getUserId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			if (!validateScreenAuthorization(flowType.getScreenId())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHORIZATION_ERROR));
				return;
			}

			StringBuilder errorMessage = new StringBuilder();
			ResponseDTO responseDTO = validateSyncStatus();
			List<ErrorResponseDTO> errorResponseDTOs = responseDTO.getErrorResponseDTOs();
			if (errorResponseDTOs != null && !errorResponseDTOs.isEmpty()) {
				for (ErrorResponseDTO errorResponseDTO : errorResponseDTOs) {
					errorMessage.append(
							RegistrationUIConstants.getMessageLanguageSpecific(errorResponseDTO.getMessage())
									+ "\n\n");
				}
				generateAlert(RegistrationConstants.ERROR, errorMessage.toString().trim());
				return;
			}

			switch (flowType) {
				case NEW:
				case LOST:
				case CORRECTION:
				case RENEWAL:
					Parent createRoot = getRoot(RegistrationConstants.CREATE_PACKET_PAGE);
					getScene(createRoot).setRoot(createRoot);
					getScene(createRoot).getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
					if(registrationController.createRegistrationDTOObject(processId)) {
						genericController.populateScreens();
						return;
					}
					break;
				case UPDATE:
					if(registrationController.createRegistrationDTOObject(processId)) {
						Parent root = BaseController.load(getClass().getResource(RegistrationConstants.UIN_UPDATE),
								applicationContext.getBundle(registrationController.getSelectedLangList().get(0), RegistrationConstants.LABELS));
						getScene(root);
						LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading Update UIN screen ended.");
						return;
					}
			}

		} catch (Exception e) {
			LOGGER.error("Failed to start registration : {}", processId, e);
		}
		clearRegistrationData();
		generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_REG_PAGE));
	}

	public void showReciept() {
		try {
			RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
			LOGGER.info("Showing receipt Started for process", registrationDTO.getProcessId());
			String platformLanguageCode = ApplicationContext.applicationLanguage();

			//slip acknowledgement
			String slipAckTemplateText = templateService.getHtmlTemplate(A6_ACKNOWLEDGEMENT_TEMPLATE_CODE,
					platformLanguageCode);

			if (slipAckTemplateText != null && !slipAckTemplateText.isEmpty()) {

				ResponseDTO templateResponse = templateGenerator.generateTemplate(slipAckTemplateText, registrationDTO,
						templateManagerBuilder, RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE, getImagePath(RegistrationConstants.CROSS_IMG, true));
				if (templateResponse != null && templateResponse.getSuccessResponseDTO() != null) {
					Writer stringWriter = (Writer) templateResponse.getSuccessResponseDTO().getOtherAttributes()
							.get(RegistrationConstants.TEMPLATE_NAME);
					ackReceiptController.setSlipStringWriter(stringWriter);
				}
			}

			//A4 ack
			String ackTemplateText = templateService.getHtmlTemplate(ACKNOWLEDGEMENT_TEMPLATE_CODE,
					platformLanguageCode);

			if (ackTemplateText != null && !ackTemplateText.isEmpty()) {

				ResponseDTO templateResponse = templateGenerator.generateTemplate(ackTemplateText, registrationDTO,
						templateManagerBuilder, RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE, getImagePath(RegistrationConstants.CROSS_IMG, true));
				if (templateResponse != null && templateResponse.getSuccessResponseDTO() != null) {
					Writer stringWriter = (Writer) templateResponse.getSuccessResponseDTO().getOtherAttributes()
							.get(RegistrationConstants.TEMPLATE_NAME);
					ackReceiptController.setStringWriter(stringWriter);
					ResponseDTO packetCreationResponse = savePacket(stringWriter, registrationDTO);
					if (packetCreationResponse.getSuccessResponseDTO() != null) {
						Parent createRoot = getRoot(RegistrationConstants.ACK_RECEIPT_PATH);
						getScene(createRoot).setRoot(createRoot);
						setIsAckOpened(true);
						LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Showing receipt ended.");
						return;
					}
				}
			}
		} catch (IOException | RegBaseCheckedException e) {
			LOGGER.error("Failed to load registration acknowledgment", e);
		}
		generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_ACKNOWLEDGEMENT_PAGE));
		clearRegistrationData();
		goToHomePageFromRegistration();
	}

	/**
	 * Validating screen authorization and Approve, Reject and Hold packets
	 */
	public void approvePacket() {
		LOGGER.info("Loading Pending Approval screen started.");
		try {
			auditFactory.audit(AuditEvent.NAV_APPROVE_REG, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
			GridPane root = BaseController.load(getClass().getResource(RegistrationConstants.PENDING_APPROVAL_PAGE));

			LOGGER.info("Validating Approve Packet screen for specific role");
			if (!validateScreenAuthorization(root.getId())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHORIZATION_ERROR));
				return;
			}

			getScene(root);

		} catch (IOException ioException) {
			LOGGER.error(ioException.getMessage(), ioException);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_APPROVAL_PAGE));
		}
		LOGGER.info("Loading Pending Approval screen ended.");
	}

	/**
	 * Validating screen authorization and Uploading packets to FTP server
	 */
	public void uploadPacket() {
		LOGGER.info("Loading Packet Upload screen started.");
		try {
			auditFactory.audit(AuditEvent.NAV_UPLOAD_PACKETS, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
			uploadRoot = BaseController.load(getClass().getResource(RegistrationConstants.FTP_UPLOAD_PAGE));
			LOGGER.info("Validating Upload Packet screen for specific role");

			if (!validateScreenAuthorization(uploadRoot.getId())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTHORIZATION_ERROR));
				return;
			}

			getScene(uploadRoot);
			clearRegistrationData();
			SessionContext.setAutoLogout(true);

		} catch (IOException ioException) {
			LOGGER.error("Failed to load upload packet page", ioException);
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading Packet Upload screen ended.");
	}

	/**
	 * Sync data through batch jobs.
	 */
	public void syncData() {
		headerController.syncData(null);
	}

	/**
	 * This method is to trigger the Pre registration sync service
	 */
	@FXML
	public void downloadPreRegData() {

		headerController.downloadPreRegData(null);
	}

	/**
	 * change On-Board user Perspective
	 */
	public void onBoardUser() {
		if (!proceedOnAction("OU"))
			return;

		auditFactory.audit(AuditEvent.NAV_ON_BOARD_USER, Components.NAVIGATION, APPLICATION_NAME,
				AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());

		SessionContext.map().put(RegistrationConstants.ONBOARD_USER, true);
		SessionContext.map().put(RegistrationConstants.ONBOARD_USER_UPDATE, true);

		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading User Onboard Update page");

		try {
			GridPane headerRoot = BaseController.load(getClass().getResource(RegistrationConstants.USER_ONBOARD));
			getScene(headerRoot);
			userOnboardParentController.userOnboardId.lookup("#onboardUser").setVisible(false);
		} catch (IOException ioException) {
			LOGGER.error("Failed to load user onboard page", ioException);
		}
		userOnboardController.initUserOnboard();
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "User Onboard Update page is loaded");
	}

	/**
	 * To save the acknowledgement receipt along with the registration data and
	 * create packet
	 */
	private ResponseDTO savePacket(Writer stringWriter, RegistrationDTO registrationDTO) {
		LOGGER.info("Packet save has been started");
		byte[] ackInBytes = null;
		try {
			ackInBytes = stringWriter.toString().getBytes(RegistrationConstants.TEMPLATE_ENCODING);
		} catch (java.io.IOException ioException) {
			LOGGER.error("",  ioException);
		}

		// packet creation
		ResponseDTO response = packetHandlerService.handle(registrationDTO);

		if (response.getSuccessResponseDTO() != null
				&& response.getSuccessResponseDTO().getMessage().equals(RegistrationConstants.SUCCESS)) {
			try {
				// Deletes the pre registration Data after creation of registration Packet.
				if (getRegistrationDTOFromSession().getPreRegistrationId() != null
						&& !getRegistrationDTOFromSession().getPreRegistrationId().trim().isEmpty()) {

					ResponseDTO responseDTO = new ResponseDTO();
					List<PreRegistrationList> preRegistrationLists = new ArrayList<>();
					PreRegistrationList preRegistrationList = preRegistrationDataSyncService
							.getPreRegistrationRecordForDeletion(
									getRegistrationDTOFromSession().getPreRegistrationId());
					preRegistrationLists.add(preRegistrationList);
					preRegistrationDataSyncService.deletePreRegRecords(responseDTO, preRegistrationLists);

				}

				packetHandlerService.createAcknowledgmentReceipt(registrationDTO.getPacketId(), ackInBytes,
						RegistrationConstants.ACKNOWLEDGEMENT_FORMAT);

				// Sync and Uploads Packet when EOD Process Configuration is set to OFF
				String supervisorApproval = getValueFromApplicationContext(RegistrationConstants.SUPERVISOR_APPROVAL_CONFIG_FLAG);
				if (supervisorApproval != null && !getValueFromApplicationContext(RegistrationConstants.SUPERVISOR_APPROVAL_CONFIG_FLAG)
						.equalsIgnoreCase(RegistrationConstants.ENABLE)) {
					updatePacketStatus();//auto-approve
				}

				LOGGER.info("Registration's Acknowledgement Receipt saved");
			} catch (io.mosip.kernel.core.exception.IOException ioException) {
				LOGGER.error("",ioException);
			} catch (RegBaseCheckedException regBaseCheckedException) {
				LOGGER.error("", regBaseCheckedException);

				if (regBaseCheckedException.getErrorCode()
						.equals(RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorCode())) {
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTH_ADVICE_FAILURE));
				}
			} catch (RuntimeException runtimeException) {
				LOGGER.error("", runtimeException);
			}
		} else {
			if (response.getErrorResponseDTOs() != null && response.getErrorResponseDTOs().get(0).getCode()
					.equals(RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorCode())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.AUTH_ADVICE_FAILURE));
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PACKET_CREATION_FAILURE));
			}
		}
		return response;
	}


	/**
	 * Load re registration screen.
	 */
	public void loadReRegistrationScreen() {
		if (!proceedOnReRegistrationAction()) {
			return;
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading re-registration screen sarted.");

		try {
			auditFactory.audit(AuditEvent.NAV_RE_REGISTRATION, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			Parent root = BaseController.load(getClass().getResource(RegistrationConstants.REREGISTRATION_PAGE));

			LOGGER.info("REGISTRATION - LOAD_REREGISTRATION_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
					APPLICATION_NAME, APPLICATION_ID, "Loading reregistration screen");

			getScene(root);
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - LOAD_REREGISTRATION_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
					APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_APPROVAL_PAGE));
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading re-registration screen ended.");
	}

	public void viewDashBoard() {

		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading dashboard screen sarted.");

		try {
			auditFactory.audit(AuditEvent.NAV_DASHBOARD, Components.NAVIGATION,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

			String dashboardTemplateText = templateService.getHtmlTemplate(
					RegistrationConstants.DASHBOARD_TEMPLATE_CODE, ApplicationContext.applicationLanguage());

			ResponseDTO templateResponse = templateGenerator.generateDashboardTemplate(dashboardTemplateText,
					templateManagerBuilder, RegistrationConstants.DASHBOARD_TEMPLATE,
					ClientApplication.getApplicationStartTime());

			if (templateResponse != null && templateResponse.getSuccessResponseDTO() != null) {
				Writer stringWriter = (Writer) templateResponse.getSuccessResponseDTO().getOtherAttributes()
						.get(RegistrationConstants.DASHBOARD_TEMPLATE);
				dashBoardController.setStringWriter(stringWriter);
				Parent root = BaseController.load(getClass().getResource(RegistrationConstants.DASHBOARD_PAGE));

				LOGGER.info("REGISTRATION - LOAD_DASHBOARD_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
						APPLICATION_NAME, APPLICATION_ID, "Loading dashboard screen");

				getScene(root);
			} else {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_DASHBOARD_PAGE));
			}
		} catch (IOException | RegBaseCheckedException exception) {
			LOGGER.error("REGISTRATION - LOAD_DASHBOARD_SCREEN - REGISTRATION_OFFICER_PACKET_CONTROLLER",
					APPLICATION_NAME, APPLICATION_ID, exception.getMessage() + ExceptionUtils.getStackTrace(exception));

			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_DASHBOARD_PAGE));
		}
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID, "Loading dashboard screen ended.");
	}

	/**
	 * Update packet status.
	 * 
	 * @throws RegBaseCheckedException
	 */
	private void updatePacketStatus() throws RegBaseCheckedException {
		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID,
				"Auto Approval of Packet when EOD process disabled started");

		registrationApprovalService.updateRegistration((getRegistrationDTOFromSession().getPacketId()),
				RegistrationConstants.EMPTY, RegistrationClientStatusCode.APPROVED.getCode());

		LOGGER.info(PACKET_HANDLER, APPLICATION_NAME, APPLICATION_ID,
				"Auto Approval of Packet when EOD process disabled ended");

	}


	public ProgressIndicator getProgressIndicator() {
		return progressIndicator;
	}
	
	public GridPane getPreRegDataPane() {
		return downloadPreRegDataPane;
	}
	
	public void setInProgressImage(Image image) {
		inProgressImage.setImage(image);
	}

	public void setLastPreRegPacketDownloadedTime() {

		SyncControl syncControl = jobConfigurationService
				.getSyncControlOfJob(RegistrationConstants.OPT_TO_REG_PDS_J00003);

		if (syncControl != null) {
			Timestamp lastPreRegPacketDownloaded = syncControl.getLastSyncDtimes();

			if (lastPreRegPacketDownloaded != null) {
				lastPreRegPacketDownloadedTime.setText(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.LAST_DOWNLOADED) + " "
						+ getLocalZoneTime(lastPreRegPacketDownloaded.toString()));
			}
		}
	}

	@FXML
	public void uploadPacketToServer() {
		auditFactory.audit(AuditEvent.SYNC_PRE_REGISTRATION_PACKET, Components.SYNC_SERVER_TO_CLIENT,
				SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		uploadPacket();
	}

	@FXML
	public void intiateRemapProcess() {
		headerController.intiateRemapProcess();
	}

	@FXML
	public void hasUpdate() {
		headerController.hasUpdate(null);
	}


	private void loadRegistrationProcesses()  {
		try {
			double version = identitySchemaDao.getLatestEffectiveSchemaVersion();
			List<ProcessSpec> processSpecs = identitySchemaDao.getAllActiveProcessSpecs(version);
			addParentRowConstraints(processSpecs == null ? 0 : processSpecs.size());
			AtomicInteger i = new AtomicInteger();
			Objects.requireNonNull(processSpecs).forEach(processSpec -> {
				try {
					FlowType flowType = FlowType.valueOf(processSpec.getFlow());
					if(flowType == null) {
						LOGGER.error("Invalid registration flow type {}", processSpec.getFlow());
						generateAlert(RegistrationConstants.ERROR,
								RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.INVALID_FLOW_TYPE));
						return;
					}
					ProcessSpecDto processSpecDto = identitySchemaDao.getProcessSpec(processSpec.getId(), version);
					GridPane gridPane = buildRegistrationProcessPane(processSpecDto);
					registrationGridPane.addRow(i.getAndIncrement(), gridPane);
				} catch (RegBaseCheckedException e) {
					LOGGER.error("Failed to build process pane {}", processSpec, e);
				}
			});
		} catch (RegBaseCheckedException e) {
			LOGGER.error("Failed to get configured processes", e);
		}
	}

	private void addParentRowConstraints(int size) {
		registrationGridPane.getRowConstraints().clear();
		for(int i=0;i<size;i++) {
			RowConstraints rowConstraints = new RowConstraints();
			rowConstraints.setPercentHeight((double)100/size);
			registrationGridPane.getRowConstraints().add(rowConstraints);
		}
	}

	private GridPane buildRegistrationProcessPane(ProcessSpecDto processSpecDto) {
		GridPane gridPane = new GridPane();
		gridPane.getStyleClass().add("operationalPaneDetailsSync");
		gridPane.setId(processSpecDto.getId());
		RowConstraints rowConstraints = new RowConstraints();
		rowConstraints.setPercentHeight(100);
		gridPane.getRowConstraints().addAll(rowConstraints);
		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(10);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(90);
		gridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2);
		gridPane.setOnMouseClicked(event -> {
			selectLanguage(event);
		});
		VBox vBox = new VBox();
		vBox.setAlignment(Pos.CENTER);
		vBox.setPrefHeight(200);
		vBox.setPrefWidth(100);
		ImageView imageView = new ImageView();
		imageView.setId(processSpecDto.getId()+"_img");
		imageView.setPreserveRatio(true);
		imageView.setPickOnBounds(true);
		imageView.setFitHeight(30);
		imageView.setFitWidth(30);
		try {
			imageView.setImage(getImage(processSpecDto.getIcon(), true));
		} catch (RegBaseCheckedException e) {
			LOGGER.error("Failed to get process image", e);
		}
		vBox.getChildren().add(imageView);
		gridPane.addColumn(0, vBox);

		Label label = new Label();
		label.setText(processSpecDto.getLabel().get(ApplicationContext.applicationLanguage()));
		label.setWrapText(true);
		label.getStyleClass().add("operationalTitle");
		gridPane.addColumn(1, label);
		changeNodeOrientation(gridPane);
		return gridPane;
	}
	
	public void selectLanguage(MouseEvent event) {
		if (!proceedOnRegistrationAction())
			return;

		StringBuilder errorMessage = new StringBuilder();
		ResponseDTO responseDTO;
		responseDTO = validateSyncStatus();
		List<ErrorResponseDTO> errorResponseDTOs = responseDTO.getErrorResponseDTOs();
		if (errorResponseDTOs != null && !errorResponseDTOs.isEmpty()) {
			for (ErrorResponseDTO errorResponseDTO : errorResponseDTOs) {
				errorMessage.append(
						RegistrationUIConstants.getMessageLanguageSpecific(errorResponseDTO.getMessage())
								+ "\n\n");
			}
			generateAlert(RegistrationConstants.ERROR, errorMessage.toString().trim());
		} else {
			String processId = ((GridPane) event.getSource()).getId();
			try {
				languageSelectionController.setProcessId(processId);
				if(isLanguageSelectionRequired()) {
					getStage().getScene().getRoot().setDisable(true);
					languageSelectionController.init();
				}
				else {
					languageSelectionController.submitLanguagesAndProceed(baseService.getMandatoryLanguages());
				}
			} catch (PreConditionCheckException e) {
				generateAlert(RegistrationConstants.ERROR, e.getErrorCode());
			}
		}
	}

	private boolean isLanguageSelectionRequired() throws PreConditionCheckException {
		return ( baseService.getMinLanguagesCount() >= 1 && baseService.getMaxLanguagesCount() > 1 );
	}
}
