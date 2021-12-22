package io.mosip.registration.constants;

import java.util.ResourceBundle;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.context.ApplicationContext;

public class RegistrationUIConstants {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(RegistrationUIConstants.class);

	// Key values to read value from messages.properties file

	public static ResourceBundle bundle = ApplicationContext.getInstance().getApplicationLanguageMessagesBundle();

	public static void setBundle(ResourceBundle messageBundle) {
		bundle = messageBundle;
	}


	public static String getMessageLanguageSpecific(String key) {
		try {
			return bundle.getString(key);
		} catch (Exception exception) {
			LOGGER.error("Could not find value in the resourcebundle for the key - {}", key);
		}
		return key != null ? key : ERROR;
	}

	// ALERT
	public static final String ERROR = bundle.getString("ERROR");
	
	


	// ALERT
	public static final String ERROR_CONSTANT = "ERROR";
	public static final String INFORMATION = "INFORMATION";
	public static final String SUCCESS = "SUCCESS";
	public static final String FAILURE = "FAILURE";

	// LOGIN
	public static final String UNABLE_LOAD_LOGIN_SCREEN_LANGUAGE_NOT_SET = "UNABLE_LOAD_LOGIN_SCREEN_LANGUAGE_NOT_SET";
	public static final String UNABLE_LOAD_LOGIN_SCREEN = "UNABLE_LOAD_LOGIN_SCREEN";
	public static final String BIOMETRIC_DISABLE_SCREEN_1 = "BIOMETRIC_DISABLE_SCREEN_1";
	public static final String BIOMETRIC_DISABLE_SCREEN_2 = "BIOMETRIC_DISABLE_SCREEN_2";
	public static final String BIOMETRIC_DISABLE_SCREEN_3 = "BIOMETRIC_DISABLE_SCREEN_3";
	public static final String BIOMETRIC_DISABLE_SCREEN_4 = "BIOMETRIC_DISABLE_SCREEN_4";
	public static final String MISSING_MANDATOTY_FIELDS = "MISSING_MANDATOTY_FIELDS";
	public static final String CREDENTIALS_FIELD_EMPTY = "CREDENTIALS_FIELD_EMPTY";
	public static final String USERNAME_FIELD_EMPTY = "USERNAME_FIELD_EMPTY";
	public static final String PWORD_FIELD_EMPTY = "PWORD_FIELD_EMPTY";
	public static final String USRNAME_LENGTH = "USRNAME_LENGTH";
	public static final String PWORD_LENGTH = "PWORD_LENGTH";
	public static final String USER_NOT_ONBOARDED = "USER_NOT_ONBOARDED";
	public static final String USER_NOT_AUTHORIZED = "USER_NOT_AUTHORIZED";
	public static final String REVIEWER_NOT_AUTHORIZED = "REVIEWER_NOT_AUTHORIZED";
	public static final String CREDENTIALS_NOT_FOUND = "CREDENTIALS_NOT_FOUND";
	public static final String TOKEN_SAVE_FAILED = "TOKEN_SAVE_FAILED";
	public static final String INCORRECT_PWORD = "INCORRECT_PWORD";
	public static final String BLOCKED_USER_ERROR = "BLOCKED_USER_ERROR";
	public static final String USERNAME_FIELD_ERROR = "USERNAME_FIELD_ERROR";
	public static final String OTP_FIELD_EMPTY = "OTP_FIELD_EMPTY";
	public static final String OTP_GENERATION_SUCCESS_MESSAGE = "OTP_GENERATION_SUCCESS_MESSAGE";
	public static final String OTP_GENERATION_ERROR_MESSAGE = "OTP_GENERATION_ERROR_MESSAGE";
	public static final String OTP_VALIDATION_SUCCESS_MESSAGE = "OTP_VALIDATION_SUCCESS_MESSAGE";
	public static final String OTP_VALIDATION_ERROR_MESSAGE = "OTP_VALIDATION_ERROR_MESSAGE";
	public static final String FINGER_PRINT_MATCH = "FINGER_PRINT_MATCH";
	public static final String IRIS_MATCH = "IRIS_MATCH";
	public static final String FACE_MATCH = "FACE_MATCH";
	public static final String FINGERPRINT = "FINGERPRINT";
	public static final String RECAPTURE = "RECAPTURE";
	public static final String SECONDS = "SECONDS";

