package io.mosip.registration.api.signaturescanner;

import io.mosip.registration.api.signaturescanner.constant.StreamType;
import io.mosip.registration.dto.ScanDevice;

import java.io.IOException;
import java.security.SignatureException;
import java.util.List;

public interface SignatureService {

    String getServiceName();

    void scan(ScanDevice docScanDevice, String deviceType) throws Exception;

    void retry() throws Exception;

    void cancel() throws Exception;

    void confirm() throws Exception;

    byte[] loadData(StreamType streamType) throws SignatureException, IOException;

    List<ScanDevice> getConnectedDevices() throws Exception;

    void stop(ScanDevice docScanDevice);
}
