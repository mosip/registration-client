package io.mosip.registration.test.mock.impl;

import java.awt.image.BufferedImage;
import java.util.List;

import io.mosip.registration.api.docscanner.DocScannerService;
import io.mosip.registration.api.docscanner.dto.DocScanDevice;

public class DocScannerServiceImpl implements DocScannerService {

	@Override
	public String getServiceName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BufferedImage scan(DocScanDevice docScanDevice, String deviceType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DocScanDevice> getConnectedDevices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stop(DocScanDevice docScanDevice) {
		// TODO Auto-generated method stub
		
	}

}
