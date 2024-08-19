package io.mosip.registration.controller.reg;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import io.mosip.registration.api.signaturescanner.SignatureFacade;
import io.mosip.registration.api.signaturescanner.constant.StreamType;
import io.mosip.registration.dto.DeviceType;
import io.mosip.registration.dto.ScanDevice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.api.docscanner.DocScannerFacade;
import io.mosip.registration.api.docscanner.DocScannerUtil;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.device.ScanPopUpViewController;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.util.control.FxControl;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * {@code DocumentScanController} is to handle the screen of the Demographic
 * document section details
 *
 * @author M1045980
 * @since 1.0.0
 */
@Controller
public class DocumentScanController extends BaseController {

	private static final Logger LOGGER = AppConfig.getLogger(DocumentScanController.class);

	@Autowired
	private ScanPopUpViewController scanPopUpViewController;

	@FXML
	protected GridPane documentScan;

	@FXML
	private GridPane documentPane;

	@FXML
	protected ImageView docPreviewImgView;

	@FXML
	protected Label docPreviewNext;

	@FXML
	protected Label docPreviewPrev;

	@FXML
	protected Label docPageNumber;

	@FXML
	protected Label docPreviewLabel;
	@FXML
	public GridPane documentScanPane;

	@FXML
	private VBox docScanVbox;

	private List<BufferedImage> scannedPages;

	@FXML
	private Label registrationNavlabel;

	@FXML
	private Button continueBtn;
	@FXML
	private Button backBtn;
	@FXML
	private ImageView backImageView;
	@FXML
	private Label biometricExceptionReq;

	@Autowired
	private DocScannerFacade docScannerFacade;

	@Autowired
	private SignatureFacade signatureFacade;

	private String selectedScanDeviceName;
	private String subType;
	private ScanDevice scanDevice;

	private FxControl fxControl;

	public void scan(Stage popupStage) {
		try {
			scanPopUpViewController.getScanningMsg().setVisible(true);
			if (scannedPages == null) {
				scannedPages = new ArrayList<>();
			}

			try {
				if(scanDevice != null) {
					signatureFacade.stopDevice(scanDevice);
				}
			} catch (Exception e){};

			Optional<ScanDevice> result = null;
			if(subType.equals(RegistrationConstants.PROOF_OF_SIGNATURE)) {
				result = signatureFacade.getConnectedDevices().stream().filter(d -> d.getId().equals(selectedScanDeviceName)).findFirst();
			} else {
				result = docScannerFacade.getConnectedDevices().stream().filter(d -> d.getId().equals(selectedScanDeviceName)).findFirst();
			}

			if(result == null || !result.isPresent()) {
				LOGGER.error("No scan devices found");
				generateAlert(RegistrationConstants.ERROR,
						RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND));
				return;
			}

			scanDevice = result.get();
			scanDevice.setFrame(null);
			scanDevice.setWidth(0);
			scanDevice.setHeight(0);
			BufferedImage bufferedImage = null;
			if(subType.equals(RegistrationConstants.PROOF_OF_SIGNATURE)) {
				signatureFacade.scanDocument(scanDevice, DeviceType.SIGNATURE_PAD.toString());
			} else {
				bufferedImage = docScannerFacade.scanDocument(scanDevice, getValueFromApplicationContext(RegistrationConstants.IMAGING_DEVICE_TYPE));
				if (bufferedImage == null) {
					LOGGER.error("captured buffered image was null");
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_ERROR));
					return;
				}

