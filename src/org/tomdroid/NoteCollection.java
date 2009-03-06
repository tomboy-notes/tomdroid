/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008 Olivier Bilodeau <olivier@bottomlesspit.org>
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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.tomdroid.ui.Tomdroid;

import android.os.Handler;
import android.util.Log;

// TODO Transform the NoteCollection into a Provider (see .../android-sdk-linux_x86-1.0_r1/docs/devel/data/contentproviders.html#creatingacontentprovider)
// this would be more android-like
public class NoteCollection {

	// TODO This is not efficient, I maintain two list, one for the UI and the other for the actual data 
	// the collection of notes
	private List<Note> notes = new ArrayList<Note>();
	
	public List<Note> getNotes() {
		return notes;
	}

	public void setNotes(List<Note> notes) {
		this.notes = notes;
	}

	public void addNote(Note note) {
		notes.add(note);
	}
	
	// TODO there is most likely a better way to do this
	// TODO how does Tomboy deals with notes with duplicate titles? I have to check that out
	public Note findNoteFromTitle(String title) {
		Log.i(this.toString(),"searching for note title "+title);
		Iterator<Note> i = notes.iterator();
		while(i.hasNext()) {
			Note curNote = i.next();
			if (curNote.getTitle().equalsIgnoreCase(title)) {
				return curNote;
			}
		}
		return null;
	}
	
	// TODO there is most likely a better way to do this
	public Note findNoteFromFilename(String filename) {
		Log.i(this.toString(),"searching for note file "+filename);
		Iterator<Note> i = notes.iterator();
		while(i.hasNext()) {
			Note curNote = i.next();
			if (curNote.getFileName().equals(filename)) {
				return curNote;
			}
		}
		return null;
	}
	
	public void loadNotes(Handler hndl) {
		// TODO crash more cleanly if sdcard is not loaded or there is no files in tomdroid/
		File notesRoot = new File(Tomdroid.NOTES_PATH);
		for (File file : notesRoot.listFiles(new NotesFilter())) {

			Note note = new Note(hndl, file);
			
			note.fetchAndParseNoteFromFileSystemAsync();
			notes.add(note);
        }
	}
	
	/**
	 * Builds a regular expression pattern that will match any of the note title currently in the collection.
	 * Useful for the Linkify to create the links to the notes.
	 * @return regexp pattern
	 */
	public Pattern buildNoteLinkifyPattern()  {
		
		StringBuilder sb = new StringBuilder();
		
		// for each note 
		//sb.append("("+note.getT..+")");
		
		// TODO I will need to espace every regexp special char in note titles here
		// check for Pattern.quote
		for (Note n : notes) {
			// Pattern.quote() here make sure that special characters in the note's title are properly escaped 
			sb.append("("+Pattern.quote(n.getTitle())+")|");
		}
		
		// get rid of the last | that is not needed
		String pt = sb.substring(0, sb.length()-1);

		// return a compiled match pattern
		return Pattern.compile(pt);
	}
	
	/**
	 * Simple filename filter that grabs files ending with .note
	 * TODO move into its own static class in a util package
	 */
	class NotesFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return (name.endsWith(".note"));
		}
	}

	// singleton pattern
	// TODO verify this singleton, I have no net access and I'm not quite sure I nailed it
	private static NoteCollection nc;
	
	// FIXME the contract provided by this singleton is not correct. 
	// If we instantiate this singleton, we expect it to be able to search through notes, which would not be the case
	// since the loadNotes is called by Tomdroid and not the constructor or something else.
	public static NoteCollection getInstance() {
		if (nc == null) {
			nc = new NoteCollection();
		}
		return nc;
	}


}
