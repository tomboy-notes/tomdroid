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
package org.tomdroid.sync;

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