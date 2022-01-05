package io.mosip.registration.test.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.GlobalParamName;
import io.mosip.registration.dao.impl.GlobalParamDAOImpl;
import io.mosip.registration.entity.GlobalParam;
import io.mosip.registration.entity.id.GlobalParamId;
import io.mosip.registration.repositories.GlobalParamRepository;

public class GlobalParamDAOTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	private GlobalParamDAOImpl globalContextParamDAOImpl;

	@Mock
	private GlobalParamRepository globalParamRepository;

	@Test
	public void getGlobalParamsTest() {
		List<GlobalParamName> params = new ArrayList<>(); 
		
		Mockito.when(globalParamRepository.findByIsActiveTrueAndValIsNotNull()).thenReturn(params);
		Map<String,Object> globalParamMap = new LinkedHashMap<>();
		assertEquals(globalParamMap, globalContextParamDAOImpl.getGlobalParams());
	}
	
	@Test
	public void saveAllTest() {
		List<GlobalParam> params = new ArrayList<>(); 
		
		Mockito.when(globalParamRepository.saveAll(Mockito.any())).thenReturn(new LinkedList<GlobalParam>());
		globalContextParamDAOImpl.saveAll(params);
	}
	@Test
	public void get() {  
		GlobalParam globalParam=new GlobalParam();
		globalParam.setName("name");
		GlobalParamId globalParamId = new GlobalParamId();
		globalParamId.setCode("code");
		Mockito.when(globalParamRepository.findById(Mockito.any(),Mockito.any())).thenReturn(globalParam);
		//globalContextParamDAOImpl.get("name");
		assertEquals(globalParam.getName(), globalContextParamDAOImpl.get(globalParamId).getName());
	}  
	
	@Test
	public void getAllTest()
	{  
		List<GlobalParam> params = new ArrayList<>(); 
		
		GlobalParam globalParam=new GlobalParam();
		globalParam.setName("1234");
		params.add(globalParam);
		
		List<String> list=new  LinkedList<>();
		list.add("1234");
		
		Mockito.when(globalParamRepository.findByNameIn(list)).thenReturn(params);
		//globalContextParamDAOImpl.get("name");
		assertEquals(params, globalContextParamDAOImpl.getAll(list));
	}  
	
	@Test
	public void updateSoftwareUpdateStatusSuccessTest() {
		
		GlobalParamId globalParamId=new GlobalParamId();
		globalParamId.setCode(RegistrationConstants.IS_SOFTWARE_UPDATE_AVAILABLE);
		globalParamId.setLangCode("eng");
		
		GlobalParam globalParam = new GlobalParam();
		globalParam.setName(RegistrationConstants.IS_SOFTWARE_UPDATE_AVAILABLE);
		globalParam.setGlobalParamId(globalParamId);
		globalParam.setVal("N");
		
		GlobalParam globalParam1 = new GlobalParam();
		globalParam.setName(RegistrationConstants.IS_SOFTWARE_UPDATE_AVAILABLE);
		globalParam.setGlobalParamId(globalParamId);
		globalParam.setVal("Y");
		
		Mockito.when(globalContextParamDAOImpl.get(globalParamId)).thenReturn(globalParam);
		Mockito.when(globalParamRepository.update(globalParam)).thenReturn(globalParam1);
		GlobalParam globalParam2 = globalContextParamDAOImpl.updateSoftwareUpdateStatus(true, Timestamp.from(Instant.now()));
		assertEquals(globalParam2.getVal(),globalParam1.getVal());
	}
	
	@Test
	public void updateSoftwareUpdateStatusFailureTest() {
		
		GlobalParamId globalParamId=new GlobalParamId();
		globalParamId.setCode(RegistrationConstants.IS_SOFTWARE_UPDATE_AVAILABLE);
		globalParamId.setLangCode("eng");
		
		GlobalParam globalParam = new GlobalParam();
		globalParam.setName(RegistrationConstants.IS_SOFTWARE_UPDATE_AVAILABLE);
		globalParam.setGlobalParamId(globalParamId);
		globalParam.setVal("Y");
		
		GlobalParam globalParam1 = new GlobalParam();
		globalParam.setName(RegistrationConstants.IS_SOFTWARE_UPDATE_AVAILABLE);
		globalParam.setGlobalParamId(globalParamId);
		globalParam.setVal("N");
		
		Mockito.when(globalContextParamDAOImpl.get(globalParamId)).thenReturn(globalParam);
		Mockito.when(globalParamRepository.update(globalParam)).thenReturn(globalParam1);
		GlobalParam globalParam2 = globalContextParamDAOImpl.updateSoftwareUpdateStatus(false, Timestamp.from(Instant.now()));
		assertEquals(globalParam2.getVal(),globalParam1.getVal());
	}
	
	@Test
	public void updatetest() {
		GlobalParam globalParam=new GlobalParam();
		GlobalParamId globalParamId=new GlobalParamId();
		globalParamId.setCode(RegistrationConstants.INITIAL_SETUP);
		globalParamId.setLangCode("en");
		globalParam.setGlobalParamId(globalParamId);
		Mockito.when(globalParamRepository.update(globalParam)).thenReturn(globalParam);
		
		assertEquals(globalContextParamDAOImpl.update(globalParam),globalParam);
	}
	@Test
	public void upsertServerProfileTest() {
		GlobalParam gParam = null;
		GlobalParamId globalParamId = new GlobalParamId();
		globalParamId.setCode("code");
		GlobalParam globalParam1 = new GlobalParam();
		globalParam1.setName(RegistrationConstants.SERVER_ACTIVE_PROFILE);
		Mockito.when(globalParamRepository.findById(Mockito.any(),Mockito.any())).thenReturn(gParam);
		Mockito.when(globalParamRepository.save(Mockito.any())).thenReturn(globalParam1);
		assertNotNull(globalContextParamDAOImpl.upsertServerProfile(RegistrationConstants.SERVER_ACTIVE_PROFILE));		
	}
	
	@Test
	public void upsertServerProfileGParamNotNullTest() {
		GlobalParamId globalParamId = new GlobalParamId();
		globalParamId.setCode("code");
		GlobalParam globalParam1 = new GlobalParam();
		globalParam1.setName(RegistrationConstants.SERVER_ACTIVE_PROFILE);
		Mockito.when(globalParamRepository.findById(Mockito.any(),Mockito.any())).thenReturn(globalParam1);
		Mockito.when(globalParamRepository.update(globalParam1)).thenReturn(globalParam1);
		assertNotNull(globalContextParamDAOImpl.upsertServerProfile(RegistrationConstants.SERVER_ACTIVE_PROFILE));		
	}
	
	@Test
	@Ignore
	public void upsertServerProfileNotNullTest() {
		GlobalParamId globalParamId = new GlobalParamId();
		globalParamId.setCode("code");
		GlobalParam globalParam1 = new GlobalParam();
		globalParam1.setName(RegistrationConstants.SERVER_ACTIVE_PROFILE);
		Mockito.when(globalContextParamDAOImpl.get(globalParamId)).thenReturn(globalParam1);
		Mockito.when(globalParamRepository.save(globalParam1)).thenReturn(globalParam1);
		globalParam1=globalContextParamDAOImpl.upsertServerProfile(RegistrationConstants.SERVER_ACTIVE_PROFILE);		
	}
	
	@Test
	public void updateSoftwareUpdateStatusDisableTest() {	
		GlobalParam gParam = new GlobalParam();
		gParam.setVal(RegistrationConstants.DISABLE);
		Calendar cal = Calendar.getInstance();
		cal.setTime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		Timestamp req = new Timestamp(cal.getTimeInMillis());
		GlobalParamId globalParamId = new GlobalParamId();
		globalParamId.setCode("code");
		Mockito.when(globalParamRepository.findById(Mockito.any(),Mockito.any())).thenReturn(gParam);
		Mockito.when(globalParamRepository.update(gParam)).thenReturn(gParam);
		Mockito.when(globalContextParamDAOImpl.get(globalParamId)).thenReturn(gParam);		
		assertNotNull(globalContextParamDAOImpl.updateSoftwareUpdateStatus(Boolean.TRUE,req));
	}
	
	@Test
	public void updateSoftwareIsUpdateAvaileTrueTest() {	
		GlobalParam gParam = new GlobalParam();
		gParam.setVal(RegistrationConstants.DISABLE);
		Calendar cal = Calendar.getInstance();
		cal.setTime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		Timestamp req = new Timestamp(cal.getTimeInMillis());
		GlobalParamId globalParamId = new GlobalParamId();
		globalParamId.setCode("code");
		Mockito.when(globalParamRepository.findById(Mockito.any(),Mockito.any())).thenReturn(gParam);
		Mockito.when(globalParamRepository.update(gParam)).thenReturn(gParam);
		Mockito.when(globalContextParamDAOImpl.get(globalParamId)).thenReturn(gParam);		
		assertNotNull(globalContextParamDAOImpl.updateSoftwareUpdateStatus(Boolean.TRUE,req));
	}
	
	@Test
	public void updateSoftwareIsUpdateAvaileFalseTest() {	
		GlobalParam gParam = new GlobalParam();
		gParam.setVal(RegistrationConstants.DISABLE);
		Calendar cal = Calendar.getInstance();
		cal.setTime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		Timestamp req = new Timestamp(cal.getTimeInMillis());
		GlobalParamId globalParamId = new GlobalParamId();
		globalParamId.setCode("code");
		Mockito.when(globalParamRepository.findById(Mockito.any(),Mockito.any())).thenReturn(gParam);
		Mockito.when(globalParamRepository.update(gParam)).thenReturn(gParam);
		Mockito.when(globalContextParamDAOImpl.get(globalParamId)).thenReturn(gParam);		
		assertNotNull(globalContextParamDAOImpl.updateSoftwareUpdateStatus(Boolean.FALSE,req));
	}
	
	@Test
	public void updateSoftwareUpdateStatusEnableTest() {	
		GlobalParam gParam = new GlobalParam();
		gParam.setVal(RegistrationConstants.ENABLE);
		Calendar cal = Calendar.getInstance();
		cal.setTime(Timestamp.valueOf(DateUtils.getUTCCurrentDateTime()));
		Timestamp req = new Timestamp(cal.getTimeInMillis());
		GlobalParamId globalParamId = new GlobalParamId();
		globalParamId.setCode("code");
		Mockito.when(globalParamRepository.findById(Mockito.any(),Mockito.any())).thenReturn(gParam);
		Mockito.when(globalParamRepository.update(gParam)).thenReturn(gParam);
		Mockito.when(globalContextParamDAOImpl.get(globalParamId)).thenReturn(gParam);		
		assertNotNull(globalContextParamDAOImpl.updateSoftwareUpdateStatus(Boolean.TRUE,req));
	}
	
	/**
	 * return the GlobalParam list
	 * @return
	 */
	private List<GlobalParam> getGlobalParamIterableList() {
		List<GlobalParam> globalParamIterableList = new ArrayList<GlobalParam>();
		GlobalParamId globalParamId = new GlobalParamId();
		globalParamId.setCode("code");
		GlobalParam gparam = new GlobalParam();
		gparam.setGlobalParamId(globalParamId);		
		globalParamIterableList.add(gparam);		
		return globalParamIterableList;
	}

}
