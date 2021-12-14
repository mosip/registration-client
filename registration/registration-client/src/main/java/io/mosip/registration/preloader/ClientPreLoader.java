package io.mosip.registration.preloader;

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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientPreLoader extends Preloader {

    private static final Logger logger = LoggerFactory.getLogger(ClientPreLoader.class);

    public static boolean errorsFound = false;
    private Stage preloaderStage;
    private ProgressBar progressBar = new ProgressBar();
    private TextArea textArea = new TextArea();
    private Label label = new Label("Please wait...");
    private Button exit = new Button("Exit");

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.preloaderStage = primaryStage;

        VBox loading = new VBox(20);
        loading.setMaxWidth(Region.USE_PREF_SIZE);
        loading.setMaxHeight(Region.USE_PREF_SIZE);
        loading.getChildren().add(progressBar);
        loading.getChildren().add(label);
        loading.getChildren().add(textArea);
        textArea.setPrefWidth(500);
        textArea.setPrefHeight(300);

        exit.setVisible(false);
        loading.getChildren().add(exit);
        exit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                textArea.appendText("Exiting from application...\n");
                System.exit(0);
            }
        });

        BorderPane root = new BorderPane(loading);
        Scene scene = new Scene(root);

        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.setTitle("Client Pre-Loader");
        primaryStage.setScene(scene);
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
            exit.setVisible(true);
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
                progressBar.setProgress(10);
                ClientSetupValidator clientSetupValidator = new ClientSetupValidator();
                clientSetupValidator.validateBuildSetup();
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
                progressBar.setProgress(30);
            }
        }
        if (stateChangeNotification.getType() == StateChangeNotification.Type.BEFORE_START) {
            if(errorsFound) {
                label.setText("Failed!");
            }
            else {
                textArea.appendText("Setting up application stage...\n");
                progressBar.setProgress(100);
                label.setText("Success.");
                textArea.appendText("Registration client started :)\n");
                //preloaderStage.hide();
            }
        }
    }
}
