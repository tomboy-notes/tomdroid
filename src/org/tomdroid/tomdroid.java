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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class tomdroid extends Activity {
	private static final int ACTIVITY_VIEW=0;
	
	// UI elements
	private EditText txtURL;
	
    /** Called when the activity is created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        // Connect UI elements to variables
        txtURL = (EditText) findViewById(R.id.txtURL);
        txtURL.setText("http://www.bottomlesspit.org/files/note.xml");
        Button button = (Button)findViewById(R.id.btnURL);
        
        // Annon inner-class for button listener 
        button.setOnClickListener(new OnClickListener() {
        	
            public void onClick(View v)
            {
                Log.i(tomdroid.this.toString(), "info: Button clicked. URL requested: "+txtURL.getText().toString());
            	
            	Intent i = new Intent(tomdroid.this, NoteView.class);
                i.putExtra(Note.URL, txtURL.getText().toString());
                startActivityForResult(i, ACTIVITY_VIEW);
            }
        });
    }
    

}