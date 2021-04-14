package io.mosip.registration.test.service.packet;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.mosip.registration.audit.AuditManagerSerivceImpl;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.packet.impl.PacketHandlerServiceImpl;

public class PacketHandlerServiceTest {
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private PacketHandlerServiceImpl packetHandlerServiceImpl;
	@Mock
	private AuditManagerSerivceImpl auditFactory;
	private ResponseDTO mockedSuccessResponse;

	@Before
	public void initialize() {
		mockedSuccessResponse = new ResponseDTO();
		mockedSuccessResponse.setSuccessResponseDTO(new SuccessResponseDTO());
	}



	@Test
	public void testCreationException() throws RegBaseCheckedException {

		ResponseDTO actualResponse = packetHandlerServiceImpl.handle(new RegistrationDTO());

		Assert.assertEquals(RegistrationExceptionConstants.REG_PACKET_CREATION_ERROR_CODE.getErrorCode(),
				actualResponse.getErrorResponseDTOs().get(0).getCode());
	}

	@Test
	public void testHandlerChkException() throws RegBaseCheckedException {
		RegBaseCheckedException exception = new RegBaseCheckedException("errorCode", "errorMsg");
		Assert.assertNotNull(packetHandlerServiceImpl.handle(null).getErrorResponseDTOs());
	}

	@Test
	public void testHandlerAuthenticationException() throws RegBaseCheckedException {
		RegBaseCheckedException exception = new RegBaseCheckedException(
				RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorCode(),
				RegistrationExceptionConstants.AUTH_ADVICE_USR_ERROR.getErrorMessage());

		Assert.assertNotNull(packetHandlerServiceImpl.handle(null).getErrorResponseDTOs());
	}

}
