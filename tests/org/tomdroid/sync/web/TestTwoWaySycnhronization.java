package org.tomdroid.sync.web;

import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.InstrumentationTestCase;

public class TestTwoWaySycnhronization extends ActivityUnitTestCase<Tomdroid> {

	private static final String TAG = "TestTwoWaySycnhronization";

    public TestTwoWaySycnhronization() {
        super(Tomdroid.class);
    }
	
	@Override
	public void setUp() throws Exception{
		super.setUp();
		Preferences.init(getInstrumentation().getContext(), false);
		
		startActivity(new Intent(), null, null);
		SnowySyncMethod syncMethod = new SnowySyncMethod(getActivity(), null);
	}
	
	public void testCreatingMockSyncServer() throws Exception{
		MockSyncServer server = new MockSyncServer();
		assertEquals("Max", server.firstName);
		assertEquals("Mustermann", server.lastName);

		assertTrue(server.isInSync());
	}
}
