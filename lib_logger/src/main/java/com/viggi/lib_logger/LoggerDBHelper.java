package com.viggi.lib_logger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Dave on 3/24/2015.
 */
public class LoggerDBHelper extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "logger.db";
    private static final int DATABASE_VERSION = 1;
    public static final String COLUMN_ID = "_id";

    public static final String TABLE_LOG = "log_entry";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_CONFIG_ID = "config_id";
    public static final String COLUMN_STATE = "state";
    public static final String COLUMN_COMMENT = "comment";
    public static final String COLUMN_VALUE = "value";

    public static final String TABLE_CONFIG = "log_config";
    public static final String COLUMN_TYPE = "log_type";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_ORDER = "order";
    public static final String COLUMN_IS_VALUE = "is_value";
    public static final String COLUMN_IS_SAFE = "is_safe";

    public static final String TABLE_SETTINGS = "settings";



    // Database creation sql statement
    private static final String DB_CREATE_CONFIG = "create table "
            + TABLE_CONFIG + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_TYPE + " int not null,"
            + COLUMN_DESCRIPTION + " text not null,"
            + COLUMN_ORDER + " int not null,"
            + COLUMN_IS_VALUE + " int not null,"
            + COLUMN_IS_SAFE + " int not null"
            + ");";

    private static final String DB_CREATE_LOG = "create table "
            + TABLE_LOG + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_DATE + " int not null,"
            + COLUMN_TIME + " int not null,"
            + COLUMN_CONFIG_ID + " int not null,"
            + COLUMN_STATE + " int,"
            + COLUMN_COMMENT + " text,"
            + COLUMN_VALUE + " real,"
            + " FOREIGN KEY (" + COLUMN_CONFIG_ID + ") REFERENCES " + TABLE_CONFIG+" (" + COLUMN_ID + ")"
            + ");";

    private static final String DB_CREATE_SETTINGS = "create table "
            + TABLE_SETTINGS + "("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_DESCRIPTION + " text not null,"
            + COLUMN_VALUE + " text not null"
            + ");";

    public LoggerDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DB_CREATE_CONFIG);
        database.execSQL(DB_CREATE_LOG);
        database.execSQL(DB_CREATE_SETTINGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LoggerDBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONFIG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }
}
