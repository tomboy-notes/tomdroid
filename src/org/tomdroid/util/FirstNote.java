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

import org.tomdroid.Note;
import org.tomdroid.R;

import android.app.Activity;
import android.text.TextUtils;

/**
 * Creates an introductory note object 
 * @author Olivier Bilodeau <olivier@bottomlesspit.org>
 *
 */
public class FirstNote {

	// Logging info
	private static final String	TAG = "FirstNote";
	
	public static Note createFirstNote(Activity activity) {
		TLog.v(TAG, "Creating first note");
		
		Note note = new Note();
		
		note.setTitle(activity.getString(R.string.firstNoteTitle));
		// FIXME as soon as we can create notes, make sure GUID is unique! - we are referencing this UUID elsewhere, don't forget to check! 
		note.setGuid("8f837a99-c920-4501-b303-6a39af57a714");
		note.setLastChangeDate("2010-10-09T16:50:12.219-04:00");
		
		
		// reconstitute HTML in note content 

		String[] contentarray = activity.getResources().getStringArray(R.array.firstNoteContent);
		String content = TextUtils.join("\n", contentarray);
		
		content = content.replaceAll("(?m)^=(.+)=$", "<size:large>$1</size:large>")
				.replaceAll("(?m)^-(.+)$", "<list-item dir=\"ltr\">$1</list-item>")
				.replaceAll("/list-item>\n<list-item", "/list-item><list-item")
				.replaceAll("(<list-item.+</list-item>)", "<list>$1</list>")
				.replaceAll("/list-item><list-item", "/list-item>\n<list-item");
		
		note.setXmlContent(content);
		
		return note;
	}
}
