package io.mosip.registration.test.jobs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.dao.*;
import io.mosip.registration.entity.*;
import io.mosip.registration.repositories.MachineMasterRepository;
import io.mosip.registration.service.BaseService;
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
import org.quartz.Trigger;
import org.springframework.context.ApplicationContext;



import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.response.SyncDataResponseDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.jobs.impl.SyncManagerImpl;
import io.mosip.registration.repositories.SyncTransactionRepository;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({io.mosip.registration.context.ApplicationContext.class})
public class SyncManagerTest {

	@Mock
	private SyncTransactionRepository syncTranscRepository;

	@Mock
	private JobExecutionContext jobExecutionContext;

	@Mock
	private JobDetail jobDetail;

	@Mock
	private Trigger trigger;

	@Mock
	private ApplicationContext applicationContext;

	@Mock
	JobDataMap jobDataMap = new JobDataMap();

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@InjectMocks
	private SyncManagerImpl syncTransactionManagerImpl;

	@Mock
	UserOnboardDAO onboardDAO;

	SyncJobDef syncJob = new SyncJobDef();

	List<SyncJobDef> syncJobList;

	HashMap<String, SyncJobDef> jobMap = new HashMap<>();

	@Mock
	SyncTransactionDAO jobTransactionDAO;

	@Mock
	SyncJobControlDAO syncJobDAO;

	@Mock
	private MachineMappingDAO machineMappingDAO;

	@Mock
	private BaseService baseService;

	@Mock
	private RegistrationCenterDAO registrationCenterDAO;

	@Mock
	private MachineMasterRepository machineMasterRepository;

	private Map<String, Object> applicationMap = new HashMap<>();

	@Before
	public void initializeSyncJob() throws RegBaseCheckedException {
		syncJob.setId("1");
		syncJob.setName("Name");
		syncJob.setApiName("API");
		syncJob.setCrBy("Yaswanth");
		syncJob.setCrDtime(new Timestamp(System.currentTimeMillis()));
		syncJob.setDeletedDateTime(new Timestamp(System.currentTimeMillis()));
		syncJob.setIsActive(true);
		syncJob.setIsDeleted(false);
		syncJob.setLangCode("EN");
		syncJob.setLockDuration("20");
		syncJob.setParentSyncJobId("ParentSyncJobId");
		syncJob.setSyncFreq("25");
		syncJob.setUpdBy("Yaswanth");
		syncJob.setUpdDtimes(new Timestamp(System.currentTimeMillis()));

		syncJobList = new LinkedList<>();
		syncJobList.add(syncJob);

		syncJobList.forEach(job -> {
			jobMap.put(job.getId(), job);
		});

		PowerMockito.mockStatic(io.mosip.registration.context.ApplicationContext.class);
		applicationMap.put(RegistrationConstants.REG_DELETION_CONFIGURED_DAYS, "5");
		io.mosip.registration.context.ApplicationContext.setApplicationMap(applicationMap);

		Mockito.when(baseService.getCenterId()).thenReturn("10011");
		Mockito.when(baseService.getStationId()).thenReturn("11002");
		Mockito.when(baseService.isInitialSync()).thenReturn(false);
		Mockito.when(registrationCenterDAO.isMachineCenterActive()).thenReturn(true);

		MachineMaster machine = new MachineMaster();

		machine.setId("11002");
		machine.setIsActive(true);
		Mockito.when(machineMasterRepository.findByNameIgnoreCase(Mockito.anyString())).thenReturn(machine);

	}

	private SyncTransaction prepareSyncTransaction() {
		SyncTransaction syncTransaction = new SyncTransaction();

		String transactionId = Integer.toString(new Random().nextInt(10000));
		syncTransaction.setId(transactionId);

		syncTransaction.setSyncJobId(syncJob.getId());

		syncTransaction.setSyncDateTime(new Timestamp(System.currentTimeMillis()));
		syncTransaction.setStatusCode("Completed");
		syncTransaction.setStatusComment("Completed");

		// TODO
		syncTransaction.setTriggerPoint("User");

		syncTransaction.setSyncFrom("Machine");

		// TODO
		syncTransaction.setSyncTo("SERVER???");

		syncTransaction.setMachmId("MachID");

		// TODO
		syncTransaction.setLangCode("EN");

		syncTransaction.setCrBy(RegistrationConstants.JOB_TRIGGER_POINT_USER);

		syncTransaction.setCrDtime(new Timestamp(System.currentTimeMillis()));
		return syncTransaction;

	}

	@Test
	public void createSyncTest() throws RegBaseCheckedException {

		SyncTransaction syncTransaction = prepareSyncTransaction();
		SyncControl syncControl = null;
		Mockito.when(syncJobDAO.findBySyncJobId(Mockito.anyString())).thenReturn(syncControl);

		Mockito.when(jobTransactionDAO.save(Mockito.any(SyncTransaction.class))).thenReturn(syncTransaction);

		assertSame(syncTransaction.getSyncJobId(),
				syncTransactionManagerImpl.createSyncTransaction("Completed", "Completed", "USER", "1").getSyncJobId());

	}

	@Test
	public void createSyncControlUpdateTest() {
		SyncTransaction syncTransaction = prepareSyncTransaction();
		SyncControl syncControl = new SyncControl();
		Mockito.when(syncJobDAO.findBySyncJobId(Mockito.anyString())).thenReturn(syncControl);
		Mockito.when(syncJobDAO.update(Mockito.any(SyncControl.class))).thenReturn(syncControl);
		assertNotNull(syncTransactionManagerImpl.createSyncControlTransaction(syncTransaction,
				Timestamp.valueOf(DateUtils.getUTCCurrentDateTime())));
	}
	
	@Test
	public void updateClientSettingLastSyncTimeTest() throws ParseException {
		SyncDataResponseDto syncDataResponseDto= new SyncDataResponseDto();
		syncDataResponseDto.setLastSyncTime("2018-12-10T06:12:52.994Z");
		SyncTransaction syncTransaction = prepareSyncTransaction();
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Date date = formatter.parse(syncDataResponseDto.getLastSyncTime());
		Timestamp timestamp = new Timestamp(date.getTime());
		SyncControl syncControl = new SyncControl();
		Mockito.when(syncJobDAO.findBySyncJobId(Mockito.anyString())).thenReturn(syncControl);
		Mockito.when(syncJobDAO.update(Mockito.any(SyncControl.class))).thenReturn(syncControl);
		assertNotNull(syncTransactionManagerImpl.updateClientSettingLastSyncTime(syncTransaction, timestamp));
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void createSyncTransactionExceptionTest() {
		
		Mockito.when(jobTransactionDAO.save(Mockito.any(SyncTransaction.class))).thenThrow(RuntimeException.class);
		syncTransactionManagerImpl.createSyncTransaction("Completed", "Completed", "USER", "1");

	}

	@Test
	public void createSyncControlNullTest() {
		SyncTransaction syncTransaction = prepareSyncTransaction();

		SyncControl syncControl = null;
		Mockito.when(syncJobDAO.findBySyncJobId(Mockito.any())).thenReturn(syncControl);

		syncControl =new SyncControl();
		syncControl.setId(syncTransaction.getId());
		Mockito.when(syncJobDAO.save(Mockito.any(SyncControl.class))).thenReturn(syncControl);
		assertNotNull(syncTransactionManagerImpl.createSyncControlTransaction(syncTransaction,Timestamp.valueOf(DateUtils.getUTCCurrentDateTime())));
	}

	

}
