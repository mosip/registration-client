package io.mosip.registration.controller.device;

import static io.mosip.registration.constants.LoggerConstants.LOG_REG_IRIS_CAPTURE_CONTROLLER;
import static io.mosip.registration.constants.LoggerConstants.LOG_REG_SCAN_CONTROLLER;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

import io.mosip.registration.api.docscanner.DocScannerFacade;
import io.mosip.registration.api.docscanner.DocScannerUtil;
import io.mosip.registration.api.docscanner.dto.DocScanDevice;
import io.mosip.registration.util.common.RectangleSelection;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.util.control.FxControl;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Controller
public class ScanPopUpViewController extends BaseController implements Initializable {
	private static final Logger LOGGER = AppConfig.getLogger(ScanPopUpViewController.class);

	/*@FXML
	private Label popupTitle;*/
	@FXML
	private Text totalScannedPages;
	@FXML
	private Button saveBtn;
	@FXML
	private Text scanningMsg;
	/*@FXML
	private Hyperlink closeButton;*/
	@FXML
	protected Label docPreviewNext;
	@FXML
	protected Label docPreviewPrev;
	@FXML
	protected Text docCurrentPageNumber;
	@FXML
	protected GridPane previewOption;
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
	/*@FXML
	private ImageView closeImageView;*/
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
	@FXML
	private Group imageGroup;
	@FXML
	private StackPane groupStackPane;
	@FXML
	private ScrollPane docPreviewScrollPane;

	@Autowired
	private BaseController baseController;
	@Autowired
	private Streamer streamer;
	@Autowired
	private DocumentScanController documentScanController;
	@Autowired
	private DocScannerFacade docScannerFacade;

	@Value("${mosip.doc.stage.width:1200}")
	private int width;

	@Value("${mosip.doc.stage.height:620}")
	private int height;

	private Thread streamer_thread = null;
	private Stage popupStage;
	public TextField streamerValue;
	private FxControl fxControl;
	private boolean isWebCamStream;
	private boolean isStreamPaused;
	public DocScanDevice docScanDevice;
	private RectangleSelection rectangleSelection = null;
	final DoubleProperty zoomProperty = new SimpleDoubleProperty(200);

	public Group getImageGroup() {
		return imageGroup;
	}

	public StackPane getGroupStackPane() {
		return groupStackPane;
	}

	public boolean isStreamPaused() {
		return isStreamPaused;
	}

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
		
		//setImage(closeImageView	, RegistrationConstants.CLOSE_IMG);
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
			streamerValue = new TextField();
			baseController = parentControllerObj;

			LOGGER.info("Loading Document scan page : {}", RegistrationConstants.SCAN_PAGE);
			Parent scanPopup = BaseController.load(getClass().getResource(RegistrationConstants.SCAN_PAGE));
			scanImage.setPreserveRatio(true);
			//popupTitle.setText(title);
			cropButton.setDisable(true);
			cancelBtn.setDisable(true);
			previewOption.setVisible(false);

