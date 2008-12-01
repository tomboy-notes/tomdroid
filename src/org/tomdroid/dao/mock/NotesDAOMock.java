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

import android.os.Bundle;
import android.os.Message;
import android.os.Handler;

public class NotesDAOMock implements NotesDAO {
	
	// thread related
	private Thread runner;
	private Handler parentHandler;
	
	public NotesDAOMock (Handler handler, String url) {
		parentHandler = handler;
	}
	
	@Override
	public void getContent() {
		runner = new Thread(this);
		runner.start();
	}
	
	@Override
	public void run() {
		Message msg = Message.obtain();
		
		// Load the message object with the note
		Bundle bundle = new Bundle();
		bundle.putString(NotesDAO.NOTE, fetchContent());
		msg.setData(bundle);
		
		// notify UI that we are done here and send result 
		parentHandler.sendMessage(msg);
	}
	
	private String fetchContent() {
		try {
			// simulate delay
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// this is a full faked note from xml-schema/complete-testcase/1331e52c-0a35-4c89-90c7-507bc9.note
		return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "\n" +
				"<note version=\"0.3\" xmlns:link=\"http://beatniksoftware.com/tomboy/link\" xmlns:size=\"http://beatniksoftware.com/tomboy/size\" xmlns=\"http://beatniksoftware.com/tomboy\">" + "\n" +
				"<title>Test Case</title>" + "\n" +
				"<text xml:space=\"preserve\"><note-content version=\"0.1\">Test Case" + "\n" +
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
				"</list-item><list-item>Me too</list-item></list>" + "\n" +
				"</note-content></text>" + "\n" +
				"<last-change-date>2008-10-21T22:17:05.8281250-04:00</last-change-date>" + "\n" +
				"<last-metadata-change-date>2008-10-21T22:17:05.8281250-04:00</last-metadata-change-date>" + "\n" +
				"<create-date>2008-10-21T21:58:25.8906250-04:00</create-date>" + "\n" +
				"<cursor-position>434</cursor-position>" + "\n" +
				"<width>448</width>" + "\n" +
				"<height>360</height>" + "\n" +
				"<x>44</x>" + "\n" +
				"<y>58</y>" + "\n" +
				"<open-on-startup>False</open-on-startup>" + "\n" +
				"</note>";
	}

}
