/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
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
package org.tomdroid.util;

import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.tomdroid.Note;
import org.tomdroid.ui.Tomdroid;

import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;

public class NoteXMLContentBuilder implements Runnable {
	
	public static final int PARSE_OK = 0;
	public static final int PARSE_ERROR = 1;
	
	private SpannableStringBuilder noteContent = null;
	
	// this is what we are building here
	private String noteXMLContent = new String();
	
	private final String TAG = "NoteBuilder";
	
	// thread related
	private Thread runner;
	private Handler parentHandler;
	
	public NoteXMLContentBuilder () {}
	
	public NoteXMLContentBuilder setCaller(Handler parent) {
		
		parentHandler = parent;
		return this;
	}
	
	public NoteXMLContentBuilder setInputSource(SpannableStringBuilder nc) {
		
		noteContent = nc;
		return this;
	}
	
	public String build() {
		
		//runner = new Thread(this);
		//runner.start();
		run();
		return noteXMLContent;
	}
	
	public void run() {
		
		boolean successful = true;
		
		try {
			for( int prevPos=0, currPos=0, maxPos=noteContent.length(); 
					currPos!=-1 && currPos<=maxPos && prevPos<maxPos;
					prevPos=currPos, currPos=noteContent.nextSpanTransition(currPos, maxPos, Object.class) )
			{
				TreeMap<Integer,LinkedList<String>> elemStartsByEnd = new TreeMap<Integer,LinkedList<String>>();
				TreeMap<Integer,LinkedList<String>> elemEndsByStart = new TreeMap<Integer,LinkedList<String>>();
				Object[] spans = noteContent.getSpans(currPos, currPos, Object.class);
				int bulletStart = 0, bulletEnd = 0, currentMargin = 0;
				for( Object span: spans )
				{
					int spanStart = noteContent.getSpanStart(span);
					int spanEnd = noteContent.getSpanEnd(span);
					String elementName = "";
					if( spanStart==currPos || spanEnd==currPos )
					{
						if( span instanceof StyleSpan )
						{
							StyleSpan style = (StyleSpan) span;
							if( (style.getStyle()&Typeface.BOLD)>0 )
							{
								elementName = "bold";
							}
							if( (style.getStyle()&Typeface.ITALIC)>0 )
							{
								elementName = "italic";
							}
						}
						else if( span instanceof StrikethroughSpan )
						{
							elementName = "strikethrough";
						}
						else if( span instanceof BackgroundColorSpan )
						{
							BackgroundColorSpan bgcolor = (BackgroundColorSpan) span;
							if( bgcolor.getBackgroundColor()==Note.NOTE_HIGHLIGHT_COLOR )
							{
								elementName = "highlight";
							}
						}
						else if( span instanceof TypefaceSpan )
						{
							TypefaceSpan typeface = (TypefaceSpan) span;
							if( typeface.getFamily()==Note.NOTE_MONOSPACE_TYPEFACE )
							{
								elementName = "monospace";
							}
						}
						else if( span instanceof RelativeSizeSpan )
						{
							RelativeSizeSpan size = (RelativeSizeSpan) span;
							if( size.getSizeChange()==Note.NOTE_SIZE_SMALL_FACTOR )
							{
								elementName = "size:small";
							}
							else if( size.getSizeChange()==Note.NOTE_SIZE_LARGE_FACTOR )
							{
								elementName = "size:large";
							}
							else if( size.getSizeChange()==Note.NOTE_SIZE_HUGE_FACTOR )
							{
								elementName = "size:huge";
							}
						}
						else if( span instanceof LeadingMarginSpan.Standard )
						{
							LeadingMarginSpan.Standard margin = (LeadingMarginSpan.Standard) span;
							currentMargin = margin.getLeadingMargin(true);
						}
						else if( span instanceof BulletSpan )
						{
							elementName = "list-item";
							bulletStart = spanStart;
							bulletEnd = spanEnd;
							int listLevelDiff = 0, marginFactor = 30;
							int currentListLevel = currentMargin / marginFactor;
							// compute indentation difference between current position and offset -1 for a starting transition
							// or current position and offset +1 for an ending transition:
							if( currPos==bulletStart )
							{
								elementName += " dir=\"ltr\""; // add unsupported (and unused by Tomboy?), fixed orientation attribute
								int prevListLevel = 0;
								if( currPos > 0 )
								{
									LeadingMarginSpan.Standard[] prevMargins = noteContent.getSpans( currPos-1, currPos, LeadingMarginSpan.Standard.class );
									for( LeadingMarginSpan.Standard prevMargin: prevMargins ) 
										prevListLevel = prevMargin.getLeadingMargin(true) / marginFactor;
								}
								listLevelDiff = currentListLevel - prevListLevel;
							}
							else if( currPos==bulletEnd )
							{
								int nextListLevel = 0;
								if( currPos < noteContent.length() )
								{
									LeadingMarginSpan.Standard[] nextMargins = noteContent.getSpans( currPos, currPos+1, LeadingMarginSpan.Standard.class );
									for( LeadingMarginSpan.Standard nextMargin: nextMargins ) 
										nextListLevel = nextMargin.getLeadingMargin(true) / marginFactor;
								}
								else if( currPos == noteContent.length() )
								{
									// force writing of last list end tag of the note
									// FIXME: this also has to be done before any non-list content following a list
									listLevelDiff = -1;
								}
								if( currentListLevel < nextListLevel )
								{
									// suppress list-items end tag, as it has to enclose the following list element
									// FIXME: what happens if abs(listLevelDiff)>1?
									elementName = "";
								}
							}
							// add needed number of list start or list end and list-item end tags to represent the observed indentation 
							// difference at this position:
							for( int i=0; i<Math.abs(listLevelDiff); i++ )
							{
								if( listLevelDiff<0)
								{
									// assume a growing negative fake start offset to force list and list-item end tags to appear behind all other end tags at this position:
									if( elemEndsByStart.get(-1-i*2) == null ) elemEndsByStart.put( -1-i*2, new LinkedList<String>() );
									elemEndsByStart.get(-1-i*2).add( "list" );
									// suppress a closing list-item end tag at the end of input
									// FIXME: must also be suppressed before non-list content following a list
									if( currPos<maxPos )
									{
										// explicitly add a previously suppressed list-items end tag after enclosed list element
										// (assume a fake start offset of -2 to force list-item end to appear behind list end)
										if( elemEndsByStart.get(-2-i*2) == null ) elemEndsByStart.put( -2-i*2, new LinkedList<String>() );
										elemEndsByStart.get(-2-i*2).add( "list-item" );
									}
								}
								else
								{
									// assume a fake end offset of maxPos to force list starts to appear in front of all other start tags at this position:
									if( elemStartsByEnd.get(maxPos) == null ) elemStartsByEnd.put( maxPos, new LinkedList<String>() );
									elemStartsByEnd.get(maxPos).add( "list" );
								}
							}
						}
					}
					// add generic start/end tags defined above
					if( !elementName.isEmpty() )
					{
						if( spanStart==currPos ) 
						{
							if( elemStartsByEnd.get(spanEnd)==null ) elemStartsByEnd.put( spanEnd, new LinkedList<String>() );
							elemStartsByEnd.get(spanEnd).add(elementName);
						}
						else if( spanEnd==currPos )
						{
							if( elemEndsByStart.get(spanStart)==null ) elemEndsByStart.put( spanStart, new LinkedList<String>() );
							elemEndsByStart.get(spanStart).add(elementName);
						}
					}
				}
				// write plain character content from previous to current (relevant) span transition:
				noteXMLContent += noteContent.subSequence( prevPos, currPos );
				// write needed end tags for the current span transition in the correct order, depending on the corresponding span start positions:
				for( Integer key: elemEndsByStart.descendingKeySet() )
				{
					for( String elementName: elemEndsByStart.get(key) ) noteXMLContent += "</"+elementName+">";
				}
				// write needed start tags for the current span transition in the correct order, depending on the corresponding span end positions:
				for( Integer key: elemStartsByEnd.descendingKeySet() )
				{
					for( String elementName: elemStartsByEnd.get(key) ) noteXMLContent += "<"+elementName+">";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// TODO handle error in a more granular way
			Log.e(TAG, "There was an error parsing the note.");
			successful = false;
		}
		
		warnHandler(successful);
	}
	
    private void warnHandler(boolean successful) {
		
		// notify the main UI that we are done here (sending an ok along with the note's title)
		Message msg = Message.obtain();
		if (successful) {
			msg.what = PARSE_OK;
		} else {
			
			msg.what = PARSE_ERROR;
		}
		
		parentHandler.sendMessage(msg);
    }
}
