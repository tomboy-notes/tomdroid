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
package org.tomdroid.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class NoteNetworkDAOImpl implements NoteDAO {
	
	private String noteURL;
	private String noteContent;
	
	// thread related
	private Thread runner;
	private Handler parentHandler;
	
	
	public NoteNetworkDAOImpl (Handler handler, String url) {
		parentHandler = handler;
		this.noteURL = url;
	}
	
	
	public void getContent() {
		runner = new Thread(this);
		runner.start();
	}
	
	public void run() {
		Message msg = Message.obtain();
		
		// Grab the note
		// TODO handle exceptions properly
		try {
			noteContent = fetch(noteURL);
		} catch (MalformedURLException e) {
			// TODO handle exceptions properly
			e.printStackTrace();
		} catch (IOException e) {
			// TODO handle exceptions properly
			e.printStackTrace();
		} 
		
		// Load the message object with the note
		Bundle bundle = new Bundle();
		bundle.putString(NoteDAO.NOTE, noteContent);
		msg.setData(bundle);
		
		// notify UI that we are done here and send result 
		parentHandler.sendMessage(msg);
	}	
	
	/**
	 * Grab the content at the target address and convert it to a string.
	 * @param address
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private String fetch(String address) throws MalformedURLException, IOException {
		
		//grab URL
		URL url = new URL(address);
		InputStream is = (InputStream) url.getContent();
		
		//Init BufferedReader and StringBuilder
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		//Convert from InputStream to String using StringBuilder
		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();

		//Return the string
		return sb.toString();
	}
}
