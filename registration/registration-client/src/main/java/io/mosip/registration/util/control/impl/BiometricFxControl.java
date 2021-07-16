package io.mosip.registration.util.control.impl;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import io.mosip.registration.api.docscanner.DocScannerUtil;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.GenericController;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.packetmanager.BiometricsDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.dto.schema.ConditionalBioAttributes;
import io.mosip.registration.service.bio.BioService;
import io.mosip.registration.enums.Modality;
import javafx.scene.layout.*;
import org.apache.commons.collections4.ListUtils;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.device.GenericBiometricsController;
import io.mosip.registration.dto.schema.UiSchemaDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.IdentitySchemaService;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.mvel2.MVEL;

import javax.imageio.ImageIO;

public class BiometricFxControl extends FxControl {

	protected static final Logger LOGGER = AppConfig.getLogger(BiometricFxControl.class);

	private GenericBiometricsController biometricsController;
	private IdentitySchemaService identitySchemaService;
	private BioService bioService;
	private Modality currentModality;
	private Map<Modality, List<List<String>>> modalityAttributeMap = new HashMap<>();
	private List<UiSchemaDTO> exceptionProofFields;


	public BiometricFxControl(List<UiSchemaDTO> biometricExceptionProofFields) {
		org.springframework.context.ApplicationContext applicationContext = Initialization.getApplicationContext();
		this.biometricsController = applicationContext.getBean(GenericBiometricsController.class);
		this.identitySchemaService = applicationContext.getBean(IdentitySchemaService.class);
		this.bioService = applicationContext.getBean(BioService.class);
		this.requiredFieldValidator = applicationContext.getBean(RequiredFieldValidator.class);
		this.auditFactory = applicationContext.getBean(AuditManagerService.class);
		exceptionProofFields = biometricExceptionProofFields;
	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {
		this.uiSchemaDTO = uiSchemaDTO;
		this.control = this;
		this.node = create();
		return this.node == null ? null : this.control;
	}

	@Override
	public void setData(Object data) {
		if (data != null) {
			getRegistrationDTo().addAllBiometrics(uiSchemaDTO.getSubType(), currentModality,
					(Map<String, BiometricsDto>) data, bioService.getMDMQualityThreshold(currentModality),
					bioService.getRetryCount(currentModality));
		}

		//remove POE_EOP document if there are no biometric exception
		if(getRegistrationDTo().getBiometricExceptions(uiSchemaDTO.getSubType()).isEmpty()) {
			LOGGER.info("Removing exception photo as no exceptions are marked currently");
			biometricsController.deleteProofOfExceptionDocument();
		}
	}

	@Override
	public void fillData(Object data) {

	}

	@Override
	public Object getData() {
		List<String> requiredAttributes = requiredFieldValidator.getRequiredBioAttributes(uiSchemaDTO, getRegistrationDTo());
		List<String> configBioAttributes = ListUtils.intersection(requiredAttributes,
				Modality.getAllBioAttributes(currentModality));
		return getRegistrationDTo().getBiometric(uiSchemaDTO.getSubType(), configBioAttributes);
	}

	@Override
	public void setListener(Node node) {

	}

	@Override
	public void selectAndSet(Object data) {

	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}

	private GridPane create() {
		String fieldName = this.uiSchemaDTO.getId();
		List<HBox> modalityList = getModalityButtons();

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(langCode -> {
					labels.add(this.uiSchemaDTO.getLabel().get(langCode));
				});

		Label label = new Label();
		label.setText(String.join(RegistrationConstants.SLASH, labels));
		label.getStyleClass().add(RegistrationConstants.QUALITY_BOX_LABEL);

		GridPane gridPane = createGridPane();
		gridPane.setHgap(10);
		gridPane.setId(fieldName);
		gridPane.add(label,1,0);
		GridPane.setHalignment(label, HPos.CENTER);
		GridPane.setValignment(label, VPos.TOP);

		Node modalityListingNode = null;
		switch (getBiometricFieldLayout()) {
			case "compact":
				modalityListingNode = new HBox();
				modalityListingNode.setId(uiSchemaDTO.getId()+"_listing");
				//((HBox)modalityListingNode).setSpacing(5);
				((HBox)modalityListingNode).getChildren().addAll(modalityList);
				gridPane.add(modalityListingNode,1,1);
				break;
			default:
				modalityListingNode = new VBox();
				modalityListingNode.setId(uiSchemaDTO.getId()+"_listing");
				//((VBox)modalityListingNode).setSpacing(5);
				((VBox)modalityListingNode).getChildren().addAll(modalityList);
				gridPane.add(modalityListingNode,0,1);
				Parent captureDetails = null;
				try {
					captureDetails = BaseController.loadWithNewInstance(getClass().getResource("/fxml/BiometricsCapture.fxml"),
							this.biometricsController);

					captureDetails.visibleProperty().bind(gridPane.visibleProperty());
					captureDetails.managedProperty().bind(gridPane.managedProperty());

					GridPane.setHalignment(captureDetails, HPos.CENTER);
					GridPane.setValignment(captureDetails, VPos.TOP);
					gridPane.add(captureDetails,1,1);
				} catch (IOException e) {
					LOGGER.error("Failed to load biometrics capture details page", e);
				}
				scanForModality(this.control, currentModality);
				break;
		}

		modalityListingNode.visibleProperty().bind(gridPane.visibleProperty());
		modalityListingNode.managedProperty().bind(gridPane.managedProperty());
		return gridPane;
	}

	private String getBiometricFieldLayout() {
		return this.uiSchemaDTO.getFieldLayout() == null ? "default" : this.uiSchemaDTO.getFieldLayout();
	}

	private GridPane createGridPane() {
		GridPane gridPane = new GridPane();
		
		gridPane.setPadding(new Insets(50, 0, 0, 0));
		RowConstraints topRowConstraints = new RowConstraints();
		topRowConstraints.setPercentHeight(5);
		RowConstraints midRowConstraints = new RowConstraints();
		midRowConstraints.setPercentHeight(95);
		gridPane.getRowConstraints().addAll(topRowConstraints,midRowConstraints);

		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(15);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(80);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(5);
		gridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2,columnConstraint3);
		return gridPane;
	}

