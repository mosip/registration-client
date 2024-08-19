package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_UIN_UPDATE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.GenericController;
import io.mosip.registration.dto.schema.ProcessSpecDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;

/**
 * UpdateUINController Class.
 * 
 * @author Mahesh Kumar
 *
 */
@Controller
public class UpdateUINController extends BaseController implements Initializable {

	private static final Logger LOGGER = AppConfig.getLogger(UpdateUINController.class);

	@Autowired
	private RegistrationController registrationController;

	@FXML
	private TextField uinId;

	@FXML
	private Button backBtn;
	@FXML
	private ImageView backImageView;
	@FXML
	private ImageView continueImageView;

	@Autowired
	private UinValidator<String> uinValidatorImpl;

	@Autowired
	Validations validation;

	@Autowired
	private GenericController genericController;

	@FXML
	FlowPane parentFlowPane;
	
	@FXML
	private HBox demographicHBox;
	
	@FXML
	private ScrollPane scrollPane;	

	private ObservableList<Node> parentFlow;

	private HashMap<String, Object> checkBoxKeeper;

	private Map<String, List<UiFieldDTO>> groupedMap;

	private Map<String, List<String>> groupedProcess;
	private Map<String, Map<String, String>> groupLabels;

	private FXUtils fxUtils;
	@FXML
	private RadioButton renewCheckbox;

	@FXML
	private RadioButton updateCheckbox;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize(java.net.URL,
	 * java.util.ResourceBundle)
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		setImage(backImageView, RegistrationConstants.ARROW_LEFT_IMG);
		setImage(continueImageView, RegistrationConstants.ARROW_RIGHT_IMG);
		
		fxUtils = FXUtils.getInstance();
		checkBoxKeeper = new HashMap<>();

		groupedMap = new HashMap<>();
		groupLabels = new HashMap<>();
		groupedProcess = new HashMap<>();
		ProcessSpecDto processSpecDto = getProcessSpec(getRegistrationDTOFromSession().getProcessId(), getRegistrationDTOFromSession().getIdSchemaVersion());
		processSpecDto.getScreens().forEach(screen -> {
			screen.getFields().forEach(field -> {
				if(field.getGroup() != null) {
					List<UiFieldDTO> fields = groupedMap.getOrDefault(field.getGroup(), new ArrayList<>());
					fields.add(field);
					groupedMap.put(field.getGroup(), fields);

					if(field.getGroupProcess() != null)
						for(String process : field.getGroupProcess())
							if(groupedProcess.containsKey(process)) {
								List<String> groups = groupedProcess.get(process);
								if(!groups.contains(field.getGroup()))
									groups.add(field.getGroup());
								groupedProcess.put(process, groups);
							} else {
								List<String> groups = new ArrayList<>();
								groups.add(field.getGroup());
								groupedProcess.put(process, groups);
							}

					if(field.getGroupLabel() != null) {
						groupLabels.put(field.getGroup(), field.getGroupLabel());
					}
				}
			});
		});

		renewCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				getRegistrationDTOFromSession().setPreRegType("RENEWAL");

				for(Object object : checkBoxKeeper.values()) {
					CheckBox checkBox = (CheckBox) object;
					checkBox.setSelected(false);
					checkBox.setDisable(true);
				}

