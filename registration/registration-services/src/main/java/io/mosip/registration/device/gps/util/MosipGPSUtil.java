package io.mosip.registration.device.gps.util;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import org.springframework.stereotype.Component;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.device.gps.dto.GPSPosition;

@Component
public class MosipGPSUtil {

	/** Object for Logger. */
	private static final Logger LOGGER = AppConfig.getLogger(MosipGPSUtil.class);

	/**
	 * Parses the GPRMC.
	 *
	 * @param tokens   the tokens
	 * @param position the position
	 */
	public void parseGPRMC(String[] tokens, GPSPosition position) {

		LOGGER.info(RegistrationConstants.GPS_LOGGER, APPLICATION_NAME, APPLICATION_ID, "parsing GPRMC Singal");

		if (tokens[2].equals("A")) {

			position.setLat(latitude2Decimal(tokens[3], tokens[4]));
			position.setLon(longitude2Decimal(tokens[5], tokens[6]));
			position.setResponse("success");

		} else {
			position.setResponse("failure");
		}

	}

	/**
	 * Decimal to longitude.
	 *
	 * @param lon       the lon
	 * @param direction the direction
	 * @return the float
	 */
	private static double longitude2Decimal(String lon, String direction) {

		LOGGER.info(RegistrationConstants.GPS_LOGGER, APPLICATION_NAME, APPLICATION_ID, "Longitude conversion begins");

		double longitudeDegrees = 0.0;

		if (lon.indexOf('.') != -1) {

			int minutesPosition = lon.indexOf('.') - 2;
			double minutes = Double.parseDouble(lon.substring(minutesPosition));
			double decimalDegrees = Double.parseDouble(lon.substring(minutesPosition)) / 60.0f;

			double degree = Double.parseDouble(lon) - minutes;
			double wholeDegrees = 100.0 * degree / 100;

			longitudeDegrees = wholeDegrees + decimalDegrees;

			if (direction.startsWith("W")) {
				longitudeDegrees = -longitudeDegrees;
			}

			LOGGER.info(RegistrationConstants.GPS_LOGGER, APPLICATION_NAME, APPLICATION_ID,
					"Longitude conversion begins");
		}
		return longitudeDegrees;
	}

	/**
	 * Decimal to latitude conversion.
	 *
	 * @param lat       the lat
	 * @param direction the direction
	 * @return the float
	 */
	private static double latitude2Decimal(String lat, String direction) {

		LOGGER.info(RegistrationConstants.GPS_LOGGER, APPLICATION_NAME, APPLICATION_ID, "Latitude conversion begins");

		double latitudeDegrees = 0.0;

		if (lat.indexOf('.') != -1) {

			int minutesPosition = lat.indexOf('.') - 2;
			double minutes = Double.parseDouble(lat.substring(minutesPosition));
			double decimalDegrees = Double.parseDouble(lat.substring(minutesPosition)) / 60.0f;

			double degree = Double.parseDouble(lat) - minutes;
			double wholeDegrees = 100.0 * degree / 100;

			latitudeDegrees = wholeDegrees + decimalDegrees;

			if (direction.startsWith("S")) {
				latitudeDegrees = -latitudeDegrees;
			}
			LOGGER.info(RegistrationConstants.GPS_LOGGER, APPLICATION_NAME, APPLICATION_ID, "Latitude conversion ends");
		}
		return latitudeDegrees;
	}

}
