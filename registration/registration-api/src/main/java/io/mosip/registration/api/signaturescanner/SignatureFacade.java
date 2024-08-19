package io.mosip.registration.api.signaturescanner;

import io.mosip.registration.api.signaturescanner.constant.StreamType;
import io.mosip.registration.dto.ScanDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.*;

@Component
public class SignatureFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignatureFacade.class);

    @Value("#{${mosip.registration.signature.scanner.model}}")
    private Map<String, String> model;


    @Autowired
    private List<SignatureService> signatureServiceList;

    /**
     * Provides all the devices including both SCANNER and CAMERA devices
     *
     * @return
     */
    public List<ScanDevice> getConnectedDevices() {
        List<ScanDevice> allDevices = new ArrayList<>();
        if (signatureServiceList == null || signatureServiceList.isEmpty()) {
            LOGGER.warn("** NO SIGNATURE SCANNER SERVICE IMPLEMENTATIONS FOUND!! **");
            return allDevices;
        }

        for (SignatureService service : signatureServiceList) {
            try {
                Objects.requireNonNull(service.getConnectedDevices()).forEach(device -> {
                    //                      allDevices.add(setDefaults(device));
                    allDevices.add(device);
                });
            } catch (Throwable t) {
                LOGGER.error("Failed to get connected device list from service " + service.getServiceName(), t);
            }
        }

        return allDevices;
    }

    public BufferedImage scanDocument(@NonNull ScanDevice docScanDevice, String deviceType) throws Exception {
        //   setDefaults(docScanDevice);
        LOGGER.debug("Selected device details with configuration fully set : {}", docScanDevice);
        Optional<SignatureService> result = signatureServiceList.stream()
                .filter(s -> s.getServiceName().equals(docScanDevice.getModel())).findFirst();

        if (result.isPresent()) {
            result.get().scan(docScanDevice, deviceType);
        }
        return null;
    }

    /*  private ScanDevice setDefaults(@NonNull ScanDevice docScanDevice) {
          if(model != null && docScanDevice.getModel() != null) {
              Optional<String> result = model.keySet().stream().filter( k -> docScanDevice.getModel().matches(model.get(k)) ).findFirst();
              if(result.isPresent()) {
                  docScanDevice.setDpi(dpi.getOrDefault(result.get(),0));
                  docScanDevice.setWidth(width.getOrDefault(result.get(),0));
                  docScanDevice.setHeight(height.getOrDefault(result.get(),0));
              }
          }
          return docScanDevice;
      }*/
    public byte[] confirm(@NonNull ScanDevice docScanDevice, StreamType streamType) {
        try {
            if (signatureServiceList == null || signatureServiceList.isEmpty())
                return null;

            Optional<SignatureService> result = signatureServiceList.stream()
                    .filter(s -> s.getServiceName().equals(docScanDevice.getModel())).findFirst();

            if (result.isPresent()) {
                result.get().confirm();
                return result.get().loadData(streamType);
            }
        } catch (Exception e) {
            LOGGER.error("Error while stopping device {}", docScanDevice.getModel(), e);
        }
        return null;
    }

    public void retry(@NonNull ScanDevice docScanDevice) {
        try {
            if (signatureServiceList == null || signatureServiceList.isEmpty())
                return;

            Optional<SignatureService> result = signatureServiceList.stream()
                    .filter(s -> s.getServiceName().equals(docScanDevice.getModel())).findFirst();

            if (result.isPresent())
                result.get().retry();
        } catch (Exception e) {
            LOGGER.error("Error while stopping device {}", docScanDevice.getModel(), e);
        }
    }

    public void stopDevice(@NonNull ScanDevice docScanDevice) {
        try {
            if (signatureServiceList == null || signatureServiceList.isEmpty())
                return;

            Optional<SignatureService> result = signatureServiceList.stream()
                    .filter(s -> s.getServiceName().equals(docScanDevice.getModel())).findFirst();

            if (result.isPresent())
                result.get().stop(docScanDevice);
        } catch (Exception e) {
            LOGGER.error("Error while stopping device {}", docScanDevice.getModel(), e);
        }
    }
}
