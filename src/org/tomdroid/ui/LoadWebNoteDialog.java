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

import org.tomdroid.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * This class is in charge of returning only the string of the URL to fetch from.
 */
public class LoadWebNoteDialog extends Activity {
	
	// UI elements
	private EditText txtURL;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.load_web_note_dialog);
        
        // Connect UI elements to variables
        txtURL = (EditText) findViewById(R.id.txtURL);
        txtURL.setText("http://www.bottomlesspit.org/files/note.xml");
        Button btnOk = (Button)findViewById(R.id.btnOk);
        Button btnCancel = (Button)findViewById(R.id.btnCancel);
        
        // Annon inner-class for button listener 
        btnOk.setOnClickListener(new OnClickListener() {
        	
            public void onClick(View v)
            {
                Log.d(LoadWebNoteDialog.this.toString(), "info: Button Ok clicked. URL requested: "+txtURL.getText().toString());
                okClicked(txtURL.getText().toString());
            }
        });

        // Annon inner-class for button listener 
        btnCancel.setOnClickListener(new OnClickListener() {
        	
            public void onClick(View v)
            {
                Log.d(LoadWebNoteDialog.this.toString(), "info: Button cancel clicked.");
                cancelClicked();
            }
        });

        
	}
	
	private void okClicked(String url) {
		
		Bundle bundle = new Bundle();
		bundle.putString(Tomdroid.RESULT_URL_TO_LOAD, url);
		
		Intent i = new Intent();
		i.putExtras(bundle);
		setResult(RESULT_OK, i);
		finish();		
	}

	private void cancelClicked() {

		setResult(RESULT_CANCELED);
		finish();
	}
	
}
