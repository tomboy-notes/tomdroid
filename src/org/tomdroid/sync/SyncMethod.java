package org.tomdroid.sync;

import java.util.ArrayList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.ui.Tomdroid;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public abstract class SyncMethod {

	private static final String	TAG					= "SyncMethod";

	private final static int	poolSize			= 1;

	// handler messages
	public final static int		PARSING_COMPLETE	= 1;
	public final static int		PARSING_FAILED		= 2;
	public final static int		PARSING_NO_NOTES	= 3;
	public final static int		NO_INTERNET			= 4;
	public final static int		SYNC_PROGRESS		= 5;

	private Activity			activity;
	private Handler				handler;

	private ExecutorService		pool;

	private int					syncProgress		= 100;

	private LocalStorage		localStorage;

	public SyncMethod(Activity activity, Handler handler) {

		this.activity = activity;
		this.handler = handler;
		pool = Executors.newFixedThreadPool(poolSize);

		localStorage = new LocalStorage(activity);
	}

	protected LocalStorage getLocalStorage(){
		return localStorage;
	}
	
	public void startSynchronization() {
		if (syncProgress != 100) {
			Toast.makeText((Context) activity, "Sync already in prgress", Toast.LENGTH_SHORT).show();
			return;
		}

		sync();
	}

	protected abstract void sync();

	public abstract boolean needsServer();

	public abstract boolean needsAuth();

	/**
	 * @return An unique identifier, not visible to the user.
	 */
	public abstract String getName();

	/**
	 * @return An human readable name, used in the preferences to distinguish the different sync
	 *         methods.
	 */
	public abstract String getDescription();

	/**
	 * Execute code in a separate thread. Use this for blocking and/or cpu intensive operations and
	 * thus avoid blocking the UI.
	 * 
	 * @param r
	 *            The Runner subclass to execute
	 */
	protected void execInThread(Runnable r) {

		pool.execute(r);
	}

	/**
	 * Send a message to the main UI.
	 * 
	 * @param message
	 *            The message id to send, the PARSING_* or NO_INTERNET attributes can be used.
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
		synchronized (TAG) {
			Log.v(TAG, "sync progress: " + progress);
			Message progressMessage = new Message();
			progressMessage.what = SYNC_PROGRESS;
			progressMessage.arg1 = progress;
			progressMessage.arg2 = syncProgress;

			handler.sendMessage(progressMessage);
			syncProgress = progress;
			
			if (progress == 100){
				onSyncCompleted();
			}
		}
	}

	protected void onSyncCompleted() {
		handler.sendEmptyMessage(PARSING_COMPLETE);
	}
	
	protected int getSyncProgress() {
		synchronized (TAG) {
			return syncProgress;
		}
	}

	public boolean isSyncable() {
		return getSyncProgress() == 100;
	}
}
