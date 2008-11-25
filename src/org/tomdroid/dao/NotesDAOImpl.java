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

public class NotesDAOImpl implements NotesDAO {
	//TODO implement runnable and launch a thread from UI to fetch Notes
	
	private String noteURL;
	private String noteContent;
	
	public NotesDAOImpl (String url) {
		this.noteURL = url;
	}
	
	/* (non-Javadoc)
	 * @see org.tomdroid.dao.NotesDAO#getContent()
	 */
	public String getContent() {
		
		if (noteContent != null) {
			return noteContent;
		} else {
			try {
				noteContent = fetch(noteURL);
				return noteContent;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
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