	// Lost UIN
	public static final String LOST_UIN_REQUEST_ERROR = "LOST_UIN_REQUEST_ERROR";

	// AUTHORIZATION
	public static final String ROLES_EMPTY_ERROR = "ROLES_EMPTY_ERROR";
	public static final String MACHINE_MAPPING_ERROR = "MACHINE_MAPPING_ERROR";
	public static final String AUTHORIZATION_ERROR = "AUTHORIZATION_ERROR";

	// MACHINE CENTER REMAP
	public static final String REMAP_NO_ACCESS_MESSAGE = "REMAP_NO_ACCESS_MESSAGE";
	public static final String REMAP_MESSAGE = "REMAP_MESSAGE";
	public static final String REMAP_EOD_PROCESS_MESSAGE = "REMAP_EOD_PROCESS_MESSAGE";
	public static final String REMAP_CLICK_OK = "REMAP_CLICK_OK";
	public static final String REMAP_PROCESS_SUCCESS = "REMAP_PROCESS_SUCCESS";
	public static final String REMAP_NOT_APPLICABLE = "REMAP_NOT_APPLICABLE";
	public static final String USERMAP_NOT_APPLICABLE = "USERMAP_NOT_APPLICABLE";
	public static final String REMAP_PROCESS_STILL_PENDING = "REMAP_PROCESS_STILL_PENDING";

	// DEVICE
	public static final String DEVICE_FP_NOT_FOUND = "DEVICE_FP_NOT_FOUND";
	public static final String FP_DEVICE_TIMEOUT = "FP_DEVICE_TIMEOUT";
	public static final String FP_DEVICE_ERROR = "FP_DEVICE_ERROR";
	public static final String FP_CAPTURE_SUCCESS = "FP_CAPTURE_SUCCESS";
	public static final String WEBCAM_ALERT_CONTEXT = "WEBCAM_ALERT_CONTEXT";
	public static final String FACE_CAPTURE_ERROR = "FACE_CAPTURE_ERROR";
	public static final String EXCEPTION_PHOTO_CAPTURE_ERROR = "EXCEPTION_PHOTO_CAPTURE_ERROR";
	public static final String PARENT_FACE_CAPTURE_ERROR = "PARENT_FACE_CAPTURE_ERROR";
	public static final String FACE_SCANNING_ERROR = "FACE_SCANNING_ERROR";
	public static final String FACE_DUPLICATE_ERROR = "FACE_DUPLICATE_ERROR";
	public static final String DEVICE_ONBOARD_NOTIFICATION = "DEVICE_ONBOARD_NOTIFICATION";
	public static final String FACE_CAPTURE_SUCCESS = "FACE_CAPTURE_SUCCESS";
	public static final String FACE_CAPTURE_SUCCESS_MSG = "FACE_CAPTURE_SUCCESS_MSG";

	// LOCK ACCOUNT
	public static final String USER_ACCOUNT_LOCK_MESSAGE_NUMBER = "USER_ACCOUNT_LOCK_MESSAGE_NUMBER";
	public static final String USER_ACCOUNT_LOCK_MESSAGE = "USER_ACCOUNT_LOCK_MESSAGE";
	public static final String USER_ACCOUNT_LOCK_MESSAGE_MINUTES ="USER_ACCOUNT_LOCK_MESSAGE_MINUTES";

	// NOTIFICATIONS
	public static final String EMAIL_NOTIFICATION_SUCCESS = "EMAIL_NOTIFICATION_SUCCESS";
	public static final String SMS_NOTIFICATION_SUCCESS = "SMS_NOTIFICATION_SUCCESS";
	public static final String INVALID_EMAIL = "INVALID_EMAIL";
	public static final String INVALID_MOBILE = "INVALID_MOBILE";
	public static final String NOTIFICATION_SUCCESS = "NOTIFICATION_SUCCESS";
	public static final String NOTIFICATION_FAIL = "NOTIFICATION_FAIL";
	public static final String NOTIFICATION_SMS_FAIL = "NOTIFICATION_SMS_FAIL";
	public static final String NOTIFICATION_EMAIL_FAIL = "NOTIFICATION_EMAIL_FAIL";
	public static final String NOTIFICATION_LIMIT_EXCEEDED = "NOTIFICATION_LIMIT_EXCEEDED";
	public static final String PACKET_CREATION_FAILURE = "PACKET_CREATION_FAILURE";

