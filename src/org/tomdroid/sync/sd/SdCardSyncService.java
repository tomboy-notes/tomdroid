/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, 2010, 2011 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2010, Rodja Trappe <mail@rodja.net>
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
package org.tomdroid.sync.sd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tomdroid.Note;
import org.tomdroid.sync.SyncService;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.ErrorList;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.util.TimeFormatException;

public class SdCardSyncService extends SyncService {
	
	private int numberOfFilesToSync = 0;
	
	// regexp for <note-content..>...</note-content>
	private static Pattern note_content = Pattern.compile("<note-content[^>]+>(.*)<\\/note-content>", Pattern.CASE_INSENSITIVE+Pattern.DOTALL);
	
	// logging related
	private final static String TAG = "SdCardSyncService";
	
	public SdCardSyncService(Activity activity, Handler handler) {
		super(activity, handler);
	}
	
	@Override
	public String getDescription() {
		return "SD Card";
	}

	@Override
	public String getName() {
		return "sdcard";
	}

	@Override
	public boolean needsServer() {
		return false;
	}
	
	@Override
	public boolean needsAuth() {
		return false;
	}

	@Override
	protected void sync() {

		setSyncProgress(0);

		// start loading local notes
		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG, "Loading local notes");
		
		File path = new File(Tomdroid.NOTES_PATH);
		
		if (!path.exists())
			path.mkdir();
		
		Log.i(TAG, "Path "+path+" exists: "+path.exists());
		
		// Check a second time, if not the most likely cause is the volume doesn't exist
		if(!path.exists()) {
			if (Tomdroid.LOGGING_ENABLED) Log.w(TAG, "Couldn't create "+path);
			sendMessage(NO_SD_CARD);
			setSyncProgress(100);
			return;
		}
		
		File[] fileList = path.listFiles(new NotesFilter());
		numberOfFilesToSync  = fileList.length;
		
		// If there are no notes, warn the UI through an empty message
		if (fileList == null || fileList.length == 0) {
			if (Tomdroid.LOGGING_ENABLED) Log.i(TAG, "There are no notes in "+path);
			sendMessage(PARSING_NO_NOTES);
			setSyncProgress(100);
			return;
		}
		
		// every but the last note
		for(int i = 0; i < fileList.length-1; i++) {
			// TODO better progress reporting from within the workers
			
			// give a filename to a thread and ask to parse it
			syncInThread(new Worker(fileList[i], false));
        }
		
		// last task, warn it so it'll warn UI when done
		syncInThread(new Worker(fileList[fileList.length-1], true));
	}
	
	/**
	 * Simple filename filter that grabs files ending with .note
	 * TODO move into its own static class in a util package
	 */
	private class NotesFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return (name.endsWith(".note"));
		}
	}
	
	/**
	 * The worker spawns a new note, parse the file its being given by the executor.
	 */
	// TODO change type to callable to be able to throw exceptions? (if you throw make sure to display an alert only once)
	// http://java.sun.com/j2se/1.5.0/docs/api/java/util/concurrent/Callable.html
	private class Worker implements Runnable {
		
		// the note to be loaded and parsed
		private Note note = new Note();
		private File file;
		private boolean isLast;
		final char[] buffer = new char[0x1000];
		
		public Worker(File f, boolean isLast) {
			file = f;
			this.isLast = isLast;
		}

		public void run() {
			
			note.setFileName(file.getAbsolutePath());
			// the note guid is not stored in the xml but in the filename
			note.setGuid(file.getName().replace(".note", ""));
			
			// Try reading the file first
			String contents = "";
			try {
				contents = readFile();
			} catch (IOException e) {
				e.printStackTrace();
				if (Tomdroid.LOGGING_ENABLED) Log.w(TAG, "Something went wrong trying to read the note");
				sendMessage(PARSING_FAILED, ErrorList.createError(note, e));
				onWorkDone();
				return;
			}

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

		        // Create the proper input source
		        StringReader sr = new StringReader(contents);
		        InputSource is = new InputSource(sr);
		        
				if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "parsing note");
				xr.parse(is);

			// TODO wrap and throw a new exception here
			} catch (Exception e) {
				e.printStackTrace();
				if(e instanceof TimeFormatException) Log.e(TAG, "Problem parsing the note's date and time");
				sendMessage(PARSING_FAILED, ErrorList.createErrorWithContents(note, e, contents));
				onWorkDone();
				return;
			}

			// extract the <note-content..>...</note-content>
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "retrieving what is inside of note-content");
			
			// FIXME here we are re-reading the whole note just to grab note-content out, there is probably a best way to do this (I'm talking to you xmlpull.org!)
			Matcher m = note_content.matcher(contents);
			if (m.find()) {
				note.setXmlContent(m.group(1));
			} else {
				if (Tomdroid.LOGGING_ENABLED) Log.w(TAG, "Something went wrong trying to grab the note-content out of a note");
				sendMessage(PARSING_FAILED, ErrorList.createErrorWithContents(note, "Something went wrong trying to grab the note-content out of a note", contents));
				onWorkDone();
				return;
			}
			
			insertNote(note);
			onWorkDone();
		}
		
		private String readFile() throws IOException {
			StringBuilder out = new StringBuilder();
			
			int read;
			Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
			
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
		
		private void onWorkDone(){
			if (isLast) {
				setSyncProgress(100);
				sendMessage(PARSING_COMPLETE);
			}
			else
				setSyncProgress((int) (getSyncProgress() + 100.0 / numberOfFilesToSync));			
		}
	}
}
