package org.tomdroid.util;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.text.SpannableStringBuilder;

public class Send {
	
	public static void sendNote(Uri intentUri, Handler handler, Activity activity) {
		Note note = NoteManager.getNote(activity, intentUri);
		SpannableStringBuilder content = null;
		String body="";
		
		if (note!=null) {
			content=note.getNoteContent(handler);

			//remove title from content
			if (content.toString()!="") {
				body = content.toString().substring(note.getTitle().length()+2);
			}

		    // Create a new Intent to send messages
		    Intent sendIntent = new Intent(Intent.ACTION_SEND);
		    // Add attributes to the intent
	
		    sendIntent.putExtra(Intent.EXTRA_SUBJECT, note.getTitle());
		    sendIntent.putExtra(Intent.EXTRA_TEXT, body);
		    sendIntent.setType("text/plain");
	
		    activity.startActivity(Intent.createChooser(sendIntent, note.getTitle()));
		}
	}

}
