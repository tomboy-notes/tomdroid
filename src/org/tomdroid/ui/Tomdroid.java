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

import org.tomdroid.Note;
import org.tomdroid.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Tomdroid extends Activity {

	// config parameters
	// TODO hardcoded for now
	public static final String NOTES_PATH = "/sdcard/tomdroid/";

	// UI elements
	private EditText txtURL;
	
	private final static int MENU_FROMWEB = Menu.FIRST;
	
    /** Called when the activity is created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        // Connect UI elements to variables
        txtURL = (EditText) findViewById(R.id.txtURL);
        txtURL.setText("http://www.bottomlesspit.org/files/note.xml");
        Button webBtn = (Button)findViewById(R.id.btnURL);
        
        // Annon inner-class for button listener 
        webBtn.setOnClickListener(new OnClickListener() {
        	
            public void onClick(View v)
            {
                Log.i(Tomdroid.this.toString(), "info: Button clicked. URL requested: "+txtURL.getText().toString());
            	
            	Intent i = new Intent(Tomdroid.this, ViewNote.class);
                i.putExtra(Note.URL, txtURL.getText().toString());
                startActivity(i);
            }
        });
        
        Button localBtn = (Button)findViewById(R.id.btnList);
        
        // Annon inner-class for button listener 
        localBtn.setOnClickListener(new OnClickListener() {
        	
            public void onClick(View v)
            {
                Log.i(Tomdroid.this.toString(), "info: Button clicked. Loading local notes");
            	
            	Intent i = new Intent(Tomdroid.this, ListNotes.class);
                startActivity(i);
            }
        });        
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

	private void createLoadWebNoteDialog() {
		
		Log.i(Tomdroid.this.toString(), "info: Menu item chosen -  Loading load web note dialog");
    	Intent i = new Intent(Tomdroid.this, LoadWebNoteDialog.class);
    	startActivity(i);
	}
}
