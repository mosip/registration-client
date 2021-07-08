package io.mosip.registration.controller.settings.impl;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.RestartController;
import io.mosip.registration.controller.settings.SettingsInterface;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.entity.SyncControl;
import io.mosip.registration.entity.SyncJobDef;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.config.JobConfigurationService;
import io.mosip.registration.service.config.LocalConfigService;
import io.mosip.registration.service.sync.impl.MasterSyncServiceImpl;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
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
import javafx.stage.Screen;

@Controller
public class ScheduledJobsSettingsController extends BaseController implements SettingsInterface {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(ScheduledJobsSettingsController.class);

	@FXML
	private ScrollPane contentPane;

	@FXML
	private Label headerLabel;

	@FXML
	private GridPane header;

	@FXML
	private StackPane progressIndicatorPane;

	@FXML
	private ProgressIndicator progressIndicator;

	@Autowired
	private JobConfigurationService jobConfigurationService;

	@Autowired
	private MasterSyncServiceImpl masterSyncServiceImpl;

	@Autowired
	private LocalConfigService localConfigService;

	@Autowired
	private RestartController restartController;

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
			List<SyncJobDef> syncJobs = masterSyncServiceImpl.getSyncJobs();
			GridPane gridPane = createGridPane(syncJobs.size());
			addContentToGridPane(gridPane, syncJobs);
			contentPane.setContent(gridPane);
		} catch (RegBaseCheckedException exception) {
			LOGGER.error(String.format("%s -> Exception while Opening Settings page  %s -> %s",
					RegistrationConstants.USER_REG_SCAN_EXP, exception.getMessage(),
					ExceptionUtils.getStackTrace(exception)));

			generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.getMessageLanguageSpecific("UNABLE_LOAD_SETTINGS_PAGE"));
		}
	}

	private void addContentToGridPane(GridPane gridPane, List<SyncJobDef> syncJobs) throws RegBaseCheckedException {
		List<String> permittedJobs = localConfigService.getPermittedJobs(RegistrationConstants.PERMITTED_JOB_TYPE);
		int rowIndex = 0;
		int columnIndex = 0;
		for (SyncJobDef syncJob : syncJobs) {
			SyncControl syncControl = jobConfigurationService.getSyncControlOfJob(syncJob.getId());
			String localSyncFrequency = localConfigService.getValue(syncJob.getId());

			GridPane mainGridPane = new GridPane();
			mainGridPane.setId(syncJob.getName());
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
			columnConstraint1.setPercentWidth(5);
			ColumnConstraints columnConstraint2 = new ColumnConstraints();
			columnConstraint2.setPercentWidth(75);
			ColumnConstraints columnConstraint3 = new ColumnConstraints();
			columnConstraint3.setPercentWidth(20);
			subGridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2, columnConstraint3);

			VBox imageVbox = new VBox();
			imageVbox.setAlignment(Pos.CENTER);

			ImageView imageView = new ImageView(getImage(RegistrationConstants.SYNC_IMG, true));
			imageView.setFitWidth(45);
			imageView.setFitHeight(45);
			imageVbox.setOnMouseClicked(event -> {
				executeJob(syncJob);
			});
			Tooltip tooltip = new Tooltip(applicationContext.getApplicationLanguageLabelBundle()
					.getString(RegistrationConstants.RUN_NOW_LABEL));
			Tooltip.install(imageVbox, tooltip);

			imageVbox.getChildren().add(imageView);

			subGridPane.add(imageVbox, 2, 0);

			VBox jobVbox = new VBox();
			jobVbox.setAlignment(Pos.CENTER_LEFT);
			Label jobNameLabel = new Label(syncJob.getName());
			jobNameLabel.getStyleClass().add(RegistrationConstants.OPERATIONAL_TITLE);
			jobNameLabel.setWrapText(true);

			String nextSyncTime = RegistrationConstants.HYPHEN;
			if (syncJob.getSyncFreq() != null) {
				nextSyncTime = getLocalZoneTime(jobConfigurationService.getNextRestartTime(
						localSyncFrequency != null && !localSyncFrequency.isBlank() ? localSyncFrequency
								: syncJob.getSyncFreq()));
			}
			Label nextRunLabel = new Label(applicationContext.getApplicationLanguageLabelBundle()
					.getString(RegistrationConstants.NEXT_RUN_LABEL) + nextSyncTime);
			nextRunLabel.getStyleClass().add(RegistrationConstants.OPERATIONAL_DETAILS);
			nextRunLabel.setWrapText(true);

			String lastSyncTime = RegistrationConstants.HYPHEN;
			if (syncControl != null && syncControl.getLastSyncDtimes() != null) {
				lastSyncTime = getLocalZoneTime(syncControl.getLastSyncDtimes().toString());
			}
			Label lastRunLabel = new Label(applicationContext.getApplicationLanguageLabelBundle()
					.getString(RegistrationConstants.LAST_RUN_LABEL) + lastSyncTime);
			lastRunLabel.getStyleClass().add(RegistrationConstants.OPERATIONAL_DETAILS);
			lastRunLabel.setWrapText(true);

			HBox hBox = new HBox();
			hBox.setSpacing(5);
			Label cronLabel = new Label(applicationContext.getApplicationLanguageLabelBundle()
					.getString(RegistrationConstants.CRON_EXPRESSION_LABEL));
			cronLabel.getStyleClass().add(RegistrationConstants.SYNC_JOB_LABEL_STYLE);
			hBox.setAlignment(Pos.CENTER_LEFT);
			TextField cronTextField = new TextField();
			String cronExp = localConfigService.getValue(syncJob.getId());
			if (cronExp != null && !cronExp.isBlank()) {
				cronTextField.setText(cronExp);
			} else {
				cronTextField.setText(syncJob.getSyncFreq());
			}
			cronTextField.getStyleClass().add(RegistrationConstants.SYNC_JOB_TEXTFIELD_STYLE);
			Button submit = new Button(applicationContext.getApplicationLanguageLabelBundle()
					.getString(RegistrationConstants.SUBMIT_LABEL));
			submit.getStyleClass().add(RegistrationConstants.SYNC_JOB_BUTTON_STYLE);
			submit.setOnAction(event -> {
				modifyCronExpression(syncJob, cronTextField.getText());
			});

			cronTextField.focusedProperty().addListener((o, oldValue, newValue) -> {
				if (newValue) {
					Platform.runLater(() -> {
						int carretPosition = cronTextField.getCaretPosition();
						if (cronTextField.getAnchor() != carretPosition) {
							cronTextField.selectRange(carretPosition, carretPosition);
						}
					});
				}
			});

			if (!permittedJobs.contains(syncJob.getId())) {
				submit.setVisible(false);
				cronTextField.setEditable(false);
			}

			hBox.getChildren().addAll(cronLabel, cronTextField, submit);

			jobVbox.getChildren().addAll(jobNameLabel, nextRunLabel, lastRunLabel, hBox);

			subGridPane.add(jobVbox, 1, 0);

			mainGridPane.add(subGridPane, 0, 0);

			changeNodeOrientation(mainGridPane);

			gridPane.add(mainGridPane, columnIndex, rowIndex);
			rowIndex = (columnIndex == 2) ? (rowIndex + 1) : rowIndex;
			columnIndex = (columnIndex == 2) ? 0 : (columnIndex + 1);
		}
	}

	private void executeJob(SyncJobDef syncJob) {
		ResourceBundle resourceBundle = applicationContext.getBundle(ApplicationContext.applicationLanguage(),
				RegistrationConstants.MESSAGES);
		progressIndicatorPane.setVisible(true);
		getStage().getScene().getRoot().setDisable(true);

		Service<ResponseDTO> taskService = new Service<ResponseDTO>() {
			@Override
			protected Task<ResponseDTO> createTask() {
				return new Task<ResponseDTO>() {
					/*
					 * (non-Javadoc)
					 *
					 * @see javafx.concurrent.Task#call()
					 */
					@Override
					protected ResponseDTO call() {
						LOGGER.info("Started execution of job {}", syncJob.getName());

						return jobConfigurationService.executeJob(syncJob.getId(),
								RegistrationConstants.JOB_TRIGGER_POINT_USER);
					}
				};
			}
		};

		progressIndicator.progressProperty().bind(taskService.progressProperty());
		taskService.start();

		taskService.setOnSucceeded(event -> {
			getStage().getScene().getRoot().setDisable(false);
			progressIndicatorPane.setVisible(false);

			ResponseDTO responseDTO = taskService.getValue();

			if (responseDTO.getSuccessResponseDTO() != null) {
				LOGGER.info("Execution is successful for the job {}", syncJob.getName());

				generateAlertLanguageSpecific(RegistrationConstants.ALERT_INFORMATION, MessageFormat
						.format(resourceBundle.getString("JOB_EXECUTION_SUCCESS_MSG"), syncJob.getName()));
				if ((responseDTO.getSuccessResponseDTO().getOtherAttributes() != null && responseDTO
						.getSuccessResponseDTO().getOtherAttributes().containsKey(RegistrationConstants.RESTART)) && configUpdateAlert("RESTART_MSG")) {
					restartController.restart();
				}
			} else if (responseDTO.getErrorResponseDTOs() != null) {
				LOGGER.error("Job execution failed with response: " + responseDTO.getErrorResponseDTOs().get(0));

				generateAlertLanguageSpecific(RegistrationConstants.ALERT_INFORMATION,
						MessageFormat.format(resourceBundle.getString("JOB_EXECUTION_FAILURE_MSG"), syncJob.getName()));
			}
			setContent();
		});
		taskService.setOnFailed(event -> {
			LOGGER.error("Failed execution of the task: ", syncJob.getName());

			getStage().getScene().getRoot().setDisable(false);
			progressIndicatorPane.setVisible(false);

			generateAlertLanguageSpecific(RegistrationConstants.ALERT_INFORMATION,
					MessageFormat.format(resourceBundle.getString("JOB_EXECUTION_FAILURE_MSG"), syncJob.getName()));
		});
	}

	private void modifyCronExpression(SyncJobDef syncJob, String cronExpression) {
		if (!jobConfigurationService.isValidCronExpression(cronExpression)) {
			generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.getMessageLanguageSpecific("INVALID_CRON_EXPRESSION"));
			return;
		}
		localConfigService.modifyJob(syncJob.getId(), cronExpression);
		if (configUpdateAlert("CRON_EXPRESSION_MODIFIED")) {
			restartController.restart();
		}
	}

	private boolean configUpdateAlert(String context) {
		if (!fXComponents.getScene().getRoot().getId().equals("mainBox") && !SessionContext.map()
				.get(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ).equals(RegistrationConstants.ENABLE)) {

			Alert alert = createAlert(AlertType.CONFIRMATION, RegistrationUIConstants.INFORMATION,
					RegistrationUIConstants.getMessageLanguageSpecific("ALERT_NOTE_LABEL"),
					RegistrationUIConstants.getMessageLanguageSpecific(context),
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

	private GridPane createGridPane(int size) {
		GridPane gridPane = new GridPane();
		gridPane.setPrefHeight(700);
		gridPane.setPrefWidth(1200);
		gridPane.setAlignment(Pos.TOP_CENTER);
		gridPane.setHgap(25);
		gridPane.setVgap(25);

		if (size <= 3) {
			RowConstraints rowConstraint = new RowConstraints();
			rowConstraint.setPercentHeight(30);
			gridPane.getRowConstraints().add(rowConstraint);
		} else {
			int ceilOfSize = (size % 3 == 0) ? size : (size + (3 - size % 3));
			for (int index = 1; index <= ceilOfSize / 3; index++) {
				RowConstraints rowConstraint = new RowConstraints();
				rowConstraint.setPercentHeight(30);
				gridPane.getRowConstraints().add(rowConstraint);
			}
		}

		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(33);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(34);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(33);

		gridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2, columnConstraint3);

		return gridPane;
	}

}
