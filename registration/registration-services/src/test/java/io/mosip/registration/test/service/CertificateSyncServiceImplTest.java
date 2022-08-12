package io.mosip.registration.test.service;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Assert;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.partnercertservice.dto.CACertificateResponseDto;
import io.mosip.kernel.partnercertservice.service.spi.PartnerCertificateManagerService;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.CaCertificateDto;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.entity.SyncTransaction;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.jobs.SyncManager;
import io.mosip.registration.service.sync.impl.CertificateSyncServiceImpl;
import io.mosip.registration.update.SoftwareUpdateHandler;
import io.mosip.registration.util.restclient.ServiceDelegateUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ObjectMapper.class })
public class CertificateSyncServiceImplTest {
	
	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	
	@InjectMocks
	private CertificateSyncServiceImpl certificateSyncServiceImpl;
	
	@Mock
	private MasterSyncDao masterSyncDao;
	
	@Mock
	private ServiceDelegateUtil serviceDelegateUtil;
	
	@Mock
	private PartnerCertificateManagerService partnerCertificateManagerService;
	
	@Mock
	private SyncManager syncManager;
	
	@Mock
	private SoftwareUpdateHandler softwareUpdateHandler;
	
	private List<CaCertificateDto> certs = new ArrayList<>();
	
	@Before
	public void initialize() throws Exception {
		ReflectionTestUtils.setField(certificateSyncServiceImpl, "rCaptureTrustDomain", "DEVICE");
		ReflectionTestUtils.setField(certificateSyncServiceImpl, "digitalIdTrustDomain", "FTM");
		ReflectionTestUtils.setField(certificateSyncServiceImpl, "deviceInfoTrustDomain", "DEVICE");
		CaCertificateDto cert1 = new CaCertificateDto();
		CaCertificateDto cert2 = new CaCertificateDto();
		CaCertificateDto cert3 = new CaCertificateDto();
		cert1.setPartnerDomain("DEVICE");
		cert1.setCertData("Test Data 1");
		cert2.setPartnerDomain("DEVICE");
		cert2.setCertData("Test Data 2");
		cert3.setPartnerDomain("FTM");
		cert3.setCertData("Test Data 3");
		certs.add(cert1);
		certs.add(cert2);
		certs.add(cert3);
		Mockito.when(softwareUpdateHandler.getCurrentVersion()).thenReturn("1.2.0.1");
	}
	
