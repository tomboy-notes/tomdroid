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

import org.tomdroid.ui.Tomdroid;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.util.Log;

public class NoteXMLContentBuilder implements Runnable {
	
	public static final int PARSE_OK = 0;
	public static final int PARSE_ERROR = 1;
	
	private SpannableStringBuilder noteContent = null;
	
	// this is what we are building here
	private String noteXMLContent = new String();
	
	private final String TAG = "NoteBuilder";
	
	// thread related
	private Thread runner;
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
			// TODO implement XML writer
			noteXMLContent=noteContent.toString();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO handle error in a more granular way
			Log.e(TAG, "There was an error parsing the note.");
			successful = false;
		}
		
		warnHandler(successful);
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
