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
package org.tomdroid.util.xml;

import org.tomdroid.Note;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * I don't know if I'm doing the right thing but I think that giving this class
 * the responsibility of filling the note is something quite cohesive and hope 
 * the coupling involved won't do much damage. I guess time will tell.
 */
// FIXME This class needs love right now
public class NoteHandler extends DefaultHandler {
	
	// position keepers
	private boolean inNoteTag = false;
	private boolean inTextTag = false;
	private boolean inNoteContentTag = false;
	
	// link to model 
	private Note note;
	
	public NoteHandler(Note note) {
		this.note = note;
	}
	
	@Override
	public void startElement(String uri, String localName, String name,	Attributes attributes) throws SAXException {
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		// TODO Auto-generated method stub
		super.endElement(uri, localName, name);
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		// TODO Auto-generated method stub
		super.characters(ch, start, length);
	}

}