				if(newValue) {
					String process = renewCheckbox.getId();
					for(String group : groupedProcess.get(process)){
						((CheckBox)checkBoxKeeper.get(group)).setSelected(true);
						((CheckBox)checkBoxKeeper.get(group)).setDisable(false);
					}
				}
			}
		});

		updateCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				getRegistrationDTOFromSession().setPreRegType("UPDATE");
				for(Object object : checkBoxKeeper.values()) {
					CheckBox checkBox = (CheckBox) object;
					checkBox.setSelected(false);
					checkBox.setDisable(true);
				}

				if(newValue) {
					String process = updateCheckbox.getId();
					for(String group : groupedProcess.get(process)){
						((CheckBox)checkBoxKeeper.get(group)).setSelected(true);
						((CheckBox)checkBoxKeeper.get(group)).setDisable(false);
					}
				}
			}
		});


		scrollPane.prefWidthProperty().bind(demographicHBox.widthProperty());
		
		parentFlow = parentFlowPane.getChildren();
		groupedMap.forEach((groupName, list) -> {
			GridPane checkBox = addCheckBox(groupName);
			if (checkBox != null) {
				parentFlow.add(checkBox);
			}
		});

		try {
			
			backBtn.hoverProperty().addListener((ov, oldValue, newValue) -> {
				if (newValue) {
					setImage(backImageView, RegistrationConstants.BACK_FOCUSED_IMG);
				} else {
					setImage(backImageView, RegistrationConstants.ARROW_LEFT_IMG);
				}
			});
		} catch (Throwable runtimeException) {
			LOGGER.error(LOG_REG_UIN_UPDATE, APPLICATION_NAME, APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}

	private GridPane addCheckBox(String groupName) {
		String groupLabel = getLabelsForGroup(groupName, groupLabels.get(groupName));
		CheckBox checkBox = new CheckBox(groupLabel);
		checkBox.setId(groupName);
		checkBox.setTooltip(new Tooltip(groupLabel));
		checkBox.getStyleClass().add(RegistrationConstants.updateUinCheckBox);
		fxUtils.listenOnSelectedCheckBox(checkBox);
		checkBox.setDisable(true);
		checkBoxKeeper.put(groupName, checkBox);
		
		GridPane gridPane = new GridPane();
		gridPane.setPrefWidth(400);
		gridPane.setPrefHeight(40);

		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(10);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(85);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(5);
		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);

		ObservableList<RowConstraints> rowConstraints = gridPane.getRowConstraints();
		RowConstraints rowConstraint1 = new RowConstraints();
		columnConstraint1.setPercentWidth(20);
		RowConstraints rowConstraint2 = new RowConstraints();
		columnConstraint1.setPercentWidth(60);
		RowConstraints rowConstraint3 = new RowConstraints();
		columnConstraint1.setPercentWidth(20);
		rowConstraints.addAll(rowConstraint1, rowConstraint2, rowConstraint3);

		gridPane.add(checkBox, 1, 1);

		return gridPane;
	}

	private String getLabelsForGroup(String groupName, Map<String, String> labels) {
		List<String> groupLabel = new ArrayList<>();
		if (labels != null && !labels.isEmpty()) {
			registrationController.getSelectedLangList().forEach(lang -> {
				if (labels.containsKey(lang)) {
					groupLabel.add(labels.get(lang));
				}
			});
		}
		return groupLabel.isEmpty() ? groupName : String.join(RegistrationConstants.SLASH, groupLabel);
	}

	/**
	 * Submitting for UIN update after selecting the required fields.
	 *
	 * @param event the event
	 */
	@FXML
	public void submitUINUpdate(ActionEvent event) {
		LOGGER.info(LOG_REG_UIN_UPDATE, APPLICATION_NAME, APPLICATION_ID, "Updating UIN details");
		try {
			if (StringUtils.isEmpty(uinId.getText())) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UPDATE_UIN_ENTER_UIN_ALERT));
				return;
			}

			List<String> selectedFieldGroups = new ArrayList<String>();
			for (String key : checkBoxKeeper.keySet()) {
				if (((CheckBox) checkBoxKeeper.get(key)).isSelected()) {
					selectedFieldGroups.add(key);
				}
			}

			if(selectedFieldGroups.isEmpty()) {
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UPDATE_UIN_SELECTION_ALERT));
				return;
			}

			if (uinValidatorImpl.validateId(uinId.getText()) && !selectedFieldGroups.isEmpty()) {
				getRegistrationDTOFromSession().addDemographicField("UIN", uinId.getText());
				getRegistrationDTOFromSession().setUpdatableFieldGroups(selectedFieldGroups);
				getRegistrationDTOFromSession().setUpdatableFields(new ArrayList<>());
				getRegistrationDTOFromSession().setBiometricMarkedForUpdate(selectedFieldGroups.contains(RegistrationConstants.BIOMETRICS_GROUP) ? true : false);

				Parent createRoot = BaseController.load(
						getClass().getResource(RegistrationConstants.CREATE_PACKET_PAGE),
						applicationContext.getBundle(getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().get(0), RegistrationConstants.LABELS));

				getScene(createRoot).setRoot(createRoot);
				genericController.populateScreens();
				return;
			}
		} catch (InvalidIDException invalidIdException) {
			LOGGER.error(invalidIdException.getMessage(), invalidIdException);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UPDATE_UIN_VALIDATION_ALERT));
		} catch (Throwable exception) {
			LOGGER.error(exception.getMessage(), exception);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_REG_PAGE));
		}
	}
}
