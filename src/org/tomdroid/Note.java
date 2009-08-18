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
package org.tomdroid;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.xml.NoteContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;

public class Note {

	// Static references to fields (used in Bundles, ContentResolvers, etc.)
	public static final String ID = "_id";
	public static final String TITLE = "title";
	public static final String MODIFIED_DATE = "modified_date";
	public static final String URL = "url";
	public static final String FILE = "file";
	public static final String NOTE_CONTENT = "content";
	public static final int NOTE_RECEIVED_AND_VALID = 1;
	public static final int NO_NOTES = 2;
	public static final int NOTE_BADURL_OR_PARSING_ERROR = 3;
	public static final String[] PROJECTION = { Note.ID, Note.TITLE, Note.FILE, Note.NOTE_CONTENT, Note.MODIFIED_DATE };
	
	// Logging info
	private static final String TAG = "Note";
	
	// Notes constants
	// TODO this is a weird yellow that was usable for the android emulator, I must confirm this for real usage
	public static final int NOTE_HIGHLIGHT_COLOR = 0xFFFFFF77;
	public static final String NOTE_MONOSPACE_TYPEFACE = "monospace";
	public static final float NOTE_SIZE_SMALL_FACTOR = 0.8f;
	public static final float NOTE_SIZE_LARGE_FACTOR = 1.3f;
	public static final float NOTE_SIZE_HUGE_FACTOR = 1.6f;
	
	// Members
	private SpannableStringBuilder noteContent = new SpannableStringBuilder();
	private String xmlContent;
	private boolean xmlChanged;
	private String url;
	private String fileName;
	private String title;
	private DateTime lastChangeDate;
	private int dbId;
	
	public Note() {}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public DateTime getLastChangeDate() {
		return lastChangeDate;
	}

	public void setLastChangeDate(DateTime lastChangeDate) {
		this.lastChangeDate = lastChangeDate;
	}

	public int getDbId() {
		return dbId;
	}

	public void setDbId(int id) {
		this.dbId = id;
	}
	
	public SpannableStringBuilder getNoteContent() {
		
		// TODO: might want to do this in another thread
		// TODO: for now, regenerate the content every time for debugging purposes
		// will need to set xmlChanged to false later
		if (xmlChanged)
			updateContent();
		
		return noteContent;
	}
	
	private void updateContent() {
		
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
	        
	        // Create a valid xml document
	        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
	        xml += "<note-content xmlns:link=\"http://beatniksoftware.com/tomboy/link\" xmlns:size=\"http://beatniksoftware.com/tomboy/size\" xmlns=\"http://beatniksoftware.com/tomboy\">";
	        xml += xmlContent;
	        xml += "</note-content>";
	        
	        // Create the proper input source based on if its a local note or a web note
        	StringReader str = new StringReader(xml);
			InputSource is = new InputSource(str);
	        
			if (Tomdroid.LOGGING_ENABLED) Log.v(TAG, "Parsing note content.");
			xr.parse(is);
		
		// TODO wrap and throw a new exception here
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getXmlContent() {
		return xmlContent;
	}
	
	public void setXmlContent(String xmlContent) {
		this.xmlContent = xmlContent;
		xmlChanged = true;
	}

	public SpannableStringBuilder getDisplayableNoteContent() {
		SpannableStringBuilder sNoteContent = new SpannableStringBuilder(getNoteContent());
		
		sNoteContent.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 17, 35, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return sNoteContent;
	}

	@Override
	public String toString() {
		// format date time according to XML standard
		DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
		return new String("Note: "+ getTitle() + " (" + fmt.print(getLastChangeDate()) + ")");
	}
	
}
