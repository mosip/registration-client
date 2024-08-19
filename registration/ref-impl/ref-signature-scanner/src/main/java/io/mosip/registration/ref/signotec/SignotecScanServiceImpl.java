package io.mosip.registration.ref.signotec;

import de.signotec.stpad.api.*;
import de.signotec.stpad.api.events.*;
import de.signotec.stpad.api.exceptions.SigPadException;
import de.signotec.stpad.control.SignatureCanvas;
import de.signotec.stpad.control.SignatureJPanel;
import de.signotec.stpad.enums.SampleRate;
import de.signotec.stpad.enums.ScrollDirection;
import de.signotec.stpad.enums.SigPadAlign;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.registration.api.signaturescanner.SignatureService;
import io.mosip.registration.api.signaturescanner.constant.StreamType;
import io.mosip.registration.dto.DeviceType;
import io.mosip.registration.dto.ScanDevice;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.*;
import java.util.List;


import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LOGO_SIGMA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_SIGMA_CONTROL;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_SIGMA_PAD;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LED1_GREEN;
import static io.mosip.registration.ref.signotec.constant.SampleResources.loadImage;

import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LED1_YELLOW;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_SIGNATURE_SIGMA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_BTN_OK_BW;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_BTN_CANCEL_BW;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LOGO_ZETA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_ZETA_CONTROL;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_ZETA_PAD;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LED2_GREEN;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LED2_YELLOW;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_SIGNATURE_ZETA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LOGO_OMEGA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_OMEGA_CONTROL;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_OMEGA_PAD;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_SIGNATURE_OMEGA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_BTN_OK;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_BTN_CANCEL;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_BTN_RETRY;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_BTN_SCROLL;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LOGO_GAMMA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_GAMMA_CONTROL;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_GAMMA_PAD;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LOGO_DELTA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_SIGNATURE_GAMMA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_DELTA_CONTROL;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_DELTA_PAD;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LED3_GREEN;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LED3_YELLOW;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_SIGNATURE_DELTA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LOGO_ALPHA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_ALPHA_CONTROL;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_ALPHA_PAD;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_SIGNATURE_PENDISPLAY;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_BTN_OK_PENDISPLAY;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_BTN_CANCEL_PENDISPLAY;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_BTN_RETRY_PENDISPLAY;
import static io.mosip.registration.ref.signotec.constant.SampleResources.RES_IMG_LOGO_PENDISPLAY;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_PEN_DISPLAY_CONTROL_MAX;
import static io.mosip.registration.ref.signotec.constant.SampleResources.PEN_WIDTH_PEN_DISPLAY_CONTROL_MIN;
import static de.signotec.stpad.driver.Constants.*;
import static de.signotec.stpad.driver.Constants.SIGPAD_MODELTYPE_ALPHA_ETHERNET;
import static io.mosip.registration.ref.signotec.constant.SampleResources.SCROLL_SPEED_OMEGA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.SCROLL_SPEED_GAMMA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.SCROLL_SPEED_DELTA;
import static io.mosip.registration.ref.signotec.constant.SampleResources.FONT_NAME_PAD;

@Component
public class SignotecScanServiceImpl implements SignatureService, DisconnectListener {
    private SigPadFacade stpadNativeFacade = null;
    private static final Logger LOGGER = LoggerFactory.getLogger(SignotecScanServiceImpl.class);

    private HashMap<String, SigPadDevice> deviceMap = new HashMap<>();
    private SigPadApi sigPad = null;
    /** Target memory (in the pad) for the disclaimer page. */
    private ImageMemory disclaimerPageMemory = null;
    /** Target memory (in the pad) for the signing page. */
    private ImageMemory signingPageMemory = null;

    private java.awt.Component signatureComponent = null;
    private SignatureGraphics signatureGraphics = null;

    private BufferedImage imgLedGreen;
    private BufferedImage imgLedYellow;

    /** The signature background and border. */
    private BufferedImage imgSignature;
    /** The confirm disclaimer/signature button. */
    private BufferedImage imgOk;
    /** The cancel signature process button. */
    private BufferedImage imgCancel;
    /** The retry signature process button. */
    private BufferedImage imgRetry;
    /** The scroll screen up/down buttons. */
    private BufferedImage imgScroll;

    private static final boolean USE_CANVAS = false;

    @Value("${mosip.sigmatec.signature.pad.dpi:600}")
    private Integer selectedDPI;

    @Value("${mosip.sigmatec.signature.pad.pen.width:10}")
    private Integer selectedPenWidth;

    @Value("${mosip.sigmatec.signature.pad.include.timestamp:false}")
    private Boolean includeTimestamp;

    private List<ScanDevice> signDeviceList ;

    @Override
    public String getServiceName() {
        return "Sigma HID";
    }

    @Override
    public void scan(ScanDevice signDevice, String deviceType) throws Exception {
        openPad(signDevice);
        onButtonStartPressed();
    }

    @Override
    public void retry() throws Exception {
        retrySignature();
    }

    @Override
    public void cancel() throws Exception {
        onButtonStopPressed();
    }

    @Override
    public void confirm() throws Exception {
        onButtonOkPressed();
    }

    @Override
    public byte[] loadData(StreamType streamType) throws SignatureException, IOException {
        return saveSignatureAsStream(streamType);
    }

