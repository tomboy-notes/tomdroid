/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, 2010 Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
 * 
 * This file is part of Tomdroid.
 * 
 * Tomdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Tomdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Tomdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomdroid;

import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.NoteListCursorAdapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.test.IsolatedContext;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class NoteManager {
	
	public static final String[] FULL_PROJECTION = { Note.ID, Note.TITLE, Note.FILE, Note.NOTE_CONTENT, Note.MODIFIED_DATE };
	public static final String[] LIST_PROJECTION = { Note.ID, Note.TITLE, Note.MODIFIED_DATE };
	public static final String[] TITLE_PROJECTION = { Note.TITLE };
	public static final String[] GUID_PROJECTION = { Note.ID, Note.GUID };
	public static final String[] ID_PROJECTION = { Note.ID };
	public static final String[] EMPTY_PROJECTION = {};
	public static final String[] LIST_NOTEBOOK = {Notebook.ID, Notebook.NAME };
	
	public static final int SORT_BY_DATE=1;
	public static final int SORT_BY_NAME=2;
	
	// static properties
	private static final String TAG = "NoteManager";
	
	// gets a note from the content provider
	public static Note getNote(Activity activity, Uri uri) {
		
		Note note = null;
		
		// can we find a matching note?
		Cursor cursor = activity.managedQuery(uri, FULL_PROJECTION, null, null, null);
		// cursor must not be null and must return more than 0 entry 
		if (!(cursor == null || cursor.getCount() == 0)) {
			
			// create the note from the cursor
			cursor.moveToFirst();
			String noteContent = cursor.getString(cursor.getColumnIndexOrThrow(Note.NOTE_CONTENT));
			String noteTitle = cursor.getString(cursor.getColumnIndexOrThrow(Note.TITLE));
			
			note = new Note();
			note.setXmlContent(noteContent);
			note.setTitle(noteTitle);
		}
		
		return note;
	}
	
	// puts a note in the content provider
	public static void putNote(Activity activity, Note note) {

		// verify if the note is already in the content provider
		
		// TODO make the query prettier (use querybuilder)
		Uri notes = Tomdroid.CONTENT_URI;
		String[] whereArgs = new String[1];
		whereArgs[0] = note.getGuid().toString();
		
		// The note identifier is the guid
		ContentResolver cr = activity.getContentResolver();
		Cursor managedCursor = cr.query(notes,
                EMPTY_PROJECTION,  
                Note.GUID + "= ?",
                whereArgs,
                null);
		activity.startManagingCursor(managedCursor);
		
		// Preparing the values to be either inserted or updated
		// depending on the result of the previous query
		ContentValues values = new ContentValues();
		values.put(Note.TITLE, note.getTitle());
		values.put(Note.FILE, note.getFileName());
		values.put(Note.GUID, note.getGuid().toString());
		// Notice that we store the date in UTC because sqlite doesn't handle RFC3339 timezone information
		values.put(Note.MODIFIED_DATE, note.getLastChangeDate().format3339(false));
		values.put(Note.NOTE_CONTENT, note.getXmlContent());
		values.put(Note.TAGS, note.getTags());
		
		if (managedCursor.getCount() == 0) {
			
			// This note is not in the database yet we need to insert it
			if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"A new note has been detected (not yet in db)");
			
    		Uri uri = cr.insert(Tomdroid.CONTENT_URI, values);

    		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Note inserted in content provider. ID: "+uri+" TITLE:"+note.getTitle()+" GUID:"+note.getGuid());
		} else {
			
			// Overwrite the previous note if it exists
			cr.update(Tomdroid.CONTENT_URI, values, Note.GUID+" = ?", whereArgs);
			
			if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Note updated in content provider. TITLE:"+note.getTitle()+" GUID:"+note.getGuid());
		}

		// put TAGS in notebooks table
		putNotebook(activity, note.getTags());
	}
	
	public static boolean deleteNote(Activity activity, int id)
	{
		Uri uri = Uri.parse(Tomdroid.CONTENT_URI+"/"+id);
		
		ContentResolver cr = activity.getContentResolver();
		int result = cr.delete(uri, null, null);
		
		if(result > 0)
			return true;
		else
			return false;
	}
	
	public static Cursor getAllNotes(Activity activity, Boolean includeNotebookTemplates,int sort,String notebook) {
		// get a cursor representing all notes from the NoteProvider
		Uri notes = Tomdroid.CONTENT_URI;
		String where = null;
		String orderBy;
		if (!includeNotebookTemplates) {
			where = Note.TAGS + " NOT LIKE '%" + "system:template" + "%'";
		}
		
		if (notebook!=null){
			if (where!=null){
				where += " AND ";
			}
			where = Note.TAGS + " LIKE '%" + notebook + "%'";
			Log.i(TAG,"where : " + where);
		}
		
		orderBy = Note.MODIFIED_DATE + " DESC";
		if (sort==SORT_BY_DATE){
			orderBy = Note.MODIFIED_DATE + " DESC";
		}
		
		if (sort==SORT_BY_NAME){
			orderBy = Note.TITLE;
		}
		
		return activity.managedQuery(notes, LIST_PROJECTION, where, null, orderBy);		
	}
	

	public static ListAdapter getListAdapter(Activity activity,int sort,String notebook) {

		Cursor notesCursor = getAllNotes(activity, false,sort,notebook);
		
		// set up an adapter binding the TITLE field of the cursor to the list item
		String[] from = new String[] { Note.TITLE, Note.MODIFIED_DATE };
		int[] to = new int[] { R.id.note_title, R.id.note_date };
		return new NoteListCursorAdapter(activity, R.layout.main_list_item, notesCursor, from, to);
	}

	// gets the titles of the notes present in the db, used in ViewNote.buildLinkifyPattern()
	public static Cursor getTitles(Activity activity) {
		
		// get a cursor containing the notes titles
		return activity.managedQuery(Tomdroid.CONTENT_URI, TITLE_PROJECTION, null, null, null);
	}
	
	// gets the ids of the notes present in the db, used in SyncService.deleteNotes()
	public static Cursor getGuids(Activity activity) {
		
		// get a cursor containing the notes guids
		return activity.managedQuery(Tomdroid.CONTENT_URI, GUID_PROJECTION, null, null, null);
	}
	
	public static int getNoteId(Activity activity, String title) {
		
		int id = 0;
		
		// get the notes ids
		String[] whereArgs = { title };
		Cursor cursor = activity.managedQuery(Tomdroid.CONTENT_URI, ID_PROJECTION, Note.TITLE+"=?", whereArgs, null);
		
		// cursor must not be null and must return more than 0 entry 
		if (!(cursor == null || cursor.getCount() == 0)) {
			
			cursor.moveToFirst();
			id = cursor.getInt(cursor.getColumnIndexOrThrow(Note.ID));
		}
		else {
			// TODO send an error to the user
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "Cursor returned null or 0 notes");
		}
		
		return id;
	}
	
	public static Cursor getAllNotebooks(Activity activity, Boolean includeNotebookTemplates) {
		// get a cursor representing all notes from the NoteProvider
		Uri notebooks = Tomdroid.CONTENT_URI_NOTEBOOK;
		String order = Notebook.NAME;
		return activity.managedQuery(notebooks, LIST_NOTEBOOK, null, null, order);		
	}
	
	public static Cursor getAllNotebooksOLD(Activity activity, Boolean includeNotebookTemplates) {
		// get a cursor representing all notes from the NoteProvider
		Uri notes = Tomdroid.CONTENT_URI;
		String where = null;
		String orderBy;
		if (!includeNotebookTemplates) {
			where = Note.TAGS + " NOT LIKE '%" + "system:template" + "%'";
		}
		orderBy = Note.TAGS + " DESC";

		SQLiteDatabase db = null;
		Cursor notebooksCursor = null;
		Log.i(TAG,"Avant ouverture de la base");
		try{
			db = SQLiteDatabase.openDatabase("/data/data/org.tomdroid/databases/tomdroid-notes.db",null,SQLiteDatabase.OPEN_READONLY);
			if (!db.isOpen()){
				Log.e(TAG,"Impossible d'ouvir la base");
			}
			Log.i(TAG,"Apres ouverture de la base");
			//SQLiteOpenHelper mOpenHelper = new SQLiteOpenHelper(activity.getApplicationContext(), "notes", null, 1);
			
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder ();
			qb.setDistinct(true);
			Log.i(TAG,"setDistinct OK");
			qb.setTables("notes");
			Log.i(TAG,"setTables OK");
			try{
				notebooksCursor = qb.query(db, LIST_NOTEBOOK, null, null, null, null, orderBy);
				Log.i(TAG,"query OK");
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG,"query KO");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return notebooksCursor;
		//return activity.managedQuery(notes, LIST_NOTEBOOK, where, null, orderBy);		
	}
	

	public static ListAdapter getListAdapterNotebook(Activity activity) {
		Cursor notebooksCursor = getAllNotebooks(activity, false);
		String[] from = new String[] { Notebook.NAME };
		int[] to = new int[] { R.id.notebook_name };
		return new SimpleCursorAdapter(activity, R.layout.notebooks_list_item, notebooksCursor, from, to);
	}
	
	// puts a note in the content provider
	public static void putNotebook(Activity activity, String notebook) {
		if (notebook.compareTo("")!=0){
			//Log.i(TAG,"putNotebook : "+notebook);
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
					ContentResolver cr = activity.getContentResolver();
					Cursor managedCursor = cr.query(uriNotebooks,EMPTY_PROJECTION,"notebook= ?",whereArgs, null);
					activity.startManagingCursor(managedCursor);
					
					// Preparing the values to be either inserted or updated
					// depending on the result of the previous query
					ContentValues values = new ContentValues();
					values.put("notebook", notebook);
					
					if (managedCursor.getCount() == 0) {
						
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
}
