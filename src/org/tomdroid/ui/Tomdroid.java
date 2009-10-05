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

import java.io.File;
import java.io.FileNotFoundException;

import org.tomdroid.Note;
import org.tomdroid.R;
import org.tomdroid.util.AsyncNoteLoaderAndParser;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class Tomdroid extends ListActivity {

	// Global definition for Tomdroid
	public static final String AUTHORITY = "org.tomdroid.notes";
	public static final Uri	CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notes");
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.tomdroid.note";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.tomdroid.note";
    public static final String PROJECT_HOMEPAGE = "http://www.launchpad.net/tomdroid/";
	
	// config parameters
	// TODO hardcoded for now
	public static final String NOTES_PATH = "/sdcard/tomdroid/";
	// Logging should be disabled for release builds
	public static final boolean LOGGING_ENABLED = true;

	// Logging info
	private static final String TAG = "Tomdroid";
	
	// data keys
	public static final String RESULT_URL_TO_LOAD = "urlToLoad"; 

	// Activity result resources
	private static final int ACTIVITY_GET_URL=0;
	private static final int ACTIVITY_VIEW=1;
	
	// UI to data model glue
	private Cursor notesCursor;
	private TextView listEmptyView;
	
	// Bundle keys for saving state
	private static final String WARNING_SHOWN = "w";
	
	// State variables
	private boolean warningShown = false;

	
    /** Called when the activity is created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        // did we already show the warning and got destroyed by android's activity killer?
        if (savedInstanceState == null || !savedInstanceState.getBoolean(WARNING_SHOWN)) {

        	// Warn that this is a "will eat your babies" release 
			new AlertDialog.Builder(this)
				.setMessage(getString(R.string.strWelcome))
				.setTitle("Warning")
				.setNeutralButton("Ok", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						warningShown = true;
						dialog.dismiss();
					}})
				.setIcon(R.drawable.icon)
				.show();
        }
        
		// get a cursor representing all notes from the NoteProvider
		String[] projection = new String[] { Note.ID, Note.TITLE };
		Uri notes = CONTENT_URI;
		notesCursor = managedQuery(notes, projection, null, null, null);
		
		// set up an adapter binding the TITLE field of the cursor to the list item
		String[] from = new String[] { Note.TITLE };
		int[] to = new int[] { R.id.note_title };
		SimpleCursorAdapter adapter =
			new SimpleCursorAdapter(this, R.layout.main_list_item, notesCursor, from, to);
        
		setListAdapter(adapter);

        // set the view shown when the list is empty
        listEmptyView = (TextView)findViewById(R.id.list_empty);
        getListView().setEmptyView(listEmptyView);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Create the menu based on what is defined in res/menu/main.xml
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.menuLoadWebNote:
	            showLoadWebNoteDialog();
	            return true;
	            
	        case R.id.menuSyncWithSD:

	        	// start loading local notes
                if (LOGGING_ENABLED) Log.v(TAG, "Loading local notes");
	        	try {
	        		File notesRoot = new File(Tomdroid.NOTES_PATH);
	        		
	        		if (!notesRoot.exists()) {
	        			throw new FileNotFoundException("Tomdroid notes folder doesn't exist. It is configured to be at: "+Tomdroid.NOTES_PATH);
	        		}
	        		
	        		AsyncNoteLoaderAndParser asyncLoader = new AsyncNoteLoaderAndParser(notesRoot, this);
	        		asyncLoader.readAndParseNotes();
	        		
	    		} catch (FileNotFoundException e) {
	    			//TODO put strings in an external resource
	    			listEmptyView.setText(R.string.strListEmptyNoNotes);
	    			new AlertDialog.Builder(this)
	    				.setMessage(e.getMessage())
	    				.setTitle("Error")
	    				.setNeutralButton("Ok", new OnClickListener() {
	    					public void onClick(DialogInterface dialog, int which) {
	    						dialog.dismiss();
	    					}})
	    				.show();
	    			e.printStackTrace();
	    		}
	        	
	        	return true;
	        
	        case R.id.menuAbout:
				showAboutDialog();
	        	return true;
        }
        
        return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// saving the state of the warning dialog
		if (warningShown) {
			outState.putBoolean(WARNING_SHOWN, true);
		}
	}

	private void showAboutDialog() {
		
		// grab version info
		String ver;
		try {
			ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			ver = "Not found!";
		}
		
		// format the string
		String aboutDialogFormat = getString(R.string.strAbout);
		String aboutDialogStr = String.format(aboutDialogFormat, 
				getString(R.string.app_desc), 	// App description
				getString(R.string.author), 	// Author name
				ver							// Version
				);
		
		// build and show the dialog
		new AlertDialog.Builder(this)
			.setMessage(aboutDialogStr)
			.setTitle("About Tomdroid")
			.setIcon(R.drawable.icon)
			.setNegativeButton("Project page", new OnClickListener() {
				public void onClick(DialogInterface dialog,	int which) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Tomdroid.PROJECT_HOMEPAGE)));
					dialog.dismiss();
				}})
			.setPositiveButton("Ok", new OnClickListener() {
				public void onClick(DialogInterface dialog,	int which) {
					dialog.dismiss();
				}})
			.show();
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

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		// get the clicked note id
		notesCursor.moveToPosition(position);
		int noteId = notesCursor.getInt(
						notesCursor.getColumnIndexOrThrow(Note.ID));
		
		Uri intentUri = Uri.parse(Tomdroid.CONTENT_URI+"/"+noteId);
		Intent i = new Intent(Intent.ACTION_VIEW, intentUri, this, ViewNote.class);
		startActivityForResult(i, ACTIVITY_VIEW);
	}
	
	private void showLoadWebNoteDialog() {
		
    	Intent i = new Intent(Tomdroid.this, LoadWebNoteDialog.class);
    	startActivityForResult(i, ACTIVITY_GET_URL);
	}

	private void loadNoteFromURL(String url) {
		
    	Intent i = new Intent(Tomdroid.this, ViewNote.class);
        i.putExtra(Note.URL, url);
        startActivity(i);
	}
}