	@Test
	public void getCACertificatesSuccessTest() throws RegBaseCheckedException, ConnectionException, IllegalArgumentException, IllegalAccessException {
		SyncControl syncControl = new SyncControl();
		syncControl.setLastSyncDtimes(Timestamp.from(Instant.now()));
		Mockito.when(masterSyncDao.syncJobDetails(Mockito.anyString())).thenReturn(syncControl);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		LinkedHashMap<String, Object> certResponse = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("certificateDTOList", "test");
		responseMap.put("lastSyncTime", "2022-01-12T10:16:34.057Z");
		certResponse.put(RegistrationConstants.RESPONSE, responseMap);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(certResponse);
		Field field = PowerMockito.field(CertificateSyncServiceImpl.class, "mapper");
		ObjectMapper mapper = PowerMockito.mock(ObjectMapper.class);
        field.set(certificateSyncServiceImpl, mapper);
		Mockito.when(mapper.convertValue(Mockito.any(), Mockito.any(TypeReference.class))).thenReturn(certs);
		CACertificateResponseDto caCertResponse = new CACertificateResponseDto();
		caCertResponse.setStatus("SUCCESS");
		Mockito.when(partnerCertificateManagerService.uploadCACertificate(Mockito.any())).thenReturn(caCertResponse);
		Mockito.when(syncManager.createSyncTransaction(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(new SyncTransaction());
		Mockito.when(syncManager.updateClientSettingLastSyncTime(Mockito.any(), Mockito.any())).thenReturn(new SyncControl());
		Assert.assertNotNull(certificateSyncServiceImpl.getCACertificates("test").getSuccessResponseDTO());
	}
	
	@Test
	public void getCACertificatesInternetFailureTest() {
		SyncControl syncControl = new SyncControl();
		syncControl.setLastSyncDtimes(Timestamp.from(Instant.now()));
		Mockito.when(masterSyncDao.syncJobDetails(Mockito.anyString())).thenReturn(syncControl);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(false);
		Assert.assertNotNull(certificateSyncServiceImpl.getCACertificates("test").getErrorResponseDTOs());
	}
	
	@Test
	public void getCACertificatesSuccessTest2() throws RegBaseCheckedException, ConnectionException, IllegalArgumentException, IllegalAccessException {
		SyncControl syncControl = new SyncControl();
		syncControl.setLastSyncDtimes(Timestamp.from(Instant.now()));
		Mockito.when(masterSyncDao.syncJobDetails(Mockito.anyString())).thenReturn(syncControl);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		LinkedHashMap<String, Object> certResponse = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("certificateDTOList", "test");
		certResponse.put(RegistrationConstants.RESPONSE, responseMap);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(certResponse);
		Field field = PowerMockito.field(CertificateSyncServiceImpl.class, "mapper");
		ObjectMapper mapper = PowerMockito.mock(ObjectMapper.class);
        field.set(certificateSyncServiceImpl, mapper);
		Mockito.when(mapper.convertValue(Mockito.any(), Mockito.any(TypeReference.class))).thenReturn(certs);
		CACertificateResponseDto caCertResponse = new CACertificateResponseDto();
		caCertResponse.setStatus("SUCCESS");
		Mockito.when(partnerCertificateManagerService.uploadCACertificate(Mockito.any())).thenReturn(caCertResponse);
		Assert.assertNotNull(certificateSyncServiceImpl.getCACertificates("test").getSuccessResponseDTO());
	}
	
	@Test
	public void getCACertificatesErrorTest() throws RegBaseCheckedException, ConnectionException {
		SyncControl syncControl = new SyncControl();
		syncControl.setLastSyncDtimes(Timestamp.from(Instant.now()));
		Mockito.when(masterSyncDao.syncJobDetails(Mockito.anyString())).thenReturn(syncControl);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		LinkedHashMap<String, Object> certResponse = new LinkedHashMap<>();
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(certResponse);
		Assert.assertNotNull(certificateSyncServiceImpl.getCACertificates("test").getErrorResponseDTOs());
	}
	
	@Test
	public void getCACertificatesErrorTest2() throws RegBaseCheckedException, ConnectionException, IllegalArgumentException, IllegalAccessException {
		SyncControl syncControl = new SyncControl();
		syncControl.setLastSyncDtimes(Timestamp.from(Instant.now()));
		Mockito.when(masterSyncDao.syncJobDetails(Mockito.anyString())).thenReturn(syncControl);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		LinkedHashMap<String, Object> certResponse = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("certificateDTOList", "test");
		certResponse.put(RegistrationConstants.RESPONSE, responseMap);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(certResponse);
		Field field = PowerMockito.field(CertificateSyncServiceImpl.class, "mapper");
		ObjectMapper mapper = PowerMockito.mock(ObjectMapper.class);
        field.set(certificateSyncServiceImpl, mapper);
		Mockito.when(mapper.convertValue(Mockito.any(), Mockito.any(TypeReference.class))).thenThrow(new RuntimeException());
		Assert.assertNull(certificateSyncServiceImpl.getCACertificates("test").getSuccessResponseDTO());
	}
	
	@Test
	public void getCACertificatesSuccessTest3() throws RegBaseCheckedException, ConnectionException, IllegalArgumentException, IllegalAccessException {
		SyncControl syncControl = new SyncControl();
		syncControl.setLastSyncDtimes(Timestamp.from(Instant.now()));
		Mockito.when(masterSyncDao.syncJobDetails(Mockito.anyString())).thenReturn(syncControl);
		Mockito.when(serviceDelegateUtil.isNetworkAvailable()).thenReturn(true);
		LinkedHashMap<String, Object> certResponse = new LinkedHashMap<>();
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("certificateDTOList", "test");
		responseMap.put("lastSyncTime", "2022-01-12T10:16:34.057Z");
		certResponse.put(RegistrationConstants.RESPONSE, responseMap);
		Mockito.when(serviceDelegateUtil.get(Mockito.anyString(), Mockito.anyMap(), Mockito.anyBoolean(), Mockito.anyString())).thenReturn(certResponse);
		Field field = PowerMockito.field(CertificateSyncServiceImpl.class, "mapper");
		ObjectMapper mapper = PowerMockito.mock(ObjectMapper.class);
        field.set(certificateSyncServiceImpl, mapper);
		Mockito.when(mapper.convertValue(Mockito.any(), Mockito.any(TypeReference.class))).thenReturn(certs);
		CACertificateResponseDto caCertResponse = new CACertificateResponseDto();
		caCertResponse.setStatus("SUCCESS");
		Mockito.when(partnerCertificateManagerService.uploadCACertificate(Mockito.any())).thenThrow(new RuntimeException());
		Mockito.when(syncManager.createSyncTransaction(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(new SyncTransaction());
		Mockito.when(syncManager.updateClientSettingLastSyncTime(Mockito.any(), Mockito.any())).thenReturn(new SyncControl());
		Assert.assertNotNull(certificateSyncServiceImpl.getCACertificates("test").getSuccessResponseDTO());
	}

}
