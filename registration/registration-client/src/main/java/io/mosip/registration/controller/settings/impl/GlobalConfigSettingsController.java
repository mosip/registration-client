package io.mosip.registration.controller.settings.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.RestartController;
import io.mosip.registration.controller.settings.SettingsInterface;
import io.mosip.registration.controller.vo.GlobalParamVO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.config.GlobalParamService;
import io.mosip.registration.service.config.LocalConfigService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;

@Controller
public class GlobalConfigSettingsController extends BaseController implements SettingsInterface {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(GlobalConfigSettingsController.class);

	@FXML
	private ScrollPane contentPane;

	@FXML
	private Label headerLabel;

	@FXML
	private TableView<GlobalParamVO> globalParamTable;

	@FXML
	private TableColumn<GlobalParamVO, String> key;

	@FXML
	private TableColumn<GlobalParamVO, String> serverValue;

	@FXML
	private TableColumn<GlobalParamVO, String> localValue;
	
	@FXML
	private TextField filterField;

	@Autowired
	private GlobalParamService globalParamService;

	@Autowired
	private LocalConfigService localConfigService;
	
	@Autowired
	private RestartController restartController;

	private ObservableList<GlobalParamVO> observableList;

	private SortedList<GlobalParamVO> sortedList;

	private List<GlobalParamVO> globalParamList = new ArrayList<>();

	private Map<String, String> modifiedProperties = new HashMap<>();

	@Override
	public void setHeaderLabel(String headerLabel) {
		LOGGER.info("Setting header label as {}", headerLabel);

		this.headerLabel.setText(headerLabel);
		setContent();
	}

	@Override
	public HBox getShortCut(String shortcutIcon) {
		try {
			HBox shortCutHbox = new HBox();
			shortCutHbox.setAlignment(Pos.CENTER);
			ImageView imageView = new ImageView(getImage(shortcutIcon, true));
			imageView.setFitHeight(24);
			imageView.setFitWidth(24);
			shortCutHbox.getChildren().add(imageView);
			return shortCutHbox;
		} catch (RegBaseCheckedException e) {
			LOGGER.error("Exception while getting image");
		}
		return null;
	}

