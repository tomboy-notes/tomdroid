/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2012, 2010, 2011, 2012 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2012, 2013 Stefan Hammer <j.4@gmx.at>
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.sd.NoteHandler;
import org.tomdroid.ui.CompareNotes;
import org.tomdroid.ui.EditNote;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.ui.actionbar.ActionBarActivity;
import org.tomdroid.xml.NoteContentHandler;
import org.tomdroid.xml.NoteXMLContentBuilder;
import org.tomdroid.xml.XmlUtils;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.util.TimeFormatException;
import android.widget.Toast;

public class Receive extends ActionBarActivity {
	
	// Logging info
	private static final String TAG = "ReceiveActivity";

	// don't import files bigger than this 
	private long MAX_FILE_SIZE = 1048576; // 1MB 

	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init preferences
		Preferences.init(this, Tomdroid.CLEAR_PREFERENCES);

		// set intent, action, MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();

		TLog.v(TAG, "Receiving note of type {0}",type);
		TLog.d(TAG, "Action type: {0}",action);
	    
		if (intent.getData() != null) {
    		TLog.d(TAG, "Receiving file from path: {0}",intent.getData().getPath());
			File file = new File(intent.getData().getPath());

			if(file.length() > MAX_FILE_SIZE ) {
	    		Toast.makeText(this, getString(R.string.messageFileTooBig), Toast.LENGTH_SHORT).show();
				finish();
			}
			else {
				
				final char[] buffer = new char[0x1000];
				
				// Try reading the file first
				String contents = "";
				try {
					
					// read as file
					contents = readFile(file,buffer);
					useSendFile(file, contents);
				} catch (IOException e) {
					try {
						
						// if previous fails, read as input stream
						InputStream input = getContentResolver().openInputStream(intent.getData());
						contents = readFile(file,buffer, input);
						useSendFile(file, contents);
						
					} catch (IOException e1) {
						
						// if both fails, print stacktrace, make user warning and exit
						e1.printStackTrace();
						e.printStackTrace();
						TLog.w(TAG, "Something went wrong trying to read the note");
						Toast.makeText(this, getString(R.string.messageFileNotReadable), Toast.LENGTH_SHORT).show();
						finish();
					}
				}
			}
    	}
    	else if (Intent.ACTION_SEND.equals(action) && type != null && "text/plain".equals(type)) {
    		TLog.v(TAG, "receiving note as plain text");
    	    String sharedContent = intent.getStringExtra(Intent.EXTRA_TEXT);
    	    String sharedTitle = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            useSendText(sharedContent, sharedTitle); // use the text being sent
        }
    	else {
    		TLog.v(TAG, "received invalid note");
			finish();
    	}
	}
	void useSendFile(File file, String contents) {
		Note remoteNote = new Note();

		if(contents.startsWith("<?xml")) { // xml note file
			
			try {
				// Parsing
		    	// XML 
		    	// Get a SAXParser from the SAXPArserFactory
		        SAXParserFactory spf = SAXParserFactory.newInstance();
		        SAXParser sp = spf.newSAXParser();
		
		        // Get the XMLReader of the SAXParser we created
		        XMLReader xr = sp.getXMLReader();
	
		        // Create a new ContentHandler, send it this note to fill and apply it to the XML-Reader
		        NoteHandler xmlHandler = new NoteHandler(remoteNote);
		        xr.setContentHandler(xmlHandler);
	
		        // Create the proper input source
		        StringReader sr = new StringReader(contents);
		        InputSource is = new InputSource(sr);
		        
				TLog.d(TAG, "parsing note");
				xr.parse(is);
	
			// TODO wrap and throw a new exception here
			} catch (Exception e) {
				e.printStackTrace();
				if(e instanceof TimeFormatException) TLog.e(TAG, "Problem parsing the note's date and time");
				finish();
			}
			// the note guid is not stored in the xml but in the filename
			remoteNote.setGuid(file.getName().replace(".note", ""));
			if (remoteNote.getGuid().toString().equals("RAW"))
				remoteNote.setGuid(UUID.randomUUID().toString());
			Pattern note_content = Pattern.compile("<note-content[^>]+>(.*)<\\/note-content>", Pattern.CASE_INSENSITIVE+Pattern.DOTALL);

			// FIXME here we are re-reading the whole note just to grab note-content out, there is probably a better way to do this (I'm talking to you xmlpull.org!)
			Matcher m = note_content.matcher(contents);
			if (m.find()) {
				remoteNote.setXmlContent(NoteManager.stripTitleFromContent(m.group(1),remoteNote.getTitle()));
			} else {
				TLog.w(TAG, "Something went wrong trying to grab the note-content out of a note");
				return;
			}
		}
		else { // ordinary text file
			remoteNote = NewNote.createNewNote(this, file.getName().replaceFirst("\\.[^.]+$", ""), XmlUtils.escape(contents));
		}

		remoteNote.setFileName(file.getAbsolutePath());

		// check and see if the note already exists; if so, send to conflict resolver
		Note localNote = NoteManager.getNoteByGuid(this, remoteNote.getGuid()); 
		
		if(localNote != null) {
			int compareBoth = Time.compare(localNote.getLastChangeDate(), remoteNote.getLastChangeDate());
			
			TLog.v(TAG, "note conflict... showing resolution dialog TITLE:{0} GUID:{1}", localNote.getTitle(), localNote.getGuid());
			
			// send everything to Tomdroid so it can show Sync Dialog
			
		    Bundle bundle = new Bundle();	
			bundle.putString("title",remoteNote.getTitle());
			bundle.putString("file",remoteNote.getFileName());
			bundle.putString("guid",remoteNote.getGuid());
			bundle.putString("date",remoteNote.getLastChangeDate().formatTomboy());
			bundle.putString("content", remoteNote.getXmlContent());
			bundle.putString("tags", remoteNote.getTags());
			bundle.putInt("datediff", compareBoth);
			bundle.putBoolean("noRemote", true);
			
			Intent cintent = new Intent(getApplicationContext(), CompareNotes.class);	
			cintent.putExtras(bundle);
	
			startActivityForResult(cintent, 0);
			return;
		}
		
		// note doesn't exist, just give it a new title if necessary and set changedate to now
		remoteNote.setTitle(NoteManager.validateNoteTitle(this, remoteNote.getTitle(), remoteNote.getGuid()));
		remoteNote.setLastChangeDate();
		
    	// add to content provider
		Uri uri = NoteManager.putNote(this, remoteNote);
		
		// view new note
		Intent i = new Intent(Intent.ACTION_VIEW, uri, this, Tomdroid.class);
		i.putExtra("view_note", true);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		finish();		
	}

	void useSendText(String sharedContent, String sharedTitle) {
	    
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
	
	private String readFile(File file, char[] buffer) throws IOException {
		return readFile(file, buffer, new FileInputStream(file));
	}
	
	
	private String readFile(File file, char[] buffer, InputStream input) throws IOException {
		StringBuilder out = new StringBuilder();
		
		int read;
		Reader reader = new InputStreamReader(input, "UTF-8");
		
		do {
		  read = reader.read(buffer, 0, buffer.length);
		  if (read > 0) {
		    out.append(buffer, 0, read);
		  }
		}
		while (read >= 0);
		
		reader.close();
		return out.toString();
	}
	protected void  onActivityResult (int requestCode, int resultCode, Intent data) {
		TLog.d(TAG, "onActivityResult called");
		Uri uri = null;
		if(data != null && data.hasExtra("uri"))
			uri = Uri.parse(data.getStringExtra("uri"));
		
		// view new note
		Intent i = new Intent(Intent.ACTION_VIEW, uri, this, Tomdroid.class);
		if (uri != null) {
			i.putExtra("view_note", true);
		}
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		finish();
	}
}
