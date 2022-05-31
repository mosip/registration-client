package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dao.impl.RegistrationDAOImpl;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.RegistrationCenterDetailDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.RegistrationMetaDataDTO;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.dto.schema.ValuesDTO;
import io.mosip.registration.entity.Registration;
import io.mosip.registration.entity.UserDetail;
import io.mosip.registration.enums.FlowType;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.RegistrationRepository;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.service.impl.IdentitySchemaServiceImpl;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ SessionContext.class })
public class RegistrationDAOTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private RegistrationDAOImpl registrationDAOImpl;
	@Mock
	private RegistrationRepository registrationRepository;

	@Mock
	private IdentitySchemaService identitySchemaService;
	
	@Mock
	private IdentitySchemaServiceImpl identitySchemaServiceImpl;
	
	Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	@Before
	public void initialize() throws Exception {

		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		List<String> roles = Arrays.asList("SUPERADMIN", "SUPERVISOR");
		RegistrationCenterDetailDTO center = new RegistrationCenterDetailDTO();
		center.setRegistrationCenterId("abc123");

		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		PowerMockito.when(SessionContext.userContext().getUserId()).thenReturn("mosip");
		PowerMockito.when(SessionContext.userContext().getName()).thenReturn("mosip");
		PowerMockito.when(SessionContext.userContext().getRoles()).thenReturn(roles);
		PowerMockito.when(SessionContext.userContext().getRegistrationCenterDetailDTO()).thenReturn(center);
	}

	@Test
	@Ignore
	public void testSaveRegistration() throws RegBaseCheckedException {
		RegistrationDTO registrationDTO = new RegistrationDTO();
		RegistrationMetaDataDTO registrationMetaDataDTO=new RegistrationMetaDataDTO();
		
		List<ValuesDTO> fullNames = new ArrayList<>();
		ValuesDTO valuesDTO = new ValuesDTO();
		valuesDTO.setLanguage("eng");
		valuesDTO.setValue("Individual Name");
		fullNames.add(valuesDTO);		
		registrationDTO.getDemographics().put("fullName", fullNames);
		
		registrationDTO.setRegistrationMetaDataDTO(registrationMetaDataDTO);
		//registrationDTO.getRegistrationMetaDataDTO().setRegistrationCategory("New");
		when(registrationRepository.create(Mockito.any(Registration.class))).thenReturn(new Registration());

		registrationDAOImpl.save("../PacketStore/28-Sep-2018/111111", registrationDTO);
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void testTransactionException() throws RegBaseCheckedException {
		when(registrationRepository.create(Mockito.any(Registration.class))).thenThrow(RegBaseUncheckedException.class);

		registrationDAOImpl.save("file", new RegistrationDTO());
	}

	@Test
	public void getRegistrationByStatusTest() {
		List<Registration> packetLists = new ArrayList<>();
		Registration reg = new Registration();
		packetLists.add(reg);
		List<String> packetNames = Arrays.asList("PUSHED", "EXPORTED", "resend", "E");
		Mockito.when(registrationRepository.findByStatusCodes("PUSHED", "EXPORTED", "resend", "E"))
				.thenReturn(packetLists);
		assertEquals(packetLists, registrationDAOImpl.getRegistrationByStatus(packetNames));
	}

	@Test
	public void updateRegStatusTest() {
		Registration updatedPacket = new Registration();
		updatedPacket.setUploadCount((short)0);
		Mockito.when(registrationRepository.findByPacketId(Mockito.any())).thenReturn(updatedPacket);
		Mockito.when(registrationRepository.update(updatedPacket)).thenReturn(updatedPacket);
		
		PacketStatusDTO packetStatusDTO=new PacketStatusDTO();
		packetStatusDTO.setPacketClientStatus("P");
		assertEquals(updatedPacket, registrationDAOImpl.updateRegStatus(packetStatusDTO));
	}

	@Test
	public void testUpdateStatusRegistration() throws RegBaseCheckedException {

		Registration regobjectrequest = new Registration();
		regobjectrequest.setId("123456");
		regobjectrequest.setClientStatusCode("R");
		regobjectrequest.setUpdBy("mosip");
		regobjectrequest.setApproverRoleCode("SUPERADMIN");
		regobjectrequest.setAckFilename("file1");

		when(registrationRepository.getOne(Mockito.anyString())).thenReturn(regobjectrequest);
		Registration regobj1 = registrationRepository.getOne("123456");
		assertEquals("123456", regobj1.getId());
		assertEquals("mosip", regobj1.getUpdBy());
		assertEquals("R", regobj1.getClientStatusCode());
		assertEquals("SUPERADMIN", regobj1.getApproverRoleCode());
		assertEquals("file1", regobj1.getAckFilename());

		Registration registration = new Registration();
		registration.setClientStatusCode("A");
		registration.setApproverUsrId("Mosip1214");
		registration.setStatusComment("");
		registration.setUpdBy("Mosip1214");

		when(registrationRepository.update(regobj1)).thenReturn(registration);
		Registration regobj = registrationDAOImpl.updateRegistration("123456", "", "A");
		assertEquals("Mosip1214", regobj.getUpdBy());
		assertEquals("A", regobj.getClientStatusCode());
		assertEquals("Mosip1214", regobj.getApproverUsrId());
		assertEquals("", regobj.getStatusComment());
	}

	@Test
	public void testGetRegistrationsByStatus() {

		List<Registration> details = new ArrayList<>();
		Registration regobject = new Registration();
		UserDetail regUserDetail = new UserDetail();

		regUserDetail.setId("Mosip123");
		regUserDetail.setName("RegistrationOfficer");

		regobject.setId("123456");
		regobject.setClientStatusCode("R");
		regobject.setCrBy("Mosip123");
		regobject.setAckFilename("file1");

		//regobject.setUserdetail(regUserDetail);
		details.add(regobject);

		Mockito.when(registrationRepository.findByclientStatusCodeOrderByCrDtime("R")).thenReturn(details);

		List<Registration> enrollmentsByStatus = registrationDAOImpl.getEnrollmentByStatus("R");
		assertTrue(enrollmentsByStatus.size() > 0);
		assertEquals("123456", enrollmentsByStatus.get(0).getId());
		assertEquals("R", enrollmentsByStatus.get(0).getClientStatusCode());
		assertEquals("Mosip123", enrollmentsByStatus.get(0).getCrBy());
		//assertEquals("RegistrationOfficer", enrollmentsByStatus.get(0).getUserdetail().getName());
		assertEquals("file1", enrollmentsByStatus.get(0).getAckFilename());
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void testValidateException() throws RegBaseCheckedException {
		when(registrationRepository.update(Mockito.any())).thenThrow(RegBaseUncheckedException.class);
		registrationDAOImpl.updateRegistration(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
	}

	@Test
	public void getAllReRegistrationPacketsTest() {
		List<Registration> registrations = new LinkedList<>();
		List<String> status = Arrays.asList("REREGISTER", "REJECTED");
		when(registrationRepository.findByClientStatusCodeAndServerStatusCodeIn(Mockito.anyString(), Mockito.any()))
				.thenReturn(registrations);
		assertEquals(registrationDAOImpl.getAllReRegistrationPackets("Approved", status), registrations);
	}

	@Test
	public void updatePacketSyncStatusTest() {
		Registration regobjectrequest = new Registration();
		regobjectrequest.setId("123456");
		regobjectrequest.setPacketId("123456");
		regobjectrequest.setClientStatusCode("R");
		regobjectrequest.setUpdBy("mosip");
		regobjectrequest.setApproverRoleCode("SUPERADMIN");
		regobjectrequest.setAckFilename("file1");

		PacketStatusDTO packetStatusDTO = new PacketStatusDTO();
		packetStatusDTO.setFileName("123456");
		packetStatusDTO.setPacketId("123456");
		packetStatusDTO.setPacketClientStatus("Approved");
		
		Registration reg = new Registration();
		reg.setId("123456");
		reg.setPacketId("123456");
		reg.setClientStatusCode("Approved");
		reg.setUpdBy("mosip");
		reg.setApproverRoleCode("SUPERADMIN");
		reg.setAckFilename("file1");
		
		when(registrationRepository.getOne(Mockito.anyString())).thenReturn(regobjectrequest);
		when(registrationRepository.update(Mockito.any())).thenReturn(reg);

		assertSame(reg.getClientStatusCode(), registrationDAOImpl.updatePacketSyncStatus(packetStatusDTO).getClientStatusCode());
	}

	@Test
	public void getPacketsToBeSynchedTest() {
		List<Registration> registration = new LinkedList<>();
		List<String> statusCodes = new LinkedList<>();

		when(registrationRepository.findByClientStatusCodeInOrderByUpdDtimesDesc(statusCodes)).thenReturn(registration);

		registrationDAOImpl.getPacketsToBeSynched(statusCodes);
	}

	@Test
	public void getRegistrationsTest() {
		List<String> ids = new LinkedList<>();
		ids.add("REG123456");
		List<Registration> registrations = new LinkedList<>();

		Registration registration = new Registration();
		registrations.add(registration);

		Mockito.when(registrationRepository.findByPacketIdIn(ids)).thenReturn(registrations);
		assertSame(registrations, registrationDAOImpl.get(ids));
	}
	
	@Test
	public void getTest() {
		List<Registration> registrations =new ArrayList<>();
		Registration registration1 = new Registration();
		registration1.setId("12345");
		registration1.setClientStatusCode("Approved");
		registrations.add(registration1);
		Registration registration2 = new Registration();
		registration2.setId("23456");
		registration2.setClientStatusCode("Approved");
		registrations.add(registration2);
		
		List<String> status = Arrays.asList("PROCESSED", "ACCEPTED");

		Mockito.when(registrationRepository.findByCrDtimeBeforeAndServerStatusCodeIn(Mockito.any(), Mockito.any())).thenReturn(registrations);
		assertSame("Approved",registrationDAOImpl.get(timestamp, status).get(0).getClientStatusCode());
	}
	
	@Test
	public void findByServerStatusCodeInTest() {
		List<Registration> registrations =new ArrayList<>();
		Registration registration1 = new Registration();
		registration1.setId("12345");
		registration1.setClientStatusCode("Approved");
		registration1.setServerStatusCode("S");
		registrations.add(registration1);
		Registration registration2 = new Registration();
		registration2.setId("23456");
		registration2.setClientStatusCode("Approved");
		registration2.setServerStatusCode("F");
		registrations.add(registration2);
		
		List<String> codes = new ArrayList<>();
		codes.add("S");
		codes.add("F");

		Mockito.when(registrationRepository.findByServerStatusCodeIn(codes)).thenReturn(registrations);
		assertSame("Approved",registrationDAOImpl.findByServerStatusCodeIn(codes).get(0).getClientStatusCode());
		assertSame( "Approved",registrationDAOImpl.findByServerStatusCodeIn(codes).get(1).getClientStatusCode());

	}
	
	@Test
	public void findByServerStatusCodeNotInTest() {
		List<Registration> registrations =new ArrayList<>();
		Registration registration1 = new Registration();
		registration1.setId("12345");
		registration1.setClientStatusCode("Approved");
		registrations.add(registration1);
		Registration registration2 = new Registration();
		registration2.setId("23456");
		registration2.setClientStatusCode("Approved");
		registrations.add(registration2);
		
		List<String> codes = new ArrayList<>();
		codes.add("S");
		codes.add("F");

		Mockito.when(registrationRepository.findByServerStatusCodeNotInOrServerStatusCodeIsNull(codes)).thenReturn(registrations);
		assertSame("Approved",registrationDAOImpl.findByServerStatusCodeNotIn(codes).get(0).getClientStatusCode());
	}
	
	@Test
	public void fetchPacketsToUploadTest() {
		List<Registration> registrations =new ArrayList<>();
		Registration registration1 = new Registration();
		registration1.setId("12345");
		registration1.setClientStatusCode("Approved");
		registration1.setServerStatusCode("S");
		registrations.add(registration1);
		Registration registration2 = new Registration();
		registration2.setId("23456");
		registration2.setClientStatusCode("Rejected");
		registration2.setServerStatusCode("S");
		registrations.add(registration2);
		
		List<String> codes = new ArrayList<>();
		codes.add("Approved");
		codes.add("Rejected");

		Mockito.when(registrationRepository.findByClientStatusCodeInOrServerStatusCodeOrderByUpdDtimesDesc(codes,"S")).thenReturn(registrations);
		assertSame("Approved",registrationDAOImpl.fetchPacketsToUpload(codes, "S").get(0).getClientStatusCode());
		assertSame("Rejected",registrationDAOImpl.fetchPacketsToUpload(codes, "S").get(1).getClientStatusCode());
	}

	@Test
	public void saveRegistration() throws RegBaseCheckedException {
		RegistrationDTO registrationDTO = getRegistrationDTO();
		RegistrationMetaDataDTO registrationMetaDataDTO=new RegistrationMetaDataDTO();		
		List<ValuesDTO> fullNames = new ArrayList<>();
		ValuesDTO valuesDTO = new ValuesDTO();
		valuesDTO.setLanguage("eng");
		valuesDTO.setValue("Individual Name");
		fullNames.add(valuesDTO);		
		registrationDTO.getDemographics().put("fullName", fullNames);		
		registrationDTO.setRegistrationMetaDataDTO(registrationMetaDataDTO);
		//registrationDTO.getRegistrationMetaDataDTO().setRegistrationCategory("New");
		when(identitySchemaServiceImpl.getAllFieldSpec(Mockito.anyString(),Mockito.anyDouble())).thenReturn(getDocumentTypeTestData());
		when(registrationRepository.create(Mockito.any(Registration.class))).thenReturn(new Registration());
		registrationDAOImpl.save("test", registrationDTO);
	}
	@Test
	public void getRegistrationByStatusWithListTest() {
		List<Registration> packetLists = getRegistrationsList();
		List<String> packetNames = Arrays.asList("PUSHED", "EXPORTED", "resend", "E");
		Mockito.when(registrationRepository.findByStatusCodes("PUSHED", "EXPORTED", "resend", "E"))
				.thenReturn(packetLists);
		assertEquals(2, registrationDAOImpl.getRegistrationByStatus(packetNames).size());
	}
	
	@Test
	public void updateRegistrationWithPacketIdTest() {
		Registration registration = new Registration();
		Mockito.when(registrationRepository.findByPacketId(Mockito.anyString())).thenReturn(registration);
		Mockito.when(registrationRepository.update(registration)).thenReturn(registration);
		assertEquals(registration, registrationDAOImpl.updateRegistrationWithPacketId("packetId","statusComments", "clientStatusCode"));
	}
	
	@Test
	public void getRegistrationByPacketIdTest() {
		Registration updatedPacket = new Registration();
		updatedPacket.setUploadCount((short)0);
		Mockito.when(registrationRepository.findByPacketId(Mockito.any())).thenReturn(updatedPacket);
		assertEquals(updatedPacket, registrationDAOImpl.getRegistrationByPacketId(Mockito.anyString()));
	}
	
	@Test
	public void getPacketsToBeSynchedWithListTest() {
		List<Registration> registrations = getRegistrationsList();
		List<String> statusCodes = new LinkedList<>();
		when(registrationRepository.findByClientStatusCodeInOrderByUpdDtimesDesc(Mockito.anyList())).thenReturn(registrations);
		assertEquals(2,registrationDAOImpl.getPacketsToBeSynched(statusCodes).size());
	}
	
	@Test
	public void getAllRegistrationsTest() {
		List<Registration> registrations = getRegistrationsList();
		when(registrationRepository.findAll()).thenReturn(registrations);
		assertEquals(2,registrationDAOImpl.getAllRegistrations().size());
	}
	
	@Test
	public void updateAckReceiptSignatureTest() {
		Registration registration = getRegistration();
		when(registrationRepository.findByPacketId(Mockito.anyString())).thenReturn(registration);
		when(registrationRepository.update(registration)).thenReturn(registration);
		assertEquals(registration,registrationDAOImpl.updateAckReceiptSignature("packetId","signature"));
	}
	
	@Test
	public void fetchReRegisterPendingPacketsTest() {
		List<Registration> registrations = getRegistrationsList();
		when(registrationRepository.findByClientStatusCodeNotInAndServerStatusCodeIn(
				Arrays.asList(RegistrationClientStatusCode.RE_REGISTER.getCode()),
				Arrays.asList(RegistrationConstants.PACKET_STATUS_CODE_REREGISTER))).thenReturn(registrations);
		assertEquals(2,registrationDAOImpl.fetchReRegisterPendingPackets().size());
	}
	
	@Test
	public void getRegistrationIdsTest() {
		List<String> regIds=getRegistrationIds();
		when(registrationRepository.getRIDByAppId(Mockito.anyString())).thenReturn("regId");
		assertEquals(2,registrationDAOImpl.getRegistrationIds(regIds).size());
	}
	
	List<Registration> getRegistrationsList(){                  		
		List<Registration> registrations =new ArrayList<>();
		Registration registration1 = new Registration();
		registration1.setId("12345");
		registration1.setClientStatusCode("Approved");
		registration1.setServerStatusCode("S");
		registrations.add(registration1);
		Registration registration2 = new Registration();
		registration2.setId("23456");
		registration2.setClientStatusCode("Rejected");
		registration2.setServerStatusCode("S");
		registrations.add(registration2);
		return registrations;
	}
	
	Registration getRegistration() {
		Registration registration1 = new Registration();
		registration1.setId("12345");
		registration1.setClientStatusCode("Approved");
		registration1.setServerStatusCode("S");
		return registration1;
	}
	List<String> getRegistrationIds(){		
		List<String> regIds = new ArrayList<>();
		regIds.add("id1");
		regIds.add("id2");
		return regIds;
	}
	
	RegistrationDTO getRegistrationDTO() {		
		RegistrationDTO registrationDTO = new RegistrationDTO();
		registrationDTO.setRegistrationId("123");
		registrationDTO.setFlowType(FlowType.NEW);
		System.out.println(registrationDTO.getFlowType().getRegistrationTypeCode());
		registrationDTO.setProcessId("1234");
		registrationDTO.setPreRegistrationId("123");
		registrationDTO.setAppId("123");
		registrationDTO.setPacketId("324");
		registrationDTO.setAdditionalInfoReqId("1234");
		return registrationDTO;
		
	}
	List<UiFieldDTO> getDocumentTypeTestData() {

		List<UiFieldDTO> fields = new ArrayList<>();
		UiFieldDTO fullname = new UiFieldDTO();
		fullname.setId("fullName");
		fullname.setType("documentType");
		fullname.setSubType(RegistrationConstants.UI_SCHEMA_SUBTYPE_FULL_NAME);
		fields.add(fullname);
		return fields;
	}
}
