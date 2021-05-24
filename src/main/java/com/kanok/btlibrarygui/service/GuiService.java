package com.kanok.btlibrarygui.service;

import com.kanok.btlibrarygui.controller.GuiController;
import com.kanok.btlibrarygui.model.TorrentRow;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.common.Torrent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
public class GuiService {

    private static final Logger logger = LoggerFactory.getLogger(GuiService.class);

    private static final String CREATED_BY = "createdByVitezslavKanok";
    private static final double DEFAULT_SPEED_RATE = 1000;
    private static final String TORRENT_EXTENSION = ".torrent";

    private static String TRACKER_URL = "http://localhost:8080";
    private static String TRACKER_ANNOUNCE_URI = "http://0.0.0.0:6969/announce";
    //TODO delete file path because it must be valit folder
    private static File TORRENT_DIR = new File("C:/Torrent");
    private static File OBSERVER_DIR = new File("C:/Torrent/Files");

    private FileChooser fc;
    private DirectoryChooser dc;

    public GuiService() {
        initFileChooser();
        initDirectoryChooser();
    }

    private void initFileChooser() {
        fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Torrent files", "*.torrent"));
    }

    private void initDirectoryChooser() {
        dc = new DirectoryChooser();
        dc.setTitle("Select directory to observe");
    }

    public void initObserverDir(Label dirLbl) throws IOException {
        if (OBSERVER_DIR != null) {
            createDirObservation(OBSERVER_DIR, dirLbl);
        }
    }

