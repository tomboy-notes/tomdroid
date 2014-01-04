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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.ui.CompareNotes;
import org.tomdroid.util.ErrorList;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.TLog;
import org.tomdroid.util.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class SyncService {
	
	private static final String TAG = "SyncService";
	
	public Activity activity;
	private final ExecutorService pool;
	private final static int poolSize = 1;
	
	private Handler handler;
	protected static boolean push;
	
	/**
	 * Contains the synchronization errors. These are stored while synchronization occurs
	 * and sent to the main UI along with the PARSING_COMPLETE message.
	 */
	private ErrorList syncErrors;
	private int syncProgress = 100;

	public boolean cancelled = false;

	// syncing arrays
	private ArrayList<String> remoteGuids;
	private ArrayList<Note> pushableNotes;
	private ArrayList<Note> pullableNotes;
	private ArrayList<Note[]> comparableNotes;
	private ArrayList<Note> deleteableNotes;
	private ArrayList<Note[]> conflictingNotes;
	
	// number of conflicting notes
	private int conflictCount;
	private int resolvedCount;
	
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
	public final static int SYNC_CONNECTED = 25;
	
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
		
		getNotesForSync(push);
	}
	
	protected abstract void getNotesForSync(boolean push);
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
	protected void prepareSyncableNotes(Cursor localGuids) {
		remoteGuids = new ArrayList<String>();
		pushableNotes = new ArrayList<Note>();
		pullableNotes = new ArrayList<Note>();
		comparableNotes = new ArrayList<Note[]>();
		deleteableNotes = new ArrayList<Note>();
		conflictingNotes = new ArrayList<Note[]>();
		
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
		
		doSyncNotes();
	}

	
	// syncing with remote changes
	protected void prepareSyncableNotes(ArrayList<Note> notesList) {

		remoteGuids = new ArrayList<String>();
		pushableNotes = new ArrayList<Note>();
		pullableNotes = new ArrayList<Note>();
		comparableNotes = new ArrayList<Note[]>();
		deleteableNotes = new ArrayList<Note>();
		conflictingNotes = new ArrayList<Note[]>();
		
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
				cursor.close();
				
				if(localNote == null)
					pullableNotes.add(remoteNote);
				else { // compare two different notes with same title
					remoteGuids.add(localNote.getGuid()); // add to avoid catching it below
					Note[] compNotes = {localNote, remoteNote};
					comparableNotes.add(compNotes);
				}
			}
			else {
				Note[] compNotes = {localNote, remoteNote};
				comparableNotes.add(compNotes);
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
					syncDate.parseTomboy(syncDateString);
					int compareSync = Time.compare(syncDate, note.getLastChangeDate());
					if(compareSync > 0) // older than last sync, means it's been deleted from server
						deleteableNotes.add(note);
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


	// deal with notes in both - compare and push, pull or diff
		
		String syncDateString = Preferences.getString(Preferences.Key.LATEST_SYNC_DATE);
		Time syncDate = new Time();
		syncDate.parseTomboy(syncDateString);

		for(Note[] notes : comparableNotes) {
			
			Note localNote = notes[0];
			Note remoteNote = notes[1];

		// if different guids, means conflicting titles

			if(!remoteNote.getGuid().equals(localNote.getGuid())) {
				TLog.i(TAG, "adding conflict of two different notes with same title");
				conflictingNotes.add(notes);
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
				pullableNotes.add(remoteNote);
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
				
			if(compareBoth != 0 && ((compareSyncLocal < 0 && compareSyncRemote < 0) || (compareSyncLocal > 0 && compareSyncRemote > 0))) { // sync conflict!  both are older or newer than last sync
				TLog.i(TAG, "Note Conflict: TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
				conflictingNotes.add(notes);
			}
			else if(compareBoth > 0) // local newer, bundle in pushable
				pushableNotes.add(localNote);
			else if(compareBoth < 0) { // local older, pull immediately, no need to bundle
				TLog.i(TAG, "Local note is older, updating in content provider TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
				pullableNotes.add(remoteNote);
			}
			else { // both same date
				if(localNote.getTags().contains("system:deleted") && push) { // deleted, bundle for remote deletion
					TLog.i(TAG, "Notes are same date, deleted, deleting remote: TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
					pushableNotes.add(localNote);
				}
				else { // do nothing
					TLog.i(TAG, "Notes are same date, doing nothing: TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
					// NoteManager.putNote(activity, remoteNote);
				}
			}
		}
		if(conflictingNotes.isEmpty())
			doSyncNotes();
		else 
			fixConflictingNotes();
	}

	// fix conflicting notes, putting sync on pause, to be resumed once conflicts are all resolved
	private void fixConflictingNotes() {
		
		conflictCount = 0;
		resolvedCount = 0;
		for (Note[] notes : conflictingNotes) {
			
			Note localNote = notes[0];
			Note remoteNote = notes[1];
			int compareBoth = Time.compare(localNote.getLastChangeDate(), remoteNote.getLastChangeDate());
			
			TLog.v(TAG, "note conflict... showing resolution dialog TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
			
			// send everything to Tomdroid so it can show Sync Dialog
			
		    Bundle bundle = new Bundle();	
			bundle.putString("title",remoteNote.getTitle());
			bundle.putString("file",remoteNote.getFileName());
			bundle.putString("guid",remoteNote.getGuid());
			bundle.putString("date",remoteNote.getLastChangeDate().formatTomboy());
			bundle.putString("content", remoteNote.getXmlContent());
			bundle.putString("tags", remoteNote.getTags());
			bundle.putInt("datediff", compareBoth);
			
			// put local guid if conflicting titles

			if(!remoteNote.getGuid().equals(localNote.getGuid()))
				bundle.putString("localGUID", localNote.getGuid());
			
			Intent intent = new Intent(activity.getApplicationContext(), CompareNotes.class);	
			intent.putExtras(bundle);
	
			// let activity know each time the conflict is resolved, to let the service know to increment resolved conflicts.
			// once all conflicts are resolved, start sync
			activity.startActivityForResult(intent, conflictCount++);
		}	
	}
	
	// actually do sync
	private void doSyncNotes() {

	// init progress bar
		
		int totalNotes = pullableNotes.size()+pushableNotes.size()+deleteableNotes.size();
		
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

	// deal with deleteable notes
		
		deleteNotes(deleteableNotes);
		
	// deal with notes not in remote service - push or delete
		
		if(pushableNotes.isEmpty())
			finishSync(true);
		else {
			// notify service that local syncing is complete, so it can update sync revision to remote
			localSyncComplete();
			setSyncProgress(90);

			// if one-way sync, delete pushable notes, else push
			if(!push) {
				deleteNotes(pushableNotes);
				finishSync(true);
			}
			else
				pushNotes(pushableNotes);
			
		} 
	}

	protected void deleteNotes(ArrayList<Note> notes) {
		
		for(Note note : notes)
			NoteManager.deleteNote(this.activity, note.getDbId());
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
		switch(message_id) {
			case PARSING_FAILED:
			case NOTE_PUSH_ERROR:
			case NOTE_DELETE_ERROR:
			case NOTE_PULL_ERROR:
			case PARSING_COMPLETE:
				if(payload == null && syncErrors == null)
					return false;
				if(syncErrors == null)
					syncErrors = new ErrorList();
				syncErrors.add(payload);
				message = handler.obtainMessage(message_id, syncErrors);
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
	
	protected abstract void pullNote(String guid);

	public abstract void finishSync(boolean refresh);

	public abstract void pushNotes(ArrayList<Note> notes);

	public abstract void backupNotes();

	public abstract void deleteAllNotes();

	protected abstract void localSyncComplete();

	public void setCancelled(boolean cancel) {
		this.cancelled  = cancel;
	}

	public boolean doCancel() {
		TLog.v(TAG, "sync cancelled");
		
		setSyncProgress(100);
		sendMessage(SYNC_CANCELLED);
		
		return true;
	}

	public void resolvedConflict(int requestCode) {
		resolvedCount++;
		if(resolvedCount == conflictCount)
			doSyncNotes();
	}

	public void addPullable(Note note) {
		this.pullableNotes.add(note);
	}

	public void addPushable(Note note) {
		this.pushableNotes.add(note);
	}

	public void addDeleteable(Note note) {
		this.deleteableNotes.add(note);
	}
}