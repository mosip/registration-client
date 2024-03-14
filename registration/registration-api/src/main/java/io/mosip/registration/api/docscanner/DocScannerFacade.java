package io.mosip.registration.api.docscanner;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import io.micrometer.core.annotation.Timed;
import io.mosip.registration.api.docscanner.dto.DocScanDevice;

@Component
public class DocScannerFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocScannerFacade.class);

    @Value("#{${mosip.registration.docscanner.id}}")
    private Map<String, String> id;

    @Value("#{${mosip.registration.docscanner.dpi}}")
    private Map<String, Integer> dpi;

    @Value("#{${mosip.registration.docscanner.width}}")
    private Map<String, Integer> width;

    @Value("#{${mosip.registration.docscanner.height}}")
    private Map<String, Integer> height;

    @Autowired
    private List<DocScannerService> docScannerServiceList;

    /**
     * Provides all the devices including both SCANNER and CAMERA devices
     * @return
     */
    public List<DocScanDevice> getConnectedDevices() {
        List<DocScanDevice> allDevices = new ArrayList<>();
        if(docScannerServiceList == null || docScannerServiceList.isEmpty()) {
            LOGGER.warn("** NO DOCUMENT SCANNER SERVICE IMPLEMENTATIONS FOUND!! **");
            return allDevices;
        }

        for(DocScannerService service : docScannerServiceList) {
            try {
                Objects.requireNonNull(service.getConnectedDevices()).forEach(device -> {
                        allDevices.add(setDefaults(device));
                });
            } catch (Throwable t) {
                LOGGER.error("Failed to get connected device list from service " + service.getServiceName(), t);
            }
        }
        return allDevices;
    }

    /**
     * Returns only devices of CAMERA DeviceType
     * @return
     */
    public List<DocScanDevice> getConnectedCameraDevices() {
        List<DocScanDevice> allDevices = new ArrayList<>();
        if(docScannerServiceList == null || docScannerServiceList.isEmpty()) {
            LOGGER.warn("** NO DOCUMENT SCANNER SERVICE IMPLEMENTATIONS FOUND!! **");
            return allDevices;
        }

        for(DocScannerService service : docScannerServiceList) {
            allDevices.addAll(service.getConnectedDevices()
                    .stream()
                    .filter(d -> d.getDeviceType().equals(DeviceType.CAMERA))
                    .map(this::setDefaults)
                    .collect(Collectors.toList()));
        }
        return allDevices;
    }

    @Timed
    public BufferedImage scanDocument(@NonNull DocScanDevice docScanDevice, String deviceType) {
        setDefaults(docScanDevice);
        LOGGER.debug("Selected device details with configuration fully set : {}", docScanDevice);
        Optional<DocScannerService> result = docScannerServiceList.stream()
                .filter(s -> s.getServiceName().equals(docScanDevice.getServiceName())).findFirst();

        if(result.isPresent()) {
            return result.get().scan(docScanDevice, deviceType);
        }
        return null;
    }

    private DocScanDevice setDefaults(@NonNull DocScanDevice docScanDevice) {
        if(id != null && docScanDevice.getId() != null) {
            Optional<String> result = id.keySet().stream().filter( k -> docScanDevice.getId().matches(id.get(k)) ).findFirst();
            if(result.isPresent()) {
                docScanDevice.setDpi(dpi.getOrDefault(result.get(),0));
                docScanDevice.setWidth(width.getOrDefault(result.get(),0));
                docScanDevice.setHeight(height.getOrDefault(result.get(),0));
            }
        }
        return docScanDevice;
    }

    public void stopDevice(@NonNull DocScanDevice docScanDevice) {
        try {
            if(docScannerServiceList == null || docScannerServiceList.isEmpty())
                return;

            Optional<DocScannerService> result = docScannerServiceList.stream()
                    .filter(s -> s.getServiceName().equals(docScanDevice.getServiceName())).findFirst();

            if(result.isPresent())
                result.get().stop(docScanDevice);
        } catch (Exception e) {
            LOGGER.error("Error while stopping device {}", docScanDevice.getId(), e);
        }
    }
}
