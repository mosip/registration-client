package io.mosip.registration.device.gps.dto;

public class GPSPosition {

	/** The latitudeFromGps. */
	private double latitudeFromGps = 0.0;

	/** The longitudeFromGps. */
	private double longitudeFromGps = 0.0;

	/** The response. */
	private String response = "";

	/**
	 * This method gets the latitude from GPS.
	 *
	 * @return the latitudeFromGps
	 */

	public double getLat() {
		return latitudeFromGps;
	}

	/**
	 * This method sets the latitude.
	 *
	 * @param lat the new lat
	 */

	public void setLat(double lat) {
		this.latitudeFromGps = lat;
	}

	/**
	 * This method gets the longitude from GPS.
	 *
	 * @return the longitudeFromGps
	 */

	public double getLon() {
		return longitudeFromGps;
	}

	/**
	 * This method sets the longitude.
	 *
	 * @param lon the new lon
	 */

	public void setLon(double lon) {
		this.longitudeFromGps = lon;
	}

	/**
	 * This method gets the response.
	 *
	 * @return the response
	 */

	public String getResponse() {
		return response;
	}

	/**
	 * This method sets the response.
	 * 
	 * @param response the response to set
	 */
	public void setResponse(String response) {
		this.response = response;
	}
}
