package com.viggi.lib_logger;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Dave on 3/24/2015.
 * Tutorial where I learned this:
 * http://www.vogella.com/tutorials/AndroidSQLite/article.html
 */
public class LoggerContentProvider extends ContentProvider
{
    // database
    private LoggerDBHelper database;

    // used for the UriMatcher
    private static final int LOG = 10;
    private static final int LOG_ID = 20;
    private static final int CONFIG = 30;
    private static final int CONFIG_ID = 40;
    private static final int SETTINGS = 50;
    private static final int SETTINGS_ID = 60;

    private static final String AUTHORITY = "com.viggi.logger.contentprovider";

    private static final String BASE_PATH_LOG = "log";
    private static final String BASE_PATH_CONFIG = "config";
    private static final String BASE_PATH_SETTINGS = "settings";

    public static final Uri CONTENT_URI_LOG = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_LOG);
    public static final Uri CONTENT_URI_CONFIG = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_CONFIG);
    public static final Uri CONTENT_URI_SETTINGS = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_SETTINGS);

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/todos";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/todo";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_LOG, LOG);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_LOG + "/#", LOG_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_CONFIG, CONFIG);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_CONFIG + "/#", CONFIG_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_SETTINGS, SETTINGS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_SETTINGS + "/#", SETTINGS_ID);
    }

    @Override
    public boolean onCreate() {
        database = new LoggerDBHelper(getContext());
        return false;
    }

    public String getTableForUriType(int uriType)
    {
        switch(uriType)
        {
            case LOG:
            case LOG_ID:
                return LoggerDBHelper.TABLE_LOG;
            case CONFIG:
            case CONFIG_ID:
                return LoggerDBHelper.TABLE_CONFIG;
            case SETTINGS:
            case SETTINGS_ID:
                return LoggerDBHelper.TABLE_SETTINGS;
        }

        return LoggerDBHelper.TABLE_LOG;
    }

    public String getBaseTypeForUriType(int uriType)
    {
        switch(uriType)
        {
            case LOG:
            case LOG_ID:
                return BASE_PATH_LOG;
            case CONFIG:
            case CONFIG_ID:
                return BASE_PATH_CONFIG;
            case SETTINGS:
            case SETTINGS_ID:
                return BASE_PATH_SETTINGS;
        }

        return BASE_PATH_LOG;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Cursor cursor = null;

        switch(sURIMatcher.match(uri))
        {
            case LOG:
                break;
        }

        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        int uriType = sURIMatcher.match(uri);

        // check if the caller has requested a column which does not exists
        checkColumns(projection, uriType);

        // Set the table
        queryBuilder.setTables(getTableForUriType(uriType));

        switch (uriType) {
            case LOG:
            case CONFIG:
            case SETTINGS:
                break;
            case LOG_ID:
            case CONFIG_ID:
            case SETTINGS_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(LoggerDBHelper.COLUMN_ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id = 0;

        switch (uriType) {
            case LOG:
            case CONFIG:
            case SETTINGS:
                id = sqlDB.insert(getTableForUriType(uriType), null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return Uri.parse(getBaseTypeForUriType(uriType) + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        int uriType = sURIMatcher.match(uri);
        String table = getTableForUriType(uriType);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {
            case LOG:
            case CONFIG:
            case SETTINGS:
                rowsDeleted = sqlDB.delete(table, selection, selectionArgs);
                break;
            case LOG_ID:
            case CONFIG_ID:
            case SETTINGS_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(table, LoggerDBHelper.COLUMN_ID + "=" + id, null);
                } else {
                    rowsDeleted = sqlDB.delete(table, LoggerDBHelper.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case LOG:
            case CONFIG:
            case SETTINGS:
                rowsUpdated = sqlDB.update(getTableForUriType(uriType), values, selection, selectionArgs);
                break;
            case LOG_ID:
            case CONFIG_ID:
            case SETTINGS_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(getTableForUriType(uriType), values, LoggerDBHelper.COLUMN_ID + "=" + id, null);
                } else {
                    rowsUpdated = sqlDB.update(getTableForUriType(uriType), values, LoggerDBHelper.COLUMN_ID
                            + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private String[] getColumnsForUriType(int uriType)
    {
        switch(uriType)
        {
            case LOG:
            case LOG_ID:
                return new String[]{LoggerDBHelper.COLUMN_ID, LoggerDBHelper.COLUMN_DATE,
                        LoggerDBHelper.COLUMN_TIME, LoggerDBHelper.COLUMN_CONFIG_ID,
                        LoggerDBHelper.COLUMN_STATE, LoggerDBHelper.COLUMN_COMMENT, LoggerDBHelper.COLUMN_VALUE};
            case CONFIG:
            case CONFIG_ID:
                return new String[]{LoggerDBHelper.COLUMN_ID, LoggerDBHelper.COLUMN_TYPE,
                        LoggerDBHelper.COLUMN_DESCRIPTION, LoggerDBHelper.COLUMN_IS_VALUE,
                        LoggerDBHelper.COLUMN_IS_SAFE};
            case SETTINGS:
            case SETTINGS_ID:
                return new String[]{LoggerDBHelper.COLUMN_ID, LoggerDBHelper.COLUMN_DESCRIPTION,
                        LoggerDBHelper.COLUMN_VALUE};
        }

        return new String[]{LoggerDBHelper.COLUMN_ID, LoggerDBHelper.COLUMN_DATE,
            LoggerDBHelper.COLUMN_TIME, LoggerDBHelper.COLUMN_CONFIG_ID,
            LoggerDBHelper.COLUMN_STATE, LoggerDBHelper.COLUMN_COMMENT, LoggerDBHelper.COLUMN_VALUE};
    }

    private void checkColumns(String[] projection, int uriType) {
        String[] available = getColumnsForUriType(uriType);
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
