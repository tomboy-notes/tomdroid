/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
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
 * along with Tomdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomdroid.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.tomdroid.Note;

import android.util.Log;

/**
 * Creates an introductory note object 
 * @author Olivier Bilodeau <olivier@bottomlesspit.org>
 *
 */
public class FirstNote {

	// Logging info
	private static final String	TAG = "FirstNote";
	
	public static Note createFirstNote() {
		Log.v(TAG, "Creating first note");
		
		Note note = new Note();
		
		note.setTitle("Tomdroid's first note");
		// FIXME as soon as we can create notes, make sure GUID is unique! 
		note.setGuid("8f837a99-c920-4501-b303-6a39af57a714");
		note.setLastChangeDate("2010-10-09T16:50:12.219-04:00");
		note.setXmlContent(getString("FirstNote.Content"));
		
		return note;
	}

	// I bundled the note's content to avoid the hassle of Java strings (escaping quotes and the lack of multi-line support)
	private static final String BUNDLE_NAME = "org.tomdroid.util.FirstNote";
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

}
