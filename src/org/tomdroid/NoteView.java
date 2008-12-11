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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

// TODO this class is starting to smell
public class NoteView extends Activity {
	
	private String url;
	private String file;
	
	// UI elements
	private TextView content;
	
	// Model objects
	private Note note;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.note_view);
		
		content = (TextView) findViewById(R.id.content);
		
		// get url or file to fetch from Intent
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			url = extras.getString(Note.URL);
			file = extras.getString(Note.FILE);
		} else {
			Log.i(this.toString(), "info: Bundle was empty.");
		}
		
		// Based on what is sent in the bundle, we either load from file or url
		if (url != null) {
			note = new Note(handler, url);
			
			// asynchronous call to fetch the note, the callback with come from the handler
			note.getNoteFromWebAsync();
		} else if (file != null) {
			note = new Note(handler, new File(file));
			
			note.getNoteFromFileSystemAsync();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		super.onKeyDown(keyCode, event);
		
		finish();
		
		return true;
	}

    private Handler handler = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {
        	
        	// thread is done fetching note and parsing went well 
        	if (msg.what == Note.NOTE_RECEIVED_AND_VALID) {
	        	// show the note (spannable makes the TextView able to output styled text)
				content.setText(note.getNoteContent(), TextView.BufferType.SPANNABLE);
				
				// add links to stuff that is understood by Android
				// TODO this is SLOWWWW!!!!
				Linkify.addLinks(content, Linkify.ALL);
        	}
		}
    };

}
