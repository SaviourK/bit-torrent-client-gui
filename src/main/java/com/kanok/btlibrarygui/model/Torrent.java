package com.kanok.btlibrarygui.model;

public class Torrent {

    private String uuid;

    private String name;

    public Torrent() {
    }

    public Torrent(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
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
}
