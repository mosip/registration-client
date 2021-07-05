package io.mosip.registration.service.packet.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.kernel.core.virusscanner.exception.VirusScannerException;
import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.service.BaseService;
import io.mosip.registration.service.packet.RegistrationclientVirusScanService;

/**
 * Implementation class for {@link RegistrationclientVirusScanService}
 * 
 * @author saravanakumar gnanaguru
 * @since 1.0.0
 */
@Service
public class RegistrationPacketVirusScanServiceImpl extends BaseService implements RegistrationclientVirusScanService {

	@Autowired
	private org.springframework.context.ApplicationContext applicationContext;

	private VirusScanner<Boolean, InputStream> virusScanner;

	private static final Logger LOGGER = AppConfig.getLogger(RegistrationPacketVirusScanServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.registration.service.packet.impl.RegistrationPacketVirusScanService#
	 * scanPacket()
	 */
	@Override
	public synchronized ResponseDTO scanPacket() {
		virusScanner = (VirusScanner<Boolean, InputStream>) applicationContext.getBean(VirusScanner.class);
		LOGGER.info("Registration-Client scanning triggered");
		ResponseDTO responseDTO = new ResponseDTO();
		SuccessResponseDTO successResponseDTO = new SuccessResponseDTO();
		List<String> pathList = Arrays.asList(
				String.valueOf(ApplicationContext.map().get(RegistrationConstants.PKT_STORE_LOC)), 
				String.valueOf(ApplicationContext.map().get(RegistrationConstants.PRE_REG_PACKET_LOCATION)),
				String.valueOf(ApplicationContext.map().get(RegistrationConstants.LOGS_PATH)),
				String.valueOf(ApplicationContext.map().get(RegistrationConstants.DB_PATH)),
				String.valueOf(ApplicationContext.map().get(RegistrationConstants.CLIENT_PATH)));

		List<File> filesList = new ArrayList<>();
		List<String> infectedFiles = new ArrayList<>();
		List<ErrorResponseDTO> errorList = new ArrayList<>();
		try {
			for (String path : pathList) {
				filesList.addAll(getFilesFromFolders(path, filesList));
			}

			for (File fileToScan : filesList) {
				LOGGER.debug("Scanning file : {}", fileToScan);
				if (!virusScanner.scanDocument(fileToScan)) {
					infectedFiles.add(fileToScan.getName());
				}
			}
			LOGGER.info("Scanning completed. Infected files : {}", infectedFiles);
			successResponseDTO.setMessage(infectedFiles.isEmpty() ? RegistrationConstants.SUCCESS :
					String.join(";", infectedFiles));
			responseDTO.setSuccessResponseDTO(successResponseDTO);
		} catch (VirusScannerException virusScannerException) {
			LOGGER.error(virusScannerException.getMessage(), virusScannerException);
			setSuccessResponse(responseDTO, RegistrationConstants.ANTIVIRUS_SERVICE_NOT_ACCESSIBLE, null);
			
		} catch (IOException ioException) {
			LOGGER.error(ioException.getMessage(), ioException);
			ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO();
			errorResponseDTO.setCode("IOException");
			errorResponseDTO.setMessage("Error in reading the file");
			errorList.add(errorResponseDTO);
			responseDTO.setErrorResponseDTOs(errorList);
		}
		LOGGER.info("Registration-Client scanning ended");
		return responseDTO;
	}

	/**
	 * This method will get the folder path and return the list of which are present
	 * inside the folder
	 * 
	 * @param folderPath
	 * @param filesList
	 * @return
	 */
	private List<File> getFilesFromFolders(String folderPath, List<File> filesList) {
		File directory = FileUtils.getFile(folderPath);

		// Get all files from a directory.
		File[] filesToScan = directory.listFiles();
		if (filesToScan != null)
			for (File fileToScan : filesToScan) {
				if (fileToScan.isFile()) {
					filesList.add(fileToScan);
				} else if (fileToScan.isDirectory()) {
					getFilesFromFolders(fileToScan.getAbsolutePath(), filesList);
				}
			}
		return filesList;

	}
}
