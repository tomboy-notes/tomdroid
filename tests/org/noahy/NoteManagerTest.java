package org.tomdroid;

import org.json.JSONObject;
import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.test.ActivityUnitTestCase;

public class NoteManagerTest extends ActivityUnitTestCase<Tomdroid> {
	
	public NoteManagerTest() {
		super(Tomdroid.class);
	}

	public void testGetAllNotes() throws Exception {
		Activity activity = getActivity();
		putNotes(activity);
		Cursor cursor;
		// Get all notes excluding the notebook template ones.
		cursor = NoteManager.getAllNotes(activity, false);
		assertEquals(1, cursor.getCount());
		
		// Get all notes, including notebook templates this time.
		cursor = NoteManager.getAllNotes(activity, true);
		assertEquals(2, cursor.getCount());
	}

	private void putNotes(Activity a) throws Exception {
		// Add a regular note to the content manager.
		JSONObject note = new JSONObject(
				"{'title': 'foo', 'note-content': 'bar', " +
				"'guid': '002e91a2-2e34-4e2d-bf88-21def49a7704', " +
				"'last-change-date': '2009-04-19T21:29:23.2197340-07:00', " +
				"'tags': ['tag1', 'tag2']}");
		Note n = new Note(note);
		NoteManager.putNote(a, n);
		
		// Add a notebook template to the content manager.
		JSONObject template = new JSONObject(
				"{'title': 'foo', 'note-content': 'bar', " +
				"'guid': '992e91a2-2e34-4e2d-bf88-21def49a7712', " +
				"'last-change-date': '2009-04-19T21:29:23.2197340-07:00', " +
				"'tags': ['system:template', 'tag2']}");
		Note t = new Note(template);
		NoteManager.putNote(a, t);
	}
		
	@Override
	public void setUp() throws Exception {
		super.setUp();
		// XXX: For some reason this will raise an 
		// "Unable to add window -- token null is not for an application"
		// error when you run the test after wiping user data from the emulator.
		// The error is actually raised when we try to display the AlertDialog that
		// is shown the first time the user runs tomdroid.
		startActivity(new Intent(), null, null);
		// XXX: Soon we'll be able to replace the two lines below with LocalStorage.resetDatabase().
		getActivity().getContentResolver().delete(Tomdroid.CONTENT_URI, null, null);
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, 0);
	}
	
	@Override
	public void tearDown() throws Exception {
		// XXX: Soon we'll be able to replace the two lines below with LocalStorage.resetDatabase().
		getActivity().getContentResolver().delete(Tomdroid.CONTENT_URI, null, null);
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, 0);
		super.tearDown();
	}
}
