package io.mosip.registration.controller;

import static io.mosip.registration.constants.RegistrationConstants.EMPTY;
import static io.mosip.registration.constants.RegistrationConstants.HASH;
import static io.mosip.registration.constants.RegistrationConstants.REG_AUTH_PAGE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.registration.dto.schema.ProcessSpecDto;
import io.mosip.registration.entity.LocationHierarchy;
import io.mosip.registration.enums.FlowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.idvalidator.spi.PridValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.ProcessNames;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.auth.AuthenticationController;
import io.mosip.registration.controller.reg.RegistrationController;
import io.mosip.registration.controller.reg.RegistrationPreviewController;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.ErrorResponseDTO;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.dto.SuccessResponseDTO;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.dto.schema.UiScreenDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.exception.RegistrationExceptionConstants;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.service.sync.PreRegistrationDataSyncService;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.util.control.impl.BiometricFxControl;
import io.mosip.registration.util.control.impl.ButtonFxControl;
import io.mosip.registration.util.control.impl.CheckBoxFxControl;
import io.mosip.registration.util.control.impl.DOBAgeFxControl;
import io.mosip.registration.util.control.impl.DOBFxControl;
import io.mosip.registration.util.control.impl.DocumentFxControl;
import io.mosip.registration.util.control.impl.DropDownFxControl;
import io.mosip.registration.util.control.impl.HtmlFxControl;
import io.mosip.registration.util.control.impl.TextFieldFxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import lombok.SneakyThrows;

/**
 * {@code GenericController} is to capture the demographic/demo/Biometric
 * details
 *
 * @author YASWANTH S
 * @since 1.0.0
 *
 */

@Controller
public class GenericController extends BaseController {

	protected static final Logger LOGGER = AppConfig.getLogger(GenericController.class);

	private static final String LABEL_CLASS = "additionaInfoReqIdLabel";
	private static final String TEXTFIELD_CLASS = "preregFetchBtnStyle";
	private static final String CONTROLTYPE_TEXTFIELD = "textbox";
	private static final String CONTROLTYPE_BIOMETRICS = "biometrics";
	private static final String CONTROLTYPE_DOCUMENTS = "fileupload";
	private static final String CONTROLTYPE_DROPDOWN = "dropdown";
	private static final String CONTROLTYPE_CHECKBOX = "checkbox";
	private static final String CONTROLTYPE_BUTTON = "button";
	private static final String CONTROLTYPE_DOB = "date";
	private static final String CONTROLTYPE_DOB_AGE = "ageDate";
	private static final String CONTROLTYPE_HTML = "html";

	/**
	 * Top most Grid pane in FXML
	 */
	@FXML
	private GridPane genericScreen;

	@FXML
	private AnchorPane anchorPane;

	@FXML
	private AnchorPane navigationAnchorPane;

	@FXML
	private Button next;

	@FXML
	private Button authenticate;

	@FXML
	private Label notification;

	@Autowired
	private RegistrationController registrationController;

	@Autowired
	private AuthenticationController authenticationController;

	@Autowired
	private MasterSyncDao masterSyncDao;

	@Autowired
	private MasterSyncService masterSyncService;

	@Autowired
	private RegistrationPreviewController registrationPreviewController;

	@Autowired
	private RequiredFieldValidator requiredFieldValidator;

	@Autowired
	private PridValidator<String> pridValidatorImpl;

	@Autowired
	private PreRegistrationDataSyncService preRegistrationDataSyncService;

	private ApplicationContext applicationContext = ApplicationContext.getInstance();

	private static TreeMap<Integer, UiScreenDTO> orderedScreens = new TreeMap<>();
	private static Map<String, FxControl> fxControlMap = new HashMap<String, FxControl>();
	
	private Stage keyboardStage;
	
	private boolean keyboardVisible = false;
	
	private String previousId;

	public static Map<String, TreeMap<Integer, String>> hierarchyLevels = new HashMap<String, TreeMap<Integer, String>>();
	public static Map<String, TreeMap<Integer, String>> currentHierarchyMap = new HashMap<String, TreeMap<Integer, String>>();
	public static List<UiFieldDTO> fields = new ArrayList<>();

	public static Map<String, FxControl> getFxControlMap() {
		return fxControlMap;
	}

