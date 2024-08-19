package io.mosip.registration.api.docscanner;

import io.mosip.registration.dto.ScanDevice;

import java.awt.image.BufferedImage;
import java.util.List;

public interface DocScannerService {

    String getServiceName();

    BufferedImage scan(ScanDevice docScanDevice, String deviceType);

    List<ScanDevice> getConnectedDevices();

    void stop(ScanDevice docScanDevice);
}
