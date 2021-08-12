package io.mosip.registration.validator;

import java.nio.file.Paths;
import java.util.*;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.dto.schema.ConditionalBioAttributes;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.schema.RequiredOnExpr;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.service.IdentitySchemaService;

@Component
public class RequiredFieldValidator {

	private static final Logger LOGGER = AppConfig.getLogger(RequiredFieldValidator.class);

	@Autowired
	private IdentitySchemaService identitySchemaService;

	public boolean isRequiredField(UiFieldDTO schemaField, RegistrationDTO registrationDTO) {
		boolean required = schemaField != null ? schemaField.isRequired() : false;
		if (schemaField != null && schemaField.getRequiredOn() != null && !schemaField.getRequiredOn().isEmpty()) {
			Optional<RequiredOnExpr> expression = schemaField.getRequiredOn().stream()
					.filter(field -> "MVEL".equalsIgnoreCase(field.getEngine()) && field.getExpr() != null).findFirst();

			if (expression.isPresent()) {
				required = executeMVEL(expression.get().getExpr(), registrationDTO);
				LOGGER.info("Refreshed {} field isRequired check, required ? {} ", schemaField.getId(), required);
			}
		}
		return required;
	}

	public boolean isFieldVisible(UiFieldDTO schemaField, RegistrationDTO registrationDTO) {
		boolean visible = true;

		if (schemaField != null && schemaField.getVisible() != null && schemaField.getVisible().getEngine().equalsIgnoreCase("MVEL")
				&& schemaField.getVisible().getExpr() != null) {
			visible = executeMVEL(schemaField.getVisible().getExpr(), registrationDTO);
			LOGGER.info("Refreshed {} field visibility : {} ", schemaField.getId(), visible);
		}
		return visible;
	}

	public List<String> getRequiredBioAttributes(UiFieldDTO field, RegistrationDTO registrationDTO) {
		if(!isRequiredField(field, registrationDTO))
			return Collections.EMPTY_LIST;

		if(field.getConditionalBioAttributes() != null) {
			ConditionalBioAttributes selectedCondition = getConditionalBioAttributes(field, registrationDTO);
			if(selectedCondition != null)
				return selectedCondition.getBioAttributes();
		}
		return field.getBioAttributes();
	}

	public ConditionalBioAttributes getConditionalBioAttributes(UiFieldDTO uiFieldDTO, RegistrationDTO registrationDTO) {
		if(uiFieldDTO.getConditionalBioAttributes() == null)
			return null;

		Optional<ConditionalBioAttributes> result = uiFieldDTO.getConditionalBioAttributes().stream().filter(c ->
				c.getAgeGroup().equalsIgnoreCase(registrationDTO.getAgeGroup()) &&
						c.getProcess().equalsIgnoreCase(registrationDTO.getProcessId())).findFirst();

		if(!result.isPresent()) {
			result = uiFieldDTO.getConditionalBioAttributes().stream().filter(c ->
					(c.getAgeGroup().equalsIgnoreCase(registrationDTO.getAgeGroup()) &&
							c.getProcess().equalsIgnoreCase("ALL")) ||
							(c.getAgeGroup().equalsIgnoreCase("ALL") &&
									c.getProcess().equalsIgnoreCase(registrationDTO.getProcessId())) ||
							(c.getAgeGroup().equalsIgnoreCase("ALL") &&
									c.getProcess().equalsIgnoreCase("ALL"))).findFirst();
		}
		return result.isPresent() ? result.get() : null;
	}

	private boolean executeMVEL(String expression, RegistrationDTO registrationDTO) {
		try {
			Map context = new HashMap();
			context.put("identity", registrationDTO.getMVELDataContext());
			VariableResolverFactory resolverFactory = new MapVariableResolverFactory(context);
			return MVEL.evalToBoolean(expression, resolverFactory);
		} catch (Throwable t) {
			LOGGER.error("Failed to evaluate mvel expr", t);
		}
		return false;
	}

	public Object evaluateMvelScript(String scriptName, RegistrationDTO registrationDTO) {
		try {

			Map<String, String>  ageGroups = new HashMap<String, String>();
			ageGroups.put("INFANT", "0-5");
			ageGroups.put("MINOR", "6-17");
			ageGroups.put("ADULT", "18-200");

			Map context = new HashMap();
			MVEL.evalFile(Paths.get(System.getProperty("user.dir"), scriptName).toFile(), context);
			context.put("identity", registrationDTO.getMVELDataContext());
			context.put("ageGroups", ageGroups);
			return MVEL.eval("return getApplicantType();", context, String.class);

		} catch (Throwable t) {
			LOGGER.error("Failed to evaluate mvel script", t);
		}
		return null;
	}

}
