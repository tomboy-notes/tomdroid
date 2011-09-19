package org.tomdroid;

import java.util.HashMap;

import org.tomdroid.ui.Tomdroid;

import android.content.ContentUris;
import android.content.UriMatcher;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class NotebookProvider extends ContentProvider {
	
	// ContentProvider stuff
	// --	
	
	private static final String DEFAULT_SORT_ORDER = Notebook.NAME;
	
    private static HashMap<String, String> notesProjectionMap;

    private static final int NOTEBOOKS = 1;
    private static final int NOTEBOOK_ID = 2;
    private static final int NOTEBOOK_TITLE = 3;
    
    private static final UriMatcher uriMatcher;
	
    // Logging info
    private static final String TAG = "NotebookProvider";
   
   
    private DbManager dbHelper;


	@Override
	public boolean onCreate() {
        dbHelper = new DbManager(getContext());
		return false;
	}
	
	@Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

    	qb.setTables(Notebook.DB_TABLE);
        qb.setProjectionMap(notesProjectionMap);
        switch (uriMatcher.match(uri)) {
        case NOTEBOOKS:
            break;

        case NOTEBOOK_ID:
            qb.appendWhere(Notebook.ID + "=" + uri.getPathSegments().get(1));
            break;
            
        case NOTEBOOK_TITLE:
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
        for (int i = 0; i < projection.length; i++) {
            Log.i(TAG, "projection:"+projection[i]);
		}
        Log.i(TAG, "selection:"+selection);
        if (selectionArgs==null){
        	 Log.i(TAG, "selectionArgs:"+selectionArgs);
        }else{
	        for (int i = 0; i < selectionArgs.length; i++) {
	            Log.i(TAG, "selectionArgs:"+selectionArgs[i]);
			}
        }
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
	    	if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"insert into notebook");
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
	        
	        if (values.containsKey(Notebook.DISPLAY) == false) {
	            values.put(Notebook.DISPLAY, 1);
	        }

	        SQLiteDatabase db = dbHelper.getWritableDatabase();
	        long rowId = db.insert(Notebook.DB_TABLE, Notebook.NAME, values); // not so sure I did the right thing here
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
	            count = db.delete(Notebook.DB_TABLE, where, whereArgs);
	            break;

	        case NOTEBOOK_ID:
	            String notebookId = uri.getPathSegments().get(1);
	            count = db.delete(Notebook.DB_TABLE, Notebook.ID + "=" + notebookId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
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
	            count = db.update(Notebook.DB_TABLE, values, where, whereArgs);
	            break;

	        case NOTEBOOK_ID:
	            String noteId = uri.getPathSegments().get(1);
	            count = db.update(Notebook.DB_TABLE, values, Notebook.ID + "=" + noteId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
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
	        notesProjectionMap.put(Notebook.DISPLAY, Notebook.DISPLAY);
	    }

}