	private List<HBox> getModalityButtons() {
		this.currentModality = null;
		List<HBox> modalityList = new ArrayList<>();
		List<String> requiredBioAttributes = requiredFieldValidator.getRequiredBioAttributes(this.uiSchemaDTO, getRegistrationDTo());
		//if(!requiredBioAttributes.isEmpty()) {
			for(Modality modality : Modality.values()) {
				List<String> modalityAttributes = ListUtils.intersection(requiredBioAttributes, modality.getAttributes());
				modalityAttributeMap.put(modality, new ArrayList<>());
				modalityAttributeMap.get(modality).add(modalityAttributes);
				modalityAttributeMap.get(modality).add(ListUtils.subtract(modality.getAttributes(), requiredBioAttributes));

				if(!modalityAttributes.isEmpty()) {
					HBox modalityView = new HBox();
					modalityView.setSpacing(10);
					modalityView.setId(uiSchemaDTO.getId() + modality);

					modalityView.getChildren().add(addModalityButton(modality));
					modalityList.add(modalityView);

					if(currentModality == null)
						currentModality = modality;

					addRemoveCaptureStatusMark(modalityView, modality);
				}
			}

			if(!modalityList.isEmpty())
				createExceptionPhoto(modalityList);
		//}
		return modalityList;
	}

	private ImageView getImageView(Image image, double height) {
		ImageView imageView = new ImageView(image);
		imageView.setFitHeight(height);
		imageView.setFitWidth(height);
		imageView.setPreserveRatio(true);
		return imageView;
	}

	private Button addModalityButton(Modality modality) {
		Button button = new Button();
		button.setMaxSize(100, 90);
		button.setMinSize(100, 90);
		button.setPrefSize(100, 90);

		List<BiometricsDto> capturedData = getRegistrationDTo().getBiometric(uiSchemaDTO.getSubType(), modality.getAttributes());
		
		try {
			Image image = null;
			if(Modality.EXCEPTION_PHOTO == modality) {
				image = getExceptionDocumentAsImage();
			}
			image = !capturedData.isEmpty() ? biometricsController.getBioStreamImage(uiSchemaDTO.getSubType(), modality,
						capturedData.get(0).getNumOfRetries()) : (image == null ? biometricsController.getImage(getImageIconPath(modality.name()),true) : image);
			button.setGraphic(getImageView(image, 80));
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Exception while Getting Image", exception);
		}
		
		button.getStyleClass().add(RegistrationConstants.MODALITY_BUTTONS);
		Tooltip tooltip = new Tooltip(ApplicationContext.getInstance().getBundle(ApplicationContext.applicationLanguage(),
				RegistrationConstants.LABELS).getString(modality.name()));
		tooltip.getStyleClass().add(RegistrationConstants.TOOLTIP_STYLE);
		button.setTooltip(tooltip);
		button.setOnAction(getModalityActionHandler(this, modality));
		return button;
	}


