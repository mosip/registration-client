package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_IRIS_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_SCAN_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

import io.mosip.registration.api.docscanner.DocScannerFacade;
import io.mosip.registration.api.docscanner.DocScannerUtil;
import io.mosip.registration.api.docscanner.dto.DocScanDevice;
import io.mosip.registration.util.common.RectangleSelection;
import javafx.scene.layout.StackPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.util.control.FxControl;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Controller
public class ScanPopUpViewController extends BaseController implements Initializable {
	private static final Logger LOGGER = AppConfig.getLogger(ScanPopUpViewController.class);

	@Autowired
	private BaseController baseController;

	@Autowired
	private DocumentScanController documentScanController;

	@FXML
	private Label popupTitle;

	@FXML
	private Text totalScannedPages;

	@FXML
	private Button saveBtn;

	@FXML
	private Text scanningMsg;

	private boolean isDocumentScan;

	@Autowired
	private Streamer streamer;

	@FXML
	private Hyperlink closeButton;

	public TextField streamerValue;

	@Value("${mosip.doc.stage.width:1200}")
	private int width;

	@Value("${mosip.doc.stage.height:620}")
	private int height;

	@FXML
	protected Label docPreviewNext;

	@FXML
	protected Label docPreviewPrev;

	@FXML
	protected Text docCurrentPageNumber;

	@FXML
	protected GridPane previewOption;

	private Stage popupStage;

	@FXML
	private Button captureBtn;

	@FXML
	private Button cancelBtn;

	@FXML
	private Button cropButton;

	@FXML
	private Button streamBtn;
	@FXML
	private Button previewBtn;

	@FXML
	private GridPane imageViewGridPane;

	@FXML
	private ImageView scanImage;
	@FXML
	private ImageView closeImageView;
	@FXML
	private ImageView streamImageView;
	@FXML
	private ImageView captureImageView;
	@FXML
	private ImageView saveImageView;
	@FXML
	private ImageView backImageView1;
	@FXML
	private ImageView cancelImageView;	
	@FXML
	private ImageView previewImageView;

	public Group getImageGroup() {
		return imageGroup;
	}

	@FXML
	private Group imageGroup;

	@Autowired
	private BiometricsController biometricsController;

	private FxControl fxControl;

	private boolean isStreamPaused;

	@Autowired
	private DocScannerFacade docScannerFacade;

	public StackPane getGroupStackPane() {
		return groupStackPane;
	}

	@FXML
	private StackPane groupStackPane;
	public DocScanDevice docScanDevice;
	private RectangleSelection rectangleSelection = null;

	public boolean isStreamPaused() {
		return isStreamPaused;
	}

	private boolean isWebCamStream;

	public boolean isWebCamStream() {
		return isWebCamStream;
	}

	public void setWebCamStream(boolean isWebCamStream) {
		this.isWebCamStream = isWebCamStream;
	}

	/**
	 * @return the popupStage
	 */
	public Stage getPopupStage() {
		return popupStage;
	}

	/**
	 * @return the scanImage
	 */
	public ImageView getScanImage() {
		return scanImage;
	}

	/**
	 * @param popupStage the popupStage to set
	 */
	public void setPopupStage(Stage popupStage) {
		this.popupStage = popupStage;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		setImage(closeImageView	, RegistrationConstants.CLOSE_IMG);
		setImage(streamImageView	, RegistrationConstants.STREAM_IMG);
		setImage(captureImageView	, RegistrationConstants.SCAN_IMG);
		setImage(saveImageView	, RegistrationConstants.DWLD_PRE_REG_DATA_IMG);
		setImage(backImageView1	, RegistrationConstants.CROP_IMG);
		setImage(cancelImageView	, RegistrationConstants.REJECT_IMG);
		setImage(previewImageView	, RegistrationConstants.HOVER_IMG);
	}

	private void initializeDocPages(int currentPage, int totalPages) {
		docCurrentPageNumber.setText(String.valueOf(currentPage));
		totalScannedPages.setText(String.valueOf(totalPages));
		boolean prevPageDisable = currentPage > 1 ? false : true;
		docPreviewPrev.setDisable(prevPageDisable);
		boolean nextPageDisable = currentPage < totalPages ? false : true;
		docPreviewNext.setDisable(nextPageDisable);
	}



