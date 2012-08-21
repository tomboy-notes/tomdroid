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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import android.widget.ListAdapter;

import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.NoteListAdapter;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.TLog;
import org.tomdroid.util.XmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

@SuppressWarnings("deprecation")
public class NoteManager {
	
	public static final String[] FULL_PROJECTION = { Note.ID, Note.TITLE, Note.FILE, Note.NOTE_CONTENT, Note.MODIFIED_DATE, Note.GUID, Note.TAGS };
	public static final String[] LIST_PROJECTION = { Note.ID, Note.TITLE, Note.MODIFIED_DATE, Note.TAGS };
	public static final String[] DATE_PROJECTION = { Note.ID, Note.GUID, Note.MODIFIED_DATE };
	public static final String[] TITLE_PROJECTION = { Note.TITLE, Note.GUID };
	public static final String[] GUID_PROJECTION = { Note.ID, Note.GUID };
	public static final String[] ID_PROJECTION = { Note.ID };

    // static properties
	private static final String TAG = "NoteManager";
    public static final Pattern NOTEBOOK_PATTERN = Pattern.compile("system:notebook:([^,]*)(,|$)");

    // gets a note from the content provider, based on guid
	public static Note getNoteByGuid(Activity activity, String guid) {
		Uri notes = Tomdroid.CONTENT_URI;
		
		String[] whereArgs = new String[1];
		whereArgs[0] = guid;
		
		// The note identifier is the guid
		ContentResolver cr = activity.getContentResolver();
		Cursor cursor = cr.query(notes,
                FULL_PROJECTION,  
                Note.GUID + "= ?",
                whereArgs,
                null);
		activity.startManagingCursor(cursor);
		if (cursor == null || cursor.getCount() == 0) {
			return null;
		}
		else {
			cursor.moveToFirst();
			String noteContent = cursor.getString(cursor.getColumnIndexOrThrow(Note.NOTE_CONTENT));
			String noteTitle = cursor.getString(cursor.getColumnIndexOrThrow(Note.TITLE));
			String noteChangeDate = cursor.getString(cursor.getColumnIndexOrThrow(Note.MODIFIED_DATE));
			String noteTags = cursor.getString(cursor.getColumnIndexOrThrow(Note.TAGS));
			String noteGUID = cursor.getString(cursor.getColumnIndexOrThrow(Note.GUID));
			int noteDbid = cursor.getInt(cursor.getColumnIndexOrThrow(Note.ID));
			
			Note note = new Note();
			note.setTitle(noteTitle);
			note.setXmlContent(stripTitleFromContent(noteContent, noteTitle));
			note.setLastChangeDate(noteChangeDate);
			note.addTag(noteTags);
			note.setGuid(noteGUID);
			note.setDbId(noteDbid);
			return note;
		}
	}
	
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
			String noteChangeDate = cursor.getString(cursor.getColumnIndexOrThrow(Note.MODIFIED_DATE));
			String noteTags = cursor.getString(cursor.getColumnIndexOrThrow(Note.TAGS));
			String noteGUID = cursor.getString(cursor.getColumnIndexOrThrow(Note.GUID));
			int noteDbid = cursor.getInt(cursor.getColumnIndexOrThrow(Note.ID));
			
