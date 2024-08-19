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
    private static final String ENABLED = "Y";

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

    @Value("${mosip.registration.gps_device_enable_flag}")
    private String forceGPSDevice;


    /**
     * @param geoPosition
     * @return
     */
    public GeoPosition getMachineGeoPosition(@NonNull GeoPosition geoPosition) {
        if (!ENABLED.equalsIgnoreCase(forceGPSDevice))
            return geoPosition;

        if (geoPositionServiceList == null || geoPositionServiceList.isEmpty()) {
            LOGGER.error("** NO GeoPositionService IMPLEMENTATIONS FOUND to capture co-ordinates!! **");
            geoPosition.setError("GeoPositionService IMPLEMENTATIONS NOT FOUND");
            return geoPosition;
        }

        LOGGER.info("Found {} GeoPositionService", geoPositionServiceList.size());
        for (GeoPositionService geoPositionService : geoPositionServiceList) {
            GeoPosition result = geoPositionService.getGeoPosition(geoPosition);

            if (result != null)
                return result;
        }
        return geoPosition;
    }


    /**
     * @param machineLongitude
     * @param machineLatitude
     * @param centerLongitude
     * @param centerLatitude
     * @return
     */
    public double getDistance(double machineLongitude, double machineLatitude,
                              double centerLongitude, double centerLatitude) {
        double earthRadiusInKM = 6371;
        double longitudeDiff = Math.toRadians(centerLongitude - machineLongitude);
        double latitudeDiff = Math.toRadians(centerLatitude - machineLatitude);

        double var = Math.sin(latitudeDiff / 2) * Math.sin(latitudeDiff / 2) +
                Math.sin(longitudeDiff / 2) * Math.sin(longitudeDiff / 2) * Math.cos(Math.toRadians(machineLatitude)) * Math.cos(Math.toRadians(centerLatitude));
        return earthRadiusInKM * (2 * Math.atan2(Math.sqrt(var), Math.sqrt(1 - var)));
    }
}
