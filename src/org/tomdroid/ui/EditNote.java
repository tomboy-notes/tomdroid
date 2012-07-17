/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
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
package org.tomdroid.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.text.util.Linkify.TransformFilter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.util.NoteViewShortcutsHelper;
import org.tomdroid.util.TLog;

import java.util.regex.Matcher;
import java.util.Date;
import java.text.SimpleDateFormat;

public class EditNote extends Activity {
	public static final String CALLED_FROM_SHORTCUT_EXTRA = "org.tomdroid.CALLED_FROM_SHORTCUT";
    public static final String SHORTCUT_NAME = "org.tomdroid.SHORTCUT_NAME";

    // UI elements

	private EditText content;
    // Model objects
	private Note note;

	private String noteContent;

	// Logging info
	private static final String TAG = "EditNote";
    // UI feedback handler
	private Handler	syncMessageHandler	= new SyncMessageHandler(this);
	private EditText titleEdit;
	private String noteTitle;

	// TODO extract methods in here
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.note_edit);
		content = (EditText) findViewById(R.id.content);
		content.setBackgroundColor(0xffffffff);
		content.setTextColor(Color.DKGRAY);
		content.setTextSize(18.0f);
		
		titleEdit = (EditText) findViewById(R.id.title_edit);
		titleEdit.setBackgroundColor(0xffffffff);
		titleEdit.setTextColor(Color.DKGRAY);
		titleEdit.setTextSize(24.0f);
		
        Uri uri = getIntent().getData();


		final ImageView saveButton = (ImageView) findViewById(R.id.save);
		saveButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				saveNote();
			}
		});
		
        if (uri == null) {
			TLog.d(TAG, "The Intent's data was null.");
            showNoteNotFoundDialog(uri);
        } else handleNoteUri(uri);
    }

    private void handleNoteUri(final Uri uri) {// We were triggered by an Intent URI
        TLog.d(TAG, "EditNote started: Intent-filter triggered.");

        // TODO validate the good action?
        // intent.getAction()

        // TODO verify that getNote is doing the proper validation
        note = NoteManager.getNote(this, uri);

        if(note != null) {
            String xml = note.getXmlContent();
            xml = xml.replaceAll("</*note-content[^>]*>","").replaceAll("</*link[^>]*>","").replaceAll("</*italic[^>]*>","*").replaceAll("</*bold[^>]*>","**").replaceAll("</*size:large>","+").replaceAll("</*size:large>","+").replaceAll("</*list>","").replaceAll("<list-item[^>]*>","-").replaceAll("</list-item>","");
            noteContent = xml;
            noteTitle = note.getTitle();
            showNote();
        } else {
            TLog.d(TAG, "The note {0} doesn't exist", uri);
            showNoteNotFoundDialog(uri);
        }
    }

    private void showNoteNotFoundDialog(final Uri uri) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        addCommonNoteNotFoundDialogElements(builder);
        addShortcutNoteNotFoundElements(uri, builder);
        builder.show();
    }

    private void addShortcutNoteNotFoundElements(final Uri uri, final AlertDialog.Builder builder) {
        final boolean proposeShortcutRemoval;
        final boolean calledFromShortcut = getIntent().getBooleanExtra(CALLED_FROM_SHORTCUT_EXTRA, false);
        final String shortcutName = getIntent().getStringExtra(SHORTCUT_NAME);
        proposeShortcutRemoval = calledFromShortcut && uri != null && shortcutName != null;

        if (proposeShortcutRemoval) {
            final Intent removeIntent = new NoteViewShortcutsHelper(this).getRemoveShortcutIntent(shortcutName, uri);
            builder.setPositiveButton(getString(R.string.btnRemoveShortcut), new OnClickListener() {
                public void onClick(final DialogInterface dialogInterface, final int i) {
                    sendBroadcast(removeIntent);
                    finish();
                }
            });
        }
    }

    private void addCommonNoteNotFoundDialogElements(final AlertDialog.Builder builder) {
        builder.setMessage(getString(R.string.messageNoteNotFound))
                .setTitle(getString(R.string.titleNoteNotFound))
                .setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });
    }

	@Override
	public void onResume(){
		super.onResume();
		SyncManager.setActivity(this);
		SyncManager.setHandler(this.syncMessageHandler);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Create the menu based on what is defined in res/menu/noteview.xml
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_note, menu);
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.edit_note_cancel:
				finish();
				return true;
			case R.id.edit_note_save:
				saveNote();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showNote() {

		content.setText(noteContent);
		titleEdit.setText(noteTitle);
	}



	// custom transform filter that takes the note's title part of the URI and translate it into the note id
	// this was done to avoid problems with invalid characters in URI (ex: ? is the query separator but could be in a note title)
	private TransformFilter noteTitleTransformFilter = new TransformFilter() {

		public String transformUrl(Matcher m, String str) {

			int id = NoteManager.getNoteId(EditNote.this, str);

			// return something like content://org.tomdroid.notes/notes/3
			return Tomdroid.CONTENT_URI.toString()+"/"+id;
		}
	};
	private void saveNote() {
		String xml = "<note-content version=\"0.1\">"+
			content.getText().toString()
			.replaceAll("\\*\\*(\\S.*\\S)\\*\\*(.*\\*\\*\\S.*\\S\\*\\*)","<bold>$1</bold>$2")
			.replaceAll("\\*\\*(\\S.*\\S)\\*\\*","<bold>$1</bold>")
			.replaceAll("\\*(\\S.*\\S)\\*(.*\\*\\S.*\\S\\*)","<italic>$1</italic>$2")
			.replaceAll("\\*(\\S.*\\S)\\*","<italic>$1</italic>")
			.replaceAll("\\+(\\S.*\\S)\\+(.*\\+\\S.*\\S\\+)","<size:large>$1</size:large>$2")
			.replaceAll("\\+(\\S.*\\S)\\+","<size:large>$1</size:large>")
			.replaceAll("^-(.+)\n","<list-item dir=\"ltr\">$1</list-item>").replaceAll("^<list-item","<list>\n<list-item").replaceAll("</list-item>\n","</list-item>\n</list>\n").replaceAll("</list-item><list-item","</list-item>\n<list-item")+
			"</note-content>";
		note.setXmlContent(xml);
		note.setTitle(titleEdit.getText().toString());

		Time now = new Time();
		now.setToNow();
		String time = now.format3339(false);
		note.setLastChangeDate(time);
		
		NoteManager.putNote(this,note);

		// put note to server
		
		SyncManager.getInstance().pushNote(note);

	}
}
