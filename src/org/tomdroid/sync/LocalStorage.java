package org.tomdroid.sync;

import java.util.ArrayList;
import java.util.UUID;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
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
	 * 
	 * @param note
	 *            The note to insert.
	 */
	public void insertNote(Note note) {

		NoteManager.putNote(this.activity, note);
	}

	public ArrayList<String> getNoteGuids() {
		ArrayList<String> idList = new ArrayList<String>();

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

	/**
	 * Delete notes in the content provider. The guids passed identify the notes existing on the
	 * remote end (ie. that shouldn't be deleted).
	 * 
	 * @param remoteGuids
	 *            The notes NOT to delete.
	 */
	public void deleteNotes(ArrayList<String> remoteGuids) {

		Cursor localGuids = NoteManager.getGuids(this.activity);

		// cursor must not be null and must return more than 0 entry
		if (!(localGuids == null || localGuids.getCount() == 0)) {

			String localGuid;

			localGuids.moveToFirst();

			do {
				localGuid = localGuids.getString(localGuids.getColumnIndexOrThrow(Note.GUID));

				if (!remoteGuids.contains(localGuid)) {
					int id = localGuids.getInt(localGuids.getColumnIndexOrThrow(Note.ID));
					NoteManager.deleteNote(this.activity, id);
				}

			} while (localGuids.moveToNext());

		} else {

			// TODO send an error to the user
			if (Tomdroid.LOGGING_ENABLED)
				Log.d(TAG, "Cursor returned null or 0 notes");
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
}
