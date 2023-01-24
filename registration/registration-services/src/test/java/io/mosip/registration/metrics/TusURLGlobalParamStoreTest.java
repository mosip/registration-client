package io.mosip.registration.metrics;

import io.mosip.registration.entity.GlobalParam;
import io.mosip.registration.entity.id.GlobalParamId;
import io.mosip.registration.repositories.GlobalParamRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class TusURLGlobalParamStoreTest {

	@MockBean
	GlobalParamRepository mockRepository;

	@Mock
	private GlobalParamRepository globalParamRepository;

	@Mock
	private ApplicationContext applicationContext;

	@InjectMocks
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

//	  @Test public void testParamStorex() throws MalformedURLException {
//	  TusURLGlobalParamStore tusURLGlobalParamStore = new
//	  TusURLGlobalParamStore(applicationContext);
//	  tusURLGlobalParamStore.set("test-metrics-file", new
//	  URL("http://tus-server/test-metrics-file/part"));
//	  
////	  GlobalParam globalParam = new GlobalParam();
////	  globalParam.setName("test-metrics-file");
////	  globalParam.setVal("http://tus-server/test-metrics-file/part");
////	  Mockito.when(globalParamRepository.getOne(Mockito.any())).thenReturn(
////	  globalParam);
//	  
//	  tusURLGlobalParamStore.set("test-metrics-file", new
//	  URL("http://tus-server/test-metrics-file/part"));
//	 
//	  URL url = tusURLGlobalParamStore.get("test-metrics-file"); 
//	  try {
//		  
//	  tusURLGlobalParamStore.get("xyz");
//	  
//	  } catch (MalformedURLException e){
//		  e.printStackTrace();
//	  }
//	 
//	 }

	@Test
	public void removeTest() {

	//	GlobalParamId globalParamId = Mockito.mock(GlobalParamId.class);
//		GlobalParamRepository globalParamRepository = mock(GlobalParamRepository.class);
		TusURLGlobalParamStore tusURLGlobalParamStore = Mockito.mock(TusURLGlobalParamStore.class);

		GlobalParamId globalParamId = new GlobalParamId();
				
//		tusURLGlobalParamStore.getGlobalParamRepository().deleteById(globalParamId); 
// 	tusURLGlobalParamStore.getGlobalParamRepository().findById(globalParamId);

//		Assert.assertNull(globalParamId);
 
//		System.out.println(globalParamId);
		
//        when(globalParamRepository.findById(globalParamId)).thenReturn(globalParamId);

//			verify(tusURLGlobalParamStore.getGlobalParamRepository(), times(1)).deleteById(globalParamId);
		}

}