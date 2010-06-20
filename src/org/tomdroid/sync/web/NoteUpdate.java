package org.tomdroid.sync.web;

import org.json.JSONException;
import org.json.JSONObject;

public class NoteUpdate {

	private String	guid;
	private String	title;
	private String	content;

	public NoteUpdate(JSONObject jsonNote) throws JSONException {
		guid = jsonNote.getString("guid");
		title = jsonNote.getString("title");
		content = jsonNote.getString("note-content");
	}

	public JSONObject toJson() throws JSONException {
		return new JSONObject("{'guid':'" + guid + "', 'title':'" + title + "', 'note-content':'"
				+ content + "'}");
	}
}