    @Override
    public List<ScanDevice> getConnectedDevices() throws Exception {
        if(signDeviceList == null || signDeviceList.isEmpty()) {
            signDeviceList = new ArrayList<>();
            try {
                if (this.stpadNativeFacade == null) {
                    SigPadFacade facade = null;
                    facade = SigPadFacade.getInstance();
                    facade.initializeApi();
                    this.stpadNativeFacade = facade;

                }

                SigPadDevice[] pads = stpadNativeFacade.getSignatureDevices();

                if (pads.length == 0) {

                } else {
                    for(SigPadDevice pad : pads) {
                        final String displayTextFormat = "%dx%d px,  %.0fx%.0f ppi";
                        ScanDevice device = new ScanDevice();
                        device.setModel(pad.getModelName());
                        device.setSerial(pad.getSerialNumber());
                        device.setName(String.format(displayTextFormat, pad.getDisplayWidth(),
                                pad.getDisplayHeight(), pad.getDisplayXPpi(), pad.getDisplayYPpi()));

                        if (!pad.isModelPenDisplay()) {
                            device.setFirmware(pad.getVersion());
                        }
                        String uuid = UUID.randomUUID().toString();
                        device.setId(uuid);
                        device.setDeviceType(DeviceType.SIGNATURE_PAD);
                        deviceMap.put(uuid, pad);
                        signDeviceList.add(device);
                    }
                }
            } catch (SigPadException e) {
                LOGGER.error("unable to initialize SigPadFacade " + e.getMessage() + ExceptionUtils.getStackTrace(e));
                throw new Exception(e.getMessage());
            }
        }

        return signDeviceList;
    }

    @Override
    public void stop(ScanDevice signDevice) {
        disconnect();
    }

