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
import org.tomdroid.sync.SyncManager;
import org.tomdroid.ui.actionbar.ActionBarActivity;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.TLog;
import org.tomdroid.util.Time;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import android.app.Activity;	
import android.content.ContentValues;
import android.content.Intent;	
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;	
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
	
public class CompareNotes extends ActionBarActivity {	
	private static final String TAG = "SyncDialog";
	
	private Note localNote;
	private boolean differentNotes;
	private Note remoteNote;
	private int dateDiff;
	private boolean noRemote;
	private float baseSize;;

	@Override	
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);	
		
		Preferences.init(this, Tomdroid.CLEAR_PREFERENCES);
		baseSize = Float.parseFloat(Preferences.getString(Preferences.Key.BASE_TEXT_SIZE));
		
		if(!this.getIntent().hasExtra("datediff")) {
			TLog.v(TAG, "no date diff");
			finish();
			return;
		}
		TLog.v(TAG, "starting CompareNotes");
		
		setContentView(R.layout.note_compare);
		// Disable the tomdroid icon home button
		setHomeButtonEnabled(false);
		
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
		
		final TextView descriptionView = (TextView)findViewById(R.id.description);
		final TextView messageView = (TextView)findViewById(R.id.message);
		descriptionView.setTextSize(baseSize*1.3f);
		descriptionView.setTextColor(Color.RED);
		messageView.setTextSize(baseSize);
		
		final ToggleButton diffLabel = (ToggleButton)findViewById(R.id.diff_label);
		final ToggleButton localLabel = (ToggleButton)findViewById(R.id.local_label);
		final ToggleButton remoteLabel = (ToggleButton)findViewById(R.id.remote_label);

		final EditText localTitle = (EditText)findViewById(R.id.local_title);
		final EditText remoteTitle = (EditText)findViewById(R.id.remote_title);
		
		final TextView diffView = (TextView)findViewById(R.id.diff);
		diffView.setTextSize(baseSize);
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
						finishForResult(new Intent());
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
						pullNote(remoteNote);
						finish();
					}
					else if(extras.getInt("datediff") == 0 || noRemote) {
						TLog.v(TAG, "compared notes have same content and titles, same date, doing nothing");
					}
					else {
						TLog.v(TAG, "compared notes have same content and titles, pushing newer local");
						pushNote(localNote);
						finish();
					}
					
					if(noRemote) {
						TLog.v(TAG, "compared notes have same content and titles, showing note");
						Uri uri = NoteManager.getUriByGuid(this, localNote.getGuid());
						Intent returnIntent = new Intent();
						returnIntent.putExtra("uri", uri.toString());
						finishForResult(returnIntent);
					}
					else // do nothing
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
		
		descriptionView.setText(String.format(getString(R.string.sync_conflict_description),localNote.getTitle()));
		messageView.setText(message);
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
		collapseDescription(descriptionView, messageView, false);
		diffView.setVisibility(View.GONE);

		diffLabel.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
				diffView.setVisibility(diffLabel.isChecked()?View.VISIBLE:View.GONE);
				collapseDescription(descriptionView, messageView, (diffLabel.isChecked() || localLabel.isChecked() || remoteLabel.isChecked()));
			}
        });	
		
		localLabel.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
				collapseNote(localTitle, localEdit, !localLabel.isChecked());
				collapseDescription(descriptionView, messageView, (diffLabel.isChecked() || localLabel.isChecked() || remoteLabel.isChecked()));
			}
        });	
		remoteLabel.setOnClickListener( new View.OnClickListener() {
			public void onClick(View v) {
				collapseNote(remoteTitle, remoteEdit, !remoteLabel.isChecked());
				collapseDescription(descriptionView, messageView, (diffLabel.isChecked() || localLabel.isChecked() || remoteLabel.isChecked()));
			}
        });	
	}
	
	@Override	
	public void onResume() {
		// if the SyncService was stopped because Android killed it, we should not show the progress dialog any more
		if (SyncManager.getInstance().getCurrentService().activity == null) {
			TLog.i(TAG, "Android killed the SyncService while in background. We will dismiss the compare view now.");
			finish();
		}
		
		super.onResume();
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
				pullNote(localNote); // update local note with new title
			}
			else { // remote older, rename remote
				remoteNote.setTitle(newTitle);
			}
		}
			
		// add remote note to local
		pullNote(remoteNote);

		if(!noRemote) {
			pushNote(localNote);
			pushNote(remoteNote);
		}
		finishForResult(new Intent());
	}

	protected void onChooseNote(String title, String content, boolean choseLocal) {
		title = NoteManager.validateNoteTitle(this, title, localNote.getGuid());
		
		Time now = new Time();
		now.setToNow();
		String time = now.formatTomboy();
		
		localNote.setTitle(title);
		localNote.setXmlContent(content);
		localNote.setLastChangeDate(time);

		// doing a title difference

		if(differentNotes) {
			if(choseLocal) { // chose to keep local, delete remote, push local
				pullNote(localNote);
				remoteNote.addTag("system:deleted");
				
				if(noRemote) {
					Uri uri = NoteManager.getUriByGuid(this, localNote.getGuid());
					Intent returnIntent = new Intent();
					returnIntent.putExtra("uri", uri.toString());
					finishForResult(returnIntent);
					return;
				}
				
				pushNote(localNote); // add for pushing
				pushNote(remoteNote); // add for deletion
				
			}
			else { // chose to keep remote, delete local, add remote, push remote back 
				deleteNote(localNote);
				remoteNote.setTitle(title);
				remoteNote.setXmlContent(content);
				remoteNote.setLastChangeDate(time);
				pullNote(remoteNote);

				if(!noRemote)
					pushNote(remoteNote);
			}
		}
		else { // just read and push modified localNote
			pullNote(localNote);

			if(!noRemote) 
				pushNote(localNote);
		}
		// if noRemote, show the imported note afterwards!
		Intent returnIntent = new Intent();
		if (noRemote) {
			Uri uri = null;
			if (choseLocal) {
				uri = NoteManager.getUriByGuid(this, localNote.getGuid());
			} else {
				uri = NoteManager.getUriByGuid(this, remoteNote.getGuid());
			}
			returnIntent.putExtra("uri", uri.toString());
		}
		finishForResult(returnIntent);
	}

	// local is deleted, delete remote as well
	protected void onChooseDelete() { 
		TLog.v(TAG, "user chose to delete remote note TITLE:{0} GUID:{1}", localNote.getTitle(),localNote.getGuid());

		// this will delete the note, since it already has the "system:deleted" tag
		pushNote(localNote);
		finish();
	}

	private void pullNote(Note note) {
		if (noRemote) {
			note.setLastChangeDate();
			NoteManager.putNote(this, note);
		} else {
			SyncManager.getInstance().getCurrentService().addPullable(note);
		}
	}

	private void pushNote(Note note) {
		if (noRemote) {
			// do nothing as we have no server in noRemote mode!
		} else {
			SyncManager.getInstance().getCurrentService().addPushable(note);
		}
	}
	
	private void deleteNote(Note note) {
		if (noRemote) {
			NoteManager.deleteNote(this, note);
		} else {
			SyncManager.getInstance().getCurrentService().addDeleteable(note);
		}
	}
	
	private void updateTextAttributes(EditText title, EditText content) {
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
	
	private void collapseDescription(TextView description, TextView message, boolean collapse) {
		if(collapse) {
			description.setVisibility(View.GONE);
			message.setVisibility(View.GONE);
		}
		else {
			description.setVisibility(View.VISIBLE);
			message.setVisibility(View.VISIBLE);
		}
		
	}

	private void finishForResult(Intent data){
		if (getParent() == null) {
		    setResult(Activity.RESULT_OK, data);
		} else {
		    getParent().setResult(Activity.RESULT_OK, data);
		}
		finish();
	}
}	