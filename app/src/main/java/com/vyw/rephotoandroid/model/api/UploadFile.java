package com.vyw.rephotoandroid.model.api;

import java.io.File;

public class UploadFile {
    private File image;

    public UploadFile(File image) {
        this.image = image;
    }

    public File getImage() {
        return image;
    }

    public void setImage(File image) {
        this.image = image;
    }
}
