package com.vyw.rephotoandroid.model;

public class File {
    private int id;
    private String extension;

    public File(int id, String extension) {
        this.id = id;
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
