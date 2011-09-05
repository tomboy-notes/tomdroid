/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, 2010, 2011 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, 2010 Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2011 Stefan Hammer <j.4@gmx.at>
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
package org.tomdroid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.NoteListCursorAdapter;
import org.tomdroid.util.XmlUtils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.ListAdapter;

public class NoteManager {
	
	public static final String[] FULL_PROJECTION = { Note.ID, Note.TITLE, Note.FILE, Note.NOTE_CONTENT, Note.MODIFIED_DATE };
	public static final String[] LIST_PROJECTION = { Note.ID, Note.TITLE, Note.MODIFIED_DATE };
	public static final String[] TITLE_PROJECTION = { Note.TITLE };
	public static final String[] GUID_PROJECTION = { Note.ID, Note.GUID };
	public static final String[] ID_PROJECTION = { Note.ID };
	public static final String[] EMPTY_PROJECTION = {};
	
	// static properties
	private static final String TAG = "NoteManager";
	
	// gets a note from the content provider
	public static Note getNote(Activity activity, Uri uri) {
		
		Note note = null;
		
		// can we find a matching note?
		Cursor cursor = activity.managedQuery(uri, FULL_PROJECTION, null, null, null);
		// cursor must not be null and must return more than 0 entry 
		if (!(cursor == null || cursor.getCount() == 0)) {
			
			// create the note from the cursor
			cursor.moveToFirst();
			String noteContent = cursor.getString(cursor.getColumnIndexOrThrow(Note.NOTE_CONTENT));
			String noteTitle = cursor.getString(cursor.getColumnIndexOrThrow(Note.TITLE));
			
			note = new Note();
			note.setTitle(noteTitle);
			note.setXmlContent(stripTitleFromContent(noteContent, noteTitle));
		}
		
		return note;
	}
	