	// SUCCESS
	public static final String PACKET_CREATED_SUCCESS = "PACKET_CREATED_SUCCESS";
	public static final String PRINT_INITIATION_SUCCESS = "PRINT_INITIATION_SUCCESS";
	public static final String REREGISTRATION_APPROVE_SUCCESS = "REREGISTRATION_APPROVE_SUCCESS";
	public static final String REREGISTER_TITLEPANE = "REREGISTER_TITLEPANE";
	public static final String APPLICATIONS = "APPLICATIONS";
	public static final String NO_PENDING_APPLICATIONS = "NO_PENDING_APPLICATIONS";
	public static final String NO_RE_REGISTER_APPLICATIONS = "NO_RE_REGISTER_APPLICATIONS";

	// AUTHENTICATION
	public static final String AUTHENTICATION_FAILURE = "AUTHENTICATION_FAILURE";
	public static final String AUTH_APPROVAL_SUCCESS_MSG = "AUTH_APPROVAL_SUCCESS_MSG";
	public static final String AUTH_PENDING_ACTION_SUCCESS_MSG = "AUTH_PENDING_ACTION_SUCCESS_MSG";
	public static final String AUTHENTICATION_ERROR_MSG = "AUTHENTICATION_ERROR_MSG";
	public static final String APPROVED = "APPROVED";
	public static final String REJECTED = "REJECTED";
	public static final String PENDING = "PENDING";
	public static final String INFORMED = "INFORMED";
	public static final String CANTINFORMED = "CANTINFORMED";

	// CAMERA
	public static final String APPLICANT_IMAGE_ERROR = "APPLICANT_IMAGE_ERROR";
	public static final String DEMOGRAPHIC_DETAILS_ERROR_CONTEXT = "DEMOGRAPHIC_DETAILS_ERROR_CONTEXT";