			note = new Note();
			note.setTitle(noteTitle);
			note.setXmlContent(stripTitleFromContent(noteContent, noteTitle));
			note.setLastChangeDate(noteChangeDate);
			note.addTag(noteTags);
			note.setGuid(noteGUID);
			note.setDbId(noteDbid);
		}
		
		return note;
	}

    // puts a note in the content provider
	// return uri
	public static Uri putNote(Activity activity, Note note) {
		
		// verify if the note is already in the content provider
		
		// TODO make the query prettier (use querybuilder)
		Uri notes = Tomdroid.CONTENT_URI;
		String[] whereArgs = new String[1];
		whereArgs[0] = note.getGuid();
		
		// The note identifier is the guid
		ContentResolver cr = activity.getContentResolver();
		Cursor managedCursor = cr.query(notes,
                LIST_PROJECTION,  
                Note.GUID + "= ?",
                whereArgs,
                null);
		activity.startManagingCursor(managedCursor);
		
		// Preparing the values to be either inserted or updated
		// depending on the result of the previous query
		ContentValues values = new ContentValues();
		values.put(Note.TITLE, note.getTitle());
		values.put(Note.FILE, note.getFileName());
		values.put(Note.GUID, note.getGuid());
		// Notice that we store the date in UTC because sqlite doesn't handle RFC3339 timezone information
		values.put(Note.MODIFIED_DATE, note.getLastChangeDate().format3339(false));
		values.put(Note.NOTE_CONTENT, note.getXmlContent());
		values.put(Note.TAGS, note.getTagsCommaSeparated());
		
		Uri uri = null;
		
		if (managedCursor == null || managedCursor.getCount() == 0) {
			
			// This note is not in the database yet we need to insert it
			TLog.v(TAG, "A new note has been detected (not yet in db)");
			
    		uri = cr.insert(Tomdroid.CONTENT_URI, values);

    		TLog.v(TAG, "Note inserted in content provider. ID: {0} TITLE:{1} GUID:{2}", uri, note.getTitle(),
                    note.getGuid());
		} else {
			
			TLog.v(TAG, "A local note has been detected (already in db)");

			cr.update(Tomdroid.CONTENT_URI, values, Note.GUID+" = ?", whereArgs); 
			
			TLog.v(TAG, "Note updated in content provider: TITLE:{0} GUID:{1}", note.getTitle(), note.getGuid());
		}
		return uri;
	}
	
	// this function just adds a "deleted" tag, to allow remote delete when syncing
	public static void deleteNote(Activity activity, Note note)
	{
		note.addTag("system:deleted");
		Time now = new Time();
		now.setToNow();
		note.setLastChangeDate(now);
		putNote(activity,note);		

	}

	// this function actually deletes the note locally, called when syncing
	public static boolean deleteNote(Activity activity, int id)
	{
		Uri uri = Uri.parse(Tomdroid.CONTENT_URI+"/"+id);

		ContentResolver cr = activity.getContentResolver();
		int result = cr.delete(uri, null, null);

        return result > 0;
	}

	// this function deletes deleted notes - if they never existed on the server, we still delete them at sync

	public static void purgeDeletedNotes(Activity activity)
	{
		// get a cursor representing all deleted notes from the NoteProvider
		Uri notes = Tomdroid.CONTENT_URI;
		String where = Note.TAGS + " LIKE '%system:deleted%'";
		ContentResolver cr = activity.getContentResolver();
		int rows = cr.delete(notes, where, null);
		TLog.v(TAG, "Deleted {0} local notes based on system:deleted tag",rows);
	}

	// this function deletes all notes - called from preferences

	public static void deleteAllNotes(Activity activity)
	{
		// get a cursor representing all deleted notes from the NoteProvider
		Uri notes = Tomdroid.CONTENT_URI;
		ContentResolver cr = activity.getContentResolver();
		int rows = cr.delete(notes, null, null);
		TLog.v(TAG, "Deleted {0} local notes",rows);
	}

	public static Cursor getAllNotes(Activity activity, Boolean includeNotebookTemplates) {
		// get a cursor representing all notes from the NoteProvider
		Uri notes = Tomdroid.CONTENT_URI;
		String where = "("+Note.TAGS + " NOT LIKE '%" + "system:deleted" + "%')";
		String orderBy;
		if (!includeNotebookTemplates) {
			where += " AND (" + Note.TAGS + " NOT LIKE '%" + "system:template" + "%')";
		}
		orderBy = Note.MODIFIED_DATE + " DESC";
		return activity.managedQuery(notes, LIST_PROJECTION, where, null, orderBy);		
	}

	// this function gets all non-deleted notes as notes in an array
	
	public static Note[] getAllNotesAsNotes(Activity activity, boolean includeNotebookTemplates) {
		Uri uri = Tomdroid.CONTENT_URI;
		String where = "("+Note.TAGS + " NOT LIKE '%" + "system:deleted" + "%')";
		String orderBy;
		if (!includeNotebookTemplates) {
			where += " AND (" + Note.TAGS + " NOT LIKE '%" + "system:template" + "%')";
		}
		orderBy = Note.MODIFIED_DATE + " DESC";
		Cursor cursor = activity.managedQuery(uri, FULL_PROJECTION, where, null, orderBy);
		if (cursor == null || cursor.getCount() == 0) {
			TLog.d(TAG, "no notes in cursor");
			return null;
		}
		TLog.d(TAG, "{0} notes in cursor",cursor.getCount());
		Note[] notes = new Note[cursor.getCount()];
		cursor.moveToFirst();
		int key = 0;

		while(!cursor.isAfterLast()) {
			String noteContent = cursor.getString(cursor.getColumnIndexOrThrow(Note.NOTE_CONTENT));
			String noteTitle = cursor.getString(cursor.getColumnIndexOrThrow(Note.TITLE));
			String noteChangeDate = cursor.getString(cursor.getColumnIndexOrThrow(Note.MODIFIED_DATE));
			String noteTags = cursor.getString(cursor.getColumnIndexOrThrow(Note.TAGS));
			String noteGUID = cursor.getString(cursor.getColumnIndexOrThrow(Note.GUID));
			int noteDbid = cursor.getInt(cursor.getColumnIndexOrThrow(Note.ID));
			
			Note note = new Note();
			note.setTitle(noteTitle);
			note.setXmlContent(stripTitleFromContent(noteContent, noteTitle));
			note.setLastChangeDate(noteChangeDate);
			note.addTag(noteTags);
			note.setGuid(noteGUID);
			note.setDbId(noteDbid);
			notes[key++] = note;
			cursor.moveToNext();
		}
		cursor.close();
		return notes;
	}	

	public static ListAdapter getListAdapterForSearchResults(Activity activity, String querys) {
        StringBuilder where = getBaseNoteQueryWhereClause();

        String[] qargs = null;
        if (querys != null ) {
			// sql statements to search notes
			String[] query = querys.split(" ");
			qargs = new String[query.length*2];
			int count = 0;
			for (String string : query) {
				qargs[count++] = "%"+string+"%"; 
				qargs[count++] = "%"+string+"%"; 
				where.append(" AND (").append(Note.TITLE).append(" LIKE ? OR ").append(Note.NOTE_CONTENT).append(" LIKE ?)");
			}	
		}

        Cursor notesCursor = activity.managedQuery(Tomdroid.CONTENT_URI, LIST_PROJECTION, where.toString(), qargs, null);
        return new NoteListAdapter(activity, notesListFromCursor(notesCursor));
	}

    private static LinkedList<Note> notesListFromCursor(Cursor notesCursor) {
        LinkedList<Note> notes = new LinkedList<Note>();
        for (notesCursor.moveToFirst(); !notesCursor.isAfterLast(); notesCursor.moveToNext()) {
            notes.add(noteFromListProjectionRow(notesCursor));
        }
        return notes;
    }

    public static ListAdapter getListAdapterForListActivity(Activity activity) {
        StringBuilder where = getBaseNoteQueryWhereClause();

        Cursor notesCursor = activity.managedQuery(Tomdroid.CONTENT_URI, LIST_PROJECTION, where.toString(), null, null);

        TreeSet<String> notebooks = new TreeSet<String>();
        List<Note> notes = new LinkedList<Note>();

        int tagsColumn = notesCursor.getColumnIndex(Note.TAGS);

        for (notesCursor.moveToFirst(); !notesCursor.isAfterLast(); notesCursor.moveToNext()) {
            Matcher matcher = NOTEBOOK_PATTERN.matcher(notesCursor.getString(tagsColumn));
            if (matcher.matches()) notebooks.add(matcher.group(1));
            else notes.add(noteFromListProjectionRow(notesCursor));
        }

        return new NoteListAdapter(activity, new LinkedList<String>(notebooks), notes);
    }

    public static ListAdapter getListAdapterForNotebookListActivity(Activity activity, String notebookName) {
        StringBuilder where = getBaseNoteQueryWhereClause();
        where.append(" AND tags LIKE '%system:notebook:").append(notebookName).append("%'");
        Cursor notesCursor = activity.managedQuery(Tomdroid.CONTENT_URI, LIST_PROJECTION, where.toString(), null, null);
        return new NoteListAdapter(activity, notesListFromCursor(notesCursor));
    }

    private static Note noteFromListProjectionRow(Cursor notesCursor) {
        int idColumn = notesCursor.getColumnIndex(Note.ID);
        int titleColumn = notesCursor.getColumnIndex(Note.TITLE);
        int modifiedColumn = notesCursor.getColumnIndex(Note.MODIFIED_DATE);
        int tagsColumn = notesCursor.getColumnIndex(Note.TAGS);

        Note note = new Note();
        note.setDbId(notesCursor.getInt(idColumn));
        note.setTitle(notesCursor.getString(titleColumn));
        note.setLastChangeDate(notesCursor.getString(modifiedColumn));
        String[] tags = notesCursor.getString(tagsColumn).split(",");
        note.setTags(new HashSet<String>(asList(tags)));
        return note;
    }

    private static StringBuilder getBaseNoteQueryWhereClause() {
        StringBuilder where = new StringBuilder("(").append(Note.TAGS).append(" NOT LIKE '%system:deleted%')");

        boolean includeNotebookTemplates = Preferences.getBoolean(Preferences.Key.INCLUDE_NOTE_TEMPLATES);
        if (!includeNotebookTemplates) {
            where.append(" AND (").append(Note.TAGS).append(" NOT LIKE '%system:template%')");
        }
        return where;
    }

    // gets the titles of the notes present in the db, used in ViewNote.buildLinkifyPattern()
	public static Cursor getTitles(Activity activity) {
		
		String where = Note.TAGS + " NOT LIKE '%system:deleted%'";
		// get a cursor containing the notes titles
		return activity.managedQuery(Tomdroid.CONTENT_URI, TITLE_PROJECTION, where, null, null);
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
			TLog.d(TAG, "Cursor returned null or 0 notes");
		}

		return id;
	}

	/**
	 * stripTitleFromContent
	 * Because of an historic oddity in Tomboy's note format, a note's title is in a <title> tag but is also repeated
	 * in the <note-content> tag. This method strips it from <note-content>.
	 */
	private static String stripTitleFromContent(String xmlContent, String title) {
		// get rid of the title that is doubled in the note's content
		// using quote to escape potential regexp chars in pattern

		Pattern stripTitle = Pattern.compile("^\\s*"+Pattern.quote(XmlUtils.escape(title))+"\\n\\n");
		Matcher m = stripTitle.matcher(xmlContent);
		if (m.find()) {
			xmlContent = xmlContent.substring(m.end(), xmlContent.length());
			TLog.d(TAG, "stripped the title from note-content");
		}

		return xmlContent;
	}

	/**
	 * getNewNotes
	 * get a guid list of notes that are newer than latest sync date
	 */
	public static Cursor getNewNotes(Activity activity) {
        return activity.managedQuery(Tomdroid.CONTENT_URI, DATE_PROJECTION, "strftime('%s', "+Note.MODIFIED_DATE+") > strftime('%s', '"+Preferences.getString(Preferences.Key.LATEST_SYNC_DATE)+"')", null, null);
	}

	/**
	 * validateNoteTitle
	 * check title against titles that exist in database, returning modified title if necessary
	 * @param activity - the calling activity
	 * @param noteTitle - the title to check
	 * @param guid - the note's guid, to avoid checking against itself
	 * @return new title
	 */
	public static String validateNoteTitle(Activity activity, String noteTitle, String guid) {

		String origTitle = noteTitle;

		// check for empty titles, set to R.string.NewNoteTitle

		if (noteTitle.replace(" ","").equals("")) {
			noteTitle = activity.getString(R.string.NewNoteTitle);
			origTitle = noteTitle; // have to set this too!
		}

		// check for duplicate titles - add number to end

		Cursor cursor = getTitles(activity);

		// cursor must not be null and must return more than 0 entry
		if (!(cursor == null || cursor.getCount() == 0)) {

			ArrayList<String> titles = new ArrayList<String>();

			cursor.moveToFirst();
			do {
				String aguid = cursor.getString(cursor.getColumnIndexOrThrow(Note.GUID));
				if(!guid.equals(aguid)) // skip this note
					titles.add(cursor.getString(cursor.getColumnIndexOrThrow(Note.TITLE)));
			} while (cursor.moveToNext());

			// sort to get {"Note","Note 2", "Note 3", ... }
			Collections.sort(titles);

			int inc = 2;
			for(String atitle : titles) {
				if(atitle.length() == 0)
					continue;

				if(atitle.equals(noteTitle)) {
					if(inc == 1)  // first match, matching "Note", set to "Note 2"
						noteTitle = noteTitle + " 2";
					else // later match, matching "Note X", set to "Note X+1"
						noteTitle = origTitle + " " + inc;
					inc++;
				}
			}
		}

		return noteTitle;
	}
}