	/**
	 * This method will open popup to scan
	 * 
	 * @param parentControllerObj
	 * @param title
	 */
	public void init(BaseController parentControllerObj, String title) {
		try {
			LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Opening pop-up screen to scan for user registration");

			streamerValue = new TextField();
			baseController = parentControllerObj;
			popupStage = new Stage();
			popupStage.initStyle(StageStyle.UNDECORATED);

			LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "loading scan.fxml");
			Parent scanPopup = BaseController.load(getClass().getResource(RegistrationConstants.SCAN_PAGE));

			scanImage.setPreserveRatio(true);

			setDefaultImageGridPaneVisibility();
			popupStage.setResizable(false);
			popupTitle.setText(title);

			cropButton.setDisable(true);
			cancelBtn.setDisable(true);
			previewOption.setVisible(false);
			Scene scene = null;

			if (!isDocumentScan) {
				scene = new Scene(scanPopup);
				captureBtn.setVisible(false);
				saveBtn.setVisible(false);
				cancelBtn.setVisible(false);
				cropButton.setVisible(false);
				previewBtn.setVisible(false);
				streamBtn.setVisible(false);
			} else {
				LOGGER.info("Setting doc screen width : {}, height : {}", width, height);
				scene = new Scene(scanPopup, width, height);

				if (documentScanController.getScannedPages() != null
						&& !documentScanController.getScannedPages().isEmpty()) {
					initializeDocPages(1, documentScanController.getScannedPages().size());
					previewBtn.setDisable(false);
				}/* else {
					saveBtn.setDisable(true);
					cropButton.setDisable(true);
					cancelBtn.setDisable(true);
					previewBtn.setDisable(true);
				}*/
			}
			scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
			popupStage.setScene(scene);
			popupStage.initModality(Modality.WINDOW_MODAL);
			popupStage.initOwner(fXComponents.getStage());
			popupStage.show();

			LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "scan screen launched");

