package com.example.mydraw;

import android.graphics.Bitmap;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Drawing {
    private int itemId;
    private String itemName;
    private Bitmap itemImage;
    private long createdAt;

    public int getId() { return itemId; }
    public void setId(int id) { this.itemId = id; }
    
    public String getName() { return itemName; }
    public void setName(String name) { this.itemName = name; }
    
    public Bitmap getBitmap() { return itemImage; }
    public void setBitmap(Bitmap bitmap) { this.itemImage = bitmap; }
    
    public long getTimestamp() { return createdAt; }
    public void setTimestamp(long timestamp) { this.createdAt = timestamp; }
    
    public String getFormattedDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return formatter.format(new Date(createdAt));
    }
}
