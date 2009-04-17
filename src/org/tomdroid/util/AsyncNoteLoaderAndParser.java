/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009 Olivier Bilodeau <olivier@bottomlesspit.org>
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
import java.io.FilenameFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.tomdroid.Note;

public class AsyncNoteLoaderAndParser implements Runnable {
	private final ExecutorService pool;
	private final static int poolSize = 5;
	private File path;
	
	public AsyncNoteLoaderAndParser(File path) {
		this.path = path;
		pool = Executors.newFixedThreadPool(poolSize);
	}

	@Override
	public void run() {
				
		for (File file : path.listFiles(new NotesFilter())) {

//			Note note = new Note(hndl, file);
//			
//			note.fetchAndParseNoteFromFileSystemAsync();
//			notes.add(note);
        }
		
		// give a filename to a thread and ask to parse it when nothing's left to do its over
		pool.execute(new Handler());
	}


	 class Handler implements Runnable {
		 public void run() {
			 // do the heavy lifting here
		 }
	}
	
	/**
	 * Simple filename filter that grabs files ending with .note
	 * TODO move into its own static class in a util package
	 */
	class NotesFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return (name.endsWith(".note"));
		}
	}
}