			LOGGER.info("Setting doc screen width :{}, height: {}", width, height);
			Scene scene = new Scene(scanPopup, width, height);
			if (documentScanController.getScannedPages() != null
					&& !documentScanController.getScannedPages().isEmpty()) {
				initializeDocPages(1, documentScanController.getScannedPages().size());
				previewBtn.setDisable(false);
			} else {
				saveBtn.setDisable(true);
				cropButton.setDisable(true);
				cancelBtn.setDisable(true);
				previewBtn.setDisable(true);
			}
			scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(getCssName()).toExternalForm());
			popupStage = new Stage();
			//popupStage.setResizable(true);
			//popupStage.setFullScreen(true);
			popupStage.setAlwaysOnTop(true);
			//popupStage.initStyle(StageStyle.UNDECORATED);
			popupStage.setScene(scene);
			popupStage.initModality(Modality.WINDOW_MODAL);
			popupStage.initOwner(fXComponents.getStage());
			popupStage.setTitle(title);
			popupStage.setMinHeight(docScanDevice.getHeight());
			popupStage.setMinWidth(docScanDevice.getWidth());
			popupStage.show();

			LOGGER.debug("scan screen launched");
			scanningMsg.textProperty().addListener((observable, oldValue, newValue) -> {
				Platform.runLater(() -> {
					if (RegistrationUIConstants.NO_DEVICE_FOUND.contains(newValue)) {
						// captureBtn.setDisable(false);
						generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.NO_DEVICE_FOUND);
						popupStage.close();
					}
				});
			});

			rectangleSelection = null;
			clearSelection();
			setScanImageViewZoomable();

			LOGGER.info("Opening pop-up screen to scan for user registration");

		} catch (IOException exception) {
			LOGGER.error(RegistrationConstants.USER_REG_SCAN_EXP, exception);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_SCAN_POPUP));
		}
	}


	private void setScanImageViewZoomable() {
		zoomProperty.addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable arg0) {
				if(getImageGroup().getChildren().size() > 0) {
					((ImageView)getImageGroup().getChildren().get(0)).setFitWidth(zoomProperty.get() * 4);
					((ImageView)getImageGroup().getChildren().get(0)).setFitHeight(zoomProperty.get() * 3);
				}
			}
		});

		docPreviewScrollPane.addEventFilter(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				if (event.getDeltaY() > 0) {
					zoomProperty.set(zoomProperty.get() * 1.1);
				} else if (event.getDeltaY() < 0) {
					zoomProperty.set(zoomProperty.get() / 1.1);
				}
			}
		});
	}


	@FXML
	public void preview() {
		setWebCamStream(false);
		clearSelection();
		showPreview(true);

		if(documentScanController.getScannedPages() != null && !documentScanController.getScannedPages().isEmpty()) {
			initializeDocPages(1, documentScanController.getScannedPages().size());
			getImageGroup().getChildren().clear();
			getImageGroup().getChildren().add(new ImageView(getImage(documentScanController.getScannedPages().get(0))));
		}
	}


	@FXML
	public void stream() {
		clearSelection();

		showPreview(false);
		showStream(true);
		cancelBtn.setDisable(true);
		cropButton.setDisable(true);

		if(getImageGroup().getChildren().isEmpty())
			getImageGroup().getChildren().add(new ImageView());
		this.scanImage = (ImageView)getImageGroup().getChildren().get(0);
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
		LOGGER.info("Invoke scan method for the passed controller");
		scanningMsg.setVisible(true);
		setWebCamStream(false);
		String docNumber = docCurrentPageNumber.getText();
		int currentPage = (docNumber == null || docNumber.isEmpty() || docNumber.equals("0")) ? 1 : Integer.valueOf(docNumber);

		if(rectangleSelection != null) {
			save(rectangleSelection.getBounds(), documentScanController.getScannedPages().get(currentPage - 1));
		}
		else {
			baseController.scan(popupStage);
			currentPage = documentScanController.getScannedPages().size();
		}

		showPreview(true);
		if(documentScanController.getScannedPages() != null && !documentScanController.getScannedPages().isEmpty()) {
			int totalCount = documentScanController.getScannedPages().size();
			initializeDocPages(currentPage, totalCount);
			getImageGroup().getChildren().clear();
			getImageGroup().getChildren().add(new ImageView(getImage(documentScanController.getScannedPages().get(currentPage-1))));
		}
		saveBtn.setDisable(false);
	}

	@FXML
	private void save() {
		stopStreaming();
		// Enable Auto-Logout
		SessionContext.setAutoLogout(true);
		try {

			if(rectangleSelection != null) {
				String docNumber = docCurrentPageNumber.getText();
				int currentPage = (docNumber == null || docNumber.isEmpty() || docNumber.equals("0")) ? 1 : Integer.valueOf(docNumber);
				save(rectangleSelection.getBounds(), documentScanController.getScannedPages().get(currentPage - 1));
			}

			documentScanController.getFxControl().setData(documentScanController.getScannedPages());
			documentScanController.getScannedPages().clear();
			popupStage.close();

		} catch (RuntimeException exception) {
			LOGGER.error("Failed to set data in documentDTO", exception);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_ERROR));
		}
		showPagination();
	}

	@FXML
	public void crop() {
		setWebCamStream(false);
		clearSelection();
		scanImage.setVisible(true);
		rectangleSelection = new RectangleSelection(imageGroup);
		LOGGER.debug("Shown stage for crop");
	}


	@FXML
	public void cancel() {
		clearSelection();
		setWebCamStream(false);

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
				getImageGroup().getChildren().clear();
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
		try {
			setWebCamStream(false);
			isStreamPaused = true;
			if(streamer_thread != null)
				streamer_thread.interrupt();
		} finally {
			docScannerFacade.stopDevice(this.docScanDevice);
		}
	}


	private void showPagination() {
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
		LOGGER.info("Calling exit window to close the popup");
		stopStreaming();
		clearSelection();
		streamer.stop();

		popupStage = (Stage) ((Node) event.getSource()).getParent().getScene().getWindow();
		popupStage.close();

		if (documentScanController.getScannedPages() != null) {
			documentScanController.getScannedPages().clear();
		}

		LOGGER.info("Scan Popup is closed");

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
		clearSelection();

		LOGGER.debug("Saving cropped image");

		if (bounds.getHeight() == 1.0 || bounds.getWidth() == 1.0)
			return;

		bufferedImage = bufferedImage.getSubimage((int)bounds.getMinX(), (int)bounds.getMinY(), (int) bounds.getWidth(),
				(int) bounds.getHeight());

		int pageNumber = Integer
				.valueOf(docCurrentPageNumber.getText().isEmpty() ? "1" : docCurrentPageNumber.getText());

		documentScanController.getScannedPages().remove(pageNumber - 1);
		documentScanController.getScannedPages().add(pageNumber - 1, bufferedImage);

		getImageGroup().getChildren().clear();
		getImageGroup().getChildren().add(new ImageView(DocScannerUtil.getImage(bufferedImage)));
		getScanningMsg().setVisible(false);
		generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.CROP_DOC_SUCCESS));
		LOGGER.debug("Saving cropped image completed");
	}

	private void startStream() {
		if(streamer_thread != null) {
			streamer_thread.interrupt();
			streamer_thread = null;
		}

		setWebCamStream(true);
		isStreamPaused = false;
		streamer_thread = new Thread(new Runnable() {
			public void run() {
				while (isWebCamStream()) {
					try {
						if (!isStreamPaused()) {
							getScanImage().setImage(DocScannerUtil.getImage(docScannerFacade.scanDocument(docScanDevice)));
						}
					} catch (Throwable t) {
						LOGGER.error("Error while streaming the captured photo", t);
						setWebCamStream(false);
					}
				}
			}
		});
		streamer_thread.start();
	}

	public void showPreview(boolean isVisible) {
		isStreamPaused = true;
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

	public void setUpPreview() {
		previewOption.setVisible(true);
		preview();
		//Disable all buttons in document preview
		saveBtn.setDisable(true);
		captureBtn.setDisable(true);
		streamBtn.setDisable(true);
		cancelBtn.setDisable(true);
		cropButton.setDisable(true);
		previewBtn.setDisable(true);
	}
}
