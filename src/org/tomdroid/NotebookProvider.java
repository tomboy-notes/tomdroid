package org.tomdroid;

import java.util.HashMap;

import org.tomdroid.ui.Tomdroid;

import android.content.ContentUris;
import android.content.UriMatcher;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class NotebookProvider extends ContentProvider {
	
	// ContentProvider stuff
	// --	
	private static final String DATABASE_NAME = "tomdroid-notes.db";
	private static final String DB_TABLE_NOTEBOOKS = "notebooks";
	private static final int DB_VERSION = 3;
	private static final String DEFAULT_SORT_ORDER = "notebook";
	
    private static HashMap<String, String> notesProjectionMap;

    private static final int NOTEBOOKS = 1;
    private static final int NOTEBOOK_ID = 2;
    private static final int NOTEBOOK_TITLE = 3;
    
    private static final UriMatcher uriMatcher;
	
    // Logging info
    private static final String TAG = "NotebookProvider";
    
    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + DB_TABLE_NOTEBOOKS	+ " (" + Notebook.ID + " INTEGER PRIMARY KEY, " + Notebook.NAME + " STRING);");
    		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Table notebook has been created");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	if (Tomdroid.LOGGING_ENABLED) {
        		Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        	}
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_NOTEBOOKS);
            onCreate(db);
        }
    }

    private DatabaseHelper dbHelper;


	@Override
	public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
		return false;
	}
	
	@Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
        case NOTEBOOKS:
            qb.setTables(DB_TABLE_NOTEBOOKS);
            qb.setProjectionMap(notesProjectionMap);
            break;

        case NOTEBOOK_ID:
            qb.setTables(DB_TABLE_NOTEBOOKS);
            qb.setProjectionMap(notesProjectionMap);
            qb.appendWhere(Notebook.ID + "=" + uri.getPathSegments().get(1));
            break;
            
        case NOTEBOOK_TITLE:
        	qb.setTables(DB_TABLE_NOTEBOOKS);
        	qb.setProjectionMap(notesProjectionMap);
        	// TODO appendWhere + whereArgs instead (new String[] whereArgs = uri.getLas..)?
        	qb.appendWhere(Notebook.NAME + " LIKE '" + uri.getLastPathSegment()+"'");
        	break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }
	
	  @Override
	    public String getType(Uri uri) {
		  	Log.i(TAG,"getType");
	        switch (uriMatcher.match(uri)) {
	        case NOTEBOOKS:
	            return Tomdroid.CONTENT_TYPE;
	
	        case NOTEBOOK_ID:
	            return Tomdroid.CONTENT_ITEM_TYPE;
	            
	        case NOTEBOOK_TITLE:
	        	return Tomdroid.CONTENT_ITEM_TYPE;
	
	        default:
	            throw new IllegalArgumentException("Unknown URI " + uri);
	        }
	    }
	  
	
	// TODO the following method is probably never called and probably wouldn't work
	    @Override
	    public Uri insert(Uri uri, ContentValues initialValues) {
	        // Validate the requested uri
	        if (uriMatcher.match(uri) != NOTEBOOKS) {
	            throw new IllegalArgumentException("Unknown URI " + uri);
	        }

	        ContentValues values;
	        if (initialValues != null) {
	            values = new ContentValues(initialValues);
	        } else {
	            values = new ContentValues();
	        }
	      

	        // TODO does this make sense?
	        if (values.containsKey(Notebook.NAME) == false) {
	            Resources r = Resources.getSystem();
	            values.put(Notebook.NAME, r.getString(android.R.string.untitled));
	        }

	        SQLiteDatabase db = dbHelper.getWritableDatabase();
	        long rowId = db.insert(DB_TABLE_NOTEBOOKS, Notebook.NAME, values); // not so sure I did the right thing here
	        if (rowId > 0) {
	            Uri notebookUri = ContentUris.withAppendedId(Tomdroid.CONTENT_URI_NOTEBOOK, rowId);
	            getContext().getContentResolver().notifyChange(notebookUri, null);
	            return notebookUri;
	        }

	        throw new SQLException("Failed to insert row into " + uri);
	    }

	    @Override
	    public int delete(Uri uri, String where, String[] whereArgs) {
	        SQLiteDatabase db = dbHelper.getWritableDatabase();
	        int count;
	        switch (uriMatcher.match(uri)) {
	        case NOTEBOOKS:
	            count = db.delete(DB_TABLE_NOTEBOOKS, where, whereArgs);
	            break;

	        case NOTEBOOK_ID:
	            String notebookId = uri.getPathSegments().get(1);
	            count = db.delete(DB_TABLE_NOTEBOOKS, Notebook.ID + "=" + notebookId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
	            break;

	        default:
	            throw new IllegalArgumentException("Unknown URI " + uri);
	        }

	        getContext().getContentResolver().notifyChange(uri, null);
	        return count;
	    }

	    @Override
	    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
	        SQLiteDatabase db = dbHelper.getWritableDatabase();
	        int count;
	        switch (uriMatcher.match(uri)) {
	        case NOTEBOOKS:
	            count = db.update(DB_TABLE_NOTEBOOKS, values, where, whereArgs);
	            break;

	        case NOTEBOOK_ID:
	            String noteId = uri.getPathSegments().get(1);
	            count = db.update(DB_TABLE_NOTEBOOKS, values, Notebook.ID + "=" + noteId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
	            break;

	        default:
	            throw new IllegalArgumentException("Unknown URI " + uri);
	        }

	        getContext().getContentResolver().notifyChange(uri, null);
	        return count;
	    }

	    static {
	        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	        uriMatcher.addURI(Tomdroid.AUTHORITY_NOTEBOOK, "notebooks", NOTEBOOKS);
	        uriMatcher.addURI(Tomdroid.AUTHORITY_NOTEBOOK, "notebooks/#", NOTEBOOK_ID);
	        uriMatcher.addURI(Tomdroid.AUTHORITY_NOTEBOOK, "notebooks/*", NOTEBOOK_TITLE);

	        notesProjectionMap = new HashMap<String, String>();
	        notesProjectionMap.put(Notebook.ID, Notebook.ID);
	        notesProjectionMap.put(Notebook.NAME, Notebook.NAME);
	    }

}
