package io.mosip.registration.controller.reg;

import static io.mosip.registration.constants.LoggerConstants.LOG_PACKET_UPLOAD;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_ID;
import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.collections.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationClientStatusCode;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.BaseController;
import io.mosip.registration.controller.vo.PacketStatusVO;
import io.mosip.registration.dto.PacketStatusDTO;
import io.mosip.registration.dto.ResponseDTO;
import io.mosip.registration.exception.RegBaseCheckedException;
import io.mosip.registration.service.packet.PacketHandlerService;
import io.mosip.registration.service.packet.PacketUploadService;
import io.mosip.registration.service.sync.PacketSynchService;
import io.mosip.registration.util.healthcheck.RegistrationAppHealthCheckUtil;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

@Controller
public class PacketUploadController extends BaseController implements Initializable {
	
	@FXML
	private StackPane progressIndicatorPane;

	@FXML
	private ProgressIndicator progressIndicator;

	@Autowired
	private PacketUploadService packetUploadService;
	
	@Autowired
	private PacketHandlerService packetHandlerService;

	@FXML
	private TableColumn<PacketStatusDTO, String> fileNameColumn;

	@FXML
	private TableView<PacketStatusVO> table;

	@FXML
	private TableColumn<PacketStatusVO, Boolean> checkBoxColumn;

	@FXML
	private TableColumn<PacketStatusVO, Boolean> regDate;

	@FXML
	private TableColumn<PacketStatusVO, Boolean> slno;

	@FXML
	private Button saveToDevice;

	@FXML
	private Button uploadBtn;

	@FXML
	private TextField filterField;

	@FXML
	private TableColumn<PacketStatusDTO, String> fileColumn;

	@FXML
	private TableColumn<PacketStatusDTO, String> statusColumn;
	
	@FXML
	private TableColumn<PacketStatusDTO, String> clientStatus;
	
	@FXML
	private TableColumn<PacketStatusDTO, String> serverStatus;
	
	@FXML
	private TableColumn<PacketStatusDTO, String> operatorId;
	
	@FXML
	private ComboBox<String> clientStatusComboBox;
	
	@FXML
	private ComboBox<String> serverStatusComboBox;

	@FXML
	private Button clearFilters;
	
	@Autowired
	private PacketSynchService packetSynchService;

	@Autowired
	private PacketExportController packetExportController;

	@FXML
	private CheckBox selectAllCheckBox;

	@FXML
	private ImageView exportCSVIcon;

	private ObservableList<PacketStatusVO> list;

	private List<PacketStatusVO> selectedPackets = new ArrayList<>();

	private static final Logger LOGGER = AppConfig.getLogger(PacketUploadController.class);

	private ObservableList<PacketStatusVO> observableList;

	private SortedList<PacketStatusVO> sortedList;

	private Stage stage;
	
	private List<PacketStatusVO> packetsToBeUploaded = new ArrayList<>();
	
	private List<PacketStatusVO> uploadedPackets = new ArrayList<>();

	@FXML
	private GridPane packetUploadPane;
	
	@FXML
	private GridPane uploadPacketRoot;

	public GridPane getUploadPacketRoot() {
		return uploadPacketRoot;
	}