	private void setContent() {
		try {
			modifiedProperties.clear();
			if (observableList != null) {
				observableList.clear();
			}
			if (sortedList != null) {
				sortedList.clear();
			}
			loadInitialData();
		} catch (Exception exception) {
			LOGGER.error(String.format("%s -> Exception while Opening Settings page  %s -> %s",
					RegistrationConstants.USER_REG_SCAN_EXP, exception.getMessage(),
					ExceptionUtils.getStackTrace(exception)));

			generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.getMessageLanguageSpecific("UNABLE_LOAD_SETTINGS_PAGE"));
		}
	}

	private void loadInitialData() {
		Map<String, Object> globalParams = globalParamService.getGlobalParams();
		Map<String, String> localPreferences = localConfigService.getLocalConfigurations();
		List<String> permittedConfigurations = localConfigService
				.getPermittedConfigurations(RegistrationConstants.PERMITTED_CONFIG_TYPE);
		globalParamList = convertToGlobalParamVO(globalParams, localPreferences);
		displayData(permittedConfigurations);
	}

	private List<GlobalParamVO> convertToGlobalParamVO(Map<String, Object> globalParams,
			Map<String, String> localPreferences) {
		List<GlobalParamVO> globalParamValues = new ArrayList<>();
		for (Entry<String, Object> entry : globalParams.entrySet()) {
			GlobalParamVO globalParamVO = new GlobalParamVO();
			globalParamVO.setKey(entry.getKey());
			globalParamVO.setServerValue(String.valueOf(entry.getValue()));
			globalParamVO
					.setLocalValue(localPreferences.containsKey(entry.getKey()) ? localPreferences.get(entry.getKey())
							: RegistrationConstants.HYPHEN);
			globalParamValues.add(globalParamVO);
		}
		return globalParamValues;
	}

	private void displayData(List<String> permittedConfigurations) {
		LOGGER.info("Displaying all the ui data");

		key.setCellValueFactory(new PropertyValueFactory<>("key"));
		serverValue.setCellValueFactory(new PropertyValueFactory<>("serverValue"));
		localValue.setCellValueFactory(new PropertyValueFactory<>("localValue"));

		setCellFactoryForLocalValue(permittedConfigurations);

		observableList = FXCollections.observableArrayList(globalParamList);

		wrapListAndAddFiltering();

		// Wrap the ObservableList in a FilteredList (initially display all data).
		globalParamTable.setItems(sortedList);
		globalParamTable.setEditable(true);
	}
	
	private void setCellFactoryForLocalValue(List<String> permittedConfigurations) {
		localValue.setCellFactory(column -> {
			return new TableCell<GlobalParamVO, String>() {
				@Override
				public void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					setText(null);
					setGraphic(null);
					if (item != null) {
						if (getTableRow() != null && getTableRow().getItem() != null && permittedConfigurations.contains(getTableRow().getItem().getKey())) {
							TextField textField = new TextField();
							textField.setText(item);
							textField.getStyleClass().add("settingsTextField");
							textField.textProperty().addListener((observable, oldValue, newValue) -> {
								if (!newValue.isBlank() && !newValue.equals(RegistrationConstants.HYPHEN)
										&& !newValue.equals(getTableRow().getItem().getLocalValue())) {
									modifiedProperties.put(getTableRow().getItem().getKey(), textField.getText());
								} else if ((newValue.isBlank() || newValue.equals(RegistrationConstants.HYPHEN)
										|| newValue.equals(getTableRow().getItem().getLocalValue()))
										&& (modifiedProperties.containsKey(getTableRow().getItem().getKey()))) {
									modifiedProperties.remove(getTableRow().getItem().getKey());
								}
							});
							setGraphic(textField);
						} else {
							setText(item);
						}
					} else {
						setText(null);
						setGraphic(null);
					}
				}
			};
		});
	}

	private void wrapListAndAddFiltering() {
		FilteredList<GlobalParamVO> filteredList = new FilteredList<>(observableList, p -> true);

		// 2. Set the filter Predicate whenever the filter changes.
		filterField.textProperty().addListener((observable, oldValue, newValue) -> {
			filterData(newValue, filteredList);
		});

		if (!filterField.getText().isEmpty()) {
			filterData(filterField.getText(), filteredList);
		}

		// 3. Wrap the FilteredList in a SortedList.
		sortedList = new SortedList<>(filteredList);

		// 4. Bind the SortedList comparator to the TableView comparator.
		sortedList.comparatorProperty().bind(globalParamTable.comparatorProperty());
	}

	private void filterData(String newValue, FilteredList<GlobalParamVO> filteredList) {
		filteredList.setPredicate(globalParam -> {
			// If filter text is empty, display all ID's.
			if (newValue == null || newValue.isEmpty()) {
				return true;
			}

			// Compare every ID with filter text.
			String lowerCaseFilter = newValue.toLowerCase();

			if (globalParam.getKey().contains(lowerCaseFilter)) {
				globalParamTable.getSelectionModel().selectFirst();
				return true; // Filter matches key.
			} 
			return false; // Does not match.
		});
		globalParamTable.getSelectionModel().selectFirst();
	}

	public void submitPreferences() {
		if (!modifiedProperties.isEmpty()) {
			localConfigService.modifyConfigurations(modifiedProperties);
			setContent();
			if (configUpdateAlert()) {
				restartController.restart();
			}
		}
	}
	
	private boolean configUpdateAlert() {
		if (!fXComponents.getScene().getRoot().getId().equals("mainBox") && !SessionContext.map()
				.get(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ).equals(RegistrationConstants.ENABLE)) {

			Alert alert = createAlert(AlertType.CONFIRMATION, RegistrationUIConstants.INFORMATION,
					RegistrationUIConstants.getMessageLanguageSpecific("ALERT_NOTE_LABEL"), RegistrationUIConstants.getMessageLanguageSpecific("LOCAL_PREFERENCES_SAVED"),
					RegistrationConstants.QUIT_NOW, RegistrationConstants.QUIT_LATER);

			alert.show();
			Rectangle2D screenSize = Screen.getPrimary().getVisualBounds();
			Double xValue = screenSize.getWidth() / 2 - alert.getWidth() + 250;
			Double yValue = screenSize.getHeight() / 2 - alert.getHeight();
			alert.hide();
			alert.setX(xValue);
			alert.setY(yValue);
			alert.showAndWait();
			/* Get Option from user */
			ButtonType result = alert.getResult();
			if (result == ButtonType.OK) {
				return true;
			} else {
				return false;
			}
		}
		return true;
	}
}
