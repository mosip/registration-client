package io.mosip.registration.util.control.impl;

import java.util.*;
import java.util.Map.Entry;

import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.dao.MasterSyncDao;
import javafx.geometry.Insets;
import org.springframework.context.ApplicationContext;

import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.GenericController;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.entity.Location;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.common.ComboBoxAutoComplete;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.util.Assert;

public class DropDownFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DropDownFxControl.class);
	private static final String loggerClassName = "DropDownFxControl";
	private int hierarchyLevel;
	private Validations validation;
	private DemographicChangeActionHandler demographicChangeActionHandler;
	private MasterSyncService masterSyncService;
	private MasterSyncDao masterSyncDao;

	public DropDownFxControl() {
		ApplicationContext applicationContext = ClientApplication.getApplicationContext();
		validation = applicationContext.getBean(Validations.class);
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);
		masterSyncService = applicationContext.getBean(MasterSyncService.class);
		masterSyncDao  = applicationContext.getBean(MasterSyncDao.class);
	}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;
		this.node = create(uiFieldDTO, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		//As subType in UI Spec is defined in any lang we find the langCode to fill initial dropdown
		String subTypeLangCode = getSubTypeLangCode(uiFieldDTO.getSubType());
		if(subTypeLangCode != null) {
			TreeMap<Integer, String> groupFields = GenericController.currentHierarchyMap.getOrDefault(uiFieldDTO.getGroup(), new TreeMap<>());
			for (Entry<Integer, String> entry : GenericController.hierarchyLevels.get(subTypeLangCode).entrySet()) {
				if (entry.getValue().equals(uiFieldDTO.getSubType())) {
					this.hierarchyLevel = entry.getKey();
					groupFields.put(entry.getKey(), uiFieldDTO.getId());
					GenericController.currentHierarchyMap.put(uiFieldDTO.getGroup(), groupFields);
					break;
				}
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0),
				getPossibleValues(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)));

		//clears & refills items
		fillData(data);
		return this.control;
	}

	private String getSubTypeLangCode(String subType) {
		for( String langCode : GenericController.hierarchyLevels.keySet()) {
			if(GenericController.hierarchyLevels.get(langCode).containsValue(subType))
				return langCode;
		}
		return null;
	}

	private VBox create(UiFieldDTO uiFieldDTO, String langCode) {
		String fieldName = uiFieldDTO.getId();

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setSpacing(5);
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setMinWidth(450);

		/** Title label */
		Label fieldTitle = getLabel(uiFieldDTO.getId() + RegistrationConstants.LABEL, "",
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, simpleTypeVBox.getWidth());
		simpleTypeVBox.getChildren().add(fieldTitle);

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiFieldDTO.getLabel().get(lCode));
		});

		String titleText = String.join(RegistrationConstants.SLASH, labels) + getMandatorySuffix(uiFieldDTO);
		ComboBox<GenericDto> comboBox = getComboBox(fieldName, titleText, RegistrationConstants.DOC_COMBO_BOX,
				simpleTypeVBox.getPrefWidth(), false);
		comboBox.setMaxWidth(Double.MAX_VALUE);
		simpleTypeVBox.getChildren().add(comboBox);
		simpleTypeVBox.setMargin(comboBox, new Insets(0, 30, 0, 0));

		comboBox.setOnMouseExited(event -> {
			getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE).setVisible(false);
			if(comboBox.getTooltip()!=null) {
				comboBox.getTooltip().hide();
			}
		});

		comboBox.setOnMouseEntered((event -> {
			getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE).setVisible(true);

		}));

		setListener(comboBox);

		fieldTitle.setText(titleText);
		Label messageLabel = getLabel(uiFieldDTO.getId() + RegistrationConstants.MESSAGE, null,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, simpleTypeVBox.getPrefWidth());
		messageLabel.setMaxWidth(200);
		simpleTypeVBox.getChildren().add(messageLabel);

		changeNodeOrientation(simpleTypeVBox, langCode);

		return simpleTypeVBox;
	}


	public List<GenericDto> getPossibleValues(String langCode) {
		boolean isHierarchical = false;
		String fieldSubType = uiFieldDTO.getSubType();

		if(GenericController.currentHierarchyMap.containsKey(uiFieldDTO.getGroup())) {
			isHierarchical = true;
			Entry<Integer, String> parentEntry = GenericController.currentHierarchyMap.get(uiFieldDTO.getGroup())
					.lowerEntry(this.hierarchyLevel);
			if(parentEntry == null) { //first parent
				parentEntry = GenericController.hierarchyLevels.get(langCode).lowerEntry(this.hierarchyLevel);
				Assert.notNull(parentEntry);
				List<Location> locations = masterSyncDao.getLocationDetails(parentEntry.getValue(), langCode);
				fieldSubType = locations != null && !locations.isEmpty() ? locations.get(0).getCode() : null;
			}
			else {
				FxControl fxControl = GenericController.getFxControlMap().get(parentEntry.getValue());
				Node comboBox = getField(fxControl.getNode(), parentEntry.getValue());
				GenericDto selectedItem = comboBox != null ?
						((ComboBox<GenericDto>) comboBox).getSelectionModel().getSelectedItem() : null;
				fieldSubType = selectedItem != null ? selectedItem.getCode() : null;
				if(fieldSubType == null)
					return Collections.EMPTY_LIST;
			}
		}
		return masterSyncService.getFieldValues(fieldSubType, langCode, isHierarchical);
	}

	private <T> ComboBox<GenericDto> getComboBox(String id, String titleText, String stycleClass, double prefWidth,
												 boolean isDisable) {
		ComboBox<GenericDto> field = new ComboBox<GenericDto>();
		StringConverter<T> uiRenderForComboBox = FXUtils.getInstance().getStringConverterForComboBox();
		field.setId(id);
		// field.setPrefWidth(prefWidth);

		//field.setPromptText(titleText);
		field.setDisable(isDisable);
		field.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_COMBOBOX);
		field.setConverter((StringConverter<GenericDto>) uiRenderForComboBox);
		return field;
	}


	@Override
	public void setData(Object data) {
		ComboBox<GenericDto> appComboBox = (ComboBox<GenericDto>) getField(uiFieldDTO.getId());
		if(appComboBox.getSelectionModel().getSelectedItem() == null) {
			return;
		}

		String selectedCode = appComboBox.getSelectionModel().getSelectedItem().getCode();
		switch (this.uiFieldDTO.getType()) {
			case RegistrationConstants.SIMPLE_TYPE:
				List<SimpleDto> values = new ArrayList<SimpleDto>();
				for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
					Optional<GenericDto> result = getPossibleValues(langCode).stream()
							.filter(b -> b.getCode().equals(selectedCode)).findFirst();
					if (result.isPresent()) {
						SimpleDto simpleDto = new SimpleDto(langCode, result.get().getName());
						values.add(simpleDto);
					}
				}
				getRegistrationDTo().addDemographicField(uiFieldDTO.getId(), values);
				getRegistrationDTo().SELECTED_CODES.put(uiFieldDTO.getId()+"Code", selectedCode);
				break;
			default:
				Optional<GenericDto> result = getPossibleValues(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)).stream()
						.filter(b -> b.getCode().equals(selectedCode)).findFirst();
				if (result.isPresent()) {
					getRegistrationDTo().addDemographicField(uiFieldDTO.getId(), result.get().getName());
					getRegistrationDTo().SELECTED_CODES.put(uiFieldDTO.getId()+"Code", selectedCode);
				}
				break;
		}
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiFieldDTO.getId());
	}

	@Override
	public boolean isValid() {
		ComboBox<GenericDto> appComboBox = (ComboBox<GenericDto>) getField(uiFieldDTO.getId());
		boolean isValid = appComboBox != null && appComboBox.getSelectionModel().getSelectedItem() != null;
		if (appComboBox != null) {
			appComboBox.getStyleClass().removeIf((s) -> {
				return s.equals("demographicComboboxFocused");
			});
			if(!isValid) {
				appComboBox.getStyleClass().add("demographicComboboxFocused");
			}
		}
		return isValid;
	}

	@Override
	public boolean isEmpty() {
		ComboBox<GenericDto> appComboBox = (ComboBox<GenericDto>) getField(uiFieldDTO.getId());
		return appComboBox == null || appComboBox.getSelectionModel().getSelectedItem() == null;
	}

	@Override
	public void setListener(Node node) {
		ComboBox<GenericDto> fieldComboBox = (ComboBox<GenericDto>) node;
		fieldComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			displayFieldLabel();
			if (isValid()) {

				List<String> toolTipText = new ArrayList<>();
				String selectedCode = fieldComboBox.getSelectionModel().getSelectedItem().getCode();
				for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
					Optional<GenericDto> result = getPossibleValues(langCode).stream()
							.filter(b -> b.getCode().equals(selectedCode)).findFirst();
					if (result.isPresent()) {

						toolTipText.add(result.get().getName());
					}
				}

				Label messageLabel = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE);
				messageLabel.setText(String.join(RegistrationConstants.SLASH, toolTipText));

				setData(null);
				refreshNextHierarchicalFxControls();
				demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),	uiFieldDTO.getChangeAction());
				// Group level visibility listeners
				refreshFields();
			}
		});
	}

	private void refreshNextHierarchicalFxControls() {
		if(GenericController.currentHierarchyMap.containsKey(uiFieldDTO.getGroup())) {
			Entry<Integer, String> nextEntry = GenericController.currentHierarchyMap.get(uiFieldDTO.getGroup())
					.higherEntry(this.hierarchyLevel);

			while (nextEntry != null) {
				FxControl fxControl = GenericController.getFxControlMap().get(nextEntry.getValue());
				Map<String, Object> data = new LinkedHashMap<>();
				data.put(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0),
						fxControl.getPossibleValues(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)));

				//clears & refills items
				fxControl.fillData(data);
				nextEntry = GenericController.currentHierarchyMap.get(uiFieldDTO.getGroup())
						.higherEntry(nextEntry.getKey());
			}
		}
	}

	private void displayFieldLabel() {
		FXUtils.getInstance().toggleUIField((Pane) getNode(), uiFieldDTO.getId() + RegistrationConstants.LABEL,
				true);
		Label label = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.LABEL);
		label.getStyleClass().add("demoGraphicFieldLabelOnType");
		label.getStyleClass().remove("demoGraphicFieldLabel");
		FXUtils.getInstance().toggleUIField((Pane) getNode(), uiFieldDTO.getId() + RegistrationConstants.MESSAGE, false);
	}



	private Node getField(Node fieldParentNode, String id) {
		return fieldParentNode.lookup(RegistrationConstants.HASH + id);
	}

	@Override
	public void fillData(Object data) {

		if (data != null) {

			Map<String, List<GenericDto>> val = (Map<String, List<GenericDto>>) data;

			List<GenericDto> items = val.get(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

			if (items != null && !items.isEmpty()) {
				setItems((ComboBox<GenericDto>) getField(uiFieldDTO.getId()), items);
			}

		}
	}

	private void setItems(ComboBox<GenericDto> comboBox, List<GenericDto> val) {
		if (comboBox != null && val != null && !val.isEmpty()) {
			comboBox.getItems().clear();
			comboBox.getItems().addAll(val);

			new ComboBoxAutoComplete<GenericDto>(comboBox);

			comboBox.hide();

		}
	}

	@Override
	public void selectAndSet(Object data) {
		ComboBox<GenericDto> field = (ComboBox<GenericDto>) getField(uiFieldDTO.getId());
		if (data == null) {
			field.getSelectionModel().clearSelection();
			return;
		}

		if (data instanceof List) {

			List<SimpleDto> list = (List<SimpleDto>) data;

			selectItem(field, list.isEmpty() ? null : list.get(0).getValue());

		} else if (data instanceof String) {

			selectItem(field, (String) data);
		}
	}

	private void selectItem(ComboBox<GenericDto> field, String val) {
		if (field != null && val != null && !val.isEmpty()) {
			for (GenericDto genericDto : field.getItems()) {
				if (genericDto.getCode().equals(val)) {
					field.getSelectionModel().select(genericDto);
					break;
				}
			}
		}
	}
}
