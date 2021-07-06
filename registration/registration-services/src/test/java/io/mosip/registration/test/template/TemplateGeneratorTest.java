package io.mosip.registration.test.template;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.*;

import javax.imageio.ImageIO;

import io.mosip.registration.service.impl.IdentitySchemaServiceImpl;
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

import io.mosip.kernel.core.qrcodegenerator.spi.QrCodeGenerator;
import io.mosip.kernel.qrcode.generator.zxing.constant.QrVersion;
import io.mosip.kernel.templatemanager.velocity.builder.TemplateManagerBuilderImpl;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.context.SessionContext.UserContext;
import io.mosip.registration.dto.RegistrationCenterDetailDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.biometric.FingerprintDetailsDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.test.util.datastub.DataProvider;
import io.mosip.registration.util.acktemplate.TemplateGenerator;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@PrepareForTest({ ImageIO.class, ApplicationContext.class, SessionContext.class })
public class TemplateGeneratorTest {
	TemplateManagerBuilderImpl template = new TemplateManagerBuilderImpl();

	@InjectMocks
	TemplateGenerator templateGenerator;

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	@Mock
	QrCodeGenerator<QrVersion> qrCodeGenerator;

	@Mock
	private IdentitySchemaServiceImpl identitySchemaServiceImpl;

	@Mock
	private ApplicationContext applicationContext;
	
	Map<String,Object> appMap = new HashMap<>();
	
	private RegistrationDTO registrationDTO;
	
	@Before
	public void initialize() throws Exception {
		registrationDTO = DataProvider.getPacketDTO();
		List<FingerprintDetailsDTO> segmentedFingerprints = new ArrayList<>();
		segmentedFingerprints.add(new FingerprintDetailsDTO());
		
		/*registrationDTO.getBiometricDTO().getApplicantBiometricDTO().getFingerprintDetailsDTO()
				.forEach(fingerPrintDTO -> {
					fingerPrintDTO.setSegmentedFingerprints(segmentedFingerprints);
				});*/
		appMap.put(RegistrationConstants.DOC_DISABLE_FLAG, "Y");
		appMap.put(RegistrationConstants.FINGERPRINT_DISABLE_FLAG, "Y");
		appMap.put(RegistrationConstants.IRIS_DISABLE_FLAG, "Y");
		appMap.put(RegistrationConstants.FACE_DISABLE_FLAG, "Y");
		appMap.put(RegistrationConstants.PRIMARY_LANGUAGE, "ara");
		appMap.put(RegistrationConstants.SECONDARY_LANGUAGE, "fra");
		//applicationContext.setApplicationMap(appMap);
		templateGenerator.setGuidelines("My GuideLines");
	//	applicationContext.loadResourceBundle();
		when(qrCodeGenerator.generateQrCode(Mockito.anyString(), Mockito.any())).thenReturn(new byte[1024]);
		BufferedImage image = null;
		PowerMockito.mockStatic(ImageIO.class);
		when(ImageIO.read(
				templateGenerator.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_EYE_IMAGE_PATH)))
						.thenReturn(image);
		when(ImageIO.read(
				templateGenerator.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_LEFT_SLAP_IMAGE_PATH)))
						.thenReturn(image);
		when(ImageIO.read(
				templateGenerator.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_RIGHT_SLAP_IMAGE_PATH)))
						.thenReturn(image);
		/*when(ImageIO.read(
				templateGenerator.getClass().getResourceAsStream(RegistrationConstants.TEMPLATE_THUMBS_IMAGE_PATH)))
						.thenReturn(image);
		*/
		UserContext userContext = Mockito.mock(SessionContext.UserContext.class);
		PowerMockito.mockStatic(SessionContext.class);
		PowerMockito.doReturn(userContext).when(SessionContext.class, "userContext");
		RegistrationCenterDetailDTO centerDetailDTO = new RegistrationCenterDetailDTO();
		centerDetailDTO.setRegistrationCenterId("mosip");
		PowerMockito.when(userContext.getRegistrationCenterDetailDTO()).thenReturn(centerDetailDTO);

		Map<String,Object> map = new LinkedHashMap<>();
		map.put(RegistrationConstants.IS_Child, false);
		map.put(RegistrationConstants.REGISTRATION_DATA, registrationDTO);
		PowerMockito.when(SessionContext.map()).thenReturn(map);

		PowerMockito.mockStatic(ApplicationContext.class);
		ApplicationContext.setApplicationMap(appMap);
		PowerMockito.doReturn(appMap).when(ApplicationContext.class, "map");
		PowerMockito.doReturn(applicationContext).when(ApplicationContext.class, "getInstance");
		Mockito.when(ApplicationContext.applicationLanguage()).thenReturn("eng");
		Mockito.when(applicationContext.getBundle(Mockito.anyString(), Mockito.anyString())).thenReturn(dummyResourceBundle);

		Mockito.when(identitySchemaServiceImpl.getUISchema(Mockito.anyDouble())).thenReturn(Collections.EMPTY_LIST);
	}
	
	ResourceBundle dummyResourceBundle = new ResourceBundle() {
		@Override
		protected Object handleGetObject(String key) {
			return "fake_translated_value";
		}

		@Override
		public Enumeration<String> getKeys() {
			return Collections.emptyEnumeration();
		}
	};

	@Test
	public void generateTemplateWithEmptyFieldsTest() throws RegBaseCheckedException {
		ResponseDTO response = templateGenerator.generateTemplate("sample text", registrationDTO, template,
				RegistrationConstants.ACKNOWLEDGEMENT_TEMPLATE, "");
		assertNotNull(response.getSuccessResponseDTO());
	}
	
	@Test
	public void generateTemplateWithDemographicFieldsTest() throws  RegBaseCheckedException {
		Mockito.when(identitySchemaServiceImpl.getUISchema(Mockito.anyDouble())).thenReturn(DataProvider.getFields());
		ResponseDTO response = templateGenerator.generateTemplate("sample text", registrationDTO, template, RegistrationConstants.TEMPLATE_PREVIEW, "");
		assertNotNull(response.getSuccessResponseDTO());
	}
}
