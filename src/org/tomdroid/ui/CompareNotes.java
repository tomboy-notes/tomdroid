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
import org.tomdroid.ui.actionbar.ActionBarActivity;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.TLog;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import android.app.Activity;	
import android.app.AlertDialog;	
import android.app.Dialog;	
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;	
import android.content.DialogInterface;	
import android.content.Intent;	
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;	
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
	
public class CompareNotes extends ActionBarActivity {	
	private static final String TAG = "SyncDialog";
	
	private Note localNote;
	private boolean differentNotes;
	private Note remoteNote;
	private int dateDiff;
	private boolean noRemote;

	private Uri uri;

	private static ProgressDialog syncProgressDialog;
	
	@Override	
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);	
		
		if(!this.getIntent().hasExtra("datediff")) {
			TLog.v(TAG, "no date diff");
			finish();
			return;
		}
		TLog.v(TAG, "starting SyncDialog");
		
		setContentView(R.layout.note_compare);
		
		String serviceDescription = SyncManager.getInstance().getCurrentService().getDescription();

		syncProgressDialog = new ProgressDialog(this);
		syncProgressDialog.setTitle(String.format(getString(R.string.syncing),serviceDescription));
		syncProgressDialog.setMessage(getString(R.string.syncing_connect));
        syncProgressDialog.setCancelable(true);
        syncProgressDialog.setIndeterminate(true);
		
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
		noRemote = extras.getBoolean("noRemote");
		
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
		
		final ToggleButton diffLabel = (ToggleButton)findViewById(R.id.diff_label);
		final ToggleButton localLabel = (ToggleButton)findViewById(R.id.local_label);
		final ToggleButton remoteLabel = (ToggleButton)findViewById(R.id.remote_label);

		final EditText localTitle = (EditText)findViewById(R.id.local_title);
		final EditText remoteTitle = (EditText)findViewById(R.id.remote_title);
		
		final TextView diffView = (TextView)findViewById(R.id.diff);
		final EditText localEdit = (EditText)findViewById(R.id.local);
		final EditText remoteEdit = (EditText)findViewById(R.id.remote);


		updateTextAttributes(localTitle, localEdit);
		updateTextAttributes(remoteTitle, remoteEdit);
		
		if(deleted) {
			TLog.v(TAG, "comparing deleted with remote");
			message = getString(R.string.sync_conflict_deleted);
			
			diffLabel.setVisibility(View.GONE);
			localLabel.setVisibility(View.GONE);
			diffView.setVisibility(View.GONE);
			localEdit.setVisibility(View.GONE);
			localTitle.setVisibility(View.GONE);

			copyBtn.setVisibility(View.GONE);
			
			// if importing note, offer cancel import option to open main screen
			if(noRemote) {
				localBtn.setText(getString(R.string.btnCancelImport));
				localBtn.setOnClickListener( new View.OnClickListener() {
					public void onClick(View v) {
						finishForResult();
					}
		        });
			}
			else {
				localBtn.setText(getString(R.string.delete_remote));
				localBtn.setOnClickListener( new View.OnClickListener() {
					public void onClick(View v) {
						onChooseDelete();
					}
		        });
			}
		}
		else {
			String diff = "";
			boolean titleMatch = localNote.getTitle().equals(extras.getString("title"));
			
			if(differentNotes)
				message = getString(R.string.sync_conflict_titles_message);
			else
				message = getString(R.string.sync_conflict_message);
			
			if(!titleMatch) {
				diff = "<b>"+getString(R.string.diff_titles)+"</b><br/><i>"+getString(R.string.local_label)+"</i><br/> "+localNote.getTitle()+"<br/><br/><i>"+getString(R.string.remote_label)+"</i><br/>"+extras.getString("title");		
			}

			if(localNote.getXmlContent().equals(extras.getString("content").replaceAll("</*link:[^>]+>", ""))) {
				TLog.v(TAG, "compared notes have same content");
				if(titleMatch) { // same note, fix the dates
					if(extras.getInt("datediff") < 0) { // local older
						TLog.v(TAG, "compared notes have same content and titles, pulling newer remote");
						uri = NoteManager.putNote(this, remoteNote);
					}
					else if(extras.getInt("datediff") == 0 || noRemote) {
						TLog.v(TAG, "compared notes have same content and titles, same date, doing nothing");
						uri = Uri.parse(Tomdroid.CONTENT_URI+"/"+NoteManager.getNoteIdByGUID(this, localNote.getGuid()));
					}
					else {
						TLog.v(TAG, "compared notes have same content and titles, pushing newer local");
						ArrayList<Note> notes = new ArrayList<Note>();
						notes.add(localNote);
						SyncManager.getInstance().getCurrentService().pushNotes(notes);
						syncProgressDialog.setMessage(getString(R.string.syncing_remote));
						syncProgressDialog.show();
						return;
					}
					
					if(noRemote) {
						TLog.v(TAG, "compared notes have same content and titles, showing note");
						finishForResult();
					}
					else
						finish();
					
					return;
				}
				else {
					TLog.v(TAG, "compared notes have different titles");
		            diffView.setText(diff);
	    			localEdit.setVisibility(View.GONE);
	    			remoteEdit.setVisibility(View.GONE);					
				}
			}
			else {
				TLog.v(TAG, "compared notes have different content");
				if(titleMatch && !differentNotes) {
	    			localTitle.setVisibility(View.GONE);
	    			remoteTitle.setVisibility(View.GONE);
				}
				else
	    			diff += "<br/><br/>";

				Patch patch = DiffUtils.diff(Arrays.asList(TextUtils.split(localNote.getXmlContent(), "\\r?\\n|\\r")), Arrays.asList(TextUtils.split(extras.getString("content"), "\\r?\\n|\\r")));
	            String diffResult = "";
				for (Delta delta: patch.getDeltas()) {
	            	diffResult += delta.toString()+"<br/>";
	            }

	            Pattern firstPattern = Pattern.compile("\\[ChangeDelta, position: ([0-9]+), lines: \\[([^]]+)\\] to \\[([^]]+)\\]\\]");
	            Pattern secondPattern = Pattern.compile("\\[InsertDelta, position: ([0-9]+), lines: \\[([^]]+)\\]\\]");
	            Pattern thirdPattern = Pattern.compile("\\[DeleteDelta, position: ([0-9]+), lines: \\[([^]]+)\\]\\]");
	        	
	            Matcher matcher = firstPattern.matcher(diffResult);
	            StringBuffer result = new StringBuffer();
	            while (matcher.find())
	            {
	                matcher.appendReplacement(
	                	result, 
                		"<b>"+String.format(getString(R.string.line_x),String.valueOf(Integer.parseInt(matcher.group(1)) + 1))+"</b><br/><i>"
                		+getString(R.string.local_label)+":</i><br/>"+matcher.group(2)+"<br/><br/><i>"
                		+getString(R.string.remote_label)+":</i><br/>"+matcher.group(3)+"<br/>"
	                );
	            }
	            matcher.appendTail(result);

	            matcher = secondPattern.matcher(result);
	            result = new StringBuffer();
	            while (matcher.find())
	            {
	                matcher.appendReplacement(
	                	result, 
						"<b>"+String.format(getString(R.string.line_x),String.valueOf(Integer.parseInt(matcher.group(1)) + 1))+"</b><br/><i>"
	                	+getString(R.string.remote_label)+":</i><br/>"+matcher.group(2)+"<br/><br/>"

	                );
	            }
	            matcher.appendTail(result);

	            matcher = thirdPattern.matcher(result);
	            result = new StringBuffer();
	            while (matcher.find())
	            {
	                matcher.appendReplacement(
	                	result, 
						"<b>"+String.format(getString(R.string.line_x),String.valueOf(Integer.parseInt(matcher.group(1)) + 1))+"</b><br/><i>"
	                	+getString(R.string.local_label)+":</i><br/>"+matcher.group(2)+"<br/><br/>"

	                );
	            }
	            matcher.appendTail(result);
	            
				diff += "<b>"+getString(R.string.diff_content)+"</b><br/>";		
	            
	            diff += result;
				
	            diff = diff.replace("\n","<br/>");
	            
	            diffView.setText(Html.fromHtml(diff));
				
			}
			
			if(noRemote) {
				localBtn.setText(getString(R.string.btnCancelImport));
				message = getString(R.string.sync_conflict_import_message);
			}
			
			localBtn.setOnClickListener( new View.OnClickListener() {
				public void onClick(View v) {

					// check if there is no remote (e.g. we are receiving a note file that conflicts with a local note - see Receive.java), just finish
					if(noRemote)
						finish();
					else {
						// take local
						TLog.v(TAG, "user chose local version for note TITLE:{0} GUID:{1}", localNote.getTitle(),localNote.getGuid());
						onChooseNote(localTitle.getText().toString(),localEdit.getText().toString(), true);
					}
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
		
		// collapse notes
		collapseNote(localTitle, localEdit, true);
		collapseNote(remoteTitle, remoteEdit, true);
		diffView.setVisibility(View.GONE);

		diffLabel.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
				diffView.setVisibility(diffLabel.isChecked()?View.VISIBLE:View.GONE);
			}
        });	
		
		localLabel.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
				collapseNote(localTitle, localEdit, !localLabel.isChecked());
			}
        });	
		remoteLabel.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
				collapseNote(remoteTitle, remoteEdit, !remoteLabel.isChecked());
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
				NoteManager.putNote(CompareNotes.this, localNote); // update local note with new title
			}
			else { // remote older, rename remote
				remoteNote.setTitle(newTitle);
			}
		}
			
		// add remote note to local
		uri = NoteManager.putNote(CompareNotes.this, remoteNote);

		if(noRemote) {
			finishForResult();
		}
		else {
			ArrayList<Note> notes = new ArrayList<Note>();
			notes.add(localNote);
			notes.add(remoteNote);
			SyncManager.getInstance().getCurrentService().pushNotes(notes);
			syncProgressDialog.setMessage(getString(R.string.syncing_remote));
			syncProgressDialog.show();
		}
	}

	protected void onChooseNote(String title, String content, boolean choseLocal) {
		ArrayList<Note> notes = new ArrayList<Note>();
		
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
				
				notes.add(localNote); // add for pushing
				notes.add(remoteNote); // add for deletion
				
				if(!noRemote)
					SyncManager.getInstance().getCurrentService().pushNotes(notes);
			}
			else { // chose to keep remote, delete local, add remote, push remote back 
				NoteManager.deleteNote(this, localNote);
				remoteNote.setTitle(title);
				remoteNote.setXmlContent(content);
				remoteNote.setLastChangeDate(time);
				NoteManager.putNote(this, remoteNote);

				if(!noRemote) {
					notes.add(remoteNote);
					SyncManager.getInstance().getCurrentService().pushNotes(notes);
					syncProgressDialog.setMessage(getString(R.string.syncing_remote));
					syncProgressDialog.show();
				}
			}
		}
		else { // just readd and push modified localNote
			uri = NoteManager.putNote(CompareNotes.this, localNote);

			if(noRemote) {
				finishForResult();
			}
			else {
				notes.add(localNote);
				SyncManager.getInstance().getCurrentService().pushNotes(notes);
				syncProgressDialog.setMessage(getString(R.string.syncing_remote));
				syncProgressDialog.show();
			}
		}

	}
	
	// local is deleted, delete remote as well
	protected void onChooseDelete() { 
		TLog.v(TAG, "user chose to delete remote note TITLE:{0} GUID:{1}", localNote.getTitle(),localNote.getGuid());
		NoteManager.deleteNote(CompareNotes.this, localNote.getDbId()); // really delete locally

		// this will delete the note, since it already has the "system:deleted" tag
		ArrayList<Note> notes = new ArrayList<Note>();
		notes.add(localNote);
		SyncManager.getInstance().getCurrentService().pushNotes(notes);
		syncProgressDialog.setMessage(getString(R.string.syncing_remote));
		syncProgressDialog.show();
	}
	
	/** Handler for the message from sync service */
	private Handler syncMessageHandler = new Handler() {
		
		@Override
        public void handleMessage(Message msg) {
			finish();
		}
    };
    
	@Override
   protected void onResume() {
    	super.onResume();
		SyncManager.setActivity(this);
		SyncManager.setHandler(this.syncMessageHandler);    	
    }
    
	@Override
    protected void onDestroy() {
    	syncProgressDialog.dismiss();
    	super.onDestroy();
    }

	private void updateTextAttributes(EditText title, EditText content) {
		float baseSize = Float.parseFloat(Preferences.getString(Preferences.Key.BASE_TEXT_SIZE));
		content.setTextSize(baseSize);
		title.setTextSize(baseSize*1.3f);

		title.setTextColor(Color.BLUE);
		title.setPaintFlags(title.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		title.setBackgroundColor(0xffffffff);

		content.setBackgroundColor(0xffffffff);
		content.setTextColor(Color.DKGRAY);
	}

	private void collapseNote(EditText title, EditText content, boolean collapse) {
		if(collapse) {
			title.setVisibility(View.GONE);
			content.setVisibility(View.GONE);
		}
		else {
			title.setVisibility(View.VISIBLE);
			content.setVisibility(View.VISIBLE);
		}
		
	}
	
	private void finishForResult(){
		// if we are receiving a note file, return the 
		Intent data = new Intent();
		data.putExtra("uri", uri.toString());
		if (getParent() == null) {
		    setResult(Activity.RESULT_OK, data);
		} else {
		    getParent().setResult(Activity.RESULT_OK, data);
		}
		finish();
	}
}	