	/**
	 * This method is used to Sync as well as upload the packets.
	 * 
	 */
	public void syncAndUploadPacket() {
		LOGGER.info("REGISTRATION - SYNC_PACKETS_AND_PUSH_TO_SERVER - PACKET_UPLOAD_CONTROLLER", APPLICATION_NAME,
				APPLICATION_ID, "Sync the packets and push it to the server");
		
		packetUploadPane.setDisable(true);
		progressIndicatorPane.setVisible(true);
		
		observableList.clear();
		table.refresh();
		service.reset();

		if (!RegistrationAppHealthCheckUtil.isNetworkAvailable()) {
			packetUploadPane.setDisable(false);
			progressIndicatorPane.setVisible(false);
			loadInitialPage();
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.NETWORK_ERROR));
			return;
		}

		if (selectedPackets.isEmpty()) {
			packetUploadPane.setDisable(false);
			progressIndicatorPane.setVisible(false);
			loadInitialPage();
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PACKET_UPLOAD_EMPTY_ERROR));
			return;
		}

		List<String> selectedAppIDs = selectedPackets.stream().map(PacketStatusVO::getFileName).collect(Collectors.toList());
		ResponseDTO responseDTO = packetSynchService.syncPacket(RegistrationConstants.JOB_TRIGGER_POINT_USER, selectedAppIDs);
		if(responseDTO.getErrorResponseDTOs() != null && !responseDTO.getErrorResponseDTOs().isEmpty()) {
			selectAllCheckBox.setSelected(false);
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SYNC_FAILURE));
		}

		auditFactory.audit(AuditEvent.UPLOAD_PACKET, Components.UPLOAD_PACKET,
				SessionContext.userContext().getUserId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		progressIndicator.progressProperty().bind(service.progressProperty());
		service.start();
		service.setOnSucceeded(event -> {
			packetUploadPane.setDisable(false);
			progressIndicatorPane.setVisible(false);
			
			String status = service.getValue();
			if (!status.equals(RegistrationConstants.EMPTY)) {
				generateAlert(RegistrationConstants.ERROR, status);
			}
		});
		service.setOnFailed(event -> {
			packetUploadPane.setDisable(false);
			progressIndicatorPane.setVisible(false);
		});
	}

	/**
	 * This anonymous service class will do the packet upload as well as the upload
	 * progress.
	 * 
	 */
	Service<String> service = new Service<String>() {
		@Override
		protected Task<String> createTask() {
			return /**
					 * @author SaravanaKumar
					 *
					 */
			new Task<String>() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see javafx.concurrent.Task#call()
				 */
				@Override
				protected String call() {

					LOGGER.info("REGISTRATION - HANDLE_PACKET_UPLOAD_START - PACKET_UPLOAD_CONTROLLER",
							APPLICATION_NAME, APPLICATION_ID, "Handling all the packet upload activities");
					List<PacketStatusVO> packetUploadList = new ArrayList<>();
					String status = "";

					Map<String, String> tableMap = new HashMap<>();
					if (!selectedPackets.isEmpty()) {
						auditFactory.audit(AuditEvent.PACKET_UPLOAD, Components.PACKET_UPLOAD,
								SessionContext.userContext().getUserId(), RegistrationConstants.PACKET_UPLOAD_REF_ID);

						for (int i = 0; i < selectedPackets.size(); i++) {
							try {
								PacketStatusDTO dto = packetUploadService.uploadPacket(selectedPackets.get(i).getFileName());
								tableMap.put(dto.getFileName(), dto.getPacketClientStatus());
							} catch (RegBaseCheckedException e) {
								tableMap.put(selectedPackets.get(i).getFileName(), RegistrationConstants.ERROR);
							}
							this.updateProgress(i+1, selectedPackets.size());
						}
						
						if (!tableMap.isEmpty()) {
							displayStatus(populateTableData(tableMap));
						} else {
							loadInitialPage();
						}
					} else {
						loadInitialPage();
						generateAlert(RegistrationConstants.ALERT_INFORMATION,
								RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PACKET_UPLOAD_EMPTY));
					}
					selectedPackets.clear();
					return status;
				}
			};
		}
	};

	/**
	 * Export the packets and show the exported packets in the table
	 */
	public void packetExport() {
		LOGGER.info("Exporting the selected packets");

		if (!selectedPackets.isEmpty()) {
			List<PacketStatusDTO> packetsToBeExported = new ArrayList<>();
			selectedPackets.forEach(packet -> {
				if ((packet.getPacketServerStatus() == null
						|| !RegistrationConstants.SERVER_STATUS_RESEND.equalsIgnoreCase(packet.getPacketServerStatus()))
						&& !RegistrationClientStatusCode.META_INFO_SYN_SERVER.getCode()
								.equalsIgnoreCase(packet.getPacketClientStatus())) {
					PacketStatusDTO packetStatusVO = new PacketStatusDTO();
					packetStatusVO.setClientStatusComments(packet.getClientStatusComments());
					packetStatusVO.setFileName(packet.getFileName());
					packetStatusVO.setPacketClientStatus(packet.getPacketClientStatus());
					packetStatusVO.setPacketPath(packet.getPacketPath());
					packetStatusVO.setPacketServerStatus(packet.getPacketServerStatus());
					packetStatusVO.setPacketStatus(packet.getPacketStatus());
					packetStatusVO.setUploadStatus(packet.getUploadStatus());
					packetStatusVO.setSupervisorStatus(packet.getSupervisorStatus());
					packetStatusVO.setSupervisorComments(packet.getSupervisorComments());
					packetStatusVO.setName(packet.getName());
					packetStatusVO.setPhone(packet.getPhone());
					packetStatusVO.setEmail(packet.getEmail());

					try (FileInputStream fis = new FileInputStream(new File(
							packet.getPacketPath().replace(RegistrationConstants.ACKNOWLEDGEMENT_FILE_EXTENSION,
									RegistrationConstants.ZIP_FILE_EXTENSION)))) {
						byte[] byteArray = new byte[(int) fis.available()];
						fis.read(byteArray);
						packetStatusVO.setPacketHash(HMACUtils2.digestAsPlainText(byteArray));
						packetStatusVO.setPacketSize(BigInteger.valueOf(byteArray.length));

					} catch (IOException | NoSuchAlgorithmException ioException) {
						LOGGER.error("REGISTRATION_BASE_SERVICE", APPLICATION_NAME, APPLICATION_ID,
								ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
					}
					packetsToBeExported.add(packetStatusVO);
				}
			});
			List<PacketStatusDTO> exportedPackets = packetExportController.packetExport(packetsToBeExported);
			List<PacketStatusVO> packetsToBeExport = new ArrayList<>();
			exportedPackets.forEach(packet -> {
				PacketStatusVO packetStatusVO = new PacketStatusVO();
				packetStatusVO.setClientStatusComments(packet.getClientStatusComments());
				packetStatusVO.setFileName(packet.getFileName());
				packetStatusVO.setPacketClientStatus(packet.getPacketClientStatus());
				packetStatusVO.setPacketPath(packet.getPacketPath());
				packetStatusVO.setPacketServerStatus(packet.getPacketServerStatus());
				packetStatusVO.setPacketStatus(packet.getPacketStatus());
				packetStatusVO.setStatus(false);
				packetStatusVO.setUploadStatus(packet.getUploadStatus());
				packetsToBeExport.add(packetStatusVO);
			});
			Map<String, String> exportedPacketMap = new LinkedHashMap<>();
			packetsToBeExport.forEach(regPacket -> {
				exportedPacketMap.put(regPacket.getFileName(), RegistrationClientStatusCode.EXPORT.getCode());
			});
			if (!exportedPacketMap.isEmpty()) {
				displayStatus(populateTableData(exportedPacketMap));
			}
			//selectedPackets.clear();
		} else {
			loadInitialPage();
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PACKET_EXPORT_EMPTY_ERROR));
		}
	}

	/**
	 * To display the Uploaded packet details in UI
	 */
	private void displayData() {
		LOGGER.info("Displaying all the ui data");
		
		checkBoxColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
		fileNameColumn.setCellValueFactory(new PropertyValueFactory<>(RegistrationConstants.PACKET_UPLOAD_FILE));
		regDate.setCellValueFactory(new PropertyValueFactory<>(RegistrationConstants.PACKET_UPLOAD_DATE));
		slno.setCellValueFactory(new PropertyValueFactory<>(RegistrationConstants.PACKET_UPLOAD_SNO));
		clientStatus.setCellValueFactory(new PropertyValueFactory<>(RegistrationConstants.PACKET_CLIENT_STATUS));
		serverStatus.setCellValueFactory(new PropertyValueFactory<>(RegistrationConstants.PACKET_SERVER_STATUS));
		operatorId.setCellValueFactory(new PropertyValueFactory<>(RegistrationConstants.PACKET_OPERATOR_ID));

		this.list = FXCollections.observableArrayList(new Callback<PacketStatusVO, Observable[]>() {
			@Override
			public Observable[] call(PacketStatusVO param) {
				return new Observable[] { param.selectedProperty() };
			}
		});
		list.addAll(packetsToBeUploaded);
		list.addAll(uploadedPackets);
		
		checkBoxColumn
				.setCellFactory(CheckBoxTableCell.forTableColumn(new Callback<Integer, ObservableValue<Boolean>>() {
					@Override
					public ObservableValue<Boolean> call(Integer param) {
						return list.get(param).selectedProperty();
					}
				}));
		
		checkBoxColumn.setCellFactory(column -> {
	        return new CheckBoxTableCell<PacketStatusVO, Boolean>() {
	            @Override
	            public void updateItem(Boolean item, boolean empty) {
	                super.updateItem(item, empty);

	                TableRow<PacketStatusVO> currentRow = getTableRow();
	                if (currentRow.getItem() != null && !empty) {
	                    if (uploadedPackets.contains(currentRow.getItem())) {
	                        this.setDisable(true);
	                    }
	                }
	            }
	        };
	    });
		list.addListener(new ListChangeListener<PacketStatusVO>() {
			@Override
			public void onChanged(Change<? extends PacketStatusVO> displayData) {
				while (displayData.next()) {
					boolean isAdded = false;
					for (PacketStatusVO packet : selectedPackets) {
						if (packet.getFileName().equals(table.getItems().get(displayData.getFrom()).getFileName())) {
							isAdded = true;
						}
					}
					if (displayData.wasUpdated()) {
						if (!isAdded && !uploadedPackets.contains(table.getItems().get(displayData.getFrom()))) {
							selectedPackets.add(table.getItems().get(displayData.getFrom()));
						} else {
							selectedPackets.remove(table.getItems().get(displayData.getFrom()));
						}
						// saveToDevice.setDisable(!selectedPackets.isEmpty());
					}
				}
			}
		});
		// 1. Wrap the ObservableList in a FilteredList (initially display all data).
		observableList = FXCollections.observableArrayList(list);

		wrapListAndAddFiltering();
		handleCombobox();

		table.setItems(sortedList);
		table.setEditable(true);
	}

	@SuppressWarnings("unchecked")
	private void handleCombobox() {
		LOGGER.info("Adding data into comboboxes for filtering");
		
		List<String> clientStatus = ((List<PacketStatusVO>) ListUtils.union(packetsToBeUploaded, uploadedPackets)).stream()
				.filter(item-> item.getPacketClientStatus() != null && !item.getPacketClientStatus().isEmpty())
				.map(PacketStatusVO::getPacketClientStatus)
				.distinct()
				.collect(Collectors.toList());
		
		List<String> serverStatus = ((List<PacketStatusVO>) ListUtils.union(packetsToBeUploaded, uploadedPackets)).stream()
				.filter(item-> item.getPacketServerStatus() != null && !item.getPacketServerStatus().isEmpty())
				.map(PacketStatusVO::getPacketServerStatus)
				.distinct()
				.collect(Collectors.toList());
		
		clientStatusComboBox.getItems().clear();
		serverStatusComboBox.getItems().clear();
		
		clientStatusComboBox.getItems().addAll(clientStatus);
		serverStatusComboBox.getItems().addAll(serverStatus);
		
		clientStatusComboBox.setButtonCell(new ListCell<String>() {
	        @Override
	        protected void updateItem(String item, boolean empty) {
	            super.updateItem(item, empty) ;
	            if (empty || item == null) {
	                setText(ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("clientStatus"));
	            } else {
	                setText(item);
	            }
	        }
	    });
		
		serverStatusComboBox.setButtonCell(new ListCell<String>() {
	        @Override
	        protected void updateItem(String item, boolean empty) {
	            super.updateItem(item, empty) ;
	            if (empty || item == null) {
	                setText(ApplicationContext.getInstance().getApplicationLanguageLabelBundle().getString("serverStatus"));
	            } else {
	                setText(item);
	            }
	        }
	    });
		
		clearFilters.setOnAction(event -> {
			clientStatusComboBox.getSelectionModel().clearSelection();
			serverStatusComboBox.getSelectionModel().clearSelection();
		});
	}

	private void wrapListAndAddFiltering() {
		FilteredList<PacketStatusVO> filteredList = new FilteredList<>(observableList, p -> true);

		// 2. Set the filter Predicate whenever the filter changes.
		filterField.textProperty().addListener((observable, oldValue, newValue) -> {
			filterData(newValue, filteredList);
		});

		if (!filterField.getText().isEmpty()) {
			filterData(filterField.getText(), filteredList);
		}
		
		clientStatusComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			filterData(newValue, filteredList);
		});
		
		serverStatusComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			filterData(newValue, filteredList);
		});

		// 3. Wrap the FilteredList in a SortedList.
		sortedList = new SortedList<>(filteredList);

		// 4. Bind the SortedList comparator to the TableView comparator.
		sortedList.comparatorProperty().bind(table.comparatorProperty());
	}

	private void filterData(String newValue, FilteredList<PacketStatusVO> filteredList) {
		filteredList.setPredicate(reg -> {
			// If filter text is empty, display all ID's.
			if (newValue == null || newValue.isEmpty()) {
				return true;
			}

			// Compare every ID with filter text.
			String lowerCaseFilter = newValue.toLowerCase();

			if (reg.getFileName().contains(lowerCaseFilter)) {
				return true; // Filter matches packet name.
			} else if (reg.getUserId().toLowerCase().contains(lowerCaseFilter)) {
				return true; // Filter matches operator ID.
			} else if (newValue.equalsIgnoreCase(reg.getPacketClientStatus())) {
				return true; // Filter matches packet client status.
			} else if (newValue.equalsIgnoreCase(reg.getPacketServerStatus())) {
				return true; // Filter matches packet server status.
			}
			return false; // Does not match.
		});
	}

	/**
	 * To populate the data for the UI table
	 * @param packetStatus
	 * @return
	 */
	private List<PacketStatusDTO> populateTableData(Map<String, String> packetStatus) {
		LOGGER.info("Populating the table data with the Updated details");

		List<PacketStatusDTO> listUploadStatus = new ArrayList<>();
		packetStatus.forEach((id, status) -> {
			PacketStatusDTO packetUploadStatusDTO = new PacketStatusDTO();
			packetUploadStatusDTO.setFileName(id);
			packetUploadStatusDTO.setClientStatusComments(status);
			listUploadStatus.add(packetUploadStatusDTO);

		});
		return listUploadStatus;
	}

	@SuppressWarnings("unchecked")
	private void loadInitialPage() {
		LOGGER.info("Loading Packet Display Screen with all the available packets");
		
		List<PacketStatusDTO> toBeUploadedPacketStatusDTOs = packetSynchService.fetchPacketsToBeSynched();
		List<PacketStatusDTO> allApprovedPackets = packetHandlerService.getAllPackets();
		List<PacketStatusDTO> uploadedPacketStatusDTOs = ListUtils.subtract(allApprovedPackets, toBeUploadedPacketStatusDTOs);
		
		exportCSVIcon.setDisable(allApprovedPackets.isEmpty());
		filterField.setDisable(allApprovedPackets.isEmpty());
		table.setDisable(allApprovedPackets.isEmpty());
		table.getColumns().forEach(column -> column.setReorderable(false));
		saveToDevice.setVisible(!allApprovedPackets.isEmpty());
		uploadBtn.setVisible(!toBeUploadedPacketStatusDTOs.isEmpty());
		selectAllCheckBox.setSelected(false);
		clientStatusComboBox.setDisable(allApprovedPackets.isEmpty());
		serverStatusComboBox.setDisable(allApprovedPackets.isEmpty() || uploadedPacketStatusDTOs.isEmpty());
		clearFilters.setDisable(allApprovedPackets.isEmpty());
		selectedPackets.clear();
		
		int count = 1;
		packetsToBeUploaded = convertToPacketStatusVO(toBeUploadedPacketStatusDTOs, count);
		uploadedPackets = convertToPacketStatusVO(uploadedPacketStatusDTOs, packetsToBeUploaded.size() + 1);
		
		if (packetsToBeUploaded.isEmpty()) {
			selectAllCheckBox.setDisable(true);
		}
		displayData();
	}

	private List<PacketStatusVO> convertToPacketStatusVO(List<PacketStatusDTO> packetStatusDTOs, int count) {
		List<PacketStatusVO> packets = new ArrayList<>();
		for (PacketStatusDTO packet : packetStatusDTOs) {
			PacketStatusVO packetStatusVO = new PacketStatusVO();
			packetStatusVO.setClientStatusComments(packet.getClientStatusComments());
			packetStatusVO.setFileName(packet.getFileName());
			packetStatusVO.setPacketClientStatus(packet.getPacketClientStatus());
			packetStatusVO.setPacketPath(packet.getPacketPath());
			packetStatusVO.setPacketServerStatus(packet.getPacketServerStatus());
			packetStatusVO.setPacketStatus(packet.getPacketStatus());
			packetStatusVO.setStatus(false);
			packetStatusVO.setUploadStatus(packet.getUploadStatus());
			packetStatusVO.setSupervisorStatus(packet.getSupervisorStatus());
			packetStatusVO.setSupervisorComments(packet.getSupervisorComments());
			packetStatusVO.setCreatedTime(packet.getCreatedTime());
			packetStatusVO.setSlno(String.valueOf(count++));
			packetStatusVO.setName(packet.getName());
			packetStatusVO.setPhone(packet.getPhone());
			packetStatusVO.setEmail(packet.getEmail());
			packetStatusVO.setUserId(packet.getUserId());
			packets.add(packetStatusVO);
		}
		return packets;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		setImage(exportCSVIcon, RegistrationConstants.EXPORT_ICON_IMG);
		selectedPackets.clear();
		loadInitialPage();
		fileNameColumn.setResizable(false);
		checkBoxColumn.setResizable(false);
		regDate.setResizable(false);

		disableColumnsReorder(table);
		// fileColumn.setResizable(false);
		// statusColumn.setResizable(false);
	}

	@SuppressWarnings("unchecked")
	private void displayStatus(List<PacketStatusDTO> filesToDisplay) {
		Platform.runLater(() -> {

			stage = new Stage();

			stage.setTitle(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PACKET_UPLOAD_HEADER_NAME));
			stage.setWidth(500);
			stage.setHeight(500);

			TableView<PacketStatusDTO> statusTable = new TableView<>();
			statusTable.setId("resultTable");
			TableColumn<PacketStatusDTO, String> fileNameCol = new TableColumn<>(
					RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UPLOAD_COLUMN_HEADER_FILE));
			fileNameCol.setMinWidth(250);
			fileNameCol.setId("PacketID");
			fileNameCol.getStyleClass().add("tableId");
			TableColumn<PacketStatusDTO, String> statusCol = new TableColumn<>(
					RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UPLOAD_COLUMN_HEADER_STATUS));
			statusCol.setMinWidth(250);
			statusCol.setId("PacketStatus");
			
			statusCol.getStyleClass().add("tableId");
			ObservableList<PacketStatusDTO> displayList = FXCollections.observableArrayList(filesToDisplay);
			statusTable.setItems(displayList);
			fileNameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
			statusCol.setCellValueFactory(new PropertyValueFactory<>("clientStatusComments"));
			statusTable.getColumns().addAll(fileNameCol, statusCol);
			Scene scene = new Scene(new StackPane(statusTable), 800, 800);
			scene.getStylesheets().add(ClassLoader.getSystemClassLoader()
					.getResource(getCssName()).toExternalForm());
			stage.initStyle(StageStyle.UTILITY);
			stage.initModality(Modality.WINDOW_MODAL);
			stage.initOwner(fXComponents.getStage());
			stage.setResizable(false);
			stage.setScene(scene);
			stage.setTitle(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.ALERT_NOTE_LABEL));
			stage.show();
			selectAllCheckBox.setSelected(false);
			stage.setOnCloseRequest((e) -> {
				// saveToDevice.setDisable(false);
				loadInitialPage();
			});
		});

	}

	public void exportData() {
		LOGGER.info("Exporting the packet upload status details");
		String str = filterField.getText();
		Stage stage = new Stage();
		DirectoryChooser destinationSelector = new DirectoryChooser();
		destinationSelector.setTitle(RegistrationConstants.FILE_EXPLORER_NAME);
		Path currentRelativePath = Paths.get("");
		File defaultDirectory = new File(currentRelativePath.toAbsolutePath().toString());
		destinationSelector.setInitialDirectory(defaultDirectory);
		File destinationPath = destinationSelector.showDialog(stage);

		if (destinationPath != null) {

			filterField.clear();
			String fileData = table.getItems().stream()
					.map(packetVo -> packetVo.getSlno().trim().concat(RegistrationConstants.COMMA).concat("'")
							.concat(packetVo.getFileName()).concat("'").concat(RegistrationConstants.COMMA).concat("'")
							.concat(packetVo.getCreatedTime()).concat("'"))
					.collect(Collectors.joining(RegistrationConstants.NEW_LINE));
			String headers = RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.EOD_SLNO_LABEL).concat(RegistrationConstants.COMMA)
					.concat(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PACKETUPLOAD_PACKETID_LABEL)).concat(RegistrationConstants.COMMA)
					.concat(RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.EOD_REGISTRATIONDATE_LABEL)).concat(RegistrationConstants.COMMA)
					.concat(RegistrationConstants.NEW_LINE);
			fileData = headers + fileData;
			filterField.setText(str);
			try (Writer writer = new BufferedWriter(new FileWriter(destinationPath + "/"
					+ RegistrationConstants.UPLOAD_FILE_NAME.concat(RegistrationConstants.UNDER_SCORE)
							.concat(getcurrentTimeStamp()).concat(RegistrationConstants.EXPORT_FILE_TYPE)))) {
				writer.write(fileData);

				generateAlert(RegistrationConstants.ALERT_INFORMATION,
						RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.EOD_DETAILS_EXPORT_SUCCESS));

			} catch (IOException ioException) {
				LOGGER.error(LOG_PACKET_UPLOAD, APPLICATION_NAME, APPLICATION_ID,
						ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));

				generateAlert(RegistrationConstants.ALERT_INFORMATION, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PACKET_STATUS_EXPORT));

			}
		}
		LOGGER.info(LOG_PACKET_UPLOAD, APPLICATION_NAME, APPLICATION_ID,
				"Exporting Packet Upload status details has been ended");
	}

	public void selectAllCheckBox(ActionEvent e) {
		// saveToDevice.setDisable(((CheckBox) e.getSource()).isSelected());
		list.forEach(item -> {
			if (!uploadedPackets.contains(item)) {
				item.setStatus(((CheckBox) e.getSource()).isSelected());
			}
		});
	}

	/**
	 * This method gets the current timestamp in yyyymmddhhmmss format.
	 * 
	 * @return current timestamp in fourteen digits
	 */
	private String getcurrentTimeStamp() {
		DateTimeFormatter format = DateTimeFormatter.ofPattern(RegistrationConstants.EOD_PROCESS_DATE_FORMAT_FOR_FILE);
		return LocalDateTime.now().format(format);
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	/**
	 * Go to home ack template.
	 */
	public void goToHomePacketUpload() {
		try {
			BaseController.load(getClass().getResource(RegistrationConstants.HOME_PAGE));
			if (!(boolean) SessionContext.map().get(RegistrationConstants.ONBOARD_USER)) {
				clearOnboardData();
				clearRegistrationData();
			} else {
				SessionContext.map().put(RegistrationConstants.ISPAGE_NAVIGATION_ALERT_REQ,
						RegistrationConstants.ENABLE);
			}
		} catch (IOException ioException) {
			LOGGER.error("REGISTRATION - UI - ACK_RECEIPT_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					ioException.getMessage() + ExceptionUtils.getStackTrace(ioException));
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_HOME_PAGE));
		} catch (RuntimeException runtimException) {
			LOGGER.error("REGISTRATION - UI - ACK_RECEIPT_CONTROLLER", APPLICATION_NAME, APPLICATION_ID,
					runtimException.getMessage() + ExceptionUtils.getStackTrace(runtimException));
			generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_HOME_PAGE));
		}

	}

}