	private EventHandler getModalityActionHandler(BiometricFxControl control, Modality modality) {
		return new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				currentModality = modality;
				scanForModality(control, currentModality);
			}
		};
	}
	

	private void scanForModality(FxControl control, Modality modality) {
		LOGGER.info("Clicked on modality {} for field {}", modality, control.getUiSchemaDTO().getId());

		//this is null when nothing to be captured for this field, so ignore
		if(modality == null)
			return;

		List<String> requiredAttributes = requiredFieldValidator.getRequiredBioAttributes(uiSchemaDTO, getRegistrationDTo());
		List<String> configBioAttributes = ListUtils.intersection(requiredAttributes, Modality.getAllBioAttributes(modality));
		List<String> nonConfigBioAttributes = ListUtils.subtract(Modality.getAllBioAttributes(modality), configBioAttributes);

		switch (getBiometricFieldLayout()) {
			case "compact" :
				try {
					biometricsController.init(control, uiSchemaDTO.getSubType(), modality,
							configBioAttributes,nonConfigBioAttributes);
				} catch (IOException e) {
					LOGGER.error("Failed to load GenericBiometricFXML.fxml", e);
				}
				break;
			default :
				biometricsController.initializeWithoutStage(control, uiSchemaDTO.getSubType(), modality,
						configBioAttributes,nonConfigBioAttributes);
		}
	}

	public String getImageIconPath(String modality) {
		String imageIconPath = RegistrationConstants.DEFAULT_EXCEPTION_IMG;

		if (modality != null) {
			switch (modality) {
				case RegistrationConstants.FACE:
					imageIconPath = RegistrationConstants.FACE_IMG;
					break;
				case RegistrationConstants.IRIS_DOUBLE:
					imageIconPath = RegistrationConstants.DOUBLE_IRIS_IMG;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_RIGHT:
					imageIconPath = RegistrationConstants.RIGHTPALM_IMG;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_LEFT:
					imageIconPath = RegistrationConstants.LEFTPALM_IMG;
					break;
				case RegistrationConstants.FINGERPRINT_SLAB_THUMBS:
					imageIconPath = RegistrationConstants.THUMB_IMG;
					break;
			}
		}
		return imageIconPath;
	}

	@Override
	public void refresh() {
		LOGGER.error("Refreshing biometric field {} ", uiSchemaDTO.getId());

		List<HBox> modalityButtons = getModalityButtons();
		if(modalityButtons.isEmpty()) {
			this.node.setVisible(false);
			this.node.setManaged(false);
		}
		else {
			Node listingNode = this.node.lookup(RegistrationConstants.HASH+uiSchemaDTO.getId()+"_listing");
			if(listingNode instanceof HBox) {
				((HBox)listingNode).getChildren().clear();
				((HBox)listingNode).getChildren().addAll(modalityButtons);
			}

			if(listingNode instanceof VBox) {
				((VBox)listingNode).getChildren().clear();
				((VBox)listingNode).getChildren().addAll(modalityButtons);
			}

			this.node.setVisible(true);
			this.node.setManaged(true);

			scanForModality(this.control, this.currentModality);
		}

		List<String> requiredAttributes = requiredFieldValidator.getRequiredBioAttributes(uiSchemaDTO, getRegistrationDTo());
		for(String bioAttribute : uiSchemaDTO.getBioAttributes()) {
			if(!requiredAttributes.contains(bioAttribute)) {
				getRegistrationDTo().clearBIOCache(uiSchemaDTO.getSubType(), bioAttribute);
			}
		}
	}


	@Override
	public boolean canContinue() {
		Map<String, Boolean> capturedDetails = bioService.getCapturedBiometrics(uiSchemaDTO.getId(),
				getRegistrationDTo().getIdSchemaVersion(), getRegistrationDTo());

		String expression = String.join(" && ", uiSchemaDTO.getBioAttributes());
		ConditionalBioAttributes selectedCondition = requiredFieldValidator.getConditionalBioAttributes(uiSchemaDTO,
				getRegistrationDTo());
		if(selectedCondition != null) {
			expression = selectedCondition.getValidationExpr();
		}

		LOGGER.debug("Validating {} expr : {} with context : {}", uiSchemaDTO.getId(), expression, capturedDetails);
		boolean valid = MVEL.evalToBoolean(expression, capturedDetails);
		valid =  biometricsController.hasApplicantBiometricException() ? valid && biometricsController.isBiometricExceptionProofCollected() : valid;
		//TODO - display message about exception proof
		if (valid) {
			auditFactory.audit(AuditEvent.REG_BIO_CAPTURE_NEXT, Components.REG_BIOMETRICS, SessionContext.userId(),
					AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		}
		return valid;
	}

	private void createExceptionPhoto(List<HBox> modalityList) {
		if(exceptionProofFields == null || exceptionProofFields.isEmpty())
			return;

		HBox modalityView = new HBox();
		modalityView.setSpacing(10);
		modalityView.setId(uiSchemaDTO.getId() + Modality.EXCEPTION_PHOTO);

		Button button = new Button();
		button.setMaxSize(100, 90);
		button.setMinSize(100, 90);
		button.setPrefSize(100, 90);
		try {
			Image image = getExceptionDocumentAsImage();
			image = image != null ? image : biometricsController.getImage(getImageIconPath(Modality.EXCEPTION_PHOTO.name()),true);
			button.setGraphic(getImageView(image, 80));
		} catch (RegBaseCheckedException exception) {
			LOGGER.error("Exception while Getting Image", exception);
		}
		button.getStyleClass().add(RegistrationConstants.MODALITY_BUTTONS);
		Tooltip tooltip = new Tooltip(ApplicationContext.getInstance().getBundle(ApplicationContext.applicationLanguage(),
				RegistrationConstants.LABELS).getString(Modality.EXCEPTION_PHOTO.name()));
		tooltip.getStyleClass().add(RegistrationConstants.TOOLTIP_STYLE);
		button.setTooltip(tooltip);
		button.setOnAction(getModalityActionHandler(this, Modality.EXCEPTION_PHOTO));

		modalityView.getChildren().add(addModalityButton(Modality.EXCEPTION_PHOTO));
		addRemoveCaptureStatusMark(modalityView, Modality.EXCEPTION_PHOTO);

		boolean exceptionExists = biometricsController.hasApplicantBiometricException();
		modalityView.setVisible(exceptionExists);
		modalityView.setManaged(exceptionExists);
		modalityView.getChildren().forEach( child -> {
			child.setVisible(exceptionExists);
			child.setManaged(exceptionExists);
		});
		modalityList.add(modalityView);
	}

	public void displayExceptionPhoto() {
		boolean exceptionExists = biometricsController.hasApplicantBiometricException();
		HBox exceptionPhotoNode = (HBox) this.node.lookup(RegistrationConstants.HASH + uiSchemaDTO.getId() + Modality.EXCEPTION_PHOTO);
		if(exceptionPhotoNode != null) {
			exceptionPhotoNode.setVisible(exceptionExists);
			exceptionPhotoNode.setManaged(exceptionExists);
			exceptionPhotoNode.getChildren().forEach( child -> {
				child.setVisible(exceptionExists);
				child.setManaged(exceptionExists);
			});
		}
	}


	public void refreshModalityButton(Modality modality) {
		LOGGER.info("Refreshing biometric field {} modality : {}", uiSchemaDTO.getId(), modality);
		Node tempNode = getField(uiSchemaDTO.getId()+modality);
		if(tempNode == null)
			return;

		HBox modalityView = (HBox) tempNode;
		Button button = (Button) modalityView.getChildren().get(0);
		Image image = null;
		List<BiometricsDto> capturedData = getRegistrationDTo().getBiometric(uiSchemaDTO.getSubType(), modality.getAttributes());
		if(!capturedData.isEmpty()) {
			Optional<BiometricsDto> bestCopy = capturedData.stream().sorted(Comparator.comparingDouble(BiometricsDto::getQualityScore)).findFirst();
			image = biometricsController.getBioStreamImage(uiSchemaDTO.getSubType(), modality,
					bestCopy.get().getNumOfRetries());
		} else {
			image = biometricsController.getBioStreamImage(uiSchemaDTO.getSubType(), modality, 0);
		}

		button.setGraphic(getImageView(image, 80));
		button.setPrefSize(105, 80);

		addRemoveCaptureStatusMark(modalityView, modality);
		displayExceptionPhoto();
	}

	private void addRemoveCaptureStatusMark(HBox pane, Modality modality) {
		if (pane.getChildren().size() > 1) {
			pane.getChildren().remove(1);
		}

		boolean isExceptionsMarked = isAnyExceptions(modality);
		boolean isCaptured = !getRegistrationDTo().getBiometric(uiSchemaDTO.getSubType(), modality.getAttributes()).isEmpty();
		if(modality.equals(Modality.EXCEPTION_PHOTO) && biometricsController.isBiometricExceptionProofCollected()) {
			pane.getChildren().add(addCompletionImg(getCompletionImgPath(false)));
			return;
		}

		if(isExceptionsMarked) {
			pane.getChildren().add(addCompletionImg(getCompletionImgPath(true)));
			return;
		}

		if(isCaptured) {
			pane.getChildren().add(addCompletionImg(getCompletionImgPath(false)));
			return;
		}
	}


	private String getCompletionImgPath(boolean isAnyException) {
		return isAnyException ? RegistrationConstants.EXCLAMATION_IMG
				: RegistrationConstants.TICK_CIRICLE_IMG;
	}

	public boolean isAnyExceptions(Modality modality) {
		//checking with configured set of attributes for the given modality
		return modalityAttributeMap.get(modality).get(0)
				.stream()
				.anyMatch( attr -> biometricsController.isBiometricExceptionAvailable(uiSchemaDTO.getSubType(), attr));
	}

	public boolean isAllExceptions(Modality modality) {
		//checking with configured set of attributes for the given modality
		return modalityAttributeMap.get(modality).get(0)
				.stream()
				.allMatch( attr -> biometricsController.isBiometricExceptionAvailable(uiSchemaDTO.getSubType(), attr));
	}

	private ImageView addCompletionImg(String imgPath) {
		ImageView tickImageView = null;
		try {
			tickImageView = new ImageView(biometricsController.getImage(imgPath,true));
			tickImageView.setId(uiSchemaDTO.getId() + currentModality.name() + "PANE");
			tickImageView.setFitWidth(35);
			tickImageView.setFitHeight(35);
			tickImageView.setPreserveRatio(true);
		} catch (RegBaseCheckedException regBaseCheckedException) {
			LOGGER.error("Exception while getting image");
		}
		
		return tickImageView;
	}

	//TODO check how multiple exception photos can be taken, and also displayed in biometric field
	public Image getExceptionDocumentAsImage() {
		try {
			DocumentDto documentDto = null;
			for(String key : getRegistrationDTo().getDocuments().keySet()) {
				if(getRegistrationDTo().getDocuments().get(key).getCategory().equals(RegistrationConstants.POE_DOCUMENT) &&
						getRegistrationDTo().getDocuments().get(key).getType().equals("EOP") &&
						getRegistrationDTo().getDocuments().get(key).getFormat().equals(RegistrationConstants.SCANNER_IMG_TYPE)) {
					documentDto = getRegistrationDTo().getDocuments().get(key);
					break;
				}
			}

			if(documentDto == null)
				return null;

			if(RegistrationConstants.PDF.equalsIgnoreCase(documentDto.getFormat())) {
				List<BufferedImage> list = DocScannerUtil.pdfToImages(documentDto.getDocument());
				return list.isEmpty() ? null : DocScannerUtil.getImage(list.get(0));
			} else {
				InputStream is = new ByteArrayInputStream(documentDto.getDocument());
				return DocScannerUtil.getImage(ImageIO.read(is));
			}
		} catch (Exception ex) {
			LOGGER.error("Failed to read document as image", ex);
		}
		return null;
	}

}