			scanningMsg.textProperty().addListener((observable, oldValue, newValue) -> {

				Platform.runLater(() -> {
					if (RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND).contains(newValue)) {

						// captureBtn.setDisable(false);

						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NO_DEVICE_FOUND));
						popupStage.close();

					}
				});

			});

			clearSelection();
			stopStreaming();

			LOGGER.debug(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
					"Opening pop-up screen to scan for user registration");

		} catch (IOException exception) {
			LOGGER.error(RegistrationConstants.USER_REG_SCAN_EXP, exception);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_SCAN_POPUP));
		}

	}



	@FXML
	public void preview() {
		clearSelection();
		stopStreaming();
		showPreview(true);
		getImageGroup().getChildren().clear();
		getImageGroup().getChildren().add(new ImageView(DocScannerUtil.getImage(documentScanController.getScannedImage(
				documentScanController.getScannedPages().size() - 1))));
		//setupImageView();
		showPagination();
	}


	@FXML
	public void stream() {
		clearSelection();
		showPreview(false);
		showStream(true);

		cancelBtn.setDisable(true);
		cropButton.setDisable(true);

		startStream();
	}

	/**
	 * This method will allow to scan
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	@FXML
	public void scan() throws MalformedURLException, IOException {
		stopStreaming();
		scanningMsg.setVisible(true);

		if(!isDocumentScan) {
			baseController.scan(popupStage);
			generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.DOC_CAPTURE_SUCCESS);
			return;
		}

		String docNumber = docCurrentPageNumber.getText();
		int currentPage = (docNumber == null || docNumber.isEmpty() || docNumber.equals("0")) ? 1 : Integer.valueOf(docNumber);

		if (currentPage <= 1) {
			docCurrentPageNumber.setText("1");
			docPreviewPrev.setDisable(true);
			docPreviewNext.setDisable(true);
		} else {
			docCurrentPageNumber.setText(String.valueOf(documentScanController.getScannedPages().size()));
			docPreviewPrev.setDisable(false);
			docPreviewNext.setDisable(true);
		}

		LOGGER.info("Invoke scan method for the passed controller");

		if(rectangleSelection != null) {
			save(rectangleSelection.getBounds(), documentScanController.getScannedPages().get(currentPage - 1));
		}
		else {
			documentScanController.scan(popupStage);
			docCurrentPageNumber.setText(String.valueOf(documentScanController.getScannedPages().size()));
		}
		totalScannedPages.setText(String.valueOf(documentScanController.getScannedPages() == null ? 0 : documentScanController.getScannedPages().size()));
		clearSelection();
	}

	@FXML
	private void save() {
		clearSelection();
		stopStreaming();
		//setDefaultImageGridPaneVisibility();
		// Enable Auto-Logout
		SessionContext.setAutoLogout(true);
		if (baseController instanceof DocumentScanController) {
			DocumentScanController documentScanController = (DocumentScanController) baseController;
			try {

				documentScanController.getFxControl().setData(documentScanController.getScannedPages());
				documentScanController.getScannedPages().clear();
				popupStage.close();

			} catch (RuntimeException exception) {
				LOGGER.error("Failed to set data in documentDTO", exception);
				generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_ERROR));
			}
		}
		showPagination();
	}

	@FXML
	public void crop() {
		clearSelection();
		stopStreaming();
		scanImage.setVisible(true);
		rectangleSelection = new RectangleSelection(imageGroup);
		LOGGER.debug("Shown stage for crop");
	}


	@FXML
	public void cancel() {
		clearSelection();
		stopStreaming();
		int currentDocPageNumber = Integer.valueOf(docCurrentPageNumber.getText());
		int pageNumberIndex = currentDocPageNumber - 1;

		// Remove current page
		documentScanController.getScannedPages().remove(pageNumberIndex);

		generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.DOC_DELETE_SUCCESS));
		// If first page
		if (currentDocPageNumber == 1) {

			// Remove current doc
			if (documentScanController.getScannedPages().size() > 0) {

				BufferedImage bufferedImage = documentScanController.getScannedImage(0);

				if (bufferedImage != null) {

					getImageGroup().getChildren().clear();
					getImageGroup().getChildren().add(new ImageView(DocScannerUtil.getImage(bufferedImage)));
				}

				initializeDocPages(1, documentScanController.getScannedPages().size());

			} else {

				scanImage.setImage(null);

				initializeDocPages(0, 0);

				previewOption.setVisible(false);

			}
		}

		// If last page
		else if (currentDocPageNumber == documentScanController.getScannedPages().size() + 1) {

			BufferedImage bufferedImage = documentScanController.getScannedImage(pageNumberIndex - 1);

			if (bufferedImage != null) {

				getImageGroup().getChildren().clear();
				getImageGroup().getChildren().add(new ImageView(DocScannerUtil.getImage(bufferedImage)));

				initializeDocPages(currentDocPageNumber - 1, documentScanController.getScannedPages().size());

			}
		}

		// If middle page
		else {
			BufferedImage bufferedImage = documentScanController.getScannedImage(pageNumberIndex);

			if (bufferedImage != null) {

				getImageGroup().getChildren().clear();
				getImageGroup().getChildren().add(new ImageView(DocScannerUtil.getImage(bufferedImage)));

				initializeDocPages(currentDocPageNumber, documentScanController.getScannedPages().size());

			}
		}

		if (!documentScanController.getScannedPages().isEmpty()) {
			previewBtn.setDisable(false);
			saveBtn.setDisable(false);
		} else {
			previewBtn.setDisable(true);
			saveBtn.setDisable(true);
			cancelBtn.setDisable(true);
			cropButton.setDisable(true);
		}
	}

	private void stopStreaming() {
		isStreamPaused = true;
		setWebCamStream(false);
	}


	private void showPagination() {
		if(!isDocumentScan)
			return;

		String docNumber = docCurrentPageNumber.getText();
		totalScannedPages.setText(String.valueOf(documentScanController.getScannedPages().size()));
		if (docNumber.isEmpty() || docNumber.equals("0")) {
			docCurrentPageNumber.setText("1");
			docPreviewPrev.setDisable(true);
			docPreviewNext.setDisable(true);
		} else {
			docCurrentPageNumber.setText(String.valueOf(documentScanController.getScannedPages().size()));
			docPreviewPrev.setDisable(false);
			docPreviewNext.setDisable(true);
		}
	}




	/**
	 * event class to exit from present pop up window.
	 *
	 * @param event
	 */
	public void exitWindow(ActionEvent event) {

		LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Calling exit window to close the popup");

		biometricsController.stopRCaptureService();
		biometricsController.stopDeviceSearchService();
		streamer.stop();

		popupStage = (Stage) ((Node) event.getSource()).getParent().getScene().getWindow();
		popupStage.close();

		if (documentScanController.getScannedPages() != null) {
			documentScanController.getScannedPages().clear();
		}

		LOGGER.info(LOG_REG_SCAN_CONTROLLER, APPLICATION_NAME, APPLICATION_ID, "Popup is closed");

	}

	public void enableCloseButton() {
		if (null != closeButton)
			closeButton.setDisable(false);
	}

	public void disableCloseButton() {
		if (null != closeButton)
			closeButton.setDisable(true);
	}

	public boolean isDocumentScan() {
		return isDocumentScan;
	}

	public void setDocumentScan(boolean isDocumentScan) {
		this.isDocumentScan = isDocumentScan;
	}

	public Text getScanningMsg() {
		return scanningMsg;
	}

	public void setScanningMsg(String msg) {
		if (scanningMsg != null) {
			scanningMsg.setText(msg);
			scanningMsg.getStyleClass().add("scanButton");
		}
	}

	public void setDefaultImageGridPaneVisibility() {
		LOGGER.info(LOG_REG_IRIS_CAPTURE_CONTROLLER, APPLICATION_NAME, APPLICATION_ID,
				"Setting default visibilities for webCamParent and imageParent");
//		webcamParent.setVisible(false);
//		imageParent.setVisible(true);
	}

	/**
	 * This method will preview the next document
	 */
	public void previewNextPage() {

		Integer docNumber = getDocPreviewNumber();

		if (docNumber != null && docNumber > 0) {

			int previousDocNumber = docNumber;

			BufferedImage bufferedImage = null;
			if (documentScanController.getScannedPages().size() > (previousDocNumber)) {

				bufferedImage = documentScanController.getScannedImage(previousDocNumber);

			}

			if (bufferedImage != null) {

				getImageGroup().getChildren().clear();
				getImageGroup().getChildren().add(new ImageView(DocScannerUtil.getImage(bufferedImage)));

				docCurrentPageNumber.setText(String.valueOf(previousDocNumber + 1));

				docPreviewPrev.setDisable(false);

				if (documentScanController.getScannedPages().size() == (previousDocNumber + 1)) {

					docPreviewNext.setDisable(true);
				}

			}

		}
	}

	/**
	 * This method will preview the previous document
	 */
	public void previewPrevPage() {

		Integer docNumber = getDocPreviewNumber();

		if (docNumber != null && docNumber > 0) {

			int previousDocNumber = docNumber - 2;

			BufferedImage bufferedImage = null;

			if (documentScanController.getScannedPages().size() > (previousDocNumber)) {

				bufferedImage = documentScanController.getScannedImage(previousDocNumber);

			}

			if (bufferedImage != null) {

				getImageGroup().getChildren().clear();
				getImageGroup().getChildren().add(new ImageView(DocScannerUtil.getImage(bufferedImage)));
				docCurrentPageNumber.setText(String.valueOf(docNumber - 1));

				docPreviewNext.setDisable(false);

				if ((previousDocNumber) == 0) {

					docPreviewPrev.setDisable(true);
				}
			}

		}
	}

	private Integer getDocPreviewNumber() {

		Integer docNumber = null;
		String docPreviewNumber = docCurrentPageNumber.getText();

		if (docPreviewNumber != null && !docPreviewNumber.isEmpty()) {

			docNumber = Integer.valueOf(docPreviewNumber);

		}

		return docNumber;
	}



	public void save(Bounds bounds, BufferedImage bufferedImage) {

		LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Saving cropped image");

		if (bounds.getHeight() == 1.0 || bounds.getWidth() == 1.0)
			return;

		bufferedImage = bufferedImage.getSubimage((int)bounds.getMinX(), (int)bounds.getMinY(), (int) bounds.getWidth(),
				(int) bounds.getHeight());

		clearSelection();

		int pageNumber = Integer
				.valueOf(docCurrentPageNumber.getText().isEmpty() ? "1" : docCurrentPageNumber.getText());

		documentScanController.getScannedPages().remove(pageNumber - 1);
		documentScanController.getScannedPages().add(pageNumber - 1, bufferedImage);

		getImageGroup().getChildren().clear();
		getImageGroup().getChildren().add(new ImageView(DocScannerUtil.getImage(bufferedImage)));
		getScanningMsg().setVisible(false);
		generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.CROP_DOC_SUCCESS));
		LOGGER.debug("REGISTRATION - DOCUMENT_SCAN_CONTROLLER", APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
				"Saving cropped image completed");
	}


	public void startStream() {
		isStreamPaused = false;
		setWebCamStream(true);
		Thread streamer_thread = new Thread(new Runnable() {
			public void run() {
				while (isWebCamStream()) {
					try {
						if (!isStreamPaused()) {
							getImageGroup().getChildren().clear();
							getImageGroup().getChildren().add(new ImageView(DocScannerUtil.getImage(docScannerFacade.scanDocument(docScanDevice))));
						}
					} catch (NullPointerException exception) {
						LOGGER.error("Error while streaming the captured photo", exception);
						setWebCamStream(false);
					}
				}
			}
		});
		streamer_thread.start();
	}

	public void showPreview(boolean isVisible) {
		previewOption.setVisible(isVisible);
		scanImage.setVisible(true);
		cancelBtn.setDisable(false);
		cropButton.setDisable(false);
	}

	private void showStream(boolean isVisible) {
		isStreamPaused = false;
	}

	private void clearSelection() {
		if(rectangleSelection != null) {
			rectangleSelection.removeEventHandlers();
			rectangleSelection = null;
		}
		imageGroup.getChildren().remove(1,imageGroup.getChildren().size());
	}

	/*private void setupImageView() {
		scanImage.setVisible(true);
		scanImage.setPreserveRatio(true);
		scanImage.fitWidthProperty().bind(scanImage.getImage().widthProperty());
		scanImage.fitHeightProperty().bind(scanImage.getImage().heightProperty());
	}*/

	public void setUpPreview() {
		saveBtn.setDisable(true);
		cropButton.setDisable(true);
		cancelBtn.setDisable(true);
		captureBtn.setDisable(true);

		streamBtn.setDisable(true);
		previewBtn.setDisable(false);
		previewOption.setVisible(true);
		preview();
	}
}
