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
package org.tomdroid.xml;

import java.util.ArrayList;

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
	
	// holding state for tags
	private int boldStartPos;
	private int boldEndPos;
	private int italicStartPos;
	private int italicEndPos;
	private int strikethroughStartPos;
	private int strikethroughEndPos;
	private int highlightStartPos;
	private int highlightEndPos;
	private int monospaceStartPos;
	private int monospaceEndPos;
	private int smallStartPos;
	private int smallEndPos;
	private int largeStartPos;
	private int largeEndPos;
	private int hugeStartPos;
	private int hugeEndPos;
	private ArrayList<Integer> listItemStartPos = new ArrayList<Integer>(0);
	private ArrayList<Integer> listItemEndPos = new ArrayList<Integer>(0);
	private ArrayList<Boolean> listItemIsEmpty =  new ArrayList<Boolean>(0);
	
	// accumulate note-content in this var since it spans multiple xml tags
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
				// Book keeping of where the list-items started and where they end.
				// we need to do this here because a list-item must always have a start,
				// but it doesn't always have any content--so we must assume that a list-item
				// is empty until characters() gets called and proves otherwise.
				
				if (listItemIsEmpty.size() < inListLevel) {
					listItemIsEmpty.add(new Boolean(true));
				}
				// if listItem's position not already in tracking array, add it.
				// Otherwise if the start position equals 0 then set
				if (listItemStartPos.size() < inListLevel) {
					listItemStartPos.add(new Integer(ssb.length()));
				} else if (listItemStartPos.get(inListLevel-1) == 0) { 
					listItemStartPos.set(inListLevel-1, new Integer(ssb.length()));					
				}
				// no matter what, we track the end (we add if array not big enough or set otherwise) 
				if (listItemEndPos.size() < inListLevel) {
					listItemEndPos.add(new Integer(ssb.length()));
				} else {
					listItemEndPos.set(inListLevel-1, ssb.length());					
				}
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
				// apply style and reset position keepers
				ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), boldStartPos, boldEndPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				boldStartPos = 0;
				boldEndPos = 0;

			} else if (name.equals(ITALIC)) {
				inItalicTag = false;
				// apply style and reset position keepers
				ssb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), italicStartPos, italicEndPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				italicStartPos = 0;
				italicEndPos = 0;

			} else if (name.equals(STRIKETHROUGH)) {
				inStrikeTag = false;
				// apply style and reset position keepers
				ssb.setSpan(new StrikethroughSpan(), strikethroughStartPos, strikethroughEndPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				strikethroughStartPos = 0;
				strikethroughEndPos = 0;

			} else if (name.equals(HIGHLIGHT)) {
				inHighlighTag = false;
				// apply style and reset position keepers
				ssb.setSpan(new BackgroundColorSpan(Note.NOTE_HIGHLIGHT_COLOR), highlightStartPos, highlightEndPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				highlightStartPos = 0;
				highlightEndPos = 0;
				
			} else if (name.equals(MONOSPACE)) {
				inMonospaceTag = false;
				// apply style and reset position keepers
				ssb.setSpan(new TypefaceSpan(Note.NOTE_MONOSPACE_TYPEFACE), monospaceStartPos, monospaceEndPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				monospaceStartPos = 0;
				monospaceEndPos = 0;

			} else if (name.equals(SMALL)) {
				inSizeSmallTag = false;
				// apply style and reset position keepers
				ssb.setSpan(new RelativeSizeSpan(Note.NOTE_SIZE_SMALL_FACTOR), smallStartPos, smallEndPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				smallStartPos = 0;
				smallEndPos = 0;
				
			} else if (name.equals(LARGE)) {
				inSizeLargeTag = false;
				// apply style and reset position keepers
				ssb.setSpan(new RelativeSizeSpan(Note.NOTE_SIZE_LARGE_FACTOR), largeStartPos, largeEndPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				largeStartPos = 0;
				largeEndPos = 0;

			} else if (name.equals(HUGE)) {
				inSizeHugeTag = false;
				// apply style and reset position keepers
				ssb.setSpan(new RelativeSizeSpan(Note.NOTE_SIZE_HUGE_FACTOR), hugeStartPos, hugeEndPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				hugeStartPos = 0;
				hugeEndPos = 0;

			} else if (name.equals(LIST)) {
				inListLevel--;
			} else if (name.equals(LIST_ITEM)) {
				
				// if this list item is "empty" then we don't need to try rendering anything.
				if (!inListItem && listItemIsEmpty.get(inListLevel-1))
				{
					listItemStartPos.set(inListLevel-1, new Integer(0));
					listItemEndPos.set(inListLevel-1, new Integer(0));
					listItemIsEmpty.set(inListLevel-1, new Boolean(true));
					
					return;					
				}
				// here, we apply margin and create a bullet span. Plus, we need to reset position keepers.
				// TODO new sexier bullets?
				// Show a leading margin that is as wide as the nested level we are in
				ssb.setSpan(new LeadingMarginSpan.Standard(30*inListLevel), listItemStartPos.get(inListLevel-1), listItemEndPos.get(inListLevel-1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				ssb.setSpan(new BulletSpan(), listItemStartPos.get(inListLevel-1), listItemEndPos.get(inListLevel-1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				listItemStartPos.set(inListLevel-1, new Integer(0));
				listItemEndPos.set(inListLevel-1, new Integer(0));
				listItemIsEmpty.set(inListLevel-1, new Boolean(true));
				
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
				// if tag is not equal to 0 then we are already in it: no need to reset it's position again 
				if (boldStartPos == 0) {
					boldStartPos = strLenStart;
				}
				// no matter what, if we are still in the tag, end is now further
				boldEndPos = strLenEnd;
			}
			if (inItalicTag) {
				// if tag is not equal to 0 then we are already in it: no need to reset it's position again 
				if (italicStartPos == 0) {
					italicStartPos = strLenStart;
				}
				// no matter what, if we are still in the tag, end is now further
				italicEndPos = strLenEnd;
			}
			if (inStrikeTag) {
				// if tag is not equal to 0 then we are already in it: no need to reset it's position again 
				if (strikethroughStartPos == 0) {
					strikethroughStartPos = strLenStart;
				}
				// no matter what, if we are still in the tag, end is now further
				strikethroughEndPos = strLenEnd;
			}
			if (inHighlighTag) {
				// if tag is not equal to 0 then we are already in it: no need to reset it's position again 
				if (highlightStartPos == 0) {
					highlightStartPos = strLenStart;
				}
				// no matter what, if we are still in the tag, end is now further
				highlightEndPos = strLenEnd;
			}
			if (inMonospaceTag) {
				// if tag is not equal to 0 then we are already in it: no need to reset it's position again 
				if (monospaceStartPos == 0) {
					monospaceStartPos = strLenStart;
				}
				// no matter what, if we are still in the tag, end is now further
				monospaceEndPos = strLenEnd;
			}
			if (inSizeSmallTag) {
				// if tag is not equal to 0 then we are already in it: no need to reset it's position again 
				if (smallStartPos == 0) {
					smallStartPos = strLenStart;
				}
				// no matter what, if we are still in the tag, end is now further
				smallEndPos = strLenEnd;
			}
			if (inSizeLargeTag) {
				// if tag is not equal to 0 then we are already in it: no need to reset it's position again 
				if (largeStartPos == 0) {
					largeStartPos = strLenStart;
				}
				// no matter what, if we are still in the tag, end is now further
				largeEndPos = strLenEnd;
			}
			if (inSizeHugeTag) {
				// if tag is not equal to 0 then we are already in it: no need to reset it's position again 
				if (hugeStartPos == 0) {
					hugeStartPos = strLenStart;
				}
				// no matter what, if we are still in the tag, end is now further
				hugeEndPos = strLenEnd;
			}
			if (inListItem) {
				// this list item is not empty, so we mark it as such. We keep track of this to avoid any
				// problems with list items nested like this: <item><item><item>Content!</item></item></item>
				listItemIsEmpty.set(inListLevel-1, new Boolean(false));
				
				// no matter what, if we are still in the tag, end is now further
				listItemEndPos.set(inListLevel-1, strLenEnd);					
			}
		}
	}
}
