package org.tomdroid;

import org.tomdroid.ui.Tomdroid;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbManager extends SQLiteOpenHelper {
	private String TAG = "dbManager";

	private static final String DATABASE_NAME = "tomdroid-notes.db";
	private static final int DB_VERSION = 4;

	public DbManager(Context context) {
		super(context, DATABASE_NAME, null, DB_VERSION);
	}


	@Override
	public void onCreate(SQLiteDatabase db) {
		 db.execSQL("CREATE TABLE " + Note.DB_TABLE	 + " ("
                 + Note.ID + " INTEGER PRIMARY KEY,"
                 + Note.GUID + " TEXT,"
                 + Note.TITLE + " TEXT,"
                 + Note.FILE + " TEXT,"
                 + Note.NOTE_CONTENT + " TEXT,"
                 + Note.MODIFIED_DATE + " STRING,"
                 + Note.TAGS + " STRING"
                 + ");");
		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Table note has been created");
		 
		db.execSQL("CREATE TABLE " + Notebook.DB_TABLE + " (" + 
        		Notebook.ID + " INTEGER PRIMARY KEY, " + 
        		Notebook.NAME + " STRING, " + 
        		Notebook.DISPLAY + " INTEGER" + 
        		");");
		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Table notebook has been created");

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (Tomdroid.LOGGING_ENABLED) {
    		Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
    	}
		
        db.execSQL("DROP TABLE IF EXISTS " + Note.DB_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Notebook.DB_TABLE);
        onCreate(db);

	}
}
