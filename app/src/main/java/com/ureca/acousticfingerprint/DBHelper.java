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

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "FingerprintDatabase.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table if not exists ads " +
                        "(name varchar(50), details varchar(65535), link varchar(100), image_id int, ad_id int, primary key (ad_id))"
        );
        db.execSQL(
                "create table if not exists fingerprints " +
                        "(anchor_frequency smallint, point_frequency smallint, delta smallint, absolute_time smallint, ad_id int, foreign key (ad_id) references ads(ad_id))"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS fingerprints");
        db.execSQL("DROP TABLE IF EXISTS ads");
        onCreate(db);
    }

    public void refreshDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS fingerprints");
        db.execSQL("DROP TABLE IF EXISTS ads");
        onCreate(db);
    }

    public boolean insertFingerprint(short anchorFrequency, short pointFrequency, byte delta, short absoluteTime, int adID) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("anchor_frequency", anchorFrequency);
        contentValues.put("point_frequency", pointFrequency);
        contentValues.put("delta", delta);
        contentValues.put("absolute_time", absoluteTime);
        contentValues.put("ad_id", adID);
        db.insert("fingerprints", null, contentValues);
        return true;
    }

    public boolean insertAd(String name, String details, String link, int imageID, int adID) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("details", details);
        contentValues.put("link", link);
        contentValues.put("image_id", imageID);
        contentValues.put("ad_id", adID);
        db.insert("ads", null, contentValues);
        return true;
    }

    public Cursor getFingerprintCouples(short anchorFrequency, short pointFrequency, byte delta) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select absolute_time, ad_id from fingerprints " +
                "where anchor_frequency=" + anchorFrequency + " and point_frequency=" + pointFrequency + " and delta=" + delta, null);
        return res;
    }

    public Cursor getAdDetails(int adID) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select name, details, link, image_id from ads " +
                "where ad_id=" + adID, null);
        return res;
    }

    public long getNumOfAds() {
        SQLiteDatabase db = this.getReadableDatabase();
        long numRows = DatabaseUtils.queryNumEntries(db, "ads");
        return numRows;
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
