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
    private static final String DATABASE_NAME = "DrawingsDB";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_DRAWINGS = "drawings";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_IMAGE = "image";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_DRAWINGS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_IMAGE + " BLOB,"
                + COLUMN_TIMESTAMP + " INTEGER"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DRAWINGS);
        onCreate(db);
    }

    public long saveDrawing(String name, Bitmap bitmap) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_IMAGE, bitmapToByteArray(bitmap));
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        long id = db.insert(TABLE_DRAWINGS, null, values);
        db.close();
        return id;
    }

    public List<Drawing> getAllDrawings() {
        List<Drawing> drawings = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_DRAWINGS + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Drawing drawing = new Drawing();
                drawing.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                drawing.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                drawing.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));
                drawing.setBitmap(byteArrayToBitmap(imageBytes));
                drawings.add(drawing);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return drawings;
    }

    public Drawing getDrawing(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DRAWINGS, null, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        Drawing drawing = null;
        if (cursor != null && cursor.moveToFirst()) {
            drawing = new Drawing();
            drawing.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            drawing.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            drawing.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
            byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_IMAGE));
            drawing.setBitmap(byteArrayToBitmap(imageBytes));
            cursor.close();
        }
        db.close();
        return drawing;
    }

    public void deleteDrawing(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DRAWINGS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private Bitmap byteArrayToBitmap(byte[] bytes) {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}