	// REGISTRATION
	public static final String AGE_WARNING = "AGE_WARNING";
	public static final String TO = "TO";
	public static final String MIN_AGE_WARNING = "MIN_AGE_WARNING";
	public static final String POA_DOCUMENT_EMPTY = "poaDocuments";
	public static final String POI_DOCUMENT_EMPTY = "poiDocuments";
	public static final String POR_DOCUMENT_EMPTY = "porDocuments";
	public static final String DOB_DOCUMENT_EMPTY = "dobDocuments";
	public static final String SCAN_DOCUMENT_ERROR = "SCAN_DOCUMENT_ERROR";
	public static final String STREAMING_PREP_MESSAGE = "STREAMING_PREP_MESSAGE";
	public static final String STREAMING_INIT_MESSAGE = "STREAMING_INIT_MESSAGE";
	public static final String STREAMING_CLOSED_MESSAGE = "STREAMING_CLOSED_MESSAGE";
	public static final String SEARCHING_DEVICE_MESSAGE = "SEARCHING_DEVICE_MESSAGE";
	public static final String CAPTURING = "CAPTURING";
	public static final String UNABLE_LOAD_SCAN_POPUP = "UNABLE_LOAD_SCAN_POPUP";
	public static final String SCAN_DOC_TITLE = "SCAN_DOC_TITLE";
	public static final String SCAN_DOC_CATEGORY_MULTIPLE = "SCAN_DOC_CATEGORY_MULTIPLE";
	public static final String SCAN_DOC_SUCCESS = "SCAN_DOC_SUCCESS";
	public static final String CROP_DOC_SUCCESS = "CROP_DOC_SUCCESS";
	public static final String SCAN_DOC_SIZE = "SCAN_DOC_SIZE";
	public static final String SCAN_DOC_INFO = "SCAN_DOC_INFO";
	public static final String SELECTED_SCANNER = "SELECTED_SCANNER";
	public static final String NO_SCANNER_FOUND = "NO_SCANNER_FOUND";
	public static final String SCAN_DOCUMENT_CONNECTION_ERR = "SCAN_DOCUMENT_CONNECTION_ERR";
	public static final String SCAN_DOCUMENT_EMPTY = "SCAN_DOCUMENT_EMPTY";
	public static final String SCAN_DOCUMENT_CONVERTION_ERR = "SCAN_DOCUMENT_CONVERTION_ERR";
	public static final String PRE_REG_ID_EMPTY = "PRE_REG_ID_EMPTY";
	public static final String REG_LGN_001 = "REG_LGN_001";
	public static final String PRE_REG_ID_NOT_VALID = "PRE_REG_ID_NOT_VALID";
	public static final String REG_ID_JSON_VALIDATION_FAILED = "REG_ID_JSON_VALIDATION_FAILED";
	public static final String SCAN = "SCAN";
	public static final String REF_NUMBER = "REF_NUMBER";
	public static final String PLEASE_SELECT = "PLEASE_SELECT";
	public static final String DOCUMENT = "DOCUMENT";
	public static final String DATE_VALIDATION_MSG = "DATE_VALIDATION_MSG";
	public static final String PHOTO_CAPTURE = "PHOTO_CAPTURE";
	public static final String PREVIOUS_ADDRESS = "PREVIOUS_ADDRESS";
	public static final String PREVIEW_DOC = "PREVIEW_DOC";
	public static final String RID_INVALID = "RID_INVALID";
	public static final String UIN_INVALID = "UIN_INVALID";
	public static final String IS_BLOCKED_WORD = "IS_BLOCKED_WORD";
	public static final String THRESHOLD = "THRESHOLD";
	public static final String INVALID_DATE = "INVALID_DATE";
	public static final String INVALID_YEAR = "INVALID_YEAR";
	public static final String FUTURE_DOB = "FUTURE_DOB";
	public static final String INVALID_AGE = "INVALID_AGE";
	public static final String INVALID_MONTH = "INVALID_MONTH";
	public static final String SELECT = "SELECT";
	public static final String LEFT_SLAP = "LEFT_SLAP";
	public static final String RIGHT_SLAP = "RIGHT_SLAP";
	public static final String THUMBS = "THUMBS";
	public static final String RIGHT_IRIS = "RIGHT_IRIS";
	public static final String LEFT_IRIS = "LEFT_IRIS";
	public static final String PHOTO = "PHOTO";
	public static final String TAKE_PHOTO = "TAKE_PHOTO";
	public static final String PACKET_CREATION_DISK_SPACE_CHECK = "PACKET_CREATION_DISK_SPACE_CHECK";
	public static final String SECONDARY_LANG_MISSING = "SECONDARY_LANG_MISSING";
	public static final String PRIMARY_LANG_MISSING = "PRIMARY_LANG_MISSING";

	public static final String PLACEHOLDER_LABEL = "PLACEHOLDER_LABEL";
	public static final String PARENT_BIO_MSG = "PARENT_BIO_MSG";

	// OPT TO REGISTER
	public static final String REG_PKT_APPRVL_CNT_EXCEED = "REG_PKT_APPRVL_CNT_EXCEED";
	public static final String REG_PKT_APPRVL_TIME_EXCEED = "REG_PKT_APPRVL_TIME_EXCEED";
	public static final String OPT_TO_REG_TIME_SYNC_EXCEED = "OPT_TO_REG_TIME_SYNC_EXCEED";
	public static final String OPT_TO_REG_TIME_EXPORT_EXCEED = "OPT_TO_REG_TIME_EXPORT_EXCEED";
	public static final String OPT_TO_REG_REACH_MAX_LIMIT = "OPT_TO_REG_REACH_MAX_LIMIT";
	public static final String OPT_TO_REG_OUTSIDE_LOCATION = "OPT_TO_REG_OUTSIDE_LOCATION";
	public static final String OPT_TO_REG_WEAK_GPS = "OPT_TO_REG_WEAK_GPS";
	public static final String OPT_TO_REG_INSERT_GPS = "OPT_TO_REG_INSERT_GPS";
	public static final String OPT_TO_REG_GPS_PORT_MISMATCH = "OPT_TO_REG_GPS_PORT_MISMATCH";
	public static final String OPT_TO_REG_LAST_SOFTWAREUPDATE_CHECK = "OPT_TO_REG_LAST_SOFTWAREUPDATE_CHECK";

	// PACKET EXPORT
	public static final String PACKET_EXPORT_SUCCESS_MESSAGE = "PACKET_EXPORT_SUCCESS_MESSAGE";
	public static final String PACKET_EXPORT_MESSAGE = "PACKET_EXPORT_MESSAGE";
	public static final String PACKET_EXPORT_FAILURE = "PACKET_EXPORT_FAILURE";

