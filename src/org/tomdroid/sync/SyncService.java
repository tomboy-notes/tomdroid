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
import android.text.TextUtils;
import android.text.format.Time;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.ui.CompareNotes;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.ErrorList;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.TLog;

import java.util.ArrayList;
import java.util.Collections;
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

	public boolean cancelled = false;

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
	public final static int NOTES_PUSHED = 13;
	public final static int BEGIN_PROGRESS = 14;
	public final static int INCREMENT_PROGRESS = 15;
	public final static int IN_PROGRESS = 16;
	public final static int NOTES_BACKED_UP = 17;
	public final static int NOTES_RESTORED = 18;
	public final static int CONNECTING_FAILED = 19;
	public final static int AUTH_COMPLETE = 20;
	public final static int AUTH_FAILED = 21;
	public final static int REMOTE_NOTES_DELETED = 22;
	public final static int SYNC_CANCELLED = 23;
	public final static int LATEST_REVISION = 24;

    public SyncService(Activity activity, Handler handler) {
		
		this.activity = activity;
		this.handler = handler;
		pool = Executors.newFixedThreadPool(poolSize);
	}

	public void startSynchronization(boolean push) {
		
		syncErrors = null;
		
		if (syncProgress != 100){
			sendMessage(IN_PROGRESS);
			return;
		}
		
		// deleting "First Note"
		Note firstNote = NoteManager.getNoteByGuid(activity, "8f837a99-c920-4501-b303-6a39af57a714");
		if(firstNote != null)
			NoteManager.deleteNote(activity, firstNote.getDbId());
		
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
					TLog.e(TAG, e, "Problem syncing in thread");
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

	// syncing based on updated local notes only
	
	protected void syncNotes(Cursor localGuids, boolean push) {
		ArrayList<String> remoteGuids = new ArrayList<String>();
		ArrayList<Note> pushableNotes = new ArrayList<Note>();
		ArrayList<Note> pullableNotes = new ArrayList<Note>();
		HashMap<String,Note[]> comparableNotes = new HashMap<String,Note[]>();
		ArrayList<String> deleteableNotes = new ArrayList<String>();
		
		localGuids.moveToFirst();
		do {
			Note note = NoteManager.getNoteByGuid(activity, localGuids.getString(localGuids.getColumnIndexOrThrow(Note.GUID)));
			
			if(!note.getTags().contains("system:template")) // don't push templates TODO: find out what's wrong with this, if anything
				pushableNotes.add(note);
		} while (localGuids.moveToNext());
		
		if(cancelled) {
			doCancel();
			return; 
		}		
		
		doSyncNotes(remoteGuids, pushableNotes, pullableNotes, comparableNotes, deleteableNotes, push);
	}

	
	protected void syncNotes(ArrayList<Note> notesList, boolean push) {

		ArrayList<String> remoteGuids = new ArrayList<String>();
		ArrayList<Note> pushableNotes = new ArrayList<Note>();
		ArrayList<Note> pullableNotes = new ArrayList<Note>();
		HashMap<String,Note[]> comparableNotes = new HashMap<String,Note[]>();
		ArrayList<String> deleteableNotes = new ArrayList<String>();
		
		// check if remote notes are already in local
		
		for ( Note remoteNote : notesList) {
			Note localNote = NoteManager.getNoteByGuid(activity,remoteNote.getGuid());
			remoteGuids.add(remoteNote.getGuid());
			if(localNote == null) {
				
				// check to make sure there is no note with this title, otherwise show conflict dialogue

				Cursor cursor = NoteManager.getTitles(activity);
				
				if (!(cursor == null || cursor.getCount() == 0)) {
					
					cursor.moveToFirst();
					do {
						String atitle = cursor.getString(cursor.getColumnIndexOrThrow(Note.TITLE));
						if(atitle.equals(remoteNote.getTitle())) {
							String aguid = cursor.getString(cursor.getColumnIndexOrThrow(Note.GUID));
							localNote = NoteManager.getNoteByGuid(activity, aguid);
							break;
						}
					} while (cursor.moveToNext());
				}
				if(localNote == null)
					pullableNotes.add(remoteNote);
				else { // compare two different notes with same title
					remoteGuids.add(localNote.getGuid()); // add to avoid catching it below
					Note[] compNotes = {localNote, remoteNote};
					comparableNotes.put(localNote.getGuid(), compNotes);
				}
			}
			else {
				Note[] compNotes = {localNote, remoteNote};
				comparableNotes.put(localNote.getGuid(), compNotes);
			}
		}

		if(cancelled) {
			doCancel();
			return; 
		}
		
		// get non-remote notes; if newer than last sync, push, otherwise delete
		
		Cursor localGuids = NoteManager.getGuids(this.activity);
		if (!(localGuids == null || localGuids.getCount() == 0)) {
			
			String localGuid;
			
			localGuids.moveToFirst();
			do {
				localGuid = localGuids.getString(localGuids.getColumnIndexOrThrow(Note.GUID));
				
				if(!remoteGuids.contains(localGuid)) {
					Note note = NoteManager.getNoteByGuid(this.activity, localGuid);
					String syncDateString = Preferences.getString(Preferences.Key.LATEST_SYNC_DATE);
					Time syncDate = new Time();
					syncDate.parse3339(syncDateString);
					int compareSync = Time.compare(syncDate, note.getLastChangeDate());
					if(compareSync > 0) // older than last sync, means it's been deleted from server
						NoteManager.deleteNote(this.activity, note.getDbId());
					else if(!note.getTags().contains("system:template")) // don't push templates TODO: find out what's wrong with this, if anything
						pushableNotes.add(note);
				}
				
			} while (localGuids.moveToNext());

		}
		TLog.d(TAG, "Notes to pull: {0}, Notes to push: {1}, Notes to delete: {2}, Notes to compare: {3}",pullableNotes.size(),pushableNotes.size(),deleteableNotes.size(),comparableNotes.size());

		if(cancelled) {
			doCancel();
			return; 
		}
		doSyncNotes(remoteGuids, pushableNotes, pullableNotes, comparableNotes, deleteableNotes, push);
	}
	
	// actually do sync
	private void doSyncNotes(ArrayList<String> remoteGuids,
			ArrayList<Note> pushableNotes, ArrayList<Note> pullableNotes,
			HashMap<String, Note[]> comparableNotes,
			ArrayList<String> deleteableNotes, boolean push) {
		
	// init progress bar
		
		int totalNotes = pullableNotes.size()+pushableNotes.size()+comparableNotes.size()+deleteableNotes.size();
		
		if(totalNotes > 0) {
			sendMessage(BEGIN_PROGRESS,totalNotes,0);
		}
		else { // quit
			setSyncProgress(100);
			sendMessage(PARSING_COMPLETE);
			return;
		}

		if(cancelled) {
			doCancel();
			return; 
		}
		
	// deal with notes that are not in local content provider - always pull
		
		for(Note note : pullableNotes)
			insertNote(note);

		setSyncProgress(70);

		if(cancelled) {
			doCancel();
			return; 
		}
		
	// deal with notes not in remote service - push or delete
		
		// if one-way sync, delete pushable notes, else add for mock comparison
		if(!push)
			deleteNonRemoteNotes(pushableNotes);
		else {
			for(Note note : pushableNotes) {
				Note blankNote = new Note();
				Note[] compNotes = {note, blankNote};
				comparableNotes.put(note.getGuid(), compNotes);
			}
		}
		
		setSyncProgress(80);

		if(cancelled) {
			doCancel();
			return; 
		}
		
	// deal with notes in both (as well as pushable notes) - compare and push, pull or diff
		
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

		ArrayList<Note> pushableNotes = new ArrayList<Note>();
		HashMap<String,Note[]> conflictingNotes = new HashMap<String,Note[]>();
		
		String syncDateString = Preferences.getString(Preferences.Key.LATEST_SYNC_DATE);
		Time syncDate = new Time();
		syncDate.parse3339(syncDateString);

		int compareCount = 0;
		
		for(Note[] notes : comparableNotes.values()) {
			
			Note localNote = notes[0];
			Note remoteNote = notes[1];

		//	if no remote note, push
			
			if(remoteNote.getGuid() == null && push) {
				TLog.i(TAG, "no remote note, pushing");
				pushableNotes.add(localNote);
				continue;
			}
			
		// if different guids, means conflicting titles

			if(!remoteNote.getGuid().equals(localNote.getGuid())) {
				TLog.i(TAG, "adding conflict of two different notes with same title");
				conflictingNotes.put(remoteNote.getGuid(), notes);
				continue;
			}
			
			if(cancelled) {
				doCancel();
				return; 
			}
			
			int compareSyncLocal = Time.compare(syncDate, localNote.getLastChangeDate());
			int compareSyncRemote = Time.compare(syncDate, remoteNote.getLastChangeDate());
			int compareBoth = Time.compare(localNote.getLastChangeDate(), remoteNote.getLastChangeDate());

		// if not two-way and not same date, overwrite the local version
		
			if(!push && compareBoth != 0) {
				TLog.i(TAG, "Different note dates, overwriting local note");
				sendMessage(INCREMENT_PROGRESS);
				NoteManager.putNote(activity, remoteNote);
				continue;
			}

		// begin compare

			if(cancelled) {
				doCancel();
				return; 
			}
			
			// check date difference
			
			TLog.v(TAG, "compare both: {0}, compare local: {1}, compare remote: {2}", compareBoth, compareSyncLocal, compareSyncRemote);
			if(compareBoth != 0)
				TLog.v(TAG, "Different note dates");
			if((compareSyncLocal < 0 && compareSyncRemote < 0) || (compareSyncLocal > 0 && compareSyncRemote > 0))
				TLog.v(TAG, "both either older or newer");
				
			if(compareBoth != 0 && ((compareSyncLocal < 0 && compareSyncRemote < 0) || (compareSyncLocal > 0 && compareSyncRemote > 0))) // sync conflict!  both are older or newer than last sync
				conflictingNotes.put(remoteNote.getGuid(), notes);
			else if(compareBoth > 0) // local newer, bundle in pushable
				pushableNotes.add(localNote);
			else if(compareBoth < 0) { // local older, pull immediately, no need to bundle
				TLog.i(TAG, "Local note is older, updating in content provider TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
				sendMessage(INCREMENT_PROGRESS);
				NoteManager.putNote(activity, remoteNote);
			}
			else { // both same date
				if(localNote.getTags().contains("system:deleted") && push) { // deleted, bundle for remote deletion
					TLog.i(TAG, "Notes are same date, deleted, deleting remote: TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
					pushableNotes.add(localNote);
				}
				else { // do nothing
					TLog.i(TAG, "Notes are same date, doing nothing: TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
					sendMessage(INCREMENT_PROGRESS);
					// NoteManager.putNote(activity, remoteNote);
				}
			}
		}

		if(cancelled) {
			doCancel();
			return; 
		}
		
	// push pushable notes
		if(push)
			pushNotes(pushableNotes); 

		if(cancelled) {
			doCancel();
			return; 
		}
		
	// fix conflicting notes
		
		for (Note[] notes : conflictingNotes.values()) {
			
			Note localNote = notes[0];
			Note remoteNote = notes[1];
			int compareBoth = Time.compare(localNote.getLastChangeDate(), remoteNote.getLastChangeDate());
			
			TLog.v(TAG, "note conflict... showing resolution dialog TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
			
			// send everything to Tomdroid so it can show Sync Dialog
			
		    Bundle bundle = new Bundle();	
			bundle.putString("title",remoteNote.getTitle());
			bundle.putString("file",remoteNote.getFileName());
			bundle.putString("guid",remoteNote.getGuid());
			bundle.putString("date",remoteNote.getLastChangeDate().format3339(false));
			bundle.putString("content", remoteNote.getXmlContent());
			bundle.putSerializable("tags", remoteNote.getTags());
			bundle.putInt("datediff", compareBoth);
			
			// put local guid if conflicting titles

			if(!remoteNote.getGuid().equals(localNote.getGuid()))
				bundle.putString("localGUID", localNote.getGuid());
			
			Intent intent = new Intent(activity.getApplicationContext(), CompareNotes.class);	
			intent.putExtras(bundle);
	
			activity.startActivityForResult(intent, compareCount++);
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
	protected void sendMessage(int message_id, int arg1, int arg2) {
		Message message = handler.obtainMessage(message_id);
		message.arg1 = arg1;
		message.arg2 = arg2;
		handler.sendMessage(message);
	}	
	protected boolean sendMessage(int message_id, HashMap<String, Object> payload) {

		Message message;
		String text;
		switch(message_id) {
			case PARSING_FAILED:
			case NOTE_PUSH_ERROR:
			case NOTE_DELETE_ERROR:
			case NOTE_PULL_ERROR:
			case PARSING_COMPLETE:
				if(payload == null && syncErrors == null)
					return false;
				message = handler.obtainMessage(message_id, syncErrors);
				syncErrors.add(payload);
				handler.sendMessage(message);
				return true;
		}
		return false;
	}
	
	/**
	 * Update the synchronization progress
	 * 
	 * TODO: rename to distinguish from new progress?
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

	public abstract void finishSync(boolean refresh);

	public abstract void pushNotes(ArrayList<Note> notes);

	protected abstract void pushNotes();

	public abstract void backupNotes();

	public abstract void deleteAllNotes();

	public void setCancelled(boolean cancel) {
		this.cancelled  = cancel;
	}

	public boolean doCancel() {
		TLog.v(TAG, "sync cancelled");
		
		setSyncProgress(100);
		sendMessage(SYNC_CANCELLED);
		
		return true;
	}
}