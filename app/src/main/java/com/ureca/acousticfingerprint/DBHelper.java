package com.ureca.acousticfingerprint;

/**
 * Created by Andrew on 1/21/17.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "MyDBName.db";
    private static final String FINGERPRINTS_TABLE_NAME = "fingerprints";
    public static final String FINGERPRINTS_COLUMN_ANCHOR_FREQUENCY = "anchor_frequency";
    public static final String FINGERPRINTS_COLUMN_POINT_FREQUENCY = "point_frequency";
    public static final String FINGERPRINTS_DELTA = "delta";
    public static final String FINGERPRINTS_ABSOLUTE_TIME = "absolute_time";
    public static final String FINGERPRINTS_SONG_ID = "song_id";
    private HashMap hp;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table if not exists fingerprints " +
                        "(anchor_frequency smallint, point_frequency smallint, delta smallint, absolute_time smallint, song_id int)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS fingerprints");
        onCreate(db);
    }

    public void refreshDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS fingerprints");
        onCreate(db);
    }

    public boolean insertFingerprint(short anchorFrequency, short pointFrequency, byte delta, short absoluteTime, int songID) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("anchor_frequency", anchorFrequency);
        contentValues.put("point_frequency", pointFrequency);
        contentValues.put("delta", delta);
        contentValues.put("absolute_time", absoluteTime);
        contentValues.put("song_id", songID);
        db.insert("fingerprints", null, contentValues);
        return true;
    }

    public Cursor getData(short anchorFrequency, short pointFrequency, byte delta) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select absolute_time, song_id from fingerprints " +
                "where anchor_frequency=" + anchorFrequency + " and point_frequency=" + pointFrequency + " and delta=" + delta, null);
        return res;
    }

    public long numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        long numRows = DatabaseUtils.queryNumEntries(db, FINGERPRINTS_TABLE_NAME);
        return numRows;
    }

    public Integer deleteFingerprint(short anchorFrequency, short pointFrequency, byte delta) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("fingerprints",
                "anchor_frequency = ? and point_frequency = ? and delta = ? ",
                new String[]{Short.toString(anchorFrequency), Short.toString(pointFrequency), Byte.toString(delta)});
    }

    /*
    public ArrayList<String> getAllCotacts() {
        ArrayList<String> array_list = new ArrayList<String>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from contacts", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){
            array_list.add(res.getString(res.getColumnIndex(CONTACTS_COLUMN_NAME)));
            res.moveToNext();
        }
        return array_list;
    }
    */
}
