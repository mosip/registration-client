package io.mosip.registration.device.scanner.impl;

import org.springframework.stereotype.Service;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.device.scanner.IMosipDocumentScannerService;

/**
 * This class is used to handle all the requests related to scanner devices
 * through Sane Daemon service
 * 
 * @author balamurugan.ramamoorthy
 * @since 1.0.0
 */
@Service
public abstract class DocumentScannerService implements IMosipDocumentScannerService {

	private static final Logger LOGGER = AppConfig.getLogger(DocumentScannerService.class);

	
}
