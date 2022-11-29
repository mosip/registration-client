/**
 * 
 */
package io.mosip.registration.util.control.impl;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.util.*;
import java.util.stream.Collectors;

import io.mosip.registration.controller.*;
import javafx.geometry.Insets;
import org.springframework.context.ApplicationContext;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.transliteration.spi.Transliteration;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.reg.Validations;
import io.mosip.registration.dto.BlocklistedConsentDto;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * @author YASWANTH S
 *
 */
public class TextFieldFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(TextFieldFxControl.class);

	private static String loggerClassName = " Text Field Control Type Class";

	private Validations validation;
	
	private DemographicChangeActionHandler demographicChangeActionHandler;
	
	private Transliteration<String> transliteration;
	
	private Node keyboardNode;
	
	private static double xPosition;
	private static double yPosition;
	
	private FXComponents fxComponents;
	
	private GenericController genericController;
	
	public TextFieldFxControl() {
		ApplicationContext applicationContext = ClientApplication.getApplicationContext();
		validation = applicationContext.getBean(Validations.class);
		fxComponents = applicationContext.getBean(FXComponents.class);
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);
		Map<String, Transliteration> beans = applicationContext.getBeansOfType(Transliteration.class);
		LOGGER.debug("Transliterations implementations found : {}", beans);
		if (!beans.keySet().isEmpty()) {
			LOGGER.info("Choosing transliteration implementations --> {}", beans.keySet().iterator().next());
			this.transliteration = beans.get(beans.keySet().iterator().next());
		}
		genericController = applicationContext.getBean(GenericController.class);
		this.auditFactory = applicationContext.getBean(AuditManagerService.class);
	}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;
		create(uiFieldDTO);
		return this.control;
	}

	@Override
	public void setData(Object data) {

		RegistrationDTO registrationDTO = getRegistrationDTo();

		if (this.uiFieldDTO.getType().equalsIgnoreCase(RegistrationConstants.SIMPLE_TYPE)) {
			List<SimpleDto> values = new ArrayList<SimpleDto>();

			for (String langCode : registrationDTO.getSelectedLanguagesByApplicant()) {

				TextField textField = (TextField) getField(uiFieldDTO.getId() + langCode);

				SimpleDto simpleDto = new SimpleDto(langCode, textField.getText());
				values.add(simpleDto);

			}

			registrationDTO.addDemographicField(uiFieldDTO.getId(), values);

		} else {
			registrationDTO
					.addDemographicField(uiFieldDTO.getId(),
							((TextField) getField(
									uiFieldDTO.getId() + registrationDTO.getSelectedLanguagesByApplicant().get(0)))
											.getText());

		}
	}

	@Override
	public void setListener(Node node) {
		FXUtils.getInstance().onTypeFocusUnfocusListener((Pane) getNode(), (TextField) node);

		TextField textField = (TextField) node;

		textField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (uiFieldDTO.isTransliterate()) {
				transliterate(textField, textField.getId().substring(textField.getId().length() - RegistrationConstants.LANGCODE_LENGTH, textField.getId().length()));
			}
			if (isValid()) {				
				setData(null);

				// handling other handlers
				demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),
						uiFieldDTO.getChangeAction());
			} else {
				getRegistrationDTo().getDemographics().remove(this.uiFieldDTO.getId());
			}
			LOGGER.info("invoked from Listener {}",uiFieldDTO.getId());
			// Group level visibility listeners
			refreshFields();
		});
	}

	private VBox create(UiFieldDTO uiFieldDTO) {
		String fieldName = uiFieldDTO.getId();

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();

		this.node = simpleTypeVBox;
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		/** Title label */
		Label fieldTitle = getLabel(uiFieldDTO.getId() + RegistrationConstants.LABEL, "",
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, simpleTypeVBox.getWidth());
		changeNodeOrientation(fieldTitle, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		simpleTypeVBox.getChildren().add(fieldTitle);

		VBox vBox = new VBox();
		vBox.setPrefWidth(simpleTypeVBox.getPrefWidth());
		List<String> labels = new ArrayList<>();
		switch (this.uiFieldDTO.getType()) {
			case RegistrationConstants.SIMPLE_TYPE :
				getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(langCode -> {
					labels.add(this.uiFieldDTO.getLabel().get(langCode));
					vBox.getChildren().add(createTextBox(langCode,true));
					vBox.getChildren().add(getLabel(uiFieldDTO.getId() + langCode + RegistrationConstants.MESSAGE, null,
							RegistrationConstants.DemoGraphicFieldMessageLabel, false, simpleTypeVBox.getPrefWidth()));
					
					HBox hyperLinkHBox = new HBox();
					hyperLinkHBox.setVisible(false);
					hyperLinkHBox.setPrefWidth(simpleTypeVBox.getPrefWidth());
					hyperLinkHBox.setId(uiFieldDTO.getId() + langCode + "HyperlinkHBox");
					hyperLinkHBox.getChildren()
							.add(getHyperlink(uiFieldDTO.getId() + langCode + "Accept", langCode,
									io.mosip.registration.context.ApplicationContext
											.getBundle(langCode, RegistrationConstants.LABELS).getString("accept_word"),
									RegistrationConstants.DemoGraphicFieldMessageLabel, true));
					hyperLinkHBox.getChildren().add(getLabel(uiFieldDTO.getId() + langCode + "HyperlinkLabel",
							io.mosip.registration.context.ApplicationContext
									.getBundle(langCode, RegistrationConstants.LABELS).getString("slash"),
							RegistrationConstants.DemoGraphicFieldMessageLabel, true, simpleTypeVBox.getPrefWidth()));
					hyperLinkHBox.getChildren()
							.add(getHyperlink(uiFieldDTO.getId() + langCode + "Reject", langCode,
									io.mosip.registration.context.ApplicationContext
											.getBundle(langCode, RegistrationConstants.LABELS).getString("reject_word"),
									RegistrationConstants.DemoGraphicFieldMessageLabel, true));

					vBox.getChildren().add(hyperLinkHBox);
				});
				break;
			default:
				String langCode = getRegistrationDTo().getSelectedLanguagesByApplicant().get(0);
				getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(langcode -> {
							labels.add(this.uiFieldDTO.getLabel().get(langcode));});
				vBox.getChildren().add(createTextBox(langCode,false));
				vBox.getChildren().add(getLabel(uiFieldDTO.getId() + langCode + RegistrationConstants.MESSAGE,
						null, RegistrationConstants.DemoGraphicFieldMessageLabel, false, simpleTypeVBox.getPrefWidth()));
				
				HBox hyperLinkHBox = new HBox();
				hyperLinkHBox.setVisible(false);
				hyperLinkHBox.setId(uiFieldDTO.getId() + langCode + "HyperlinkHBox");
				hyperLinkHBox.getChildren()
						.add(getHyperlink(uiFieldDTO.getId() + langCode + "Accept", langCode,
								io.mosip.registration.context.ApplicationContext
										.getBundle(langCode, RegistrationConstants.LABELS).getString("accept_word"),
								RegistrationConstants.DemoGraphicFieldMessageLabel, true));
				hyperLinkHBox.getChildren()
						.add(getLabel(uiFieldDTO.getId() + "HyperlinkLabel",
								io.mosip.registration.context.ApplicationContext
										.getBundle(langCode, RegistrationConstants.LABELS).getString("slash"),
								RegistrationConstants.DemoGraphicFieldMessageLabel, true, simpleTypeVBox.getPrefWidth()));
				hyperLinkHBox.getChildren()
						.add(getHyperlink(uiFieldDTO.getId() + langCode + "Reject", langCode,
								io.mosip.registration.context.ApplicationContext.getBundle(langCode, RegistrationConstants.LABELS).getString("reject_word"),
								RegistrationConstants.DemoGraphicFieldMessageLabel, true));

				vBox.getChildren().add(hyperLinkHBox);
				break;
		}

		fieldTitle.setText(String.join(RegistrationConstants.SLASH, labels)	+ getMandatorySuffix(uiFieldDTO));
		simpleTypeVBox.getChildren().add(vBox);
		simpleTypeVBox.setMargin(vBox, new Insets(0, 30, 0, 0));
		return simpleTypeVBox;
	}
	
	private Hyperlink getHyperlink(String id, String langCode, String titleText, String styleClass, boolean isVisible) {
		/** Field Title */
		Hyperlink hyperLink = new Hyperlink();
		hyperLink.setId(id);
		hyperLink.setText(titleText);
		hyperLink.getStyleClass().add(styleClass);
		hyperLink.setVisible(isVisible);
		hyperLink.setWrapText(true);
		if (id.contains("Accept")) {
			hyperLink.setOnAction(event -> {
				auditFactory.audit(AuditEvent.REG_BLOCKLISTED_WORD_ACCEPTED, Components.REG_DEMO_DETAILS, SessionContext.userId(),
						AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
				
				TextField textField = (TextField) getField(uiFieldDTO.getId() + langCode);
				if (getRegistrationDTo().BLOCKLISTED_CHECK.containsKey(uiFieldDTO.getId())) {
					List<String> words = getRegistrationDTo().BLOCKLISTED_CHECK.get(uiFieldDTO.getId()).getWords();
					words.addAll(validation.getBlockListedWordsList(textField));
					getRegistrationDTo().BLOCKLISTED_CHECK.get(uiFieldDTO.getId()).setWords(words.stream().distinct().collect(Collectors.toList()));
				} else {
					BlocklistedConsentDto blockListedConsent = new BlocklistedConsentDto();
					blockListedConsent.setWords(validation.getBlockListedWordsList(textField));
					blockListedConsent.setOperatorConsent(true);
					blockListedConsent.setScreenName(genericController.getCurrentScreenName());
					blockListedConsent.setOperatorId(SessionContext.userId());
					getRegistrationDTo().BLOCKLISTED_CHECK.put(uiFieldDTO.getId(), blockListedConsent);
				}

				if (isValid()) {
					FXUtils.getInstance().setTextValidLabel((Pane) getNode(), textField, uiFieldDTO.getId());
					setData(null);
					// handling other handlers
					demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),
							uiFieldDTO.getChangeAction());
				}
			});
		} else {
			hyperLink.setOnAction(event -> {
				auditFactory.audit(AuditEvent.REG_BLOCKLISTED_WORD_REJECTED, Components.REG_DEMO_DETAILS, SessionContext.userId(),
						AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
				
				TextField textField = (TextField) getField(uiFieldDTO.getId() + langCode);
				textField.setText(RegistrationConstants.EMPTY);
				getField(uiFieldDTO.getId() + langCode + "HyperlinkHBox").setVisible(false);
				if (!isValid()) {
					getRegistrationDTo().BLOCKLISTED_CHECK.remove(uiFieldDTO.getId());
					getRegistrationDTo().getDemographics().remove(this.uiFieldDTO.getId());
					FXUtils.getInstance().showErrorLabel(textField, (Pane) getNode());
				}
			});
		}
		return hyperLink;
	}

	private HBox createTextBox(String langCode, boolean isSimpleType) {
		HBox textFieldHBox = new HBox();
		TextField textField = getTextField(langCode, uiFieldDTO.getId() + langCode, false);
		textField.setMinWidth(400);
		textFieldHBox.getChildren().add(textField);

		if(isSimpleType) {
			HBox imagesHBox = new HBox();
			imagesHBox.getStyleClass().add(RegistrationConstants.ICONS_HBOX);
			imagesHBox.setPrefWidth(10);

			VirtualKeyboard keyBoard = new VirtualKeyboard(langCode);
			keyBoard.changeControlOfKeyboard(textField);
			
			ImageView keyBoardImgView = getKeyBoardImage();
			keyBoardImgView.setId(langCode);
			keyBoardImgView.visibleProperty().bind(textField.visibleProperty());
			keyBoardImgView.managedProperty().bind(textField.visibleProperty());

			if (keyBoardImgView != null) {
				keyBoardImgView.setOnMouseClicked((event) -> {
					setFocusOnField(event, keyBoard, langCode, textField);
				});
			}
			imagesHBox.getChildren().add(keyBoardImgView);
			textFieldHBox.getChildren().add(imagesHBox);
		}

		setListener(textField);
		changeNodeOrientation(textFieldHBox, langCode);
		Validations.putIntoLabelMap(uiFieldDTO.getId() + langCode, uiFieldDTO.getLabel().get(langCode));
		return textFieldHBox;
	}


	private TextField getTextField(String langCode, String id, boolean isDisable) {

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(id);
		textField.setPromptText(io.mosip.registration.context.ApplicationContext
				.getBundle(langCode, RegistrationConstants.LABELS).getString("language"));
		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		textField.setDisable(isDisable);

		return textField;
	}

	private ImageView getKeyBoardImage() {
		ImageView imageView = null;

		imageView = new ImageView(new Image(getClass().getResourceAsStream("/images/keyboard.png")));
		imageView.setId(uiFieldDTO.getId() + "KeyBoard");
		imageView.setFitHeight(20.00);
		imageView.setFitWidth(22.00);

		return imageView;
	}

	@Override
	public Object getData() {

		return getRegistrationDTo().getDemographics().get(uiFieldDTO.getId());
	}


	@Override
	public boolean isValid() {
		boolean isValid = true;
		removeNonExistentBlockListedWords();
		for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
			TextField textField = (TextField) getField(uiFieldDTO.getId() + langCode);
			if (textField == null)  {
				isValid = false;
				break;
			}

			getField(uiFieldDTO.getId() + langCode + "HyperlinkHBox").setVisible(false);
			
			if (validation.validateTextField((Pane) getNode(), textField, uiFieldDTO.getId(), true, langCode)) {
				if (validation.validateForBlockListedWords((Pane) getNode(), textField, uiFieldDTO.getId(), true, langCode)) {
					FXUtils.getInstance().setTextValidLabel((Pane) getNode(), textField, uiFieldDTO.getId());
					getField(uiFieldDTO.getId() + langCode + "HyperlinkHBox").setVisible(false);
				} else {
					FXUtils.getInstance().showErrorLabel(textField, (Pane) getNode());
					if (!getField(uiFieldDTO.getId() + langCode + "HyperlinkHBox").isVisible()) {
						getField(uiFieldDTO.getId() + langCode + "HyperlinkHBox").setVisible(true);
					}
					isValid = false;
					break;
				}
			} else {
				FXUtils.getInstance().showErrorLabel(textField, (Pane) getNode());
				isValid = false;
				break;
			}
			
			if(!this.uiFieldDTO.getType().equalsIgnoreCase(RegistrationConstants.SIMPLE_TYPE)) {
				break; //not required to iterate further
			}
		}
		return isValid;
	}

	private void removeNonExistentBlockListedWords() {
		if (getRegistrationDTo().BLOCKLISTED_CHECK.containsKey(uiFieldDTO.getId()) &&
				!getRegistrationDTo().BLOCKLISTED_CHECK.get(uiFieldDTO.getId()).getWords().isEmpty()) {
			StringBuilder content = new StringBuilder();
			getRegistrationDTo().getSelectedLanguagesByApplicant().stream().forEach(langCode -> {
				TextField textField = (TextField) getField(uiFieldDTO.getId() + langCode);
				if (textField != null && textField.getText() != null) {
					content.append(textField.getText()).append(RegistrationConstants.SPACE);
				}
			});

			List<String> inputText = new ArrayList<>(Arrays.asList(content.toString().split(RegistrationConstants.SPACE)));
			if (inputText.size() > 1) inputText.add(content.toString());
			//String[] tokens = content.toString().split(RegistrationConstants.SPACE);
			getRegistrationDTo().BLOCKLISTED_CHECK.get(uiFieldDTO.getId())
					.getWords()
					.removeIf(word -> inputText.stream().noneMatch(t -> t.toLowerCase().contains(word)));
		}
	}

	@Override
	public boolean isEmpty() {
		
		List<String> langCodes = new LinkedList<String>();
		if(!this.uiFieldDTO.getType().equalsIgnoreCase(RegistrationConstants.SIMPLE_TYPE)) {
			langCodes.add(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));
		} else {
			langCodes.addAll(getRegistrationDTo().getSelectedLanguagesByApplicant());
		}
		
		return langCodes.stream().allMatch(langCode -> {
			TextField textField = (TextField) getField(uiFieldDTO.getId() + langCode);
			return textField.getText().trim().isEmpty();
		});
	}

	private void transliterate(TextField textField, String langCode) {
		for (String langCodeToBeTransliterated : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
			if (!langCodeToBeTransliterated.equalsIgnoreCase(langCode)) {
				TextField textFieldToBeTransliterated = (TextField) getField(uiFieldDTO.getId() + langCodeToBeTransliterated);
				if (textFieldToBeTransliterated != null)  {
					try {
						textFieldToBeTransliterated.setText(transliteration.transliterate(langCode,
								langCodeToBeTransliterated, textField.getText()));
					} catch (RuntimeException runtimeException) {
						LOGGER.error(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
								"Exception occured while transliterating secondary language for field : "
										+ textField.getId()  + " due to >>>> " + runtimeException.getMessage());
					}
				}
			}
		}
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}


	@Override
	public void fillData(Object data) {
		selectAndSet(data);
	}

	@Override
	public void selectAndSet(Object data) {
		if (data == null) {
			getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(langCode -> {
				TextField textField = (TextField) getField(uiFieldDTO.getId() + langCode);
				if(textField != null) { textField.clear(); }
			});
			return;
		}

		if (data instanceof String) {

			TextField textField = (TextField) getField(
					uiFieldDTO.getId() + getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

			textField.setText((String) data);
		} else if (data instanceof List) {

			List<SimpleDto> list = (List<SimpleDto>) data;

			for (SimpleDto simpleDto : list) {

				TextField textField = (TextField) getField(uiFieldDTO.getId() + simpleDto.getLanguage());

				if (textField != null) {
					textField.setText(simpleDto.getValue());
				}
			}

		}

	}


	/**
	 *
	 * Setting the focus to specific fields when keyboard loads
	 * @param event
	 * @param keyBoard
	 * @param langCode
	 * @param textField 
	 *
	 */
	public void setFocusOnField(MouseEvent event, VirtualKeyboard keyBoard, String langCode, TextField textField) {
		try {
			Node node = (Node) event.getSource();
			node.requestFocus();
			Node parentNode = node.getParent().getParent().getParent();
			if (genericController.isKeyboardVisible()) {
				genericController.getKeyboardStage().close();
				genericController.setKeyboardVisible(false);
				if (!textField.getId().equalsIgnoreCase(genericController.getPreviousId())) {
					openKeyBoard(keyBoard, langCode, textField, parentNode);
				}
			} else {
				openKeyBoard(keyBoard, langCode, textField, parentNode);
			}
		} catch (RuntimeException runtimeException) {
			LOGGER.error(loggerClassName, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}
	
	private void openKeyBoard(VirtualKeyboard keyBoard, String langCode, TextField textField, Node parentNode) {
		if (genericController.getKeyboardStage() != null)  {
			genericController.getKeyboardStage().close();
		}
		keyboardNode = keyBoard.view();
		keyBoard.setParentStage(fxComponents.getStage());
		keyboardNode.setVisible(true);
		keyboardNode.setManaged(true);
		getField(textField.getId()).requestFocus();
		openKeyBoardPopUp();
		genericController.setPreviousId(textField.getId());
		genericController.setKeyboardVisible(true);
	}

	private GridPane prepareMainGridPaneForKeyboard() {
		GridPane gridPane = new GridPane();
		gridPane.setPrefWidth(740);

		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(10);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(80);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(10);
		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);
	
		return gridPane;
	}
	
	private void openKeyBoardPopUp() {
		try {
			Stage keyBoardStage = new Stage();
			genericController.setKeyboardStage(keyBoardStage);
			keyBoardStage.setAlwaysOnTop(true);
			keyBoardStage.initStyle(StageStyle.UNDECORATED);
			keyBoardStage.setX(300);
			keyBoardStage.setY(500);
			GridPane gridPane = prepareMainGridPaneForKeyboard();
			gridPane.addColumn(1, keyboardNode);
			Scene scene = new Scene(gridPane);
			scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(validation.getCssName()).toExternalForm());
			gridPane.getStyleClass().add(RegistrationConstants.KEYBOARD_PANE);
			keyBoardStage.setScene(scene);
			makeDraggable(keyBoardStage, gridPane);
			genericController.setKeyboardStage(keyBoardStage);
			keyBoardStage.show();
		} catch (Exception exception) {
			LOGGER.error(loggerClassName, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));

			validation.generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_SCAN_POPUP));
		}
	}
	
	private static void makeDraggable(final Stage stage, final Node node) {
	    node.setOnMousePressed(mouseEvent -> {
	    	// record a distance for the drag and drop operation.
	    	xPosition = stage.getX() - mouseEvent.getScreenX();
	    	yPosition = stage.getY() - mouseEvent.getScreenY();
	    	node.setCursor(Cursor.MOVE);
	    });
	    node.setOnMouseReleased(mouseEvent -> node.setCursor(Cursor.HAND));
	    node.setOnMouseDragged(mouseEvent -> {
	    	stage.setX(mouseEvent.getScreenX() + xPosition);
	    	stage.setY(mouseEvent.getScreenY() + yPosition);
	    });
	    node.setOnMouseEntered(mouseEvent -> {
	    	if (!mouseEvent.isPrimaryButtonDown()) {
	    		node.setCursor(Cursor.HAND);
	    	}
	    });
	    node.setOnMouseExited(mouseEvent -> {
	    	if (!mouseEvent.isPrimaryButtonDown()) {
	    		node.setCursor(Cursor.DEFAULT);
	    	}
	    });
	}
}
