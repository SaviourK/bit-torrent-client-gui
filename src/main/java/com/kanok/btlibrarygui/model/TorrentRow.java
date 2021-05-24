package com.kanok.btlibrarygui.model;

public class TorrentRow {

    private String uuid;

    private String name;

    private String downloaded;

    public TorrentRow() {
        this.uuid = "";
        this.name = "";
        this.downloaded = "0%";
    }

    public TorrentRow(String uuid, String name, String downloaded) {
        this.uuid = uuid;
        this.name = name;
        this.downloaded = downloaded;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(String downloaded) {
        this.downloaded = downloaded;
    }
}
