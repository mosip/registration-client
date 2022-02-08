package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
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

import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.registration.dao.impl.IdentitySchemaDaoImpl;
import io.mosip.registration.dto.schema.ProcessSpecDto;
import io.mosip.registration.dto.schema.SchemaDto;
import io.mosip.registration.entity.IdentitySchema;
import io.mosip.registration.entity.ProcessSpec;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.repositories.IdentitySchemaRepository;
import io.mosip.registration.repositories.ProcessSpecRepository;
import io.mosip.registration.util.mastersync.MapperUtils;

/**
 * @author anusha
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ FileUtils.class, MapperUtils.class, CryptoUtil.class })
public class IdentitySchemaDaoTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private IdentitySchemaDaoImpl identitySchemaDaoImpl;

	@Mock
	private IdentitySchemaRepository identitySchemaRepository;
	
	@Mock
	private ProcessSpecRepository processSpecRepository;
	
	@Before
	public void initialize() throws IOException {
		PowerMockito.mockStatic(FileUtils.class, CryptoUtil.class, MapperUtils.class);
		Mockito.when(FileUtils.readFileToString(Mockito.any(File.class), Mockito.any(Charset.class))).thenReturn("test-content");
		Mockito.when(CryptoUtil.computeFingerPrint("test-content", null)).thenReturn("testhash");
		SchemaDto schemaDto = new SchemaDto();
		schemaDto.setSchemaJson("test-json");
		schemaDto.setSettings(new ArrayList<>());
		Mockito.when(MapperUtils.convertJSONStringToDto(Mockito.anyString(), Mockito.any())).thenReturn(schemaDto);
		Mockito.when(CryptoUtil.computeFingerPrint("test-content", null)).thenReturn("testhash");
		Mockito.when(MapperUtils.convertObjectToJsonString(Mockito.any())).thenReturn("test-content");
	}

	@Test
	public void testGetLatestEffectiveSchemaVersion() throws RegBaseCheckedException {
		Mockito.when(identitySchemaRepository.findLatestEffectiveIdVersion(Mockito.any())).thenReturn(0.5);
		assertEquals(identitySchemaDaoImpl.getLatestEffectiveSchemaVersion(), 0.5);
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void testGetLatestEffectiveSchemaVersionException() throws RegBaseCheckedException {
		Mockito.when(identitySchemaRepository.findLatestEffectiveIdVersion(Mockito.any())).thenReturn(null);
		identitySchemaDaoImpl.getLatestEffectiveSchemaVersion();
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void testgetLatestEffectiveIDSchemaException() throws RegBaseCheckedException {
		Mockito.when(identitySchemaRepository.findLatestEffectiveIdentitySchema(Mockito.any())).thenReturn(null);
		identitySchemaDaoImpl.getLatestEffectiveIDSchema();
	}
	
	@Test
	public void testgetLatestEffectiveIDSchema() throws RegBaseCheckedException, IOException {
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setIdVersion(0.5);
		identitySchema.setFileHash("testhash");
		Mockito.when(identitySchemaRepository.findLatestEffectiveIdentitySchema(Mockito.any())).thenReturn(identitySchema);
		assertNotNull(identitySchemaDaoImpl.getLatestEffectiveIDSchema());
	}

	@Test(expected = RegBaseCheckedException.class)
	public void testgetLatestEffectiveIDSchemaIOException() throws RegBaseCheckedException, IOException {
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setIdVersion(0.5);
		identitySchema.setFileHash("testhash");
		Mockito.when(identitySchemaRepository.findLatestEffectiveIdentitySchema(Mockito.any())).thenReturn(identitySchema);
		Mockito.when(FileUtils.readFileToString(Mockito.any(File.class), Mockito.any(Charset.class))).thenThrow(IOException.class);
		identitySchemaDaoImpl.getLatestEffectiveIDSchema();
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void testgetLatestEffectiveIDSchemaChecksumException() throws RegBaseCheckedException, IOException {
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setIdVersion(0.5);
		identitySchema.setFileHash("testhash");
		Mockito.when(identitySchemaRepository.findLatestEffectiveIdentitySchema(Mockito.any())).thenReturn(identitySchema);
		Mockito.when(CryptoUtil.computeFingerPrint("test-content", null)).thenReturn("test-content");
		identitySchemaDaoImpl.getLatestEffectiveIDSchema();
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void testgetLatestEffectiveIDSchemaRuntimeException() throws RegBaseCheckedException, IOException {
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setIdVersion(0.5);
		identitySchema.setFileHash("testhash");
		Mockito.when(identitySchemaRepository.findLatestEffectiveIdentitySchema(Mockito.any())).thenReturn(identitySchema);
		Mockito.when(MapperUtils.convertJSONStringToDto(Mockito.anyString(), Mockito.any())).thenThrow(IOException.class);
		identitySchemaDaoImpl.getLatestEffectiveIDSchema();
	}
	
	@Test
	public void getIDSchemaTest() throws IOException, RegBaseCheckedException {
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setIdVersion(0.5);
		identitySchema.setFileHash("testhash");
		Mockito.when(identitySchemaRepository.findByIdVersionAndFileName(Mockito.anyDouble(), Mockito.anyString())).thenReturn(identitySchema);
		assertNotNull(identitySchemaDaoImpl.getIDSchema(0.5));
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void getIDSchemaExceptionTest() throws IOException, RegBaseCheckedException {
		Mockito.when(identitySchemaRepository.findByIdVersionAndFileName(Mockito.anyDouble(), Mockito.anyString())).thenReturn(null);
		identitySchemaDaoImpl.getIDSchema(0.5);
	}
	
	@Test
	public void createIdentitySchemaTest() throws IOException {
		SchemaDto schemaReponseDto = new SchemaDto();
		schemaReponseDto.setIdVersion(0.5);
		schemaReponseDto.setId("1");
		schemaReponseDto.setEffectiveFrom(LocalDateTime.now());
		Mockito.when(identitySchemaRepository.save(Mockito.any(IdentitySchema.class))).thenReturn(new IdentitySchema());
		identitySchemaDaoImpl.createIdentitySchema(schemaReponseDto);
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void getIdentitySchemaExceptionTest() throws RegBaseCheckedException {
		Mockito.when(identitySchemaRepository.findByIdVersionAndFileName(Mockito.anyDouble(), Mockito.anyString())).thenReturn(null);
		identitySchemaDaoImpl.getIdentitySchema(0.5);
	}
	
	@Test
	public void getIdentitySchemaTest() throws RegBaseCheckedException, IOException {
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setIdVersion(0.5);
		identitySchema.setFileHash("testhash");
		Mockito.when(identitySchemaRepository.findByIdVersionAndFileName(Mockito.anyDouble(), Mockito.anyString())).thenReturn(identitySchema);
		assertNotNull(identitySchemaDaoImpl.getIdentitySchema(0.5));
	}
	
	@Test
	public void getSettingsSchema() throws RegBaseCheckedException, IOException {
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setIdVersion(0.5);
		identitySchema.setFileHash("testhash");
		Mockito.when(identitySchemaRepository.findByIdVersionAndFileName(Mockito.anyDouble(), Mockito.anyString())).thenReturn(identitySchema);
		assertNotNull(identitySchemaDaoImpl.getSettingsSchema(0.5));
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void getSettingsSchemaExceptionTest() throws RegBaseCheckedException {
		Mockito.when(identitySchemaRepository.findByIdVersionAndFileName(Mockito.anyDouble(), Mockito.anyString())).thenReturn(null);
		identitySchemaDaoImpl.getSettingsSchema(0.5);
	}
	
	@Test
	public void createProcessSpecTest() throws IOException {
		Mockito.when(identitySchemaRepository.save(Mockito.any(IdentitySchema.class))).thenReturn(new IdentitySchema());
		Mockito.when(processSpecRepository.save(Mockito.any(ProcessSpec.class))).thenReturn(new ProcessSpec());
		identitySchemaDaoImpl.createProcessSpec("newProcess", 0.5, new ProcessSpecDto());
	}
	
	@Test
	public void getAllActiveProcessSpecsTest() {
		Mockito.when(processSpecRepository.findAllByIdVersionAndIsActiveTrueOrderByOrderNumAsc(Mockito.anyDouble())).thenReturn(new ArrayList<>());
		assertNotNull(identitySchemaDaoImpl.getAllActiveProcessSpecs(0.5));
	}
	
	@Test
	public void getProcessSpecTest() throws RegBaseCheckedException, IOException {
		ProcessSpec processSpec = new ProcessSpec();
		processSpec.setType("newProcess");
		Mockito.when(processSpecRepository.findByIdAndIdVersionAndIsActiveTrue(Mockito.anyString(), Mockito.anyDouble())).thenReturn(processSpec);
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setFileHash("testhash");
		Mockito.when(identitySchemaRepository.findByIdVersionAndFileName(Mockito.anyDouble(), Mockito.anyString())).thenReturn(identitySchema);
		Mockito.when(MapperUtils.convertJSONStringToDto(Mockito.anyString(), Mockito.any())).thenReturn(new ProcessSpecDto());
		assertNotNull(identitySchemaDaoImpl.getProcessSpec("newProcess", 0.5));
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void getProcessSpecNullTest() throws RegBaseCheckedException {
		Mockito.when(processSpecRepository.findByIdAndIdVersionAndIsActiveTrue(Mockito.anyString(), Mockito.anyDouble())).thenReturn(null);
		identitySchemaDaoImpl.getProcessSpec("newProcess", 0.5);
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void getProcessSpecSchemaNullTest() throws RegBaseCheckedException {
		Mockito.when(processSpecRepository.findByIdAndIdVersionAndIsActiveTrue(Mockito.anyString(), Mockito.anyDouble())).thenReturn(new ProcessSpec());
		Mockito.when(identitySchemaRepository.findByIdVersionAndFileName(Mockito.anyDouble(), Mockito.anyString())).thenReturn(null);
		identitySchemaDaoImpl.getProcessSpec("newProcess", 0.5);
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void getProcessSpecTestIOException() throws RegBaseCheckedException, IOException {
		ProcessSpec processSpec = new ProcessSpec();
		processSpec.setType("newProcess");
		Mockito.when(processSpecRepository.findByIdAndIdVersionAndIsActiveTrue(Mockito.anyString(), Mockito.anyDouble())).thenReturn(processSpec);
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setFileHash("testhash");
		Mockito.when(identitySchemaRepository.findByIdVersionAndFileName(Mockito.anyDouble(), Mockito.anyString())).thenReturn(identitySchema);
		Mockito.when(FileUtils.readFileToString(Mockito.any(File.class), Mockito.any(Charset.class))).thenThrow(IOException.class);
		identitySchemaDaoImpl.getProcessSpec("newProcess", 0.5);
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void getProcessSpecTestChecksumMismatch() throws RegBaseCheckedException, IOException {
		ProcessSpec processSpec = new ProcessSpec();
		processSpec.setType("newProcess");
		Mockito.when(processSpecRepository.findByIdAndIdVersionAndIsActiveTrue(Mockito.anyString(), Mockito.anyDouble())).thenReturn(processSpec);
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setFileHash("testhash");
		Mockito.when(identitySchemaRepository.findByIdVersionAndFileName(Mockito.anyDouble(), Mockito.anyString())).thenReturn(identitySchema);
		Mockito.when(CryptoUtil.computeFingerPrint("test-content", null)).thenReturn("test-content");
		identitySchemaDaoImpl.getProcessSpec("newProcess", 0.5);
	}
	
	@Test(expected = RegBaseCheckedException.class)
	public void getProcessSpecTestException() throws RegBaseCheckedException, IOException {
		ProcessSpec processSpec = new ProcessSpec();
		processSpec.setType("newProcess");
		Mockito.when(processSpecRepository.findByIdAndIdVersionAndIsActiveTrue(Mockito.anyString(), Mockito.anyDouble())).thenReturn(processSpec);
		IdentitySchema identitySchema = new IdentitySchema();
		identitySchema.setFileHash("testhash");
		Mockito.when(identitySchemaRepository.findByIdVersionAndFileName(Mockito.anyDouble(), Mockito.anyString())).thenReturn(identitySchema);
		Mockito.when(MapperUtils.convertJSONStringToDto(Mockito.anyString(), Mockito.any())).thenThrow(IOException.class);
		identitySchemaDaoImpl.getProcessSpec("newProcess", 0.5);
	}
}