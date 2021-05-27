package io.mosip.registration.api.docscanner.dto;

import io.mosip.registration.api.docscanner.DeviceType;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class DocScanDevice {

    private String serviceName;
    private String id;
    private String name;
    private DeviceType deviceType;

    private int dpi;
    //accepts 4 elements, x, y, width, height (in pixels)
    private int[] frame;
    private int height;
    private int width;
    private String mode;
}
