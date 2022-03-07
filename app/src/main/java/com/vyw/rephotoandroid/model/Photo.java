package com.vyw.rephotoandroid.model;

public class Photo {
    private int id;
    private String captured_at;
    private int id_file;
    private int id_place;
    private int id_user;
    private User user;

    public Photo(int id,
                 String captured_at,
                 int id_file,
                 int id_place,
                 int id_user,
                 User user) {
        this.id = id;
        this.captured_at = captured_at;
        this.id_file = id_file;
        this.id_place = id_place;
        this.id_user = id_user;
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCaptured_at() {
        return captured_at;
    }

    public void setCaptured_at(String captured_at) {
        this.captured_at = captured_at;
    }

    public int getId_file() {
        return id_file;
    }

    public void setId_file(int id_file) {
        this.id_file = id_file;
    }

    public int getId_place() {
        return id_place;
    }

    public void setId_place(int id_place) {
        this.id_place = id_place;
    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
