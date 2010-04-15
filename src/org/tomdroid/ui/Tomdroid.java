/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
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
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.util.AsyncNoteLoaderAndParser;
import org.tomdroid.util.Send;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

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
	public static final boolean LOGGING_ENABLED = false;

	// Logging info
	private static final String TAG = "Tomdroid";
	
	// UI to data model glue
	private TextView listEmptyView;
	private ListAdapter adapter;
	private ListView listView;
	
	// Bundle keys for saving state
	private static final String WARNING_SHOWN = "w";
	
	// State variables
	private boolean warningShown = false;
	private boolean parsingErrorShown = false;

	
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
        
        // adapter that binds the ListView UI to the notes in the note manager
        adapter = NoteManager.getListAdapter(this);
		setListAdapter(adapter);

        // set the view shown when the list is empty
		// TODO default empty-list text is butt-ugly!
        listEmptyView = (TextView)findViewById(R.id.list_empty);
        getListView().setEmptyView(listEmptyView);
        listView = (ListView)findViewById(android.R.id.list);
        registerForContextMenu(listView);
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
	        case R.id.menuSyncWithSD:

	        	// start loading local notes
                if (LOGGING_ENABLED) Log.v(TAG, "Loading local notes");
        		// reset parsing error flag
        		parsingErrorShown = false;

            	try {
            		File notesRoot = new File(Tomdroid.NOTES_PATH);
        		
            		if (!notesRoot.exists()) {
        			throw new FileNotFoundException("Tomdroid notes folder doesn't exist. It is configured to be at: "+Tomdroid.NOTES_PATH);
            		}
        		
	        		AsyncNoteLoaderAndParser asyncLoader = new AsyncNoteLoaderAndParser(this, notesRoot);
	        		asyncLoader.readAndParseNotes(handler);
        		
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
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_longclick, menu);
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		long noteId=info.id;
		Uri intentUri = Uri.parse(Tomdroid.CONTENT_URI+"/"+noteId);
		
		switch (item.getItemId()) {
		case R.id.menu_send:
			Send.sendNote(intentUri, handler, this);
			break;
			
		case R.id.menu_delete:
			getContentResolver().delete(Tomdroid.CONTENT_URI, Note.ID+"="+noteId, null);
			break;
			
		default:
			break;
		}
		return super.onContextItemSelected(item);
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
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		Cursor item = (Cursor)adapter.getItem(position);
		int noteId = item.getInt(item.getColumnIndexOrThrow(Note.ID));
		
		Uri intentUri = Uri.parse(Tomdroid.CONTENT_URI+"/"+noteId);
		Intent i = new Intent(Intent.ACTION_VIEW, intentUri, this, ViewNote.class);
		startActivity(i);
	}
	
	private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

        	switch(msg.what) {
        	case AsyncNoteLoaderAndParser.PARSING_COMPLETE:
        		// TODO put string in a translatable bundle
        		Toast.makeText(getApplicationContext(),
        				"Synchronization with SD Card is complete.",
        				Toast.LENGTH_SHORT)
        				.show();
        		break;
        		
        	case AsyncNoteLoaderAndParser.PARSING_NO_NOTES:
    			// TODO put string in a translatable bundle
    			Toast.makeText(getApplicationContext(),
    					"There are no files in tomdroid/ on the sdcard.",
    					Toast.LENGTH_SHORT)
    					.show();
    			break;

        	case AsyncNoteLoaderAndParser.PARSING_FAILED:
        		if (Tomdroid.LOGGING_ENABLED) Log.w(TAG,"handler called with a parsing failed message");
        		
        		// if we already shown a parsing error in this pass, we won't show it again
        		if (!parsingErrorShown) {
	        		parsingErrorShown = true;

	        		// TODO put error string in a translatable resource
					new AlertDialog.Builder(Tomdroid.this)
						.setMessage("There was an error trying to parse your note collection. If " +
								    "you are able to replicate the problem, please contact us!")
						.setTitle("Error")
						.setNeutralButton("Ok", new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}})
						.show();
        		}
        		break;
        		
        	default:
        		if (Tomdroid.LOGGING_ENABLED) Log.i(TAG,"handler called with an unknown message");
        		break;
        	
        	}
        }
    };
}
