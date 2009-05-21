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
import org.tomdroid.NoteCollection;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.xml.NoteHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AsyncNoteLoaderAndParser {
	private final ExecutorService pool;
	private final static int poolSize = 3;
	private File path;
	private NoteCollection noteCollection;
	private Handler parentHandler;
	
	// logging related
	private final static String TAG = "AsyncNoteLoaderAndParser";
	
	public AsyncNoteLoaderAndParser(File path, NoteCollection nc, Handler hndl) {
		this.path = path;
		pool = Executors.newFixedThreadPool(poolSize);
		noteCollection = nc;
		parentHandler = hndl;
	}

	public void readAndParseNotes() {
		File[] fileList = path.listFiles(new NotesFilter());
		
		// If there are no notes, warn the UI through an empty message
		if (fileList.length == 0) {
			parentHandler.sendEmptyMessage(Note.NO_NOTES);
		}
		
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
	 * The worker spawns a new note, parse the file its being given by the executor and add it to the NoteCollection
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
				BufferedReader in = new BufferedReader(new InputStreamReader(fin));
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
			
			synchronized (noteCollection) {
				noteCollection.addNote(note);
			}
			
			// notify UI that we are done here and send result 
			warnHandler();
		}
		
	    private void warnHandler() {
			
			// notify the main UI that we are done here (sending an ok along with the note's title)
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putString(Note.TITLE, note.getTitle());
			msg.setData(bundle);
			msg.what = Note.NOTE_RECEIVED_AND_VALID;
			
			parentHandler.sendMessage(msg);
	    }
		
	}
}
