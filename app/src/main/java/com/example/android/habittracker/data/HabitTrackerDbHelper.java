package com.example.android.habittracker.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.example.android.habittracker.data.HabitTrackerContract.HabitEntry;


public class HabitTrackerDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "habittracker.db";
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + HabitEntry.TABLE_NAME + " ("
            + HabitEntry.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + HabitEntry.COLUMN_JOGGING_TIME + " INTEGER, "
            + HabitEntry.COLUMN_SWIMMING_TIME + " INTEGER, "
            + HabitEntry.COLUMN_GRANMA_TIME + " INTEGER, "
            + HabitEntry.COLUMN_FRENCH_TIME + " INTEGER"
            + ");";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS "
            + HabitEntry.TABLE_NAME + ";";

    public HabitTrackerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, // Use default cursor
                DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