				scannedPages.add(bufferedImage);
				scanPopUpViewController.getImageGroup().getChildren().clear();
				scanPopUpViewController.getImageGroup().getChildren().add(new ImageView(DocScannerUtil.getImage(bufferedImage)));
				scanPopUpViewController.getScanImage().setVisible(true);
				scanPopUpViewController.getScanningMsg().setVisible(false);
				scanPopUpViewController.showPreview(true);
				generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.DOC_CAPTURE_SUCCESS));
			}
		} catch (RuntimeException exception) {
			LOGGER.error("Exception while scanning documents for registration", exception);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_ERROR));
		} catch (Exception e) {
			LOGGER.error("Exception while scanning documents for registration", e);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_ERROR));
		}
	}

	public BufferedImage saveSignature() throws IOException {
		if(subType.equals(RegistrationConstants.PROOF_OF_SIGNATURE)) {
			Optional<ScanDevice> result = signatureFacade.getConnectedDevices().stream().filter(d -> d.getId().equals(selectedScanDeviceName)).findFirst();
			if(result == null || !result.isPresent()) {
				LOGGER.error("No scan devices found");
				generateAlert(RegistrationConstants.ERROR,
						RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND));
				return null;
			}
			InputStream is = new ByteArrayInputStream(signatureFacade.confirm(result.get(), StreamType.IMAGE));
			signatureFacade.stopDevice(result.get());
			return ImageIO.read(is);
		}
		return null;
	}

	public byte[] captureAndConvertBufferedImage() throws Exception {
		List<ScanDevice> devices = docScannerFacade.getConnectedCameraDevices();

		byte[] byteArray = new byte[0];
		if(!devices.isEmpty()) {
			BufferedImage bufferedImage = docScannerFacade.scanDocument(devices.get(0), getValueFromApplicationContext(RegistrationConstants.IMAGING_DEVICE_TYPE));
			if (bufferedImage != null) {
				byteArray = DocScannerUtil.getImageBytesFromBufferedImage(bufferedImage);
			}
			// Enable Auto-Logout
			SessionContext.setAutoLogout(true);
			return byteArray;
		}
		throw new Exception("No Camera Devices connected");
	}


	/**
	 * This method is to select the device and initialize document scan pop-up
	 */
	private void initializeAndShowScanPopup(boolean isPreviewOnly, String subType) {
		List<ScanDevice> devices = null;
		if(subType.equals(RegistrationConstants.PROOF_OF_SIGNATURE)) {
			devices = signatureFacade.getConnectedDevices();
		} else {
			devices = docScannerFacade.getConnectedDevices();
		}

		LOGGER.info("Connected devices : {}", devices);

		if (devices.isEmpty()) {
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND));
			return;
		}

		this.subType = subType;
		//selectedScanDeviceName = selectedScanDeviceName == null ? devices.get(0).getId() : selectedScanDeviceName;
		selectedScanDeviceName =  devices.get(0).getId() ;
		Optional<ScanDevice> result = devices.stream().filter(d -> d.getId().equals(selectedScanDeviceName)).findFirst();

		LOGGER.info("Selected device name : {}", selectedScanDeviceName);

		if(!result.isPresent()) {
			LOGGER.info("No devices found for the selected device name : {}", selectedScanDeviceName);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND));
			return;
		}

		scanPopUpViewController.docScanDevice = result.get();
		scanPopUpViewController.init(this,
				RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOC_TITLE));

		if(isPreviewOnly)
			scanPopUpViewController.setUpPreview();
		/*else
			scanPopUpViewController.docScanDevice = result.get();*/
	}

	public List<BufferedImage> getScannedPages() {
		return scannedPages;
	}

	public void setScannedPages(List<BufferedImage> scannedPages) {
		this.scannedPages = scannedPages;
	}

	public BufferedImage getScannedImage(int docPageNumber) {
		return scannedPages.get(docPageNumber <= 0 ? 0 : docPageNumber);
	}

	public boolean loadDataIntoScannedPages(String fieldId) throws IOException {
		DocumentDto documentDto = getRegistrationDTOFromSession().getDocuments().get(fieldId);
		if(documentDto == null) {
			this.scannedPages = new ArrayList<>();
			return false;
		}

		if (RegistrationConstants.PDF.equalsIgnoreCase(documentDto.getFormat())) {
			setScannedPages(DocScannerUtil.pdfToImages(documentDto.getDocument()));
			return true;
		} else {
			InputStream is = new ByteArrayInputStream(documentDto.getDocument());
			BufferedImage newBi = ImageIO.read(is);
			List<BufferedImage> list = new LinkedList<>();
			list.add(newBi);
			setScannedPages(list);
			return true;
		}
	}


	public void scanDocument(String fieldId, FxControl fxControl, boolean isPreviewOnly, String subType) {
		try {
			this.fxControl = fxControl;

			loadDataIntoScannedPages(fieldId);

			initializeAndShowScanPopup(isPreviewOnly, subType);

			LOGGER.info(RegistrationConstants.DOCUMNET_SCAN_CONTROLLER, RegistrationConstants.APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID, "Scan window displayed to scan and upload documents");
			return;

		} catch (IOException exception) {
			LOGGER.error(exception.getMessage() , exception);
		}

		generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_ERROR));
	}

	public String getSelectedScanDeviceName() {
		return selectedScanDeviceName;
	}

	public void setSelectedScanDeviceName(String selectedScanDeviceName) {
		this.selectedScanDeviceName = selectedScanDeviceName;
	}

	public FxControl getFxControl() {
		return fxControl;
	}

	public void setFxControl(FxControl fxControl) {
		this.fxControl = fxControl;
	}

}
