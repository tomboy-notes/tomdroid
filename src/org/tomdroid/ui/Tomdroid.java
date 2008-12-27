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
package org.tomdroid.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.tomdroid.Note;
import org.tomdroid.NoteCollection;
import org.tomdroid.R;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Tomdroid extends ListActivity {

	// config parameters
	// TODO hardcoded for now
	public static final String NOTES_PATH = "/sdcard/tomdroid/";
	
	// data keys
	public static final String RESULT_URL_TO_LOAD = "urlToLoad"; 

	// Activity result resources
	private static final int ACTIVITY_GET_URL=0;
	private static final int ACTIVITY_VIEW=1;
	
	private final static int MENU_FROMWEB = Menu.FIRST;
	
	// domain elements
	private NoteCollection localNotes;
	
	// Notes names for list in UI
	private List<String> notesNamesList = new ArrayList<String>();

	
    /** Called when the activity is created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        // start loading local notes
        Log.i(Tomdroid.this.toString(), "Loading local notes");
		localNotes = new NoteCollection();
		localNotes.loadNotes(handler);
     
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_FROMWEB, 0, R.string.menuLoadWebNote);
		
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_FROMWEB:
            createLoadWebNoteDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(requestCode) {
			case ACTIVITY_GET_URL:
				String url = data.getExtras().getString(RESULT_URL_TO_LOAD);
				loadNoteFromURL(url);

		}
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

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(this.toString(),"Position: " + position + " id:" + id + " Note file:" + localNotes.getNotes().get(position).getFileName());
		
		
		Intent i = new Intent(Tomdroid.this, ViewNote.class);
		i.putExtra(Note.FILE, Tomdroid.NOTES_PATH+localNotes.getNotes().get(position).getFileName());
		startActivityForResult(i, ACTIVITY_VIEW);

	}
	
	private void createLoadWebNoteDialog() {
		
		Log.i(Tomdroid.this.toString(), "info: Menu item chosen -  Loading load web note dialog");
    	Intent i = new Intent(Tomdroid.this, LoadWebNoteDialog.class);
    	startActivityForResult(i, ACTIVITY_GET_URL);
	}

	private void loadNoteFromURL(String url) {
		
    	Intent i = new Intent(Tomdroid.this, ViewNote.class);
        i.putExtra(Note.URL, url);
        startActivity(i);
	}
	
	private void updateNoteList() {
		notesNamesList.clear();
		
		// TODO this is not efficient but I have to make it work now..
		Iterator<Note> i = localNotes.getNotes().iterator();
		while(i.hasNext()) {
			Note curNote = i.next();
			if (curNote.getTitle() != null) {
				notesNamesList.add(curNote.getTitle());
			}
		}
		
		// listAdapter
		ArrayAdapter<String> notesListAdapter = new ArrayAdapter<String>(this, R.layout.main_list_item, notesNamesList);
        setListAdapter(notesListAdapter);
	}
}
