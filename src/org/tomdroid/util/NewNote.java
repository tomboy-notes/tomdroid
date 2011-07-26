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

import java.util.UUID;

import org.tomdroid.Note;

import android.util.Log;
import android.text.format.Time;


public class NewNote {

	// Logging info
	private static final String	TAG = "FirstNote";
	
	public static Note createNewNote( String title ) {
		Log.v(TAG, "Creating new note");
		
		Note note = new Note();
		
		note.setTitle( title );
		note.setGuid( UUID.randomUUID().toString() );
		Time time = new Time( Time.getCurrentTimezone() );
		time.setToNow();
		note.setLastChangeDate( time.format3339(false) );
		note.setXmlContent("");
		
		return note;
	}
}
