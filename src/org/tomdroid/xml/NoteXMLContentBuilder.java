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
package org.tomdroid.xml;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import org.tomdroid.Note;
import org.tomdroid.util.TLog;
import org.tomdroid.util.TagNode;
import org.tomdroid.util.TagType;
import org.xmlpull.v1.XmlSerializer;

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
import android.util.Xml;

public class NoteXMLContentBuilder implements Runnable {
	
	public static final int PARSE_OK = 0;
	public static final int PARSE_ERROR = 1;
	
	private SpannableStringBuilder noteContent = null;
	
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
			
			// build TagTree
			TagNode root = new TagNode();
			root.start = 0;
			root.end = noteContent.length();
			List<TagType> tabuList = new LinkedList<TagType>();
			appendTree(root, root.start, root.end, tabuList);
			
			// add List nodes instead of margin nodes
			fixListNodes(root);
			
			// convert TagTree to String
			noteXMLContent = writeXML (root);
			TLog.d(TAG, "Build XMLContent: {0}", noteXMLContent);
			
		} catch (Exception e) {
			e.printStackTrace();
			// TODO handle error in a more granular way
			TLog.e(TAG, "There was an error parsing the note.");
			successful = false;
		}
		warnHandler(successful);
	}
	
	private void appendTree(TagNode parentNode, int start, int end, List<TagType> tabuList) {
		List<TagNode> siblingNodes = findSiblingNodes(tabuList, start, end);
				
		int previousEnd = start;
		for (TagNode sibling : siblingNodes) {
			int siblingStart = sibling.start;
			int siblingEnd = sibling.end;
			
			if (previousEnd != siblingStart) {
				TagNode textNode = getTextNode(previousEnd, siblingStart);
				parentNode.add(textNode);
				TLog.v(TAG, "Added a text-node before a span in the tree: {0}, ({1}/{2})", 
						textNode.text, textNode.start, textNode.end);
			}
			
			parentNode.add(sibling);
			TLog.v(TAG, "Added a span-node in the tree: {0}, ({1}/{2})", 
					sibling.tagType.toString(), sibling.start, sibling.end);
			tabuList.add(sibling.tagType);
			
			// recursion starts here
			appendTree(sibling, siblingStart, siblingEnd, tabuList);
			
			tabuList.remove(sibling.tagType);
			previousEnd = sibling.end;
		}
		
		if (previousEnd != end) {
			TagNode textNode = getTextNode(previousEnd, end);
			parentNode.add(textNode);
			TLog.v(TAG, "Added a text-node after a span in the tree: {0}, ({1}/{2})", 
					textNode.text, textNode.start, textNode.end);
		}
		
	}
	
	private Object[] getSpansInRange (int start, int end) {
		Object[] allSpans = noteContent.getSpans(start, end, Object.class);
		List<Object> rangeSpans = new LinkedList<Object>();
		
		for (Object span : allSpans) {
			if (	noteContent.getSpanStart(span) >= start
					&& noteContent.getSpanEnd(span) <= end
					&& noteContent.getSpanStart(span) <= end
					&& noteContent.getSpanEnd(span) >= start ) {
				rangeSpans.add(span);
			}
		}
		return rangeSpans.toArray();
	}

	private List<TagNode> findSiblingNodes(List<TagType> tabuList, int start, int end) {
		List<TagNode> nodes = new LinkedList<TagNode>();
		
		for (Object[] allSpans = getSpansInRange(start, end); 
				allSpans.length > 0; 
				allSpans = getSpansInRange(start, end)) {
			TagNode node = findFirstNode(allSpans, tabuList, start, end);
			if (node == null) break;
			nodes.add(node);
			start = node.end;
		}
		return nodes;
	}
	
	private TagNode findFirstNode (Object[] spans, List<TagType> tabuList, int cursorStart, int cursorEnd) {
		int min = cursorEnd;
		int max = cursorStart;
		TagNode returnNode = null;
		
		// get first span
		for (Object span : spans) {
			TagNode node = getNode(span);
			if (!node.tagType.equals(TagType.OTHER)
					&& !tabuList.contains(node.tagType)) {
				if ( node.start < min) {
					min = node.start;
				}
			}
		}
		
		// in case of multiple spans at this position find the outermost one
		for (Object span : noteContent.getSpans(min, min, Object.class)) {
			TagNode node = getNode(span);
			if (!node.tagType.equals(TagType.OTHER) 
					&& !tabuList.contains(node.tagType)
					&& node.start == min ) {
				if ( node.end > max ) {
					max = node.end;
					returnNode = node;
				}
			}
			
		}
		return returnNode;
	}

	private TagNode getTextNode (int start, int end) {
		TagNode node = new TagNode();
		node.tagType = TagType.TEXT;
		String text = noteContent.subSequence(start, end).toString();
		node.text = text;
		node.start = start;
		node.end = end;
		return node;
	}
	
	private TagNode getNode (Object span) {
		TagNode node = new TagNode();
		node.start = noteContent.getSpanStart(span);
		node.end = noteContent.getSpanEnd(span);
		
		if( span instanceof StyleSpan ) {
			StyleSpan style = (StyleSpan) span;
			if( (style.getStyle()&Typeface.BOLD)>0 )
			{
				node.tagType = TagType.BOLD;
			}
			if( (style.getStyle()&Typeface.ITALIC)>0 )
			{
				node.tagType = TagType.ITALIC;
			}
		}
		else if( span instanceof StrikethroughSpan )
		{
			node.tagType = TagType.STRIKETHROUGH;
		}

		else if( span instanceof BackgroundColorSpan )
		{
			BackgroundColorSpan bgcolor = (BackgroundColorSpan) span;
			if( bgcolor.getBackgroundColor()==Note.NOTE_HIGHLIGHT_COLOR )
			{
				node.tagType = TagType.HIGHLIGHT;
			}
		}
		else if( span instanceof TypefaceSpan )
		{
			TypefaceSpan typeface = (TypefaceSpan) span;
			if( typeface.getFamily()==Note.NOTE_MONOSPACE_TYPEFACE )
			{
				node.tagType = TagType.MONOSPACE;
			}
		}
		else if( span instanceof RelativeSizeSpan )
		{
			RelativeSizeSpan size = (RelativeSizeSpan) span;
			if( size.getSizeChange()==Note.NOTE_SIZE_SMALL_FACTOR )
			{
				node.tagType = TagType.SIZE_SMALL;
			}
			else if( size.getSizeChange()==Note.NOTE_SIZE_LARGE_FACTOR )
			{
				node.tagType = TagType.SIZE_LARGE;
			}
			else if( size.getSizeChange()==Note.NOTE_SIZE_HUGE_FACTOR )
			{
				node.tagType = TagType.SIZE_HUGE;
			}
		}
		else if( span instanceof LinkInternalSpan )
		{
			node.tagType = TagType.LINK_INTERNAL;
		}
		else if( span instanceof LeadingMarginSpan.Standard )
		{
			LeadingMarginSpan.Standard margin = (LeadingMarginSpan.Standard) span;
			int currentMargin = margin.getLeadingMargin(true);
			node.tagType = TagType.MARGIN;
			int marginFactor = 30;
			node.listLevel = currentMargin / marginFactor;
		}
		else if( span instanceof BulletSpan )
		{
			node.tagType = TagType.LIST_ITEM;
		}
		else {
			node.tagType = TagType.OTHER;
		}
		
		return node;
	}
	
	private void fixListNodes(TagNode node) {
		
		int previousListLevel = 0;
		TagNode parentListNode = node;
		
		for (TagNode marginNode : node.getChildren()) {
			if (marginNode.tagType.equals(TagType.MARGIN)) {
				
				int listLeveldiff = marginNode.listLevel - previousListLevel;
				previousListLevel = marginNode.listLevel;
				
				if (listLeveldiff > 0) {
					// add a new nested list in form of a list node
					for (int i = 0; i < listLeveldiff; i++) {
						TagNode listNode = new TagNode();
						listNode.tagType = TagType.LIST;
						
						parentListNode.add(listNode);
						parentListNode = listNode;
					}
				} else if (listLeveldiff < 0) {
					// go up in list-levels
					for (int i = 0; i > listLeveldiff; i--) {
						TagNode parent = parentListNode.getParent();
						parentListNode = parent;
					}
				}
				// move all children to new parentListNode
				TagNode.moveChildren(marginNode, parentListNode);
				// remove marginNode
				node.remove(marginNode);
			} else {
				fixListNodes(marginNode);
			}
		}
	}
	
	private String writeXML (TagNode root) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			//serializer.startDocument("UTF-8", true);
			
			appendXml(root, serializer);
			
			serializer.endDocument();
			return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	
	private void appendXml(TagNode node, XmlSerializer serializer){

		TagNode[] children = node.getChildren();
		
		for (TagNode child : children){
			try {
				if (child.tagType.equals(TagType.TEXT)) {
					serializer.text(child.text);
				} else {
					serializer.startTag("", child.getTagName());
					if (child.tagType.equals(TagType.LIST_ITEM)) {
						serializer.attribute("", "dir", "ltr");
					}
					// recursion starts here
					appendXml(child, serializer);
					
					serializer.endTag("", child.getTagName());
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
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