	// JOBS
	public static final String EXECUTE_JOB_ERROR_MESSAGE = "EXECUTE_JOB_ERROR_MESSAGE";
	public static final String BATCH_JOB_START_SUCCESS_MESSAGE = "BATCH_JOB_START_SUCCESS_MESSAGE";
	public static final String BATCH_JOB_STOP_SUCCESS_MESSAGE = "BATCH_JOB_STOP_SUCCESS_MESSAGE";
	public static final String START_SCHEDULER_ERROR_MESSAGE = "START_SCHEDULER_ERROR_MESSAGE";
	public static final String STOP_SCHEDULER_ERROR_MESSAGE = "STOP_SCHEDULER_ERROR_MESSAGE";
	public static final String CURRENT_JOB_DETAILS_ERROR_MESSAGE = "CURRENT_JOB_DETAILS_ERROR_MESSAGE";
	public static final String SYNC_SUCCESS = "SYNC_SUCCESS";
	public static final String SYNC_FAILURE = "SYNC_FAILURE";

	// MACHINE MAPPING
	public static final String MACHINE_MAPPING_SUCCESS_MESSAGE = "MACHINE_MAPPING_SUCCESS_MESSAGE";
	public static final String MACHINE_MAPPING_ERROR_MESSAGE = "MACHINE_MAPPING_ERROR_MESSAGE";
	public static final String MACHINE_MAPPING_ENTITY_SUCCESS_MESSAGE = "MACHINE_MAPPING_ENTITY_SUCCESS_MESSAGE";
	public static final String MACHINE_MAPPING_ENTITY_ERROR_NO_RECORDS = "MACHINE_MAPPING_ENTITY_ERROR_NO_RECORDS";

	// SYNC
	public static final String PACKET_STATUS_SYNC_SUCCESS_MESSAGE ="PACKET_STATUS_SYNC_SUCCESS_MESSAGE";
	public static final String PACKET_STATUS_SYNC_ERROR_RESPONSE = "PACKET_STATUS_SYNC_ERROR_RESPONSE";

	// GENERIC
	public static final String UNABLE_LOAD_HOME_PAGE = "UNABLE_LOAD_HOME_PAGE";
	public static final String UNABLE_LOAD_LOGOUT_PAGE = "UNABLE_LOAD_LOGOUT_PAGE";
	public static final String UNABLE_LOAD_APPROVAL_PAGE = "UNABLE_LOAD_APPROVAL_PAGE";
	public static final String UNABLE_LOAD_REG_PAGE = "UNABLE_LOAD_REG_PAGE";
	public static final String UNABLE_LOAD_DEMOGRAPHIC_PAGE = "UNABLE_LOAD_DEMOGRAPHIC_PAGE";
	public static final String UNABLE_LOAD_NOTIFICATION_PAGE = "UNABLE_LOAD_NOTIFICATION_PAGE";
	public static final String UNABLE_LOAD_PREVIEW_PAGE = "UNABLE_LOAD_PREVIEW_PAGE";
	public static final String UNABLE_LOAD_ACKNOWLEDGEMENT_PAGE = "UNABLE_LOAD_ACKNOWLEDGEMENT_PAGE";
	public static final String UNABLE_LOAD_DASHBOARD_PAGE = "UNABLE_LOAD_DASHBOARD_PAGE";

