package io.mosip.registration.controller.settings.impl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import io.mosip.registration.dto.DeviceType;
import io.mosip.registration.dto.ScanDevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.api.docscanner.DocScannerFacade;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.controller.settings.SettingsInterface;
import io.mosip.registration.dto.BiometricDeviceInfo;
import io.mosip.registration.dto.ScanDeviceInfo;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.mdm.dto.MdmBioDevice;
import io.mosip.registration.mdm.service.impl.MosipDeviceSpecificationFactory;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

@Controller
public class DeviceSettingsController extends BaseController implements SettingsInterface {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DeviceSettingsController.class);

	@FXML
	private Label headerLabel;

	@FXML
	private GridPane subContentGridPane;

	@FXML
	private ScrollPane contentPane;

	@FXML
	private TextField fromPort;

	@FXML
	private TextField toPort;

	@FXML
	private Button submit;

	@FXML
	private StackPane progressIndicatorPane;

	@FXML
	private ProgressIndicator progressIndicator;

	@Autowired
	private MosipDeviceSpecificationFactory mosipDeviceSpecificationFactory;

	@Autowired
	private DocScannerFacade docScannerFacade;

	@Autowired
	private DocumentScanController documentScanController;

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
			shortCutHbox.setAlignment(Pos.TOP_CENTER);

			HBox devicesHBox = new HBox();
			devicesHBox.setAlignment(Pos.CENTER);
			devicesHBox.setSpacing(10);
			devicesHBox.getStyleClass().add("deviceDetailsHBox");

			ImageView fpDeviceImage = new ImageView(
					getImage(getImageByDeviceStatus(RegistrationConstants.FINGERPRINT_DEVICE_KEY), true));
			fpDeviceImage.setFitHeight(35);
			fpDeviceImage.setFitWidth(35);

			ImageView irisDeviceImage = new ImageView(
					getImage(getImageByDeviceStatus(RegistrationConstants.IRIS_DEVICE_KEY), true));
			irisDeviceImage.setFitHeight(35);
			irisDeviceImage.setFitWidth(35);

			ImageView faceDeviceImage = new ImageView(
					getImage(getImageByDeviceStatus(RegistrationConstants.FACE_DEVICE_KEY), true));
			faceDeviceImage.setFitHeight(35);
			faceDeviceImage.setFitWidth(35);

			devicesHBox.getChildren().addAll(fpDeviceImage, irisDeviceImage, faceDeviceImage);

			ImageView refreshImage = new ImageView(getImage(RegistrationConstants.REFRESH_ICON, true));
			refreshImage.setFitHeight(15);
			refreshImage.setFitWidth(15);
			refreshImage.setOnMouseClicked(event -> {
				refreshDeviceStatus(fpDeviceImage, irisDeviceImage, faceDeviceImage);
			});
			Tooltip tooltip = new Tooltip(
					applicationContext.getApplicationLanguageLabelBundle().getString("refreshStatus"));
			Tooltip.install(refreshImage, tooltip);

			shortCutHbox.getChildren().addAll(devicesHBox, refreshImage);
			return shortCutHbox;
		} catch (RegBaseCheckedException e) {
			LOGGER.error("Exception while getting image");
		}
		return null;
	}

	private String getImageByDeviceStatus(String deviceType) {
		switch (deviceType.toLowerCase()) {
		case RegistrationConstants.FINGERPRINT_DEVICE_KEY:
			if (isDeviceAvailable(deviceType)) {
				return RegistrationConstants.FP_DEVICE_CONNECTED_IMG;
			} else {
				return RegistrationConstants.FP_DEVICE_DISCONNECTED_IMG;
			}
		case RegistrationConstants.IRIS_DEVICE_KEY:
			if (isDeviceAvailable(deviceType)) {
				return RegistrationConstants.IRIS_DEVICE_CONNECTED_IMG;
			} else {
				return RegistrationConstants.IRIS_DEVICE_DISCONNECTED_IMG;
			}
		case RegistrationConstants.FACE_DEVICE_KEY:
			if (isDeviceAvailable(deviceType)) {
				return RegistrationConstants.FACE_DEVICE_CONNECTED_IMG;
			} else {
				return RegistrationConstants.FACE_DEVICE_DISCONNECTED_IMG;
			}
		}
		return null;
	}

	private boolean isDeviceAvailable(String deviceType) {
		try {
			return mosipDeviceSpecificationFactory.isDeviceAvailable(deviceType);
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Exception in searching for device", exception);
			return false;
		}
	}

	private void refreshDeviceStatus(ImageView fpDeviceImage, ImageView irisDeviceImage, ImageView faceDeviceImage) {
		try {
			fpDeviceImage
					.setImage(getImage(getImageByDeviceStatus(RegistrationConstants.FINGERPRINT_DEVICE_KEY), true));
			irisDeviceImage.setImage(getImage(getImageByDeviceStatus(RegistrationConstants.IRIS_DEVICE_KEY), true));
			faceDeviceImage.setImage(getImage(getImageByDeviceStatus(RegistrationConstants.FACE_DEVICE_KEY), true));
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Exception in searching for device", exception);
		}
	}

	public void modifyPortRange() {
		if (validatePort(fromPort.getText()) && validatePort(toPort.getText())
				&& Integer.parseInt(toPort.getText()) > Integer.parseInt(fromPort.getText())) {
			ApplicationContext.setGlobalConfigValueOf(RegistrationConstants.MDM_START_PORT_RANGE, fromPort.getText());
			ApplicationContext.setGlobalConfigValueOf(RegistrationConstants.MDM_END_PORT_RANGE, toPort.getText());
			SessionContext.map().put(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ,
					RegistrationConstants.ENABLE);
			generateAlert(RegistrationConstants.ALERT_INFORMATION,
					RegistrationUIConstants.getMessageLanguageSpecific("PORT_RANGE_MODIFIED_SUCCESSFULLY"));
		} else {
			generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.getMessageLanguageSpecific("INVALID_PORT_RANGE"));
			fromPort.setText((String) ApplicationContext.map().get(RegistrationConstants.MDM_START_PORT_RANGE));
			toPort.setText((String) ApplicationContext.map().get(RegistrationConstants.MDM_END_PORT_RANGE));
		}
	}

	public void scanForDevices() {
		getStage().getScene().getRoot().setDisable(true);
		progressIndicatorPane.setVisible(true);

		Service<Boolean> taskService = new Service<Boolean>() {
			@Override
			protected Task<Boolean> createTask() {
				return new Task<Boolean>() {
					/*
					 * (non-Javadoc)
					 * 
					 * @see javafx.concurrent.Task#call()
					 */
					@Override
					protected Boolean call() {
						try {
							mosipDeviceSpecificationFactory.initializeDeviceMap(false);
							return true;
						} catch (Exception exception) {
							LOGGER.error("Exception while scanning for devices", exception);
							return false;
						}
					}
				};
			}
		};

		progressIndicator.progressProperty().bind(taskService.progressProperty());
		taskService.start();
		taskService.setOnSucceeded(event -> {
			if (taskService.getValue()) {
				contentPane.setContent(null);
				setContent();
			}
			getStage().getScene().getRoot().setDisable(false);
			progressIndicatorPane.setVisible(false);
			LOGGER.info("Device search completed");
		});
		taskService.setOnFailed(event -> {
			getStage().getScene().getRoot().setDisable(false);
			progressIndicatorPane.setVisible(false);
			LOGGER.info("Device search failed and stopped");
		});
	}

	private boolean validatePort(String text) {
		return text.matches(RegistrationConstants.PORT_RANGE_REGEX);
	}

	private void setContent() {
		try {
			int columnsCount = 0;
			fromPort.setText((String) ApplicationContext.map().get(RegistrationConstants.MDM_START_PORT_RANGE));
			toPort.setText((String) ApplicationContext.map().get(RegistrationConstants.MDM_END_PORT_RANGE));
			fromPort.textProperty().addListener((observable, oldValue, newValue) -> {
				SessionContext.map().put(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ,
						RegistrationConstants.DISABLE);
				submit.setVisible(true);
			});
			toPort.textProperty().addListener((observable, oldValue, newValue) -> {
				SessionContext.map().put(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ,
						RegistrationConstants.DISABLE);
				submit.setVisible(true);
			});
			Map<String, List<MdmBioDevice>> biometricDevices = MosipDeviceSpecificationFactory.getAvailableDeviceInfo();
			columnsCount = biometricDevices.size();
			List<ScanDevice> scannerDevices = docScannerFacade.getConnectedDevices();
			if (!scannerDevices.isEmpty()) {
				++columnsCount;
			}
			if (applicationContext.isPrimaryLanguageRightToLeft()) {
				subContentGridPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
			}
			GridPane gridPane = createGridPane(columnsCount);
			addContentToGridPane(gridPane, biometricDevices, scannerDevices);
			contentPane.setContent(gridPane);
			
			SessionContext.map().put(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ,
					RegistrationConstants.ENABLE);
		} catch (RegBaseCheckedException exception) {
			LOGGER.error(String.format("%s -> Exception while Opening Settings page  %s -> %s",
					RegistrationConstants.USER_REG_SCAN_EXP, exception.getMessage(),
					ExceptionUtils.getStackTrace(exception)));

			generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.getMessageLanguageSpecific("UNABLE_LOAD_SETTINGS_PAGE"));
		}
	}

	private GridPane createGridPane(int size) {
		GridPane gridPane = new GridPane();
		gridPane.setAlignment(Pos.TOP_CENTER);
		gridPane.setHgap(35);
		gridPane.setVgap(35);

		if (size <= 2) {
			RowConstraints rowConstraint = new RowConstraints();
			rowConstraint.setPercentHeight(25);
			gridPane.getRowConstraints().add(rowConstraint);
		} else {
			int ceilOfSize = (size % 2 == 0) ? size : (size + (2 - size % 2));
			for (int index = 1; index <= ceilOfSize / 2; index++) {
				RowConstraints rowConstraint = new RowConstraints();
				rowConstraint.setPercentHeight(25);
				gridPane.getRowConstraints().add(rowConstraint);
			}
		}

		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(50);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(50);

		gridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2);

		return gridPane;
	}

	private void addContentToGridPane(GridPane gridPane, Map<String, List<MdmBioDevice>> biometricDevices,
			List<ScanDevice> scannerDevices) throws RegBaseCheckedException {
		int rowIndex = 0;
		int columnIndex = 0;
		for (Entry<String, List<MdmBioDevice>> entry : biometricDevices.entrySet()) {
			GridPane mainGridPane = createDevicePane("biometricDevice", entry.getKey(), entry.getValue(), null);
			if (applicationContext.isPrimaryLanguageRightToLeft()) {
				mainGridPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
			}
			gridPane.add(mainGridPane, columnIndex, rowIndex);
			rowIndex = (columnIndex == 1) ? (rowIndex + 1) : rowIndex;
			columnIndex = (columnIndex == 1) ? 0 : (columnIndex + 1);
		}
		if (!scannerDevices.isEmpty()) {
			GridPane mainGridPane = createDevicePane("scannerDevice", "scanner", null, scannerDevices);
			if (applicationContext.isPrimaryLanguageRightToLeft()) {
				mainGridPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
			}
			gridPane.add(mainGridPane, columnIndex, rowIndex);
			rowIndex = (columnIndex == 1) ? (rowIndex + 1) : rowIndex;
			columnIndex = (columnIndex == 1) ? 0 : (columnIndex + 1);
		}
	}

	private GridPane createDevicePane(String type, String key, List<MdmBioDevice> bioDevices,
			List<ScanDevice> scannerDevices) throws RegBaseCheckedException {
		GridPane mainGridPane = new GridPane();
		mainGridPane.getStyleClass().add(RegistrationConstants.SYNC_JOB_STYLE);

		RowConstraints rowConstraint = new RowConstraints();
		rowConstraint.setPercentHeight(100);
		mainGridPane.getRowConstraints().add(rowConstraint);

		ColumnConstraints columnConstraint = new ColumnConstraints();
		columnConstraint.setPercentWidth(100);
		mainGridPane.getColumnConstraints().addAll(columnConstraint);

		GridPane subGridPane = new GridPane();

		subGridPane.getRowConstraints().add(rowConstraint);

		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(20);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(75);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(5);
		subGridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2, columnConstraint3);

		VBox imageVbox = new VBox();
		imageVbox.setAlignment(Pos.CENTER);

		ImageView imageView = new ImageView(getImage(getImageByDeviceType(key), true));
		imageView.setFitWidth(75);
		imageView.setFitHeight(75);

		imageVbox.getChildren().add(imageView);

		subGridPane.add(imageVbox, 0, 0);

		VBox deviceDetailsVbox = new VBox();
		deviceDetailsVbox.setAlignment(Pos.CENTER_LEFT);

		if (type.equalsIgnoreCase("biometricDevice")) {
			addDeviceDetails(deviceDetailsVbox, key, bioDevices);
		} else {
			addScannerDetails(deviceDetailsVbox, scannerDevices);
		}

		subGridPane.add(deviceDetailsVbox, 1, 0);

		mainGridPane.add(subGridPane, 0, 0);

		return mainGridPane;
	}

	private void addDeviceDetails(VBox deviceDetailsVbox, String key, List<MdmBioDevice> devices) {
		if (devices.size() > 1) {
			ComboBox<BiometricDeviceInfo> comboBox = new ComboBox<>();
			comboBox.getStyleClass().add("deviceDetailsComboBox");

			for (MdmBioDevice device : devices) {
				comboBox.getItems().add(convertToBiometricDeviceInfo(key, device));
			}
			comboBox.getSelectionModel().select(getSelectedDeviceInfo(key));
			// Create our custom cells for the ComboBox
			comboBox.setCellFactory(param -> new ListCell<BiometricDeviceInfo>() {
				@Override
				protected void updateItem(BiometricDeviceInfo item, boolean empty) {
					super.updateItem(item, empty);
					// Create a Label to store our text. We'll set it to wrap text and it's preferred width
					Label label = new Label();
					label.setWrapText(true);
					label.setPrefWidth(280);

					if (item == null || empty) {
						setGraphic(null);
					} else {
						// Add our text to the Label
						label.setText(item.toString());
						setGraphic(label);
						mosipDeviceSpecificationFactory.modifySelectedDeviceInfo(item.getDeviceType(),
								item.getSerialNumber());
					}
				}
			});
			deviceDetailsVbox.getChildren().add(comboBox);
		} else {
			Label deviceDetailsLabel = new Label(convertToBiometricDeviceInfo(key, devices.get(0)).toString());
			deviceDetailsLabel.getStyleClass().add("deviceDetailsLabel");
			deviceDetailsLabel.setWrapText(true);

			deviceDetailsVbox.getChildren().add(deviceDetailsLabel);
		}
	}

	private BiometricDeviceInfo getSelectedDeviceInfo(String key) {
		MdmBioDevice selectedBioDevice = MosipDeviceSpecificationFactory.getDeviceRegistryInfo().get(key);
		return convertToBiometricDeviceInfo(key, selectedBioDevice);
	}

	private BiometricDeviceInfo convertToBiometricDeviceInfo(String key, MdmBioDevice device) {
		BiometricDeviceInfo deviceInfo = new BiometricDeviceInfo();
		deviceInfo.setDeviceType(key);
		deviceInfo.setSerialNumber(device.getSerialNumber());
		deviceInfo.setMake(device.getDeviceMake());
		deviceInfo.setModel(device.getDeviceModel());
		return deviceInfo;
	}

	private String getImageByDeviceType(String key) {
		if (key.toLowerCase().contains(SingleType.FINGER.value().toLowerCase())) {
			return RegistrationConstants.FINGERPRINT_DEVICE_IMG;
		} else if (key.toLowerCase().contains(SingleType.IRIS.value().toLowerCase())) {
			return RegistrationConstants.IRIS_DEVICE_IMG;
		} else if (key.toLowerCase().contains(SingleType.FACE.value().toLowerCase())) {
			return RegistrationConstants.FACE_DEVICE_IMG;
		} else if (key.equalsIgnoreCase("scanner")) {
			return RegistrationConstants.DOC_SCANNER_DEVICE;
		}
		return null;
	}

	private void addScannerDetails(VBox deviceDetailsVbox, List<ScanDevice> scannerDevices) {
		if (scannerDevices.size() > 1) {
			ComboBox<ScanDeviceInfo> comboBox = new ComboBox<>();
			comboBox.getStyleClass().add("deviceDetailsComboBox");

			for (ScanDevice device : scannerDevices) {
				comboBox.getItems().add(convertToScanDeviceInfo(device));
			}
			comboBox.getSelectionModel().select(getSelectedScanDevice(scannerDevices));
			// Create our custom cells for the ComboBox
			comboBox.setCellFactory(param -> new ListCell<ScanDeviceInfo>() {
				@Override
				protected void updateItem(ScanDeviceInfo item, boolean empty) {
					super.updateItem(item, empty);
					// Create a Label to store our text. We'll set it to wrap text and it's preferred width
					Label label = new Label();
					label.setWrapText(true);
					label.setPrefWidth(280);
					
					if (item == null || empty) {
						setGraphic(null);
					} else {
						// Add our text to the Label
						label.setText(item.toString());
						setGraphic(label);
						documentScanController.setSelectedScanDeviceName(item.getId());
					}
				}
			});
			deviceDetailsVbox.getChildren().add(comboBox);
		} else {
			Label deviceDetailsLabel = new Label(convertToScanDeviceInfo(scannerDevices.get(0)).toString());
			deviceDetailsLabel.getStyleClass().add("deviceDetailsLabel");
			deviceDetailsLabel.setWrapText(true);

			deviceDetailsVbox.getChildren().add(deviceDetailsLabel);
		}
	}

	private ScanDeviceInfo getSelectedScanDevice(List<ScanDevice> scannerDevices) {
		String selectedScanDevice = documentScanController.getSelectedScanDeviceName();
		if (selectedScanDevice != null && !selectedScanDevice.isBlank()) {
			Optional<ScanDevice> docScanDevice = scannerDevices.stream().filter(device -> device.getId().equalsIgnoreCase(selectedScanDevice)).findFirst();
			if (docScanDevice.isPresent()) 
				return convertToScanDeviceInfo(docScanDevice.get());
		}
		documentScanController.setSelectedScanDeviceName(scannerDevices.get(0).getName());
		return convertToScanDeviceInfo(scannerDevices.get(0));
	}

	private ScanDeviceInfo convertToScanDeviceInfo(ScanDevice device) {
		ScanDeviceInfo deviceInfo = new ScanDeviceInfo();
		deviceInfo.setId(device.getId());
		deviceInfo.setName(device.getName());
		if (device.getDeviceType().equals(DeviceType.CAMERA)) {
			deviceInfo.setModel(applicationContext.getApplicationLanguageLabelBundle().getString("webcam"));
		} else {
			deviceInfo.setModel(RegistrationConstants.HYPHEN);
		}
		return deviceInfo;
	}

}
