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

import org.tomdroid.dao.NotesDAO;
import org.tomdroid.dao.NotesDAOImpl;
import org.tomdroid.dao.mock.NotesDAOMock;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

public class NoteView extends Activity {
	
	private String url; 
	
	// UI elements
	private TextView content;
	
	// Data Access Objects
	private NotesDAO notesDAO;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.note_view);
		
		content = (TextView) findViewById(R.id.content);
		
		// get url to fetch from Intent
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			url = extras.getString(Note.URL);
		} else {
			Log.i(this.toString(), "info: Bundle was empty.");
		}
		
		if (url != null) {
			notesDAO = new NotesDAOImpl(handler, url);

			// asynchronous call to get the note's content
			notesDAO.getContent();
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
        	// thread is done and msg contains note 
			content.setText(msg.getData().getString(NotesDAO.NOTE));
		}
    };

}
