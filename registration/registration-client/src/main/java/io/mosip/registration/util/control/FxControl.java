/**
 *
 */
package io.mosip.registration.util.control;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.ArrayList;
import java.util.List;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.controller.GenericController;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.enums.FlowType;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 *
 * Control Type will give high level controls for fields TextField,CheckBox,
 * DropDown,DropDown, Buttons, document type, Biometric Type
 *
 * It also provides a features to copy,disable and visible
 *
 * @author YASWANTH S
 *
 */
public abstract class FxControl  {

	protected static final Logger LOGGER = AppConfig.getLogger(FxControl.class);
	private static final String loggerClassName = "FxControl";

	protected UiFieldDTO uiFieldDTO;
	protected FxControl control;
	public Node node;

	protected AuditManagerService auditFactory;
	protected RequiredFieldValidator requiredFieldValidator;


	/**
	 * Build Error code, title and fx Element Set Listeners Set Actione events
	 *
	 * @param uiFieldDTO field information
	 */
	public abstract FxControl build(UiFieldDTO uiFieldDTO);

	/**
	 *
	 * @param node
	 */
	public abstract void setListener(Node node);

	/**
	 *
	 * Set Data into Registration DTO
	 *
	 * @param data value
	 */
	public abstract void setData(Object data);

	/**
	 *
	 * Fill Data into fx element
	 *
	 * @param data value
	 */
	public abstract void fillData(Object data);

	/**
	 * Get Value from fx element
	 *
	 * @return Value
	 */
	public abstract Object getData();

	/**
	 * Check value is valid or not
	 *
	 * @return boolean is valid or not
	 */
	//public abstract boolean isValid(Node node);

	/**
	 *
	 * @param data
	 */
	public abstract void selectAndSet(Object data);

	/**
	 * Check value is valid or not
	 *
	 * @return boolean is valid or not
	 */
	public abstract boolean isValid();

	/**
	 * Check value is empty
	 *
	 * @return boolean is valid or not
	 */
	public abstract boolean isEmpty();

	/**
	 *
	 * @param langCode
	 * @return
	 */
	public abstract List<GenericDto> getPossibleValues(String langCode);


	/**
	 * Disable the field
	 */
	public void disable(Node node, boolean isDisable) {

		node.setDisable(isDisable);

	}

	/**
	 * Refresh the field
	 */
	public void refresh() {
		boolean isFieldVisible =  isFieldVisible(uiFieldDTO);
		if(!isFieldVisible) {
			switch (uiFieldDTO.getType()) {
				case "documentType":
					getRegistrationDTo().removeDocument(uiFieldDTO.getId());
					break;
				case "biometricsType":
					List<String> requiredAttributes = requiredFieldValidator.getRequiredBioAttributes(uiFieldDTO, getRegistrationDTo());
					for(String bioAttribute : uiFieldDTO.getBioAttributes()) {
						if(!requiredAttributes.contains(bioAttribute))
							getRegistrationDTo().clearBIOCache(uiFieldDTO.getId(), bioAttribute);
					}
					break;
				default:
					getRegistrationDTo().removeDemographicField(uiFieldDTO.getId());
					break;
			}
		}
		visible(this.node, isFieldVisible(uiFieldDTO));
		setMandatorySuffix(this.node);

	}

	/**
	 * Hide the field
	 */
	public void visible(Node node, boolean isVisible) {
		node.setVisible(isVisible);
		node.setManaged(isVisible);
	}

