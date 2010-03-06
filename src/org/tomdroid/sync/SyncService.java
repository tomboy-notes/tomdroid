package org.tomdroid.sync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;

import android.app.Activity;
import android.os.Handler;

public abstract class SyncService {
	
	private static final String TAG = "SyncService";
	
	private Activity activity;
	private final ExecutorService pool;
	private final static int poolSize = 1;
	
	private Handler handler;
	
	// handler messages
	public final static int PARSING_COMPLETE = 1;
	public final static int PARSING_FAILED = 2;
	public final static int PARSING_NO_NOTES = 3;
	
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
	 * Insert a note in the content provider. The identifier for the notes is the title.
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
	 * Send a message to the main UI.
	 * 
	 * @param message The message id to send, the PARSING_* attributes can be used.
	 */
	
	protected void sendMessage(int message) {
		
		handler.sendEmptyMessage(message);
	}
}
