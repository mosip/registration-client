package io.mosip.registration.metrics;

import io.mosip.registration.entity.GlobalParam;
import io.mosip.registration.entity.id.GlobalParamId;
import io.mosip.registration.repositories.GlobalParamRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.Mockito;

import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class TusURLGlobalParamStoreTest {

	@MockBean
	GlobalParamRepository mockRepository;

	@Mock
	private GlobalParamRepository globalParamRepository;

	@Mock
	private ApplicationContext applicationContext;

	@Mock
	TusURLGlobalParamStore tusURLGlobalParamStore;

	@Before
	public void init() {
		Mockito.when(applicationContext.getBean(GlobalParamRepository.class)).thenReturn(globalParamRepository);
	}

	@Test
	public void testParamStore() throws MalformedURLException {
		TusURLGlobalParamStore tusURLGlobalParamStore = new TusURLGlobalParamStore(applicationContext);
		tusURLGlobalParamStore.set("test-metrics-file", new URL("http://tus-server/test-metrics-file/part"));

		GlobalParam globalParam = new GlobalParam();
		globalParam.setName("test-metrics-file");
		globalParam.setVal("http://tus-server/test-metrics-file/part");
		Mockito.when(globalParamRepository.getOne(Mockito.any())).thenReturn(globalParam);

		tusURLGlobalParamStore.set("test-metrics-file", new URL("http://tus-server/test-metrics-file/part"));

		URL url = tusURLGlobalParamStore.get("test-metrics-file");
		Assert.assertNotNull(url);
		Assert.assertEquals(url.toString(), "http://tus-server/test-metrics-file/part");

	}

	@Test
	public void testRemoveExistingGlobalParam() throws MalformedURLException {
		TusURLGlobalParamStore tusURLGlobalParamStore = new TusURLGlobalParamStore(applicationContext);
		String code = "testCode";
		URL url = new URL("http://www.example.com");
		GlobalParamId globalParamId = new GlobalParamId();
		globalParamId.setCode(code);
		globalParamId.setLangCode("eng");
		GlobalParam globalParam = new GlobalParam();
		globalParam.setGlobalParamId(globalParamId);
		globalParam.setName(code);
		globalParam.setVal(url.toString());
		globalParam.setTyp("INTERNAL");
		globalParam.setCrBy("SYSTEM");
		globalParam.setCrDtime(Timestamp.valueOf(LocalDateTime.now()));
		when(globalParamRepository.getOne(globalParamId)).thenReturn(globalParam);
		tusURLGlobalParamStore.remove(code);
		verify(globalParamRepository, times(1)).deleteById(globalParamId);
	}

	
}