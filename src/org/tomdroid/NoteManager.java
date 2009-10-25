package org.tomdroid;

import org.tomdroid.ui.Tomdroid;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class NoteManager {
	
	public static final String[] FULL_PROJECTION = { Note.ID, Note.TITLE, Note.FILE, Note.NOTE_CONTENT, Note.MODIFIED_DATE };
	public static final String[] LIST_PROJECTION = { Note.ID, Note.TITLE };
	public static final String[] TITLE_PROJECTION = { Note.TITLE };
	public static final String[] ID_PROJECTION = { Note.ID };
	public static final String[] EMPTY_PROJECTION = {};
	
	// static properties
	private static final String TAG = "NoteManager";
	
	private static NoteManager instance = null;
	// the main activity this manager is linked to
	private static Activity activity = null;
	
	public static NoteManager getInstance()
	{
		if(instance == null) {
			try {
				instance = new NoteManager();
			} catch (Exception e) {
				if(Tomdroid.LOGGING_ENABLED) Log.e(TAG, e.getMessage());
			}
		}
		
		return instance;
	}
	
	// instance properties
	private Cursor notesCursor;
	
	public NoteManager() throws Exception
	{
		if(activity == null)
			throw new Exception("init() has not been called.");
	}
	
	public static void init(Activity a)
	{
		activity = a;
	}
	
	// gets a note from the content provider
	public Note getNote(Uri uri) {
		
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
	public void putNote(Note note) {
		
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
		values.put(Note.NOTE_CONTENT, note.getXmlContent());
		
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
	}
	
	public ListAdapter getListAdapter() {
		
		// get a cursor representing all notes from the NoteProvider
		Uri notes = Tomdroid.CONTENT_URI;
		notesCursor = activity.managedQuery(notes, LIST_PROJECTION, null, null, null);
		
		// set up an adapter binding the TITLE field of the cursor to the list item
		String[] from = new String[] { Note.TITLE };
		int[] to = new int[] { R.id.note_title };
		return new SimpleCursorAdapter(activity, R.layout.main_list_item, notesCursor, from, to);
	}
	
	// gets the titles of the notes present in the db, used in ViewNote.buildLinkifyPattern()
	public Cursor getTitles() {
		
		// get a cursor containing the notes titles
		return activity.managedQuery(Tomdroid.CONTENT_URI, TITLE_PROJECTION, null, null, null);
	}
	
	public int getNoteId(String title) {
		
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
}
