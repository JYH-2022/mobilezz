package com.example.mydraw;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ArtworksDB";
    private static final String TABLE_NAME = "artworks";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   "title TEXT, imageData BLOB, createdTime INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long saveDrawing(String title, Bitmap image) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues data = new ContentValues();
        data.put("title", title);
        data.put("imageData", convertToBytes(image));
        data.put("createdTime", System.currentTimeMillis());
        long rowId = db.insert(TABLE_NAME, null, data);
        db.close();
        return rowId;
    }

    public List<Drawing> getAllDrawings() {
        List<Drawing> results = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY createdTime DESC", null);
        
        while (cursor.moveToNext()) {
            Drawing item = new Drawing();
            item.setId(cursor.getInt(0));
            item.setName(cursor.getString(1));
            item.setBitmap(convertToBitmap(cursor.getBlob(2)));
            item.setTimestamp(cursor.getLong(3));
            results.add(item);
        }
        cursor.close();
        db.close();
        return results;
    }

    public void deleteDrawing(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    private byte[] convertToBytes(Bitmap image) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, output);
        return output.toByteArray();
    }

    private Bitmap convertToBitmap(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }
}