    public void openPad(ScanDevice selectedDevice) throws Exception {
        SigPadDevice device = deviceMap.get(selectedDevice.getId());
        this.sigPad = new SigPadApi(device);
        this.sigPad.addSigPadListener(new SigPadAdapter() {

            @SneakyThrows
            @Override
            public void errorOccurred(ErrorOccurredEvent event) {
                event.consume(); // the signoPAD-API should not log this error
                LOGGER.error("Error event received: " + event.cause.getMessage() + event.cause);
                throw new Exception("Error event received: " + event.cause.getMessage());
            }
        });

        // set sample rate & pen to default values and configure view
        float dpi = 0;
        float maxPenWidth = Constants.MAXIMUM_PEN_WIDTH;
        String displayImg = null;
        Point displayPos = null; // the position of the pad display

        try {
            if (device.isModelSigma()) {
                this.sigPad.openDevice(this);
                dpi = 88;
                displayImg = RES_IMG_LOGO_SIGMA;
                displayPos = new Point(96, 90);
                maxPenWidth = PEN_WIDTH_SIGMA_CONTROL;
                this.sigPad.setSampleRate(SampleRate.HZ_250);
                this.sigPad.setSignaturePenWidth(PEN_WIDTH_SIGMA_PAD);
                this.disclaimerPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
                this.signingPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
                this.imgLedGreen = loadImage(RES_IMG_LED1_GREEN);
                this.imgLedYellow = loadImage(RES_IMG_LED1_YELLOW);
                this.imgSignature = loadImage(RES_IMG_SIGNATURE_SIGMA);
                this.imgOk = loadImage(RES_IMG_BTN_OK_BW);
                this.imgCancel = loadImage(RES_IMG_BTN_CANCEL_BW);
                this.imgRetry = null;
                this.imgScroll = null;
            } else if (device.isModelZeta()) {
                this.sigPad.openDevice(this);
                dpi = 85;
                displayImg = RES_IMG_LOGO_ZETA;
                displayPos = new Point(96, 98);
                maxPenWidth = PEN_WIDTH_ZETA_CONTROL;
                this.sigPad.setSampleRate(SampleRate.HZ_250);
                this.sigPad.setSignaturePenWidth(PEN_WIDTH_ZETA_PAD);
                this.disclaimerPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
                this.signingPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
                this.imgLedGreen = loadImage(RES_IMG_LED2_GREEN);
                this.imgLedYellow = loadImage(RES_IMG_LED2_YELLOW);
                this.imgSignature = loadImage(RES_IMG_SIGNATURE_ZETA);
                this.imgOk = loadImage(RES_IMG_BTN_OK_BW);
                this.imgCancel = loadImage(RES_IMG_BTN_CANCEL_BW);
                this.imgRetry = null;
                this.imgScroll = null;
            } else if (device.isModelOmega()) {
                this.sigPad.openDevice(this);
                dpi = 90;
                displayImg = RES_IMG_LOGO_OMEGA;
                displayPos = new Point(76, 70);
                maxPenWidth = PEN_WIDTH_OMEGA_CONTROL;
                this.sigPad.setSampleRate(SampleRate.HZ_250);
                this.sigPad.setSignaturePenWidth(PEN_WIDTH_OMEGA_PAD, Color.BLUE);
                this.disclaimerPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
                this.signingPageMemory = ImageMemory.requestPermanentStore(this.sigPad);
                this.imgLedGreen = loadImage(RES_IMG_LED1_GREEN);
                this.imgLedYellow = loadImage(RES_IMG_LED1_YELLOW);
                this.imgSignature = loadImage(RES_IMG_SIGNATURE_OMEGA);
                this.imgOk = loadImage(RES_IMG_BTN_OK);
                this.imgCancel = loadImage(RES_IMG_BTN_CANCEL);
                this.imgRetry = loadImage(RES_IMG_BTN_RETRY);
                this.imgScroll = loadImage(RES_IMG_BTN_SCROLL);
            } else if (device.isModelGamma()) {
                this.sigPad.openDevice(this);
                dpi = 94;
                displayImg = RES_IMG_LOGO_GAMMA;
                displayPos = new Point(55, 102);
                maxPenWidth = PEN_WIDTH_GAMMA_CONTROL;
                this.sigPad.setSampleRate(SampleRate.HZ_250);
                this.sigPad.setSignaturePenWidth(PEN_WIDTH_GAMMA_PAD, Color.BLUE);
                this.disclaimerPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
                if (device.getConnectionType() == SIGPAD_CONNECTION_TYPE_USB) {
                    // in USB mode the image transfer is fast enough
                    // we can use the background buffer directly
                    this.signingPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
                } else {
                    this.signingPageMemory = ImageMemory.requestPermanentStore(this.sigPad);
                }
                this.imgLedGreen = loadImage(RES_IMG_LED2_GREEN);
                this.imgLedYellow = loadImage(RES_IMG_LED2_YELLOW);
                this.imgSignature = loadImage(RES_IMG_SIGNATURE_GAMMA);
                this.imgOk = loadImage(RES_IMG_BTN_OK);
                this.imgCancel = loadImage(RES_IMG_BTN_CANCEL);
                this.imgRetry = loadImage(RES_IMG_BTN_RETRY);
                this.imgScroll = loadImage(RES_IMG_BTN_SCROLL);
            } else if (device.isModelDelta()) {
                this.sigPad.openDevice(this);
                dpi = 54f;
                displayImg = RES_IMG_LOGO_DELTA;
                displayPos = new Point(27, 66);
                maxPenWidth = PEN_WIDTH_DELTA_CONTROL;
                this.sigPad.setSampleRate(SampleRate.HZ_250);
                this.sigPad.setSignaturePenWidth(PEN_WIDTH_DELTA_PAD, Color.BLUE);
                this.disclaimerPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
                if (device.getConnectionType() == SIGPAD_CONNECTION_TYPE_USB) {
                    // in USB mode the image transfer is fast enough
                    // we can use the background buffer directly
                    this.signingPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
                } else {
                    this.signingPageMemory = ImageMemory.requestPermanentStore(this.sigPad);
                }
                this.imgLedGreen = loadImage(RES_IMG_LED3_GREEN);
                this.imgLedYellow = loadImage(RES_IMG_LED3_YELLOW);
                this.imgSignature = loadImage(RES_IMG_SIGNATURE_DELTA);
                this.imgOk = loadImage(RES_IMG_BTN_OK);
                this.imgCancel = loadImage(RES_IMG_BTN_CANCEL);
                this.imgRetry = null;
                this.imgScroll = loadImage(RES_IMG_BTN_SCROLL);
            } else if (device.isModelAlpha()) {
                this.sigPad.openDevice(this);
                dpi = 26.9f;
                displayImg = RES_IMG_LOGO_ALPHA;
                displayPos = new Point(149, 31);
                maxPenWidth = PEN_WIDTH_ALPHA_CONTROL;
                this.sigPad.setSignaturePenWidth(PEN_WIDTH_ALPHA_PAD, Color.BLUE);
                // this.disclaimerPageMemory is not used
                if (device.getConnectionType() == SIGPAD_CONNECTION_TYPE_USB) {
                    // in USB mode the image transfer is fast enough
                    // we can use the background buffer directly
                    this.signingPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
                } else {
                    this.signingPageMemory = ImageMemory.requestPermanentStore(this.sigPad);
                }
                this.imgLedGreen = loadImage(RES_IMG_LED1_GREEN);
                this.imgLedYellow = loadImage(RES_IMG_LED1_YELLOW);
                this.imgSignature = loadImage(RES_IMG_SIGNATURE_OMEGA);
                this.imgOk = loadImage(RES_IMG_BTN_OK);
                this.imgCancel = loadImage(RES_IMG_BTN_CANCEL);
                this.imgRetry = loadImage(RES_IMG_BTN_RETRY);
                this.imgScroll = loadImage(RES_IMG_BTN_SCROLL);
            } else if (device.isModelPenDisplay()) {
                this.imgLedGreen = loadImage(RES_IMG_LED3_GREEN);
                this.imgLedYellow = loadImage(RES_IMG_LED3_YELLOW);
                this.imgSignature = loadImage(RES_IMG_SIGNATURE_PENDISPLAY);
                this.imgOk = loadImage(RES_IMG_BTN_OK_PENDISPLAY);
                this.imgCancel = loadImage(RES_IMG_BTN_CANCEL_PENDISPLAY);
                this.imgRetry = loadImage(RES_IMG_BTN_RETRY_PENDISPLAY);

                if (USE_CANVAS) {
                    final SignatureCanvas panel = new SignatureCanvas(this.sigPad,
                            device.getDisplayWidth(), device.getDisplayHeight());
                    panel.setMouseEnabled(true);
                    panel.setPenColor(Color.RED);
                    panel.setLocation(27, 66);
                    panel.setStandbyImage(loadImage(RES_IMG_LOGO_PENDISPLAY));
                    panel.setMaxPenWidth(PEN_WIDTH_PEN_DISPLAY_CONTROL_MAX);
                    panel.setMinPenWidth(PEN_WIDTH_PEN_DISPLAY_CONTROL_MIN);

                    this.signatureComponent = panel;
                    this.signatureGraphics = panel.getSignatureGraphics();
                } else {
                    final SignatureJPanel panel = new SignatureJPanel(this.sigPad,
                            device.getDisplayWidth(), device.getDisplayHeight());
                    panel.setMouseEnabled(true);
                    panel.setPenColor(Color.BLUE);
                    panel.setLocation(27, 66);
                    panel.setStandbyImage(loadImage(RES_IMG_LOGO_PENDISPLAY));
                    panel.setMaxPenWidth(PEN_WIDTH_PEN_DISPLAY_CONTROL_MAX);
                    panel.setMinPenWidth(PEN_WIDTH_PEN_DISPLAY_CONTROL_MIN);

                    this.signatureComponent = panel;
                    this.signatureGraphics = panel.getSignatureGraphics();
                }

                this.sigPad.openDevice(this.signatureComponent);
                this.sigPad.setSampleRate(SampleRate.HZ_250);
                this.sigPad.setSignaturePenWidth(3, Color.BLUE);
                this.sigPad.addSigPadListener(
                        new SigPadSwingAdapter((SigPadListener) this.signatureComponent));

                this.disclaimerPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
                this.signingPageMemory = ImageMemory.requestBackgroundBuffer(this.sigPad);
            } else {
                this.sigPad.closeDevice();
                LOGGER.error("This model type is not supported by this sample application!");
                throw new Exception("This model type is not supported by this sample application!");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage() + "\nopenPad - openDevice : " + ExceptionUtils.getStackTrace(e));
            closePad();
            throw e;
        }

        if (device.isModelPenDisplay()) {
            // empty - the widget is already created
        } else if (USE_CANVAS) {
            // get CanvasBased Control from SigPadApi
            final SignatureCanvas canvas = new SignatureCanvas(this.sigPad, dpi);
            canvas.setMouseEnabled(true);
            canvas.setPenColor(Color.RED);
            canvas.setLocation(displayPos);
            canvas.setMaxPenWidth(maxPenWidth);

            if (displayImg != null) {
                canvas.setStandbyImage(loadImage(displayImg));
            }
            this.signatureComponent = canvas;
            this.signatureGraphics = canvas.getSignatureGraphics();
            this.sigPad.addSigPadListener(new SigPadSwingAdapter(canvas));
        } else {
            // get JPanelBased Control from SigPadApi
            final SignatureJPanel panel = new SignatureJPanel(this.sigPad, dpi);
            panel.setMouseEnabled(true);
            panel.setPenColor(Color.BLUE);
            panel.setLocation(displayPos);
            panel.setMaxPenWidth(maxPenWidth);

            if (displayImg != null) {
                panel.setStandbyImage(loadImage(displayImg));
            }
            this.signatureComponent = panel;
            this.signatureGraphics = panel.getSignatureGraphics();
            this.sigPad.addSigPadListener(new SigPadSwingAdapter(panel));
        }
        if (this.signatureGraphics != null) {
            this.signatureGraphics.setBackgroundColor(Color.WHITE);
            this.signatureGraphics.setBorderColor(new Color(0xF07901));
            this.signatureGraphics.showStandbyImage();
        }
    }

