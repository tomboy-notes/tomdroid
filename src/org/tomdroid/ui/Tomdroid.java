/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009 Olivier Bilodeau <olivier@bottomlesspit.org>
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

import java.io.FileNotFoundException;

import org.tomdroid.Note;
import org.tomdroid.NoteCollection;
import org.tomdroid.R;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Tomdroid extends ListActivity {

	// Global definition for Tomdroid
	public static final String AUTHORITY = "org.tomdroid.notes";
	public static final Uri	CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notes");
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.tomdroid.note";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.tomdroid.note";
	
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
	
	// UI to data model glue
	private ArrayAdapter<String> notesListAdapter;
	private TextView listEmptyView;

	
    /** Called when the activity is created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
	    // listAdapter that binds the UI to the notes names
		notesListAdapter = new ArrayAdapter<String>(this, R.layout.main_list_item);
        setListAdapter(notesListAdapter);

        // set the view shown when the list is empty
        listEmptyView = (TextView)findViewById(R.id.list_empty);
        getListView().setEmptyView(listEmptyView);
        
        // start loading local notes
        Log.i(Tomdroid.this.toString(), "Loading local notes");
		localNotes = NoteCollection.getInstance();
		try {
			localNotes.loadNotes(handler);
		} catch (FileNotFoundException e) {
			//TODO put strings in ressource
			listEmptyView.setText(R.string.strListEmptyNoNotes);
			new AlertDialog.Builder(this).setMessage(e.getMessage())
										 .setTitle("Error")
										 .setNeutralButton("Ok", new OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog,
																int which) {
												dialog.dismiss();
											}})
										 .show();
			e.printStackTrace();
		}
     
		// if there are no notes, say so in the list_empty message
		if (localNotes.isEmpty()) {
			listEmptyView.setText(R.string.strListEmptyNoNotes);
		}
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		
		// TODO this is ugly, fix it!
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
				if (resultCode == RESULT_OK) {
					String url = data.getExtras().getString(RESULT_URL_TO_LOAD);
					loadNoteFromURL(url);
				}
		}
	}
	
    private Handler handler = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {
        	
        	// thread is done fetching a note and parsing went well 
        	if (msg.what == Note.NOTE_RECEIVED_AND_VALID) {

        		// update the note list with this newly parsed note
        		updateNoteListWith(msg.getData().getString(Note.TITLE));
        	}
		}

    };

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		// get the clicked note
		Note n =  localNotes.findNoteFromTitle(notesListAdapter.getItem(position));
		
		Log.d(this.toString(),"Menu clicked. Position: " + position + " id:" + id + " note:" + n.getTitle());
		
		Intent i = new Intent(Tomdroid.this, ViewNote.class);
		i.putExtra(Note.FILE, n.getFileName());
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
	
	private void updateNoteListWith(String noteTitle) {
		
		// add note to the note list
		notesListAdapter.add(noteTitle);

		// get the note instance we will work with that instead  from now on
		Note note = localNotes.findNoteFromTitle(noteTitle);
		
		// verify if the note is already in the content provider
		String[] projection = new String[] {
			    Note.ID,
			    Note.TITLE,
			};

		// TODO I could see a problem where someone delete a note and recreate one with the same title.
		// It would been seen as not new although it is (it will have a new filename)
		Uri notes = Tomdroid.CONTENT_URI;
		Cursor managedCursor = managedQuery( notes,
                projection,  
                Note.TITLE + "='" + noteTitle + "'",       // TODO build proper where clause
                null,
                Note.TITLE + " ASC");
		if (managedCursor.getCount() == 0) {
			
			// This note is not in the database yet we need to insert it
			Log.i(this.toString(),"A new note has been detected (not yet in db)");

			// This add the note to the content Provider
			// TODO PoC code that should be removed in next iteration's refactoring (no notecollection, everything should come from the provider I guess?)
    		ContentValues values = new ContentValues();
    		values.put(Note.TITLE, note.getTitle());
    		values.put(Note.FILE, note.getFileName());
    		Uri uri = getContentResolver().insert(CONTENT_URI, values);
    		// now that we inserted the note put its ID in the note itself
    		note.setDbId(Integer.parseInt(uri.getLastPathSegment()));

    		Log.i(this.toString(),"Note inserted in content provider. ID: "+uri+" TITLE:"+noteTitle+" ID:"+note.getDbId());
		} else {
			
			// find out the note's id and put it in the note
		    if (managedCursor.moveToFirst()) {
		        int idColumn = managedCursor.getColumnIndex(Note.ID);
	            note.setDbId(managedCursor.getInt(idColumn));
		    }
		    
			// note already in database
			Log.i(this.toString(),"Note '" + noteTitle + "' was already in the database. Id:" + note.getDbId());
		}
	}
}
