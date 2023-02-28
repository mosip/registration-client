package io.mosip.registration.test.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.kernel.auditmanager.request.AuditRequestDto;
import io.mosip.kernel.core.auditmanager.spi.AuditHandler;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.audit.AuditManagerSerivceImpl;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dao.AuditDAO;
import io.mosip.registration.dao.RegistrationDAO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.service.packet.RegPacketStatusService;
import io.mosip.registration.util.healthcheck.RegistrationSystemPropertiesChecker;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ InetAddress.class, SessionContext.class, ApplicationContext.class, DateUtils.class })
public class AuditFactoryTest {

	@Mock
    private RegistrationSystemPropertiesChecker registrationSystemPropertiesChecker;
    @InjectMocks
    private AuditManagerSerivceImpl auditManagerSerivceImpl = new AuditManagerSerivceImpl();
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
	
    
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@Mock
	private AuditHandler<AuditRequestDto> auditHandler;
	@InjectMocks
	private AuditManagerSerivceImpl auditFactory;

	@Mock
    private SessionContext sessionContext;
	
	@Mock
	private RegistrationDAO registrationDAO;

	@Mock
	private RegPacketStatusService regPacketStatusService;

	@Mock
	Map<String, Object> applicationMap;

	@Mock
	private GlobalParamService globalParamService;

	@Mock
	private LocalConfigService localConfigService;

	@Mock
	private SessionContext.SecurityContext securityContext;

	@Mock
	private AuditDAO auditDAO;

	@Before
	public void init() throws Exception {
		PowerMockito.mockStatic(InetAddress.class, SessionContext.class);
		PowerMockito.when(SessionContext.securityContext()).thenReturn(securityContext);
		Map<String, Object> value = new HashMap<>();
		value.put(RegistrationConstants.REGISTRATION_DATA, new RegistrationDTO());
		PowerMockito.when(securityContext.getUserId()).thenReturn("user");
		PowerMockito.when(SessionContext.map()).thenReturn(value);

		Map<String, Object> map = new HashMap<>();
		map.put(RegistrationConstants.AUDIT_LOG_DELETION_CONFIGURED_DAYS, "5");
		map.put(RegistrationConstants.DEFAULT_HOST_IP, "127.0.0.0");
		map.put(RegistrationConstants.DEFAULT_HOST_NAME, "LOCALHOST");
		map.put(RegistrationConstants.APP_NAME, "REGISTRATION");
		map.put(RegistrationConstants.APP_ID, "REG");

		PowerMockito.mockStatic(ApplicationContext.class);
		when(ApplicationContext.map()).thenReturn(map);
	}

	@Test
	public void auditTest() throws Exception {
		PowerMockito.when(InetAddress.getLocalHost()).thenCallRealMethod();
		PowerMockito.doReturn("userId").when(SessionContext.class, "userId");
		PowerMockito.doReturn("userName").when(SessionContext.class, "userName");
		when(auditHandler.addAudit(Mockito.any(AuditRequestDto.class))).thenReturn(true);

		auditFactory.audit(AuditEvent.PACKET_APPROVED, Components.PACKET_CREATOR, "id", "ref");

	}
	
	 @Test
	    public void testAuditWithParams_HostnameAvailable() {
		 AuditEvent auditEventEnum = AuditEvent.NAV_RE_REGISTRATION ;
	        Components appModuleEnum = Components.REGISTRATION_CONTROLLER;
	        String refId = "1234";
	        String refIdType = "RegistrationId";
	        Map<String, String> map = new HashMap<String, String>();
	    
	        when(sessionContext.isSessionContextAvailable()).thenReturn(true);
	        //when(sessionContext.isSessionContextAvailable()).thenReturn(true);
	        //when(sessionContext.getRegistrationDTO()).thenReturn(registrationDTO);
	        
	        when(sessionContext.userId()).thenReturn(refId);
	       // when(registrationDTO.getRegistrationId()).thenReturn(refId);
	        
	        
	        auditFactory.auditWithParams(auditEventEnum, appModuleEnum, refId, refIdType, map);
	        // Assert
//	        verify(sessionContext, times(1)).isSessionContextAvailable();
//	        verify(sessionContext, times(1)).userId();
//	        assertEquals(AuditReferenceIdTypes.USER_ID.getReferenceTypeId(), refIdType);
	    }
	    

	@Test
	public void auditTestWithDefaultValues() throws Exception {
		PowerMockito.mockStatic(InetAddress.class);
		PowerMockito.when(InetAddress.getLocalHost()).thenThrow(new UnknownHostException("Unknown"));
		when(auditHandler.addAudit(Mockito.any(AuditRequestDto.class))).thenReturn(true);

		auditFactory.audit(AuditEvent.PACKET_APPROVED, Components.PACKET_CREATOR, "id", "ref");
	}

