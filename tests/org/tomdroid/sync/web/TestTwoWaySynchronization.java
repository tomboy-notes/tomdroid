package org.tomdroid.sync.web;

import org.json.JSONException;
import org.tomdroid.Note;
import org.tomdroid.sync.LocalStorage;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.content.Intent;
import android.os.Handler;
import android.test.ActivityUnitTestCase;
import android.test.InstrumentationTestCase;

public class TestTwoWaySynchronization extends ActivityUnitTestCase<Tomdroid> {

	private static final String	TAG	= "TestTwoWaySynchronization";
	private LocalStorage		localStorage;
	private MockSyncServer		server;
	private SnowySyncMethod		syncMethod;

	public TestTwoWaySynchronization() {
		super(Tomdroid.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Preferences.init(getInstrumentation().getContext(), false);

		startActivity(new Intent(), null, null);
		syncMethod = new SnowySyncMethod(getActivity(), new Handler() {
		});

		localStorage = new LocalStorage(getActivity());
		localStorage.resetDatabase();

		server = new MockSyncServer();
	}

	@Override
	public void tearDown() {
		localStorage.resetDatabase();
	}

	public void testInitialization() throws Exception {
		assertEquals("Max", server.firstName);
		assertEquals("Mustermann", server.lastName);

		assertTrue("should be in sync", server.isInSync());

		syncMethod.syncWith(server);
		assertTrue("should be in sync", server.isInSync());
	}

	public void testLoadingNewNoteFromServer() throws Exception {
		Note remoteNote = server.createNewNote();
		assertFalse("should be out of sync", server.isInSync());

		syncMethod.syncWith(server);
		assertTrue("should be in sync again", server.isInSync());

		assertEquals("note ids should be the same", server.getNoteIds(), localStorage.getNoteGuids());
		Note localNote = localStorage.getNote(remoteNote.getGuid());
		assertEquals(remoteNote, localNote);
	}

	public void testChangingNoteTitleOnServer() throws Exception {
		Note remoteNote = server.createNewNote();
		syncMethod.syncWith(server);

		remoteNote = server.setTitleOfNewestNote("Another Title");
		assertEquals("server should still have one note", 1, server.storedNotes.size());
		assertFalse("should be out of sync", server.isInSync());

		syncMethod.syncWith(server);
		assertTrue("should be in sync again", server.isInSync());

		assertEquals(1, localStorage.getNoteGuids().size());
		assertEquals("note ids should be the same", server.getNoteIds(), localStorage.getNoteGuids());
		
		Note localNote = localStorage.getNote(remoteNote.getGuid());
		assertEquals(remoteNote, localNote);
		assertEquals("local title should have changed", "Another Title", localNote.getTitle());
	}

	public void testChangingNoteContentOnServer() throws Exception {
		Note remoteNote = server.createNewNote();
		syncMethod.syncWith(server);

		remoteNote = server.setContentOfNewestNote("some other note content");
		assertEquals("server should still have one note", 1, server.storedNotes.size());
		assertFalse("should be out of sync", server.isInSync());

		syncMethod.syncWith(server);
		assertTrue("should be in sync again", server.isInSync());

		assertEquals(1, localStorage.getNoteGuids().size());
		assertEquals("note ids should be the same", server.getNoteIds(), localStorage.getNoteGuids());
		
		Note localNote = localStorage.getNote(remoteNote.getGuid());
		assertEquals(remoteNote, localNote);
		assertEquals("local content should have changed", "some other note content", localNote.getXmlContent());
	}
	
	private void assertEquals(Note expected, Note actual) throws JSONException {
		assertEquals("notes should be the same", expected.toJson().toString(), actual.toJson()
				.toString());
	}
}
