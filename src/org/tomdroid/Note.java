/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010, 2011 Olivier Bilodeau <olivier@bottomlesspit.org>
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
package org.tomdroid;

import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.format.Time;
import android.util.TimeFormatException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tomdroid.util.NoteContentBuilder;
import org.tomdroid.util.XmlUtils;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Note {

	// Static references to fields (used in Bundles, ContentResolvers, etc.)
	public static final String ID = "_id";
	public static final String GUID = "guid";
	public static final String TITLE = "title";
	public static final String MODIFIED_DATE = "modified_date";
	public static final String FILE = "file";
	public static final String TAGS = "tags";
	public static final String NOTE_CONTENT = "content";
	
	// Notes constants
	public static final int NOTE_HIGHLIGHT_COLOR = 0x99FFFF00; // lowered alpha to show cursor
	public static final String NOTE_MONOSPACE_TYPEFACE = "monospace";
	public static final float NOTE_SIZE_SMALL_FACTOR = 0.8f;
	public static final float NOTE_SIZE_LARGE_FACTOR = 1.5f;
	public static final float NOTE_SIZE_HUGE_FACTOR = 1.8f;

    private String xmlContent;
	private String fileName;
	private String title;
	private HashSet<String> tags = new HashSet<String>();
	private Time lastChangeDate;
	private int dbId;

	// Unused members (for SD Card)
	
	public Time createDate = new Time();
	public int cursorPos = 0;
	public int height = 0;
	public int width = 0;
	public int X = -1;
	public int Y = -1;

	
	// TODO before guid were of the UUID object type, now they are simple strings 
	// but at some point we probably need to validate their uniqueness (per note collection or universe-wide?) 
	private String guid;
	
	// Date converter pattern (remove extra sub milliseconds from datetime string)
	// ex: will strip 3020 in 2010-01-23T12:07:38.7743020-05:00
	private static final Pattern dateCleaner = Pattern.compile(
			"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})" +	// matches: 2010-01-23T12:07:38.774
			".+" + 														// matches what we are getting rid of
			"([-\\+]\\d{2}:\\d{2})");									// matches timezone (-xx:xx or +xx:xx)
	
	public Note(JSONObject json) {
		
		// These methods return an empty string if the key is not found
		setTitle(XmlUtils.unescape(json.optString("title")));
		setGuid(json.optString("guid"));
		setLastChangeDate(json.optString("last-change-date"));
		String newXMLContent = json.optString("note-content");
		setXmlContent(newXMLContent);
		JSONArray jtags = json.optJSONArray("tags");
        if (jtags == null) return;
        for (int i = 0; i < jtags.length(); i++ ) tags.add(jtags.optString(i));
    }

    public Note() {}

    public HashSet<String> getTags() {
		return tags;
	}

    public String getTagsCommaSeparated() {
        StringBuilder sb = new StringBuilder();
        Iterator<String> tagsIterator = tags.iterator();
        while (tagsIterator.hasNext()) sb.append(tagsIterator.next()).append(tagsIterator.hasNext() ? "," : "");
        return sb.toString();
    }
	
	public void setTags(HashSet<String> tags) {
		this.tags = tags;
	}
	
	public void addTag(String tag) {
        tags.add(tag);
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
		return lastChangeDate;
	}

	public void setLastChangeDate(Time lastChangeDate) {
		this.lastChangeDate = lastChangeDate;
	}
	
	public void setLastChangeDate(String lastChangeDateStr) throws TimeFormatException {
		
		// regexp out the sub-milliseconds from tomboy's datetime format
		// Normal RFC 3339 format: 2008-10-13T16:00:00.000-07:00
		// Tomboy's (C# library) format: 2010-01-23T12:07:38.7743020-05:00
		Matcher m = dateCleaner.matcher(lastChangeDateStr);
		if (m.find()) {
			//TLog.d(TAG, "I had to clean out extra sub-milliseconds from the date");
			lastChangeDateStr = m.group(1)+m.group(2);
			//TLog.v(TAG, "new date: {0}", lastChangeDateStr);
		}
		
		lastChangeDate = new Time();
		lastChangeDate.parse3339(lastChangeDateStr);
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
        return new NoteContentBuilder().setCaller(handler).setInputSource(xmlContent).setTitle(this.getTitle()).build();
	}
	
	public String getXmlContent() {
		return xmlContent;
	}
	
	public void setXmlContent(String xmlContent) {
		this.xmlContent = xmlContent;
	}

	@Override
	public String toString() {
		return MessageFormat.format("Note: {0} ({1})", getTitle(), getLastChangeDate());
	}
	
}
