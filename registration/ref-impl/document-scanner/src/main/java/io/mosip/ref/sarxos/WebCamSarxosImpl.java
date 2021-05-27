package io.mosip.scanner.sarxos;

import com.github.sarxos.webcam.Webcam;
import io.mosip.registration.api.docscanner.DeviceType;
import io.mosip.registration.api.docscanner.DocScannerService;
import io.mosip.registration.api.docscanner.dto.DocScanDevice;
import io.mosip.scanner.morena.MorenaDocScanServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
    public BufferedImage scan(DocScanDevice docScanDevice) {
        Optional<Webcam> result = Webcam.getWebcams().stream()
                .filter(c -> c.getName().equals(docScanDevice.getName()))
                .findFirst();

        if(result.isPresent()) {
            try {
                LOGGER.debug("WEBCAM {}, getDevice().getResolutions {}, getViewSizes {}", docScanDevice.getName(),
                        result.get().getDevice().getResolutions(), result.get().getViewSizes());
                Dimension requiredDimension = new Dimension(docScanDevice.getWidth(), docScanDevice.getHeight());
                result.get().setCustomViewSizes(new Dimension[] { requiredDimension });
                result.get().getLock().disable();
                result.get().open();
                Webcam.getDiscoveryService().stop();
                if(result.get().isOpen()) {
                    BufferedImage bufferedImage = result.get().getImage();
                    return (docScanDevice.getFrame() != null && docScanDevice.getFrame().length > 3) ?
                                bufferedImage.getSubimage(docScanDevice.getFrame()[0], docScanDevice.getFrame()[1],
                                docScanDevice.getFrame()[2], docScanDevice.getFrame()[3]) : bufferedImage;
                }
            } finally {
                if(result.get().isOpen())
                    result.get().close();
            }
        }
        return null;
    }

    @Override
    public List<DocScanDevice> getConnectedDevices() {
        List<DocScanDevice> devices = new ArrayList<>();
        for(Webcam webcam : Webcam.getWebcams()) {
            DocScanDevice docScanDevice = new DocScanDevice();
            docScanDevice.setDeviceType(DeviceType.CAMERA);
            docScanDevice.setName(webcam.getName());
            docScanDevice.setServiceName(getServiceName());
            docScanDevice.setId(SERVICE_NAME+":"+webcam.getName());
            devices.add(docScanDevice);
        }
        return devices;
    }
}
