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

import org.openintents.intents.AboutIntents;
import org.tomdroid.Note;
import org.tomdroid.NoteCollectingService;
import org.tomdroid.NoteCollection;
import org.tomdroid.NoteProvider;
import org.tomdroid.R;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class Tomdroid extends ListActivity {

	// Global definition for Tomdroid
	public static final String AUTHORITY = "org.tomdroid.notes";
	public static final Uri	CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notes");
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.tomdroid.note";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.tomdroid.note";

	// config parameters
	// TODO hardcoded for now
	public static final String NOTES_PATH = "/sdcard/tomdroid/";
	// Logging should be disabled for release builds
	public static final boolean LOGGING_ENABLED = true;

	// Logging info
	public static final String TAG = "Tomdroid";
	
	// data keys
	public static final String RESULT_URL_TO_LOAD = "urlToLoad"; 

	// Activity result resources
	private static final int ACTIVITY_GET_URL=0;
	private static final int ACTIVITY_VIEW=1;
	
	// domain elements
	private NoteCollection localNotes;
	
	// UI to data model glue
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
        
        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(CONTENT_URI);
        }

        
        // Perform a managed query. The Activity will handle closing and requerying the cursor
        // when needed.
        Cursor cursor = managedQuery(getIntent().getData(), NoteProvider.PROJECTION, null, null,
                NoteProvider.DEFAULT_SORT_ORDER);

        // Used to map notes entries from the database to views
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.main_list_item, cursor,
                new String[] { Note.TITLE }, new int[] { android.R.id.text1 });
        setListAdapter(adapter);

        // set the view shown when the list is empty
        listEmptyView = (TextView)findViewById(R.id.list_empty);
        getListView().setEmptyView(listEmptyView);
        
        // start loading local notes
        if (LOGGING_ENABLED) Log.v(TAG, "Loading local notes");
		File notesRoot = new File(Tomdroid.NOTES_PATH);
		if (!notesRoot.exists()) {
			new AlertDialog.Builder(this)
			.setMessage("Tomdroid notes folder doesn't exist. It is configured to be at: "+Tomdroid.NOTES_PATH)
			.setTitle("Error")
			.setNeutralButton("Ok", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}})
			.show();
		}
		startService(new Intent(this, NoteCollectingService.class));
		localNotes = NoteCollection.getInstance();
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
/*        
	        case R.id.menuClose:
	        	// closing everything then closing itself
	        	finishActivity(ACTIVITY_VIEW);
	        	finish();
	        	return true;*/
	        
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
	
	/**
	 * Show an about dialog for this application.
	 */
	protected void showAboutDialog() {
		Intent intent = new Intent(AboutIntents.ACTION_SHOW_ABOUT_DIALOG);
		
		// Start about activity. Needs to be "forResult" with requestCode>=0
		// so that the package name is passed properly.
		//
		// The details are obtained from the Manifest through
		// default tags and metadata.
		try{
			startActivityForResult(intent, 0);
		}catch(ActivityNotFoundException e){
			try{
				//FlurryAgent.onError("LCA:showAboutDialog1", getString(R.string.about_backup), "Toast");
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_about_dialog))));
			}catch(ActivityNotFoundException e2){
				//FlurryAgent.onError("LCA:showAboutDialog2", getString(R.string.about_backup), "Toast");
				Toast.makeText(this, getString(R.string.market_backup), Toast.LENGTH_LONG).show();
			}
		}
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

		// get the clicked note
		Note n =  localNotes.findNoteFromDbId(id);
		
		Intent i = new Intent(Tomdroid.this, ViewNote.class);
		i.putExtra(Note.FILE, n.getFileName());
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
