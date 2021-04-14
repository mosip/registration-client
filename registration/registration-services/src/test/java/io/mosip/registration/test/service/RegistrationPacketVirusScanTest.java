package io.mosip.registration.test.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import io.mosip.kernel.core.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.mosip.kernel.core.virusscanner.exception.VirusScannerException;
import io.mosip.kernel.core.virusscanner.spi.VirusScanner;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.service.packet.impl.RegistrationPacketVirusScanServiceImpl;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ FileUtils.class })
public class RegistrationPacketVirusScanTest {

	@Rule
	public MockitoRule MockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private RegistrationPacketVirusScanServiceImpl registrationPacketVirusScanServiceImpl;

	@Mock
	private org.springframework.context.ApplicationContext applicationContext;

	@Mock
	private VirusScanner mockScanner = new VirusScanner<Boolean, InputStream>() {
		@Override
		public Boolean scanFile(String fileName) {
			return true;
		}

		@Override
		public Boolean scanFile(InputStream file) {
			return true;
		}

		@Override
		public Boolean scanFolder(String folderPath) {
			return true;
		}

		@Override
		public Boolean scanDocument(byte[] array) throws IOException {
			return true;
		}

		@Override
		public Boolean scanDocument(File doc) throws IOException {
			return true;
		}
	};
	
	@Before
	public void Initialize() {
		Map<String,Object> appMap = new HashMap<>();
		appMap.put("mosip.registration.registration_packet_store_location", "..//packets");
		appMap.put("mosip.registration.registration_pre_reg_packet_location", "..//PreRegPacketStore");
		appMap.put("mosip.registration.database_path", "..//reg");
		appMap.put("mosip.registration.logs_path", "..//Logs");
		ApplicationContext.getInstance().setApplicationMap(appMap);

		Mockito.when(applicationContext.getBean(VirusScanner.class)).thenReturn(mockScanner);
		PowerMockito.mockStatic(FileUtils.class);
		Mockito.when(FileUtils.getFile(Mockito.any())).thenReturn(new File("../pom.xml"));
	}

	
	@Test
	public void scanPacket() throws IOException {
		byte[] bytes = new byte[0];
		Mockito.when(mockScanner.scanDocument(bytes)).thenReturn(true);
		assertEquals("Success",registrationPacketVirusScanServiceImpl.scanPacket().getSuccessResponseDTO().getMessage());
	}
	
	@Test
	public void scanPacketNegative() throws IOException {
		byte[] bytes = new byte[0];
		Mockito.when(mockScanner.scanDocument(bytes)).thenReturn(false);
		assertNotNull(registrationPacketVirusScanServiceImpl.scanPacket().getSuccessResponseDTO().getMessage());
	}
	
	@Test
	public void scanPacketVirusScannerException() throws IOException {
		Mockito.when(mockScanner.scanDocument(new File("test"))).thenThrow(new VirusScannerException());
		assertNotNull(registrationPacketVirusScanServiceImpl.scanPacket().getSuccessResponseDTO());
	}
	
	@Test
	public void scanPacketIOException() throws IOException {
		File mockFile = Mockito.mock(File.class);
		Mockito.when(mockFile.listFiles()).thenReturn(new File[] {mockFile});
		Mockito.when(FileUtils.getFile(Mockito.any())).thenReturn(mockFile);
		Mockito.when(mockFile.isFile()).thenReturn(true);
		Mockito.when(mockScanner.scanDocument(Mockito.any(File.class))).thenThrow(new IOException());
		assertNotNull(registrationPacketVirusScanServiceImpl.scanPacket().getErrorResponseDTOs());
	}
}