	@Test
	public void auditTestWithParams() throws Exception {
//		RegistrationSystemPropertiesChecker registrationSystemPropertiesChecker = Mockito.mock(RegistrationSystemPropertiesChecker.class);
//		registrationSystemPropertiesChecker.getMachineId();
//	
		Map<String, String> map = new HashMap<String, String>();
		map.put("<time>", "1");
		map.put("<count>", "1");
		
		PowerMockito.mockStatic(InetAddress.class);
		PowerMockito.when(InetAddress.getLocalHost()).thenThrow(new UnknownHostException("Unknown"));
		when(auditHandler.addAudit(Mockito.any(AuditRequestDto.class))).thenReturn(true);

		auditFactory.auditWithParams(AuditEvent.REG_BIO_CAPTURE_DETAILS, Components.PACKET_HANDLER, "id", "ref",map);
	}

	// Java11 correction
	@Test
	public void deleteAuditLogsSuccessTest() {
		when(ApplicationContext.map()).thenReturn(applicationMap);
		Mockito.when(applicationMap.get(Mockito.anyString())).thenReturn("2020-12-12 12:12:12");
		PowerMockito.mockStatic(DateUtils.class);
		PowerMockito.when(DateUtils.parseToLocalDateTime(Mockito.anyString())).thenReturn(LocalDateTime.now());
		Mockito.doNothing().when(auditDAO).deleteAudits(Mockito.any(LocalDateTime.class));
		assertSame(RegistrationConstants.AUDIT_LOGS_DELETION_SUCESS_MSG,
				auditFactory.deleteAuditLogs().getSuccessResponseDTO().getMessage());
	}
	
	@Test
	public void deleteAuditLogsExceptionTest() {
		when(ApplicationContext.map()).thenReturn(applicationMap);
		Mockito.when(applicationMap.get(Mockito.anyString())).thenReturn("2020-12-12 12:12:12");
		PowerMockito.mockStatic(DateUtils.class);
		PowerMockito.when(DateUtils.parseToLocalDateTime(Mockito.anyString())).thenReturn(LocalDateTime.now());
		Mockito.doNothing().when(auditDAO).deleteAudits(Mockito.any(LocalDateTime.class));
//		Mockito.doThrow(RuntimeException.class).when(setSuccessResponse(Mockito.any(), Mockito.anyString(), null));
		assertSame(RegistrationConstants.AUDIT_LOGS_DELETION_SUCESS_MSG,
				auditFactory.deleteAuditLogs().getSuccessResponseDTO().getMessage());
	}

	@Test
	public void auditLogsDeletionFailureTest() {
		Mockito.when(applicationMap.get(Mockito.anyString())).thenReturn(null);
		assertNotNull(auditFactory.deleteAuditLogs().getSuccessResponseDTO());
		assertSame(RegistrationConstants.AUDIT_LOGS_DELETION_EMPTY_MSG,
				auditFactory.deleteAuditLogs().getSuccessResponseDTO().getMessage());

	}

	/*
	 * @Test public void auditLogsDeleteException(){ try{
	 * when(ApplicationContext.map()).thenReturn(applicationMap);
	 * Mockito.when(applicationMap.get(Mockito.anyString())).
	 * thenReturn("2020-12-12 12:12:12"); PowerMockito.mockStatic(DateUtils.class);
	 * PowerMockito.when(DateUtils.parseToLocalDateTime(Mockito.anyString())).
	 * thenReturn(LocalDateTime.now());
	 * Mockito.doNothing().when(auditDAO).deleteAudits(Mockito.any(LocalDateTime.
	 * class)); assertSame(RegistrationConstants.AUDIT_LOGS_DELETION_SUCESS_MSG,
	 * auditFactory.deleteAuditLogs().getSuccessResponseDTO().getMessage());
	 * }catch(RuntimeException runtimeException) {
	 * 
	 * } }
	 */

	// Java11 correction

	/*
	 * @Test public void auditLogsDeletionExceptionTest() { //
	 * Mockito.when(auditLogControlDAO.get(new //
	 * Timestamp(Mockito.anyLong()))).thenThrow(DataException.class); //
	 * PowerMockito.when(DateUtils.parseToLocalDateTime(null)).thenThrow(
	 * RuntimeException.class);
	 * 
	 * assertSame(RegistrationConstants.AUDIT_LOGS_DELETION_EMPTY_MSG,
	 * auditFactory.deleteAuditLogs().getErrorResponseDTOs().get(0).getMessage());
	 * 
	 * }
	 */

}
