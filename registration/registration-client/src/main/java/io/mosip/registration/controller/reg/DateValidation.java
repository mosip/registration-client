package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.LoggerConstants;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.dto.schema.Validator;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

/**
 * Class for validating the date fields
 * 
 * @author Taleev.Aalam
 * @author Balaji
 * @since 1.0.0
 *
 */
@Component
public class DateValidation extends BaseController {

	private static final Logger LOGGER = AppConfig.getLogger(DateValidation.class);
	@Autowired
	private Validations validation;

	int maxAge = 0;

	public boolean isNewValueValid(String newValue, String fieldType) {
		if (newValue.isEmpty())
			return true;

		if (newValue.matches(RegistrationConstants.NUMBER_REGEX)) {
			switch (fieldType) {
			case RegistrationConstants.DD:
				return Integer.parseInt(newValue) > RegistrationConstants.DAYS ? false : true;
			case RegistrationConstants.MM:
				return Integer.parseInt(newValue) > RegistrationConstants.MONTH ? false : true;
			case RegistrationConstants.YYYY:
				return newValue.length() > 4 ? false : true;
			case RegistrationConstants.AGE_FIELD:
				return Integer.parseInt(newValue) < 0 ? false : true;
			}
		}
		return false;
	}

	public boolean validateDate(Pane parentPane, String fieldId) {
		resetFieldStyleClass(parentPane, fieldId, null);

		TextField dd = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
		TextField mm = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
		TextField yyyy = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);

		boolean isValid = false;
		Validator validator = null;
		if (dd.getText().matches(RegistrationConstants.NUMBER_REGEX)
				&& mm.getText().matches(RegistrationConstants.NUMBER_REGEX)
				&& yyyy.getText().matches(RegistrationConstants.NUMBER_REGEX)
				&& yyyy.getText().matches(RegistrationConstants.FOUR_NUMBER_REGEX)) {

			validator = validation.validateSingleString(fieldId,
					getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().get(0));
			isValid = isValidDate(validator, parentPane, dd.getText(), mm.getText(), yyyy.getText(), fieldId);
			if (isValid) {
				populateAge(parentPane, fieldId);
			}
		}

