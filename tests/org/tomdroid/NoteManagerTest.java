package org.tomdroid;

import org.json.JSONObject;
import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.ui.Tomdroid;

import android.app.Activity;
import android.database.Cursor;
import android.test.ActivityInstrumentationTestCase2;

public class NoteManagerTest extends
		ActivityInstrumentationTestCase2<Tomdroid> {

	public NoteManagerTest() {
		super("org.tomdroid", Tomdroid.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Activity activity = this.getActivity();
		// Add a regular note to the content manager.
		JSONObject note = new JSONObject(
				"{'title': 'foo', 'note-content': 'bar', " +
				"'guid': '002e91a2-2e34-4e2d-bf88-21def49a7704', " +
				"'last-change-date': '2009-04-19T21:29:23.2197340-07:00', " +
				"'tags': ['tag1', 'tag2']}");
		Note n = new Note(note);
		NoteManager.putNote(activity, n);
		
		// Add a notebook template to the content manager.
		JSONObject template = new JSONObject(
				"{'title': 'foo', 'note-content': 'bar', " +
				"'guid': '992e91a2-2e34-4e2d-bf88-21def49a7712', " +
				"'last-change-date': '2009-04-19T21:29:23.2197340-07:00', " +
				"'tags': ['system:template', 'tag2']}");
		Note t = new Note(template);
		NoteManager.putNote(activity, t);
	}
	
	public void testGetAllNotes() throws Exception {
		Cursor cursor;
		// Get all notes excluding the notebook template ones.
		cursor = NoteManager.getAllNotes(this.getActivity(), false);
		assertEquals(1, cursor.getCount());
		
		// Get all notes, including notebook templates this time.
		cursor = NoteManager.getAllNotes(this.getActivity(), true);
		assertEquals(2, cursor.getCount());
	}
	
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		// This is a hack to clear the DB after we're finished. What we
		// should really do is drop the whole table and let it be recreated
		// automatically the next time it's needed.
		Activity activity = getActivity();
		Cursor cursor = NoteManager.getIDs(activity);
		if (cursor.moveToFirst()) {
			do {
				NoteManager.deleteNote(activity, cursor.getInt(0));
			} while (cursor.moveToNext());
		}
	}
}
