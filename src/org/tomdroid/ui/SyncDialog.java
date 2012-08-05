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
	
	private Note note;
	private Uri uri;
	
	@Override	
	public void onCreate(Bundle savedInstanceState) {	
		TLog.v(TAG, "starting SyncDialog");
		super.onCreate(savedInstanceState);	
		this.context = this;
		
		setContentView(R.layout.note_compare);
		
		final Bundle extras = this.getIntent().getExtras();

		ContentValues values = new ContentValues();
		values.put(Note.TITLE, extras.getString("title"));
		values.put(Note.FILE, extras.getString("file"));
		values.put(Note.GUID, extras.getString("guid"));
		values.put(Note.MODIFIED_DATE, extras.getString("date"));
		values.put(Note.NOTE_CONTENT, extras.getString("content"));
		values.put(Note.TAGS, extras.getString("tags"));

		uri = Uri.parse(extras.getString("uri"));
		
		note = NoteManager.getNote(this, uri);
		
		final ContentResolver cr = getContentResolver();
		final boolean deleted = note.getTags().contains("system:deleted"); 
		
		String message;

		Button localBtn = (Button)findViewById(R.id.localButton);
		Button remoteBtn = (Button)findViewById(R.id.remoteButton);
		Button copyBtn = (Button)findViewById(R.id.copyButton);
		
		TextView messageView = (TextView)findViewById(R.id.message);
		TextView diffView = (TextView)findViewById(R.id.diff);
		final EditText localEdit = (EditText)findViewById(R.id.local);
		final EditText remoteEdit = (EditText)findViewById(R.id.remote);

		final EditText localTitle = (EditText)findViewById(R.id.local_title);
		final EditText remoteTitle = (EditText)findViewById(R.id.remote_title);
		
		if(deleted) {
			message = getString(R.string.sync_conflict_deleted);
			
			diffView.setVisibility(View.GONE);
			localEdit.setVisibility(View.GONE);
			localTitle.setVisibility(View.GONE);

			copyBtn.setVisibility(View.GONE);
			
			localBtn.setText(getString(R.string.delete_remote));
			localBtn.setOnClickListener( new View.OnClickListener() {
				public void onClick(View v) {
	            	// take local
					TLog.v(TAG, "user chose to delete remote note TITLE:{0} GUID:{1}", note.getTitle(),note.getGuid());
					SyncManager.getInstance().deleteNote(note.getGuid()); // delete from remote
					NoteManager.deleteNote(SyncDialog.this, note.getDbId()); // really delete locally
				}
	        });
		}
		else {
			message = getString(R.string.sync_conflict_message);
			Patch patch = DiffUtils.diff(Arrays.asList(TextUtils.split(note.getXmlContent(), "\\r?\\n|\\r")), Arrays.asList(TextUtils.split(extras.getString("content"), "\\r?\\n|\\r")));
			String diff = "";
            for (Delta delta: patch.getDeltas()) {
            	diff += delta.toString()+"\n";
            }
            
            Pattern digitPattern = Pattern.compile(".*position: ([0-9]+), lines: ");

            Matcher matcher = digitPattern.matcher(diff);
            StringBuffer result = new StringBuffer();
            while (matcher.find())
            {
                matcher.appendReplacement(result, "Line "+String.valueOf(Integer.parseInt(matcher.group(1)) + 1)+":\n");
            }
            matcher.appendTail(result);
            diff = result.toString();
            
            diff = diff.replaceAll("\\[([^]]*)\\] to \\[([^]]*)\\]","Local: $1\nRemote: $2").replaceAll("]$","");
			
            
            
            diffView.setText(diff);
			localBtn.setOnClickListener( new View.OnClickListener() {
				public void onClick(View v) {
	            	// take local
					TLog.v(TAG, "user chose local version for note TITLE:{0} GUID:{1}", note.getTitle(),note.getGuid());
					onChooseNote(localTitle.getText().toString(),localEdit.getText().toString());
				}
	        });
			localTitle.setText(note.getTitle());
			localEdit.setText(note.getXmlContent());
		}
		
		messageView.setText(String.format(message,note.getTitle()));
		remoteTitle.setText(extras.getString("title"));
		remoteEdit.setText(extras.getString("content"));

		remoteBtn.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
            	// take local
				TLog.v(TAG, "user chose remote version for note TITLE:{0} GUID:{1}", note.getTitle(),note.getGuid());
				onChooseNote(remoteTitle.getText().toString(),remoteEdit.getText().toString());
			}
        });
		
		copyBtn.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
            	// take local
				TLog.v(TAG, "user chose to create copy for note TITLE:{0} GUID:{1}", note.getTitle(),note.getGuid());
				copyNote(extras);
			}
        });
	}

	protected void copyNote(Bundle extras) {
		final int dateDiff = extras.getInt("datediff");
    	
		TLog.v(TAG, "user chose to create new copy for conflicting note TITLE:{0} GUID:{1}", note.getTitle(),note.getGuid());

		UUID newid = UUID.randomUUID();

		Note rnote = new Note();
		rnote.setTitle(extras.getString("title"));
		rnote.setTags(extras.getString("tags"));
		rnote.setGuid(newid.toString());
		rnote.setLastChangeDate(extras.getString("date"));
		rnote.setXmlContent(extras.getString("content"));
		
		if(!note.getTitle().equals(extras.getString("title"))) { // different titles, just create new note
		}
		else if(dateDiff < 0) { // local older, rename local
			note.setTitle(String.format(getString(R.string.old),note.getTitle()));
			NoteManager.putNote(SyncDialog.this, note, false);
		}
		else { // remote older, rename remote
			rnote.setTitle(String.format(getString(R.string.old),rnote.getTitle()));
		}
		
		// create note
		NoteManager.putNote(SyncDialog.this, rnote, false);

		// push both
		
		SyncManager.getInstance().pushNote(note);
		SyncManager.getInstance().pushNote(rnote);
		finish();	
	}

	protected void onChooseNote(String title, String content) {
		note.setTitle(title);
		note.setXmlContent(content);
		Time now = new Time();
		now.setToNow();
		String time = now.format3339(false);
		note.setLastChangeDate(time);
		NoteManager.putNote(SyncDialog.this, note, false);
		SyncManager.getInstance().pushNote(note);
		finish();
	}	
}	