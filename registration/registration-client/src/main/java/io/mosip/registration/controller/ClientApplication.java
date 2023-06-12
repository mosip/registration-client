package io.mosip.registration.controller;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.ResourceBundle;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.audit.AuditManagerSerivceImpl;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.auth.LoginController;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.exception.PreConditionCheckException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import io.mosip.registration.preloader.ClientPreLoader;
import io.mosip.registration.preloader.ClientPreLoaderErrorNotification;
import io.mosip.registration.preloader.ClientPreLoaderNotification;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.config.JobConfigurationService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.service.login.LoginService;
import io.mosip.registration.update.ClientIntegrityValidator;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.restclient.AuthTokenUtilService;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Class for initializing the application
 * 
 * @author Sravya Surampalli
 * @since 1.0.0
 *
 */
@Component
public class ClientApplication extends Application {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(ClientApplication.class);

	private static ApplicationContext applicationContext;
	private static Stage applicationPrimaryStage;
	private static String applicationStartTime;
	private static boolean syncCompleted = false;

	private AuditManagerService auditFactory;
	
	@Override
	public void init() throws Exception {
		try { //Do heavy lifting here
			if(ClientPreLoader.errorsFound)
				return;

			ClientIntegrityValidator.verifyClientIntegrity();
			notifyPreloader(new ClientPreLoaderNotification("Client integrity check successful."));

			applicationStartTime = String.valueOf(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));

			notifyPreloader(new ClientPreLoaderNotification("Creating application context..."));
			applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
			SessionContext.setApplicationContext(applicationContext);
			notifyPreloader(new ClientPreLoaderNotification("Created application context."));

			auditFactory = applicationContext.getBean("auditManagerSerivceImpl", AuditManagerSerivceImpl.class);
			upgradeLocalDatabase();

			setupLanguages();
			notifyPreloader(new ClientPreLoaderNotification("Language setup complete."));
			setupResourceBundleBasedOnDefaultAppLang();
			setupAppProperties();
			notifyPreloader(new ClientPreLoaderNotification("Properties with local preferences loaded."));
			setupResourceBundleBasedOnDefaultAppLang();

			notifyPreloader(new ClientPreLoaderNotification(RegistrationConstants.MOSIP_HOSTNAME + " : " +
					io.mosip.registration.context.ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.MOSIP_HOSTNAME)));
			notifyPreloader(new ClientPreLoaderNotification(RegistrationConstants.MOSIP_UPGRADE_SERVER_URL + " : " +
					io.mosip.registration.context.ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.MOSIP_UPGRADE_SERVER_URL)));

			handleInitialSync();
			discoverDevices();

		} catch (Throwable t) {
			ClientPreLoader.errorsFound = true;
			LOGGER.error("Application Initialization Error", t);
			notifyPreloader(new ClientPreLoaderErrorNotification(t));
		}
	}

	@Override
	public void start(Stage primaryStage) {
		try {
			if(ClientPreLoader.errorsFound)
				return;

			LOGGER.info("Login screen Initialization {}", new SimpleDateFormat(RegistrationConstants.HH_MM_SS).format(System.currentTimeMillis()));

			setPrimaryStage(primaryStage);
			LoginController loginController = applicationContext.getBean(LoginController.class);
			loginController.loadInitialScreen(primaryStage);

			LOGGER.info("Login screen loaded {}", new SimpleDateFormat(RegistrationConstants.HH_MM_SS).format(System.currentTimeMillis()));

		} catch (Exception exception) {
			LOGGER.error("Application Initialization Error {}",new SimpleDateFormat(RegistrationConstants.HH_MM_SS).format(System.currentTimeMillis()),
					exception);
		}
	}


	@Override
	public void stop() {
		try {
			super.stop();
			getClientCryptoFacade().getClientSecurity().closeSecurityInstance();
			LOGGER.info("Closed the Client Security Instance");
		} catch (Exception exception) {
			LOGGER.error("REGISTRATION - APPLICATION INITILIZATION - REGISTRATIONAPPINITILIZATION", APPLICATION_NAME,
					APPLICATION_ID,
					"Application Initilization Error"
							+ new SimpleDateFormat(RegistrationConstants.HH_MM_SS).format(System.currentTimeMillis())
							+ ExceptionUtils.getStackTrace(exception));
		} finally {
			System.exit(0);
		}
	}

	// Execute SQL file (Script files on update)
	private void upgradeLocalDatabase() {
		notifyPreloader(new ClientPreLoaderNotification("Checking for any DB upgrades started..."));
		SoftwareUpdateHandler softwareUpdateHandler = applicationContext.getBean(SoftwareUpdateHandler.class);
		ResponseDTO responseDTO = softwareUpdateHandler.updateDerbyDB();
		if(responseDTO == null) {
			notifyPreloader(new ClientPreLoaderNotification("Nothing to be upgraded."));
			return;
		}

		if(responseDTO.getErrorResponseDTOs() != null) {
			ErrorResponseDTO errorResponseDTO = responseDTO.getErrorResponseDTOs().get(0);
			if (RegistrationConstants.BACKUP_PREVIOUS_SUCCESS.equalsIgnoreCase(errorResponseDTO.getMessage())) {
				notifyPreloader(new ClientPreLoaderErrorNotification(new RegBaseCheckedException(
						RegistrationConstants.BACKUP_PREVIOUS_SUCCESS,
						RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SQL_EXECUTION_FAILED_AND_REPLACED)
								+ RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.RESTART_APPLICATION)
				)));
			} else {
				notifyPreloader(new ClientPreLoaderErrorNotification(new RegBaseCheckedException(
						errorResponseDTO.getCode(), errorResponseDTO.getMessage()
				)));
			}
		}

		if(responseDTO.getSuccessResponseDTO() != null) {
			auditFactory.audit(AuditEvent.CLIENT_DB_UPGRADE_SCRIPTS, Components.CLIENT_UPGRADE, 
					RegistrationConstants.APPLICATION_NAME, AuditReferenceIdTypes.APPLICATION_ID.getReferenceTypeId());
			
			notifyPreloader(new ClientPreLoaderNotification(responseDTO.getSuccessResponseDTO().getMessage()));
		}
	}

	private void discoverDevices() {
		BaseService baseService = applicationContext.getBean("baseService", BaseService.class);
		if(baseService.isInitialSync()) {
			return;
		}

		notifyPreloader(new ClientPreLoaderNotification("Biometric device scanning started..."));
		MosipDeviceSpecificationFactory deviceSpecificationFactory = applicationContext.getBean(MosipDeviceSpecificationFactory.class);
		notifyPreloader(new ClientPreLoaderNotification("Scanning port "+
				deviceSpecificationFactory.getPortFrom()+" - "+deviceSpecificationFactory.getPortTo()));
		deviceSpecificationFactory.initializeDeviceMap(false);
		notifyPreloader(new ClientPreLoaderNotification(deviceSpecificationFactory.getAvailableDeviceInfoMap().size() + " devices discovered."));
	}


	private void handleInitialSync() {
		BaseService baseService = applicationContext.getBean("baseService", BaseService.class);
		ServiceDelegateUtil serviceDelegateUtil = applicationContext.getBean(ServiceDelegateUtil.class);
		LoginService loginService = applicationContext.getBean(LoginService.class);

		notifyPreloader(new ClientPreLoaderNotification("Checking server connectivity..."));
		boolean status = serviceDelegateUtil.isNetworkAvailable();
		notifyPreloader(new ClientPreLoaderNotification("Machine is "+ ( status ? "ONLINE" : "OFFLINE")));

		if(!status || baseService.isInitialSync())  {
			return;
		}

		AuthTokenUtilService authTokenUtilService = applicationContext.getBean(AuthTokenUtilService.class);

		if(authTokenUtilService.hasAnyValidToken()) {
			long start = System.currentTimeMillis();
			notifyPreloader(new ClientPreLoaderNotification("Client settings startup sync started..."));
			loginService.initialSync(RegistrationConstants.JOB_TRIGGER_POINT_SYSTEM);
			syncCompleted = true;
			notifyPreloader(new ClientPreLoaderNotification("Client setting startup sync completed in " +
					((System.currentTimeMillis() - start)/1000) + " seconds." ));
		}
		else {
			notifyPreloader(new ClientPreLoaderNotification("Warning : ** NO VALID AUTH-TOKEN TO SYNC **"));
		}

		JobConfigurationService jobConfigurationService = applicationContext.getBean(JobConfigurationService.class);
		jobConfigurationService.initiateJobs();
		notifyPreloader(new ClientPreLoaderNotification("Job scheduler started."));
	}

	private void setupLanguages() throws PreConditionCheckException {
		BaseService baseService = applicationContext.getBean("baseService", BaseService.class);
		io.mosip.registration.context.ApplicationContext.getInstance().setMandatoryLanguages(baseService.getMandatoryLanguages());
		io.mosip.registration.context.ApplicationContext.getInstance().setOptionalLanguages(baseService.getOptionalLanguages());
	}

	private void setupAppProperties() {
		GlobalParamService globalParamService = applicationContext.getBean(GlobalParamService.class);
		LocalConfigService localConfigService = applicationContext.getBean(LocalConfigService.class);
		Map<String, Object> globalProps = globalParamService.getGlobalParams();
		globalProps.putAll(localConfigService.getLocalConfigurations());
		io.mosip.registration.context.ApplicationContext.setApplicationMap(globalProps);
	}

	protected void setupResourceBundleBasedOnDefaultAppLang() throws RegBaseCheckedException {
		//load all bundles in memory and sets default application language
		io.mosip.registration.context.ApplicationContext.loadResources();

		ResourceBundle messageBundle = io.mosip.registration.context.ApplicationContext.getBundle(
				io.mosip.registration.context.ApplicationContext.applicationLanguage(),
				RegistrationConstants.MESSAGES);
		Validations.setResourceBundle(messageBundle);
		RegistrationUIConstants.setBundle(messageBundle);
	}

	private ClientCryptoFacade getClientCryptoFacade() {
		return applicationContext.getBean(ClientCryptoFacade.class);
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public static Stage getPrimaryStage() {
		return applicationPrimaryStage;
	}

	public static void setPrimaryStage(Stage primaryStage) {
		applicationPrimaryStage = primaryStage;
	}
	
	public static String getApplicationStartTime() {
		return applicationStartTime;
	}

	public static boolean isSyncCompleted() {
		return syncCompleted;
	}
}
