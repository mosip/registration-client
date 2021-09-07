package io.mosip.registration.test.dao.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.BeforeClass;
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

import io.mosip.kernel.auditmanager.entity.Audit;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.registration.dao.impl.AuditDAOImpl;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.RegAuditRepository;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ DateUtils.class })
public class AuditDAOTest {

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();
	@InjectMocks
	private AuditDAOImpl auditDAO;
	@Mock
	private RegAuditRepository auditRepository;
	private static List<Audit> audits;

	@BeforeClass
	public static void initialize() {
		new ArrayList<>();
		audits = new LinkedList<>();
		Audit audit = new Audit();
		audit.setUuid(UUID.randomUUID().toString());
		audit.setCreatedAt(LocalDateTime.now());
		audits.add(audit);
		audit = new Audit();
		audit.setUuid(UUID.randomUUID().toString());
		audit.setCreatedAt(LocalDateTime.now());
		audits.add(audit);
	}

	@Test
	public void testGetAudits() {
		when(auditRepository.findAllByOrderByActionTimeStampAsc())
				.thenReturn(audits);

		Assert.assertThat(auditDAO.getAudits("1234", null), is(audits));
	}

	@Test
	public void testGetAuditsByNullAuditLogToTime() {
		when(auditRepository.findAllByOrderByActionTimeStampAsc())
				.thenReturn(audits);

		Assert.assertThat(auditDAO.getAudits("1234", null), is(audits));
	}

	@Test
	public void testGetAuditsByAuditLogToTime() {
		when(auditRepository.findByActionTimeStampGreaterThanOrderByActionTimeStampAsc(Mockito.any(LocalDateTime.class)))
				.thenReturn(audits);
		PowerMockito.mockStatic(DateUtils.class);
		PowerMockito.when(DateUtils.parseToLocalDateTime(Mockito.anyString())).thenReturn(LocalDateTime.now());
		Assert.assertThat(auditDAO.getAudits("1234", "2020-12-12 12:12:12"), is(audits));
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void testGetAuditsRuntimeException() {
		when(auditRepository.findAllByOrderByActionTimeStampAsc())
				.thenThrow(new RuntimeException("SQL exception"));

		auditDAO.getAudits("1234", null);
	}

	@Test
	public void deleteAuditsTest() {
		LocalDateTime toTime = new Timestamp(System.currentTimeMillis()).toLocalDateTime();

		Mockito.doNothing().when(auditRepository).deleteAllInBatchByActionTimeStampLessThan(toTime);
		auditDAO.deleteAudits(toTime);

	}

}
