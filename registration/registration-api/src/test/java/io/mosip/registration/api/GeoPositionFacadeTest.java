package io.mosip.registration.api;

import io.mosip.registration.api.geoposition.GeoPositionFacade;
import io.mosip.registration.api.geoposition.GeoPositionService;
import io.mosip.registration.api.geoposition.dto.GeoPosition;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

public class GeoPositionFacadeTest {

    @Test
    public void getDistanceTest() {
        GeoPositionFacade facade = new GeoPositionFacade();
        double distance = facade.getDistance(-6.453278, 34.52117,
                -6.453278, 34.52117);
        Assert.assertEquals(0, distance, 0);
    }

    @Test
    public void getDistanceTest1() {
        GeoPositionFacade facade = new GeoPositionFacade();
        double distance = facade.getDistance(77.5963265, 12.9107832,
                77.6011961, 12.9077343);
        Assert.assertEquals(0.6272937029017828, distance, 0);
    }

    @Test
    public void getMachineGeoPositionTest() {
        GeoPositionFacade facade = new GeoPositionFacade();
        GeoPositionService serviceImpl = getMockGeoPositionService();
        ReflectionTestUtils.setField(facade, "forceGPSDevice", "Y");
        ReflectionTestUtils.setField(facade, "geoPositionServiceList", Collections.singletonList(serviceImpl));
        GeoPosition geoPosition = new GeoPosition();
        geoPosition = facade.getMachineGeoPosition(geoPosition);
        Assert.assertNotNull(geoPosition);
    }

    @Test
    public void getMachineGeoPositionTest2() {
        GeoPositionFacade facade = new GeoPositionFacade();
        GeoPositionService serviceImpl = getMockGeoPositionService2();
        ReflectionTestUtils.setField(facade, "forceGPSDevice", "Y");
        ReflectionTestUtils.setField(facade, "geoPositionServiceList", Collections.singletonList(serviceImpl));
        GeoPosition geoPosition = new GeoPosition();
        geoPosition = facade.getMachineGeoPosition(geoPosition);
        Assert.assertNotNull(geoPosition);
    }

    @Test
    public void getMachineGeoPositionNoImplTest() {
        GeoPositionFacade facade = new GeoPositionFacade();
        ReflectionTestUtils.setField(facade, "forceGPSDevice", "Y");
        GeoPosition geoPosition = new GeoPosition();
        geoPosition = facade.getMachineGeoPosition(geoPosition);
        Assert.assertNotNull(geoPosition.getError());
        Assert.assertEquals("GeoPositionService IMPLEMENTATIONS NOT FOUND", geoPosition.getError());
    }

    @Test
    public void getMachineGeoPositionNoImplTestWithoutForceCapture() {
        GeoPositionFacade facade = new GeoPositionFacade();
        ReflectionTestUtils.setField(facade, "forceGPSDevice", "N");
        GeoPosition geoPosition = new GeoPosition();
        geoPosition = facade.getMachineGeoPosition(geoPosition);
        Assert.assertNotNull(geoPosition);
    }

    @Test
    public void getMachineGeoPositionNoImplTest2WithoutForceCapture() {
        GeoPositionFacade facade = new GeoPositionFacade();
        ReflectionTestUtils.setField(facade, "forceGPSDevice", "N");
        GeoPosition geoPosition = facade.getMachineGeoPosition(null);
        Assert.assertNull(geoPosition);
    }

    private GeoPositionService getMockGeoPositionService() {
        return new GeoPositionService() {
            @Override
            public GeoPosition getGeoPosition(GeoPosition geoPosition) {
                geoPosition.setLatitude(12.9107832);
                geoPosition.setLongitude(77.5963265);
                return geoPosition;
            }
        };
    }

    private GeoPositionService getMockGeoPositionService2() {
        return new GeoPositionService() {
            @Override
            public GeoPosition getGeoPosition(GeoPosition geoPosition) {
                return null;
            }
        };
    }
}
