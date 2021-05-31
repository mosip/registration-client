package io.mosip.registration.api.geoposition;

import io.mosip.registration.api.geoposition.dto.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeoPositionFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoPositionFacade.class);

    @Autowired
    private List<GeoPositionService> geoPositionServiceList;

    @Value("${mosip.registration.gps_serial_port_windows:}")
    private String serialPort_win;

    @Value("${mosip.registration.gps_serial_port_linux:}")
    private String serialPort_linux;

    @Value("${mosip.registration.gps_port_timeout:1000}")
    private int timeout;

    @Value("${mosip.registration.gps_device_model:}")
    private String deviceModelName;

    @Value("${mosip.registration.serial_port.baudrate:9600}")
    private int baudRate;
    
    /**
     *
     * @param geoPosition
     * @return
     */
    public GeoPosition getMachineGeoPosition(@NonNull GeoPosition geoPosition) {
        LOGGER.info("Found {} GeoPositionService", geoPositionServiceList.size());
        for(GeoPositionService geoPositionService : geoPositionServiceList) {
            geoPosition = geoPositionService.getGeoPosition(geoPosition);

            if(geoPosition != null)
                return geoPosition;
        }
        return geoPosition;
    }


    /**
     *
     * @param machineLongitude
     * @param machineLatitude
     * @param centerLongitude
     * @param centerLatitude
     * @return
     */
    //TODO - validate this logic once again
    public double getDistance(double machineLongitude, double machineLatitude,
                              double centerLongitude, double centerLatitude) {
        double a = (machineLatitude - centerLatitude) * GeoPositionUtil.distPerLat(machineLatitude);
        double b = (machineLongitude - centerLongitude) * GeoPositionUtil.distPerLng(machineLongitude);
        return Math.sqrt(a * a + b * b);
    }

}
