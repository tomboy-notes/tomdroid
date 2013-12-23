/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2013 Stefan Hammer <j-4@gmx.at>
 * Copyright 2013 Timo Dörr <timo@latecrew.de>
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
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Xml;

/**
 * Converts a SpannableStringBuilder to a XML String
 * The method uses a Graph-Theoretical approach to get a proper XML parsing.
 * 
 * At first the spans are converted into tree-nodes (from out to in and from beginning to end).
 * The obtained tree is then converted to XML using a Breadth First Search with a standard XML serializer.
 * 
 * A little bit of magic is needed to convert the margin spans to list nodes. This is done
 * within the main recursion.
 * Text fragments between spans are also converted to nodes within the main loop. This is done with
 * a simple end == newstart check before and after inserting a node.
 */
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
					
			// build TagTree first to get a Tree-Representation of our Spans
			TagNode root = new TagNode();
			root.setType(TagType.ROOT);
			root.start = 0;
			root.end = noteContent.length();
			
			// keep track of the tags we are in already (not to find the same ones again,
			// which only happens if start and end is equal to the parents)
			List<TagType> blackList = new LinkedList<TagType>();
			
			// First cut all spans crossing list-items and lists into parts, so that they appear nested
			// within the list-item
			cutAllSpansCrossingLists();
			
			// error finding
			Object[] allSpans = noteContent.getSpans(0, noteContent.length(), Object.class);
			for (Object span : allSpans) {
				TLog.v(TAG, "({0}/{1}) {2}", noteContent.getSpanStart(span), noteContent.getSpanEnd(span), span.getClass().toString());
			}
			
			// build the Tree here (starts a recursion from the root)
			appendTree(root, root.start, root.end, blackList);
			
			// convert TagTree to String
			noteXMLContent = writeXML (root);
			TLog.v(TAG, "Built XMLContent: {0}", noteXMLContent);
			
		} catch (Exception e) {
			e.printStackTrace();
			TLog.e(TAG, "There was an error parsing the note.");
			successful = false;
		}
		warnHandler(successful);
	}
	
	// main recursion to build the tree out of spans (Nodes are defined in TagNode.java)
	private void appendTree(TagNode parentNode, int start, int end, List<TagType> blackList) throws Exception {
				
		// its necessary to remember previous span end to be able to find text in between
		int previousEnd = start;
		// also need to remember listlevel and the current parent node (within lists)
		int previousListLevel = 0;
		TagNode parentListNode = parentNode;
		
		// get all spans next to each other (sorted from beginning to end)
		// do NOT include nested spans!
		List<TagNode> siblingNodes = findSiblingNodes(blackList, start, end);
		
		for (TagNode sibling : siblingNodes) {
			int siblingStart = sibling.start;
			int siblingEnd = sibling.end;
			
			// if there is text within spans, add a text-node to the tree
			if (previousEnd != siblingStart) {
				TagNode textNode = getTextNode(previousEnd, siblingStart);
				parentNode.add(textNode);
				TLog.v(TAG, "Added a text-node before a span in the tree: {0}, ({1}/{2})", 
						textNode.text, textNode.start, textNode.end);
				
				// if there is plain text between two lists, reset listlevel and listparent
				// otherwise lists will be merged together and the text moved to the end
				previousListLevel = 0;
				parentListNode = parentNode;
			}
			
			// do some magic to add list tags
			// searches for margin-spans and converts them to the right amount of list nodes
			// and list-item nodes. 
			if (sibling.getType().equals(TagType.MARGIN)) {
				
				int listLeveldiff = sibling.listLevel - previousListLevel;
				previousListLevel = sibling.listLevel;
				
				if (listLeveldiff > 0) {
					// add a new nested list in form of a list node
					for (int i = 0; i < listLeveldiff; i++) {
						// if lists are nested, we need a list-item node in between
						if (parentListNode.getType().equals(TagType.LIST)) {
							TagNode listItem = new TagNode();
							listItem.setType(TagType.LIST_ITEM);
							
							parentListNode.add(listItem);						
							parentListNode = listItem;
							TLog.v(TAG, "Added a list-node in the tree: {0}, ({1}/{2})", 
									parentListNode.getType().toString(), sibling.start, sibling.end);
						}
						
						TagNode listNode = new TagNode();
						listNode.setType(TagType.LIST);
						
						parentListNode.add(listNode);	
						parentListNode = listNode;
						TLog.v(TAG, "Added a list-node in the tree: {0}, ({1}/{2})", 
								parentListNode.getType().toString(), sibling.start, sibling.end);
					}
				} else if (listLeveldiff < 0) {
					// go up in list-levels
					for (int i = 0; i > listLeveldiff; i--) {
						TagNode parent = parentListNode.getParent();
						if (parent.getType().equals(TagType.LIST_ITEM)) {
							i++;
						}
						parentListNode = parent;
					}
				}
				
				// update exclusion list
				blackList.add(sibling.getType());
				
				// recursion for lists starts here
				appendTree(parentListNode, siblingStart, siblingEnd, blackList);
				
			} else {
				
				// add current span as a node to the tree.
				parentNode.add(sibling);
				TLog.v(TAG, "Added a span-node in the tree: {0}, ({1}/{2})", 
						sibling.getType().toString(), sibling.start, sibling.end);
				
				// add the tag we saw already in this tree-branch to the exclusion list
				// necessary to find different spans with equal length just once.
				blackList.add(sibling.getType());
				
				// main recursion starts here (lets go one step down!)
				appendTree(sibling, siblingStart, siblingEnd, blackList);
			}
			
			// remove the tag of this recursion again as the next sibling is allowed to use the tag again
			blackList.remove(sibling.getType());
			// update span end
			previousEnd = sibling.end;
		}
		
		// if there is a text between the previous end and the next start add a text-node again!
		if (previousEnd != end) {
			TagNode textNode = getTextNode(previousEnd, end);
			parentNode.add(textNode);
			TLog.v(TAG, "Added a text-node after a span in the tree: {0}, ({1}/{2})", 
					textNode.text, textNode.start, textNode.end);
		}
		
	}
	
	// function to get all spans within a certain range (span-start and -end must be inside this range)
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
	
	// function to get all neighbouring spans (excluding nested ones) within a certain range
	// Also excludes spans mentioned in the blackList
	private List<TagNode> findSiblingNodes(List<TagType> blackList, int start, int end) {
		List<TagNode> nodes = new LinkedList<TagNode>();
		
		for (Object[] allSpans = getSpansInRange(start, end); 
				allSpans.length > 0; 
				allSpans = getSpansInRange(start, end)) {
			TagNode node = findFirstNode(allSpans, blackList, start, end);
			if (node == null) break;
			nodes.add(node);
			// if a span starts within this one and ends outside, we need to cut it into pieces!
			cutOverlappingSpans(node, blackList);
			start = node.end;
		}
		return nodes;
	}
	
	// function to find the first and biggest span in a certain range
	// span-start must be closest to cursorStart and from those, find the longest (outermost)
	private TagNode findFirstNode (Object[] spans, List<TagType> blackList, int cursorStart, int cursorEnd) {
		int min = cursorEnd;
		int max = cursorStart;
		TagNode returnNode = null;
		
		// get first span
		for (Object span : spans) {
			TagNode node = getNode(span);
			if (!node.getType().equals(TagType.OTHER)
					&& !blackList.contains(node.getType())) {
				if ( node.start < min) {
					min = node.start;
				}
			}
		}
		
		List<TagNode> candidates = new LinkedList<TagNode>();
		
		// in case of multiple spans at this position find the outermost (longest) one
		for (Object span : spans) {
			TagNode node = getNode(span);
			if (!node.getType().equals(TagType.OTHER) 
					&& !blackList.contains(node.getType())
					&& node.start == min ) {
				if ( node.end >= max ) {
					max = node.end;
					returnNode = node;
					candidates.add(node);
				}
			}
		}
		
		// check if bullet span (=list-item) is as long as others, if yes - it must be returned second!
		for (TagNode node : candidates) {
			if (node.end == max && node.getType() == TagType.LIST_ITEM) {
				returnNode = node;
			}
		}
		
		// check if margin span (=list) is as long as others, if yes - it must be returned very first!
		for (TagNode node : candidates) {
			if (node.end == max && node.getType() == TagType.MARGIN) {
				returnNode = node;
			}
		}
		
		return returnNode;
	}
	
	// cut overlapping spans into two spans. one is nested and the other one is a sibling then.
	private void cutOverlappingSpans (TagNode currentNode, List<TagType> blackList) {
		int begin = currentNode.start;
		int end = currentNode.end;
		
		// get all spans from the beginning of the currentNode to end and filter it to avoid cutting
		// OTHER tag and blackList tags
		Object[] spans = getSpansInRange(begin, noteContent.length());
		for (Object span : spans) {
			if (noteContent.getSpanStart(span) < end && noteContent.getSpanEnd(span) > end) {
				TagNode overlappingNode = getNode(span);
				if (overlappingNode != null 
						&& !blackList.contains(overlappingNode.getType())
						&& !overlappingNode.getType().equals(TagType.OTHER)) {
					// actually "split" the span into two pieces
					int oldBegin = noteContent.getSpanStart(span);
					int oldEnd = noteContent.getSpanEnd(span);
					noteContent.setSpan(span, oldBegin, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					Object newSpan = duplicateSpan(span);
					noteContent.setSpan(newSpan, end, oldEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
		}
	}
	
	// function that cuts all spans which cross a margin-span end into two pieces.
	// (And any span starting before and reaching into a list at margin-span start)
	private void cutAllSpansCrossingLists () {
		
		// create a pseudo-list including everything except for margins
		List<TagType> blackList = new LinkedList<TagType>();
		for (TagType value : TagType.values()) {
			if (!value.equals(TagType.MARGIN))
				blackList.add(value);
		}
		
		// get all margin-spans (as all other siblings are black-listed by blackList
		List<TagNode> marginSiblings = findSiblingNodes(blackList, 0, noteContent.length());
		
		// if a span starts before a list and ends within a list, we need to do create  a
		// fake node and use this one to cut overlapping spans at the list-begin
		int firstMarginStart = noteContent.length();
		// calculate the start of the first list item
		for(TagNode marginNode : marginSiblings) {
			if (marginNode.start < firstMarginStart) {
				firstMarginStart = marginNode.start;
			}
		}
		
		TagNode fakeNode = new TagNode();
		fakeNode.start = 0;
		fakeNode.end = firstMarginStart;
		fakeNode.setType(TagType.OTHER);
		// now we need a empty blacklist (as we want to cut anything that crosses our lists
		blackList.clear();
		cutOverlappingSpans(fakeNode, blackList);
		
		// get all margin-spans and cut all other overlapping spans at its borders
		for(TagNode marginNode : marginSiblings) {
			cutOverlappingSpans(marginNode, blackList);
		}
	}
	
	// function that analyses a span and creates a identical new one (to be able to cut a span into 2)
	private Object duplicateSpan(Object span) {
		
		if (span instanceof StyleSpan) {
			int style = ((StyleSpan) span).getStyle();
			StyleSpan newSpan = new StyleSpan(style);
			return newSpan;
		} else if (span instanceof StrikethroughSpan) {
			StrikethroughSpan newSpan = new StrikethroughSpan();
			return newSpan;
		} else if (span instanceof BackgroundColorSpan) {
			int color = ((BackgroundColorSpan) span).getBackgroundColor();
			BackgroundColorSpan newSpan = new BackgroundColorSpan(color);
			return newSpan;
		} else if (span instanceof TypefaceSpan) {
			String family = ((TypefaceSpan) span).getFamily();
			TypefaceSpan newSpan = new TypefaceSpan(family);
			return newSpan;
		} else if (span instanceof RelativeSizeSpan ) {
			float size = ((RelativeSizeSpan) span).getSizeChange();
			RelativeSizeSpan newSpan = new RelativeSizeSpan(size);
			return newSpan;
		} else {
			Object newSpan = null;
			return newSpan;
		}
		
	}

	// returns a text node using the characters between start and end
	private TagNode getTextNode (int start, int end) {
		TagNode node = new TagNode();
		node.setType(TagType.TEXT);
		String text = noteContent.subSequence(start, end).toString();
		node.text = text;
		node.start = start;
		node.end = end;
		return node;
	}
	
	// returns a tag node using the information of the span
	private TagNode getNode (Object span) {
		TagNode node = new TagNode();
		node.start = noteContent.getSpanStart(span);
		node.end = noteContent.getSpanEnd(span);
		
		if( span instanceof StyleSpan ) {
			StyleSpan style = (StyleSpan) span;
			if( (style.getStyle()&Typeface.BOLD)>0 )
			{
				node.setType(TagType.BOLD);
			}
			if( (style.getStyle()&Typeface.ITALIC)>0 )
			{
				node.setType(TagType.ITALIC);
			}
		}
		else if( span instanceof StrikethroughSpan )
		{
			node.setType(TagType.STRIKETHROUGH);
		}

		else if( span instanceof BackgroundColorSpan )
		{
			BackgroundColorSpan bgcolor = (BackgroundColorSpan) span;
			if( bgcolor.getBackgroundColor()==Note.NOTE_HIGHLIGHT_COLOR )
			{
				node.setType(TagType.HIGHLIGHT);
			}
		}
		else if( span instanceof TypefaceSpan )
		{
			TypefaceSpan typeface = (TypefaceSpan) span;
			if( typeface.getFamily()==Note.NOTE_MONOSPACE_TYPEFACE )
			{
				node.setType(TagType.MONOSPACE);
			}
		}
		else if( span instanceof RelativeSizeSpan )
		{
			RelativeSizeSpan size = (RelativeSizeSpan) span;
			if( size.getSizeChange()==Note.NOTE_SIZE_SMALL_FACTOR )
			{
				node.setType(TagType.SIZE_SMALL);
			}
			else if( size.getSizeChange()==Note.NOTE_SIZE_LARGE_FACTOR )
			{
				node.setType(TagType.SIZE_LARGE);
			}
			else if( size.getSizeChange()==Note.NOTE_SIZE_HUGE_FACTOR )
			{
				node.setType(TagType.SIZE_HUGE);
			}
		}
		else if( span instanceof LinkInternalSpan )
		{
			node.setType(TagType.LINK_INTERNAL);
		}
		else if( span instanceof LeadingMarginSpan.Standard )
		{
			LeadingMarginSpan.Standard margin = (LeadingMarginSpan.Standard) span;
			int currentMargin = margin.getLeadingMargin(true);
			node.setType(TagType.MARGIN);
			node.listLevel = currentMargin / Note.NOTE_BULLET_INTENT_FACTOR;
		}
		else if( span instanceof BulletSpan )
		{
			node.setType(TagType.LIST_ITEM);
		}
		else {
			node.setType(TagType.OTHER);
		}
		
		return node;
	}
	
	// Function which starts the conversion from our Tagtree to XML using the XML Serializer
	// returns the whole note content as XML string
	private String writeXML (TagNode root) {
		
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			
			// start a recursion here to walk through the tree
			appendXml(root, serializer);
			
			serializer.endDocument();
			return writer.toString();
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	// Recursion function to walk through the tag-tree and create the right XML tags
	private void appendXml(TagNode node, XmlSerializer serializer){

		TagNode[] children = node.getChildren();
		
		for (TagNode child : children){
			try {
				if (child.getType().equals(TagType.TEXT)) {
					// remove illegal xml characters!
					serializer.text(XmlUtils.removeIllegal(child.text));
				} else {
					serializer.startTag("", child.getTagName());
					if (child.getType().equals(TagType.LIST_ITEM)) {
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
