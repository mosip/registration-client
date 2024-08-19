package io.mosip.registration.dto;

import lombok.Data;

@Data
public class ScanDevice {

    private String serviceName;
    private String id;
    private String name;
    private DeviceType deviceType;
    private String firmware;
    private String serial;
    private String model;

    private int dpi;
    //accepts 4 elements, x, y, width, height (in pixels)
    private int[] frame;
    private int height;
    private int width;
    private String mode;
}
