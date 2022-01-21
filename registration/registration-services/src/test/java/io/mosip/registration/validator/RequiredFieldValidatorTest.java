package io.mosip.registration.validator;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.mosip.kernel.clientcrypto.service.impl.ClientCryptoFacade;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.kernel.signature.service.SignatureService;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.schema.ConditionalBioAttributes;
import io.mosip.registration.dto.schema.RequiredOnExpr;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.repositories.FileSignatureRepository;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;

/**
 * 
 * @author M1063027
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PrepareForTest({ RegistrationAppHealthCheckUtil.class, ApplicationContext.class, SessionContext.class,RequiredFieldValidator.class })
public class RequiredFieldValidatorTest {

	@Mock
	private IdentitySchemaService identitySchemaService;

	@Mock
	private FileSignatureRepository fileSignatureRepository;

	@Mock
	private KeymanagerService keymanagerService;

	@Mock
	private SignatureService signatureService;

	@Mock
	private ClientCryptoFacade clientCryptoFacade;

	@InjectMocks
	private RequiredFieldValidator requiredFieldValidator;
	
	@Mock
	io.mosip.registration.context.ApplicationContext context;	

	private static final String SCRIPT_NAME = "applicanttype.mvel";
	@Before
	public void init() throws Exception {
		Map<String,Object> appMap = new HashMap<>();
		appMap.put(RegistrationConstants.HTTP_API_READ_TIMEOUT, "30");
		appMap.put(RegistrationConstants.HTTP_API_WRITE_TIMEOUT, "30");
		appMap.put(RegistrationConstants.AGE_GROUP_CONFIG, "30");		
		PowerMockito.mockStatic(ApplicationContext.class);
		Mockito.when(ApplicationContext.map()).thenReturn(appMap);
	}

	@Test
	public void isRequiredFieldTest() {
		UiFieldDTO schemaField = getUiFieldDTO();
		RegistrationDTO registrationDTO = new RegistrationDTO();
		RequiredOnExpr requiredOnExpr = new RequiredOnExpr();
		requiredOnExpr.setEngine("engine");
		Optional<RequiredOnExpr> mockedCenter = Optional.of(requiredOnExpr);
		assertEquals(Boolean.TRUE, requiredFieldValidator.isRequiredField(schemaField, registrationDTO));
	}

	@Test
	public void isRequiredFieldFalseTest() {
		UiFieldDTO schemaField = null;
		RegistrationDTO registrationDTO = new RegistrationDTO();
		RequiredOnExpr requiredOnExpr = new RequiredOnExpr();
		requiredOnExpr.setEngine("engine");
		Optional<RequiredOnExpr> mockedCenter = Optional.of(requiredOnExpr);
		assertEquals(Boolean.FALSE, requiredFieldValidator.isRequiredField(schemaField, registrationDTO));
	}

	@Test
	public void getRequiredBioAttributesTest() {
		UiFieldDTO schemaField =  getUiFieldDTO();
		RegistrationDTO registrationDTO = new RegistrationDTO();
		registrationDTO.setProcessId("processId");
		registrationDTO.AGE_GROUPS.put("ageGroup", "ageGroup");		
		assertEquals(schemaField.getBioAttributes().size(),requiredFieldValidator.getRequiredBioAttributes(schemaField, registrationDTO).size());
	}

	@Test
	public void isFieldVisibleFalseTest() {
		UiFieldDTO schemaField = null;
		RegistrationDTO registrationDTO = new RegistrationDTO();
		registrationDTO.setProcessId("processId");
	    assertEquals(Boolean.TRUE, requiredFieldValidator.isFieldVisible(schemaField, registrationDTO));
	}

	@Test
	public void isFieldVisibleTest() {
		UiFieldDTO schemaField = getUiFieldDTO();
		RegistrationDTO registrationDTO = new RegistrationDTO();
		registrationDTO.setProcessId("processId");	
		assertEquals(Boolean.FALSE,requiredFieldValidator.isFieldVisible(schemaField, registrationDTO));
	}
	
	@Test
	public void getConditionalBioAttributesFalseTest() {
		UiFieldDTO schemaField = new UiFieldDTO();
		RegistrationDTO registrationDTO = new RegistrationDTO();		
		registrationDTO.setProcessId("processId");
		assertEquals(null, requiredFieldValidator.getConditionalBioAttributes(schemaField, registrationDTO));
	}

	@Test
	public void getConditionalBioAttributesTest() {
		UiFieldDTO schemaField =  getUiFieldDTO();
		RegistrationDTO registrationDTO = new RegistrationDTO();
		registrationDTO.setProcessId("processId");
		registrationDTO.AGE_GROUPS.put("ageGroup", "ageGroup");		
		assertNotNull(requiredFieldValidator.getConditionalBioAttributes(schemaField, registrationDTO));
	}
	@Test
	public void evaluateMvelScriptFalseTest() {
		UiFieldDTO schemaField = new UiFieldDTO();
		RegistrationDTO registrationDTO = new RegistrationDTO();
		registrationDTO.setProcessId("processId");
		assertEquals(null, requiredFieldValidator.evaluateMvelScript("scriptName", registrationDTO));
	}
	@Test
	@Ignore
	public void evaluateMvelScriptTest() {
		UiFieldDTO schemaField = new UiFieldDTO();
		RegistrationDTO registrationDTO = new RegistrationDTO();
		registrationDTO.setProcessId("processId");
		Map<String, Object> applicationMap = new HashMap<>();		
		applicationMap.put(RegistrationConstants.AGE_GROUP_CONFIG, SCRIPT_NAME);
		applicationMap.put(RegistrationConstants.APPLICANT_TYPE_MVEL_SCRIPT, SCRIPT_NAME);
		when(context.map()).thenReturn(applicationMap);
		JSONObject ageGroupConfig = new JSONObject((String) ApplicationContext.map().get(RegistrationConstants.AGE_GROUP_CONFIG));
		System.out.println("ageGroupConfig:  "+ageGroupConfig);
		assertNotNull(requiredFieldValidator.evaluateMvelScript("scriptName", registrationDTO));
	}
	
	private ConditionalBioAttributes ConditionalBioAttributes() {
		List<String> bioAttributes = new ArrayList<String>();
		bioAttributes.add(new String("str1"));
		ConditionalBioAttributes condAttr = new ConditionalBioAttributes();
		condAttr.setProcess("processId");
		condAttr.setBioAttributes(bioAttributes);
		return condAttr;
	}

	private UiFieldDTO getUiFieldDTO() {

		UiFieldDTO schemaField = new UiFieldDTO();
		List<RequiredOnExpr> requiredOn = new ArrayList<RequiredOnExpr>();
		RequiredOnExpr requiredOnExpr = new RequiredOnExpr();
		requiredOnExpr.setEngine("engine");
		requiredOnExpr.setExpr("engine");
		
		List<String> bioAttributes = new ArrayList<String>();
		bioAttributes.add(new String("str1"));
		
		RequiredOnExpr requiredOnExpr1 = new RequiredOnExpr();
		requiredOnExpr1.setEngine("engine1");
		requiredOnExpr1.setExpr("engine1");
		requiredOn.add(requiredOnExpr1);

		List<ConditionalBioAttributes> conditionalAttributes = new ArrayList<ConditionalBioAttributes>();
		ConditionalBioAttributes condAttr = new ConditionalBioAttributes();
		condAttr.setProcess("processId");	
		condAttr.setAgeGroup("ageGroup");
		condAttr.setBioAttributes(bioAttributes);
		conditionalAttributes.add(condAttr);

		
		schemaField.setId("id");
		schemaField.setRequired(Boolean.TRUE);
		schemaField.setRequiredOn(requiredOn);
		schemaField.setConditionalBioAttributes(conditionalAttributes);
		schemaField.setBioAttributes(bioAttributes);
		requiredOnExpr.setEngine("MVEL");
		requiredOnExpr.setExpr("MVEL");
		schemaField.setVisible(requiredOnExpr);
		return schemaField;

	}

}
