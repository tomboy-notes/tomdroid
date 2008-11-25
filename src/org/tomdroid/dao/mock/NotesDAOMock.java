package org.tomdroid.dao.mock;

import org.tomdroid.dao.NotesDAO;

public class NotesDAOMock implements NotesDAO {
	
	public NotesDAOMock(String url){

	}

	@Override
	public String getContent() {
		return "<note-content version=\"0.1\">Test Case" + "\n" +
		"This is a test case trying to make Tomboy write to a note most of its XML note format attributes/element." + "\n" +
		"This is <bold><link:broken>bold</link:broken></bold>." + "\n" +
		"This is <italic>italic</italic>." + "\n" +
		"This is <strikethrough>striked</strikethrough>." + "\n" +
		"This is <highlight>highlighted</highlight>." + "\n" +
		"<monospace>This has fixed width.</monospace>" + "\n" +
		"What about fontsize?" + "\n" +
		"<size:small>This is small</size:small>" + "\n" +
		"This is Normal" + "\n" +
		"<size:large>This is Large</size:large>" + "\n" +
		"<size:huge>This is Huge</size:huge>" + "\n" +
		"Bullets" + "\n" +
		"<list><list-item>I am a bullet" + "\n" +
		"</list-item><list-item>Me too</list-item></list>";
	}

}