	// Individual Registartion - Iris Capture
	public static final String UNABLE_LOAD_IRIS_SCAN_POPUP = "UNABLE_LOAD_IRIS_SCAN_POPUP";
	public static final String IRIS_SUCCESS_MSG = "IRIS_SUCCESS_MSG";
	public static final String IRIS_NAVIGATE_NEXT_SECTION_ERROR = "IRIS_NAVIGATE_NEXT_SECTION_ERROR";
	public static final String IRIS_NAVIGATE_PREVIOUS_SECTION_ERROR = "IRIS_NAVIGATE_PREVIOUS_SECTION_ERROR";
	public static final String BIOMETRIC_SCANNING_ERROR = "BIOMETRIC_SCANNING_ERROR";
	public static final String IRIS_SCANNING_ERROR = "IRIS_SCANNING_ERROR";
	public static final String FINGERPRINT_SCANNING_ERROR = "FINGERPRINT_SCANNING_ERROR";
	public static final String NO_DEVICE_FOUND = "NO_DEVICE_FOUND";
	public static final String FINGERPRINT_SELECTION_PANE_ALERT = "FINGERPRINT_SELECTION_PANE_ALERT";
	public static final String FINGERPRINT_SCAN_ALERT = "FINGERPRINT_SCAN_ALERT";
	public static final String IRIS_VALIDATION_ERROR = "IRIS_VALIDATION_ERROR";
	public static final String FINGERPRINT_DUPLICATION_ALERT = "FINGERPRINT_DUPLICATION_ALERT";
	public static final String FINGERPRINT_MAX_RETRIES_ALERT = "FINGERPRINT_MAX_RETRIES_ALERT";
	public static final String FINGERPRINT_NAVIGATE_NEXT_SECTION_ERROR = "FINGERPRINT_NAVIGATE_NEXT_SECTION_ERROR";
	public static final String FINGERPRINT_NAVIGATE_PREVIOUS_SECTION_ERROR = "FINGERPRINT_NAVIGATE_PREVIOUS_SECTION_ERROR";
	public static final String UNABLE_LOAD_FINGERPRINT_SCAN_POPUP ="UNABLE_LOAD_FINGERPRINT_SCAN_POPUP";
	public static final String IRIS_SCAN_RETRIES_EXCEEDED = "IRIS_SCAN_RETRIES_EXCEEDED";
	public static final String IRIS_QUALITY_SCORE_ERROR = "IRIS_QUALITY_SCORE_ERROR";
	public static final String IRIS_SCAN = "IRIS_SCAN";

	// UIN update
	public static final String UPDATE_UIN_ENTER_UIN_ALERT = "UPDATE_UIN_ENTER_UIN_ALERT";
	public static final String UPDATE_UIN_VALIDATION_ALERT = "UPDATE_UIN_VALIDATION_ALERT";
	public static final String UPDATE_UIN_SELECTION_ALERT = "UPDATE_UIN_SELECTION_ALERT";
	public static final String UPDATE_UIN_INDIVIDUAL_AND_PARENT_SAME_UIN_ALERT = "UPDATE_UIN_INDIVIDUAL_AND_PARENT_SAME_UIN_ALERT";
	public static final String UPDATE_UIN_NO_BIOMETRIC_CONFIG_ALERT ="UPDATE_UIN_NO_BIOMETRIC_CONFIG_ALERT";

	// Biometric Exception
	public static final String BIOMETRIC_EXCEPTION_ALERT = "BIOMETRIC_EXCEPTION_ALERT";

	// User Onboard
	public static final String UNABLE_LOAD_USERONBOARD_SCREEN = "UNABLE_LOAD_USERONBOARD_SCREEN";
	public static final String USER_MACHINE_VALIDATION_MSG = "USER_MACHINE_VALIDATION_MSG";
	public static final String USER_ONBOARD_HI = "USER_ONBOARD_HI";
	public static final String USER_ONBOARD_NOTONBOARDED = "USER_ONBOARD_NOTONBOARDED";
	public static final String USER_ONBOARD_ERROR = "USER_ONBOARD_ERROR";
	public static final String NO_INTERNET = "NO_INTERNET";

	// Supervisor Authentication configuration from global_param
	public static final String SUPERVISOR_AUTHENTICATION_CONFIGURATION = "mosip.registration.supervisor_authentication_configuration";

	// Registration Approval - EOD Process
	public static final String ERROR_IN_SYNC_AND_UPLOAD = "ERROR_IN_SYNC_AND_UPLOAD";
	public static final String UNABLE_TO_SYNC_AND_UPLOAD = "UNABLE_TO_SYNC_AND_UPLOAD";
	public static final String NETWORK_ERROR = "NETWORK_ERROR";
	public static final String SUPERVISOR_VERIFICATION = "SUPERVISOR_VERIFICATION";
	public static final String EOD_DETAILS_EXPORT_FAILURE = "EOD_DETAILS_EXPORT_FAILURE";
	public static final String EOD_DETAILS_EXPORT_SUCCESS = "EOD_DETAILS_EXPORT_SUCCESS";
	public static final String EOD_SLNO_LABEL = "EOD_SLNO_LABEL";
	public static final String EOD_REGISTRATIONID_LABEL = "EOD_REGISTRATIONID_LABEL";
	public static final String EOD_REGISTRATIONDATE_LABEL = "EOD_REGISTRATIONDATE_LABEL";
	public static final String EOD_OPERATORID_LABEL = "EOD_OPERATORID_LABEL";
	public static final String EOD_STATUS_LABEL = "EOD_STATUS_LABEL";
	public static final String PACKETUPLOAD_PACKETID_LABEL = "PACKETUPLOAD_PACKETID_LABEL";
	public static final String AUTH_FAILURE = "AUTH_FAILURE";
	public static final String AUTH_ADVICE_FAILURE = "AUTH_ADVICE_FAILURE";

