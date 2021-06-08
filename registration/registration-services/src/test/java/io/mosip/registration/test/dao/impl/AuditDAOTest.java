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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.mosip.kernel.auditmanager.entity.Audit;
import io.mosip.registration.dao.impl.AuditDAOImpl;
import io.mosip.registration.exception.RegBaseUncheckedException;
import io.mosip.registration.repositories.RegAuditRepository;

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
		when(auditRepository.findByIdOrderByCreatedAtAsc("1234"))
				.thenReturn(audits);

		Assert.assertThat(auditDAO.getAudits("1234", null), is(audits));
	}

	@Test
	public void testGetAuditsByNullAuditLogToTime() {
		when(auditRepository.findByIdOrderByCreatedAtAsc("1234"))
				.thenReturn(audits);

		Assert.assertThat(auditDAO.getAudits("1234", null), is(audits));
	}

	@Test
	public void testGetAuditsByAuditLogToTime() {
		when(auditRepository.findByCreatedAtGreaterThanOrderByCreatedAtAsc(Mockito.any(LocalDateTime.class)))
				.thenReturn(audits);

		Assert.assertThat(auditDAO.getAudits("1234", "2020-12-12 12:12:12"), is(audits));
	}

	@Test(expected = RegBaseUncheckedException.class)
	public void testGetAuditsRuntimeException() {
		when(auditRepository.findByIdOrderByCreatedAtAsc("1234"))
				.thenThrow(new RuntimeException("SQL exception"));

		auditDAO.getAudits("1234", null);
	}

	@Test
	public void deleteAuditsTest() {
		LocalDateTime toTime = new Timestamp(System.currentTimeMillis()).toLocalDateTime();

		Mockito.doNothing().when(auditRepository).deleteAllInBatchByCreatedAtLessThan(toTime);
		auditDAO.deleteAudits(toTime);

	}

}
