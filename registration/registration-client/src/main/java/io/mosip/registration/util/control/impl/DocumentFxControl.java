package io.mosip.registration.util.control.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;


import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.api.docscanner.DocScannerUtil;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.enums.FlowType;
import io.mosip.registration.service.doc.category.ValidDocumentService;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.common.ComboBoxAutoComplete;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class DocumentFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DocumentFxControl.class);
	private static final String SCRIPT_NAME = "applicanttype.mvel";
	private static String loggerClassName = " Text Field Control Type Class";

	private DocumentScanController documentScanController;

	private MasterSyncService masterSyncService;

	private ValidDocumentService validDocumentService;

	private String PREVIEW_ICON = "previewIcon";

	private String CLEAR_ID = "clear";

	public DocumentFxControl() {
		org.springframework.context.ApplicationContext applicationContext = ClientApplication.getApplicationContext();
		auditFactory = applicationContext.getBean(AuditManagerService.class);
		documentScanController = applicationContext.getBean(DocumentScanController.class);
		masterSyncService = applicationContext.getBean(MasterSyncService.class);
		validDocumentService = applicationContext.getBean(ValidDocumentService.class);
		this.requiredFieldValidator = applicationContext.getBean(RequiredFieldValidator.class);
	}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;

		HBox hBox = new HBox();
		hBox.setSpacing(20);
		hBox.setPrefHeight(78);

		// DROP-DOWN
		hBox.getChildren().add(create(uiFieldDTO));

		// REF-FIELD
		hBox.getChildren().add(createDocRef(uiFieldDTO.getId()));

		// CLEAR IMAGE
		GridPane tickMarkGridPane = getImageGridPane(PREVIEW_ICON, RegistrationConstants.DOC_PREVIEW_ICON);
		tickMarkGridPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {

			scanDocument(true);

		});
		// TICK-MARK
		hBox.getChildren().add(tickMarkGridPane);

		// CLEAR IMAGE
		GridPane clearGridPane = getImageGridPane(CLEAR_ID, RegistrationConstants.CLOSE_IMAGE_PATH);
		clearGridPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(
					uiFieldDTO.getId());
			comboBox.getSelectionModel().clearSelection();
			clearCapturedDocuments();
		});
		hBox.getChildren().add(clearGridPane);

		// SCAN-BUTTON
		hBox.getChildren().add(createScanButton(uiFieldDTO));

		this.node = hBox;

		setListener(getField(uiFieldDTO.getId() + RegistrationConstants.BUTTON));

		changeNodeOrientation(hBox, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		fillData(getDocumentCategories());

		return this.control;
	}

	private void clearCapturedDocuments() {
		AuditEvent auditEvent = null;
		try {
			auditEvent = AuditEvent.valueOf(String.format("REG_DOC_%S_DELETE", uiFieldDTO.getSubType()));
		} catch (Exception exception) {
			LOGGER.error("Unable to find audit event for button : " + uiFieldDTO.getSubType());

			auditEvent = AuditEvent.REG_DOC_DELETE;
		}
		auditFactory.audit(auditEvent, Components.REG_DOCUMENTS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		getRegistrationDTo().removeDocument(this.uiFieldDTO.getId());

		TextField textField = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);
		textField.setText(RegistrationConstants.EMPTY);

		getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(false);
		getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(false);
		getField(uiFieldDTO.getId() + PREVIEW_ICON).setManaged(true);
		getField(uiFieldDTO.getId() + CLEAR_ID).setManaged(true);
	}

	private GridPane getImageGridPane(String id, String imagePath) {
		VBox imageVBox = new VBox();
		imageVBox.setId(uiFieldDTO.getId() + id);
		ImageView imageView = new ImageView(
				(new Image(this.getClass().getResourceAsStream(imagePath), 25, 25, true, true)));

		boolean isVisible = getData() != null ? true : false;
		imageView.setPreserveRatio(true);
		imageVBox.setVisible(isVisible);

		imageVBox.getChildren().add(imageView);

		GridPane gridPane = new GridPane();
		RowConstraints rowConstraint1 = new RowConstraints();
		RowConstraints rowConstraint2 = new RowConstraints();
		rowConstraint1.setPercentHeight(45);
		rowConstraint2.setPercentHeight(55);
		gridPane.getRowConstraints().addAll(rowConstraint1, rowConstraint2);
		gridPane.add(imageVBox, 0, 1);

		return gridPane;
	}

	private GridPane createScanButton(UiFieldDTO uiFieldDTO) {

		Button scanButton = new Button();
		scanButton.setText(ApplicationContext.getBundle(null, RegistrationConstants.LABELS)
				.getString(RegistrationConstants.SCAN_BUTTON));
		scanButton.setId(uiFieldDTO.getId() + RegistrationConstants.BUTTON);
		scanButton.getStyleClass().add(RegistrationConstants.DOCUMENT_CONTENT_BUTTON);
		scanButton.setGraphic(new ImageView(
				new Image(this.getClass().getResourceAsStream(RegistrationConstants.SCAN), 12, 12, true, true)));

		GridPane scanButtonGridPane = new GridPane();
		scanButtonGridPane.setPrefWidth(80);
		scanButtonGridPane.setPadding(new Insets(21, 0, 0, 0));
		scanButtonGridPane.add(scanButton, 0, 1);

		return scanButtonGridPane;
	}

	private void scanDocument(boolean isPreviewOnly) {

		if(!isValid()) {
			documentScanController.generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PLEASE_SELECT)
					+ RegistrationConstants.SPACE + uiFieldDTO.getSubType() + " " + RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.DOCUMENT));
			return;
		}

		documentScanController.scanDocument(uiFieldDTO.getId(), this,	isPreviewOnly);
	}

	private VBox createDocRef(String id) {
		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(id + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);
		simpleTypeVBox.getStyleClass().add(RegistrationConstants.SCAN_VBOX);

		double prefWidth = simpleTypeVBox.getPrefWidth();

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {

			ResourceBundle rb = ApplicationContext.getInstance().getBundle(lCode, RegistrationConstants.LABELS);
			labels.add(rb.getString(RegistrationConstants.REF_NUMBER));
		});

		String titleText = String.join(RegistrationConstants.SLASH, labels);
		ResourceBundle rb = ApplicationContext.getInstance()
				.getBundle(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0), RegistrationConstants.LABELS);

		/** Title label */
		Label fieldTitle = getLabel(id + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.LABEL, titleText,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, prefWidth);

		simpleTypeVBox.getChildren().add(fieldTitle);

		/** Text Field */
		TextField textField = getTextField(id + RegistrationConstants.DOC_TEXT_FIELD, titleText,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false);

		textField.textProperty().addListener((observable, oldValue, newValue) -> {
			Label label = (Label) getField(
					uiFieldDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.LABEL);
			if (textField.getText().isEmpty()) {
				label.setVisible(false);
			} else {
				label.setVisible(true);
			}

			if(newValue != null &&
					getRegistrationDTo().getDocuments().containsKey(uiFieldDTO.getId())) {
				getRegistrationDTo().getDocuments().get(uiFieldDTO.getId()).setRefNumber(newValue);
			}
		});

		simpleTypeVBox.getChildren().add(textField);
		return simpleTypeVBox;
	}

	private VBox create(UiFieldDTO uiFieldDTO) {

		String fieldName = uiFieldDTO.getId();

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		double prefWidth = 300;

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiFieldDTO.getLabel().get(lCode));
		});
		String titleText = String.join(RegistrationConstants.SLASH, labels) + getMandatorySuffix(uiFieldDTO);

		/** Title label */
		Label fieldTitle = getLabel(fieldName + RegistrationConstants.LABEL, titleText,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, prefWidth);
		simpleTypeVBox.getChildren().add(fieldTitle);

		/** comboBox Field */
		ComboBox<DocumentCategoryDto> comboBox = getComboBox(fieldName, titleText,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false);
		simpleTypeVBox.getChildren().add(comboBox);

		comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			if (comboBox.getSelectionModel().getSelectedItem() != null) {
				String selectedCode = comboBox.getSelectionModel().getSelectedItem().getCode();

				if(getRegistrationDTo().getDocuments().containsKey(uiFieldDTO.getId()) &&
						!selectedCode.equals(getRegistrationDTo().getDocuments().get(uiFieldDTO.getId()).getType())) {
					LOGGER.error("As selected document is not part of applicantype based filtered doc categories, clearing previously captured document : {}",
							uiFieldDTO.getId());
					clearCapturedDocuments();
				}

				List<String> toolTipTextList = new ArrayList<>();

				for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
					DocumentType documentType = masterSyncService.getDocumentType(selectedCode, langCode);
					if (documentType != null) {
						toolTipTextList.add(documentType.getName());
					}
				}
				Label messageLabel = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE);
				messageLabel.setText(String.join(RegistrationConstants.SLASH, toolTipTextList));
				fieldTitle.setVisible(true);
			} else {
				Label messageLabel = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE);
				messageLabel.setText(RegistrationConstants.EMPTY);
			}
		});

		comboBox.setOnMouseExited(event -> {
			if (comboBox.getTooltip() != null) {
				comboBox.getTooltip().hide();
			}

			Label messageLabel = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE);
			messageLabel.setVisible(false);
			messageLabel.setManaged(false);
		});

		comboBox.setOnMouseEntered((event -> {
			Label messageLabel = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE);
			if (messageLabel.getText()!=null && !messageLabel.getText().isEmpty()) {
				messageLabel.setVisible(true);
				messageLabel.setManaged(true);
			}
		}));

		Label messageLabel = getLabel(uiFieldDTO.getId() + RegistrationConstants.MESSAGE, null,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, simpleTypeVBox.getPrefWidth());
		messageLabel.setWrapText(true);
		messageLabel.setPrefWidth(prefWidth);
		messageLabel.setManaged(false);
		simpleTypeVBox.getChildren().add(messageLabel);

		return simpleTypeVBox;
	}

	@Override
	public void setData(Object data) {

		try {

			if (data == null) {
				getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(false);
				getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(false);
			} else {
				List<BufferedImage> bufferedImages = (List<BufferedImage>) data;
				if (bufferedImages == null || bufferedImages.isEmpty()) {
					documentScanController.generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_EMPTY));
					return;
				}

				String configuredDocType = ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.DOC_TYPE);
				byte[] byteArray =  ("pdf".equalsIgnoreCase(configuredDocType)) ?
						DocScannerUtil.asPDF(bufferedImages,
								ApplicationContext.getFloatValueFromApplicationMap(RegistrationConstants.JPG_COMPRESSION_QUALITY)) :
						DocScannerUtil.asImage(bufferedImages);

				if (byteArray == null) {
					documentScanController.generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_CONVERTION_ERR));
					return;
				}

				int docSize = (int) Math.ceil(Integer.parseInt(documentScanController
						.getValueFromApplicationContext(RegistrationConstants.DOC_SIZE)) / (double)(1024 * 1024));
				if (docSize <= (byteArray.length / (1024 * 1024))) {
					bufferedImages.clear();
					documentScanController.generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOC_SIZE).replace("1", Integer.toString(docSize)));
					return;
				}

				ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());
				DocumentDto documentDto = getRegistrationDTo().getDocuments().get(uiFieldDTO.getId());
				if (documentDto == null) {
					documentDto = new DocumentDto();
					documentDto.setFormat(configuredDocType);
					documentDto.setCategory(uiFieldDTO.getSubType());
					documentDto.setOwner(RegistrationConstants.APPLICANT);
				}

				documentDto.setType(comboBox.getValue().getCode());
				documentDto.setValue(uiFieldDTO.getSubType().concat(RegistrationConstants.UNDER_SCORE)
						.concat(comboBox.getValue().getCode()));

				documentDto.setDocument(byteArray);
				TextField textField = (TextField) getField(
						uiFieldDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);
				documentDto.setRefNumber(textField.getText());
				getRegistrationDTo().addDocument(uiFieldDTO.getId(), documentDto);

				getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(true);
				getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(true);

				getField(uiFieldDTO.getId() + PREVIEW_ICON).setManaged(true);
				getField(uiFieldDTO.getId() + CLEAR_ID).setManaged(true);

				Label label = (Label) getField(uiFieldDTO.getId()+RegistrationConstants.LABEL);
				label.getStyleClass().clear();
				label.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
			}
		} catch (IOException exception) {
			LOGGER.error("Unable to parse the buffered images to byte array ", exception);
			getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(false);
			getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(false);
			documentScanController.generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_REG_PAGE));
		}
		//refreshFields();
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDocuments().get(uiFieldDTO.getId());
	}

	@Override
	public boolean isValid() {
		String poeDocValue = documentScanController
				.getValueFromApplicationContext(RegistrationConstants.POE_DOCUMENT_VALUE);

		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());
		if (comboBox.getValue() == null) {
			comboBox.requestFocus();
			return false;
		} else if (comboBox.getValue().getCode().equalsIgnoreCase(poeDocValue)) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean isEmpty() {
		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());
		return (comboBox.getValue() == null);
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}

	@Override
	public void setListener(Node node) {
		Button scanButton = (Button) node;
		scanButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				AuditEvent auditEvent = null;
				try {
					auditEvent = AuditEvent
							.valueOf(String.format("REG_DOC_%S_SCAN", uiFieldDTO.getSubType()));
				} catch (Exception exception) {
					auditEvent = AuditEvent.REG_DOC_SCAN;
				}
				auditFactory.audit(auditEvent, Components.REG_DOCUMENTS, SessionContext.userId(),
						AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

				scanDocument(false);
			}
		});
		scanButton.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				scanButton.setGraphic(new ImageView(new Image(
						this.getClass().getResourceAsStream(RegistrationConstants.SCAN_FOCUSED), 12, 12, true, true)));
			} else {
				scanButton.setGraphic(new ImageView(new Image(
						this.getClass().getResourceAsStream(RegistrationConstants.SCAN), 12, 12, true, true)));
			}
		});
	}

	private <T> ComboBox<DocumentCategoryDto> getComboBox(String id, String titleText, String styleClass,
														  double prefWidth, boolean isDisable) {
		ComboBox<DocumentCategoryDto> field = new ComboBox<DocumentCategoryDto>();
		StringConverter<T> uiRenderForComboBox = FXUtils.getInstance().getStringConverterForComboBox();
		//VBox vbox = new VBox();
		field.setId(id);
		field.setPrefWidth(prefWidth);
		field.setPromptText(titleText);
		field.setDisable(isDisable);
		field.setConverter((StringConverter<DocumentCategoryDto>) uiRenderForComboBox);
		field.getStyleClass().add(styleClass);
		field.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			getField(uiFieldDTO.getId() + RegistrationConstants.LABEL).setVisible(true);
		});

		changeNodeOrientation(field, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		return field;
	}

	private TextField getTextField(String id, String titleText, String demographicTextfield, double prefWidth,
								   boolean isDisable) {

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(id);
		textField.setPromptText(titleText);
		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		textField.setPrefWidth(prefWidth);
		textField.setDisable(isDisable);

		return textField;
	}

	@Override
	public void fillData(Object data) {
		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());

		if (data != null) {
			List<DocumentCategoryDto> vals = (List<DocumentCategoryDto>) data;
			comboBox.getItems().addAll(vals);
		}

	}

	public boolean canContinue() {

		if (requiredFieldValidator == null) {
			requiredFieldValidator = ClientApplication.getApplicationContext().getBean(RequiredFieldValidator.class);
		}

		boolean isRequired = requiredFieldValidator.isRequiredField(this.uiFieldDTO, getRegistrationDTo());
		if (isRequired && getRegistrationDTo().getDocuments().get(this.uiFieldDTO.getId()) == null) {

			Label label = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.LABEL);
			label.getStyleClass().clear();
			label.getStyleClass().add(RegistrationConstants.DemoGraphicFieldMessageLabel);
			label.setVisible(true);
			return false;
		}

		return true;
	}

	@Override
	public void selectAndSet(Object data) {
		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());

		if(data == null) {
			comboBox.getSelectionModel().clearSelection();
			return;
		}

		if (comboBox != null) {
			DocumentDto documentDto = (DocumentDto) data;
			Optional<DocumentCategoryDto> selected = comboBox.getItems()
					.stream()
					.filter(doc -> doc.getCode().equals(documentDto.getType()))
					.findFirst();

			if(selected.isPresent()) {
				comboBox.getSelectionModel().select(selected.get());
				TextField textField = (TextField) getField(uiFieldDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);
				textField.setText(documentDto.getRefNumber());
				getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(true);
				getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(true);
			}
			else {
				LOGGER.error("Unable to find doc from pre-reg sync, field: {}, value: {}", uiFieldDTO.getId(), documentDto.getValue());
				getRegistrationDTo().removeDocument(uiFieldDTO.getId());
				auditFactory.audit(AuditEvent.REG_DOC_DELETE, Components.REG_DOCUMENTS, SessionContext.userId(),
						AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
			}
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiFieldDTO.getId());
		List<DocumentCategoryDto> list = getDocumentCategories();
		if(list != null) {
			comboBox.getItems().clear();
			comboBox.getItems().addAll(list);

			Optional<DocumentCategoryDto> savedValue = list.stream()
					.filter( d -> getRegistrationDTo().getDocuments().containsKey(uiFieldDTO.getId())
							&& d.getCode().equals(getRegistrationDTo().getDocuments().get(uiFieldDTO.getId()).getType()))
					.findFirst();

			if(savedValue.isPresent())
				comboBox.getSelectionModel().select(savedValue.get());
		}


		if(getRegistrationDTo().getDocuments().containsKey(uiFieldDTO.getId())) {
			getField(uiFieldDTO.getId() + PREVIEW_ICON).setVisible(true);
			getField(uiFieldDTO.getId() + CLEAR_ID).setVisible(true);
		}
	}

	private List<DocumentCategoryDto> getDocumentCategories() {
		Object applicantTypeCode = requiredFieldValidator.evaluateMvelScript((String) ApplicationContext.map().getOrDefault(
				RegistrationConstants.APPLICANT_TYPE_MVEL_SCRIPT, SCRIPT_NAME), getRegistrationDTo());
		LOGGER.info("Document field {}, for applicantType : {}", uiFieldDTO.getId(), applicantTypeCode);
		if(applicantTypeCode != null) {
			return validDocumentService.getDocumentCategories((String) applicantTypeCode,
					this.uiFieldDTO.getSubType(),
					getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));
		}
		return Collections.EMPTY_LIST;
	}
}
