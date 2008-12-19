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

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class NoteList extends ListActivity {
	
	private static final int ACTIVITY_VIEW=0;
	
	// TODO hardcoded for now
	private static final String NOTES_PATH = "/sdcard/tomdroid/";
	
	// TODO This is not efficient, I maintain two list, one for the UI and the other for the actual data 
	// the collection of notes
	private List<Note> notes = new ArrayList<Note>();
	
	// Notes names for list in UI
	List<String> notesNamesList = new ArrayList<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// load UI
		setContentView(R.layout.note_list);

        // start loading local notes
		loadNotes();
	}
	
	public void loadNotes() {
		// TODO crash more cleanly if sdcard is not loaded or there is no files in tomdroid/
		File notesRoot = new File(NOTES_PATH);
		for (File file : notesRoot.listFiles(new NotesFilter())) {

			Note note = new Note(handler, file);
			// FIXME this is not a good name since its confusing between getter / setters
			note.getNoteFromFileSystemAsync();
			notes.add(note);
        }
		
	}
	
	private void updateNoteList() {
		notesNamesList.clear();
		
		// TODO this is not efficient but I have to make it work now..
		Iterator<Note> i = notes.iterator();
		while(i.hasNext()) {
			Note curNote = i.next();
			if (curNote.getTitle() != null) {
				notesNamesList.add(curNote.getTitle());
			}
		}
		
		// listAdapter
		ArrayAdapter<String> notesListAdapter = new ArrayAdapter<String>(this, R.layout.note_list_item, notesNamesList);
        setListAdapter(notesListAdapter);
	}
	
    private Handler handler = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {
        	
        	// thread is done fetching a note and parsing went well 
        	if (msg.what == Note.NOTE_RECEIVED_AND_VALID) {
        		
        		updateNoteList();
        	}
		}

    };
	
	/**
	 * Simple filename filter that grabs files ending with .note  
	 */
	class NotesFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return (name.endsWith(".note"));
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(this.toString(),"Position: " + position + " id:" + id + " Note file:" + notes.get(position).getFileName());
		
		
		Intent i = new Intent(NoteList.this, NoteView.class);
		i.putExtra(Note.FILE, NOTES_PATH+notes.get(position).getFileName());
		startActivityForResult(i, ACTIVITY_VIEW);

	}
}
