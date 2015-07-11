package com.nemesis.minisocialnetwork.data;

/**
 * Created by Avijit Ghosh on 08 Jul 2015.
 */

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

public class TimeLineProvider extends ContentProvider {

    public static final String PROVIDER_NAME = "com.nemesis.minisocialnetwork.timeline";
    public static final String URL = "content://" + PROVIDER_NAME + "/timeline";
    public static final Uri CONTENT_URI = Uri.parse(URL);

    public static final String _ID = "_id";


    public static final String NAME = "name";
    public static final String POST = "post";
    public static final String LIKES = "likes";
    public static final String COMMENTS= "comments";
    public static final String UID= "uid";
    public static final String FID= "fid";



    private static HashMap<String, String> STUDENTS_PROJECTION_MAP;

    static final int TIMELINE = 1;
    static final int SPECIFICPOST = 2;

    static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "timeline", TIMELINE);
        uriMatcher.addURI(PROVIDER_NAME, "timeline/*", SPECIFICPOST);

    }

    /**
     * Database specific constant declarations
     */
    private SQLiteDatabase db;
    public static final String DATABASE_NAME = "minisocial.db";
    public static final String STUDENTS_TABLE_NAME = "timeline";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_DB_TABLE =
            " CREATE TABLE " + STUDENTS_TABLE_NAME +
                    " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    " name TEXT, " +
                    " post TEXT, " +
                    " likes TEXT, " +
                    " comments TEXT, " +
                    " uid TEXT NOT NULL, " +
                    " fid TEXT NOT NULL UNIQUE);";

    /**
     * Helper class that actually creates and manages 
     * the provider's underlying data repository.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " +  STUDENTS_TABLE_NAME);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        /**
         * Create a write able database which will trigger its 
         * creation if it doesn't already exist.
         */
        db = dbHelper.getWritableDatabase();
        return (db == null)? false:true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /**
         * Add a new student record
         */
        try{
            long rowID = db.insert(	STUDENTS_TABLE_NAME, "", values);
            if (rowID > 0)
            {
                Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
                getContext().getContentResolver().notifyChange(uri, null);
                return _uri;
            }

        }
        catch(Exception e)
        {
            //Unique constraint
        }
        /**
         * If record is added successfully
         */


        //throw new SQLException("Failed to add a record into " + uri);
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(STUDENTS_TABLE_NAME);

        switch (uriMatcher.match(uri)) {
            case TIMELINE:
                //qb.setProjectionMap(STUDENTS_PROJECTION_MAP);
                break;

            case SPECIFICPOST:
                qb.appendWhere( FID + "=" + uri.getPathSegments().get(1));
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (sortOrder == null || sortOrder == ""){
            /**
             * By default sort on student names
             */
            sortOrder = _ID;
        }
        Cursor c = qb.query(db,	projection,	selection, selectionArgs,null, null, sortOrder);

        /**
         * register to watch a content URI for changes
         *
         */

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }



    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case TIMELINE:
                count = db.delete(STUDENTS_TABLE_NAME, selection, selectionArgs);
                break;

            case SPECIFICPOST:
                String id = uri.getPathSegments().get(1);
                count = db.delete( STUDENTS_TABLE_NAME, _ID +  " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case TIMELINE:
                count = db.update(STUDENTS_TABLE_NAME, values, selection, selectionArgs);
                break;

            case SPECIFICPOST:
                count = db.update(STUDENTS_TABLE_NAME, values, _ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri );
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            /**
             * Get all student records 
             */
            case TIMELINE:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + PROVIDER_NAME + "/" + DATABASE_NAME;

            /**
             * Get a particular student
             */
            case SPECIFICPOST:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + PROVIDER_NAME + "/" + DATABASE_NAME +uri.getPathSegments().get(1);

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}
