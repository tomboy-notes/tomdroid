package org.tomdroid.util;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

import org.tomdroid.Note;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ErrorList extends LinkedList<HashMap<String, Object>> {

	// Eclipse wants this, let's grant his wish
	private static final long serialVersionUID = 2442181279736146737L;
	
	private static class Error extends HashMap<String, Object> {
		
		// Eclipse wants this, let's grant his wish
		private static final long serialVersionUID = -8279130686438869537L;

		public Error addError(Exception e ) {
			this.put("error", e.getStackTrace());
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
	
	public void show(final Activity activity) {
		Uri intentUri = Uri.parse("tomdroid://errors");
		Intent intent = new Intent(Intent.ACTION_VIEW, intentUri);
		Bundle bundle = new Bundle();
		bundle.putStringArray("labels", getLabels());
		bundle.putStringArray("filenames", getFilenames());
		bundle.putStringArray("contents", getContents());
		bundle.putStringArray("errors", getErrors());
		intent.putExtra("org.tomdroid.errors.ShowAll", bundle);
		activity.startActivity(intent);
	}
	
	private String[] getLabels() {
		String[] labels = new String[size()];
		
		for(int i = 0; i < size(); i++) {
			labels[i] = (String)get(i).get("label");
		}
		
		return labels;
	}
	
	private String[] getFilenames() {
		String[] filenames = new String[size()];
		
		for(int i = 0; i < size(); i++) {
			filenames[i] = (String)get(i).get("filename");
		}
		
		return filenames;
	}
	
	private String[] getErrors() {
		String[] errors = new String[size()];
		
		for(int i = 0; i < size(); i++) {
			errors[i] = (String)get(i).get("error");
		}
		
		return errors;
	}
	
	private String[] getContents() {
		String[] contents = new String[size()];
		
		for(int i = 0; i < size(); i++) {
			contents[i] = (String)get(i).get("note-content");
		}
		
		return contents;
	}
}
