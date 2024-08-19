package io.mosip.registration.controller;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.PridValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.schema.ProcessSpecDto;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

/**
 * {@code QrCodePopUpController} is to capture the qrcode from pre-registration
 * details
 *
 * @author Cifu Kig
 * @since 1.0.0
 *
 */
@Controller
public class QrCodePopUpViewController extends BaseController implements
        Initializable, Runnable, ThreadFactory {
    private static final Logger LOGGER =
            AppConfig.getLogger(QrCodePopUpViewController.class);
    @FXML
    private GridPane captureWindow;
    @FXML
    private ComboBox<String> availableWebcams;
    @Value("${mosip.doc.stage.width:400}")
    private int width;
    @Value("${mosip.doc.stage.height:400}")
    private int height;
    @Autowired
    private GenericController genericController;
    @Autowired
    private PridValidator<String> pridValidatorImpl;
    private Stage popupStage;
    private WebcamPanel panel = null;
    private Webcam webcam = null;
    private ExecutorService executor =
            Executors.newSingleThreadExecutor(this);
    public Stage getPopupStage() {
        return popupStage;
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
//Check if there are any cameras connected
        if (Webcam.getWebcams().isEmpty()) {
            LOGGER.error(RegistrationConstants.USER_REG_SCAN_EXP);
            generateAlert(RegistrationConstants.ERROR,
                    RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.
                            NO_DEVICES_DETECTED));
        } else {
            availableWebcams.getItems().addAll(Webcam.getWebcams().stream().map(s ->
                    s.getName()).collect(Collectors.toList()));
            availableWebcams.getSelectionModel().select(0);
            availableWebcams.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    Webcam oldWebcam = Webcam.getWebcamByName(oldValue);
                    oldWebcam.close();
                    initWebcam(newValue);
                });
            });
            initWebcam(availableWebcams.getSelectionModel().getSelectedItem());
        }
    }
    /**
     * This method will open popup to scan
     *
     * @param title
     */
    public void init(String title) {
        try {
            LOGGER.info("Loading QR code popup page : {}",
                    "/fxml/QrCode.fxml");
            Parent scanPopup =
                    BaseController.load(getClass().getResource("/fxml/QrCode.fxml"));
            LOGGER.info("Setting doc screen width :{}, height: {}", width,
                    height);
            Scene scene = new Scene(scanPopup, width, height);
            scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
            popupStage = new Stage();
            popupStage.setResizable(true);
            popupStage.setMaximized(false);
            popupStage.setScene(scene);
            popupStage.initModality(Modality.WINDOW_MODAL);
            popupStage.initOwner(fXComponents.getStage());
            popupStage.setTitle(title);
            popupStage.setOnCloseRequest((e) -> {
                exitWindow(e);
            });
            popupStage.show();
            LOGGER.debug("qr scan screen launched");
            LOGGER.info("Opening pop-up screen to qr code scan for loading pre-registration id");
        } catch (IOException exception) {
            LOGGER.error(RegistrationConstants.USER_REG_SCAN_EXP,
                    exception);
            generateAlert(RegistrationConstants.ERROR,
                    RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.
                            UNABLE_LOAD_QR_SCAN_POPUP));
        }
    }
    private void initWebcam(String name) {
        Dimension size = WebcamResolution.VGA.getSize();
        webcam = Webcam.getWebcamByName(name);
        LOGGER.info("Setting webcam image size for QR Code");
        if (!webcam.isOpen()) {
            webcam.setViewSize(size);
            panel = new WebcamPanel(webcam);
            panel.setPreferredSize(size);
            panel.setFPSDisplayed(true);
            final SwingNode swingNode = new SwingNode();
            swingNode.setContent(panel);
            captureWindow.getChildren().add(swingNode);
            executor.execute(this);
        }
    }
    @Override
    public void run() {
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                LOGGER.error(RegistrationConstants.USER_REG_SCAN_EXP, ex);
            }
            Result result = null;
            BufferedImage image = null;
            if (webcam.isOpen()) {
                if ((image = webcam.getImage()) == null) {
                    continue;
                }
            } else {
                continue;
            }
            LuminanceSource source = new
                    BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new
                    HybridBinarizer(source));
            try {
                result = new MultiFormatReader().decode(bitmap);
            } catch (NotFoundException nfe) {
                LOGGER.info("Valid QR Code not found");
            } catch (Exception e) {
                LOGGER.error(RegistrationConstants.USER_REG_SCAN_EXP, e);
                generateAlertLanguageSpecific(RegistrationConstants.ERROR,
                        RegistrationUIConstants.ERROR_DECODING_QR_CODE);
            }
            if (result != null) {
                boolean isValid;
                try {
                    isValid =
                            pridValidatorImpl.validateId(result.getText());
                } catch (InvalidIDException invalidIDException) {
                    isValid = false;
                }
                if (isValid) {
                    Result finalResult = result;
                    Platform.runLater(() -> close(finalResult.getText()));
                } else {
                    Platform.runLater(() ->
                            generateAlertLanguageSpecific(RegistrationConstants.ERROR,
                                    RegistrationUIConstants.PRE_REG_ID_NOT_VALID));
                }
            }
        } while (true);
    }
    @Override
    public Thread newThread(@NotNull Runnable r) {
        Thread t = new Thread(r, "Scan QR Code Thread");
        t.setDaemon(true);
        return t;
    }
    private void close(String registrationNumber) {
        if (registrationNumber != null) {
            genericController.getRegistrationNumberTextField().setText(registrationNumber);
            LOGGER.info("Execute pre-registration packet fetching");

            RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
            ProcessSpecDto processSpecDto = genericController.getProcessSpec(registrationDTO.getProcessId(), registrationDTO.getIdSchemaVersion());
            genericController.executePreRegFetchTask(genericController.getRegistrationNumberTextField(), processSpecDto.getFlow());
        }
        Thread.currentThread().interrupt();
        stopStreaming();
        popupStage.close();
        generateAlert(RegistrationConstants.ALERT_INFORMATION,
                RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.
                        QR_CODE_SCAN_SUCCESS));
        LOGGER.debug("Scanning QR code completed");
    }
    /**
     * event class to exit from present pop up window.
     *
     * @param event
     */
    public void exitWindow(WindowEvent event) {
        LOGGER.info("Calling exit window to close the popup");
        stopStreaming();
        popupStage.close();
        LOGGER.info("Scan Popup is closed");
    }
    private void stopStreaming() {
        try {
            if (webcam.isOpen()) {
                webcam.close();
            }
        } finally {
            webcam.close();
        }
    }
}