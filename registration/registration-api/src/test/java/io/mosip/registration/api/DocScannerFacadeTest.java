package io.mosip.registration.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.registration.api.docscanner.DeviceType;
import io.mosip.registration.api.docscanner.DocScannerFacade;
import io.mosip.registration.api.docscanner.DocScannerService;
import io.mosip.registration.api.docscanner.dto.DocScanDevice;

public class DocScannerFacadeTest {

    @Test
    public void getConnectedDevicesTest() {
        DocScannerFacade facade = new DocScannerFacade();
        DocScannerService serviceImpl = getMockDocScannerService();
        ReflectionTestUtils.setField(facade, "docScannerServiceList", Collections.singletonList(serviceImpl));
        List<DocScanDevice> identifiedDevices = facade.getConnectedDevices();
        Assert.assertEquals(2, identifiedDevices.size());
    }

    @Test
    public void getConnectedCameraDevicesTest() {
        DocScannerFacade facade = new DocScannerFacade();
        DocScannerService serviceImpl = getMockDocScannerService();
        ReflectionTestUtils.setField(facade, "docScannerServiceList", Collections.singletonList(serviceImpl));
        List<DocScanDevice> identifiedDevices = facade.getConnectedCameraDevices();
        Assert.assertEquals(1, identifiedDevices.size());
    }

    @Test
    public void getConnectedDevicesNoImplTest() {
        DocScannerFacade facade = new DocScannerFacade();
        facade.getConnectedDevices();
    }

    @Test
    public void getConnectedCameraDevicesNoImplTest() {
        DocScannerFacade facade = new DocScannerFacade();
        List<DocScanDevice> identifiedDevices = facade.getConnectedCameraDevices();
        Assert.assertEquals(0, identifiedDevices.size());
    }

    @Test
    public void scanDocumentTest() {
        DocScannerFacade facade = new DocScannerFacade();
        DocScannerService serviceImpl = getMockDocScannerService();
        ReflectionTestUtils.setField(facade, "docScannerServiceList", Collections.singletonList(serviceImpl));
        DocScanDevice device = new DocScanDevice();
        device.setDeviceType(DeviceType.SCANNER);
        device.setId("SCANNER_001");
        device.setName("SCANNER_001");
        device.setServiceName("Test-Scanner");
        BufferedImage image = facade.scanDocument(device, ".*");
        Assert.assertNotNull(image);
    }

    @Test
    public void scanDocumentWithInvalidServiceNameTest() {
        DocScannerFacade facade = new DocScannerFacade();
        DocScannerService serviceImpl = getMockDocScannerService();
        ReflectionTestUtils.setField(facade, "docScannerServiceList", Collections.singletonList(serviceImpl));
        DocScanDevice device = new DocScanDevice();
        device.setDeviceType(DeviceType.SCANNER);
        device.setId("SCANNER_001");
        device.setName("SCANNER_001");
        device.setServiceName("Null-Scanner");
        BufferedImage image = facade.scanDocument(device, ".*");
        Assert.assertNull(image);
    }

    @Test
    public void scanDocumentWithDefaultsTest() {
        DocScannerFacade facade = new DocScannerFacade();
        Map<String, String> ids = new HashMap<>();
        ids.put("id1", "SCANNER_001");
        ReflectionTestUtils.setField(facade, "id", ids);
        Map<String, Integer> dpis = new HashMap<>();
        dpis.put("id1", 250);
        ReflectionTestUtils.setField(facade, "dpi", dpis);
        Map<String, Integer> widths = new HashMap<>();
        widths.put("id1", 2500);
        ReflectionTestUtils.setField(facade, "width", widths);
        Map<String, Integer> heights = new HashMap<>();
        heights.put("id1", 2500);
        ReflectionTestUtils.setField(facade, "height", heights);

        DocScannerService serviceImpl = getMockDocScannerService();
        ReflectionTestUtils.setField(facade, "docScannerServiceList", Collections.singletonList(serviceImpl));
        DocScanDevice device = new DocScanDevice();
        device.setDeviceType(DeviceType.SCANNER);
        device.setId("SCANNER_001");
        device.setName("SCANNER_001");
        device.setServiceName("Test-Scanner");
        BufferedImage image = facade.scanDocument(device, ".*");
        Assert.assertEquals(2500, device.getHeight());
        Assert.assertEquals(2500, device.getWidth());
        Assert.assertEquals(250, device.getDpi());
    }

