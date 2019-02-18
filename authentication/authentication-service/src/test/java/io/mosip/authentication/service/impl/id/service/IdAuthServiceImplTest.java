package io.mosip.authentication.service.impl.id.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.UndeclaredThrowableException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.authentication.core.constant.IdAuthenticationErrorConstants;
import io.mosip.authentication.core.constant.RequestType;
import io.mosip.authentication.core.dto.indauth.IdType;
import io.mosip.authentication.core.dto.otpgen.OtpRequestDTO;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.spi.id.service.IdAuthService;
import io.mosip.authentication.core.spi.id.service.IdRepoService;
import io.mosip.authentication.service.entity.AutnTxn;
import io.mosip.authentication.service.factory.AuditRequestFactory;
import io.mosip.authentication.service.factory.RestRequestFactory;
import io.mosip.authentication.service.helper.RestHelper;
import io.mosip.authentication.service.impl.id.service.impl.IdAuthServiceImpl;
import io.mosip.authentication.service.repository.AutnTxnRepository;
import io.mosip.authentication.service.repository.VIDRepository;

/**
 * IdAuthServiceImplTest test class.
 *
 * @author Rakesh Roshan
 */
@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class IdAuthServiceImplTest {

	@Mock
	private IdRepoService idRepoService;
	@Mock
	private AuditRequestFactory auditFactory;
	@Mock
	private RestRequestFactory restFactory;
	@Mock
	private RestHelper restHelper;
	@Mock
	private VIDRepository vidRepository;

	@InjectMocks
	IdAuthServiceImpl idAuthServiceImpl;

	@Mock
	IdAuthServiceImpl idAuthServiceImplMock;

	@Mock
	IdAuthService idAuthService;
	
	@Mock
	AutnTxnRepository autntxnrepository;
	@Mock
	AutnTxn autnTxn;

	@Autowired
	Environment env;

	@Before
	public void before() {
		ReflectionTestUtils.setField(idAuthServiceImpl, "idRepoService", idRepoService);
		ReflectionTestUtils.setField(idAuthServiceImpl, "auditFactory", auditFactory);
		ReflectionTestUtils.setField(idAuthServiceImpl, "restFactory", restFactory);
		ReflectionTestUtils.setField(idAuthServiceImpl, "vidRepository", vidRepository);
		ReflectionTestUtils.setField(idAuthServiceImpl, "env", env);

		/*
		 * ReflectionTestUtils.setField(idAuthServiceImplMock, "idRepoService",
		 * idRepoService); ReflectionTestUtils.setField(idAuthServiceImplMock,
		 * "auditFactory", auditFactory);
		 * ReflectionTestUtils.setField(idAuthServiceImplMock, "restFactory",
		 * restFactory); ReflectionTestUtils.setField(idAuthServiceImplMock,
		 * "uinRepository", uinRepository);
		 * ReflectionTestUtils.setField(idAuthServiceImplMock, "vidRepository",
		 * vidRepository);testProcessIdType_IdTypeIsD
		 */
	}

	@Ignore
	@Test
	public void testGetIdRepoByUinNumber() throws IdAuthenticationBusinessException {

		ReflectionTestUtils.invokeMethod(idAuthServiceImpl, "getIdRepoByUinNumber", Mockito.anyString(),
				Mockito.anyBoolean());

	}

	@Test
	public void testAuditData() {
		ReflectionTestUtils.invokeMethod(idAuthServiceImpl, "auditData");
	}

	@Test(expected=IdAuthenticationBusinessException.class)
	public void testGetIdRepoByVidNumberVIDExpired() throws Throwable {
		try {
			ReflectionTestUtils.invokeMethod(idAuthServiceImpl, "getIdRepoByVID", "232343234", false);
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	@Ignore
	@Test
	public void testGetIdRepoByVidAsRequest_IsNotNull() throws IdAuthenticationBusinessException {
		Map<String, Object> idRepo = new HashMap<>();
		idRepo.put("uin", "476567");
		Mockito.when(idRepoService.getIdenity(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(idRepo);
		Object invokeMethod = ReflectionTestUtils.invokeMethod(idAuthServiceImpl, "getIdRepoByVidAsRequest",
				Mockito.anyString());
		assertNotNull(invokeMethod);
	}

	@Test
	public void testProcessIdType_IdTypeIsD() throws IdAuthenticationBusinessException {
		String idvIdType = "D";
		String idvId = "875948796";
		Map<String, Object> idRepo = new HashMap<>();
		idRepo.put("uin", "476567");

		ReflectionTestUtils.invokeMethod(idAuthServiceImpl, "processIdType", idvIdType, idvId, false);
	}

	@Test
	public void testProcessIdType_IdTypeIsV() throws IdAuthenticationBusinessException {
		String idvIdType = "V";
		String idvId = "875948796";
		Map<String, Object> idRepo = new HashMap<>();
		idRepo.put("uin", "476567");
		Optional<String> optUIN=Optional.of("47657");
		Mockito.when(vidRepository.findUinByVid(Mockito.any(), Mockito.any())).thenReturn(optUIN);
		Mockito.when(idRepoService.getIdenity(Mockito.any(),Mockito.anyBoolean())).thenReturn(idRepo);
		Map<String,Object> idResponseMap=	(Map<String,Object>)ReflectionTestUtils.invokeMethod(idAuthServiceImpl, "processIdType", idvIdType, idvId, false);
		assertEquals("476567", idResponseMap.get("uin"));
	}

	@Ignore
	@Test(expected = IdAuthenticationBusinessException.class)
	public void processIdtypeVIDFailed() throws IdAuthenticationBusinessException {
		String idvIdType = "V";
		String idvId = "875948796";

		IdAuthenticationBusinessException idBusinessException = new IdAuthenticationBusinessException(
				IdAuthenticationErrorConstants.INVALID_VID);

		Mockito.when(idRepoService.getIdenity(Mockito.anyString(), Mockito.anyBoolean()))
				.thenThrow(idBusinessException);

		Mockito.when(idAuthService.getIdRepoByVID(Mockito.anyString(), Mockito.anyBoolean()))
				.thenThrow(idBusinessException);
		Mockito.when(idAuthServiceImpl.processIdType(idvIdType, idvId, false)).thenThrow(idBusinessException);

	}

	@Test(expected = IdAuthenticationBusinessException.class)
	public void processIdtypeUINFailed() throws IdAuthenticationBusinessException {
		String idvIdType = "D";
		String idvId = "875948796";

		IdAuthenticationBusinessException idBusinessException = new IdAuthenticationBusinessException(
				IdAuthenticationErrorConstants.INVALID_UIN);

		Mockito.when(idRepoService.getIdenity(Mockito.anyString(), Mockito.anyBoolean()))
				.thenThrow(idBusinessException);

		Mockito.when(idAuthService.getIdRepoByVID(Mockito.anyString(), Mockito.anyBoolean()))
				.thenThrow(idBusinessException);
		Mockito.when(idAuthServiceImpl.processIdType(idvIdType, idvId, false)).thenThrow(idBusinessException);

	}

	@Test
	public void testSaveAutnTxn() {
		OtpRequestDTO otpRequestDto = getOtpRequestDTO();
		String idvId = otpRequestDto.getIdvId();
		String idvIdType = otpRequestDto.getIdvIdType();
		String reqTime = otpRequestDto.getReqTime();
		String txnId = otpRequestDto.getTxnID();

		RequestType requestType = RequestType.OTP_AUTH;

		String uin = "8765";
		String status = "Y";
		String comment = "OTP_GENERATED";
		ReflectionTestUtils.invokeMethod(autntxnrepository, "saveAndFlush", autnTxn);
		ReflectionTestUtils.invokeMethod(idAuthServiceImpl, "saveAutnTxn", autnTxn);
	}

	// =========================================================
	// ************ Helping Method *****************************
	// =========================================================
	private OtpRequestDTO getOtpRequestDTO() {
		OtpRequestDTO otpRequestDto = new OtpRequestDTO();
		otpRequestDto.setId("id");
		otpRequestDto.setTspID("2345678901234");
		otpRequestDto.setIdvIdType(IdType.UIN.getType());
		otpRequestDto.setReqTime(new SimpleDateFormat(env.getProperty("datetime.pattern")).format(new Date()));
		otpRequestDto.setTxnID("2345678901234");
		otpRequestDto.setIdvId("2345678901234");
		// otpRequestDto.setVer("1.0");

		return otpRequestDto;
	}
}
