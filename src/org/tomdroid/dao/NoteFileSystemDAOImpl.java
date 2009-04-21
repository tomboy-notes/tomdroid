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
package org.tomdroid.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tomdroid.Note;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.xml.NoteHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NoteFileSystemDAOImpl implements NoteDAO {
	
	private String noteContent;
	private File file;
	private Note note;
	
	private final String TAG = "NoteFileSystemDAO";
	
	// thread related
	private Thread runner;
	private Handler parentHandler;
	
	
	public NoteFileSystemDAOImpl (Handler handler, File file, Note note) {
		parentHandler = handler;
		this.file = file;
		this.note = note;
	}
	
	
	@Override
	public void getContent() {
		runner = new Thread(this);
		runner.start();
	}
	
	@Override
	public void run() {
		Message msg = Message.obtain();
		
		// Grab the note
		// TODO handle exceptions properly
		try {
			noteContent = fetch(file);
		} catch (FileNotFoundException e) {
			// TODO handle exceptions properly
			e.printStackTrace();
		} catch (IOException e) {
			// TODO handle exceptions properly
			e.printStackTrace();
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
	        
	        if (Tomdroid.LOGGING_ENABLED) Log.v(TAG, "parsing note");
	        // Parse the xml-data from the note String and it will take care of loading the note
	        xr.parse(new InputSource(new StringReader(noteContent)));
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
		
		// notify UI that we are done here and send result 
		parentHandler.sendEmptyMessage(Note.NOTE_RECEIVED_AND_VALID);
	}	
	
	/**
	 * Grab the content at the target address and convert it to a string.
	 * @param address
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException
	 */
	private String fetch(File file) throws FileNotFoundException, IOException {
		
		
		//Init BufferedReader and StringBuilder
		FileInputStream fin = new FileInputStream(file);
		BufferedReader in = new BufferedReader(new InputStreamReader(fin));
		StringBuilder sb = new StringBuilder();

		//Convert from InputStream to String using StringBuilder
		String line = null;
		while ((line = in.readLine()) != null) {
			sb.append(line + "\n");
		}
		in.close();

		//Return the string
		return sb.toString();
	}
}
