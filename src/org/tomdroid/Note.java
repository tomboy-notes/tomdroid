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

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tomdroid.dao.NotesDAO;
import org.tomdroid.dao.mock.NotesDAOMock;
import org.tomdroid.util.xml.NoteHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Note {

	// Static references to fields (used in Bundles)
	public static final String URL = "url";
	public static final String NOTE_CONTENT = "note-content";
	
	// Members
	private String noteContent;
	private String note;
	private String noteURL;
	
	// Handles async state
	private Handler parentHandler;
	
	public Note(Handler hdl, String url) {
		this.parentHandler = hdl;
		this.noteURL = url;
	}
	
	/**
	 * Asynchronously get the note from URL
	 */
	public void fetchNoteAsync() {
		
		//  TODO my naive way of using mock objects
		//NotesDAOImpl notesDAO = new NotesDAOImpl(handler, noteURL);
		NotesDAOMock notesDAO = new NotesDAOMock(handler, noteURL);

		// asynchronous call to get the note's content
		notesDAO.getContent();
	}
	
	public String getAndroidCompatibleNoteContent() {
		return note;
	}

	public String getNoteContent() {
		return noteContent;
	}

	public void setNoteContent(String noteContent) {
		this.noteContent = noteContent;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
	
	// TODO I don't know if this double handler thingy is efficient but it was (for me) the more maintainable 
	// way of doing this. When I'll know more about Android, I should come back to this
    private Handler handler = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {
        	
        	String noteStr = msg.getData().getString(NotesDAO.NOTE);
        	Log.i(this.toString(), "Note handler triggered. Content:" + noteStr);
        	
        	// TODO eeuuhhhh, see buildNote()'s TODO regarding exceptions..
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
    	//TODO this will have to properly build the note, splitting metadata and content et al.
    	note = noteStream;
    	
    	// XML 
    	// Get a SAXParser from the SAXPArserFactory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();

        // Get the XMLReader of the SAXParser we created
        XMLReader xr = sp.getXMLReader();
        
        // Create a new ContentHandler, send it this note to fill and apply it to the XML-Reader
        NoteHandler xmlHandler = new NoteHandler(this);
        xr.setContentHandler(xmlHandler);
        
        // Parse the xml-data from the note String and it will take care of loading the note
        xr.parse(new InputSource(new StringReader(noteStream)));
    }
    
    private void warnHandler() {
		Message msg = Message.obtain();
		
		Log.i(this.toString(), "warnHandler: sending to handler: " + noteContent);

		// Load the message object with the note
		Bundle bundle = new Bundle();
		bundle.putString(Note.NOTE_CONTENT, noteContent);
		msg.setData(bundle);
		
		// notify UI that we are done here and send result 
		parentHandler.sendMessage(msg);

    }
	
}
