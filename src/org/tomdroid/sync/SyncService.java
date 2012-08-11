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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.ui.SyncDialog;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.ErrorList;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.TLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SyncService {
	
	private static final String TAG = "SyncService";
	
	public Activity activity;
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
	public final static int NOTE_DELETED = 7;
	public final static int NOTE_PUSHED = 8;
	public final static int NOTE_PULLED = 9;
	public final static int NOTE_PUSH_ERROR = 10;
	public final static int NOTE_DELETE_ERROR = 11;
	public final static int NOTE_PULL_ERROR = 12;
	public final static int BEGIN_PROGRESS = 13;
	public final static int INCREMENT_PROGRESS = 14;
	public final static int IN_PROGRESS = 15;
	public SyncService(Activity activity, Handler handler) {
		
		this.activity = activity;
		this.handler = handler;
		pool = Executors.newFixedThreadPool(poolSize);
		syncErrors = new ErrorList();
	}

	public void startSynchronization(boolean push) {
		
		if (syncProgress != 100){
			sendMessage(IN_PROGRESS);
			return;
		}
		
		sync(push);
	}
	
	protected abstract void sync(boolean push);
	public abstract boolean needsServer();
	public abstract boolean needsLocation();
	public abstract boolean needsAuth();
	
	/**
	 * @return An unique identifier, not visible to the user.
	 */
	
	public abstract String getName();
	
	/**
	 * @return An human readable name, used in the preferences to distinguish the different sync services.
	 */
	public abstract int getDescriptionAsId();
	
	public String getDescription() {
		return activity.getString(getDescriptionAsId());
	}
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
	 * Insert last note in the content provider.
	 * 
	 * @param note The note to insert.
	 */
	
	protected void insertNote(Note note) {
		NoteManager.putNote(this.activity, note);
		sendMessage(INCREMENT_PROGRESS );
	}	


	protected void syncNotes(ArrayList<Note> notesList, boolean push) {

		ArrayList<String> remoteGuids = new ArrayList<String>();
		ArrayList<Note> pushableNotes = new ArrayList<Note>();
		ArrayList<Note> pullableNotes = new ArrayList<Note>();
		HashMap<String,Note[]> comparableNotes = new HashMap<String,Note[]>();
		ArrayList<String> deleteableNotes = new ArrayList<String>();
		
		for ( Note remoteNote : notesList) {
			Note localNote = NoteManager.getNoteByGuid(activity,remoteNote.getGuid());
			remoteGuids.add(remoteNote.getGuid());
			if(localNote == null) {
				pullableNotes.add(remoteNote);
			}
			else if(push) {
				Note[] compNotes = {localNote, remoteNote};
				comparableNotes.put(localNote.getGuid(), compNotes);
			}
		}
		// get non-remote notes
		
		Cursor localGuids = NoteManager.getGuids(this.activity);
		// cursor must not be null and must return more than 0 entry 
		if (!(localGuids == null || localGuids.getCount() == 0)) {
			
			String localGuid;
			
			localGuids.moveToFirst();
			do {
				localGuid = localGuids.getString(localGuids.getColumnIndexOrThrow(Note.GUID));
				
				if(!remoteGuids.contains(localGuid)) {
					int id = localGuids.getInt(localGuids.getColumnIndexOrThrow(Note.ID));
					Note note = NoteManager.getNoteByGuid(this.activity, localGuid);
					if(note.getTags().contains("system:deleted"))
						deleteableNotes.add(note.getGuid());
					else
						pushableNotes.add(note);
				}
				
			} while (localGuids.moveToNext());

		}
		TLog.d(TAG, "Notes to pull: {0}, Notes to push: {1}, Notes to delete: {2}, Notes to compare: {3}",pullableNotes.size(),pushableNotes.size(),deleteableNotes.size(),comparableNotes.size());
		
	// init progress bar

		HashMap<String, Object> hm = new HashMap<String, Object>();
		hm.put("total", pullableNotes.size()+pushableNotes.size()+comparableNotes.size()+deleteableNotes.size());
		sendMessage(BEGIN_PROGRESS,hm);

	// deal with notes that are not in local content provider - always pull
		
		for(Note note : pullableNotes)
			insertNote(note);

		setSyncProgress(70);
		
	// deal with notes not in remote service - push or delete
			
		// if two-way sync, push local only notes, otherwise delete them
		
		if(push) {
			SyncManager.getInstance().getCurrentService().pushNotes(pushableNotes);
		}
		else
			deleteNonRemoteNotes(pullableNotes);

		// deleted notes not in remote
		
		SyncManager.getInstance().getCurrentService().deleteNotes(deleteableNotes);
		
		setSyncProgress(80);

	// deal with notes in both - compare and push, pull or diff
		
		compareNotes(comparableNotes,push);
		
		setSyncProgress(90);
	}

	protected void deleteNonRemoteNotes(ArrayList<Note> notes) {
		
		for(Note note : notes) {
			NoteManager.deleteNote(this.activity, note.getDbId());
			sendMessage(INCREMENT_PROGRESS);
		}
	}

	
	private void compareNotes(HashMap<String, Note[]> comparableNotes, boolean push) {

		String syncDateString = Preferences.getString(Preferences.Key.LATEST_SYNC_DATE);
		Time syncDate = new Time();
		syncDate.parse3339(syncDateString);

		int compareCount = 0;
		
		for(Note[] notes : comparableNotes.values()) {
			
			Note localNote = notes[0];
			Note remoteNote = notes[1];

			int compareSyncLocal = Time.compare(syncDate, localNote.getLastChangeDate());
			int compareSyncRemote = Time.compare(syncDate, remoteNote.getLastChangeDate());
			int compareBoth = Time.compare(localNote.getLastChangeDate(), remoteNote.getLastChangeDate());

		// if not two-way, overwrite the local version
		
			if(!push) {
				NoteManager.putNote(activity, remoteNote);
			}
			else {
				
				// check date difference
				
				TLog.v(TAG, "compare both: {0}, compare local: {1}, compare remote: {2}", compareBoth, compareSyncLocal, compareSyncRemote);
				if(compareBoth != 0)
					TLog.v(TAG, "Different note dates");
				if((compareSyncLocal < 0 && compareSyncRemote < 0) || (compareSyncLocal > 0 && compareSyncRemote > 0))
					TLog.v(TAG, "both either older or newer");
					
				if(compareBoth != 0 && ((compareSyncLocal < 0 && compareSyncRemote < 0) || (compareSyncLocal > 0 && compareSyncRemote > 0))) { // sync conflict!  both are older or newer than last sync
					
					TLog.v(TAG, "note conflict... showing resolution dialog TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
					
					// send everything to Tomdroid so it can show Sync Dialog
				    Bundle bundle = new Bundle();	
					bundle.putString("title",remoteNote.getTitle());
					bundle.putString("file",remoteNote.getFileName());
					bundle.putString("guid",remoteNote.getGuid());
					bundle.putString("date",remoteNote.getLastChangeDate().format3339(false));
					bundle.putString("content", remoteNote.getXmlContent());
					bundle.putString("tags", remoteNote.getTags());
					bundle.putInt("datediff", compareBoth);
	
					Intent intent = new Intent(activity.getApplicationContext(), SyncDialog.class);	
					intent.putExtras(bundle);

					activity.startActivityForResult(intent, compareCount++);		
				}
				else if(compareBoth > 0) { // local newer 

					TLog.v(TAG, "local newer, pushing local to remote");
	
						/* pushing local changes, reject older incoming note.
						 * If the local counterpart has the tag "system:deleted", delete from both local and remote.
						 * Otherwise, push local to remote.
						 */
						
						if(localNote.getTags().contains("system:deleted")) {
							TLog.v(TAG, "local note is deleted, deleting from server TITLE:{0} GUID:{1}", localNote.getTitle(),localNote.getGuid());
							SyncManager.getInstance().deleteNote(localNote.getGuid()); // delete from remote
							NoteManager.deleteNote(activity,localNote.getDbId()); // really delete locally
						}
						else {
							TLog.v(TAG, "local note is newer, sending new version TITLE:{0} GUID:{1}", localNote.getTitle(),localNote.getGuid());
							SyncManager.getInstance().pushNote(localNote);
						}
				}
				else if(compareBoth < 0) { // local older
					TLog.v(TAG, "Local note is older, updating in content provider TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
					sendMessage(INCREMENT_PROGRESS);
					// pull remote changes
					NoteManager.putNote(activity, remoteNote);
				}
				else { // both same date
					if(localNote.getTags().contains("system:deleted")) {
						TLog.v(TAG, "local note is deleted, deleting from server TITLE:{0} GUID:{1}", localNote.getTitle(),localNote.getGuid());
						SyncManager.getInstance().deleteNote(localNote.getGuid()); // delete from remote
						NoteManager.deleteNote(activity,localNote.getDbId()); // really delete locally
					}
					else {
						TLog.v(TAG, "Notes are same date, updating in content provider TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
						
						sendMessage(INCREMENT_PROGRESS);
						// pull remote changes anyway
						NoteManager.putNote(activity, remoteNote);
					}
				}
			}
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
		
		Message message;
		
		switch(message_id) {
		case PARSING_FAILED:
		case NOTE_PUSH_ERROR:
		case NOTE_DELETE_ERROR:
		case NOTE_PULL_ERROR:
			syncErrors.add(payload);
			return true;
		case BEGIN_PROGRESS:
			 message = handler.obtainMessage(BEGIN_PROGRESS, payload);
			handler.sendMessage(message);			
			return true;
		case PARSING_COMPLETE:
			message = handler.obtainMessage(PARSING_COMPLETE, syncErrors);
			handler.sendMessage(message);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Update the synchronization progress
	 * 
	 * @param progress new progress (syncProgress is old)
	 */
	
	public void setSyncProgress(int progress) {
		synchronized (TAG) {
			TLog.v(TAG, "sync progress: {0}", progress);
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

	// new methods to T Edit
	
	protected abstract void pushNote(Note note);
	protected abstract void deleteNote(String guid);
	protected abstract void pullNote(String guid);

	public void setLastGUID(String lastGuid) {
		// TODO Auto-generated method stub
		
	}

	public void finishSync(boolean refresh) {
		// TODO Auto-generated method stub
		
	}

	protected void pushNotes(ArrayList<Note> notes) {
		if(notes.size() == 0)
			return;
		
		TLog.v(TAG, "pushing {0} notes to remote service",notes.size());
		for(Note note : notes) {
			pushNote(note);
		}
	}
	protected void deleteNotes(ArrayList<String> notes) {
		if(notes.size() == 0)
			return;
		
		TLog.v(TAG, "deleting {0} notes from remote service",notes.size());
		for(String note : notes) {
			deleteNote(note);
		}
	}

	public void pushNotes() {
		// TODO Auto-generated method stub
		
	}


}