    @SneakyThrows
    @Override
    public void disconnect() {
        closePad();
        if(signDeviceList != null && !signDeviceList.isEmpty())
            signDeviceList.clear();
    }

    @SneakyThrows
    @Override
    public void handleError(SigPadException e) {
        LOGGER.error("Error  : " + e.getMessage() + ExceptionUtils.getStackTrace(e));
        throw new Exception(e.getMessage());
    }

    private void closePad() throws Exception {
        if (this.sigPad != null) {
            // drop signature data
            cancelSignature();

            // close connection
            try {
                this.sigPad.closeDevice();
               // this.stpadNativeFacade.finalizeApi();
            } catch (SigPadException e) {
                LOGGER.error("Error While Closing pad : " + e.getMessage() + ExceptionUtils.getStackTrace(e));
                throw new Exception(e.getMessage());
            }
            this.sigPad = null;
           // this.stpadNativeFacade=null;
        }
    }

    /**
     * Cancels the signature procedure.
     */
    private void cancelSignature() throws Exception {
        try {
            this.sigPad.cancelSignature();
        } catch (SigPadException e) {
            LOGGER.error("Error While Cancelling Signature : " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new Exception(e.getMessage());
        }
        finishSignature();
    }

    private void finishSignature() throws Exception {
        try {
            this.sigPad.clearHotSpots();
        } catch (SigPadException e) {
            LOGGER.error("Error While finishSignature - clearHotSpots : " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new Exception(e.getMessage());
        }
    }

    public void onButtonStartPressed() throws Exception {
        switch (this.sigPad.getPad().getModelType()) {
            case SIGPAD_MODELTYPE_SIGMA_HID:
            case SIGPAD_MODELTYPE_SIGMA_SERIAL:
            case SIGPAD_MODELTYPE_ZETA_HID:
            case SIGPAD_MODELTYPE_ZETA_SERIAL:
            case SIGPAD_MODELTYPE_OMEGA_HID:
            case SIGPAD_MODELTYPE_OMEGA_SERIAL:
            case SIGPAD_MODELTYPE_GAMMA_HID:
            case SIGPAD_MODELTYPE_GAMMA_SERIAL:
            case SIGPAD_MODELTYPE_DELTA_HID:
            case SIGPAD_MODELTYPE_DELTA_SERIAL:
            case SIGPAD_MODELTYPE_DELTA_ETHERNET:
            case SIGPAD_MODELTYPE_PEN_DISPLAY:
                // Sigma, Zeta, Omega, Gamma, Delta, PenDisplay
                showDisclaimer();
                break;

            case SIGPAD_MODELTYPE_ALPHA_HID:
            case SIGPAD_MODELTYPE_ALPHA_SERIAL:
            case SIGPAD_MODELTYPE_ALPHA_ETHERNET:
                // Alpha
                startSignature();
                break;

            default:
                LOGGER.error("This model type is not supported by this application!");
                throw new Exception("This model type is not supported by this application!");
        }
    }

    /**
     * Shows the signature disclaimer
     */
    private void showDisclaimer() throws Exception {
        try {
            // reset display
            clearDisplay();
            if (this.signatureGraphics != null) {
                this.signatureGraphics.resetSignData();
            }
            // remove sign rectangle
            this.sigPad.clearSignRect();
            // remove hotspots
            this.sigPad.clearHotSpots();
            // show disclaimer text in a rectangle
            final int textHeight = drawDisclaimerText(this.disclaimerPageMemory);

            final SigPadDevice device = this.sigPad.getPad();
            SigPadRectangle hotspotCancel = null;
            SigPadRectangle hotspotConfirm = null;
            SigPadRectangle hotspotDown = null;
            SigPadRectangle hotspotUp = null;
            Rectangle overlayArea = null;
            int scrollSpeed = 0;

            if (device.isModelSigma()) {
                // add the images for the buttons
                this.sigPad.setImage(10, 120, this.imgCancel, this.disclaimerPageMemory);
                this.sigPad.setImage(220, 120, this.imgOk, this.disclaimerPageMemory);
                // specify the hotspots for the buttons
                hotspotCancel = new SigPadRectangle(8, 118, 89, 37, this.sigPad);
                hotspotConfirm = new SigPadRectangle(218, 118, 89, 37, this.sigPad);
            } else if (device.isModelZeta()) {
                // add the images for the buttons
                this.sigPad.setImage(10, 160, this.imgCancel, this.disclaimerPageMemory);
                this.sigPad.setImage(220, 160, this.imgOk, this.disclaimerPageMemory);
                // specify the hotspots for the buttons
                hotspotCancel = new SigPadRectangle(8, 158, 89, 37, this.sigPad);
                hotspotConfirm = new SigPadRectangle(218, 158, 89, 37, this.sigPad);
            } else if (device.isModelOmega()) {
                final ImageMemory overlayBuffer = ImageMemory.requestOverlayBuffer(this.sigPad);

                scrollSpeed = SCROLL_SPEED_OMEGA;
                // add the images for the buttons
                this.sigPad.setImage(20, 400, this.imgCancel, overlayBuffer);
                this.sigPad.setImage(210, 400, this.imgOk, overlayBuffer);
                this.sigPad.setImage(450, 400, this.imgScroll, overlayBuffer);
                // specify the overlay area and its hotspots for the buttons
                overlayArea = new Rectangle(0, 392, 640, 88);
                hotspotCancel = new SigPadRectangle(18, 398, 174, 70, this.sigPad);
                hotspotConfirm = new SigPadRectangle(208, 398, 174, 70, this.sigPad);
                hotspotDown = new SigPadRectangle(448, 398, 70, 70, this.sigPad);
                hotspotUp = new SigPadRectangle(552, 398, 70, 70, this.sigPad);
            } else if (device.isModelGamma()) {
                final ImageMemory overlayBuffer = ImageMemory.requestOverlayBuffer(this.sigPad);

                scrollSpeed = SCROLL_SPEED_GAMMA;
                // add the images for the buttons
                this.sigPad.setImage(50, 380, this.imgCancel, overlayBuffer);
                this.sigPad.setImage(270, 380, this.imgOk, overlayBuffer);
                this.sigPad.setImage(580, 380, this.imgScroll, overlayBuffer);
                // specify the overlay area and its hotspots for the buttons
                overlayArea = new Rectangle(0, 378, 800, 102);
                hotspotCancel = new SigPadRectangle(48, 378, 174, 70, this.sigPad);
                hotspotConfirm = new SigPadRectangle(268, 378, 174, 70, this.sigPad);
                hotspotDown = new SigPadRectangle(578, 378, 70, 70, this.sigPad);
                hotspotUp = new SigPadRectangle(682, 378, 70, 70, this.sigPad);
            } else if (device.isModelDelta()) {
                final ImageMemory overlayBuffer = ImageMemory.requestOverlayBuffer(this.sigPad);

                scrollSpeed = SCROLL_SPEED_DELTA;
                // add the images for the buttons
                this.sigPad.setImage(50, 700, this.imgCancel, overlayBuffer);
                this.sigPad.setImage(270, 700, this.imgOk, overlayBuffer);
                this.sigPad.setImage(1060, 700, this.imgScroll, overlayBuffer);
                // specify the overlay area and its hotspots for the buttons
                overlayArea = new Rectangle(0, 698, 1280, 102);
                hotspotCancel = new SigPadRectangle(48, 698, 174, 70, this.sigPad);
                hotspotConfirm = new SigPadRectangle(268, 698, 174, 70, this.sigPad);
                hotspotDown = new SigPadRectangle(1058, 698, 70, 70, this.sigPad);
                hotspotUp = new SigPadRectangle(1162, 698, 70, 70, this.sigPad);
            } else if (device.isModelPenDisplay()) {
                // add the images for the buttons
                this.sigPad.setImage(25, 230, this.imgCancel, this.disclaimerPageMemory);
                this.sigPad.setImage(351, 230, this.imgOk, this.disclaimerPageMemory);
                // specify the hotspots for the buttons
                hotspotCancel = new SigPadRectangle(23, 228, 89, 37, this.sigPad);
                hotspotConfirm = new SigPadRectangle(349, 228, 89, 37, this.sigPad);
            } else {
                LOGGER.error("This model type is not supported by this application!");
                throw new Exception("This model type is not supported by this application!");
            }
            // show the overlay and draw the text on the display
            if (overlayArea != null) {
                this.sigPad.setOverlayArea(overlayArea, this.disclaimerPageMemory);
            }
            this.sigPad.setImageFromStore(this.disclaimerPageMemory);

            // add hotspots
            this.sigPad.addHotSpot(hotspotCancel);
            this.sigPad.addHotSpot(hotspotConfirm);
            if (hotspotDown != null) {
                this.sigPad.addScrollHotSpot(hotspotDown, ScrollDirection.DOWN);
                this.sigPad.addScrollHotSpot(hotspotUp, ScrollDirection.UP);
                this.sigPad.setScrollSpeed(scrollSpeed);

                // the scrollable area used by the scroll buttons
                // and the native scrolling with the pen
                final int areaWidth = device.getBufferWidth();
                final int areaHeight = textHeight + overlayArea.height + 10; // +10 bottom border
                final Rectangle scrollArea = new Rectangle(areaWidth, areaHeight);
                this.sigPad.setScrollArea(scrollArea);

                // enable scrolling with the pen
                enablePenScroll();
            }

            // handle hotspot events within the GUI
            this.sigPad.setHotSpotEventHandler(new DisclaimerHotspotListener());
        } catch (SigPadException e) {
            LOGGER.error("The disclaimer page could not be created.\nCause: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Start the sign procedure.
     */
    private void startSignature() throws Exception {
        final SigPadDevice device = this.sigPad.getPad();
        SigPadRectangle[] hotspots = null;
        SigPadRectangle signingArea = null;

        try {
            // reset pad display
            clearDisplay();
            // reset window display - removes the standby image
            this.signatureGraphics.clearScreen();
            this.sigPad.clearSignRect();
            // remove hotspots
            this.sigPad.clearHotSpots();

            if (device.isModelSigma()) {
                // place background image (including buttons)
                this.sigPad.setImage(0, 0, this.imgSignature, this.signingPageMemory);
                // place the hotspots for the buttons
                hotspots = new SigPadRectangle[] {
                        new SigPadRectangle(10, 7, 89, 37, this.sigPad),
                        new SigPadRectangle(115, 7, 89, 37, this.sigPad),
                        new SigPadRectangle(220, 7, 89, 37, this.sigPad)
                };
                // signature area is the whole display except the toolbar
                signingArea = new SigPadRectangle(10, 54, 300, 96, this.sigPad);
            } else if (device.isModelZeta()) {
                // place background image (including buttons)
                this.sigPad.setImage(0, 0, this.imgSignature, this.signingPageMemory);
                // place the hotspots for the buttons
                hotspots = new SigPadRectangle[] {
                        new SigPadRectangle(10, 7, 89, 37, this.sigPad),
                        new SigPadRectangle(115, 7, 89, 37, this.sigPad),
                        new SigPadRectangle(220, 7, 89, 37, this.sigPad)
                };
                // signature area is the whole display except the toolbar
                signingArea = new SigPadRectangle(10, 54, 300, 136, this.sigPad);
            } else if (device.isModelOmega()) {
                // place background image
                this.sigPad.setImage(25, 100, this.imgSignature, this.signingPageMemory);
                // place the images for the buttons
                this.sigPad.setImage(35, 9, this.imgCancel, this.signingPageMemory);
                this.sigPad.setImage(235, 9, this.imgRetry, this.signingPageMemory);
                this.sigPad.setImage(435, 9, this.imgOk, this.signingPageMemory);
                // specify the hotspots for the buttons
                hotspots = new SigPadRectangle[] {
                        new SigPadRectangle(33, 7, 174, 70, this.sigPad),
                        new SigPadRectangle(233, 7, 174, 70, this.sigPad),
                        new SigPadRectangle(433, 7, 174, 70, this.sigPad)
                };
                // signature area is the whole display except the toolbar
                signingArea = new SigPadRectangle(10, 87, 620, 383, this.sigPad);
            } else if (device.isModelGamma()) {
                // place background image (without buttons)
                this.sigPad.setImage(25, 140, this.imgSignature, this.signingPageMemory);
                // place the images for the buttons
                this.sigPad.setImage(50, 20, this.imgCancel, this.signingPageMemory);
                this.sigPad.setImage(312, 20, this.imgRetry, this.signingPageMemory);
                this.sigPad.setImage(575, 20, this.imgOk, this.signingPageMemory);
                // specify the hotspots for the buttons
                hotspots = new SigPadRectangle[] {
                        new SigPadRectangle(48, 18, 174, 70, this.sigPad),
                        new SigPadRectangle(310, 18, 174, 70, this.sigPad),
                        new SigPadRectangle(573, 18, 174, 70, this.sigPad)
                };
                // signature area is the whole display except the toolbar
                signingArea = new SigPadRectangle(10, 98, 780, 372, this.sigPad);
            } else if (device.isModelDelta()) {
                // place background image (including buttons)
                this.sigPad.setImage(0, 0, this.imgSignature, this.signingPageMemory);
                // specify the hotspots for the buttons
                hotspots = new SigPadRectangle[] {
                        new SigPadRectangle(148, 16, 174, 70, this.sigPad),
                        new SigPadRectangle(553, 16, 174, 70, this.sigPad),
                        new SigPadRectangle(958, 16, 174, 70, this.sigPad)
                };
                // signature area is the whole display except the toolbar
                signingArea = new SigPadRectangle(10, 98, 1260, 692, this.sigPad);
            } else if (device.isModelAlpha()) {
                // show disclaimer text in a rectangle
                drawDisclaimerText(this.signingPageMemory);
                // place background image (without buttons)
                this.sigPad.setImage(89, 550, this.imgSignature, this.signingPageMemory);
                // place the images for the buttons
                this.sigPad.setImage(107, 9, this.imgCancel, this.signingPageMemory);
                this.sigPad.setImage(299, 9, this.imgRetry, this.signingPageMemory);
                this.sigPad.setImage(491, 9, this.imgOk, this.signingPageMemory);
                // specify the hotspots for the buttons
                hotspots = new SigPadRectangle[] {
                        new SigPadRectangle(105, 7, 174, 70, this.sigPad),
                        new SigPadRectangle(297, 7, 174, 70, this.sigPad),
                        new SigPadRectangle(489, 7, 174, 70, this.sigPad)
                };
                // signature area is the whole display except the toolbar
                signingArea = new SigPadRectangle(10, 87, 748, 1269, this.sigPad);
            } else if (device.isModelPenDisplay()) {
                // place background image on the display
                this.sigPad.setImage(10, 80, this.imgSignature, this.signingPageMemory);
                // place buttons on the display
                this.sigPad.setImage(25, 15, this.imgCancel, this.signingPageMemory);
                this.sigPad.setImage(187, 15, this.imgRetry, this.signingPageMemory);
                this.sigPad.setImage(351, 15, this.imgOk, this.signingPageMemory);
                // calculate hotspots
                hotspots = new SigPadRectangle[] {
                        new SigPadRectangle(23, 13, 89, 37, this.sigPad),
                        new SigPadRectangle(185, 13, 89, 37, this.sigPad),
                        new SigPadRectangle(349, 13, 89, 37, this.sigPad)
                };
                // signature area is the whole display except the toolbar
                final int areaWidth = device.getDisplayWidth() - 10;
                final int areaHeight = device.getDisplayHeight() - 60;
                signingArea = new SigPadRectangle(5, 55, areaWidth, areaHeight, this.sigPad);
            } else {
                LOGGER.error("This model type is not supported by this application!");
                throw new Exception("This model type is not supported by this application!");
            }
            // draw the page on display
            this.sigPad.setImageFromStore(this.signingPageMemory);
        } catch (SigPadException e) {
            LOGGER.error("startSignature. Cause: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new Exception(e.getMessage());
        }

        // add hotspot for cancel, retry, confirm
        try {
            if (hotspots != null) {
                this.sigPad.addHotSpot(hotspots[0]);
                this.sigPad.addHotSpot(hotspots[1]);
                this.sigPad.addHotSpot(hotspots[2]);
            }
        } catch (SigPadException e) {
            LOGGER.error("startSignature - addHotSpot Cause: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new Exception(e.getMessage());
        }
        this.sigPad.setHotSpotEventHandler(new SigningHotspotListener());

        // set signature area
        try {
            this.sigPad.setSignRect(signingArea);
        } catch (SigPadException e) {
            LOGGER.error("startSignature - setSignRect Cause: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new Exception(e.getMessage());
        }

        // start signature
        try {
            this.sigPad.startSignature();
        } catch (SigPadException e) {
            LOGGER.error("startSignature - startSignature Cause: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Clears all data on the display.
     */
    private void clearDisplay() throws Exception {
        try {
            this.sigPad.eraseDisplay();
        } catch (SigPadException e) {
            LOGGER.error("clearDisplay - eraseDisplay Cause: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Draw the disclaimer text.
     *
     * @param destination
     *            The target image buffer.
     *
     * @return Returns the height of the text in pixel.
     *
     * @throws SigPadException
     *             If an error occurred.
     */
    private int drawDisclaimerText(ImageMemory destination)
            throws Exception {

        final SigPadDevice device = this.sigPad.getPad();
        final String text = "With my signature, I certify that I'm excited about the signotec LCD "
                + "Signature Pad and the signotec Pad Capture Control. This sample application has "
                + "blown me away and I can't wait to integrate all these great features in my own "
                + "application.";
        final String scrollText = "Congratulations! If you can read this text you have found the "
                + "scroll function!";
        final String endText = "You have scrolled to the end of this text!";

        int lastTextY = 0;
        int lastTextHeight = 0;

        if (device.isModelSigma()) {
            final int textWidth = device.getDisplayWidth() - 20;

            this.sigPad.setFont(new Font(FONT_NAME_PAD, Font.PLAIN, 14));
            this.sigPad.setTextWrapped(10, 5, textWidth, SigPadAlign.LEFT, text, destination);
        } else if (device.isModelZeta()) {
            final int textWidth = device.getDisplayWidth() - 20;

            this.sigPad.setFont(new Font(FONT_NAME_PAD, Font.PLAIN, 17));
            this.sigPad.setTextWrapped(10, 5, textWidth, SigPadAlign.LEFT, text, destination);
        } else if (device.isModelOmega()) {
            final int textWidth = device.getDisplayWidth() - 40;
            lastTextY = 680;

            this.sigPad.setFont(new Font(FONT_NAME_PAD, Font.PLAIN, 35));
            this.sigPad.setTextWrapped(20, 20, textWidth, SigPadAlign.LEFT, text, destination);
            this.sigPad.setTextWrapped(20, 410, textWidth, SigPadAlign.LEFT, scrollText,
                    destination);
            lastTextHeight = this.sigPad.setTextWrapped(20, lastTextY, textWidth,
                    SigPadAlign.LEFT, endText, destination);
        } else if (device.isModelGamma()) {
            final int textWidth = device.getDisplayWidth() - 120;
            lastTextY = 680;

            this.sigPad.setFont(new Font(FONT_NAME_PAD, Font.PLAIN, 35));
            this.sigPad.setTextWrapped(60, 40, textWidth, SigPadAlign.CENTER, text, destination);
            this.sigPad.setTextWrapped(60, 410, textWidth, SigPadAlign.CENTER, scrollText,
                    destination);
            lastTextHeight = this.sigPad.setTextWrapped(60, lastTextY, textWidth,
                    SigPadAlign.CENTER, endText, destination);
        } else if (device.isModelDelta()) {
            final int textWidth = device.getDisplayWidth() - 120;
            lastTextY = 1270;

            this.sigPad.setFont(new Font(FONT_NAME_PAD, Font.PLAIN, 45));
            this.sigPad.setTextWrapped(60, 100, textWidth, SigPadAlign.CENTER, text, destination);
            this.sigPad.setTextWrapped(60, 730, textWidth, SigPadAlign.CENTER, scrollText,
                    destination);
            lastTextHeight = this.sigPad.setTextWrapped(60, lastTextY, textWidth,
                    SigPadAlign.CENTER, endText, destination);
        } else if (device.isModelAlpha()) {
            final int textWidth = device.getDisplayWidth() - 120;
            lastTextY = 100;

            this.sigPad.setFont(new Font(FONT_NAME_PAD, Font.PLAIN, 40));
            lastTextHeight = this.sigPad.setTextWrapped(60, lastTextY, textWidth,
                    SigPadAlign.LEFT, text, destination);
        } else if (device.isModelPenDisplay()) {
            final int textWidth = device.getDisplayWidth() - 60;

            this.sigPad.setFont(new Font(FONT_NAME_PAD, Font.PLAIN, 20));
            this.sigPad.setTextWrapped(30, 30, textWidth, SigPadAlign.CENTER, text, destination);
        } else {
            LOGGER.error("This model type is not supported by this application!");
            throw new Exception("This model type is not supported by this application!");
        }

        return lastTextY + lastTextHeight;
    }

    /**
     * Enable the native scrolling with the pen on the device if the function is supported by the
     * device and the facade.
     */
    private void enablePenScroll() throws Exception {
        try {
            final SigPadDevice device = this.sigPad.getPad();

            if (device.isPenScrollSupported()) {

                this.sigPad.setPenScrollEnabled(true);
            }
        } catch (SigPadException e) {
            LOGGER.error("enablePenScroll - setPenScrollEnabled Cause: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new Exception(e.getMessage());
        }
    }

    /**
     * The listener for the hotspots on the disclaimer screen.
     */
    private final class DisclaimerHotspotListener implements HotspotListener {

        private static final int HOTSPOT_ID_CANCEL = 0;
        private static final int HOTSPOT_ID_CONFIRM = 1;
        private static final int HOTSPOT_ID_SCROLL_DOWN = 2;
        private static final int HOTSPOT_ID_SCROLL_UP = 3;

        @SneakyThrows
        @Override
        public void pressHotSpot(int hotSpotId, boolean isPressed) {
            if (!isPressed) { // release of the button
                handleHotSpot(hotSpotId);
            }
        }

        private void handleHotSpot(int hotSpotId) throws Exception {
            try {
                switch (hotSpotId) {

                    case HOTSPOT_ID_CANCEL:
                        // we leave the page - ignore further hotspot changes
                        getSigPad().setHotSpotEventHandler(null);
                        disablePenScroll();
                        getSigPad().eraseDisplay();
                        finishSignature();
                        break;

                    case HOTSPOT_ID_CONFIRM:
                        // we leave the page - ignore further hotspot changes
                        getSigPad().setHotSpotEventHandler(null);
                        // scrolling and signing at the same time is not possible - disable scrolling
                        disablePenScroll();
                        getSigPad().eraseDisplay();
                        startSignature();
                        break;

                    case HOTSPOT_ID_SCROLL_UP:
                    case HOTSPOT_ID_SCROLL_DOWN:
                        // nothing to do - scrolls automatically
                        break;

                    default:
                        LOGGER.error("invalid hotspot " + hotSpotId + " for disclaimer screen");
                        throw new Exception("invalid hotspot " + hotSpotId + " for disclaimer screen");
                }
            } catch (SigPadException e) {
                handleError(e);
            }
        }

        @Override
        public void handleError(SigPadException cause) {
            SignotecScanServiceImpl.this.handleError(cause);
        }
    }

    /**
     * The listener for the hotspots on the signing screen.
     */
    private final class SigningHotspotListener implements HotspotListener {

        private static final int HOTSPOT_ID_CANCEL = 0;
        private static final int HOTSPOT_ID_RETRY = 1;
        private static final int HOTSPOT_ID_CONFIRM = 2;

        @SneakyThrows
        @Override
        public void pressHotSpot(final int hotSpotId, boolean isPressed) {
            if (!isPressed) { // release of the button
                handleHotSpot(hotSpotId);
            }
        }

        private void handleHotSpot(int hotSpotId) throws Exception {
            switch (hotSpotId) {

                case HOTSPOT_ID_CANCEL:
                    // we leave the page - ignore further hotspot changes
                    getSigPad().setHotSpotEventHandler(null);
                    cancelSignature();
                    break;

                case HOTSPOT_ID_RETRY:
                    retrySignature();
                    break;

                case HOTSPOT_ID_CONFIRM:
                    // we leave the page - ignore further hotspot changes
                    getSigPad().setHotSpotEventHandler(null);
                    confirmSignature();
                    break;

                default:
                    // do nothing
            }
        }

        @Override
        public void handleError(SigPadException cause) {
            SignotecScanServiceImpl.this.handleError(cause);
        }
    }

    /**
     * @return Returns the manager for the connected signature pad. Returns <code>null</code> if no
     *         pad is connected.
     */
    private SigPadApi getSigPad() {
        return this.sigPad;
    }

    /**
     * Disable the native scrolling with the pen on the device if the function is supported by the
     * device and the facade.
     */
    private void disablePenScroll() throws Exception {
        try {
            final SigPadDevice device = this.sigPad.getPad();

            if (device.isPenScrollSupported()) {

                this.sigPad.setPenScrollEnabled(false);
            }
        } catch (SigPadException e) {
            LOGGER.error("disablePenScroll - setPenScrollEnabled Cause: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Resets the signature for retry.
     */
    private void retrySignature() throws Exception {
        try {
            this.sigPad.retrySignature();
        } catch (SigPadException e) {
            LOGGER.error("retrySignature - retrySignature Cause: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Confirms the signature data.
     */
    private String confirmSignature() throws Exception {
        try {
            this.sigPad.confirmSignature();

            if (this.sigPad.getSignatureCount() > 0) {
                return this.sigPad.getSignatureCount() + " points succesfully captured.";
            } else {
                LOGGER.error("This is not a valid Signature!");
                finishSignature();
                throw new Exception("This is not a valid Signature!");
            }
        } catch (SigPadException e) {
            LOGGER.error("confirmSignature - confirmSignature Cause: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
            finishSignature();
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Saves the captured Signature as image to disk.
     *
     */
    private byte[] saveSignatureAsStream(StreamType streamType) throws SignatureException, IOException {
        // check if signature data was captured
        if (this.sigPad.getSignatureCount() == 0) {
            LOGGER.error("This is not a valid signature");
            throw new SignatureException("This is not a valid signature");
        }

        switch (streamType) {
            case ISO:
                return this.sigPad.getSignatureIsoData();
            case SDB:
                return this.sigPad.getSignatureDataBytes();
            case DAT:
                return this.sigPad.getSignatureData().getBytes(StandardCharsets.UTF_8);
            case IMAGE:
            default:
                final int dpi = selectedDPI;
                final int penWidth = selectedPenWidth;
                final boolean addTimestamp = includeTimestamp;
                this.sigPad.setFont(new Font(FONT_NAME_PAD, Font.PLAIN, 40));
                BufferedImage bi = this.sigPad.saveSignatureAsStream(dpi, penWidth, addTimestamp, BufferedImage.TYPE_BYTE_GRAY, Color.GREEN);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bi, "jpg", baos);
                return baos.toByteArray();
        }
    }

    private void onButtonStopPressed() throws Exception {
        //      if (this.buttonRetry.isEnabled()) {
        cancelSignature();
        //      } else {
        //          try {
        //              disablePenScroll();
        //              this.sigPad.eraseDisplay();
        //         } catch (SigPadException e1) {
        //              showError(e1.getMessage(), e1);
        //          }
        //          finishSignature();
        //      }
    }

    private void onButtonOkPressed() throws Exception {
        //    if (SignoPadSample.this.buttonRetry.isEnabled()) {
        confirmSignature();
        //    } else {
        //        disablePenScroll();
        //        startSignature();
        //    }
    }
}
