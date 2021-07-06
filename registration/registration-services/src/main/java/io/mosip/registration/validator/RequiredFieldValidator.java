package io.mosip.registration.validator;

import java.util.*;
import java.util.stream.Collectors;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.dto.schema.ConditionalBioAttributes;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.commons.packet.constants.PacketManagerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.schema.RequiredOnExpr;
import io.mosip.registration.dto.schema.UiSchemaDTO;
import io.mosip.registration.dto.response.SchemaDto;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.IdentitySchemaService;

@Component
public class RequiredFieldValidator {

	private static final Logger LOGGER = AppConfig.getLogger(RequiredFieldValidator.class);
	//private static final String APPLICANT_SUBTYPE = "applicant";

	@Autowired
	private IdentitySchemaService identitySchemaService;

	public boolean isRequiredField(String fieldId, RegistrationDTO registrationDTO) throws RegBaseCheckedException {
		SchemaDto schema = identitySchemaService.getIdentitySchema(registrationDTO.getIdSchemaVersion());
		Optional<UiSchemaDTO> schemaField = schema.getSchema().stream().filter(field -> field.getId().equals(fieldId))
				.findFirst();
		if (!schemaField.isPresent())
			return false;

		return isRequiredField(schemaField.get(), registrationDTO);
	}

	//TODO - add validations for updateUIN flow as well
	public boolean isRequiredField(UiSchemaDTO schemaField, RegistrationDTO registrationDTO) {
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

	public boolean isFieldVisible(UiSchemaDTO schemaField, RegistrationDTO registrationDTO) {
		boolean visible = true;

		if (schemaField != null && schemaField.getVisible() != null && schemaField.getVisible().getEngine().equalsIgnoreCase("MVEL")
				&& schemaField.getVisible().getExpr() != null) {
			visible = executeMVEL(schemaField.getVisible().getExpr(), registrationDTO);
			LOGGER.info("Refreshed {} field visibility : {} ", schemaField.getId(), visible);
		}
		return visible;
	}

	public List<String> getRequiredBioAttributes(UiSchemaDTO field, RegistrationDTO registrationDTO) {
		if(!isRequiredField(field, registrationDTO))
			return Collections.EMPTY_LIST;

		if(field.getConditionalBioAttributes() != null) {
			ConditionalBioAttributes selectedCondition = getConditionalBioAttributes(field, registrationDTO);
			if(selectedCondition != null)
				return selectedCondition.getBioAttributes();
		}
		return field.getBioAttributes();
	}

	public ConditionalBioAttributes getConditionalBioAttributes(UiSchemaDTO uiSchemaDTO, RegistrationDTO registrationDTO) {
		if(uiSchemaDTO.getConditionalBioAttributes() == null)
			return null;

		Optional<ConditionalBioAttributes> result = uiSchemaDTO.getConditionalBioAttributes().stream().filter(c ->
				c.getAgeGroup().equalsIgnoreCase(registrationDTO.getAgeGroup()) &&
						c.getProcess().equalsIgnoreCase(registrationDTO.getRegistrationCategory())).findFirst();

		if(!result.isPresent()) {
			result = uiSchemaDTO.getConditionalBioAttributes().stream().filter(c ->
					(c.getAgeGroup().equalsIgnoreCase(registrationDTO.getAgeGroup()) &&
							c.getProcess().equalsIgnoreCase("ALL")) ||
							(c.getAgeGroup().equalsIgnoreCase("ALL") &&
									c.getProcess().equalsIgnoreCase(registrationDTO.getRegistrationCategory())) ||
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

}
