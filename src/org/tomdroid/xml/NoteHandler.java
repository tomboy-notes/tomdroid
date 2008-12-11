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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
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
public class NoteHandler extends DefaultHandler {
	
	// position keepers
	private boolean inNoteTag = false;
	private boolean inTextTag = false;
	private boolean inNoteContentTag = false;
	private boolean inNoteContentTagMustGrabTitle = true; // hack to grab title and make it big
	private boolean inBoldTag = false;
	private boolean inItalicTag = false;
	private boolean inStrikeTag = false;
	private boolean inHighlighTag = false;
	private boolean inMonospaceTag = false;
	private boolean inSizeSmallTag = false;
	private boolean inSizeLargeTag = false;
	private boolean inSizeHugeTag = false;
	private boolean inList = false;
	private boolean inListItem = false;
	
	// -- Tomboy's notes XML tags names --
	// Style related
	private final static String NOTE_CONTENT = "note-content";
	private final static String BOLD = "bold";
	private final static String ITALIC = "italic";
	private final static String STRIKETHROUGH = "strikethrough";
	private final static String HIGHLIGHT = "highlight";
	private final static String MONOSPACE = "monospace";
	// Sizes are using a namespace identifier size. Ex: <size:small></size:small>
	private final static String NS_SIZE = "http://beatniksoftware.com/tomboy/size";
	private final static String SMALL = "small";
	private final static String LARGE = "large";
	private final static String HUGE = "huge";
	// List-related
	// TODO nested lists are not supported
	// I think that using a list integer instead of a boolean and incrementing or decrementing it depending on state would do it
	private final static String LIST = "list";
	private final static String LIST_ITEM = "list-item";
	
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
		// TODO is XML CaSe SeNsItIve? if not change equals to equalsIgnoreCase and apply to endElement()
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
			} else if (localName.equals(LIST)) {
				inList = true;
			} else if (localName.equals(LIST_ITEM)) {
				inListItem = true;
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
			} else if (localName.equals(LIST)) {
				inList = false;
			} else if (localName.equals(LIST_ITEM)) {
				inListItem = false;
			}
		}
	}

	// FIXME we'll have to think about how we handle the title soon.. IMHO there's a problem with duplicating the data from the <title> tag and also putting it straight into the note.. this will have to be reported to tomboy 
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		
		String currentString = new String(ch, start, length);
		
		// TODO remove this call when we will be done
		Log.i(this.toString(), "char string: " + currentString);

		if (inNoteContentTag) {
			// while we are in note-content, append
			ssb.append(currentString, start, length);
			int strLenStart = ssb.length()-length;
			int strLenEnd = ssb.length();
			
			// the first line of the note-content tag is the note's title. It must be big like in tomboy.
			// TODO tomboy's fileformat suggestion: title should not be repeated in the note-content. This is ugly IMHO
			if (inNoteContentTagMustGrabTitle) {
				ssb.setSpan(new RelativeSizeSpan(Note.NOTE_SIZE_HUGE_FACTOR), strLenStart, strLenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				inNoteContentTagMustGrabTitle = false;
			}
			
			// apply style if required
			// TODO I haven't tested nested tags yet
			if (inBoldTag) {
				ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), strLenStart, strLenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inItalicTag) {
				ssb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), strLenStart, strLenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inStrikeTag) {
				ssb.setSpan(new StrikethroughSpan(), strLenStart, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inHighlighTag) {
				ssb.setSpan(new BackgroundColorSpan(Note.NOTE_HIGHLIGHT_COLOR), strLenStart, strLenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inMonospaceTag) {
				ssb.setSpan(new TypefaceSpan(Note.NOTE_MONOSPACE_TYPEFACE), strLenStart, strLenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inSizeSmallTag) {
				ssb.setSpan(new RelativeSizeSpan(Note.NOTE_SIZE_SMALL_FACTOR), strLenStart, strLenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inSizeLargeTag) {
				ssb.setSpan(new RelativeSizeSpan(Note.NOTE_SIZE_LARGE_FACTOR), strLenStart, strLenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inSizeHugeTag) {
				ssb.setSpan(new RelativeSizeSpan(Note.NOTE_SIZE_HUGE_FACTOR), strLenStart, strLenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (inList && inListItem) {
				ssb.setSpan(new BulletSpan(), strLenStart, strLenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
	}
}