    public void createDirObservation(File selectedFile, Label dirLbl) throws IOException {
        Path path = Paths.get(selectedFile.toURI());
        WatchService watchService = FileSystems.getDefault().newWatchService();
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        Timeline watchDirTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), (actionEvent1 -> {
                    WatchKey key;
                    if ((key = watchService.poll()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            logger.info("Event kind: {}. File affected: {}.", event.kind(), event.context());
                            Path dir = (Path) key.watchable();
                            String fileName = dir.resolve((Path) event.context()).toString();
                            logger.info(event.context() + "");
                            File file = new File(fileName);
                            logger.info(file.getPath());
                            File torrentFile = createTorrentFile(file);
                            if (torrentFile != null) {
                                try {
                                    startClient(torrentFile);
                                    GuiController.torrents.add(new TorrentRow("AABBX", torrentFile.getName(), "100%"));
                                } catch (IOException | NoSuchAlgorithmException e) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }
                        key.reset();
                    }
                }))
        );
        watchDirTimeline.setCycleCount(Timeline.INDEFINITE);
        watchDirTimeline.play();
        OBSERVER_DIR = selectedFile;
        logger.info("Observed directory set: {}", OBSERVER_DIR.getAbsolutePath());
        refreshDirLbl(dirLbl);
    }

    private File createTorrentFile(File file) {
        try {
            logger.info("Creating new .torrent metainfo file: {}", file.getAbsolutePath());
            Torrent torrent = Torrent.create(file, new URI(TRACKER_ANNOUNCE_URI), CREATED_BY);
            announceTorrentToTracker(file);
            logger.info("Seed torrent: {}", torrent.isSeeder());
            String pathToTorrentFile = saveTorrentFile(torrent);
            return new File(pathToTorrentFile);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    private String saveTorrentFile(Torrent torrent) throws IOException {
        String torrentFileName = FilenameUtils.removeExtension(torrent.getName()) + TORRENT_EXTENSION;
        String pathToTorrentFile = TORRENT_DIR.getPath() + File.separator + torrentFileName;
        logger.info("Save .torrent to file to: {}", pathToTorrentFile);
        FileOutputStream fos = new FileOutputStream(pathToTorrentFile);
        torrent.save(fos);
        fos.close();
        return pathToTorrentFile;
    }

    private void announceTorrentToTracker(File torrent) {
        try {
            WebClient client = WebClient.create(TRACKER_URL);
            Mono<String> resource = client
                    .post()
                    .uri("/torrents")
                    .body(Mono.just(torrent), File.class)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(msg -> {
                        logger.info("Success message: {}", msg);
                    })
                    .doOnError(e -> {
                        logger.error("Error when announcing torrent to tracker. Error message: {}", e.getMessage());
                    });
            resource.block();
        } catch (WebClientRequestException e) {
            logger.error("Cannot contact server: {}", e.getUri(), e);
        }
    }

    private void startClient(File file) throws IOException, NoSuchAlgorithmException {
        Client seeder = new Client(
                // This is the interface the client will listen on (you might need something
                // else than localhost here).
                InetAddress.getLocalHost(),

                // Load the torrent from the torrent file and use the given
                // output directory. Partials downloads are automatically recovered.
                SharedTorrent.fromFile(file, OBSERVER_DIR));

        // You can optionally set download/upload rate limits
        // in kB/second. Setting a limit to 0.0 disables rate
        // limits.
        seeder.setMaxDownloadRate(DEFAULT_SPEED_RATE);
        seeder.setMaxUploadRate(DEFAULT_SPEED_RATE);

        // At this point, can you either call download() to download the torrent and
        // stop immediately after...
        //seeder.download();

        // Or call client.share(...) with a seed time in seconds:
        seeder.share(3600);
        // Which would seed the torrent for an hour after the download is complete.

        // Downloading and seeding is done in background threads.
        // To wait for this process to finish, call:
        //seeder.waitForCompletion();
    }

    public void handleDownloadByUUIDBtn() {
        TextInputDialog td = new TextInputDialog();
        td.setHeaderText("Set UUID");
        Optional<String> result = td.showAndWait();
        if (result.isPresent()) {
            try {
                String uuid = td.getEditor().getText();
                logger.info("Searching for torrent with UUID: {}", uuid);
                WebClient client = WebClient.create(TRACKER_URL);
                Mono<Torrent> resource = client
                        .get()
                        .uri("/torrents/{uuid}", uuid)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(Torrent.class);
                Torrent torrent = resource.block();
                if (torrent != null) {
                    logger.info("Torrent name: {}, torrent UUID: {}", torrent.getName(), torrent.getSize());

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

    public void handleAddTorrentBtn(BorderPane pane, ObservableList<TorrentRow> torrents) {
        File selectedFile = fc.showOpenDialog(pane.getScene().getWindow());
        if (selectedFile != null) {
            /*file = fc.getSelectedFile();
            options.setMetaInfoFile(file);
            logger.info("Opening: " + file.getName() + ".");
            selectedFileLbl.setVisible(true);
            selectedFileLbl.setText("Selected file: " + file.getName());
            startDownload(options);*/
            torrents.add(new TorrentRow("AAA", selectedFile.getName(), "0%"));
            logger.warn("Add torrent btn clicked");
        } else {
            logger.warn("Open command cancelled by user.");
        }
    }

    public void handleSetTrackerBtn() {
        TextInputDialog td = new TextInputDialog(TRACKER_URL);
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
                TRACKER_URL = newTrackerUrl;
                TRACKER_ANNOUNCE_URI = announceUri;
                logger.info("Tracker URL set: {}", TRACKER_URL);
                logger.info("Tracker announce URI set: {}", TRACKER_ANNOUNCE_URI);
            } else {
                logger.error("Cannot contact tracker for announce URI. Requested tracker URL: {}", newTrackerUrl);
                logger.info("Set tracker URL was canceled. Current tracker URL: {}", TRACKER_URL);
            }
        } else {
            logger.info("Set tracker URL was canceled. Current tracker URL: {}", TRACKER_URL);
        }
    }

    public void handSetObservedDirBtn(BorderPane pane, Label dirLbl) throws IOException {
        File selectedFile = dc.showDialog(pane.getScene().getWindow());
        if (selectedFile != null) {
            createDirObservation(selectedFile, dirLbl);
        } else {
            String observedDirPathString = OBSERVER_DIR != null ? OBSERVER_DIR.getPath() : "";
            logger.info("Set folder command cancelled. Current folder: {}", observedDirPathString);
        }
    }

    public void refreshDirLbl(Label dirLbl) {
        String observedDirPathString = OBSERVER_DIR != null ? OBSERVER_DIR.getPath() : "";
        dirLbl.setText("Observed directory: " + observedDirPathString);
    }

    public void refreshTorrentDir(Label torrentDirLbl) {
        String torrentDirPathString = TORRENT_DIR != null ? TORRENT_DIR.getPath() : "";
        torrentDirLbl.setText("Torrent directory: " + torrentDirPathString);
    }

    public void refreshTrackerUrlLbl(Label trackerUrlLbl) {
        trackerUrlLbl.setText("Tracker: " + TRACKER_URL);
    }

    public void refreshTrackerAnnounceUriLbl(Label trackerAnnounceUriLbl) {
        trackerAnnounceUriLbl.setText("Tracker announce URI: " + TRACKER_ANNOUNCE_URI);
    }
}
