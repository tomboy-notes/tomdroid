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
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

/*
 * This class is responsible for parsing the xml note content
 * and formatting the contents in a SpannableStringBuilder
 */
public class NoteContentHandler extends DefaultHandler {
	
	// position keepers
	private boolean inNoteContentTag = false;
	private boolean inBoldTag = false;
	private boolean inItalicTag = false;
	private boolean inStrikeTag = false;
	private boolean inHighlighTag = false;
	private boolean inMonospaceTag = false;
	private boolean inSizeSmallTag = false;
	private boolean inSizeLargeTag = false;
	private boolean inSizeHugeTag = false;
	private int inListLevel = 0;
	private boolean inListItem = false;
	
	// -- Tomboy's notes XML tags names -- 
	// Style related
	private final static String NOTE_CONTENT = "note-content";
	private final static String BOLD = "bold";
	private final static String ITALIC = "italic";
	private final static String STRIKETHROUGH = "strikethrough";
	private final static String HIGHLIGHT = "highlight";
	private final static String MONOSPACE = "monospace";
	private final static String SMALL = "size:small";
	private final static String LARGE = "size:large";
	private final static String HUGE = "size:huge";
	// Bullet list-related
	private final static String LIST = "list";
	private final static String LIST_ITEM = "list-item";
	
	// accumulate notecontent is this var since it spans multiple xml tags
	private SpannableStringBuilder ssb;
	
	public NoteContentHandler(SpannableStringBuilder noteContent) {
		
		this.ssb = noteContent;
	}
	
	@Override
	public void startElement(String uri, String localName, String name,	Attributes attributes) throws SAXException {
		
		if (name.equals(NOTE_CONTENT)) {

			// we are under the note-content tag
			// we will append all its nested tags so I create a string builder to do that
			inNoteContentTag = true;
		}

		// if we are in note-content, keep and convert formatting tags
		// TODO is XML CaSe SeNsItIve? if not change equals to equalsIgnoreCase and apply to endElement()
		if (inNoteContentTag) {
			if (name.equals(BOLD)) {
				inBoldTag = true;
			} else if (name.equals(ITALIC)) {
				inItalicTag = true;
			} else if (name.equals(STRIKETHROUGH)) {
				inStrikeTag = true;
			} else if (name.equals(HIGHLIGHT)) {
				inHighlighTag = true;
			} else if (name.equals(MONOSPACE)) {
				inMonospaceTag = true;
			} else if (name.equals(SMALL)) {
				inSizeSmallTag = true;
			} else if (name.equals(LARGE)) {
				inSizeLargeTag = true;
			} else if (name.equals(HUGE)) {
				inSizeHugeTag = true;
			} else if (name.equals(LIST)) {
				inListLevel++;
			} else if (name.equals(LIST_ITEM)) {
				inListItem = true;
			}
		}

	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {

		if (name.equals(NOTE_CONTENT)) {
			inNoteContentTag = false;
		}
		
		// if we are in note-content, keep and convert formatting tags
		if (inNoteContentTag) {
			if (name.equals(BOLD)) {
				inBoldTag = false;
			} else if (name.equals(ITALIC)) {
				inItalicTag = false;
			} else if (name.equals(STRIKETHROUGH)) {
				inStrikeTag = false;
			} else if (name.equals(HIGHLIGHT)) {
				inHighlighTag = false;
			} else if (name.equals(MONOSPACE)) {
				inMonospaceTag = false;
			} else if (name.equals(SMALL)) {
				inSizeSmallTag = false;
			} else if (name.equals(LARGE)) {
				inSizeLargeTag = false;
			} else if (name.equals(HUGE)) {
				inSizeHugeTag = false;
			} else if (name.equals(LIST)) {
				inListLevel--;
			} else if (name.equals(LIST_ITEM)) {
				inListItem = false;
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		
		String currentString = new String(ch, start, length);

		if (inNoteContentTag) {
			// while we are in note-content, append
			ssb.append(currentString, start, length);
			int strLenStart = ssb.length()-length;
			int strLenEnd = ssb.length();
			
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
			if (inListItem) {
				// TODO new sexier bullets?
				// Show a leading margin that is as wide as the nested level we are in
				ssb.setSpan(new LeadingMarginSpan.Standard(10*inListLevel), strLenStart, strLenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				ssb.setSpan(new BulletSpan(), strLenStart, strLenEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
	}
}
