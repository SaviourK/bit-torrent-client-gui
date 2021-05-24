package com.kanok.btlibrarygui.controller;

import com.kanok.btlibrarygui.ConfirmBox;
import com.kanok.btlibrarygui.model.TorrentRow;
import com.kanok.btlibrarygui.service.GuiService;
import com.turn.ttorrent.client.Client;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class GuiController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(GuiController.class);

    @Autowired
    private GuiService guiService;

    public BorderPane pane;
    @FXML
    private Label dirLbl;
    @FXML
    private Label torrentDirLbl;
    @FXML
    private Label trackerUrlLbl;
    @FXML
    private Label trackerAnnounceUriLbl;

    public TableView<TorrentRow> table;
    public TableColumn<TorrentRow, String> uuidColumn;
    public TableColumn<TorrentRow, String> nameColumn;
    public TableColumn<TorrentRow, String> downloadedColumn;
    public static ObservableList<TorrentRow> torrents;

    private List<Client> clients;

    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addTableContent();
        guiService.initObserverDir(dirLbl);
        guiService.refreshDirLbl(dirLbl);
        guiService.refreshTorrentDir(torrentDirLbl);
        guiService.refreshTrackerUrlLbl(trackerUrlLbl);
        guiService.refreshTrackerAnnounceUriLbl(trackerAnnounceUriLbl);
        clients = new ArrayList<>();
    }

    private void addTableContent() {
        uuidColumn.setMinWidth(40);
        uuidColumn.setCellValueFactory(new PropertyValueFactory<>("uuid"));

        nameColumn.setMinWidth(350);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        downloadedColumn.setMinWidth(40);
        downloadedColumn.setCellValueFactory(new PropertyValueFactory<>("downloaded"));

        torrents = getTorrents();
        table.setItems(torrents);

        ContextMenu cm = new ContextMenu();
        MenuItem delete = new MenuItem("Delete");

        delete.setOnAction(e -> {
            TorrentRow item = table.getSelectionModel().getSelectedItem();
            cm.show(table, 2, 2);
            logger.info(item.toString());
        });
        cm.getItems().add(delete);

        table.setContextMenu(cm);
    }

    public ObservableList<TorrentRow> getTorrents() {
        ObservableList<TorrentRow> torrentRows = FXCollections.observableArrayList();
        torrentRows.add(new TorrentRow("AA55", "Acidobacteria", "20%"));
        torrentRows.add(new TorrentRow("AA56", "Dictyoglomi", "50%"));
        torrentRows.add(new TorrentRow("AA58", "Fibrobacteres", "0%"));
        return torrentRows;
    }

    private void closeProgram() {
        boolean answer = ConfirmBox.display("Close", "Sure you want to exit?");
        if (answer) {
            //window.close();
            logger.info("File saves");
        }
    }

    public void handleAddTorrentBtn() {
        guiService.handleAddTorrentBtn(pane, torrents);
    }

    public void handleSetTrackerBtn() {
        guiService.handleSetTrackerBtn();

    }

    public void handleSetObservedDirBtn() throws IOException {
        guiService.handSetObservedDirBtn(pane, dirLbl);
    }

    public void handleDownloadByUUIDBtn() {
        guiService.handleDownloadByUUIDBtn();
    }
}
