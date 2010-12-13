/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2010, Rodja Trappe <mail@rodja.net>
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
package org.tomdroid.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.ErrorList;

import org.tomdroid.R;
import android.app.Activity;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public abstract class SyncService {
	
	private static final String TAG = "SyncService";
	
	private Activity activity;
	private final ExecutorService pool;
	private final static int poolSize = 1;
	
	private Handler handler;
	
	/**
	 * Contains the synchronization errors. These are stored while synchronization occurs
	 * and sent to the main UI along with the PARSING_COMPLETE message.
	 */
	private ErrorList syncErrors;
	private int syncProgress = 100;
	
	// handler messages
	public final static int PARSING_COMPLETE = 1;
	public final static int PARSING_FAILED = 2;
	public final static int PARSING_NO_NOTES = 3;
	public final static int NO_INTERNET = 4;
	public final static int NO_SD_CARD = 5;
	public final static int SYNC_PROGRESS = 6;
	
	public SyncService(Activity activity, Handler handler) {
		
		this.activity = activity;
		this.handler = handler;
		pool = Executors.newFixedThreadPool(poolSize);
	}

	public void startSynchronization() {
		
		if (syncProgress != 100){
			Toast.makeText(activity, activity.getString(R.string.messageSyncAlreadyInProgress), Toast.LENGTH_SHORT).show();
			return;
		}
		
		syncErrors = new ErrorList();
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
	 * Execute code in a separate thread.
	 * Any exception thrown by the thread will be added to the error list
	 * @param r The runner subclass to execute
	 */
	protected void syncInThread(final Runnable r) {
		Runnable task = new Runnable() {
			public void run() {
				try {
					r.run();
				} catch(Exception e) {
					sendMessage(PARSING_FAILED, ErrorList.createError("System Error", "system", e));
				}
			}
		};
		
		execInThread(task);
	}
	
	/**
	 * Insert a note in the content provider. The identifier for the notes is the guid.
	 * 
	 * @param note The note to insert.
	 */
	
	protected void insertNote(Note note) {
		
		NoteManager.putNote(this.activity, note);
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
		
		if(!sendMessage(message, null)) {
			handler.sendEmptyMessage(message);
		}
	}
	
	protected boolean sendMessage(int message_id, HashMap<String, Object> payload) {
		
		switch(message_id) {
		case PARSING_FAILED:
			syncErrors.add(payload);
			return true;
		case PARSING_COMPLETE:
			Message message = handler.obtainMessage(PARSING_COMPLETE, syncErrors);
			handler.sendMessage(message);
			return true;
		}
		
		return false;
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
		}
	}
	
	protected int getSyncProgress(){
		synchronized (TAG) {
			return syncProgress;
		}
	}

	public boolean isSyncable() {
		return getSyncProgress() == 100;
	}
}
