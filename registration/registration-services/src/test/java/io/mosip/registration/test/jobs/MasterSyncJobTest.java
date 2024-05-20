package io.mosip.registration.test.jobs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dao.SyncJobConfigDAO;
import io.mosip.registration.dto.AuthenticationValidatorDTO;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.entity.SyncJobDef;
import io.mosip.registration.exception.ConnectionException;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.jobs.BaseJob;
import io.mosip.registration.jobs.JobManager;
import io.mosip.registration.jobs.SyncManager;
import io.mosip.registration.jobs.impl.MasterSyncJob;
import io.mosip.registration.jobs.impl.UserDetailServiceJob;
import io.mosip.registration.service.config.impl.JobConfigurationServiceImpl;
import io.mosip.registration.service.packet.RegPacketStatusService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.service.sync.impl.MasterSyncServiceImpl;


/**
 * @author Sreekar Chukka
 *
 * @since 1.0.0
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ JobConfigurationServiceImpl.class })
public class MasterSyncJobTest {

	@Mock
	private ApplicationContext applicationContext;

	@Mock
	SyncManager syncManager;

	@Mock
	private SyncJobConfigDAO jobConfigDAO;

	@Mock
	JobManager jobManager;

	@Mock
	JobExecutionContext context;

	@Mock
	JobDetail jobDetail;

	@Mock
	JobDataMap jobDataMap;

	@InjectMocks
	MasterSyncJob masterSyncJob;

	@Mock
	BaseJob baseJob;

	@Mock
	MasterSyncServiceImpl masterSyncService;
	
	@Mock
	UserDetailServiceJob userDetailServiceJob;

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	private LinkedList<SyncJobDef> syncJobList;
	HashMap<String, SyncJobDef> jobMap = new HashMap<>();

	@Before
	public void intiate() {
		syncJobList = new LinkedList<>();
		SyncJobDef syncJob = new SyncJobDef();
		syncJob.setId("1234");

		syncJob.setApiName("masterSyncJob");
		syncJob.setSyncFreq("0/5 * * * * ?");
		syncJobList.add(syncJob);

		syncJobList.forEach(job -> {
			jobMap.put(job.getId(), job);
		});
		Mockito.when(jobConfigDAO.getActiveJobs()).thenReturn(syncJobList);
		
		PowerMockito.mockStatic(JobConfigurationServiceImpl.class);

		Map<String, SyncJobDef> parentJobMap = new HashMap<>();
		parentJobMap.put("1", syncJob);
		Mockito.when(JobConfigurationServiceImpl.getParentJobMap()).thenReturn(parentJobMap);
		
	}

	@Test
	public void executeinternalTest() throws JobExecutionException, RegBaseCheckedException {

		SyncJobDef syncJob = new SyncJobDef();
		syncJob.setId("1");
		
		Map<String, SyncJobDef> jobMap=new HashMap<>();
		
		jobMap.put(syncJob.getId(), syncJob);
		
		syncJob.setId("2");
		syncJob.setParentSyncJobId("1");
		
		
		jobMap.put("2", syncJob);
		
		ResponseDTO responseDTO = new ResponseDTO();
		SuccessResponseDTO successResponseDTO=new SuccessResponseDTO();
		responseDTO.setSuccessResponseDTO(successResponseDTO);

		Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
		Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
		Mockito.when(jobDataMap.get(Mockito.any())).thenReturn(applicationContext);
		Mockito.when(applicationContext.getBean(SyncManager.class)).thenReturn(syncManager);
		Mockito.when(applicationContext.getBean(JobManager.class)).thenReturn(jobManager);
		Mockito.when(applicationContext.getBean(MasterSyncServiceImpl.class)).thenReturn(masterSyncService);
		Mockito.when(applicationContext.getBean(UserDetailServiceJob.class)).thenReturn(userDetailServiceJob);
		
//		Mockito.when(jobManager.getChildJobs(Mockito.any())).thenReturn(jobMap);
		Mockito.when(jobManager.getJobId(Mockito.any(JobExecutionContext.class))).thenReturn("2");
		
		
		Mockito.when(applicationContext.getBean(Mockito.anyString())).thenReturn(masterSyncJob);
	
		Mockito.when(applicationContext.getBean(MasterSyncService.class)).thenReturn(masterSyncService);
		
		Mockito.when(masterSyncService.getMasterSync(Mockito.anyString(),Mockito.anyString())).thenReturn(responseDTO);

		assertNotNull(responseDTO);
		masterSyncJob.executeInternal(context);
		masterSyncJob.executeJob("User", "1");

	}
	
	@Test
	public void executeinternalFailurTest() throws JobExecutionException, RegBaseCheckedException {

		SyncJobDef syncJob = new SyncJobDef();
		syncJob.setId("1");

		Map<String, SyncJobDef> jobMap=new HashMap<>();

		jobMap.put(syncJob.getId(), syncJob);

		syncJob.setId("2");
		syncJob.setParentSyncJobId("1");


		jobMap.put("2", syncJob);

		ResponseDTO responseDTO = new ResponseDTO();
		SuccessResponseDTO successResponseDTO=new SuccessResponseDTO();
		responseDTO.setSuccessResponseDTO(null);

		Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
		Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
		Mockito.when(jobDataMap.get(Mockito.any())).thenReturn(applicationContext);
		Mockito.when(applicationContext.getBean(SyncManager.class)).thenReturn(syncManager);
		Mockito.when(applicationContext.getBean(JobManager.class)).thenReturn(jobManager);
		Mockito.when(applicationContext.getBean(MasterSyncServiceImpl.class)).thenReturn(masterSyncService);

		Mockito.when(jobManager.getJobId(Mockito.any(JobExecutionContext.class))).thenReturn("1");

		Mockito.when(applicationContext.getBean(Mockito.anyString())).thenReturn(masterSyncJob);

		Mockito.when(applicationContext.getBean(MasterSyncService.class)).thenReturn(masterSyncService);

		Mockito.when(masterSyncService.getMasterSync(Mockito.anyString(),Mockito.anyString())).thenReturn(responseDTO);

		assertNotNull(responseDTO);
		masterSyncJob.executeInternal(context);
		masterSyncJob.executeJob("User", "1");
   
	}

	@Test
	public void executeinternalFailure1Test() throws JobExecutionException, RegBaseCheckedException {

		SyncJobDef syncJob = new SyncJobDef();
		syncJob.setId("1");

		Map<String, SyncJobDef> jobMap=new HashMap<>();

		jobMap.put(syncJob.getId(), syncJob);

		syncJob.setId("2");
		syncJob.setParentSyncJobId("1");


		jobMap.put("2", syncJob);

		ResponseDTO responseDTO = new ResponseDTO();
		SuccessResponseDTO successResponseDTO=new SuccessResponseDTO();
		responseDTO.setSuccessResponseDTO(successResponseDTO);

		Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
		Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
		Mockito.when(jobDataMap.get(Mockito.any())).thenReturn(applicationContext);
		Mockito.when(applicationContext.getBean(SyncManager.class)).thenReturn(syncManager);
		Mockito.when(applicationContext.getBean(JobManager.class)).thenReturn(jobManager);
		Mockito.when(applicationContext.getBean(MasterSyncServiceImpl.class)).thenReturn(masterSyncService);

		Mockito.when(jobManager.getJobId(Mockito.any(JobExecutionContext.class))).thenReturn("2");

		Mockito.when(applicationContext.getBean(Mockito.anyString())).thenReturn(masterSyncJob);

		Mockito.when(applicationContext.getBean(MasterSyncService.class)).thenReturn(masterSyncService);

		Mockito.when(masterSyncService.getMasterSync(Mockito.anyString(), Mockito.anyString())).thenThrow(RegBaseCheckedException.class);
        
		assertNotNull(responseDTO);
		masterSyncJob.executeInternal(context);
		masterSyncJob.executeJob("User", "2");
	}

	
	@Test(expected = RegBaseUncheckedException.class)
	public void executejobNoSuchBeanDefinitionExceptionTest() {
		ResponseDTO responseDTO=new ResponseDTO();
		SuccessResponseDTO successResponseDTO=new SuccessResponseDTO();
		responseDTO.setSuccessResponseDTO(successResponseDTO);
//		Mockito.when(applicationContext.getBean(SyncManager.class)).thenThrow(NoSuchBeanDefinitionException.class);
//				preRegistrationDataSyncJob.executeJob("User");
//				
		Mockito.when(context.getJobDetail()).thenThrow(NoSuchBeanDefinitionException.class);
		masterSyncJob.executeInternal(context);
	}
	
	@Test(expected = RegBaseUncheckedException.class)
	public void executejobNullPointerExceptionTest() {
		ResponseDTO responseDTO=new ResponseDTO();
		SuccessResponseDTO successResponseDTO=new SuccessResponseDTO();
		responseDTO.setSuccessResponseDTO(successResponseDTO);
		Mockito.when(context.getJobDetail()).thenThrow(NullPointerException.class);
		
		masterSyncJob.executeInternal(context);
	}
	
	@Test(expected = RegBaseUncheckedException.class)
	public void executeChildJobsTest() throws JobExecutionException {
		SyncJobDef syncJob = new SyncJobDef();
		syncJob.setId("1");
		
		Map<String, SyncJobDef> jobMap=new HashMap<>();
		
		jobMap.put(syncJob.getId(), syncJob);
		
		syncJob.setId("2");
		syncJob.setParentSyncJobId("1");
		
		
		jobMap.put("2", syncJob);
		
		ResponseDTO responseDTO = new ResponseDTO();
		SuccessResponseDTO successResponseDTO=new SuccessResponseDTO();
		responseDTO.setSuccessResponseDTO(successResponseDTO);

		Mockito.when(applicationContext.getBean(Mockito.anyString())).thenThrow(NoSuchBeanDefinitionException.class);
		
		masterSyncJob.executeParentJob("1");

	}
	
	
	  @Test(expected = RuntimeException.class) 
	  public void executejobThrowingException() throws RegBaseCheckedException { 
	  ResponseDTO responseDTO = new ResponseDTO(); 
	  ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(); 
	  List<ErrorResponseDTO> errorResponseDTOs = new ArrayList<>(); errorResponseDTOs.add(errorResponseDTO);
	  responseDTO.setErrorResponseDTOs(errorResponseDTOs); 
	  SyncJobDef syncJob = new SyncJobDef(); syncJob.setId("1");
	  
	  Map<String, SyncJobDef> jobMap = new HashMap<>();
	  
	  jobMap.put(syncJob.getId(), syncJob);
	  
	  syncJob.setId("2"); syncJob.setParentSyncJobId("1");
	  
	  jobMap.put("2", syncJob);
	  
	  Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
	  Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
	  Mockito.when(jobDataMap.get(Mockito.any())).thenReturn(applicationContext);
	  Mockito.when(applicationContext.getBean(SyncManager.class)).thenReturn(syncManager);
	  Mockito.when(applicationContext.getBean(JobManager.class)).thenReturn(jobManager);
	  Mockito.when(applicationContext.getBean(MasterSyncService.class)).thenReturn(masterSyncService);
	  
	  Mockito.when(jobManager.getJobId(Mockito.any(JobExecutionContext.class))).
	  thenReturn("1");
	  
	  Mockito.when(applicationContext.getBean(Mockito.anyString())).thenReturn(masterSyncJob);
	  
	  Mockito.when(masterSyncService.getMasterSync("User", "1")).thenThrow(RuntimeException.class);
	  
	  masterSyncJob.executeInternal(context);
	  
	  }
	 

	
	/*
	 * @Test public void Test() { SyncControl masterSyncDetails =
	 * masterSyncDao.syncJobDetails(masterSyncDtls);
	 * 
	 * Mockito.when(masterSyncService.getMasterSync("User", "1")).thenReturn(null);
	 * String errorCode = null; try {
	 * masterSyncService.executeInternal(masterSyncDetails); } catch
	 * (RegBaseCheckedException e) { errorCode = e.getErrorCode(); }
	 * assertEquals(LoggerConstants.USER_DETAIL_SERVICE_JOB_TITLE, errorCode);
	 * 
	 * }
	 */

}
