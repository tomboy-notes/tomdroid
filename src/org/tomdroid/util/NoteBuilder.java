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

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tomdroid.ui.Tomdroid;
import org.tomdroid.xml.NoteContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.util.Log;

public class NoteBuilder implements Runnable {
	
	public static final int PARSE_OK = 0;
	public static final int PARSE_ERROR = 1;
	
	// Metadata for the Note that will be built
	private InputSource noteContentIs;
	
	// the object being built
	private SpannableStringBuilder noteContent = new SpannableStringBuilder();
	
	private final String TAG = "NoteBuilder";
	
	// thread related
	private Thread runner;
	private Handler parentHandler;
	
	public NoteBuilder () {}
	
	public NoteBuilder setCaller(Handler parent) {
		
		parentHandler = parent;
		return this;
	}
	
	public NoteBuilder setInputSource(String nc) {
		
		//FIXME: I would pay a beer to get rid of that ugliness; I can't believe we cannot parse a partial XML tree using SAX
		// Create a valid xml document
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
		xml += "<note-content xmlns:link=\"http://beatniksoftware.com/tomboy/link\" xmlns:size=\"http://beatniksoftware.com/tomboy/size\" xmlns=\"http://beatniksoftware.com/tomboy\">";
		xml += nc;
		xml += "</note-content>";

		noteContentIs = new InputSource(new StringReader(xml));
		return this;
	}
	
	public SpannableStringBuilder build() {
		
		runner = new Thread(this);
		runner.start();		
		return noteContent;
	}
	
	public void run() {
		
		boolean successful = true;
		
		try {
			// Parsing
	    	// XML 
	    	// Get a SAXParser from the SAXPArserFactory
	        SAXParserFactory spf = SAXParserFactory.newInstance();
	        SAXParser sp = spf.newSAXParser();
	
	        // Get the XMLReader of the SAXParser we created
	        XMLReader xr = sp.getXMLReader();
	        
	        // Create a new ContentHandler, send it this note to fill and apply it to the XML-Reader
	        NoteContentHandler xmlHandler = new NoteContentHandler(noteContent);
	        xr.setContentHandler(xmlHandler);
	        
			if (Tomdroid.LOGGING_ENABLED) Log.v(TAG, "parsing note");
			xr.parse(noteContentIs);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO handle error in a more granular way
			Log.e(TAG, "There was an error parsing the note.");
			successful = false;
		}
		
		warnHandler(successful);
	}
	
    private void warnHandler(boolean successful) {
		
		// notify the main UI that we are done here (sending an ok along with the note's title)
		Message msg = Message.obtain();
		if (successful) {
			msg.what = PARSE_OK;
		} else {
			
			msg.what = PARSE_ERROR;
		}
		
		parentHandler.sendMessage(msg);
    }
}
