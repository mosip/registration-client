package io.mosip.registration.geoposition.rxtx;

import gnu.io.*;
import io.mosip.registration.api.geoposition.GeoPositionService;
import io.mosip.registration.api.geoposition.dto.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;

@Component
public class GeoPositionServiceImpl implements GeoPositionService, SerialPortEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoPositionServiceImpl.class);

    private InputStream inputStream = null;

    @Override
    public GeoPosition getGeoPosition(GeoPosition geoPosition) {
        LOGGER.debug("getGeoPosition invoked with port {} and timeout {}",
                geoPosition.getPort(), geoPosition.getTimeout());
        @SuppressWarnings("unchecked")
        Enumeration<CommPortIdentifier> portListEnumeration = CommPortIdentifier.getPortIdentifiers();
        while (portListEnumeration.hasMoreElements()) {
            CommPortIdentifier commPortIdentifier = portListEnumeration.nextElement();
            if(commPortIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                LOGGER.debug("getGeoPosition serial port name {}", commPortIdentifier.getName());
                SerialPort serialPort = null;
                try {
                    serialPort = (SerialPort) commPortIdentifier.open("MOSIP", 0);
                    serialPort.notifyOnDataAvailable(true);
                    serialPort.addEventListener(this);
                    serialPort.setSerialPortParams(geoPosition.getBaudRate(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    inputStream = serialPort.getInputStream();
                    Thread.sleep(geoPosition.getTimeout());

                } catch (PortInUseException | TooManyListenersException | InterruptedException | UnsupportedCommOperationException | IOException e) {
                    LOGGER.error("Failed to open serial port", e);
                } finally {
                    if(serialPort != null) {
                        serialPort.removeEventListener();
                        serialPort.close();
                        serialPort = null;
                    }
                }
            }
        }
        return geoPosition;
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        //TODO
    }
}
