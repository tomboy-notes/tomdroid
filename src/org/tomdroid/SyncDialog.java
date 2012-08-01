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
package org.tomdroid;

import org.tomdroid.sync.SyncManager;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.TLog;

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
	
public class SyncDialog extends Activity {	
	private static final String TAG = "SyncDialog";
	private Context context;
	
	private ContentValues values = new ContentValues();
	private Note note;
	private Uri uri;
	
	@Override	
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);	
		this.context = this;
		
		Bundle extras = this.getIntent().getExtras();

		values.put(Note.TITLE, extras.getString("title"));
		values.put(Note.FILE, extras.getString("file"));
		values.put(Note.GUID, extras.getString("guid"));
		values.put(Note.MODIFIED_DATE, extras.getString("date"));
		values.put(Note.NOTE_CONTENT, extras.getString("content"));
		values.put(Note.TAGS, extras.getString("tags"));
		uri = Uri.parse(extras.getString("uri"));
		note = NoteManager.getNote(this, uri);

		final ContentResolver cr = getContentResolver();
		
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.sync_conflict_title)
        .setMessage(String.format(getString(R.string.sync_conflict_message),note.getTitle()))
        .setPositiveButton(R.string.remote, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	String[] where = {note.getGuid()};
            	// pull remote changes
				TLog.v(TAG, "user chose to pull remote conflicting note TITLE:{0} GUID:{1}", note.getTitle(),note.getGuid());
				cr.update(Tomdroid.CONTENT_URI, values, Note.GUID+" = ?", where);
				finish();	
            }
        })
        .setNegativeButton(R.string.local, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
				/* pushing local changes, reject older incoming note.
				 * If the local counterpart has the tag "system:deleted", delete from both local and remote.
				 * Otherwise, push local to remote.
				 */
            	
				
				TLog.v(TAG, "user chose to send local conflicting note TITLE:{0} GUID:{1}", note.getTitle(),note.getGuid());

				if(note.getTags().contains("system:deleted")) {
					TLog.v(TAG, "local note is deleted, deleting from server TITLE:{0} GUID:{1}", note.getTitle(),note.getGuid());
					SyncManager.getInstance().deleteNote(note.getGuid()); // delete from remote
					NoteManager.deleteNote(SyncDialog.this, note.getDbId()); // really delete locally
				}
				else {
					SyncManager.getInstance().pushNote(note);
				}
				finish();	
            }
        })
        .show();
		
	}	
}	