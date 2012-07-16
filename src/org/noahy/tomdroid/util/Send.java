package org.noahy.tomdroid.util;

import org.noahy.tomdroid.Note;
import org.noahy.tomdroid.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;

public class Send {
	
	private Activity activity;
	private Note note;
	private SpannableStringBuilder noteContent;
	
	public Send(Activity activity, Note note) {
		this.activity = activity;
		this.note = note;
	}
	
	public void send() {
		if (note != null) {
			noteContent = note.getNoteContent(noteContentHandler);
		}
	}
	
	private Handler noteContentHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			//parsed ok - show
			if(msg.what == NoteContentBuilder.PARSE_OK) {
				
				String body = noteContent.toString();
				
			    // Create a new Intent to send messages
			    Intent sendIntent = new Intent(Intent.ACTION_SEND);
			    // Add attributes to the intent
		
			    sendIntent.putExtra(Intent.EXTRA_SUBJECT, note.getTitle());
			    sendIntent.putExtra(Intent.EXTRA_TEXT, body);
			    sendIntent.setType("text/plain");
		
			    activity.startActivity(Intent.createChooser(sendIntent, note.getTitle()));

			//parsed not ok - error
			} else if(msg.what == NoteContentBuilder.PARSE_ERROR) {
				
				new AlertDialog.Builder(activity)
					.setMessage(activity.getString(R.string.messageErrorNoteParsing))
					.setTitle(activity.getString(R.string.error))
					.setNeutralButton(activity.getString(R.string.btnOk), new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}})
					.show();
        	}
		}
	};
}
