/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008 Olivier Bilodeau <olivier@bottomlesspit.org>
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