		String defaultErrorMessage = dd.getText().isEmpty() && mm.getText().isEmpty() && yyyy.getText().isEmpty() ? RegistrationConstants.DOB_REQUIRED : RegistrationConstants.INVALID_DATE;
		resetFieldStyleClass(parentPane, fieldId, isValid ? null : getErrorMessage(validator, defaultErrorMessage,
				RegistrationConstants.EMPTY));
		return isValid;
	}

	public boolean validateAge(Pane parentPane, String schemaId) {
		String fieldId = schemaId;
		resetFieldStyleClass(parentPane, fieldId, null);

		TextField ageField = ((TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.AGE_FIELD + RegistrationConstants.TEXT_FIELD));
		if (ageField.getText().isBlank()) {
			TextField dd = (TextField) getFxElement(parentPane,
					fieldId + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
			TextField mm = (TextField) getFxElement(parentPane,
					fieldId + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
			TextField yyyy = (TextField) getFxElement(parentPane,
					fieldId + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);
			dd.setText(RegistrationConstants.EMPTY);
			mm.setText(RegistrationConstants.EMPTY);
			yyyy.setText(RegistrationConstants.EMPTY);

		}
		boolean isValid = ageField.getText().matches(RegistrationConstants.NUMBER_REGEX);
		Validator validator = null;

		if (isValid) {
			int maxAge = Integer.parseInt(getValueFromApplicationContext(RegistrationConstants.MAX_AGE));
			try {
				int age = Integer.parseInt(ageField.getText());
				if (age > maxAge)
					isValid = false;
				else {

					Calendar defaultDate = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("UTC")));
					defaultDate.set(Calendar.DATE, 1);
					defaultDate.set(Calendar.MONTH, 0);
					defaultDate.add(Calendar.YEAR, -age);

					LocalDate date = LocalDate.of(defaultDate.get(Calendar.YEAR), defaultDate.get(Calendar.MONTH) + 1,
							defaultDate.get(Calendar.DATE));
					
					validator = validation.validateSingleString(fieldId,
							getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().get(0));

					isValid = validator != null && validator.getValidator() != null
							? (date.format(DateTimeFormatter.ofPattern(ApplicationContext.getDateFormat())))
									.matches(validator.getValidator())
							: true;
									
					if (isValid) {
						populateDateFields(parentPane, fieldId, age);
					}
				}
			} catch (Exception ex) {
				LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						ExceptionUtils.getStackTrace(ex));
				isValid = false;
			}
		}

		resetFieldStyleClass(parentPane, fieldId, isValid ? null : getErrorMessage(validator, RegistrationConstants.INVALID_AGE,
				maxAge));
		return isValid;
	}

	private String getErrorMessage(Validator validator, String defaultMessageKey, Object... args) {
		ResourceBundle rb = ApplicationContext.getBundle(
				getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().get(0),
				RegistrationConstants.MESSAGES);
		return validator != null && validator.getErrorCode() != null
				&& rb.getString(validator.getErrorCode()) != null ? rb.getString(validator.getErrorCode())
				: MessageFormat.format(rb.getString(defaultMessageKey), args);
	}

	public void resetFieldStyleClass(Pane parentPane, String fieldId, String errorMessage) {
		TextField dd = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
		TextField mm = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
		TextField yyyy = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);
		TextField ageField = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.AGE_FIELD + RegistrationConstants.TEXT_FIELD);

		Label dobMessage = (Label) getFxElement(parentPane, fieldId + RegistrationConstants.ERROR_MSG);

		setTextFieldStyle(parentPane, dd, errorMessage != null);
		setTextFieldStyle(parentPane, mm, errorMessage != null);
		setTextFieldStyle(parentPane, yyyy, errorMessage != null);
		setTextFieldStyle(parentPane, ageField, errorMessage != null);

		if(errorMessage != null) {
			dobMessage.setText(errorMessage);
			dobMessage.setVisible(true);
			generateAlert(parentPane, RegistrationConstants.DOB, dobMessage.getText());
		}
		else {
			dobMessage.setText(RegistrationConstants.EMPTY);
			dobMessage.setVisible(false);
		}
	}

	private void populateAge(Pane parentPane, String fieldId) {

		TextField ageField = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.AGE_FIELD + RegistrationConstants.TEXT_FIELD);
		if (ageField != null) {
			LocalDate date = getCurrentSetDate(parentPane, fieldId);
			String age = String.valueOf(Period.between(date, LocalDate.now(ZoneId.of("UTC"))).getYears());
			if (!age.equals(ageField.getText())) {
				ageField.setText(age);
			}

		}
	}

	private void populateDateFields(Pane parentPane, String fieldId, int age) {
		TextField dd = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
		TextField mm = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
		TextField yyyy = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);

		try {
			LocalDate date = LocalDate.of(Integer.valueOf(yyyy.getText()), Integer.valueOf(mm.getText()),
					Integer.valueOf(dd.getText()));
			if (Period.between(date, LocalDate.now(ZoneId.of("UTC"))).getYears() == age) {
				return;
			}
		} catch (Throwable t) {
			LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
					t.getMessage());
		}

		Calendar defaultDate = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("UTC")));
		defaultDate.set(Calendar.DATE, 1);
		defaultDate.set(Calendar.MONTH, 0);
		defaultDate.add(Calendar.YEAR, -age);

		dd.setText(String.valueOf(defaultDate.get(Calendar.DATE)));
		mm.setText(String.valueOf(defaultDate.get(Calendar.MONTH)+ 1));
		yyyy.setText(String.valueOf(defaultDate.get(Calendar.YEAR)));

	}

	private boolean isValidDate(Validator validator, Pane parentPane, String dd, String mm, String yyyy, String fieldId) {
		if (isValidValue(dd) && isValidValue(mm) && isValidValue(yyyy)) {
			try {
				LocalDate date = LocalDate.of(Integer.valueOf(yyyy), Integer.valueOf(mm), Integer.valueOf(dd));

				if (LocalDate.now().compareTo(date) >= 0) {
					String dob = date.format(DateTimeFormatter.ofPattern(ApplicationContext.getDateFormat()));
					return validator != null && validator.getValidator() != null ? dob.matches(validator.getValidator()) : true;
				}
			} catch (Exception ex) {
				LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						ExceptionUtils.getStackTrace(ex));
			}
		}
		return false;
	}

	private boolean isValidValue(String value) {
		return value != null && !value.isEmpty();
	}

	private LocalDate getCurrentSetDate(Pane parentPane, String fieldId) {

		TextField dd = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
		TextField mm = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
		TextField yyyy = (TextField) getFxElement(parentPane,
				fieldId + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);

		if (isValidValue(dd.getText()) && isValidValue(mm.getText()) && isValidValue(yyyy.getText())) {
			try {
				return LocalDate.of(Integer.valueOf(yyyy.getText()), Integer.valueOf(mm.getText()),
						Integer.valueOf(dd.getText()));
			} catch (Throwable ex) {
				LOGGER.error(LoggerConstants.DATE_VALIDATION, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
						ExceptionUtils.getStackTrace(ex));
			}
		}
		return null;
	}

	private void setTextFieldStyle(Pane parentPane, TextField node, boolean isError) {
		if (parentPane == null || node == null) {
			return;
		}
		Node labelNode = getFxElement(parentPane, node.getId() + RegistrationConstants.LABEL);
		if (labelNode == null) {
			return;
		}
		Label label = (Label) labelNode;
		if (isError) {
			node.getStyleClass().clear();
			node.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD_FOCUSED);
			label.getStyleClass().clear();
			label.getStyleClass().add("demoGraphicFieldLabelOnType");
		} else {
			node.getStyleClass().clear();
			node.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		}
	}

	private Node getFxElement(Pane pane, String fieldId) {
		Node node = pane.lookup(RegistrationConstants.HASH + fieldId);
		if (node == null)
			node = pane.getParent().getParent().getParent().lookup(RegistrationConstants.HASH + fieldId);
		return node;
	}


	/**
	 * Validate for the single string.
	 *
	 * @return <code>true</code>, if successful, else <code>false</code>
	 * @throws ParseException
	 */
	public boolean validateDateWithMaxAndMinDays(Pane parentPane, String fieldId, int minDays, int maxDays) {
			resetFieldStyleClass(parentPane, fieldId, null);

			TextField dd = (TextField) getFxElement(parentPane,
					fieldId + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
			TextField mm = (TextField) getFxElement(parentPane,
					fieldId + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
			TextField yyyy = (TextField) getFxElement(parentPane,
					fieldId + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);

			boolean isValid = false;
			Validator validator = null;
			if (dd.getText().matches(RegistrationConstants.NUMBER_REGEX)
					&& mm.getText().matches(RegistrationConstants.NUMBER_REGEX)
					&& yyyy.getText().matches(RegistrationConstants.NUMBER_REGEX)
					&& yyyy.getText().matches(RegistrationConstants.FOUR_NUMBER_REGEX)) {

				validator = validation.validateSingleString(fieldId,
						getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().get(0));

				LocalDate localDate = LocalDate.of(Integer.valueOf(yyyy.getText()),
							Integer.valueOf(mm.getText()), Integer.valueOf(dd.getText()));

				String dob = localDate.format(DateTimeFormatter.ofPattern(ApplicationContext.getDateFormat()));
				isValid = validator != null && validator.getValidator() != null ? dob.matches(validator.getValidator()) : true;
				if (isValid) {
					LocalDate afterMaxDays = LocalDate.now().plusDays(maxDays);
					LocalDate beforeMinDays = LocalDate.now().plusDays(minDays);
					isValid = (localDate.isAfter(beforeMinDays) && localDate.isBefore(afterMaxDays));
				}
				if (isValid) {
					populateAge(parentPane, fieldId);
				}
			}
			resetFieldStyleClass(parentPane, fieldId, isValid ? null : getErrorMessage(validator, RegistrationConstants.INVALID_DATE_LIMIT,
					minDays, maxDays));
			return isValid;
	}
}
