/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, 2010, 2011 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, 2010 Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2011 Stefan Hammer <j.4@gmx.at>
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Tomdroid.	If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomdroid.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.R.string;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.TLog;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import android.app.Activity;	
import android.app.AlertDialog;	
import android.app.Dialog;	
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;	
import android.content.DialogInterface;	
import android.content.Intent;	
import android.net.Uri;
import android.os.Bundle;	
import android.text.TextUtils;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
	
public class SyncDialog extends Activity {	
	private static final String TAG = "SyncDialog";
	private Context context;
	
	private Note localNote;
	private boolean differentNotes;
	private Note remoteNote;
	private int dateDiff;
	
	@Override	
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);	
		
		if(!this.getIntent().hasExtra("datediff")) {
			finish();
			return;
		}
		if (Preferences.getString(Preferences.Key.THEME_CHOICE).equals("dark"))
			super.setTheme( R.style.DarkTheme);
		TLog.v(TAG, "starting SyncDialog");
		this.context = this;
		
		setContentView(R.layout.note_compare);
		
		final Bundle extras = this.getIntent().getExtras();

		remoteNote = new Note();
		remoteNote.setTitle(extras.getString("title"));
		remoteNote.setGuid(extras.getString("guid"));
		remoteNote.setLastChangeDate(extras.getString("date"));
		remoteNote.setXmlContent(extras.getString("content"));	
		remoteNote.setTags(extras.getString("tags"));
		
		ContentValues values = new ContentValues();
		values.put(Note.TITLE, extras.getString("title"));
		values.put(Note.FILE, extras.getString("file"));
		values.put(Note.GUID, extras.getString("guid"));
		values.put(Note.MODIFIED_DATE, extras.getString("date"));
		values.put(Note.NOTE_CONTENT, extras.getString("content"));
		values.put(Note.TAGS, extras.getString("tags"));
		 
		dateDiff = extras.getInt("datediff");
		
		// check if we're comparing two different notes with same title
		
		differentNotes = getIntent().hasExtra("localGUID"); 
		if(differentNotes) {
			localNote = NoteManager.getNoteByGuid(this, extras.getString("localGUID"));
			TLog.v(TAG, "comparing two different notes with same title");
		}
		else {
			localNote = NoteManager.getNoteByGuid(this, extras.getString("guid"));
			TLog.v(TAG, "comparing two versions of the same note");
		}
		
		final boolean deleted = localNote.getTags().contains("system:deleted"); 
		
		String message;

		Button localBtn = (Button)findViewById(R.id.localButton);
		Button remoteBtn = (Button)findViewById(R.id.remoteButton);
		Button copyBtn = (Button)findViewById(R.id.copyButton);
		
		TextView messageView = (TextView)findViewById(R.id.message);
		TextView diffView = (TextView)findViewById(R.id.diff);
		TextView diffLabel = (TextView)findViewById(R.id.diff_label);
		TextView localLabel = (TextView)findViewById(R.id.local_label);

		final EditText localEdit = (EditText)findViewById(R.id.local);
		final EditText remoteEdit = (EditText)findViewById(R.id.remote);

		final EditText localTitle = (EditText)findViewById(R.id.local_title);
		final EditText remoteTitle = (EditText)findViewById(R.id.remote_title);

		if(deleted) {
			message = getString(R.string.sync_conflict_deleted);
			
			diffLabel.setVisibility(View.GONE);
			localLabel.setVisibility(View.GONE);
			diffView.setVisibility(View.GONE);
			localEdit.setVisibility(View.GONE);
			localTitle.setVisibility(View.GONE);

			copyBtn.setVisibility(View.GONE);
			
			localBtn.setText(getString(R.string.delete_remote));
			localBtn.setOnClickListener( new View.OnClickListener() {
				public void onClick(View v) {
					onChooseDelete();
				}
	        });
		}
		else {
			String diff = "";
			boolean titleMatch = localNote.getTitle().equals(extras.getString("title"));
			
			if(differentNotes)
				message = getString(R.string.sync_conflict_titles_message);
			else
				message = getString(R.string.sync_conflict_message);
			
			if(!titleMatch) {
				diff = getString(R.string.diff_titles)+"\n"+getString(R.string.local_label)+": "+localNote.getTitle()+"\n"+getString(R.string.remote_label)+": "+extras.getString("title");		
			}

			if(localNote.getXmlContent().equals(extras.getString("content"))){
				if(titleMatch) { // same note, fix the dates
					if(extras.getInt("datediff") < 0) { // local older
						NoteManager.putNote(this, localNote);
					}
					else {
						SyncManager.getInstance().pushNote(localNote);
					}
					finish();				
				}
				else {
		            diffView.setText(diff);
	    			localEdit.setVisibility(View.GONE);
	    			remoteEdit.setVisibility(View.GONE);					
				}
			}
			else {
				if(titleMatch && !differentNotes) {
	    			localTitle.setVisibility(View.GONE);
	    			remoteTitle.setVisibility(View.GONE);
				}
				else
	    			diff += "\n\n";

				Patch patch = DiffUtils.diff(Arrays.asList(TextUtils.split(localNote.getXmlContent(), "\\r?\\n|\\r")), Arrays.asList(TextUtils.split(extras.getString("content"), "\\r?\\n|\\r")));
	            String diffResult = "";
				for (Delta delta: patch.getDeltas()) {
	            	diffResult += delta.toString()+"\n";
	            }
	            
	            Pattern digitPattern = Pattern.compile(".*position: ([0-9]+), lines: ");
	
	            Matcher matcher = digitPattern.matcher(diffResult);
	            StringBuffer result = new StringBuffer();
	            while (matcher.find())
	            {
	                matcher.appendReplacement(result, "Line "+String.valueOf(Integer.parseInt(matcher.group(1)) + 1)+":\n");
	            }
	            matcher.appendTail(result);
				
	            diff += getString(R.string.diff_content)+"\n";		
	            
	            diff += result.toString().replaceAll("\\[([^]]*)\\] to \\[([^]]*)\\]",getString(R.string.local_label)+": $1\n"+getString(R.string.remote_label)+": $2").replaceAll("]$","");
				
	            diffView.setText(diff);
				
			}
			
			localBtn.setOnClickListener( new View.OnClickListener() {
				public void onClick(View v) {
	            	// take local
					TLog.v(TAG, "user chose local version for note TITLE:{0} GUID:{1}", localNote.getTitle(),localNote.getGuid());
					onChooseNote(localTitle.getText().toString(),localEdit.getText().toString(), true);
				}
	        });
			localTitle.setText(localNote.getTitle());
			localEdit.setText(localNote.getXmlContent());
		}
		
		messageView.setText(String.format(message,localNote.getTitle()));
		remoteTitle.setText(extras.getString("title"));
		remoteEdit.setText(extras.getString("content"));

		remoteBtn.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
            	// take local
				TLog.v(TAG, "user chose remote version for note TITLE:{0} GUID:{1}", localNote.getTitle(),localNote.getGuid());
				onChooseNote(remoteTitle.getText().toString(),remoteEdit.getText().toString(), false);
			}
        });
		
		copyBtn.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
            	// take local
				TLog.v(TAG, "user chose to create copy for note TITLE:{0} GUID:{1}", localNote.getTitle(),localNote.getGuid());
				copyNote();
			}
        });
	}

	protected void copyNote() {
		
		TLog.v(TAG, "user chose to create new copy for conflicting note TITLE:{0} GUID:{1}", localNote.getTitle(),localNote.getGuid());

		// not doing a title difference, get new guid for new note
		
		if(!differentNotes) {
			UUID newid = UUID.randomUUID();
			remoteNote.setGuid(newid.toString());
		}
		
		String localTitle = ((EditText)findViewById(R.id.local_title)).getText().toString();
		String remoteTitle = ((EditText)findViewById(R.id.remote_title)).getText().toString();
		localNote.setTitle(localTitle);
		remoteNote.setTitle(remoteTitle);
		
		if(!localNote.getTitle().equals(remoteNote.getTitle())) { // different titles, just create new note
		}
		else {
			
			// validate against existing titles
			String newTitle = NoteManager.validateNoteTitle(this, String.format(getString(R.string.old),localNote.getTitle()), localNote.getGuid());
			
			if(dateDiff < 0) { // local older, rename local
				localNote.setTitle(newTitle);
				NoteManager.putNote(SyncDialog.this, localNote); // update local note with new title
			}
			else { // remote older, rename remote
				remoteNote.setTitle(newTitle);
			}
		}
			
		// add remote note to local
		NoteManager.putNote(SyncDialog.this, remoteNote);

		// push both
		SyncManager.getInstance().pushNote(localNote);
		SyncManager.getInstance().pushNote(remoteNote);
		finish();	
	}

	protected void onChooseNote(String title, String content, boolean choseLocal) {
		
		title = NoteManager.validateNoteTitle(this, title, localNote.getGuid());
		
		Time now = new Time();
		now.setToNow();
		String time = now.format3339(false);
		
		localNote.setTitle(title);
		localNote.setXmlContent(content);
		localNote.setLastChangeDate(time);

		// doing a title difference

		if(differentNotes) {
			if(choseLocal) { // chose to keep local, delete remote, push local
				NoteManager.putNote(this, localNote);
				remoteNote.addTag("system:deleted");
				
				ArrayList<Note> notes = new ArrayList<Note>();
				notes.add(localNote); // add for pushing
				notes.add(remoteNote); // add for deletion
				
				SyncManager.getInstance().getCurrentService().pushNotes(notes);
			}
			else { // chose to keep remote, delete local, add remote, push remote back 
				NoteManager.deleteNote(this, localNote);
				remoteNote.setTitle(title);
				remoteNote.setXmlContent(content);
				remoteNote.setLastChangeDate(time);
				NoteManager.putNote(this, remoteNote);
				SyncManager.getInstance().pushNote(remoteNote);
			}
		}
		else { // just readd and push modified localNote
			NoteManager.putNote(SyncDialog.this, localNote);
			SyncManager.getInstance().pushNote(localNote);
		}

		finish();
	}
	
	protected void onChooseDelete() {
		TLog.v(TAG, "user chose to delete remote note TITLE:{0} GUID:{1}", localNote.getTitle(),localNote.getGuid());
		SyncManager.getInstance().deleteNote(localNote.getGuid()); // delete from remote
		NoteManager.deleteNote(SyncDialog.this, localNote.getDbId()); // really delete locally
		finish();
	}
}	