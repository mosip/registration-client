package io.mosip.registration.api.geoposition.dto;

import lombok.Data;

@Data
public class GeoPosition {

    private String modelName;
    private String port;
    private int timeout;
    private int baudRate;

    //To be set by implementation class
    private double longitude;
    private double latitude;
    private String error;
}
