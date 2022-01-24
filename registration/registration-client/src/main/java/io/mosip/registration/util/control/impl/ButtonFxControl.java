package io.mosip.registration.util.control.impl;

import java.util.*;

import io.mosip.registration.controller.ClientApplication;
import javafx.scene.control.Tooltip;
import org.springframework.context.ApplicationContext;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.control.FxControl;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ButtonFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(ButtonFxControl.class);

	private FXUtils fxUtils;

	private io.mosip.registration.context.ApplicationContext regApplicationContext;

	private String selectedResidence = "genderSelectedButton";

	private String residence = "genderButton";

	private String buttonStyle = "button";

	private MasterSyncService masterSyncService;

	public ButtonFxControl() {
		ApplicationContext applicationContext = ClientApplication.getApplicationContext();
		fxUtils = FXUtils.getInstance();
		regApplicationContext = io.mosip.registration.context.ApplicationContext.getInstance();
		masterSyncService = applicationContext.getBean(MasterSyncService.class);
	}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;
		this.node = create(uiFieldDTO, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		Map<String, Object> data = new LinkedHashMap<>();
		String lang = getRegistrationDTo().getSelectedLanguagesByApplicant().get(0);
		data.put(lang, masterSyncService.getFieldValues(uiFieldDTO.getSubType(), lang, false));
		fillData(data);

		return this.control;
	}

	private VBox create(UiFieldDTO uiFieldDTO, String langCode) {
		String fieldName = uiFieldDTO.getId();

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();

		HBox hbox = new HBox();
		hbox.setId(fieldName + RegistrationConstants.HBOX);

		double prefWidth = simpleTypeVBox.getPrefWidth();

		/** Title label */
		Label fieldTitle = getLabel(fieldName + RegistrationConstants.LABEL, null, RegistrationConstants.BUTTONS_LABEL,
				true, prefWidth);
		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiFieldDTO.getLabel().get(lCode));
		});

		fieldTitle.setText(String.join(RegistrationConstants.SLASH, labels)	+ getMandatorySuffix(uiFieldDTO));
		hbox.getChildren().add(fieldTitle);
		simpleTypeVBox.getChildren().add(hbox);
		simpleTypeVBox.getChildren().add(getLabel(uiFieldDTO.getId() + RegistrationConstants.ERROR_MSG, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, simpleTypeVBox.getPrefWidth()));
		changeNodeOrientation(simpleTypeVBox, langCode);
		return simpleTypeVBox;
	}

	@Override
	public void setData(Object data) {
		HBox primaryHbox = (HBox) getField(uiFieldDTO.getId() + RegistrationConstants.HBOX);

		Button selectedButton = getSelectedButton(primaryHbox);

		if(selectedButton == null) {
			return;
		}

		String code = selectedButton.getId().replaceAll(uiFieldDTO.getId(), "");

		switch (this.uiFieldDTO.getType()) {
		case RegistrationConstants.SIMPLE_TYPE:
			List<SimpleDto> values = new ArrayList<SimpleDto>();
			List<String> toolTipText = new ArrayList<>();
			for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
				Optional<GenericDto> result = masterSyncService.getFieldValues(uiFieldDTO.getSubType(), langCode, false).stream()
						.filter(b -> b.getCode().equalsIgnoreCase(code)).findFirst();
				if (result.isPresent()) {
					values.add(new SimpleDto(langCode, result.get().getName()));
					toolTipText.add(result.get().getName());
				}
			}
			selectedButton.setTooltip(new Tooltip(String.join(RegistrationConstants.SLASH, toolTipText)));
			getRegistrationDTo().addDemographicField(uiFieldDTO.getId(), values);
			getRegistrationDTo().SELECTED_CODES.put(uiFieldDTO.getId()+"Code", code);
			break;
		default:
			Optional<GenericDto> result = masterSyncService.getFieldValues(uiFieldDTO.getSubType(), getRegistrationDTo().getSelectedLanguagesByApplicant().get(0), false).stream()
					.filter(b -> b.getCode().equalsIgnoreCase(code)).findFirst();
			if (result.isPresent()) {
				getRegistrationDTo().addDemographicField(uiFieldDTO.getId(), result.get().getName());
				getRegistrationDTo().SELECTED_CODES.put(uiFieldDTO.getId()+"Code", code);
			}
		}
	}

	private Button getSelectedButton(HBox hBox) {
		if (hBox != null) {
			for (Node node : hBox.getChildren()) {
				if (node instanceof Button) {
					Button button = (Button) node;
					if (button.getStyleClass().contains(selectedResidence)) {
						return button;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void fillData(Object data) {
		if (data != null) {
			Map<String, List<GenericDto>> val = (Map<String, List<GenericDto>>) data;
			setItems((HBox) getField(uiFieldDTO.getId() + RegistrationConstants.HBOX),
					val.get(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)));
		}
	}

	private void setItems(HBox hBox, List<GenericDto> val) {
		if (hBox != null && val != null && !val.isEmpty()) {
			val.forEach(genericDto -> {
				Button button = new Button(genericDto.getName());
				button.setId(uiFieldDTO.getId() + genericDto.getCode());
				hBox.setSpacing(10);
				hBox.setPadding(new Insets(10, 10, 10, 10));
				button.getStyleClass().addAll(residence, buttonStyle);
				hBox.getChildren().add(button);
				setListener(button);
			});

		}
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiFieldDTO.getId());
	}


	@Override
	public boolean isValid() {
		HBox primaryHbox = (HBox) getField(uiFieldDTO.getId() + RegistrationConstants.HBOX);
		Button selectedButton = getSelectedButton(primaryHbox);

		if(selectedButton != null)
			return true;

		return false;
	}

	@Override
	public boolean isEmpty() {
		HBox primaryHbox = (HBox) getField(uiFieldDTO.getId() + RegistrationConstants.HBOX);
		Button selectedButton = getSelectedButton(primaryHbox);
		return selectedButton == null ? true : false;
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}

	@Override
	public void setListener(Node node) {
		Button button = (Button) node;
		button.addEventHandler(ActionEvent.ACTION, event -> {
			resetButtons(button);
			if (isValid()) {
				setData(null);
				refreshFields();
			}
		});
	}

	private void resetButtons(Button button) {
		button.getStyleClass().clear();
		button.getStyleClass().addAll(selectedResidence, buttonStyle);
		button.getParent().getChildrenUnmodifiable().forEach(node -> {
			if (node instanceof Button && !node.getId().equals(button.getId())) {
				node.getStyleClass().clear();
				node.getStyleClass().addAll(residence, buttonStyle);
			}
		});
	}

	@Override
	public void selectAndSet(Object data) {
		HBox hbox = (HBox) getField(uiFieldDTO.getId() + RegistrationConstants.HBOX);
		if (data == null) {
			hbox.getChildrenUnmodifiable().forEach(node -> {
				if (node instanceof Button) {
					node.getStyleClass().clear();
					node.getStyleClass().addAll(residence, buttonStyle);
				}
			});
			return;
		}

		Optional<Node> selectedNode;

		if (data instanceof List) {
			List<SimpleDto> list = (List<SimpleDto>) data;
			selectedNode = hbox.getChildren().stream()
					.filter(node1 -> node1.getId().equals(uiFieldDTO.getId()+(list.isEmpty()? null : list.get(0).getValue())))
					.findFirst();
		}
		else {
			selectedNode = hbox.getChildren().stream()
					.filter(node1 -> node1.getId().equals(uiFieldDTO.getId()+(String)data))
					.findFirst();
		}

		if(selectedNode.isPresent()) {
			resetButtons((Button) selectedNode.get());
		}
	}

}
