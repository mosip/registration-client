package io.mosip.registration.controller;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_SCAN_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_SELECT_LANGUAGE;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.reg.HeaderController;
import io.mosip.registration.controller.settings.impl.DeviceSettingsController;
import io.mosip.registration.controller.settings.impl.GlobalConfigSettingsController;
import io.mosip.registration.controller.settings.impl.ScheduledJobsSettingsController;
import io.mosip.registration.dto.schema.SettingsSchema;
import io.mosip.registration.exception.RegBaseCheckedException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Controller
public class SettingsController extends BaseController {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(SettingsController.class);

	@FXML
	private GridPane parentGridPane;

	@Autowired
	private HeaderController headerController;

	@Autowired
	private ScheduledJobsSettingsController scheduledJobsSettingsController;

	@Autowired
	private GlobalConfigSettingsController globalConfigSettingsController;

	@Autowired
	private DeviceSettingsController deviceSettingsController;
	
	@Autowired
	private GenericController genericController;

	private Stage popupStage;

	public void init(List<SettingsSchema> settingsByRole) {
		try {
			LOGGER.info("Opening pop-up screen to show Settings Page");
			
			if (genericController.getKeyboardStage() != null && genericController.getKeyboardStage().isShowing()) {
				genericController.getKeyboardStage().close();
			}

			popupStage = new Stage();
			popupStage.initStyle(StageStyle.UNDECORATED);

			LOGGER.info(LOG_SELECT_LANGUAGE, APPLICATION_NAME, APPLICATION_ID, "loading Settings.fxml");
			Parent scanPopup = BaseController.load(getClass().getResource(RegistrationConstants.SETTINGS_PAGE));

			popupStage.setResizable(false);
			Scene scene = new Scene(scanPopup);
			scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());

			setContent(settingsByRole);

			popupStage.setScene(scene);
			popupStage.initModality(Modality.WINDOW_MODAL);
			popupStage.initOwner(fXComponents.getStage());
			popupStage.show();

			LOGGER.info("Settings screen launched");
		} catch (IOException | RegBaseCheckedException | RuntimeException exception) {
			LOGGER.error(String.format("%s -> Exception while Opening pop-up screen to open Settings page  %s -> %s",
					RegistrationConstants.USER_REG_SCAN_EXP, exception.getMessage(),
					ExceptionUtils.getStackTrace(exception)));

			getStage().getScene().getRoot().setDisable(false);
			generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.getMessageLanguageSpecific("UNABLE_LOAD_SETTINGS_PAGE"));
		}
	}

	public void exitWindow() {
		LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Calling exit window to close the settings popup");

		popupStage.close();
		parentGridPane.getChildren().clear();
		getStage().getScene().getRoot().setDisable(false);

		LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Settings popup is closed");
	}

	private void setContent(List<SettingsSchema> settingsByRole) throws RegBaseCheckedException {
		if (settingsByRole != null && !settingsByRole.isEmpty()) {
			GridPane gridPane = createGridPane(settingsByRole.size());
			addContentToGridPane(gridPane, settingsByRole);
			parentGridPane.add(gridPane, 1, 1);
		}
	}

	private void addContentToGridPane(GridPane gridPane, List<SettingsSchema> settings) throws RegBaseCheckedException {
		int rowIndex = 0;
		int columnIndex = 0;
		for (SettingsSchema schema : settings) {
			VBox vbox = new VBox();
			vbox.setAlignment(Pos.TOP_CENTER);
			vbox.setId(schema.getName());

			Label label = new Label(schema.getLabel().get(ApplicationContext.applicationLanguage()));
			label.getStyleClass().add("settingsLabel");
			label.setWrapText(true);

			HBox hBox = new HBox();
			hBox.setAlignment(Pos.TOP_CENTER);
			hBox.setId(schema.getName());

			HBox imageHBox = new HBox();
			ImageView imageView = new ImageView(getImage(schema.getIcon(), true));
			imageView.setFitWidth(45);
			imageView.setFitHeight(45);
			imageHBox.setOnMouseClicked(event -> {
				loadFXML(schema.getFxml(), label.getText());
			});
			imageHBox.getChildren().add(imageView);
			hBox.getChildren().add(imageHBox);

			if (schema.getName().equalsIgnoreCase(RegistrationConstants.DEVICE_SETTINGS_NAME)) {
				ImageView shortCutIcon = new ImageView(getImage(RegistrationConstants.SHORTCUT_ICON, true));
				shortCutIcon.setFitWidth(20);
				shortCutIcon.setFitHeight(20);
				shortCutIcon.setOnMouseClicked(event -> {
					createShortCut(schema);
				});

				Tooltip tooltip = new Tooltip(
						applicationContext.getApplicationLanguageLabelBundle().getString("addShortcut"));
				Tooltip.install(shortCutIcon, tooltip);
				hBox.getChildren().add(shortCutIcon);
			}

			vbox.getChildren().addAll(hBox, label);

			gridPane.add(vbox, columnIndex, rowIndex);
			rowIndex = (columnIndex == 3) ? (rowIndex + 1) : rowIndex;
			columnIndex = (columnIndex == 3) ? 0 : (columnIndex + 1);
		}
	}

	public void createShortCut(SettingsSchema schema) {
		String controllerName = schema.getFxml().replace(".fxml", "Controller");
		HBox shortCutHBox = getShortCut(controllerName, schema.getShortcutIcon());
		shortCutHBox.setId(schema.getName());
		headerController.addShortCut(shortCutHBox);
	}

	private HBox getShortCut(String controllerName, String shortcutIcon) {
		switch (controllerName) {
		case "ScheduledJobsSettingsController":
			return scheduledJobsSettingsController.getShortCut(shortcutIcon);
		case "GlobalConfigSettingsController":
			return globalConfigSettingsController.getShortCut(shortcutIcon);
		case "DeviceSettingsController":
			return deviceSettingsController.getShortCut(shortcutIcon);
		default:
			return null;
		}
	}

	private void loadFXML(String fxmlName, String headerLabel) {
		LOGGER.info("Loading {} screen started.", fxmlName);
		try {
			exitWindow();
			FXMLLoader fxmlLoader = BaseController
					.loadChild(getClass().getResource(RegistrationConstants.FXML_PATH.concat(fxmlName)));
			Parent root = fxmlLoader.load();
			setHeader(fxmlLoader.getController().getClass().getSimpleName(), headerLabel);
			getScene(root);
		} catch (IOException ioException) {
			LOGGER.error("Exception in loading settings", ioException);
		}
		LOGGER.info("Loading {} screen ended.", fxmlName);
	}

	private void setHeader(String className, String headerLabel) {
		switch (className) {
		case "ScheduledJobsSettingsController":
			scheduledJobsSettingsController.setHeaderLabel(headerLabel);
			break;
		case "GlobalConfigSettingsController":
			globalConfigSettingsController.setHeaderLabel(headerLabel);
			break;
		case "DeviceSettingsController":
			deviceSettingsController.setHeaderLabel(headerLabel);
			break;
		}
	}

	private GridPane createGridPane(int size) {
		GridPane gridPane = new GridPane();
		gridPane.setHgap(10);
		gridPane.setVgap(10);

		if (size <= 4) {
			RowConstraints rowConstraint = new RowConstraints();
			rowConstraint.setPercentHeight(25);
			gridPane.getRowConstraints().add(rowConstraint);
		} else {
			int ceilOfSize = (size % 4 == 0) ? size : (size + (4 - size % 4));
			for (int index = 1; index <= ceilOfSize / 4; index++) {
				RowConstraints rowConstraint = new RowConstraints();
				rowConstraint.setPercentHeight(25);
				gridPane.getRowConstraints().add(rowConstraint);
			}
		}

		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(25);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(25);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(25);
		ColumnConstraints columnConstraint4 = new ColumnConstraints();
		columnConstraint4.setPercentWidth(25);

		gridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2, columnConstraint3,
				columnConstraint4);

		return gridPane;
	}

}
