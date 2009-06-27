package org.tomdroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tomdroid.ui.Tomdroid;
import org.tomdroid.xml.NoteHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

public class NoteCollectingService extends Service {
	private File notesRoot;

	// domain elements
	private NoteCollection noteCollection;

	// Logging info
	private static final String TAG = Tomdroid.TAG;//"NoteCollectingService";

	/* (non-Javadoc)
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		
		noteCollection = NoteCollection.getInstance();
		notesRoot = new File(Tomdroid.NOTES_PATH);
    	try {
    		if (!notesRoot.exists()) {
    			throw new FileNotFoundException("Tomdroid notes folder doesn't exist. It is configured to be at: "+Tomdroid.NOTES_PATH);
    		}//TODO: FNFE not catched anymore
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		  new Thread(new Runnable() {
			    public void run() {
					readAndParseNotes();
					stopSelf();
			    }
			  }).start();
	}


	public void readAndParseNotes() {
		File[] fileList = notesRoot.listFiles(new NotesFilter());
		
		// If there are no notes, warn the UI through an empty message
		if (fileList.length == 0) {
			//TODO: also remove notes from ContentProvider
		}
		
		for (File file : fileList) {

			// give a filename to a thread and ask to parse it when nothing's left to do its over
			parseFile(file);
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


		public void parseFile(File file) {
			Note note = new Note();
			
			note.setFileName(file.getAbsolutePath());
			
			try {
				// Parsing
		    	// XML 
		    	// Get a SAXParser from the SAXPArserFactory
		        SAXParserFactory spf = SAXParserFactory.newInstance();
		        SAXParser sp = spf.newSAXParser();
		
		        // Get the XMLReader of the SAXParser we created
		        XMLReader xr = sp.getXMLReader();
		        
		        // Create a new ContentHandler, send it this note to fill and apply it to the XML-Reader
		        NoteHandler xmlHandler = new NoteHandler(note);
		        xr.setContentHandler(xmlHandler);

		        
		        // Create the proper input source based on if its a local note or a web note
	        	FileInputStream fin = new FileInputStream(file);
				BufferedReader in = new BufferedReader(new InputStreamReader(fin), 8192);
				InputSource is = new InputSource(in);
		        
				if (Tomdroid.LOGGING_ENABLED) Log.v(TAG, "parsing note");
				xr.parse(is);
			
			// TODO wrap and throw a new exception here
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			synchronized (noteCollection) {
				noteCollection.addNote(note);
			}
			
			// notify UI that we are done here and send result 
			updateNoteListWith(note.getTitle());
		}
	    
		private void updateNoteListWith(String noteTitle) {
			
			// get the note instance we will work with that instead  from now on
			Note note = NoteCollection.getInstance().findNoteFromTitle(noteTitle);
			
			// verify if the note is already in the content provider

			// TODO I could see a problem where someone delete a note and recreate one with the same title.
			// It would been seen as not new although it is (it will have a new filename)
			// TODO make the query prettier (use querybuilder)
			Uri notes = Tomdroid.CONTENT_URI;
			String[] whereArgs = new String[1];
			whereArgs[0] = noteTitle;
			Cursor managedCursor = getContentResolver().query( notes,
	                NoteProvider.PROJECTION,  
	                Note.TITLE + "= ?",
	                whereArgs,
	                Note.TITLE + " ASC");
			if (managedCursor.getCount() == 0) {
				
				// This note is not in the database yet we need to insert it
				if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"A new note has been detected (not yet in db)");

				// This add the note to the content Provider
				// TODO PoC code that should be removed in next iteration's refactoring (no notecollection, everything should come from the provider I guess?)
	    		ContentValues values = new ContentValues();
	    		values.put(Note.TITLE, note.getTitle());
	    		values.put(Note.FILE, note.getFileName());
	    		Uri uri = getContentResolver().insert(Tomdroid.CONTENT_URI, values);
	    		// now that we inserted the note put its ID in the note itself
	    		note.setDbId(Integer.parseInt(uri.getLastPathSegment()));

	    		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Note inserted in content provider. ID: "+uri+" TITLE:"+noteTitle+" ID:"+note.getDbId());
			} else {
				
				// find out the note's id and put it in the note
			    if (managedCursor.moveToFirst()) {
			        int idColumn = managedCursor.getColumnIndex(Note.ID);
		            note.setDbId(managedCursor.getInt(idColumn));
			    }
			}
		}


		@Override
		public IBinder onBind(Intent intent) {
			throw new RuntimeException("onBind not supported for NoteCollectingService");
		}
}
