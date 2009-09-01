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
package org.tomdroid.xml;

import org.tomdroid.Note;
import org.tomdroid.util.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * I don't know if I'm doing the right thing but I think that giving this class
 * the responsibility of filling the note is something quite cohesive and hope 
 * the coupling involved won't do much damage. I guess time will tell.
 */
public class NoteHandler extends DefaultHandler {
	
	// position keepers
	private boolean inTitleTag = false;
	private boolean inLastChangeDateTag = false;
	private boolean inNoteContentTag = false;
	
	// -- Tomboy's notes XML tags names --
	// Metadata related
	private final static String TITLE = "title";
	private final static String LAST_CHANGE_DATE = "last-change-date"; 
	// Style related
	private final static String NOTE_CONTENT = "note-content";
	
	private final static String NS_SIZE = "http://beatniksoftware.com/tomboy/size";
	private final static String PREFIX_SIZE = "size";
	private final static String NS_LINK = "http://beatniksoftware.com/tomboy/link";
	private final static String PREFIX_LINK = "link";
	
	// accumulate notecontent is this var since it spans multiple xml tags
	private StringBuilder xmlContent = new StringBuilder();
	
	// link to model 
	private Note note;
	
	public NoteHandler(Note note) {
		this.note = note;
	}
	
	@Override
	public void startElement(String uri, String localName, String name,	Attributes attributes) throws SAXException {
		
		// TODO validate top-level tag for tomboy notes and throw exception if its the wrong version number (maybe offer to parse also?)
		
		if (localName.equals(NOTE_CONTENT)) {

			// we are under the note-content tag
			// we will append all its nested tags so I create a string builder to do that
			inNoteContentTag = true;
		} else if (localName.equals(TITLE)) {
			inTitleTag = true;
		} else if (localName.equals(LAST_CHANGE_DATE)) {
			inLastChangeDateTag = true;
		}

		// if we are in note-content, recreate the xml
		// we're not adding the note-content tags to the xml content
		if (inNoteContentTag && !localName.equals(NOTE_CONTENT)) {
			
			String tag = "<";
			
			if (uri != null) {
				if (uri.equals(NS_LINK)) {
					tag += PREFIX_LINK+":";
				} else if (uri.equals(NS_SIZE)) {
					tag += PREFIX_SIZE+":";
				}
			}
			
			tag += localName+">";
			xmlContent.append(tag);
		}
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {

		if (localName.equals(NOTE_CONTENT)) {
			inNoteContentTag = false;
		} else if (localName.equals(TITLE)) {
			inTitleTag = false;
		} else if (localName.equals(LAST_CHANGE_DATE)) {
			inLastChangeDateTag = false;
		}
		
		// if we are in note-content, recreate the xml
		if (inNoteContentTag) {
			
			String tag = "</";
			
			if (uri != null) {
				if (uri.equals(NS_LINK)) {
					tag += PREFIX_LINK+":";
				} else if (uri.equals(NS_SIZE)) {
					tag += PREFIX_SIZE+":";
				}
			}
			
			tag += localName+">";
			xmlContent.append(tag);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		note.setXmlContent(xmlContent.toString());
	}

	// FIXME we'll have to think about how we handle the title soon.. IMHO there's a problem with duplicating the data from the <title> tag and also putting it straight into the note.. this will have to be reported to tomboy 
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		
		String currentString = new String(ch, start, length);
		
		if (inTitleTag) {
			note.setTitle(currentString);
// commented out Date parsing for 0.2.0 since I don't plan to sort notes based on that yet and it is a big performance regression
//		} else if (inLastChangeDateTag) {
//			//TODO there is probably a parsing error here we should trap 
//			DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
//			note.setLastChangeDate(fmt.parseDateTime(currentString));
		}

		if (inNoteContentTag) {
			// while we are in note-content, append
			xmlContent.append(XmlUtils.escape(currentString));
		}
	}
}
