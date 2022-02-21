package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import io.mosip.registration.controller.GenericController;
import io.mosip.registration.enums.FlowType;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.RidValidator;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.idvalidator.spi.VidValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.dto.schema.Validator;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Class for validation of the Registration Field
 * 
 * @author Taleev.Aalam
 * @author Balaji
 * @since 1.0.0
 *
 */
@Component
public class Validations extends BaseController {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(Validations.class);
	private static ResourceBundle applicationMessageBundle;
	private StringBuilder validationMessage;
	private List<String> noAlert;

	@Autowired
	private RequiredFieldValidator requiredFieldValidator;
	@Autowired
	private UinValidator<String> uinValidator;
	@Autowired
	private RidValidator<String> ridValidator;
	@Autowired
	private VidValidator<String> vidValidator;
	@Autowired
	private DateValidation dateValidation;
	@Autowired
	private MasterSyncService masterSync;

	/**
	 * Instantiates a new validations.
	 */
	public Validations() {
		try {
			noAlert = new ArrayList<>();
			noAlert.add(RegistrationConstants.DD);
			noAlert.add(RegistrationConstants.MM);
			noAlert.add(RegistrationConstants.YYYY);
			noAlert.add(RegistrationConstants.DD + RegistrationConstants.LOCAL_LANGUAGE);
			noAlert.add(RegistrationConstants.MM + RegistrationConstants.LOCAL_LANGUAGE);
			noAlert.add(RegistrationConstants.YYYY + RegistrationConstants.LOCAL_LANGUAGE);
			validationMessage = new StringBuilder();
		} catch (RuntimeException runtimeException) {
			LOGGER.error(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	/**
	 * Sets the resource bundles for Validations, Messages in Application and
	 * Secondary Languages and Labels in Application and Secondary Languages
	 */
	public static void setResourceBundle(ResourceBundle resourceBundle) {
		applicationMessageBundle = resourceBundle;
	}


	/**
	 * Validate for the TextField.
	 *
	 * @param parentPane      the {@link Pane} containing the fields
	 * @param node            the {@link Node} to be validated
	 * @param id              the id of the UI field
	 * @param isPreviousValid
	 * @return true, if successful
	 */
	public boolean validateTextField(Pane parentPane, TextField node, String id, boolean isPreviousValid,
			String langCode) {
		return languageSpecificValidation(parentPane, node, id, getMessagesBundle(langCode), isPreviousValid, langCode);
	}
	
	public boolean validateForBlockListedWords(Pane parentPane, TextField node, String fieldId, boolean isPreviousValid,
			String langCode) {
		LOGGER.debug("started to validate :: {} for blocklisted words", fieldId);
		
		ResourceBundle messageBundle = getMessagesBundle(langCode);
		boolean showAlert = (noAlert.contains(node.getId()) && fieldId.contains(RegistrationConstants.ON_TYPE));
		if (validateBlockListedWords(parentPane, node, node.getId(), fieldId, showAlert, messageBundle)) {
			return true;
		}

		return false;
	}

	private ResourceBundle getMessagesBundle(String langCode) {
		return ApplicationContext.getBundle(langCode, RegistrationConstants.MESSAGES);
	}

	/**
	 * Language specific validation of text field
	 *
	 * @param parentPane      the {@link Pane} containing the fields
	 * @param node            the {@link Node} to be validated
	 * @param id              the id of the UI field
	 * @param isPreviousValid
	 * @return true, if successful
	 */
	private boolean languageSpecificValidation(Pane parentPane, TextField node, String id, ResourceBundle messageBundle,
			boolean isPreviousValid, String langCode) {
		LOGGER.debug("started to validate :: {} " , id);
		boolean isInputValid = true;
		try {

			boolean showAlert = (noAlert.contains(node.getId()) && id.contains(RegistrationConstants.ON_TYPE));

			UiFieldDTO uiFieldDTO = GenericController.getFxControlMap().get(id).getUiSchemaDTO();

			if (uiFieldDTO != null) {
				if (requiredFieldValidator.isRequiredField(uiFieldDTO, getRegistrationDTOFromSession())
						&& !isMandatoryFieldFilled(parentPane, uiFieldDTO, node, node.getText())) {
					generateInvalidValueAlert(parentPane, id + langCode,
							getFromLabelMap(id + langCode).concat(RegistrationConstants.SPACE)
									.concat(messageBundle.getString(RegistrationConstants.REG_LGN_001)),
							showAlert);
					if (isPreviousValid) {
						addInvalidInputStyleClass(parentPane, node, true);
					}
					isInputValid = false;
				} else { /** Remove Error message for fields */
					Label messageLable = ((Label) (parentPane
							.lookup(RegistrationConstants.HASH + node.getId() + RegistrationConstants.MESSAGE)));
					if (messageLable != null) {
						messageLable.setText(RegistrationConstants.EMPTY);
					}
					isInputValid = true;
				}

				if (node.isVisible() && (node.getText() != null && !node.getText().isEmpty())) {
				isInputValid = checkForValidValue(parentPane, node, id, node.getText(), messageBundle, showAlert,
						isPreviousValid, uiFieldDTO, langCode);
			}
			
			}

		} catch (RuntimeException runtimeException) {
			LOGGER.error(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
		return isInputValid;
	}

	private boolean checkForValidValue(Pane parentPane, TextField node, String fieldId, String value,
                                       ResourceBundle messageBundle, boolean showAlert, boolean isPreviousValid,
                                       UiFieldDTO uiFieldDTO, String langCode) {

		boolean isLocalLanguageField = node.getId().contains(RegistrationConstants.LOCAL_LANGUAGE);

		if (RegistrationConstants.AGE_DATE.equals(uiFieldDTO.getControlType())) {
			boolean isValid = dateValidation.validateDate(parentPane, uiFieldDTO.getId());
			LOGGER.debug(RegistrationConstants.VALIDATION_LOGGER, APPLICATION_NAME, APPLICATION_ID,
					"Date validation FAILED :: " + uiFieldDTO.getId());
			if (!isValid)
				return false;
		}

		Validator validator = getRegex(fieldId, RegistrationConstants.REGEX_TYPE, langCode);
		if (validator != null && validator.getValidator() != null && !value.matches(validator.getValidator())) {
			String errorMessage = validator.getErrorCode() != null
					&& messageBundle.containsKey(validator.getErrorCode())
							? messageBundle.getString(validator.getErrorCode())
							: (getFromLabelMap(fieldId + langCode).concat(RegistrationConstants.SPACE)
									.concat(messageBundle.getString(RegistrationConstants.REG_DDC_004)));
			generateInvalidValueAlert(parentPane, node.getId(), errorMessage, showAlert);
			if (isPreviousValid && !node.getId().contains(RegistrationConstants.ON_TYPE)) {
				addInvalidInputStyleClass(parentPane, node, false);
			}
			return false;
		}

		if (!isLocalLanguageField && uiFieldDTO != null
				&& Arrays.asList("UIN", "RID","VID").contains(uiFieldDTO.getSubType())
				&& !validateUinOrRidField(value, getRegistrationDTOFromSession(), uiFieldDTO)) {
			generateInvalidValueAlert(parentPane, node.getId(),
					getFromLabelMap(fieldId).concat(RegistrationConstants.SPACE)
							.concat(messageBundle.getString(RegistrationConstants.REG_DDC_004)),
					showAlert);
			if (isPreviousValid && !node.getId().contains(RegistrationConstants.ON_TYPE)) {
				addInvalidInputStyleClass(parentPane, node, false);
			}
			return false;
		}

		addValidInputStyleClass(parentPane, node);
		return true;
	}

	private boolean isMandatoryFieldFilled(Pane parentPane, UiFieldDTO uiFieldDTO, TextField node, String value) {
		boolean fieldFilled = false;

		if (!node.isVisible()) {
			generateAlert(parentPane, "Mandatory field is hidden : " + uiFieldDTO.getId(),
					RegistrationConstants.ERROR);
			return fieldFilled;
		}

		RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
		switch (registrationDTO.getFlowType()) {
			case UPDATE:
				if (node.isDisabled()) {
					return true;
				}
				fieldFilled = doMandatoryCheckOnUpdateUIN(parentPane, value, node.getId(), uiFieldDTO, true, node,
						registrationDTO);
				break;
			case NEW:
				fieldFilled = doMandatoryCheckOnNewReg(value, uiFieldDTO, true);
				break;
		}
		return fieldFilled;
	}

	private void addValidInputStyleClass(Pane parentPane, TextField node) {
		String id = node.getId().substring(0, node.getId().length()-3);
		Label nodeLabel = (Label) parentPane.lookup("#" + id + "Label");

		if (nodeLabel == null && parentPane.getParent() != null && parentPane.getParent().getParent() != null
				&& parentPane.getParent().getParent().getParent() != null) {
			nodeLabel = (Label) parentPane.getParent().getParent().getParent().lookup("#" + id + "Label");
		}
		// node.requestFocus();
		node.getStyleClass().removeIf((s) -> {
			return s.equals(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
		});
		node.getStyleClass().removeIf((s) -> {
			return s.equals("demoGraphicTextFieldOnType");
		});
		if (nodeLabel != null) {
			nodeLabel.getStyleClass().removeIf((s) -> {
				return s.equals("demoGraphicFieldLabelOnType");
			});
		}		
		node.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
	}

	private void addInvalidInputStyleClass(Pane parentPane, Node node, boolean mandatoryCheck) {
		if (mandatoryCheck) {
			node.getStyleClass().removeIf((s) -> {
				return s.equals(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
			});
			node.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
		} else {
			Label nodeLabel = (Label) parentPane.lookup("#" +
					node.getId().substring(0, node.getId().length() - RegistrationConstants.LANGCODE_LENGTH) + "Label");
			node.requestFocus();
			node.getStyleClass().removeIf((s) -> {
				return s.equals(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
			});
			node.getStyleClass().removeIf((s) -> {
				return s.equals("demoGraphicTextFieldOnType");
			});
			nodeLabel.getStyleClass().removeIf((s) -> {
				return s.equals("demoGraphicFieldLabelOnType");
			});
			node.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
		}
	}

	private boolean doMandatoryCheckOnNewReg(String inputText, UiFieldDTO schemaField, boolean isMandatory) {
		if (schemaField != null) {

			if (isMandatory && (inputText == null || inputText.isEmpty())) {
				return false;
			}
		}
		return true;
	}

	private boolean doMandatoryCheckOnUpdateUIN(Pane parentPane, String inputText, String id, UiFieldDTO schemaField,
			boolean isMandatory, Node node, RegistrationDTO registrationDto) {

		if (schemaField != null && !node.isDisabled()) {
			if (isMandatory && (inputText == null || inputText.isEmpty())) {
				return false;
			}
		}
		return true;
	}

	public List<String> getBlockListedWordsList(TextField textField) {
		List<String> blockListedWords = getRegistrationDTOFromSession().getConfiguredBlockListedWords();
		List<String> inputText = new ArrayList<>(Arrays.asList(textField.getText().split(RegistrationConstants.SPACE)));
		if (inputText.size() > 1) inputText.add(textField.getText());
		return blockListedWords.stream()
				.map(String::toLowerCase)
				.distinct()
				.filter(bword -> inputText.stream().anyMatch(text -> text.equalsIgnoreCase(bword) ||
						text.toLowerCase().contains(bword)))
				.collect(Collectors.toList());
	}

	private boolean validateBlockListedWords(Pane parentPane, TextField node, String id, String fieldId,
			boolean showAlert, ResourceBundle messageBundle) {

		if (node.getText()==null || id.contains(RegistrationConstants.ON_TYPE))
			return true;

		List<String> identifiedBlockListedWords = getBlockListedWordsList(node);
		if(identifiedBlockListedWords.isEmpty())
			return true;

		List<String> acceptedWords = getRegistrationDTOFromSession().BLOCKLISTED_CHECK.containsKey(fieldId) ?
				getRegistrationDTOFromSession().BLOCKLISTED_CHECK.get(fieldId).getWords() : Collections.EMPTY_LIST;
		if(acceptedWords.isEmpty()) {
			generateInvalidValueAlert(parentPane, id, MessageFormat.format(messageBundle.getString("BLOCKLISTED_ERROR"),
					identifiedBlockListedWords.toString()), showAlert);
			return false;
		}

		List<String> unAcceptedWords = identifiedBlockListedWords.stream()
				.filter(w -> acceptedWords.stream().noneMatch(bw ->
						bw.equalsIgnoreCase(w) || w.contains(bw))).collect(Collectors.toList());
		if (unAcceptedWords.isEmpty())
			return true;

		generateInvalidValueAlert(parentPane, id, MessageFormat.format(messageBundle.getString("BLOCKLISTED_ERROR"),
				unAcceptedWords.toString()), showAlert);
		return false;
	}

	private void generateInvalidValueAlert(Pane parentPane, String id, String message, boolean showAlert) {
		if (!showAlert)
			generateAlert(parentPane, id, message);
	}

	public void setErrorMessage(Pane parentPane, String id, String label, String errorType, boolean showAlert,
			ResourceBundle messageBundle) {
		if (!showAlert) {

			generateInvalidValueAlert(parentPane, id,
					label.concat(RegistrationConstants.SPACE).concat(messageBundle.getString(errorType)), showAlert);

		}

	}

	private boolean validateUinOrRidField(String inputText, RegistrationDTO registrationDto, UiFieldDTO schemaField) {
		boolean isValid = true;
		try {
			
			switch (schemaField.getSubType()) {
				case "UIN":
					String updateUIN = FlowType.UPDATE == registrationDto.getFlowType()
								? (String) registrationDto.getDemographics().get("UIN")
								: null;

					if (updateUIN != null && inputText.equals(updateUIN))
						isValid = false;
	
					if (isValid)
						isValid = uinValidator.validateId(inputText);
					break;
			
				case "RID": 
					isValid = ridValidator.validateId(inputText);
					break;
				case "VID":
					isValid = vidValidator.validateId(inputText);
					break;
			}

		} catch (InvalidIDException invalidRidException) {
			isValid = false;
			LOGGER.error(schemaField.getSubType() + " VALIDATION FAILED", APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, ExceptionUtils.getStackTrace(invalidRidException));
		}
		return isValid;
	}

	/**
	 * Validate for the single string.
	 *
	 * @param id    the id of the UI field whose value is provided as input
	 * @param langCode the langCode for validation
	 * @return <code>Validator</code>
	 */
	public Validator validateSingleString(String id, String langCode) {
		return getRegex(id, RegistrationConstants.REGEX_TYPE, langCode);
	}

	private Validator getRegex(String fieldId, String regexType, String langCode) {
		UiFieldDTO uiFieldDTO = GenericController.getFxControlMap().get(fieldId).getUiSchemaDTO();
		if (uiFieldDTO != null && uiFieldDTO.getValidators() != null) {

			Optional<Validator> validator = (langCode != null) ? uiFieldDTO.getValidators().stream()
					.filter(v -> v.getType().equalsIgnoreCase(regexType)
							&& (v.getLangCode() != null ? langCode.equalsIgnoreCase(v.getLangCode()) : true))
					.findFirst() :
					uiFieldDTO.getValidators().stream()
							.filter(v -> v.getType().equalsIgnoreCase(regexType)
									&& v.getLangCode() == null).findFirst() ;
			if (validator.isPresent()) {
				return validator.get();
			}
		}
		return null;
	}
}