	public void setMandatorySuffix(Node node) {
		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			if(this.uiFieldDTO.getLabel().get(lCode) != null) {
				labels.add(this.uiFieldDTO.getLabel().get(lCode));
			}
		});

		if(labels.size() > 0) {
			String titleText = String.join(RegistrationConstants.SLASH, labels) + getMandatorySuffix(uiFieldDTO);

			if(node instanceof VBox) {
				var element = ((VBox)node).getChildren().get(0);
				if(element instanceof Label) {
					((Label)element).setText(titleText);
				}
			}
		}
	}

	/**
	 *
	 */
	public void refreshFields() {
		LOGGER.info(loggerClassName, APPLICATION_NAME, APPLICATION_ID, "Refreshing fields from fx control");
		GenericController genericController = ClientApplication.getApplicationContext().getBean(GenericController.class);
		genericController.refreshFields();
	}

	/**
	 *
	 * @return
	 */
	public boolean canContinue() {
		//field is not visible, ignoring valid value and isRequired check
		if(!isFieldVisible(this.uiFieldDTO)) {
			return true;
		}

		if (requiredFieldValidator == null) {
			requiredFieldValidator = ClientApplication.getApplicationContext().getBean(RequiredFieldValidator.class);
		}

		try {
			boolean isValid = isValid();
			if(isValid)
				return true;

			boolean isRequiredField = requiredFieldValidator.isRequiredField(this.uiFieldDTO, getRegistrationDTo());
			if(isEmpty() && !isRequiredField)
				return true;

			if(getRegistrationDTo().getFlowType() == FlowType.UPDATE
					&& !getRegistrationDTo().getUpdatableFields().contains(this.uiFieldDTO.getId())) {
				LOGGER.error("canContinue check on, {} is non-updatable ignoring", uiFieldDTO.getId());
				return true;
			}
		} catch (Exception exception) {
			LOGGER.error("Error checking RequiredOn for field : " + uiFieldDTO.getId(), exception);
		}
		return false;
	}

	/**
	 *
	 * @param schema
	 * @return
	 */
	protected String getMandatorySuffix(UiFieldDTO schema) {
		String mandatorySuffix = RegistrationConstants.EMPTY;
		requiredFieldValidator = ClientApplication.getApplicationContext().getBean(RequiredFieldValidator.class);
		boolean isRequired = requiredFieldValidator.isRequiredField(schema, getRegistrationDTo());

		switch (getRegistrationDTo().getFlowType()) {
			case UPDATE:
				if (getRegistrationDTo().getUpdatableFields().contains(schema.getId())) {
					mandatorySuffix = isRequired ? RegistrationConstants.ASTRIK : RegistrationConstants.EMPTY;
				}
				break;

			case CORRECTION:
			case NEW:
				mandatorySuffix = isRequired ? RegistrationConstants.ASTRIK : RegistrationConstants.EMPTY;
				break;
		}
		return mandatorySuffix;
	}

	/**
	 *
	 * @param id
	 * @param titleText
	 * @param styleClass
	 * @param isVisible
	 * @param prefWidth
	 * @return
	 */
	protected Label getLabel(String id, String titleText, String styleClass, boolean isVisible, double prefWidth) {
		/** Field Title */
		Label label = new Label();
		label.setId(id);
		label.setText(titleText);
		label.getStyleClass().add(styleClass);
		label.setVisible(isVisible);
		label.setWrapText(true);
		// label.setPrefWidth(prefWidth);
		return label;
	}

	protected RegistrationDTO getRegistrationDTo() {
		RegistrationDTO registrationDTO = null;
		if (SessionContext.map() != null || !SessionContext.map().isEmpty()) {
			registrationDTO = (RegistrationDTO) SessionContext.map().get(RegistrationConstants.REGISTRATION_DATA);
		}
		return registrationDTO;
	}

	protected boolean isFieldVisible(UiFieldDTO schemaDTO) {
		if (requiredFieldValidator == null) {
			requiredFieldValidator = ClientApplication.getApplicationContext().getBean(RequiredFieldValidator.class);
		}
		try {
			boolean isVisibleAccordingToSpec = requiredFieldValidator.isFieldVisible(schemaDTO, getRegistrationDTo());

			switch (getRegistrationDTo().getFlowType()) {
				case UPDATE:
					return (getRegistrationDTo().getUpdatableFields().contains(schemaDTO.getId())) ? isVisibleAccordingToSpec : false;
				case CORRECTION:
				case NEW:
				case LOST:
					return isVisibleAccordingToSpec;
			}
		} catch (Exception exception) {
			LOGGER.error("Failed to check field visibility", exception);
		}
		return true;
	}

	protected void changeNodeOrientation(Node node, String langCode) {

		if (ApplicationContext.getInstance().isLanguageRightToLeft(langCode)) {
			node.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
		}
	}

	protected void addValidationMessage(VBox vBox, String id, String langCode, String styleClass, boolean isVisible) {
		Label validationMessage = getLabel(id + langCode + RegistrationConstants.MESSAGE, null,
				styleClass, isVisible, 0);
		validationMessage.setWrapText(false);
		vBox.getChildren().add(validationMessage);

		HBox validationHBox = new HBox();
		validationHBox.setSpacing(20);
		validationHBox.getChildren().add(validationMessage);
		validationHBox.setStyle("-fx-background-color:WHITE");
		vBox.getChildren().add(validationHBox);
	}

	protected Node getField(String id) {
		return node.lookup(RegistrationConstants.HASH + id);
	}

	protected FxControl getFxControl(String fieldId) {
		return GenericController.getFxControlMap().get(fieldId);
	}

	public UiFieldDTO getUiSchemaDTO() {
		return uiFieldDTO;
	}

	public Node getNode() {
		return this.node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

}
