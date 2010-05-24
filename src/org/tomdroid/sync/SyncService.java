package org.tomdroid.sync;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.ui.Tomdroid;

import android.app.Activity;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public abstract class SyncService {
	
	private static final String TAG = "SyncService";
	
	private Activity activity;
	private final ExecutorService pool;
	private final static int poolSize = 1;
	
	private Handler handler;
	private int mSyncProgress = 100;
	
	// handler messages
	public final static int PARSING_COMPLETE = 1;
	public final static int PARSING_FAILED = 2;
	public final static int PARSING_NO_NOTES = 3;
	public final static int NO_INTERNET = 4;
	public final static int SYNC_PROGRESS = 5;
	
	public SyncService(Activity activity, Handler handler) {
		
		this.activity = activity;
		this.handler = handler;
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
	 * Insert a note in the content provider. The identifier for the notes is the guid.
	 * 
	 * @param note The note to insert.
	 */
	
	protected void insertNote(Note note, boolean syncFinished) {
		
		NoteManager.putNote(this.activity, note);
		
		// if last note warn in UI that we are done
		if (syncFinished) {
			handler.sendEmptyMessage(PARSING_COMPLETE);
		}
	}
	
	/**
	 * Delete notes in the content provider. The guids passed identify the notes existing
	 * on the remote end (ie. that shouldn't be deleted).
	 * 
	 * @param remoteGuids The notes NOT to delete.
	 */
	
	protected void deleteNotes(ArrayList<String> remoteGuids) {
		
		Cursor localGuids = NoteManager.getGuids(this.activity);
		
		// cursor must not be null and must return more than 0 entry 
		if (!(localGuids == null || localGuids.getCount() == 0)) {
			
			String localGuid;
			
			localGuids.moveToFirst();
			
			do {
				localGuid = localGuids.getString(localGuids.getColumnIndexOrThrow(Note.GUID));
				
				if(!remoteGuids.contains(localGuid)) {
					int id = localGuids.getInt(localGuids.getColumnIndexOrThrow(Note.ID));
					NoteManager.deleteNote(this.activity, id);
				}
				
			} while (localGuids.moveToNext());
			
		} else {
			
			// TODO send an error to the user
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "Cursor returned null or 0 notes");
		}
	}
	
	/**
	 * Send a message to the main UI.
	 * 
	 * @param message The message id to send, the PARSING_* or NO_INTERNET attributes can be used.
	 */
	
	protected void sendMessage(int message) {
		
		handler.sendEmptyMessage(message);
	}
	
	/**
	 * Update the synchronization progress
	 * 
	 * @param progress 
	 */
	
	protected void setSyncProgress(int progress) {
		Message progressMessage = new Message();
		progressMessage.what = SYNC_PROGRESS;
		progressMessage.arg1 = progress;
		progressMessage.arg2 = mSyncProgress;
		
		handler.sendMessage(progressMessage);
		mSyncProgress = progress;
	}
}
