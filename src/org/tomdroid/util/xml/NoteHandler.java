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

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;

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
	private boolean inBoldTag = false;
	private boolean inItalicTag = false;
	private boolean inStrikeTag = false;
	private boolean inHighlighTag = false;
	private boolean inMonospaceTag = false;
	private boolean inSizeSmallTag = false;
	private boolean inSizeLargeTag = false;
	private boolean inSizeHugeTag = false;
	
	// tag names
	private final static String NOTE_CONTENT = "note-content";
	private final static String BOLD = "bold";
	private final static String ITALIC = "italic";
	private final static String STRIKETHROUGH = "strikethrough";
	private final static String HIGHLIGHT = "highlight";
	private final static String MONOSPACE = "monospace";
	// Sizes are using a namespace identifier like <size:small></size:small>
	private final static String NS_SIZE = "http://beatniksoftware.com/tomboy/size";
	private final static String SMALL = "small";
	private final static String LARGE = "large";
	private final static String HUGE = "huge";
	
	// accumulate notecontent is this var since it spans multiple xml tags
	private SpannableStringBuilder ssb;
	
	// link to model 
	private Note note;
	
	public NoteHandler(Note note) {
		this.note = note;
		
		// we will use the SpannableStringBuilder from the note
		this.ssb = note.getNoteContent();
	}
	
	@Override
	public void startElement(String uri, String localName, String name,	Attributes attributes) throws SAXException {
		
		Log.i(this.toString(), "startElement: uri: " + uri + " local: " + localName + " name: " + name);
		if (localName.equals(NOTE_CONTENT)) {

			// we are under the note-content tag
			// we will append all its nested tags so I create a string builder to do that
			inNoteContentTag = true;
		}

		// if we are in note-content, keep and convert formatting tags
		// TODO is XML CaSe SeNsItIve? if not change equals to equalsIgnoreCase
		if (inNoteContentTag) {
			if (localName.equals(BOLD)) {
				inBoldTag = true;
			} else if (localName.equals(ITALIC)) {
				inItalicTag = true;
			} else if (localName.equals(STRIKETHROUGH)) {
				inStrikeTag = true;
			} else if (localName.equals(HIGHLIGHT)) {
				inHighlighTag = true;
			} else if (localName.equals(MONOSPACE)) {
				inMonospaceTag = true;
			} else if (uri.equals(NS_SIZE)) {
				// now check for the different possible sizes
				if (localName.equals(SMALL)) {
					inSizeSmallTag = true;
				} else if (localName.equals(LARGE)) {
					inSizeLargeTag = true;
				} else if (localName.equals(HUGE)) {
					inSizeHugeTag = true;
				}
			}
		}

	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {

		Log.i(this.toString(), "endElement: uri: " + uri + " local: " + localName + " name: " + name);
		
		if (localName.equals(NOTE_CONTENT)) {
			
			// note-content is over, we can set the builded note to Note's noteContent
			inNoteContentTag = false;
		}
		
		// if we are in note-content, keep and convert formatting tags
		if (inNoteContentTag) {
			if (localName.equals(BOLD)) {
				inBoldTag = false;
			} else if (localName.equals(ITALIC)) {
				inItalicTag = false;
			} else if (localName.equals(STRIKETHROUGH)) {
				inStrikeTag = false;
			} else if (localName.equals(HIGHLIGHT)) {
				inHighlighTag = false;
			} else if (localName.equals(MONOSPACE)) {
				inMonospaceTag = false;
			} else if (uri.equals(NS_SIZE)) {
				// now check for the different possible sizes
				if (localName.equals(SMALL)) {
					inSizeSmallTag = false;
				} else if (localName.equals(LARGE)) {
					inSizeLargeTag = false;
				} else if (localName.equals(HUGE)) {
					inSizeHugeTag = false;
				}
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		
		// TODO remove this call to avoid creating unused strings
		Log.i(this.toString(), "char string: " + new String(ch, start, length));

		if (inNoteContentTag) {
			
			// while we are in note-content, append
			ssb.append(new String(ch), start, length);
			
			// apply style if required
			// TODO I haven't tested nested tags yet
			// TODO RelativeSpan
			// TODO BulletSpan?
			if (inBoldTag) {
				ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), ssb.length()-length, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inItalicTag) {
				ssb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), ssb.length()-length, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inStrikeTag) {
				ssb.setSpan(new StrikethroughSpan(), ssb.length()-length, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inHighlighTag) {
				ssb.setSpan(new BackgroundColorSpan(Note.NOTE_HIGHLIGHT_COLOR), ssb.length()-length, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inMonospaceTag) {
				ssb.setSpan(new TypefaceSpan(Note.NOTE_MONOSPACE_TYPEFACE), ssb.length()-length, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inSizeSmallTag) {
				ssb.setSpan(new RelativeSizeSpan(Note.NOTE_SIZE_SMALL_FACTOR), ssb.length()-length, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inSizeLargeTag) {
				ssb.setSpan(new RelativeSizeSpan(Note.NOTE_SIZE_LARGE_FACTOR), ssb.length()-length, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inSizeHugeTag) {
				ssb.setSpan(new RelativeSizeSpan(Note.NOTE_SIZE_HUGE_FACTOR), ssb.length()-length, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
	}

}