	private void initialize(RegistrationDTO registrationDTO) {
		orderedScreens.clear();
		fxControlMap.clear();
		hierarchyLevels.clear();
		currentHierarchyMap.clear();
		fillHierarchicalLevelsByLanguage();
		anchorPane.prefWidthProperty().bind(genericScreen.widthProperty());
		anchorPane.prefHeightProperty().bind(genericScreen.heightProperty());
		fields = getAllFields(registrationDTO.getProcessId(), registrationDTO.getIdSchemaVersion());
	}


	private void fillHierarchicalLevelsByLanguage() {
		for(String langCode : getConfiguredLangCodes()) {
			TreeMap<Integer, String> hierarchicalData = new TreeMap<>();
			List<LocationHierarchy> hierarchies = masterSyncDao.getAllLocationHierarchy(langCode);
			hierarchies.forEach( hierarchy -> {
				hierarchicalData.put(hierarchy.getHierarchyLevel(), hierarchy.getHierarchyLevelName());
			});
			hierarchyLevels.put(langCode, hierarchicalData);
		}
	}

	private HBox getPreRegistrationFetchComponent() {
		String langCode = getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().get(0);

		HBox hBox = new HBox();
		hBox.setAlignment(Pos.CENTER_LEFT);
		hBox.setSpacing(20);
		hBox.setPrefHeight(100);
		hBox.setPrefWidth(200);

		Label label = new Label();
		label.getStyleClass().add(LABEL_CLASS);
		label.setId("preRegistrationLabel");
		label.setText(applicationContext.getBundle(langCode, RegistrationConstants.LABELS)
				.getString("search_for_Pre_registration_id"));
		hBox.getChildren().add(label);
		TextField textField = new TextField();
		textField.setId("preRegistrationId");
		textField.getStyleClass().add(TEXTFIELD_CLASS);
		hBox.getChildren().add(textField);
		Button button = new Button();
		button.setId("fetchBtn");
		button.getStyleClass().add("demoGraphicPaneContentButton");
		button.setText(applicationContext.getBundle(langCode, RegistrationConstants.LABELS)
				.getString("fetch"));

		button.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				boolean isValid = false;
				try {
					isValid = pridValidatorImpl.validateId(textField.getText());
				} catch (InvalidIDException invalidIDException) { isValid = false; }

				if(!isValid) {
					generateAlertLanguageSpecific(RegistrationConstants.ERROR, RegistrationUIConstants.PRE_REG_ID_NOT_VALID);
					return;
				}

				ResponseDTO responseDTO = preRegistrationDataSyncService.getPreRegistration(textField.getText(), false);

				try {
					loadPreRegSync(responseDTO);
					getRegistrationDTOFromSession().setPreRegistrationId(textField.getText());
					getRegistrationDTOFromSession().setAppId(textField.getText());

				} catch (RegBaseCheckedException exception) {
					generateAlertLanguageSpecific(RegistrationConstants.ERROR, responseDTO.getErrorResponseDTOs().get(0).getMessage());
				}
			}
		});

		hBox.getChildren().add(button);
		return hBox;
	}

	private HBox getAdditionalInfoRequestIdComponent() {
		String langCode = getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().get(0);
		HBox hBox = new HBox();
		hBox.setAlignment(Pos.CENTER_LEFT);
		hBox.setSpacing(20);
		hBox.setPrefHeight(100);
		hBox.setPrefWidth(200);
		Label label = new Label();
		label.getStyleClass().add(LABEL_CLASS);
		label.setId("additionalInfoRequestIdLabel");
		label.setText(applicationContext.getBundle(langCode, RegistrationConstants.LABELS)
				.getString("additionalInfoRequestId"));
		hBox.getChildren().add(label);
		TextField textField = new TextField();
		textField.setId("additionalInfoRequestId");
		textField.getStyleClass().add(TEXTFIELD_CLASS);
		hBox.getChildren().add(textField);

		textField.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				getRegistrationDTOFromSession().setAdditionalRequestId(textField.getText());
			}
		});
		return hBox;
	}

	private boolean isAdditionalInfoRequestIdProvided(UiScreenDTO screenDTO) {
		if(screenDTO.isAdditionalInfoRequestIdRequired()) {
			TextField textField = (TextField) anchorPane.lookup("#additionalInfoRequestId");
			if(textField.getText() == null || textField.getText().isBlank()) {
				generateAlertLanguageSpecific(RegistrationConstants.ERROR, RegistrationUIConstants.ADDITIONAL_INFO_REQ_ID_MISSING);
				return false;
			}
		}
		return true;
	}

	private void loadPreRegSync(ResponseDTO responseDTO) throws RegBaseCheckedException{
		auditFactory.audit(AuditEvent.REG_DEMO_PRE_REG_DATA_FETCH, Components.REG_DEMO_DETAILS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		
		SuccessResponseDTO successResponseDTO = responseDTO.getSuccessResponseDTO();
		List<ErrorResponseDTO> errorResponseDTOList = responseDTO.getErrorResponseDTOs();

		if (errorResponseDTOList != null && !errorResponseDTOList.isEmpty() || 
				successResponseDTO==null || 
				successResponseDTO.getOtherAttributes() == null || 
				!successResponseDTO.getOtherAttributes().containsKey(RegistrationConstants.REGISTRATION_DTO)) {
			throw new RegBaseCheckedException(RegistrationExceptionConstants.PRE_REG_SYNC_FAIL.getErrorCode(),
					RegistrationExceptionConstants.PRE_REG_SYNC_FAIL.getErrorMessage());
		}

		RegistrationDTO preRegistrationDTO = (RegistrationDTO) successResponseDTO.getOtherAttributes()
				.get(RegistrationConstants.REGISTRATION_DTO);

		for (UiScreenDTO screenDTO : orderedScreens.values()) {
			for (UiFieldDTO field : screenDTO.getFields()) {

				FxControl fxControl = getFxControl(field.getId());

				if (fxControl != null) {

					if (preRegistrationDTO.getDemographics().containsKey(field.getId())) {
						fxControl.selectAndSet(preRegistrationDTO.getDemographics().get(field.getId()));
					}

					else if (preRegistrationDTO.getDocuments().containsKey(field.getId())) {
						fxControl.selectAndSet(preRegistrationDTO.getDocuments().get(field.getId()));
					}
				}
			}
		}
	}


	private void getScreens(List<UiScreenDTO> screenDTOS) {
		screenDTOS.forEach( dto -> {
			orderedScreens.put(dto.getOrder(), dto);
		});
	}

	private Map<String, List<UiFieldDTO>> getFieldsBasedOnAlignmentGroup(List<UiFieldDTO> screenFields) {
		Map<String, List<UiFieldDTO>> groupedScreenFields = new LinkedHashMap<>();
		if(screenFields == null || screenFields.isEmpty())
			return groupedScreenFields;

		//Applies only during Update flow
		if(getRegistrationDTOFromSession().getUpdatableFieldGroups() != null) {
			screenFields = screenFields.stream()
					.filter(f -> f.getGroup() != null && (getRegistrationDTOFromSession().getUpdatableFieldGroups().contains(f.getGroup()) ||
							getRegistrationDTOFromSession().getDefaultUpdatableFieldGroups().contains(f.getGroup())) )
					.collect(Collectors.toList());
			screenFields.forEach(f -> { getRegistrationDTOFromSession().getUpdatableFields().add(f.getId()); });
		}

		screenFields.forEach( field -> {
				String alignmentGroup = field.getAlignmentGroup() == null ? field.getId()+"TemplateGroup"
						: field.getAlignmentGroup();

				if(field.isInputRequired()) {
					if(!groupedScreenFields.containsKey(alignmentGroup))
						groupedScreenFields.put(alignmentGroup, new LinkedList<UiFieldDTO>());

					groupedScreenFields.get(alignmentGroup).add(field);
				}
		});
		return groupedScreenFields;
	}

	private GridPane getScreenGridPane(String screenName) {
		GridPane gridPane = new GridPane();
		gridPane.setId(screenName);
		RowConstraints topRowConstraints = new RowConstraints();
		topRowConstraints.setPercentHeight(2);
		RowConstraints midRowConstraints = new RowConstraints();
		midRowConstraints.setPercentHeight(93);
		RowConstraints bottomRowConstraints = new RowConstraints();
		bottomRowConstraints.setPercentHeight(5);
		gridPane.getRowConstraints().addAll(topRowConstraints,midRowConstraints, bottomRowConstraints);

		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(5);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(90);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(5);

		gridPane.getColumnConstraints().addAll(columnConstraint1, columnConstraint2,
				columnConstraint3);

		return gridPane;
	}

	private GridPane getScreenGroupGridPane(String id, GridPane screenGridPane) {
		GridPane groupGridPane = new GridPane();
		groupGridPane.setId(id);
		groupGridPane.prefWidthProperty().bind(screenGridPane.widthProperty());
		groupGridPane.getColumnConstraints().clear();
		ColumnConstraints columnConstraint = new ColumnConstraints();
		columnConstraint.setPercentWidth(100);
		groupGridPane.getColumnConstraints().add(columnConstraint);
		groupGridPane.setHgap(20);
		groupGridPane.setVgap(20);
		return groupGridPane;
	}

	private void addNavigationButtons(ProcessSpecDto processSpecDto) {

		Label navigationLabel = new Label();
		navigationLabel.getStyleClass().add(LABEL_CLASS);
		navigationLabel.setText(RegistrationConstants.SLASH + RegistrationConstants.SPACE +
				processSpecDto.getLabel().get(ApplicationContext.applicationLanguage()));

		navigationAnchorPane.getChildren().add(navigationLabel);
		AnchorPane.setTopAnchor(navigationLabel, 5.0);
		AnchorPane.setLeftAnchor(navigationLabel, 10.0);

		next.setOnAction(getNextActionHandler());
		authenticate.setOnAction(getRegistrationAuthActionHandler());
	}

	private String getScreenName(Tab tab) {
		return tab.getId().replace("_tab", EMPTY);
	}

	private boolean refreshScreenVisibility(String screenName) {
		boolean atLeastOneVisible = true;
		Optional<UiScreenDTO> screenDTO = orderedScreens.values()
				.stream()
				.filter(screen -> screen.getName().equals(screenName))
				.findFirst();

		if(screenDTO.isPresent()) {
			LOGGER.info("Refreshing Screen: {}", screenName);
			screenDTO.get().getFields().forEach( field -> {
				FxControl fxControl = getFxControl(field.getId());
				if(fxControl != null)
					fxControl.refresh();
			});

			atLeastOneVisible = screenDTO.get()
					.getFields()
					.stream()
					.anyMatch( field -> getFxControl(field.getId()) != null && getFxControl(field.getId()).getNode().isVisible() );
		}
		LOGGER.info("Screen refreshed, Screen: {} visible : {}", screenName, atLeastOneVisible);
		return atLeastOneVisible;
	}

	private EventHandler getNextActionHandler() {
		return new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				TabPane tabPane = (TabPane) anchorPane.lookup(HASH+getRegistrationDTOFromSession().getRegistrationId());
				tabPane.getTabs().size();
				int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();
				while(selectedIndex < tabPane.getTabs().size()) {
					selectedIndex++;
					if(!tabPane.getTabs().get(selectedIndex).isDisabled()) {
						tabPane.getSelectionModel().select(selectedIndex);
						break;
					}
				}
			}
		};
	}

	private EventHandler getRegistrationAuthActionHandler() {
		return new EventHandler<ActionEvent>() {
			@SneakyThrows
			@Override
			public void handle(ActionEvent event) {
				TabPane tabPane = (TabPane) anchorPane.lookup(HASH+getRegistrationDTOFromSession().getRegistrationId());
				String incompleteScreen = getInvalidScreenName(tabPane);

				if(incompleteScreen == null) {
					generateAlert(RegistrationConstants.ERROR, incompleteScreen +" Screen with ERROR !");
					return;
				}
				authenticationController.goToNextPage();
			}
		};
	}

	private void setTabSelectionChangeEventHandler(TabPane tabPane) {

		tabPane.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>(){
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				LOGGER.debug("Old selection : {} New Selection : {}", oldValue, newValue);
				
				if (isKeyboardVisible() && keyboardStage != null) {
					keyboardStage.close();
				}

				int newSelection = newValue.intValue() < 0 ? 0 : newValue.intValue();
				final String newScreenName = tabPane.getTabs().get(newSelection).getId().replace("_tab", EMPTY);

				//Hide continue button in preview page
				next.setVisible(newScreenName.equals("AUTH") ? false : true);
				authenticate.setVisible(newScreenName.equals("AUTH") ? true : false);

				//request to load Preview / Auth page, allowed only when no errors are found in visible screens
				if((newScreenName.equals("AUTH") || newScreenName.equals("PREVIEW")) && getInvalidScreenName(tabPane).equals(EMPTY)) {
					notification.setVisible(false);
					loadPreviewOrAuthScreen(tabPane, tabPane.getTabs().get(newValue.intValue()));
					return;
				}

				//Refresh screen visibility
				tabPane.getTabs().get(newSelection).setDisable(!refreshScreenVisibility(newScreenName));
				boolean isSelectedDisabledTab = tabPane.getTabs().get(newSelection).isDisabled();

				if(oldValue.intValue() <0 && !isSelectedDisabledTab) {
					tabPane.getSelectionModel().selectFirst();
					return;
				}

				if(isSelectedDisabledTab) {
					tabPane.getSelectionModel().selectNext();
					return;
				}

				if(oldValue.intValue() < newSelection && !isScreenValid(tabPane.getTabs().get(oldValue.intValue()).getId())) {
					LOGGER.error("Current screen is not fully valid : {}", oldValue.intValue());
					tabPane.getSelectionModel().select(oldValue.intValue());
					return;
				}
				tabPane.getSelectionModel().select(newValue.intValue());
			}
		});
	}

	private boolean isScreenValid(final String screenName) {
		Optional<UiScreenDTO> result = orderedScreens.values()
				.stream().filter(screen -> screen.getName().equals(screenName.replace("_tab", EMPTY))).findFirst();

		boolean isValid = true;
		if(result.isPresent()) {
			if(!isAdditionalInfoRequestIdProvided(result.get())) { return false; }

			for(UiFieldDTO field : result.get().getFields()) {
				if(getFxControl(field.getId()) != null && !getFxControl(field.getId()).canContinue()) {
					LOGGER.error("Screen validation , fieldId : {} has invalid value", field.getId());
					String label = getFxControl(field.getId()).getUiSchemaDTO().getLabel().getOrDefault(ApplicationContext.applicationLanguage(), field.getId());
					notification.setText(applicationContext.getBundle(ApplicationContext.applicationLanguage(), RegistrationConstants.MESSAGES)
							.getString("SCREEN_VALIDATION_ERROR") + " [ " + label + " ]");
					notification.setVisible(true);
					isValid = false;
					break;
				}
			}
		}
		if (isValid) {
			notification.setText(EMPTY);
			//notification.setVisible(false);
			auditFactory.audit(AuditEvent.REG_NAVIGATION, Components.REGISTRATION_CONTROLLER,
					SessionContext.userContext().getUserId(), AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		}
		return isValid;
	}

	private String getInvalidScreenName(TabPane tabPane) {
		String errorScreen = EMPTY;
		for(UiScreenDTO screen : orderedScreens.values()) {
			LOGGER.error("Started to validate screen : {} ", screen.getName());

			if(!isAdditionalInfoRequestIdProvided(screen)) {
				LOGGER.error("Screen validation failed {}, Additional Info request Id is required", screen.getName());
				errorScreen = screen.getName();
				break;
			}

			boolean anyInvalidField = screen.getFields()
					.stream()
					.anyMatch( field -> getFxControl(field.getId()) != null &&
							getFxControl(field.getId()).canContinue() == false );

			if(anyInvalidField) {
				LOGGER.error("Screen validation failed {}", screen.getName());
				errorScreen = screen.getName();
				break;
			}
		}
		return errorScreen;
	}

	private TabPane createTabPane(ProcessSpecDto processSpecDto) {
		TabPane tabPane = new TabPane();
		tabPane.setId(getRegistrationDTOFromSession().getRegistrationId());
		tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		tabPane.prefWidthProperty().bind(anchorPane.widthProperty());
		tabPane.prefHeightProperty().bind(anchorPane.heightProperty());

		setTabSelectionChangeEventHandler(tabPane);
		anchorPane.getChildren().add(tabPane);
		addNavigationButtons(processSpecDto);
		return tabPane;
	}

	public void populateScreens() throws Exception {
		RegistrationDTO registrationDTO = getRegistrationDTOFromSession();
		LOGGER.debug("Populating Dynamic screens for process : {}", registrationDTO.getProcessId());
		initialize(registrationDTO);
		ProcessSpecDto processSpecDto = getProcessSpec(registrationDTO.getProcessId(), registrationDTO.getIdSchemaVersion());
		getScreens(processSpecDto.getScreens());
		TabPane tabPane = createTabPane(processSpecDto);

		for(UiScreenDTO screenDTO : orderedScreens.values()) {
			Map<String, List<UiFieldDTO>> screenFieldGroups = getFieldsBasedOnAlignmentGroup(screenDTO.getFields());

			List<String> labels = new ArrayList<>();
			getRegistrationDTOFromSession().getSelectedLanguagesByApplicant().forEach(langCode -> {
				labels.add(screenDTO.getLabel().get(langCode));
			});

			Tab screenTab = new Tab();
			screenTab.setId(screenDTO.getName()+"_tab");
			screenTab.setText(labels.get(0));
			screenTab.setTooltip(new Tooltip(String.join(RegistrationConstants.SLASH, labels)));

			if(screenFieldGroups == null || screenFieldGroups.isEmpty())
				screenTab.setDisable(true);

			GridPane screenGridPane = getScreenGridPane(screenDTO.getName());
			screenGridPane.prefWidthProperty().bind(tabPane.widthProperty());
			screenGridPane.prefHeightProperty().bind(tabPane.heightProperty());

			int rowIndex = 0;
			GridPane gridPane = getScreenGroupGridPane(screenGridPane.getId()+"_col_1", screenGridPane);

			if(screenDTO.isPreRegFetchRequired()) {
				gridPane.add(getPreRegistrationFetchComponent(), 0, rowIndex++);
			}
			if(screenDTO.isAdditionalInfoRequestIdRequired()) {
				gridPane.add(getAdditionalInfoRequestIdComponent(), 0, rowIndex++);
			}

			for(Entry<String, List<UiFieldDTO>> groupEntry : screenFieldGroups.entrySet()) {
				FlowPane groupFlowPane = new FlowPane();
				groupFlowPane.prefWidthProperty().bind(gridPane.widthProperty());
				groupFlowPane.setHgap(20);
				groupFlowPane.setVgap(20);

				for(UiFieldDTO fieldDTO : groupEntry.getValue()) {
					try {
						FxControl fxControl = buildFxElement(fieldDTO);
						if(fxControl.getNode() instanceof GridPane) {
							((GridPane)fxControl.getNode()).prefWidthProperty().bind(groupFlowPane.widthProperty());
						}
						groupFlowPane.getChildren().add(fxControl.getNode());
					} catch (Exception exception){
						LOGGER.error("Failed to build control " + fieldDTO.getId(), exception);
					}
				}
				gridPane.add(groupFlowPane, 0, rowIndex++);
			}

			screenGridPane.setStyle("-fx-background-color: white;");
			screenGridPane.add(gridPane, 1, 1);
			final ScrollPane scrollPane = new ScrollPane(screenGridPane);
			scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
			scrollPane.setId("scrollPane");
			screenTab.setContent(scrollPane);
			tabPane.getTabs().add(screenTab);
		}

		//refresh to reflect the initial visibility configuration
		refreshFields();
		addPreviewAndAuthScreen(tabPane);
	}


	private void addPreviewAndAuthScreen(TabPane tabPane) throws Exception {
		List<String> previewLabels = new ArrayList<>();
		List<String> authLabels = new ArrayList<>();
		for (String langCode : getRegistrationDTOFromSession().getSelectedLanguagesByApplicant()) {
			previewLabels.add(applicationContext.getBundle(langCode, RegistrationConstants.LABELS)
					.getString(RegistrationConstants.previewHeader));
			authLabels.add(applicationContext.getBundle(langCode, RegistrationConstants.LABELS)
					.getString(RegistrationConstants.authentication));
		}

		Tab previewScreen = new Tab();
		previewScreen.setId("PREVIEW");
		previewScreen.setText(previewLabels.get(0));
		previewScreen.setTooltip(new Tooltip(String.join(RegistrationConstants.SLASH, previewLabels)));
		tabPane.getTabs().add(previewScreen);

		Tab authScreen = new Tab();
		authScreen.setId("AUTH");
		authScreen.setText(authLabels.get(0));
		authScreen.setTooltip(new Tooltip(String.join(RegistrationConstants.SLASH, authLabels)));
		tabPane.getTabs().add(authScreen);
	}

	private void loadPreviewOrAuthScreen(TabPane tabPane, Tab tab) {
		switch (tab.getId()) {
			case "PREVIEW":
				try {
					tabPane.getSelectionModel().select(tab);
					tab.setContent(getPreviewContent(tabPane));
				} catch (Exception exception) {
					LOGGER.error("Failed to load preview page!!, clearing registration data.");
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_PREVIEW_PAGE));
				}
				break;

			case "AUTH":
				try {
					tabPane.getSelectionModel().select(tab);
					tab.setContent(loadAuthenticationPage(tabPane));
					authenticationController.initData(ProcessNames.PACKET.getType());
				} catch (Exception exception) {
					LOGGER.error("Failed to load auth page!!, clearing registration data.");
					generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_APPROVAL_PAGE));
				}
				break;
		}
	}

	private Node getPreviewContent(TabPane tabPane) throws Exception {
		String content = registrationPreviewController.getPreviewContent();
		if(content != null) {
			final WebView webView = new WebView();
			webView.setId("webView");
			webView.prefWidthProperty().bind(tabPane.widthProperty());
			webView.prefHeightProperty().bind(tabPane.heightProperty());
			webView.getEngine().loadContent(content);
			final GridPane gridPane = new GridPane();
			gridPane.prefWidthProperty().bind(tabPane.widthProperty());
			gridPane.prefHeightProperty().bind(tabPane.heightProperty());
			gridPane.setAlignment(Pos.TOP_LEFT);
			gridPane.getChildren().add(webView);
			return gridPane;
		}
		throw new RegBaseCheckedException("", "Failed to load preview screen");
	}

	private Node loadAuthenticationPage(TabPane tabPane) throws Exception {
		GridPane gridPane = (GridPane)BaseController.load(getClass().getResource(REG_AUTH_PAGE));
		gridPane.prefWidthProperty().bind(tabPane.widthProperty());
		gridPane.prefHeightProperty().bind(tabPane.heightProperty());

		Node node = gridPane.lookup("#backButton");
		if(node != null) {
			node.setVisible(false);
			node.setDisable(true);
		}

		node = gridPane.lookup("#operatorAuthContinue");
		if(node != null) {
			node.setVisible(false);
			node.setDisable(true);
		}
		return gridPane;
	}


	private FxControl buildFxElement(UiFieldDTO uiFieldDTO) throws Exception {
		LOGGER.info("Building fxControl for field : {}", uiFieldDTO.getId());

		FxControl fxControl = null;
		if (uiFieldDTO.getControlType() != null) {
			switch (uiFieldDTO.getControlType()) {
				case CONTROLTYPE_TEXTFIELD:
					fxControl = new TextFieldFxControl().build(uiFieldDTO);
					break;

				case CONTROLTYPE_BIOMETRICS:
					fxControl = new BiometricFxControl(getProofOfExceptionFields()).build(uiFieldDTO);
					break;

				case CONTROLTYPE_BUTTON:
					fxControl =  new ButtonFxControl().build(uiFieldDTO);
					break;

				case CONTROLTYPE_CHECKBOX:
					fxControl = new CheckBoxFxControl().build(uiFieldDTO);
					break;

				case CONTROLTYPE_DOB:
					fxControl =  new DOBFxControl().build(uiFieldDTO);
					break;

				case CONTROLTYPE_DOB_AGE:
					fxControl =  new DOBAgeFxControl().build(uiFieldDTO);
					break;

				case CONTROLTYPE_DOCUMENTS:
					fxControl =  new DocumentFxControl().build(uiFieldDTO);
					break;

				case CONTROLTYPE_DROPDOWN:
					fxControl = new DropDownFxControl().build(uiFieldDTO);
					break;
				case CONTROLTYPE_HTML:
					fxControl = new HtmlFxControl().build(uiFieldDTO);
					break;
			}
		}

		if(fxControl == null)
			throw  new Exception("Failed to build fxControl");

		fxControlMap.put(uiFieldDTO.getId(), fxControl);
		return fxControl;
	}

	public void refreshFields() {
		orderedScreens.values().forEach(screen -> { refreshScreenVisibility(screen.getName()); });
	}

	public List<UiFieldDTO> getProofOfExceptionFields() {
		return fields.stream().filter(field ->
				field.getSubType().contains(RegistrationConstants.POE_DOCUMENT)).collect(Collectors.toList());
	}

	private FxControl getFxControl(String fieldId) {
		return GenericController.getFxControlMap().get(fieldId);
	}
	
	public Stage getKeyboardStage() {		
		return keyboardStage;
	}
	
	public void setKeyboardStage(Stage keyboardStage) {
		this.keyboardStage = keyboardStage;
	}

	public boolean isKeyboardVisible() {
		return keyboardVisible;
	}

	public void setKeyboardVisible(boolean keyboardVisible) {
		this.keyboardVisible = keyboardVisible;
	}

	public String getPreviousId() {
		return previousId;
	}

	public void setPreviousId(String previousId) {
		this.previousId = previousId;
	}
}