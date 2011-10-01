package org.tomdroid.util;

import org.tomdroid.Note;

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
				
				// TODO put this String in a translatable resource
				new AlertDialog.Builder(activity)
					.setMessage("The requested note could not be parsed. If you see this error " +
								" and you are able to replicate it, please file a bug!")
					.setTitle("Error")
					.setNeutralButton("Ok", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}})
					.show();
        	}
		}
	};
}
