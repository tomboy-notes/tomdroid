package org.tomdroid.sync.web;

import org.json.JSONException;
import org.tomdroid.Note;
import org.tomdroid.sync.LocalStorage;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.content.Intent;
import android.os.Handler;
import android.test.ActivityUnitTestCase;

public class MockedSyncServerTestCase  extends ActivityUnitTestCase<Tomdroid> {

	private LocalStorage		localStorage;
	private MockSyncServer		server;
	private SnowySyncMethod		syncMethod;

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

	public MockedSyncServerTestCase() {
		super(Tomdroid.class);
	}
	
	protected MockSyncServer getServer(){
		return server;
	}

	protected SnowySyncMethod getSyncMethod(){
		return syncMethod;
	}

	protected LocalStorage getLocalStorage(){
		return localStorage;
	}

	protected void assertEquals(Note expected, Note actual) throws JSONException {
		assertEquals("notes should be the same", expected.toJson().toString(), actual.toJson()
				.toString());
	}
}