	// puts a note in the content provider
	public static void putNote(Activity activity, Note note) {
		
		// verify if the note is already in the content provider
		
		// TODO make the query prettier (use querybuilder)
		Uri notes = Tomdroid.CONTENT_URI;
		String[] whereArgs = new String[1];
		whereArgs[0] = note.getGuid().toString();
		
		// The note identifier is the guid
		ContentResolver cr = activity.getContentResolver();
		Cursor managedCursor = cr.query(notes,
                EMPTY_PROJECTION,  
                Note.GUID + "= ?",
                whereArgs,
                null);
		activity.startManagingCursor(managedCursor);
		
		// Preparing the values to be either inserted or updated
		// depending on the result of the previous query
		ContentValues values = new ContentValues();
		values.put(Note.TITLE, note.getTitle());
		values.put(Note.FILE, note.getFileName());
		values.put(Note.GUID, note.getGuid().toString());
		// Notice that we store the date in UTC because sqlite doesn't handle RFC3339 timezone information
		values.put(Note.MODIFIED_DATE, note.getLastChangeDate().format3339(false));
		values.put(Note.NOTE_CONTENT, note.getXmlContent());
		values.put(Note.TAGS, note.getTags());
		
		if (managedCursor.getCount() == 0) {
			
			// This note is not in the database yet we need to insert it
			if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"A new note has been detected (not yet in db)");
			
    		Uri uri = cr.insert(Tomdroid.CONTENT_URI, values);

    		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Note inserted in content provider. ID: "+uri+" TITLE:"+note.getTitle()+" GUID:"+note.getGuid());
		} else {
			
			// Overwrite the previous note if it exists
			cr.update(Tomdroid.CONTENT_URI, values, Note.GUID+" = ?", whereArgs);
			
			if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Note updated in content provider. TITLE:"+note.getTitle()+" GUID:"+note.getGuid());
		}
	}
	
	public static boolean deleteNote(Activity activity, int id)
	{
		Uri uri = Uri.parse(Tomdroid.CONTENT_URI+"/"+id);
		
		ContentResolver cr = activity.getContentResolver();
		int result = cr.delete(uri, null, null);
		
		if(result > 0)
			return true;
		else
			return false;
	}
	
	public static Cursor getAllNotes(Activity activity, Boolean includeNotebookTemplates) {
		// get a cursor representing all notes from the NoteProvider
		Uri notes = Tomdroid.CONTENT_URI;
		String where = null;
		String orderBy;
		if (!includeNotebookTemplates) {
			where = Note.TAGS + " NOT LIKE '%" + "system:template" + "%'";
		}
		orderBy = Note.MODIFIED_DATE + " DESC";
		return activity.managedQuery(notes, LIST_PROJECTION, where, null, orderBy);		
	}
	

	public static ListAdapter getListAdapter(Activity activity, String querys) {
		
		String where;
		if (querys==null) {
			where=null;
		} else {
			// sql statements to search notes
			String[] query = querys.split(" ");
			where="";
			int count=0;
			for (String string : query) {
				if (count>0) where = where + " AND ";
				where = where + "("+Note.TITLE+" LIKE '%"+string+"%' OR "+Note.NOTE_CONTENT+" LIKE '%"+string+"%')";
				count++;
			}	
		}

		// get a cursor representing all notes from the NoteProvider
		Uri notes = Tomdroid.CONTENT_URI;
		Cursor notesCursor = activity.managedQuery(notes, LIST_PROJECTION, where, null, null);
		
		// set up an adapter binding the TITLE field of the cursor to the list item
		String[] from = new String[] { Note.TITLE };
		int[] to = new int[] { R.id.note_title };
		return new NoteListCursorAdapter(activity, R.layout.main_list_item, notesCursor, from, to);
	}
	
	public static ListAdapter getListAdapter(Activity activity) {
		
		return getListAdapter(activity, null);
	}

	// gets the titles of the notes present in the db, used in ViewNote.buildLinkifyPattern()
	public static Cursor getTitles(Activity activity) {
		
		// get a cursor containing the notes titles
		return activity.managedQuery(Tomdroid.CONTENT_URI, TITLE_PROJECTION, null, null, null);
	}
	
	// gets the ids of the notes present in the db, used in SyncService.deleteNotes()
	public static Cursor getGuids(Activity activity) {
		
		// get a cursor containing the notes guids
		return activity.managedQuery(Tomdroid.CONTENT_URI, GUID_PROJECTION, null, null, null);
	}
	
	public static int getNoteId(Activity activity, String title) {
		
		int id = 0;
		
		// get the notes ids
		String[] whereArgs = { title };
		Cursor cursor = activity.managedQuery(Tomdroid.CONTENT_URI, ID_PROJECTION, Note.TITLE+"=?", whereArgs, null);
		
		// cursor must not be null and must return more than 0 entry 
		if (!(cursor == null || cursor.getCount() == 0)) {
			
			cursor.moveToFirst();
			id = cursor.getInt(cursor.getColumnIndexOrThrow(Note.ID));
		}
		else {
			// TODO send an error to the user
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "Cursor returned null or 0 notes");
		}
		
		return id;
	}
	
	/**
	 * stripTitleFromContent
	 * Because of an historic oddity in Tomboy's note format, a note's title is in a <title> tag but is also repeated
	 * in the <note-content> tag. This method strips it from <note-content>.
	 * @param noteContent
	 */
	private static String stripTitleFromContent(String xmlContent, String title) {
		// get rid of the title that is doubled in the note's content
		// using quote to escape potential regexp chars in pattern
		
		Pattern stripTitle = Pattern.compile("^\\s*"+Pattern.quote(XmlUtils.escape(title))+"\\n\\n"); 
		Matcher m = stripTitle.matcher(xmlContent);
		if (m.find()) {
			xmlContent = xmlContent.substring(m.end(), xmlContent.length());
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "stripped the title from note-content");
		}
		
		return xmlContent;
	}
}