	// Virus Scan
	public static final String VIRUS_SCAN_ERROR_FIRST_PART = "VIRUS_SCAN_ERROR_FIRST_PART";
	public static final String VIRUS_SCAN_ERROR_SECOND_PART = "VIRUS_SCAN_ERROR_SECOND_PART";
	public static final String VIRUS_SCAN_SUCCESS = "VIRUS_SCAN_SUCCESS";

	public static final String INVALID_KEY = "INVALID_KEY";
	public static final String CENTER_MACHINE_INACTIVE = "CENTER_MACHINE_INACTIVE";

	public static final String RESTART_APPLICATION = "RESTART_APPLICATION";
	public static final String LOGOUT_ALERT = "LOGOUT_ALERT";

	public static final String PRE_REG_TO_GET_ID_ERROR = "PRE_REG_TO_GET_ID_ERROR";
	public static final String PRE_REG_TO_GET_PACKET_ERROR = "PRE_REG_TO_GET_PACKET_ERROR";
	public static final String PRE_REG_PACKET_NETWORK_ERROR = "PRE_REG_PACKET_NETWORK_ERROR";
	public static final String PRE_REG_SUCCESS_MESSAGE = "PRE_REG_SUCCESS_MESSAGE";

	// PRE-REG DELETE JOB
	public static final String PRE_REG_DELETE_SUCCESS = "PRE_REG_DELETE_SUCCESS";
	public static final String PRE_REG_DELETE_FAILURE = "PRE_REG_DELETE_FAILURE";

	// PRE-REG DELETE JOB
	public static final String SYNC_CONFIG_DATA_FAILURE = "SYNC_CONFIG_DATA_FAILURE";

	// Packet Upload
	public static final String PACKET_UPLOAD_EMPTY_ERROR = "PACKET_UPLOAD_EMPTY_ERROR";
	public static final String PACKET_EXPORT_EMPTY_ERROR = "PACKET_EXPORT_EMPTY_ERROR";
	public static final String PACKET_UPLOAD_DUPLICATE = "PACKET_UPLOAD_DUPLICATE";
	public static final String PACKET_NOT_AVAILABLE = "PACKET_NOT_AVAILABLE";
	public static final String PACKET_UPLOAD_SERVICE_ERROR = "PACKET_UPLOAD_SERVICE_ERROR";
	public static final String PACKET_UPLOAD_EMPTY = "PACKET_UPLOAD_EMPTY";
	public static final String PACKET_UPLOAD_ERROR = "PACKET_UPLOAD_ERROR";
	public static final String PACKET_PARTIAL_UPLOAD_ERROR = "PACKET_PARTIAL_UPLOAD_ERROR";
	public static final String PACKET_UPLOAD_HEADER_NAME = "PACKET_UPLOAD_HEADER_NAME";
	public static final String UPLOAD_COLUMN_HEADER_FILE = "UPLOAD_COLUMN_HEADER_FILE";
	public static final String UPLOAD_COLUMN_HEADER_STATUS = "UPLOAD_COLUMN_HEADER_STATUS";
	public static final String PACKET_UPLOAD_SUCCESS = "PACKET_UPLOAD_SUCCESS";
	public static final String UPLOAD_FAILED = "UPLOAD_FAILED";
	public static final String PACKET_STATUS_EXPORT = "PACKET_STATUS_EXPORT";

	// Scheduler
	public static final String TIMEOUT_TITLE = "TIMEOUT_TITLE";
	public static final String TIMEOUT_INITIAL = "TIMEOUT_INITIAL";
	public static final String TIMEOUT_MIDDLE = "TIMEOUT_MIDDLE";
	public static final String TIMEOUT_END = "TIMEOUT_END";

