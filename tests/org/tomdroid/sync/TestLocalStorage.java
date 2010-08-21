package org.tomdroid.sync;

import java.util.UUID;

import org.tomdroid.Note;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.content.Intent;
import android.test.ActivityUnitTestCase;

public class TestLocalStorage extends ActivityUnitTestCase<Tomdroid> {

	private LocalStorage		localStorage;

	public TestLocalStorage() {
		super(Tomdroid.class);
	}
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Preferences.init(getInstrumentation().getContext(), false);

		startActivity(new Intent(), null, null);
		localStorage = new LocalStorage(getActivity());
		localStorage.resetDatabase();
	}

	@Override
	public void tearDown() {
		localStorage.resetDatabase();
	}

	protected LocalStorage getLocalStorage(){
		return localStorage;
	}

	public void testOutOfSyncFlagStoringInContentProvider(){
		
		Note note = new Note();
		note.setTitle("title");
		note.setXmlContent("content");
		note.isSynced(true);
		assertTrue("default should be 'in sync'", note.isSynced());
		getLocalStorage().insertNote(note);
		note = getLocalStorage().getNote(note.getGuid());
		assertTrue("should still be 'in sync'", note.isSynced());

		note.changeXmlContent("modified content");
		assertFalse("should be 'out of sync'", note.isSynced());
		getLocalStorage().insertNote(note);
		note = getLocalStorage().getNote(note.getGuid());
		assertFalse("locally stored note should still marked as 'out of sync with server'", note
				.isSynced());

	}
}