/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2012, 2010, 2011, 2012 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2012 Stefan Hammer <j.4@gmx.at>
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

package org.tomdroid.util;

import java.io.StringReader;
import java.util.UUID;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.ui.EditNote;
import org.tomdroid.ui.actionbar.ActionBarActivity;
import org.tomdroid.xml.NoteContentHandler;
import org.xml.sax.InputSource;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;

public class Receive extends ActionBarActivity {
	
	// Logging info
	private static final String TAG = "ReceiveActivity";

	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    // set intent, action, MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();

	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	        if ("text/plain".equals(type)) {
	            useSendText(intent); // use the text being sent
	        }
	    }
	}

	void useSendText(Intent intent) {
	    String sharedContent = intent.getStringExtra(Intent.EXTRA_TEXT);
	    String sharedTitle = intent.getStringExtra(Intent.EXTRA_SUBJECT);
	    
	    if (sharedContent != null) {
			// parse XML
			SpannableStringBuilder newNoteContent = new SpannableStringBuilder();
			
			String xmlContent = "<note-content version=\"1.0\">"+sharedContent+"</note-content>";
	        InputSource noteContentIs = new InputSource(new StringReader(xmlContent));
			try {
				// Parsing
		    	// XML 
		    	// Get a SAXParser from the SAXPArserFactory
		        SAXParserFactory spf = SAXParserFactory.newInstance();

		        // trashing the namespaces but keep prefixes (since we don't have the xml header)
		        spf.setFeature("http://xml.org/sax/features/namespaces", false);
		        spf.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		        SAXParser sp = spf.newSAXParser();

		        sp.parse(noteContentIs, new NoteContentHandler(newNoteContent));
			} catch (Exception e) {
				e.printStackTrace();
				// TODO handle error in a more granular way
				TLog.e(TAG, "There was an error parsing the note {0}", sharedTitle);
			}
			// store changed note content
			String newXmlContent = new NoteXMLContentBuilder().setCaller(noteXMLWriteHandler).setInputSource(newNoteContent).build();
			// validate title (duplicates, empty,...)
			String validTitle = NoteManager.validateNoteTitle(this, sharedTitle, UUID.randomUUID().toString());
			
	    	// add a new note
			Note note = NewNote.createNewNote(this, validTitle, newXmlContent);
			Uri uri = NoteManager.putNote(this, note);
			
			// view new note
			Intent i = new Intent(Intent.ACTION_VIEW, uri, this, EditNote.class);
			startActivity(i);
			finish();
	    }
	}
	
	private Handler noteXMLWriteHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			//parsed ok - do nothing
			if(msg.what == NoteXMLContentBuilder.PARSE_OK) {
			//parsed not ok - error
			} else if(msg.what == NoteXMLContentBuilder.PARSE_ERROR) {
				
				// TODO put this String in a translatable resource
				new AlertDialog.Builder(Receive.this)
					.setMessage("The requested note could not be parsed. If you see this error " +
								" and you are able to replicate it, please file a bug!")
					.setTitle("Error")
					.setNeutralButton("Ok", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}})
					.show();
        	}
		}
	};
}
