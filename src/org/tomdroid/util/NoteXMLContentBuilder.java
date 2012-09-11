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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.tomdroid.Note;
import org.tomdroid.xml.LinkInternalSpan;

import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

public class NoteXMLContentBuilder implements Runnable {
	
	public static final int PARSE_OK = 0;
	public static final int PARSE_ERROR = 1;
	
	private SpannableStringBuilder noteContent = null;

	// set up check for mismatch (cross-boundary spans like <bold>foo<italic>bar</bold>foo</italic>)
	
	private ArrayList<String> openTags = new ArrayList<String>();
	private ArrayList<String> closeTags = new ArrayList<String>();
	private ArrayList<String> tagsToOpen = new ArrayList<String>();
	
	// this is what we are building here
	private String noteXMLContent = new String();
	
	private final String TAG = "NoteBuilder";
	
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
			// replace illegal XML characters with corresponding entities:
			String plainText = noteContent.toString();
			TreeMap<String,String> replacements = new TreeMap<String,String>();
			replacements.put( "&", "&amp;" ); replacements.put( "<", "&lt;" ); replacements.put( ">", "&gt;" );
			for( Map.Entry<String,String> entry: replacements.entrySet() ) {
				for( int currPos=plainText.length(); currPos>=0; currPos-- ){
	 				currPos = plainText.lastIndexOf(entry.getKey(), currPos);
	 				if(currPos < 0)
	 				    break;
	 				noteContent.replace( currPos, currPos+entry.getKey().length(), entry.getValue() );
					TLog.d(TAG, "new xml content: {0}", noteContent.toString());
				}
				plainText = noteContent.toString(); // have to refresh!
			}

			// translate spans into XML elements:
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
						else if( span instanceof LinkInternalSpan )
						{
							elementName = "link:internal";
						}
						else if( span instanceof BulletSpan )
						{
							elementName = "list-item";
							bulletStart = spanStart;
							bulletEnd = spanEnd;
							int listLevelDiff = 0, marginFactor = 30;
							int currentListLevel = currentMargin / marginFactor;
							int prevListLevel = 0, nextListLevel = 0;
							// compute indentation difference between current position and offset -1 for a starting transition
							// or current position and offset +1 for an ending transition:
							if( currPos==bulletStart )
							{
								elementName += " dir=\"ltr\""; // add unsupported (and unused by Tomboy?), fixed orientation attribute
								prevListLevel = 0;
								if( currPos > 0 )
								{
									LeadingMarginSpan.Standard[] prevMargins = noteContent.getSpans( currPos-1, currPos-1, LeadingMarginSpan.Standard.class );
									if( prevMargins.length>1 ) throw new Exception("Multiple margins at "+new Integer(currPos-1).toString() );
									for( LeadingMarginSpan.Standard prevMargin: prevMargins ) 
										prevListLevel = prevMargin.getLeadingMargin(true) / marginFactor;
								}
								listLevelDiff = currentListLevel - prevListLevel;
							}
							else if( currPos==bulletEnd )
							{
								// most needed list tags are triggered by bullet start transitions, but we have
								// to trigger some list end tags on special bullet end transitions...
								nextListLevel = 0;
								if( currPos < noteContent.length() )
								{
									LeadingMarginSpan.Standard[] nextMargins = noteContent.getSpans( currPos+1, currPos+1, LeadingMarginSpan.Standard.class );
									if( nextMargins.length>1 ) throw new Exception("Multiple margins at "+new Integer(currPos+1).toString() );;
									for( LeadingMarginSpan.Standard nextMargin: nextMargins )
										nextListLevel = nextMargin.getLeadingMargin(true) / marginFactor;
									// force writing of missing list end tags before non-list content:
									if( nextMargins.length==0 ) listLevelDiff = -currentListLevel;
								}
								// force writing of missing list end tags at the end of the note:
								else listLevelDiff = -currentListLevel;
								// suppress list-item end tag, as it has to enclose the following list element:
								// FIXME: what happens if abs(listLevelDiff)>1?
								if( currentListLevel < nextListLevel ) elementName = "";
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
									int levelCorrector = Math.abs(listLevelDiff)-i-1; 
									if( nextListLevel+levelCorrector>0 || prevListLevel+levelCorrector>1 )
									{
										// explicitly add a previously suppressed list-items end tag after enclosed list element,
										// but only if this was not the root list element of the list
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
					if( elementName.length() != 0 )
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

				// API 3 compat - is this reversal really necessary?  I think it should be reversed in the for(String elementName...)
				
				ListIterator<Integer> iter = new ArrayList<Integer>(elemEndsByStart.keySet()).listIterator(elemEndsByStart.size());

				// write needed end tags for the current span transition in the correct order, depending on the corresponding span start positions:

				while (iter.hasPrevious()) {
				    Integer key = iter.previous();
					for( String elementName: elemEndsByStart.get(key) ) {
						closeTags.add(elementName); // add for comparing with later ones
					}				    
				}
				
				iter = new ArrayList<Integer>(elemStartsByEnd.keySet()).listIterator(elemStartsByEnd.size());

				// write needed start tags for the current span transition in the correct order, depending on the corresponding span end positions:
				
				while (iter.hasPrevious()) {
				    Integer key = iter.previous();
					for( String elementName: elemStartsByEnd.get(key) ) {
						tagsToOpen.add(elementName); // add for comparing with later ones
					}
				}

			    noteXMLContent += addTags(currPos == maxPos); 

			}
		} catch (Exception e) {
			e.printStackTrace();
			// TODO handle error in a more granular way
			TLog.e(TAG, "There was an error parsing the note.");
			successful = false;
		}
		if(!openTags.isEmpty()) {
			for(int x = 0; x < openTags.size(); x++) {
				String tag = openTags.get(openTags.size()-x-1);
				TLog.d(TAG, "Closed final tag: {0}","</"+tag+">");
				noteXMLContent += "</"+tag+">";
			}
		}
		
		
		warnHandler(successful);
	}
	
    private String addTags(boolean end) {
    	String tags = "";
		if(!openTags.isEmpty()) { 
			if(!closeTags.isEmpty()) { // check for mismatch
				String tag = openTags.get(openTags.size()-1);
				tags += "</"+tag+">";
				if(closeTags.get(closeTags.size()-1).equals(tag)) { // match, close tag
					closeTags.remove(closeTags.size()-1);
					openTags.remove(openTags.size()-1);
				}
				else { // mismatch, close for reopening, reiterate for closing
					openTags.remove(openTags.size()-1);
					tagsToOpen.add(tag);
					tags += addTags(end);
				}
			}
		}
		if(!end) {
			for(String tag : tagsToOpen) {
				if(TextUtils.join(",", openTags).contains(tag)) // already opened
					continue;
				tags+="<"+tag+">";
				openTags.add(tag);
			}
		}
		tagsToOpen.clear();
		
		return tags;
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
