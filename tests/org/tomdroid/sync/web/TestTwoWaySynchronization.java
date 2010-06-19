package org.tomdroid.sync.web;

import org.tomdroid.sync.LocalStorage;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.InstrumentationTestCase;

public class TestTwoWaySynchronization extends ActivityUnitTestCase<Tomdroid> {

	private static final String	TAG	= "TestTwoWaySynchronization";
	private LocalStorage		localStorage;

	public TestTwoWaySynchronization() {
		super(Tomdroid.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Preferences.init(getInstrumentation().getContext(), false);

		startActivity(new Intent(), null, null);
		localStorage = new LocalStorage(getActivity());
		localStorage.reset();
	}

	@Override
	public void tearDown(){
		localStorage.reset();
	}

	public void testCreatingMockSyncServer() throws Exception {
		MockSyncServer server = new MockSyncServer();
		assertEquals("Max", server.firstName);
		assertEquals("Mustermann", server.lastName);

		assertTrue("should be in sync", server.isInSync());
	}
}
