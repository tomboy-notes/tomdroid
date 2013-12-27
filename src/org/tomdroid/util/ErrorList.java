/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2010, Benoit Garret <benoit.garret_launchpad@gadz.org>
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;

import org.tomdroid.Note;
import org.tomdroid.ui.Tomdroid;

public class ErrorList extends LinkedList<HashMap<String, Object>> {
	
	// Eclipse wants this, let's grant his wish
	private static final long serialVersionUID = 2442181279736146737L;
	
	private static class Error extends HashMap<String, Object> {
		
		// Eclipse wants this, let's grant his wish
		private static final long serialVersionUID = -8279130686438869537L;

		public Error addError(Exception e ) {
			Writer result = new StringWriter();
			PrintWriter printWriter = new PrintWriter(result);
			e.printStackTrace(printWriter);
			this.put("error", result.toString());
			return this;
		}
		
		public Error addError(String message) {
			this.put("error", message);
			return this;
		}
		
		public Error addNote(Note note) {
			this.put("label", note.getTitle());
			this.put("filename", new File(note.getFileName()).getName());
			return this;
		}
		
		public Error addObject(String key, Object o) {
			this.put(key, o);
			return this;
		}
	}
	
	public static HashMap<String, Object> createError(Note note, Exception e) {
		return new Error()
			.addNote(note)
			.addError(e);
	}
	
	public static HashMap<String, Object> createError(String label, String filename, Exception e) {
		return new Error()
			.addError(e)
			.addObject("label", label)
			.addObject("filename", filename);
	}
	
	public static HashMap<String, Object> createErrorWithContents(Note note, Exception e, String noteContents) {
		return new Error()
			.addNote(note)
			.addError(e)
			.addObject("note-content", noteContents);
	}
	
	public static HashMap<String, Object> createErrorWithContents(Note note, String message, String noteContents) {
		return new Error()
			.addNote(note)
			.addError(message)
			.addObject("note-content", noteContents);
	}
	
	public static HashMap<String, Object> createErrorWithContents(String label, String filename, Exception e, String noteContents) {
		return new Error()
			.addObject("label", label)
			.addObject("filename", filename)
			.addError(e)
			.addObject("note-content", noteContents);
	}
	
	/**
	 * Saves the error list in an "errors" directory located in the notes directory.
	 * Both the exception and the note content are saved.
	 * @return true if the save was successful, false if it wasn't
	 */
	public boolean save() {
		String path = Tomdroid.NOTES_PATH+"errors/";
		
		File fPath = new File(path);
		if (!fPath.exists()) {
			fPath.mkdirs();
			// Check a second time, if not the most likely cause is the volume doesn't exist
			if(!fPath.exists()) return false;
		}
		
		if(this == null || this.isEmpty() || this.size() == 0)
			return false;
		
		for(int i = 0; i < this.size(); i++) {
			HashMap<String, Object> error = this.get(i);
			if(error == null)
				continue;
			String filename = findFilename(path, (String)error.get("filename"), 0);
			
			try {
				FileWriter fileWriter;
				String content = (String)error.get("note-content");
				
				if(content != null) {
					fileWriter = new FileWriter(path+filename);
					fileWriter.write(content);
					fileWriter.flush();
					fileWriter.close();
				}
				
				fileWriter = new FileWriter(path+filename+".exception");
				fileWriter.write((String)error.get("error"));
				fileWriter.flush();
				fileWriter.close();
			} catch (FileNotFoundException e) {
			 // TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
			 // TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	/**
	 * Finds a not existing filename to write the error.
	 * @param path The directory in which to save the error
	 * @param baseName The base filename of the error
	 * @param level The number that get appended to the filename
	 * @return A filename that doesn't exists in the path directory
	 */
	private String findFilename(String path, String baseName, int level) {
		
		if(level < 0) level = 0;
		
		String suffix = ""+(level == 0 ? "" : level);
		String filePath = path+baseName+suffix;
		File file = new File(filePath);
		
		return file.exists() ? findFilename(path, baseName, level + 1) : baseName+suffix;		
	}
}
