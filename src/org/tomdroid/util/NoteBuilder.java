/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009 Olivier Bilodeau <olivier@bottomlesspit.org>
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tomdroid.Note;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.xml.NoteHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.util.Log;

/*
 * For now, you should only use this class for a Note fetched through the network
 * TODO remove duplication between here and AsyncNoteLoaderAndParser
 * 
 */
public class NoteBuilder implements Runnable {
	
	// Metadata for the Note that will be built
	private File file;
	private URL url;
	// true means local note and false means Web note
	private Boolean noteTypeLocal; // using the Object only to be able to test against null
	
	// the object being built
	private Note note = new Note();
	
	private final String TAG = "NoteBuilder";
	
	// thread related
	private Thread runner;
	
	public NoteBuilder () {}
	
	public NoteBuilder setInputSource(File f) {
		
		file = f;
		note.setFileName(file.getAbsolutePath());
		noteTypeLocal = new Boolean(true);
		return this;
	}
	
	public NoteBuilder setInputSource(URL u) {
		
		url = u;
		noteTypeLocal = new Boolean(false);
		return this;
	}
	
	public Note build() {
		
		runner = new Thread(this);
		runner.start();		
		return note;
	}
	
	public void run() {
		
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
	        
	        if (noteTypeLocal == null) {
	        	// TODO find the proper exception to throw here.
	        	throw new IllegalArgumentException("You are not respecting NoteBuilder's contract.");
	        }
	        
	        // Create the proper input source based on if its a local note or a web note
	        InputSource is;
	        if (noteTypeLocal) {

	        	FileInputStream fin = new FileInputStream(file);
				BufferedReader in = new BufferedReader(new InputStreamReader(fin));
				is = new InputSource(in);
	        } else {
	        	is = new InputSource(new BufferedInputStream((InputStream) url.getContent()));
	        }
	        
			if (Tomdroid.LOGGING_ENABLED) Log.v(TAG, "parsing note");
			xr.parse(is);
		} catch (Exception e) {
			// TODO handle error in a more granular way
			Log.e(TAG, "There was an error parsing the note.");
		}
	}
}
