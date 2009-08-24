/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009 Olivier Bilodeau <olivier@bottomlesspit.org>
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tomdroid.Note;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.xml.NoteHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class AsyncNoteLoaderAndParser {
	private final ExecutorService pool;
	private final static int poolSize = 1;
	private File path;
	private Activity activity;
	
	// logging related
	private final static String TAG = "AsyncNoteLoaderAndParser";
	
	public AsyncNoteLoaderAndParser(File path, Activity activity) {
		this.path = path;
		pool = Executors.newFixedThreadPool(poolSize);
		this.activity = activity;
	}

	public void readAndParseNotes() {
		File[] fileList = path.listFiles(new NotesFilter());
		
		for (File file : fileList) {

			// give a filename to a thread and ask to parse it when nothing's left to do its over
			pool.execute(new Worker(file));
        }
	}
	
	/**
	 * Simple filename filter that grabs files ending with .note
	 * TODO move into its own static class in a util package
	 */
	class NotesFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return (name.endsWith(".note"));
		}
	}
	
	/**
	 * The worker spawns a new note, parse the file its being given by the executor.
	 */
	class Worker implements Runnable {
		
		// the note to be loaded and parsed
		private Note note = new Note();
		private File file;
		
		public Worker(File f) {
			file = f;
		}

		public void run() {
			
			note.setFileName(file.getAbsolutePath());
			// the note guid is not stored in the xml but in the filename
			note.setGuid(file.getName().replace(".note", ""));
			
			try {
				// Parsing
		    	// XML 
		    	// Get a SAXParser from the SAXPArserFactory
		        SAXParserFactory spf = SAXParserFactory.newInstance();
		        SAXParser sp = spf.newSAXParser();
		
		        // Get the XMLReader of the SAXParser we created
		        XMLReader xr = sp.getXMLReader();
		        
		        // Create a new ContentHandler, send it this note to fill and apply it to the XML-Reader
		        NoteHandler xmlHandler = new NoteHandler(note);
		        xr.setContentHandler(xmlHandler);

		        
		        // Create the proper input source based on if its a local note or a web note
	        	FileInputStream fin = new FileInputStream(file);
				BufferedReader in = new BufferedReader(new InputStreamReader(fin), 8192);
				InputSource is = new InputSource(in);
		        
				if (Tomdroid.LOGGING_ENABLED) Log.v(TAG, "parsing note");
				xr.parse(is);
			
			// TODO wrap and throw a new exception here
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			insertNote(note);
		}
	}
	
	private void insertNote(Note note) {
		
		// verify if the note is already in the content provider
		String[] projection = new String[] {
			    Note.ID,
			    Note.TITLE,
			};
		
		// TODO make the query prettier (use querybuilder)
		Uri notes = Tomdroid.CONTENT_URI;
		String[] whereArgs = new String[1];
		whereArgs[0] = note.getGuid().toString();
		
		ContentResolver cr = activity.getContentResolver();
		Cursor managedCursor = cr.query(notes,
                projection,  
                Note.GUID + "= ?",
                whereArgs,
                null);
		activity.startManagingCursor(managedCursor);
		
		// Preparing the values to be either inserted or updated
		// depending on the result of the previous query
		// TODO PoC code that should be removed in next iteration's refactoring (no notecollection, everything should come from the provider I guess?)
		ContentValues values = new ContentValues();
		values.put(Note.TITLE, note.getTitle());
		values.put(Note.FILE, note.getFileName());
		values.put(Note.GUID, note.getGuid().toString());
		values.put(Note.NOTE_CONTENT, note.getXmlContent());
		
		if (managedCursor.getCount() == 0) {
			
			// This note is not in the database yet we need to insert it
			if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"A new note has been detected (not yet in db)");
			
    		Uri uri = cr.insert(Tomdroid.CONTENT_URI, values);

    		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Note inserted in content provider. ID: "+uri+" TITLE:"+note.getTitle()+" GUID:"+note.getGuid());
		} else {
			
			// Overwrite the previous note if it exists
			cr.update(Tomdroid.CONTENT_URI, values, Note.GUID+" = ?", whereArgs);
			
			if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Note updated in content provider. TITLE:"+note.getTitle()+" GUID:"+note.getGuid());
		}
	}
}
