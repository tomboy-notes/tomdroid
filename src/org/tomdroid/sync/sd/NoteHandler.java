/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
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

import org.tomdroid.Note;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.TimeFormatException;

public class NoteHandler extends DefaultHandler {

	private String TAG = "NoteHandler";

	// position keepers
	private boolean inTitleTag = false;
	private boolean inLastChangeDateTag = false;
	private boolean inNoteContentTag = false;
	private boolean inCreateDateTag = false;
	private boolean inCursorTag = false;
	private boolean inWidthTag = false;
	private boolean inHeightTag = false;
	private boolean inXTag = false;
	private boolean inYTag = false;
	private boolean inTagTag = false;

	// -- Tomboy's notes XML tags names --
	private final static String TITLE = "title";
	private final static String LAST_CHANGE_DATE = "last-change-date";
	private final static String NOTE_CONTENT = "note-content";
	private final static String CREATE_DATE = "create-date";
	private final static String NOTE_C = "cursor-position";
	private final static String NOTE_W = "width";
	private final static String NOTE_H = "height";
	private final static String NOTE_X = "x";
	private final static String NOTE_Y = "y";
	private final static String NOTE_TAG = "tag";
	
	// Buffers for parsed elements
	private StringBuilder title = new StringBuilder();
	private StringBuilder lastChangeDate = new StringBuilder();
	private StringBuilder noteContent = new StringBuilder();
	private StringBuilder createDate = new StringBuilder();
	private StringBuilder cursorPos = new StringBuilder();
	private StringBuilder width = new StringBuilder();
	private StringBuilder height = new StringBuilder();
	private StringBuilder X = new StringBuilder();
	private StringBuilder Y = new StringBuilder();
	private StringBuilder tag = new StringBuilder();
	
	// link to model 
	private Note note;

	
	public NoteHandler(Note note) {
		this.note = note;
	}
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		
		// TODO validate top-level tag for tomboy notes and throw exception if its the wrong version number (maybe offer to parse also?)		

		if (localName.equals(TITLE)) {
			inTitleTag = true;
		} 
		else if (localName.equals(LAST_CHANGE_DATE)) {
			inLastChangeDateTag = true;
		}
		else if (localName.equals(NOTE_CONTENT)) {
			inNoteContentTag = true;
		}
		else if (localName.equals(CREATE_DATE)) {
			inCreateDateTag = true;
		}
		else if (localName.equals(NOTE_C)) {
			inCursorTag = true;
		}
		else if (localName.equals(NOTE_W)) {
			inWidthTag = true;
		}
		else if (localName.equals(NOTE_H)) {
			inHeightTag = true;
		}
		else if (localName.equals(NOTE_X)) {
			inXTag = true;
		}
		else if (localName.equals(NOTE_Y)) {
			inYTag = true;
		}
		else if (localName.equals(NOTE_TAG)) {
			inTagTag = true;
		}
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException, TimeFormatException {

		if (localName.equals(TITLE)) {
			inTitleTag = false;
			note.setTitle(title.toString());
		} 
		else if (localName.equals(LAST_CHANGE_DATE)) {
			inLastChangeDateTag = false;
			note.setLastChangeDate(lastChangeDate.toString());
		}
		else if (localName.equals(NOTE_CONTENT)) {
			inNoteContentTag = false;
			note.setXmlContent(noteContent.toString());
		}
		else if (localName.equals(CREATE_DATE)) {
			inCreateDateTag = false;
			if(createDate.length() > 0)
				note.setCreateDate(createDate.toString());
		}
		else if (localName.equals(NOTE_C)) {
			inCursorTag = false;
			if(cursorPos.length() > 0)
				note.cursorPos = Integer.parseInt(cursorPos.toString());
		}
		else if (localName.equals(NOTE_W)) {
			inWidthTag = false;
			if(width.length() > 0)
				note.width = Integer.parseInt(width.toString());
		}
		else if (localName.equals(NOTE_H)) {
			inHeightTag = false;
			if(height.length() > 0)
				note.height = Integer.parseInt(height.toString());
		}
		else if (localName.equals(NOTE_X)) {
			inXTag = false;
			if(X.length() > 0)
				note.X = Integer.parseInt(X.toString());
		}
		else if (localName.equals(NOTE_Y)) {
			inYTag = false;
			if(Y.length() > 0)
				note.Y = Integer.parseInt(Y.toString());
		}
		else if (localName.equals(NOTE_TAG)) {
			inTagTag = false;
			if(tag.length() > 0)
				note.addTag(tag.toString());
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		
		if (inTitleTag) {
			title.append(ch, start, length);
		} 
		else if (inLastChangeDateTag) {
			lastChangeDate.append(ch, start, length);
		} 
		else if (inNoteContentTag) {
			noteContent.append(ch, start, length);
		}
		else if (inCreateDateTag) {
			createDate.append(ch, start, length);
		}
		else if (inCursorTag) {
			cursorPos.append(ch, start, length);
		}
		else if (inWidthTag) {
			width.append(ch, start, length);
		}
		else if (inHeightTag) {
			height.append(ch, start, length);
		}
		else if (inXTag) {
			X.append(ch, start, length);
		}
		else if (inYTag) {
			Y.append(ch, start, length);
		}
		else if (inTagTag) {
			tag.append(ch, start, length);
		}
	}
}
