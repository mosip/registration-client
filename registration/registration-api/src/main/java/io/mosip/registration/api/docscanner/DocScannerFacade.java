package io.mosip.registration.api.docscanner;

import io.mosip.registration.api.docscanner.dto.DocScanDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "mosip.registration.docscanner")
@Component
public class DocScannerFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocScannerFacade.class);

    //<id> : <deviceId>
    private Map<String, String> id;

    //<id> : <dpi>
    private Map<String, Integer> dpi;

    //<id> : <width>
    private Map<String, Integer> width;

    //<id> : <height>
    private Map<String, Integer> height;

    @Autowired
    private List<DocScannerService> docScannerServiceList;

    public List<DocScanDevice> getConnectedDevices() {
        List<DocScanDevice> allDevices = new ArrayList<>();
        for(DocScannerService service : docScannerServiceList) {
            try {
                allDevices.addAll(service.getConnectedDevices());
            } catch (Throwable t) {
                LOGGER.error("Failed to get connected device list from service " + service.getServiceName(), t);
            }
        }
        return allDevices;
    }

    public List<DocScanDevice> getConnectedCameraDevices() {
        List<DocScanDevice> allDevices = new ArrayList<>();
        for(DocScannerService service : docScannerServiceList) {
            allDevices.addAll(service.getConnectedDevices()
                    .stream().filter(d -> d.getDeviceType().equals(DeviceType.CAMERA)).collect(Collectors.toList()));
        }
        return allDevices;
    }

    public BufferedImage scanDocument(@NonNull DocScanDevice docScanDevice) {
        if(id != null && id.containsValue(docScanDevice.getId())) {
            Optional<String> result = id.keySet().stream().filter( k -> id.get(k).equals(docScanDevice.getId()) ).findFirst();
            if(result.isPresent()) {
                docScanDevice.setDpi(dpi.get(result.get()));
                docScanDevice.setWidth(width.get(result.get()));
                docScanDevice.setHeight(height.get(result.get()));
            }
        }

        LOGGER.debug("Selected device details with configuration fully set : {}", docScanDevice);

        Optional<DocScannerService> result = docScannerServiceList.stream()
                .filter(s -> s.getServiceName().equals(docScanDevice.getServiceName())).findFirst();

        if(result.isPresent()) {
            return result.get().scan(docScanDevice);
        }
        return null;
    }
}
