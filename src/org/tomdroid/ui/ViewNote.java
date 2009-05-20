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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;

import org.tomdroid.Note;
import org.tomdroid.NoteCollection;
import org.tomdroid.R;
import org.tomdroid.util.NoteBuilder;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

// TODO this class is starting to smell
public class ViewNote extends Activity {
	
	private String url;
	private String file;
	
	// UI elements
	private TextView content;
	
	// Model objects
	private Note note;
	
	// Logging info
	private static final String TAG = "ViewNote";
	
	// TODO extract methods in here
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.note_view);
		
		content = (TextView) findViewById(R.id.content);
		
		final Intent intent = getIntent();
		
		Uri uri = intent.getData();
		if (uri == null) {
			
			// we were not fired by an Intent-filter so two choice here:
			// get external web url or filename
			Bundle extras = intent.getExtras();
			if (extras != null) {

				// lets grab both variables and test against null later
				url = extras.getString(Note.URL);
				file = extras.getString(Note.FILE);
				
				// Based on what was sent in the bundle, we either load from file or url
				if (url != null) {

					if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"ViewNote started: Loading a note from Web URL.");
					
					try {

						note = new NoteBuilder().setCaller(handler).setInputSource(new URL(url)).build();
					} catch (MalformedURLException e) {
						// TODO catch correctly
						e.printStackTrace();
					}

				} else if (file != null) {

					if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"ViewNote started: Loading a note based on a filename.");
					note = NoteCollection.getInstance().findNoteFromFilename(file);
					showNote();
				} else {
					
					if (Tomdroid.LOGGING_ENABLED) Log.d(TAG,"ViewNote started: Bundle's content was not helpful to find which note to load..");
				}
			} else {
				if (Tomdroid.LOGGING_ENABLED) Log.d(TAG,"ViewNote started: No extra information in the bundle, we don't know what to load");
			}
		} else {
			
			// We were triggered by an Intent URI 
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "ViewNote started: Intent-filter triggered.");

			// TODO validate the good action?
			// intent.getAction()
			
			// can we find a matching note?
			Cursor cursor = managedQuery(uri, Note.PROJECTION, null, null, null);
			// cursor must not be null and must return more than 0 entry 
			if (!(cursor == null || cursor.getCount() == 0)) {
				
				cursor.moveToFirst();
				String noteFilename = cursor.getString(cursor.getColumnIndexOrThrow(Note.FILE));
				note = NoteCollection.getInstance().findNoteFromFilename(noteFilename);
				showNote();
				
			} else {
				
				// TODO send an error to the user
				if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "Cursor returned null or 0 notes");
			}
		}
	}
	
	// TODO add a menu that switches the view to an EditText instead of TextView
	// this will need some other quit mechanism as onKeyDown though.. (but the back key might do it)
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		super.onKeyDown(keyCode, event);
		
		finish();
		
		return true;
	}

    private void showNote() {
		// show the note (spannable makes the TextView able to output styled text)
		content.setText(note.getNoteContent(), TextView.BufferType.SPANNABLE);
		
		// add links to stuff that is understood by Android
		// TODO this is SLOWWWW!!!!
		Linkify.addLinks(content, Linkify.ALL);
		
		// This will create a link every time a note title is found in the text.
		// The pattern is built by NoteCollection and contains a very dumb (title1)|(title2) escaped correctly
		// Then we tranform the url from the note name to the note id to avoid characters that mess up with the URI (ex: ?)
		Linkify.addLinks(content, 
						 NoteCollection.getInstance().buildNoteLinkifyPattern(),
						 Tomdroid.CONTENT_URI+"/",
						 null,
						 
						 // custom transform filter that takes the note's title part of the URI and translate it into the note id
						 // this was done to avoid problems with invalid characters in URI (ex: ? is the query separator but could be in a note title)
						 new TransformFilter() {

							@Override
							public String transformUrl(Matcher m, String str) {

								// FIXME if this activity is called from another app and Tomdroid was never launched, getting here will probably make it crash
								int id = 0;
								try {
									id = NoteCollection.getInstance().findNoteFromTitle(str).getDbId();
								} catch (Exception e) {
									if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "NoteCollection accessed but it was not ready for it..");
								}
								
								// return something like content://org.tomdroid.notes/notes/3
								return Tomdroid.CONTENT_URI.toString()+"/"+id;
							}  
						});
	}

	private Handler handler = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {
        	
        	// thread is done fetching note and parsing went well 
        	if (msg.what == Note.NOTE_RECEIVED_AND_VALID) {
	        	showNote();
        	}
		}
    };
}
