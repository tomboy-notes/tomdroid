package org.tomdroid.sync;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.database.Cursor;
import android.util.Log;

/**
 * Wrapper around NoteManager to hide "Android content cursors" and enables note storage
 * manipulation via Tomboy id's, titles and the like.
 */
public class LocalStorage {

	private static final String	TAG	= "LocalStorage";

	// TODO This data base accessor should not need a reference to an Activity. Currently the
	// NoteManager unfortunately uses managed queries (which is dispensable).
	private Activity			activity;

	public LocalStorage(Activity activity) {
		this.activity = activity;
	}

	/**
	 * Insert a note in the content provider. The identifier for the notes is the guid.
	 */
	public void insertNote(Note note) {
		NoteManager.putNote(this.activity, note);
	}

	/**
	 * merges content into the existing note. The identifier for the note is the guid.
	 */
	public void mergeNote(Note note) {
		// TODO implement a better merge algorithm then "append"
		Note storedNote = getNote(note.getGuid());
		if (storedNote != null && !storedNote.isSynced()){
			note.changeXmlContent(note.getXmlContent() + " --merged-- " + storedNote.getXmlContent());
		}
		NoteManager.putNote(this.activity, note);
	}

	public Set<String> getNoteGuids() {
		Set<String> idList = new HashSet<String>();

		Cursor idCursor = NoteManager.getGuids(this.activity);

		// cursor must not be null and must return more than 0 entry
		if (!(idCursor == null || idCursor.getCount() == 0)) {

			String guid;
			idCursor.moveToFirst();

			do {
				guid = idCursor.getString(idCursor.getColumnIndexOrThrow(Note.GUID));
				idList.add(guid);

			} while (idCursor.moveToNext());

		} else {

			// TODO send an error to the user
			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "Cursor returned null or 0 notes");
		}

		return idList;
	}

	public void deleteNotes(Set<String> guids) {

		for (String guid : guids) {
			NoteManager.deleteNote(activity, UUID.fromString(guid));
		}
	}

	/**
	 * Empties the complete database. Used to get a fresh start.
	 */
	public void resetDatabase() {
		activity.getContentResolver().delete(Tomdroid.CONTENT_URI, null, null);
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, 0);
	}

	public long getLatestSyncVersion() {
		return (Long) Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION);
	}

	public Note getNote(UUID guid) {
		return NoteManager.getNote(activity, guid);
	}

	public ArrayList<Note> getNewAndUpdatedNotes() {
		ArrayList<Note> notes = new ArrayList<Note>();

		String[] whereArgs = { "0" };
		Cursor cursor = activity.getContentResolver().query(Tomdroid.CONTENT_URI,
				NoteManager.FULL_PROJECTION, Note.IS_SYNCED + "=?", whereArgs, null);

		if (cursor == null)	return notes;
		if (cursor.getCount() == 0) {
			cursor.close();
			return notes;
		}
		
		cursor.moveToFirst();

		do {
			notes.add(new Note(cursor));
		} while (cursor.moveToNext());
		
		cursor.close();
		
		return notes;
	}

	public void onSynced(Long syncRevisionOfServer) {
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, syncRevisionOfServer);
		ArrayList<Note> syncedNotes = getNewAndUpdatedNotes();
		for (Note note : syncedNotes) {
			note.isSynced(true);
			NoteManager.putNote(activity, note);
		}
	}
}
