package org.tomdroid;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.tomdroid.Note;
import org.json.JSONException;
import org.json.JSONObject;


public class NoteTest extends TestCase {

	public void testConstructorForNoteWithTags() throws JSONException {
		JSONObject json = new JSONObject(
				"{'title': 'foo', 'note-content': 'bar', " +
				"'guid': '002e91a2-2e34-4e2d-bf88-21def49a7705', " +
				"'last-change-date': '2009-04-19T21:29:23.2197340-07:00', " +
				"'tags': ['tag1', 'tag2']}");
		Note n = new Note(json);
		Assert.assertEquals("foo", n.getTitle());	
		Assert.assertEquals("002e91a2-2e34-4e2d-bf88-21def49a7705", n.getGuid().toString());
		Assert.assertEquals("bar", n.getXmlContent());	
		Assert.assertEquals("tag1,tag2,", n.getTags());
	}

	public void testConstructorForNoteWithNoTags() throws JSONException {
		JSONObject json = new JSONObject(
				"{'title': 'foo', 'note-content': 'bar', " +
				"'guid': '002e91a2-2e34-4e2d-bf88-21def49a7705', " +
				"'last-change-date': '2009-04-19T21:29:23.2197340-07:00'}");
		Note n = new Note(json);
		Assert.assertEquals("foo", n.getTitle());
		Assert.assertEquals("", n.getTags());
	}
}
