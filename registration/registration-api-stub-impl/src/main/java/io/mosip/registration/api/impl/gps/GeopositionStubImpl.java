package io.mosip.registration.api.impl.gps;

import io.mosip.registration.api.geoposition.GeoPositionService;
import io.mosip.registration.api.geoposition.dto.GeoPosition;
import org.springframework.stereotype.Component;

@Component
public class GeopositionStubImpl implements GeoPositionService {

    @Override
    public GeoPosition getGeoPosition(GeoPosition geoPosition) {
        geoPosition.setLatitude(0);
        geoPosition.setLongitude(0);
        return geoPosition;
    }
}
