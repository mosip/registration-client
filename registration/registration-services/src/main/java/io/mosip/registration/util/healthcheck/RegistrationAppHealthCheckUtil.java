package io.mosip.registration.util.healthcheck;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.micrometer.core.annotation.Timed;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.config.DaoConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.util.restclient.RestClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.software.os.windows.WindowsOperatingSystem;

/**
 * Registration Health Checker Utility.
 *
 * @author Sivasankar Thalavai
 * @since 1.0.0
 */

@Component
public class RegistrationAppHealthCheckUtil {

	private static final Logger LOGGER = AppConfig.getLogger(RegistrationAppHealthCheckUtil.class);

	/** The system info. */
	private static SystemInfo systemInfo;

	/** The operating system. */
	private static OperatingSystem operatingSystem;


	static {
		systemInfo = new SystemInfo();
		operatingSystem = systemInfo.getOperatingSystem();
	}

	/**
	 * This method checks for the Disk Space Availability.
	 * 
	 * <p>
	 * Gets the {@link FileSystem} of the {@link OperatingSystem} that is currently
	 * used. Gets all the {@link OSFileStore} from the fileSystem and takes the
	 * FileStore that matches the current directory and then checks whether the
	 * usable space of the fileStore is greater than required disk space threshold.
	 * If it is greater, then it indicates that the required disk space is
	 * available, else, the required space is not available.
	 * </p>
	 *
	 * @return true, if is disk space available and false, if space is not
	 *         available.
	 */
	public static boolean isDiskSpaceAvailable() {
		LOGGER.info("REGISTRATION - REGISTRATIONAPPHEALTHCHECKUTIL - ISDISKSPACEAVAILABLE", APPLICATION_NAME,
				APPLICATION_ID, "Registration Disk Space Checker had been called.");
		boolean isSpaceAvailable = false;
		FileSystem fileSystem = operatingSystem.getFileSystem();
		String currentDirectory = System.getProperty("user.dir").substring(0, 3);
		OSFileStore[] fileStores = fileSystem.getFileStores();
		Long diskSpaceThreshold = 80000L;
		for (OSFileStore fs : fileStores) {
			if (currentDirectory.equalsIgnoreCase(fs.getMount())) {
				if (fs.getUsableSpace() > diskSpaceThreshold) {
					isSpaceAvailable = true;
					LOGGER.info("REGISTRATION - REGISTRATIONAPPHEALTHCHECKUTIL - ISDISKSPACEAVAILABLE",
							APPLICATION_NAME, APPLICATION_ID, "Required Disk Space Available.");
				} else {
					LOGGER.info("REGISTRATION - REGISTRATIONAPPHEALTHCHECKUTIL - ISDISKSPACEAVAILABLE",
							APPLICATION_NAME, APPLICATION_ID, "Required Disk Space Not Available.");
				}
			}
		}
		LOGGER.info("REGISTRATION - REGISTRATIONAPPHEALTHCHECKUTIL - ISDISKSPACEAVAILABLE", APPLICATION_NAME,
				APPLICATION_ID, "Registration Disk Space Checker had been ended.");
		return isSpaceAvailable;
	}
}