    @Test
    public void scanDocumentWithDefaultsTest2() {
        DocScannerFacade facade = new DocScannerFacade();
        Map<String, String> ids = new HashMap<>();
        ids.put("id1", "SCANNER_001");
        ReflectionTestUtils.setField(facade, "id", ids);
        Map<String, Integer> dpis = new HashMap<>();
        dpis.put("id2", 250);
        ReflectionTestUtils.setField(facade, "dpi", dpis);
        Map<String, Integer> widths = new HashMap<>();
        widths.put("id2", 2500);
        ReflectionTestUtils.setField(facade, "width", widths);
        Map<String, Integer> heights = new HashMap<>();
        heights.put("id2", 2500);
        ReflectionTestUtils.setField(facade, "height", heights);

        DocScannerService serviceImpl = getMockDocScannerService();
        ReflectionTestUtils.setField(facade, "docScannerServiceList", Collections.singletonList(serviceImpl));
        DocScanDevice device = new DocScanDevice();
        device.setDeviceType(DeviceType.SCANNER);
        device.setId("SCANNER_001");
        device.setName("SCANNER_001");
        device.setServiceName("Test-Scanner");
        BufferedImage image = facade.scanDocument(device, ".*");
        Assert.assertEquals(0, device.getHeight());
        Assert.assertEquals(0, device.getWidth());
        Assert.assertEquals(0, device.getDpi());
    }

    @Test
    public void stopDeviceTest() {
        DocScannerFacade facade = new DocScannerFacade();
        DocScannerService serviceImpl = getMockDocScannerService();
        ReflectionTestUtils.setField(facade, "docScannerServiceList", Collections.singletonList(serviceImpl));
        DocScanDevice device = new DocScanDevice();
        device.setDeviceType(DeviceType.SCANNER);
        device.setId("SCANNER_001");
        device.setName("SCANNER_001");
        device.setServiceName("Test-Scanner");
        facade.stopDevice(device);
        Assert.assertTrue(true);
    }

    @Test
    public void stopDeviceTest2() {
        DocScannerFacade facade = new DocScannerFacade();
        DocScanDevice device = new DocScanDevice();
        device.setDeviceType(DeviceType.SCANNER);
        device.setId("SCANNER_001");
        device.setName("SCANNER_001");
        device.setServiceName("Test-Scanner");
        facade.stopDevice(device);
        Assert.assertTrue(true);
    }

    private DocScannerService getMockDocScannerService() {
        return new DocScannerService() {
            @Override
            public String getServiceName() {
                return "Test-Scanner";
            }

            @Override
            public BufferedImage scan(DocScanDevice docScanDevice, String deviceType) {
                try {
                    return ImageIO.read(this.getClass().getResourceAsStream("/images/stubdoc.png"));
                } catch (IOException e) { }
                return null;
            }

            @Override
            public List<DocScanDevice> getConnectedDevices() {
                List<DocScanDevice> devices = new ArrayList<>();
                DocScanDevice device1 = new DocScanDevice();
                device1.setDeviceType(DeviceType.SCANNER);
                device1.setId("SCANNER_001");
                device1.setName("SCANNER_001");
                device1.setServiceName(getServiceName());
                devices.add(device1);
                DocScanDevice device2 = new DocScanDevice();
                device2.setDeviceType(DeviceType.CAMERA);
                device2.setId("CAMERA_001");
                device2.setName("CAMERA_001");
                device2.setServiceName(getServiceName());
                devices.add(device2);
                return devices;
            }

            @Override
            public void stop(DocScanDevice docScanDevice) {
                //Do nothing
            }
        };
    }
}