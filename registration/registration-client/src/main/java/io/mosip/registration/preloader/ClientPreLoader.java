package io.mosip.registration.preloader;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.update.ClientSetupValidator;
import javafx.application.Preloader;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientPreLoader extends Preloader {

    private static final Logger logger = LoggerFactory.getLogger(ClientPreLoader.class);

    public static boolean errorsFound = false;
    public static boolean restartRequired = false;
    private Stage preloaderStage;
    private ProgressBar progressBar = new ProgressBar();
    private TextArea textArea = new TextArea();
    private Label label = new Label("Starting Registration Client : Please wait...");
    private Button stopClient = new Button("Stop Client");
    private Button hidePreLoader = new Button("Exit");

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.preloaderStage = primaryStage;

        VBox loading = new VBox(20);
        loading.setMaxWidth(Region.USE_PREF_SIZE);
        loading.setMaxHeight(Region.USE_PREF_SIZE);
        loading.getChildren().add(label);
        loading.getChildren().add(progressBar);
        loading.getChildren().add(textArea);
        textArea.setPrefWidth(500);
        textArea.setPrefHeight(300);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttons = new HBox();
        buttons.getChildren().add(hidePreLoader);
        buttons.getChildren().add(spacer);
        buttons.getChildren().add(stopClient);
        loading.getChildren().add(buttons);

        stopClient.setVisible(false);
        stopClient.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                textArea.appendText("Exiting from application...\n");
                System.exit(0);
            }
        });

        hidePreLoader.setVisible(false);
        hidePreLoader.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                preloaderStage.hide();
            }
        });

        BorderPane root = new BorderPane(loading);
        Scene scene = new Scene(root);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.setResizable(true);
        primaryStage.setTitle("Client Pre-Loader");
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResource(RegistrationConstants.LOGO).toExternalForm()));
        primaryStage.show();
    }

    @Override
    public boolean handleErrorNotification(ErrorNotification info) {
        return true;
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if(info instanceof ClientPreLoaderErrorNotification) {
            errorsFound = true;
            Throwable t = ((ClientPreLoaderErrorNotification)info).getCause();
            textArea.appendText(t.getMessage()+"\n");
            hidePreLoader.setVisible(false);
            stopClient.setVisible(true);
            logger.error(t.getMessage(), t);
            return;
        }

        if(info instanceof ClientPreLoaderNotification) {
            textArea.appendText(((ClientPreLoaderNotification)info).getMessage());
            textArea.appendText("\n");
            return;
        }
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification stateChangeNotification) {
        if (stateChangeNotification.getType() == StateChangeNotification.Type.BEFORE_LOAD) {
            textArea.appendText("Started to validate the build setup...\n");
            try {
                progressBar.setProgress(0.1);
                ClientSetupValidator clientSetupValidator = new ClientSetupValidator();
                clientSetupValidator.validateBuildSetup();
                if(clientSetupValidator.isPatch_downloaded()) {
                    restartRequired = true;
                    throw new RegBaseCheckedException("","New patches downloaded, Kindly restart the client");
                }
                if(clientSetupValidator.isUnknown_jars_found()) {
                    restartRequired = true;
                    throw new RegBaseCheckedException("","Unrecognized jars found in the classpath, Kindly restart the client");
                }
                else
                    errorsFound = clientSetupValidator.isValidationFailed();

            } catch (RegBaseCheckedException e) {
                errorsFound = true;
                notifyPreloader(new ClientPreLoaderErrorNotification(e));
            }
            textArea.appendText("Build setup validation completed with status : " + (errorsFound ? "FAILURE" : "SUCCESS") + "\n");
        }
        if (stateChangeNotification.getType() == StateChangeNotification.Type.BEFORE_INIT) {
            if(!errorsFound) {
                textArea.appendText("Started to initialize application...\n");
                progressBar.setProgress(0.3);
            }
        }
        if (stateChangeNotification.getType() == StateChangeNotification.Type.BEFORE_START) {
            if(restartRequired) {
                label.setText("Starting Registration Client : Restart Required");
            }
            else if(errorsFound) {
                label.setText("Starting Registration Client : Failed!");
            }
            else {
                textArea.appendText("Setting up application stage...\n");
                progressBar.setProgress(100);
                label.setText("Starting Registration Client : Success.");
                textArea.appendText("Registration client started :)\n");
                hidePreLoader.setVisible(true);
                stopClient.setVisible(true);
            }
        }
    }
}