	// Notification
	public static final String EMAIL_ERROR_MSG = "EMAIL_ERROR_MSG";
	public static final String SMS_ERROR_MSG = "SMS_ERROR_MSG";

	// Application Updates
	public static final String NO_UPDATES_FOUND = "NO_UPDATES_FOUND";
	public static final String UNABLE_FIND_UPDATES = "UNABLE_FIND_UPDATES";
	public static final String NO_INTERNET_CONNECTION = "NO_INTERNET_CONNECTION";
	public static final String UPDATE_AVAILABLE = "UPDATE_AVAILABLE";
	public static final String CONFIRM_UPDATE = "CONFIRM_UPDATE";
	public static final String UPDATE_COMPLETED = "UPDATE_COMPLETED";
	public static final String UNABLE_TO_UPDATE = "UNABLE_TO_UPDATE";
	public static final String UPDATE_LATER = "UPDATE_LATER";
	public static final String UPDATE_FREEZE_TIME_EXCEED = "UPDATE_FREEZE_TIME_EXCEED";
	public static final String SQL_EXECUTION_FAILED_AND_REPLACED = "SQL_EXECUTION_FAILED_AND_REPLACED";

	// AUTH TOKEN
	public static String UNABLE_TO_GET_AUTH_TOKEN = "UNABLE_TO_GET_AUTH_TOKEN";

	// PAGE NAVIGATION
	public static final String PAGE_NAVIGATION_MESSAGE = "PAGE_NAVIGATION_MESSAGE";
	public static final String PAGE_NAVIGATION_CONFIRM = "PAGE_NAVIGATION_CONFIRM";
	public static final String PAGE_NAVIGATION_CANCEL = "PAGE_NAVIGATION_CANCEL";

	// SYNC DATE TIME
	public static final String LAST_DOWNLOADED = "LAST_DOWNLOADED";
	public static final String LAST_UPDATED = "LAST_UPDATED";

	// Alert
	public static final String ALERT_NOTE_LABEL = "ALERT_NOTE_LABEL";
	public static final String ALERT_FAILED_LABEL = "ALERT_FAILED_LABEL";

	// Unable to get Auth Token
	public static final String ALERT_AUTH_TOKEN_NOT_FOUND = "AUTH_TOKEN_NOT_FOUND";

	// Device Searching to start stream
	public static final String SEARCHING_DEVICE = "SEARCHING_DEVICE";
	public static final String VALIDATION_MESSAGE = "VALIDATION_MESSAGE";

	public static final String EXCEPTION_PHOTO_MANDATORY = "EXCEPTION_PHOTO_MANDATORY";

	public static final String BIOMETRIC_CAPTURE_SUCCESS = "BIOMETRIC_CAPTURE_SUCCESS";
	public static final String BIOMETRIC_CAPTURE_FAILURE = "BIOMETRIC_CAPTURE_FAILURE";

	public static final String LOCAL_DEDUP_CHECK_FAILED = "LOCAL_DEDUP_CHECK_FAILED";

	public static final String STREAMING_ERROR = "STREAMING_ERROR";

	public static final String EXCEPTION_PHOTO_REQUIRED = "EXCEPTION_PHOTO_REQUIRED";

	public static final String USER_RELOGIN_REQUIRED = "USER_RELOGIN_REQUIRED";

	public static final String DOC_CAPTURE_SUCCESS = "DOC_CAPTURE_SUCCESS";

	public static final String DOC_DELETE_SUCCESS = "DOC_DELETE_SUCCESS";

	public static final String USER_IN_ACTIVE = "USER_IN_ACTIVE";

	public static final String ONBOARD_USER_TITLE = "officerbiometrics";
	
	public static final String INVALID_CRON_EXPRESSION = "INVALID_CRON_EXPRESSION";
	
	public static final String CRON_EXPRESSION_MODIFIED = "CRON_EXPRESSION_MODIFIED";

	public static final String INVALID_FLOW_TYPE = "INVALID_FLOW_TYPE";
	public static final String ADDITIONAL_INFO_REQ_ID_MISSING = "ADDITIONAL_INFO_REQ_ID_MISSING";
}
