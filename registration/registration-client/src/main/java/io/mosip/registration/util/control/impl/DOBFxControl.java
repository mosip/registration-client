package io.mosip.registration.util.control.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import io.mosip.registration.controller.ClientApplication;
import org.springframework.context.ApplicationContext;


import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DateValidation;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DOBFxControl extends FxControl {
	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DOBAgeFxControl.class);
	private static final String DOBSubType = "dateOfBirth";
	private static String loggerClassName = "DOB Age Control Type Class";

	private FXUtils fxUtils;

	private DateValidation dateValidation;

	public DOBFxControl() {
		fxUtils = FXUtils.getInstance();
		ApplicationContext applicationContext = ClientApplication.getApplicationContext();
		this.dateValidation = applicationContext.getBean(DateValidation.class);
	}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;
		VBox appLangDateVBox = create(uiFieldDTO);
		HBox hBox = new HBox();
		hBox.setSpacing(30);
		hBox.getChildren().add(appLangDateVBox);
		HBox.setHgrow(appLangDateVBox, Priority.ALWAYS);

		this.node = hBox;
		setListener(hBox);
		return this.control;
	}

	private VBox create(UiFieldDTO uiFieldDTO) {

		HBox dobHBox = new HBox();
		dobHBox.setId(uiFieldDTO.getId() + RegistrationConstants.HBOX);
		dobHBox.setSpacing(10);

		String mandatorySuffix = getMandatorySuffix(uiFieldDTO);

		String langCode = getRegistrationDTo().getSelectedLanguagesByApplicant().get(0);
		ResourceBundle resourceBundle = io.mosip.registration.context.ApplicationContext.getInstance()
				.getBundle(langCode, RegistrationConstants.LABELS);

		VBox ageVBox = new VBox();
		ageVBox.setPrefWidth(390);

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiFieldDTO.getLabel().get(lCode));
		});

		/** DOB Label */
		ageVBox.getChildren().add(getLabel(uiFieldDTO.getId() + RegistrationConstants.LABEL,
				String.join(RegistrationConstants.SLASH, labels) + mandatorySuffix,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, ageVBox.getWidth()));

		/** Add Date */
		dobHBox.getChildren().add(addDateTextField(uiFieldDTO, RegistrationConstants.DD,
				resourceBundle.getString(RegistrationConstants.DD)));
		/** Add Month */
		dobHBox.getChildren().add(addDateTextField(uiFieldDTO, RegistrationConstants.MM,
				resourceBundle.getString(RegistrationConstants.MM)));
		/** Add Year */
		dobHBox.getChildren().add(addDateTextField(uiFieldDTO, RegistrationConstants.YYYY,
				resourceBundle.getString(RegistrationConstants.YYYY)));

		ageVBox.getChildren().add(dobHBox);

		/** Validation message (Invalid/wrong,,etc,.) */
		ageVBox.getChildren().add(getLabel(uiFieldDTO.getId() + RegistrationConstants.ERROR_MSG, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, ageVBox.getPrefWidth()));

		dobHBox.prefWidthProperty().bind(ageVBox.widthProperty());

		changeNodeOrientation(ageVBox, langCode);
		return ageVBox;
	}

	private VBox addDateTextField(UiFieldDTO uiFieldDTO, String dd, String text) {

		VBox dateVBox = new VBox();
		dateVBox.setId(uiFieldDTO.getId() + dd + RegistrationConstants.VBOX);

		double prefWidth = dateVBox.getPrefWidth();

		/** DOB Label */
		dateVBox.getChildren().add(getLabel(uiFieldDTO.getId() + dd + RegistrationConstants.LABEL, text,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, prefWidth));

		/** DOB Text Field */
		dateVBox.getChildren().add(getTextField(uiFieldDTO.getId() + dd + RegistrationConstants.TEXT_FIELD, text,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false));

		return dateVBox;
	}

	@Override
	public void setData(Object data) {

		TextField dd = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
		TextField mm = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
		TextField yyyy = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);

		getRegistrationDTo().setDateField(uiFieldDTO.getId(), dd.getText(), mm.getText(), yyyy.getText(), uiFieldDTO.getSubType());
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiFieldDTO.getId());
	}

	@Override
	public boolean isValid() {
		return dateValidation.validateDateWithMaxAndMinDays((Pane) getNode(), uiFieldDTO.getId(),
				getUiSchemaDTO().getMinimum(), getUiSchemaDTO().getMaximum());
	}

	@Override
	public boolean isEmpty() {
		TextField dd = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
		TextField mm = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
		TextField yyyy = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);
		return dd != null && dd.getText().isEmpty() && mm != null && mm.getText().isEmpty() && yyyy != null
				&& yyyy.getText().isEmpty();
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}

	@Override
	public void setListener(Node node) {

		addListener(
				(TextField) getField(uiFieldDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD),
				RegistrationConstants.DD);
		addListener(
				(TextField) getField(uiFieldDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD),
				RegistrationConstants.MM);
		addListener(
				(TextField) getField(
						uiFieldDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD),
				RegistrationConstants.YYYY);

	}

	private void addListener(TextField textField, String dateType) {
		textField.textProperty().addListener((ob, ov, nv) -> {
			fxUtils.toggleUIField((Pane) node,
					textField.getId().replaceAll(RegistrationConstants.TEXT_FIELD, "") + RegistrationConstants.LABEL,
					!textField.getText().isEmpty());

			if (!dateValidation.isNewValueValid(nv, dateType)) {
				textField.setText(ov);
			}
			boolean isValid = dateValidation.validateDateWithMaxAndMinDays((Pane) getNode(), uiFieldDTO.getId(),
					getUiSchemaDTO().getMinimum(), getUiSchemaDTO().getMaximum());
			if (isValid) {
				setData(null);
				refreshFields();
			}
		});
	}

	@Override
	public void fillData(Object data) {
		// TODO Parse and set the date
	}

	private TextField getTextField(String id, String titleText, String demographicTextfield, double prefWidth,
			boolean isDisable) {

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(id);
		textField.setPromptText(titleText);
		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		// textField.setPrefWidth(prefWidth);
		textField.setDisable(isDisable);

		return textField;
	}

	@Override
	public void selectAndSet(Object data) {
		TextField yyyy = ((TextField) getField(
				this.uiFieldDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD));
		TextField mm = ((TextField) getField(
				this.uiFieldDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD));
		TextField dd = ((TextField) getField(
				this.uiFieldDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD));

		if(data == null || ((String) data).trim().isEmpty()) {
			yyyy.clear();
			mm.clear();
			dd.clear();
			return;
		}

		String[] dobArray = ((String) data).split("/");
		yyyy.setText(dobArray[0]);
		mm.setText(dobArray[1]);
		dd.setText(dobArray[2]);

	}
}
