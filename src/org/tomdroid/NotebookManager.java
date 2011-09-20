package org.tomdroid;

import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.NotebookListCursorAdapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class NotebookManager {
	private static String TAG = "NotebookManager";
	public static final String[] FULL_PROJECTION = {Notebook.ID, Notebook.NAME,Notebook.DISPLAY};
	public static final String[] NAME_PROJECTION = {Notebook.ID, Notebook.NAME};
	
	public static NotebookListCursorAdapter getNotbookListAdapter(Activity activity){
		Log.i(TAG, "getNotbookListAdapter starting");
		
		// get a cursor representing all notes from the NoteProvider
		Cursor notebooksCursor = getAllNotebooks(activity, false);
		Log.i(TAG, "notebooksCursor OK");
		
		// set up an adapter binding the TITLE field of the cursor to the list item
		String[] from = new String[] { Notebook.NAME};
		int[] to = new int[] { R.id.notebook_name};
		return new NotebookListCursorAdapter(activity, R.layout.notebooks_list_item, notebooksCursor, from, to);
	}
	
	public static Cursor getAllNotebooks(Activity activity, Boolean includeNotebookTemplates) {
		// get a cursor representing all notes from the NoteProvider
		Uri notebooksUri = Tomdroid.CONTENT_URI_NOTEBOOK;
		String order = Notebook.NAME;
		Log.i(TAG, "getAllNotebooks :order OK");
		return activity.managedQuery(notebooksUri, FULL_PROJECTION, null, null, order);		
	}
	
	// puts a note in the content provider
	public static void putNotebook(Activity activity, String notebook) {
		if (notebook.compareTo("")!=0){
			if (Tomdroid.LOGGING_ENABLED) Log.i(TAG,"putNotebook : "+notebook);
			String[] notebooks = notebook.split(",");
			for (int i = 0; i < notebooks.length; i++) {
				notebook = notebooks[i];
				
				if (notebook.startsWith(Notebook.PATERN)){
					notebook = notebook.substring(Notebook.PATERN.length());
					// verify if the notebook is already in the content provider
					
					// TODO make the query prettier (use querybuilder)
					Uri uriNotebooks = Tomdroid.CONTENT_URI_NOTEBOOK;
					String[] whereArgs = new String[1];
					whereArgs[0] = notebook;
					
					
					// The note identifier is the guid
					if (Tomdroid.LOGGING_ENABLED) Log.d(TAG,"Query database if this notebook already exist");
					ContentResolver cr = activity.getContentResolver();
					Cursor managedCursor = cr.query(uriNotebooks,NoteManager.EMPTY_PROJECTION,Notebook.NAME + "= ?",whereArgs, null);
					activity.startManagingCursor(managedCursor);
					
					// Preparing the values to be either inserted or updated
					// depending on the result of the previous query
					
					if (managedCursor.getCount() == 0) {
						if (Tomdroid.LOGGING_ENABLED) Log.d(TAG,"This notebook does not exist in the db. It will be insert into the db");
						ContentValues values = new ContentValues();
						values.put(Notebook.NAME, notebook);
						values.put(Notebook.DISPLAY, 1);
						
						// This note is not in the database yet we need to insert it
						if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"A new notebook has been detected (not yet in db)");
			
						Log.i(TAG,"putNotebook : ajout de "+notebook);
			    		Uri uri = cr.insert(uriNotebooks, values);
			
			    		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"notebook inserted in content provider. ID: "+uri+" notebook:"+notebook);
					} 
				}				
			}
			
			
		}
	}
	
	public static String getNotebookDisplayed(Activity activity){
		String filter = "";
		Uri notebooksUri = Tomdroid.CONTENT_URI_NOTEBOOK;
		String order = Notebook.NAME;
		Cursor cur =  activity.managedQuery(notebooksUri, NAME_PROJECTION, Notebook.DISPLAY + "=1", null, order);
		if (cur.moveToFirst()){
			int col = cur.getColumnIndex(Notebook.NAME);
			filter+= Note.TAGS + " like '%" + cur.getString(col)+"%'";
			while (cur.moveToNext()){
				filter+=" OR ";
				filter+= Note.TAGS + " like '%" + cur.getString(col)+"%'";
			}
			filter = "("+filter+")";
		}
		Log.i(TAG, "filter :"+filter);
		
		return filter;
	}
	
	
	
}
