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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.tomdroid.dao.NoteDAO;
import org.tomdroid.dao.NoteFileSystemDAOImpl;
import org.tomdroid.dao.NoteNetworkDAOImpl;
import org.tomdroid.xml.NoteHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;

public class Note {

	// Static references to fields (used in Bundles)
	public static final String URL = "url";
	public static final String FILE = "file";
	public static final String NOTE_CONTENT = "note-content";
	public static final int NOTE_RECEIVED_AND_VALID = 1;
	
	// Notes constants
	// TODO this is a weird yellow that was usable for the android emulator, I must confirm this for real usage
	public static final int NOTE_HIGHLIGHT_COLOR = 0xFFFFFF77;
	public static final String NOTE_MONOSPACE_TYPEFACE = "monospace";
	public static final float NOTE_SIZE_SMALL_FACTOR = 0.8f;
	public static final float NOTE_SIZE_LARGE_FACTOR = 1.3f;
	public static final float NOTE_SIZE_HUGE_FACTOR = 1.6f;
	
	// Members
	private SpannableStringBuilder noteContent = new SpannableStringBuilder();
	private String url;
	private String fileName;
	private File file;
	private String title;
	private DateTime lastChangeDate;
	
	// Handles async state
	private Handler parentHandler;
	
	// TODO is this still useful as of iteration3?
	public Note(Handler hdl, String url) {
		this.parentHandler = hdl;
		this.url = url;
	}
	
	public Note(Handler hdl, File file) {
		this.parentHandler = hdl;
		this.file = file;
		this.fileName = file.getName();
	}
	
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

	/**
	 * Asynchronously get the note from URL
	 */
	public void getNoteFromWebAsync() {
		
		//  TODO my naive way of using mock objects
		//NotesDAOImpl notesDAO = new NotesDAOImpl(handler, noteURL);
		NoteNetworkDAOImpl notesDAO = new NoteNetworkDAOImpl(handler, url);

		// asynchronous call to get the note's content
		notesDAO.getContent();
	}
	
	/**
	 * Asynchronously get the note from file system
	 */
	public void getNoteFromFileSystemAsync() {
		
		NoteFileSystemDAOImpl notesDAO = new NoteFileSystemDAOImpl(handler, file);

		// asynchronous call to get the note's content
		notesDAO.getContent();
	}
	
	public SpannableStringBuilder getNoteContent() {
		return noteContent;
	}

	public void setNoteContent(SpannableStringBuilder noteContent) {
		this.noteContent = noteContent;
	}

	public SpannableStringBuilder getDisplayableNoteContent() {
		SpannableStringBuilder sNoteContent = new SpannableStringBuilder(getNoteContent());
		
		sNoteContent.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 17, 35, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return sNoteContent;
	}
	
	// TODO I don't know if this double handler thingy is efficient but it was (for me) the more maintainable 
	// way of doing this. When I'll know more about Android, I should come back to this
    private Handler handler = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {
        	
        	String noteStr = msg.getData().getString(NoteDAO.NOTE);
        	Log.i(this.toString(), "Note handler triggered.");
        	
        	// TODO eeuuhhhh, see buildNote()'s todo regarding exceptions..
        	try {
				buildNote(noteStr);
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	warnHandler();
		}
    };
    
    // TODO I should not throw but handle or wrap exceptions here, I am being lazy I guess
    private void buildNote(String noteStream) throws ParserConfigurationException, SAXException, IOException {
    	
    	// XML 
    	// Get a SAXParser from the SAXPArserFactory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();

        // Get the XMLReader of the SAXParser we created
        XMLReader xr = sp.getXMLReader();
        
        // Create a new ContentHandler, send it this note to fill and apply it to the XML-Reader
        NoteHandler xmlHandler = new NoteHandler(this);
        xr.setContentHandler(xmlHandler);
        
        Log.d(this.toString(), "about to parse a note");
        // Parse the xml-data from the note String and it will take care of loading the note
        xr.parse(new InputSource(new StringReader(noteStream)));
        Log.d(this.toString(), "note parsed");
    }
    
    private void warnHandler() {
		
		Log.i(this.toString(), "warnHandler: sending ok to NoteView");
		
		// notify UI that we are done here and sending an ok 
		parentHandler.sendEmptyMessage(NOTE_RECEIVED_AND_VALID);

    }

	@Override
	public String toString() {
		// format date time according to XML standard
		DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
		return new String("Note: "+ getTitle() + " (" + fmt.print(getLastChangeDate()) + ")");
	}
	
}
