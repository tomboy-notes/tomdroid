/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010, 2011 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2013 Stefan Hammer <j.4@gmx.at>
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
package org.tomdroid;

import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tomdroid.util.Time;
import org.tomdroid.xml.NoteContentBuilder;
import org.tomdroid.xml.XmlUtils;

import java.io.Serializable;
public class Note implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Static references to fields (used in Bundles, ContentResolvers, etc.)
	public static final String ID = "_id";
	public static final String GUID = "guid";
	public static final String TITLE = "title";
	public static final String MODIFIED_DATE = "modified_date";
	public static final String URL = "url";
	public static final String FILE = "file";
	public static final String TAGS = "tags";
	public static final String NOTE_CONTENT = "content";
	public static final String NOTE_CONTENT_PLAIN = "content_plain";
	
	// Notes constants
	public static final int NOTE_HIGHLIGHT_COLOR = 0x99FFFF00; // lowered alpha to show cursor
	public static final int NOTE_BULLET_INTENT_FACTOR = 30;			// intent factor of bullet lists
	public static final String NOTE_MONOSPACE_TYPEFACE = "monospace";
	public static final float NOTE_SIZE_SMALL_FACTOR = 0.8f;
	public static final float NOTE_SIZE_LARGE_FACTOR = 1.5f;
	public static final float NOTE_SIZE_HUGE_FACTOR = 1.8f;
	
	// Members
	private SpannableStringBuilder noteContent;
	private String xmlContent;
	private String url;
	private String fileName;
	private String title;
	private String tags = "";
	private String lastChangeDate;
	private int dbId;

	// Unused members (for SD Card)
	
	public String createDate = new Time().formatTomboy();
	public int cursorPos = 0;
	public int height = 0;
	public int width = 0;
	public int X = -1;
	public int Y = -1;

	
	// TODO before guid were of the UUID object type, now they are simple strings 
	// but at some point we probably need to validate their uniqueness (per note collection or universe-wide?) 
	private String guid;
	
	// this is to tell the sync service to update the last date after pushing this note
	public boolean lastSync = false;
	
	public Note() {
		tags = new String();
	}
	
	public Note(JSONObject json) {
		
		// These methods return an empty string if the key is not found
		setTitle(XmlUtils.unescape(json.optString("title")));
		setGuid(json.optString("guid"));
		setLastChangeDate(json.optString("last-change-date"));
		String newXMLContent = json.optString("note-content");
		setXmlContent(newXMLContent);
		JSONArray jtags = json.optJSONArray("tags");
		String tag;
		tags = new String();
		if (jtags != null) {
			for (int i = 0; i < jtags.length(); i++ ) {
				tag = jtags.optString(i);
				tags += tag + ",";
			}
		}
	}

	public String getTags() {
		return tags;
	}
	
	public void setTags(String tags) {
		this.tags = tags;
	}
	
	public void addTag(String tag) {
		if(tags.length() > 0)
			this.tags = this.tags+","+tag;
		else
			this.tags = tag;
	}
	
	public void removeTag(String tag) {
		
		String[] taga = TextUtils.split(this.tags, ",");
		String newTags = "";
		for(String atag : taga){
			if(!atag.equals(tag))
				newTags += atag;
		}
		this.tags = newTags;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Time getLastChangeDate() {
		Time time = new Time();
		time.parseTomboy(lastChangeDate);
		return time;
	}
	
	public Time getCreateDate() {
		Time time = new Time();
		// quick and dirty bugfix for synchronisation with Rainy server (have to send create Date)
		//TODO: we should store the createDate in the note!
		time.set(946681200000L);
		return time;
	}
	
	// sets change date to now
	public void setLastChangeDate() {
		Time now = new Time();
		now.setToNow();
		setLastChangeDate(now);
	}
	
	public void setLastChangeDate(Time lastChangeDateTime) {
		this.lastChangeDate = lastChangeDateTime.formatTomboy();
	}
	
	public void setLastChangeDate(String lastChangeDateStr) {

		this.lastChangeDate = lastChangeDateStr;
	}	

	public void setCreateDate(String createDateStr) {		
		this.createDate = createDateStr;
	}
	
	public int getDbId() {
		return dbId;
	}

	public void setDbId(int id) {
		this.dbId = id;
	}
	
	public String getGuid() {
		return guid;
	}
	
	public void setGuid(String guid) {
		this.guid = guid;
	}

	// TODO: should this handler passed around evolve into an observer pattern?
	public SpannableStringBuilder getNoteContent(Handler handler) {
		
		// TODO not sure this is the right place to do this
		noteContent = new NoteContentBuilder().setCaller(handler).setInputSource(xmlContent).setTitle(this.getTitle()).build();
		return noteContent;
	}
	
	public String getXmlContent() {
		return xmlContent;
	}
	
	public void setXmlContent(String xmlContent) {
		this.xmlContent = xmlContent;
	}

	@Override
	public String toString() {

		return new String("Note: "+ getTitle() + " (" + getLastChangeDate() + ")");
	}
	
	// gets full xml to be exported as .note file
	public String getXmlFileString() {
		
		String tagString = "";

		if(tags.length()>0) {
			String[] tagsA = tags.split(",");
			tagString = "\n\t<tags>";
			for(String atag : tagsA) {
				tagString += "\n\t\t<tag>"+atag+"</tag>"; 
			}
			tagString += "\n\t</tags>"; 
		}

		// TODO: create-date
		String fileString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<note version=\"0.3\" xmlns:link=\"http://beatniksoftware.com/tomboy/link\" xmlns:size=\"http://beatniksoftware.com/tomboy/size\" xmlns=\"http://beatniksoftware.com/tomboy\">\n\t<title>"
				+getTitle().replace("&", "&amp;")+"</title>\n\t<text xml:space=\"preserve\"><note-content version=\"0.1\">"
				+getTitle().replace("&", "&amp;")+"\n\n" // added for compatibility
				+getXmlContent()+"</note-content></text>\n\t<last-change-date>"
				+getLastChangeDate()+"</last-change-date>\n\t<last-metadata-change-date>"
				+getLastChangeDate()+"</last-metadata-change-date>\n\t<create-date>"
				+getCreateDate()+"</create-date>\n\t<cursor-position>"
				+cursorPos+"</cursor-position>\n\t<width>"
				+width+"</width>\n\t<height>"
				+height+"</height>\n\t<x>"
				+X+"</x>\n\t<y>"
				+Y+"</y>"
				+tagString+"\n\t<open-on-startup>False</open-on-startup>\n</note>\n";
		return fileString;
	}

}
