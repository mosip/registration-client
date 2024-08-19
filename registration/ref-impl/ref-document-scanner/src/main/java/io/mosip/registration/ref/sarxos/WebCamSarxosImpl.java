package io.mosip.registration.ref.sarxos;

import com.github.sarxos.webcam.Webcam;
import io.mosip.registration.api.docscanner.DocScannerService;
import io.mosip.registration.dto.DeviceType;
import io.mosip.registration.dto.ScanDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class WebCamSarxosImpl implements DocScannerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebCamSarxosImpl.class);
    private static final String SERVICE_NAME = "WebCamSarxos";


    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public BufferedImage scan(ScanDevice docScanDevice, String deviceType) {
        Optional<Webcam> result = Webcam.getWebcams().stream()
                .filter(c -> c.getName().equals(docScanDevice.getName()))
                .findFirst();

        if(result.isPresent()) {
            LOGGER.debug("WEBCAM {}, getDevice().getResolutions {}, getViewSizes {}", docScanDevice.getName(),
                    result.get().getDevice().getResolutions(), result.get().getViewSizes());
            openDevice(result.get(), docScanDevice.getWidth(), docScanDevice.getHeight());
            if(result.get().isOpen()) {
                BufferedImage bufferedImage = result.get().getImage();
                return (docScanDevice.getFrame() != null && docScanDevice.getFrame().length > 3) ?
                        bufferedImage.getSubimage(docScanDevice.getFrame()[0], docScanDevice.getFrame()[1],
                                docScanDevice.getFrame()[2], docScanDevice.getFrame()[3]) : bufferedImage;
            }
        }
        return null;
    }

    private void openDevice(@NotNull Webcam webcam, int width, int height) {
        LOGGER.info("Opening webcam device ");
        if(webcam.isOpen())
            return;

        Dimension requiredDimension = new Dimension(width, height);
        webcam.setCustomViewSizes(new Dimension[] { requiredDimension });
        webcam.getLock().disable();
        webcam.open();
        Webcam.getDiscoveryService().stop();
    }

    @Override
    public List<ScanDevice> getConnectedDevices() {
        List<ScanDevice> devices = new ArrayList<>();
        for(Webcam webcam : Webcam.getWebcams()) {
            ScanDevice docScanDevice = new ScanDevice();
            docScanDevice.setDeviceType(DeviceType.CAMERA);
            docScanDevice.setName(webcam.getName());
            docScanDevice.setServiceName(getServiceName());
            docScanDevice.setId(SERVICE_NAME+":"+webcam.getName());
            devices.add(docScanDevice);
        }
        return devices;
    }

    @Override
    public void stop(ScanDevice docScanDevice) {
        Optional<Webcam> result = Webcam.getWebcams().stream()
                .filter(c -> c.getName().equals(docScanDevice.getName()))
                .findFirst();

        if(result.get().isOpen())
            result.get().close();

        LOGGER.info("Closing webcam device ");
    }
}
