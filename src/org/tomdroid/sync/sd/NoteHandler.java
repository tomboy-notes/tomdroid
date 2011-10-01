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
	
	// position keepers
	private boolean inTitleTag = false;
	private boolean inLastChangeDateTag = false;
	
	// -- Tomboy's notes XML tags names --
	// Metadata related
	private final static String TITLE = "title";
	private final static String LAST_CHANGE_DATE = "last-change-date";
	
	// Buffers for parsed elements
	private StringBuilder title = new StringBuilder();
	private StringBuilder lastChangeDate = new StringBuilder();
	
	// link to model 
	private Note note;
	
	public NoteHandler(Note note) {
		this.note = note;
	}
	
	@Override
	public void startElement(String uri, String localName, String name,	Attributes attributes) throws SAXException {
		
		// TODO validate top-level tag for tomboy notes and throw exception if its the wrong version number (maybe offer to parse also?)		

		if (localName.equals(TITLE)) {
			inTitleTag = true;
		} else if (localName.equals(LAST_CHANGE_DATE)) {
			inLastChangeDateTag = true;
		}

	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException, TimeFormatException {

		if (localName.equals(TITLE)) {
			inTitleTag = false;
			note.setTitle(title.toString());
		} else if (localName.equals(LAST_CHANGE_DATE)) {
			inLastChangeDateTag = false;
			note.setLastChangeDate(lastChangeDate.toString());
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		
		if (inTitleTag) {
			title.append(ch, start, length);
		} else if (inLastChangeDateTag) {
			lastChangeDate.append(ch, start, length);
		}
	}
}
