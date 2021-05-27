package io.mosip.registration.api.geoposition;

public class GeoPositionUtil {


    /**
     * Distance per longitude.
     *
     * @param lat the lat
     * @return the double
     */
    protected static double distPerLng(double lat) {
        return 0.0003121092 * Math.pow(lat, 4) + 0.0101182384 * Math.pow(lat, 3) - 17.2385140059 * lat * lat
                + 5.5485277537 * lat + 111301.967182595;
    }

    /**
     * Distance per latitude.
     *
     * @param lat
     * @return
     */
    protected static double distPerLat(double lat) {
        return -0.000000487305676 * Math.pow(lat, 4) - 0.0033668574 * Math.pow(lat, 3) + 0.4601181791 * lat * lat
                - 1.4558127346 * lat + 110579.25662316;
    }

}
