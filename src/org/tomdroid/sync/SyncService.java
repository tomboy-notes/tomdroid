package org.tomdroid.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.tomdroid.Note;
import org.tomdroid.ui.Tomdroid;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public abstract class SyncService {
	
	private static final String TAG = "SyncService";
	
	private Activity activity;
	private final ExecutorService pool;
	private final static int poolSize = 1;
	
	public SyncService(Activity activity) {
		
		this.activity = activity;
		pool = Executors.newFixedThreadPool(poolSize);
	}
	
	public abstract void sync();
	public abstract boolean needsServer();
	public abstract boolean needsAuth();
	
	/**
	 * @return An unique identifier, not visible to the user.
	 */
	
	public abstract String getName();
	
	/**
	 * @return An human readable name, used in the preferences to distinguish the different sync services.
	 */
	
	public abstract String getDescription();
	
	/**
	 * Execute code in a separate thread.
	 * Use this for blocking and/or cpu intensive operations and thus avoid blocking the UI.
	 * 
	 * @param r The Runner subclass to execute
	 */
	
	protected void execInThread(Runnable r) {
		
		pool.execute(r);
	}
	
	/**
	 * Insert a note in the content provider. The identifier for the notes is the title.
	 * 
	 * @param note The note to insert.
	 */
	
	protected void insertNote(Note note) {
		
		// verify if the note is already in the content provider
		String[] projection = new String[] {
			    Note.ID,
			    Note.TITLE,
			};
		
		// TODO make the query prettier (use querybuilder)
		Uri notes = Tomdroid.CONTENT_URI;
		String[] whereArgs = new String[1];
		whereArgs[0] = note.getGuid().toString();
		
		// The note identifier is the guid
		ContentResolver cr = activity.getContentResolver();
		Cursor managedCursor = cr.query(notes,
                projection,  
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
}
