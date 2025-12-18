package com.example.mydraw;

import android.graphics.Bitmap;

public class Drawing {
    private int id;
    private String name;
    private Bitmap bitmap;
    private long timestamp;

    public Drawing() {
    }

    public Drawing(int id, String name, Bitmap bitmap, long timestamp) {
        this.id = id;
        this.name = name;
        this.bitmap = bitmap;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }
}