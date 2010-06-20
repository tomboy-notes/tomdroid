package org.tomdroid.sync.web;

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
		syncMethod = new SnowySyncMethod(getActivity(), new Handler() {});

		localStorage = new LocalStorage(getActivity());
		localStorage.reset();

		server = new MockSyncServer();
	}

	@Override
	public void tearDown() {
		localStorage.reset();
	}

	public void testInitialization() throws Exception {
		assertEquals("Max", server.firstName);
		assertEquals("Mustermann", server.lastName);

		assertTrue("should be in sync", server.isInSync());

		syncMethod.syncWith(server);
		assertTrue("should be in sync", server.isInSync());
	}

	public void testLoadingNewNoteFromServer() throws Exception {
		Note noteAsItIsStoredOnTheServer = server.createNewNote();
		assertFalse("should be out of sync", server.isInSync());

		syncMethod.syncWith(server);
		assertTrue("should be in sync", server.isInSync());

		assertEquals("note ids should be the same", localStorage.getLocalNoteIds(), server.getNoteIds());
	}
}
