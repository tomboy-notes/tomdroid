/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2010, Rodja Trappe <mail@rodja.net>
 * 
 * This file is part of Tomdroid.
 * 
 * Tomdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Tomdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Tomdroid. If not, see <http://www.gnu.org/licenses/>.
 */
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

	public MockedSyncServerTestCase() {
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
