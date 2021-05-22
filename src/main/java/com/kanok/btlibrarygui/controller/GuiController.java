package com.kanok.btlibrarygui.controller;

import com.kanok.btlibrarygui.ConfirmBox;
import com.kanok.btlibrarygui.model.Torrent;
import com.kanok.btlibrarygui.model.TorrentRow;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
public class GuiController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(GuiController.class);

    private String trackerUrl = "http://localhost:8080";

    private String trackerAnnounceUri = "http://0.0.0.0:6969/announce";

    //TODO delete because it must be valit folder
    private File observedDir = null; //= new File("C:/Torrent/Files");

    public BorderPane pane;

    public Stage window;

    public Button addTorrentBtn;


    public TableView<TorrentRow> table;

    public TableColumn<TorrentRow, String> nameColumn;
    public TableColumn<TorrentRow, Double> downloadedColumn;
    public TableColumn<TorrentRow, Integer> quantityColumn;

    private FileChooser fc;
    private DirectoryChooser dc;

    @FXML
    private Label dirLbl;
    @FXML
    private Label trackerUrlLbl;
    @FXML
    private Label trackerAnnounceUriLbl;

    private ObservableList<TorrentRow> torrents;

    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //TODO load all torrents and start download if is downloading
        logger.info("loading user data...");
        addTableContent();
        //window = (Stage) pane.getScene().getWindow();
        initFileChooser();
        initDirectoryChooser();
        refreshDirLbl();
        refreshTrackerUrlLbl();
        refreshTrackerAnnounceUriLbl();
    }

    private void refreshDirLbl() {
        String observedDirPathString = observedDir != null ? observedDir.getPath() : "";
        dirLbl.setText("Observed directory: " + observedDirPathString);
    }

    private void refreshTrackerUrlLbl() {
        trackerUrlLbl.setText("Tracker: " + trackerUrl);
    }

    private void refreshTrackerAnnounceUriLbl() {
        trackerAnnounceUriLbl.setText("Tracker announce URI: " + trackerAnnounceUri);
    }

    private void initFileChooser() {
        fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Torrent files", "*.torrent"));

    }

    private void initDirectoryChooser() {
        dc = new DirectoryChooser();
        dc.setTitle("Select directory to observe");
    }


    public void setWindow(Stage window) {
        this.window = window;
    }


    private void createNewTorrentAction() {
        //AlertBox.display("Add torrent", "Add new torrent");
        boolean answer = ConfirmBox.display("None", "Are you sure");
        logger.info("Are you sure: {}", answer);

    }

    private void addTableContent() {

        nameColumn.setMinWidth(200);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        downloadedColumn.setMinWidth(200);
        downloadedColumn.setCellValueFactory(new PropertyValueFactory<>("downloaded"));

        quantityColumn.setMinWidth(200);
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        torrents = getTorrents();
        table.setItems(torrents);

        ContextMenu cm = new ContextMenu();
        MenuItem delete = new MenuItem("Delete");

        delete.setOnAction(e -> {
            logger.info("ab");
            TorrentRow item = table.getSelectionModel().getSelectedItem();
            cm.show(table, 2, 2);
            logger.info(item.toString());

        });
        cm.getItems().add(delete);

        //table.getColumns().addAll(downloadedColumn);
        table.setContextMenu(cm);


    }


    public ObservableList<TorrentRow> getTorrents() {
        ObservableList<TorrentRow> torrentRows = FXCollections.observableArrayList();
        torrentRows.add(new TorrentRow("Windows", 20, 1));
        torrentRows.add(new TorrentRow("ZOO Tycoon", 50, 1));
        torrentRows.add(new TorrentRow("GTA 5", 0, 1));
        return torrentRows;
    }

    private void closeProgram() {
        boolean answer = ConfirmBox.display("Close", "Sure you want to exit?");
        if (answer) {
            //window.close();
            logger.info("File saves");
        }
    }

    public void handleAddTorrentBtn(ActionEvent actionEvent) {
        File selectedFile = fc.showOpenDialog(pane.getScene().getWindow());
        if (selectedFile != null) {
            /*file = fc.getSelectedFile();
            options.setMetaInfoFile(file);
            logger.info("Opening: " + file.getName() + ".");
            selectedFileLbl.setVisible(true);
            selectedFileLbl.setText("Selected file: " + file.getName());
            startDownload(options);*/
            torrents.add(new TorrentRow(selectedFile.getName(), 0, 0));
            logger.warn("Add torrent btn clicked");
        } else {
            logger.warn("Open command cancelled by user.");
        }
    }

    public void handleSetTrackerBtn(ActionEvent actionEvent) {
        // create a text input dialog
        TextInputDialog td = new TextInputDialog(trackerUrl);
        // setHeaderText
        td.setHeaderText("Set tracker URL");
        Optional<String> result = td.showAndWait();
        if (result.isPresent()) {
            String newTrackerUrl = td.getEditor().getText();
            WebClient client = WebClient.create(newTrackerUrl);
            Mono<String> resource = client
                    .get()
                    .uri("/get-announce-uri")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class);
            String announceUri = resource.block();
            if (announceUri != null) {
                trackerUrl = newTrackerUrl;
                trackerAnnounceUri = announceUri;
                logger.info("Tracker URL set: {}", trackerUrl);
                logger.info("Tracker announce URI set: {}", trackerAnnounceUri);
            } else {
                logger.error("Cannot contact tracker for announce URI. Requested tracker URL: {}", newTrackerUrl);
                logger.info("Set tracker URL was canceled. Current tracker URL: {}", trackerUrl);
            }
        } else {
            logger.info("Set tracker URL was canceled. Current tracker URL: {}", trackerUrl);
        }
    }

    public void handleSetObservedDirBtn(ActionEvent actionEvent) throws IOException {
        File selectedFile = dc.showDialog(pane.getScene().getWindow());
        if (selectedFile != null) {
            Path path = Paths.get(selectedFile.toURI());
            WatchService watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);

            Timeline watchDirTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(5), (actionEvent1 -> {
                        WatchKey key;

                       if ((key = watchService.poll()) != null) {
                            for (WatchEvent<?> event : key.pollEvents()) {
                                logger.info("Event kind: {}. File affected: {}.", event.kind(), event.context());
                                Path dir = (Path)key.watchable();
                                File file = new File(dir.toUri().toString() + event.context().toString());
                                logger.info(file.getPath());
                            }
                            key.reset();
                        }
                    }))
            );

            watchDirTimeline.setCycleCount(Timeline.INDEFINITE);
            watchDirTimeline.play();
            observedDir = selectedFile;
            logger.info("Observed directory set: {}", observedDir.getAbsolutePath());
            refreshDirLbl();
        } else {
            String observedDirPathString = observedDir != null ? observedDir.getPath() : "";
            logger.info("Set folder command cancelled. Current folder: {}", observedDirPathString);
        }
    }

    public void handleDownloadByUUIDBtn(ActionEvent actionEvent) {
        // create a text input dialog
        TextInputDialog td = new TextInputDialog();
        // setHeaderText
        td.setHeaderText("Set UUID");
        Optional<String> result = td.showAndWait();
        if (result.isPresent()) {
            String uuid = td.getEditor().getText();
            logger.info("Searching for torrent with UUID: {}", uuid);
            WebClient client = WebClient.create(trackerUrl);
            Mono<Torrent> resource = client
                    .get()
                    .uri("/torrents/{uuid}", uuid)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Torrent.class);
            try {
                Torrent torrent = resource.block();
                if (torrent != null) {
                    logger.info("Torrent name: {}, torrent UUID: {}", torrent.getName(), torrent.getUuid());
                    refreshTrackerUrlLbl();
                    refreshTrackerAnnounceUriLbl();
                } else {
                    logger.info("Torrent with UUID {} not exists.", uuid);
                }
            } catch (WebClientRequestException e) {
                logger.error("Cannot contact server: {}", e.getUri(), e);
            }
        } else {
            logger.info("Download by UUID was canceled");
        }
    }
}
