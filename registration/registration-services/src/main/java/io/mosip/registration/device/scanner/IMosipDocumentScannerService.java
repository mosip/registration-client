package io.mosip.registration.device.scanner;

import io.mosip.registration.device.scanner.dto.ScanDevice;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * 
 * @author balamurugan ramamoorthy
 * @since 1.0.0
 */
public interface IMosipDocumentScannerService {

	/**
	 * This method is used to check whether the scanner is connected to the machine
	 * 
	 * @return boolean - the value is true if the scanner is connected
	 */
	@Deprecated
	boolean isConnected();

	/**
	 * This is used to connect the scanner device and get the scanned document
	 * 
	 * @return byte[] - The scanned document data
	 */
	@Deprecated
	BufferedImage scan();

	/**
	 *
	 * @return
	 */
	List<ScanDevice> getDevices();

	/**
	 *
	 * @param deviceName
	 * @return
	 */
	BufferedImage scan(String deviceName);

}