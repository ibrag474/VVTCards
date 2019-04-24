package com.bjobs.vvtcards;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    private static String TAG = "DBHelper";

    public DBHelper(Context context) {
        // конструктор суперкласса
        super(context, "settings", null, 2);
    }

    private static final String D2 = "ALTER TABLE cards"
            + " ADD COLUMN cardData" + " varchar;";

    @Override
    public void onCreate(SQLiteDatabase mainDatabase) {
        Log.d("DataBase", "DataBase has been created");
        mainDatabase.execSQL("create table cards ("
                + "id integer primary key autoincrement,"
                + "name varchar,"
                + "number varchar"
                + "cardData varchar" + ");");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(D2);
        }
    }

    public void manageDataStorage(int wtd, String cardName, String cardId, String delName) {
        SQLiteDatabase mainDatabase = getWritableDatabase();

        if (wtd == 0) {
            ContentValues cv = new ContentValues();
            cv.put("name", cardName);
            cv.put("number", cardId);

            long rowID = mainDatabase.insert("cards", null, cv);
            Log.d(TAG, "row inserted, ID = " + rowID + " " + cardName + " " + cardId);
        } else if (wtd == 1) {
            mainDatabase.execSQL("DELETE FROM cards WHERE name='" + delName + "'");
            Log.d(TAG, "deleted row" + delName);
        } else if (wtd == 2) {
            ContentValues cv = new ContentValues();
            cv.put("name", cardName);
            cv.put("number", cardId);
            cv.put("cardData", delName);

            long rowID = mainDatabase.insert("cards", null, cv);
            Log.d(TAG, "row inserted, ID = " + rowID + " NAME =  " + cardName + " CID = " + cardId + " CDAT = " + delName);
        }
        mainDatabase.close();
    }
}

