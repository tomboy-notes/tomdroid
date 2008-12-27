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
import java.util.List;

import org.tomdroid.ui.Tomdroid;

import android.os.Handler;

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
	
	// TODO implement
//	public Note getNoteFromTitle(String title) {
//	
//	}
	
	public void loadNotes(Handler hndl) {
		// TODO crash more cleanly if sdcard is not loaded or there is no files in tomdroid/
		File notesRoot = new File(Tomdroid.NOTES_PATH);
		for (File file : notesRoot.listFiles(new NotesFilter())) {

			Note note = new Note(hndl, file);
			
			note.fetchNoteFromFileSystemAsync();
			notes.add(note);
        }
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
}
