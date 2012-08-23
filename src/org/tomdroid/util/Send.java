package org.tomdroid.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.tomdroid.Note;
import org.tomdroid.R;
import org.tomdroid.ui.Tomdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;

public class Send {

	private String TAG = "Send";

	private Activity activity;
	private Note note;
	private SpannableStringBuilder noteContent;
	private int DIALOG_CHOOSE = 0;
	private boolean sendAsFile;;
	
	public Send(Activity activity, Note note, boolean sendAsFile) {
		this.activity = activity;
		this.note = note;
		this.sendAsFile = sendAsFile;
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
				if(sendAsFile)
					sendNoteAsFile();
				else
					sendNoteAsText();

			//parsed not ok - error
			} else if(msg.what == NoteContentBuilder.PARSE_ERROR) {
				activity.showDialog(Tomdroid.DIALOG_PARSE_ERROR);

        	}
		}
	};
	
	private void sendNoteAsFile() {
		int cursorPos = 0;
		int width = 0;
		int height = 0;
		int X = -1;
		int Y = -1;
		String tags = note.getTags();
		if(tags.length()>0) {
			String[] tagsA = tags.split(",");
			tags = "\n\t<tags>";
			for(String atag : tagsA) {
				tags += "\n\t\t<tag>"+atag+"</tag>"; 
			}
			tags += "\n\t</tags>"; 
		}
		
		String xmlOutput = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<note version=\"0.3\" xmlns:link=\"http://beatniksoftware.com/tomboy/link\" xmlns:size=\"http://beatniksoftware.com/tomboy/size\" xmlns=\"http://beatniksoftware.com/tomboy\">\n\t<title>"
				+note.getTitle().replace("&", "&amp;")+"</title>\n\t<text xml:space=\"preserve\"><note-content version=\"0.1\">"
				+note.getXmlContent()+"</note-content></text>\n\t<last-change-date>"
				+note.getLastChangeDate().format3339(false)+"</last-change-date>\n\t<last-metadata-change-date>"
				+note.getLastChangeDate().format3339(false)+"</last-metadata-change-date>\n\t<create-date>"
				+note.getLastChangeDate().format3339(false)+"</create-date>\n\t<cursor-position>"
				+cursorPos+"</cursor-position>\n\t<width>"
				+width+"</width>\n\t<height>"
				+height+"</height>\n\t<x>"
				+X+"</x>\n\t<y>"
				+Y+"</y>"
				+note.getTags()+"\n\t<open-on-startup>False</open-on-startup>\n</note>\n";	
		
		FileOutputStream outFile = null;
		Uri noteUri = null;
		try {
			File outputDir = activity.getCacheDir(); // context being the Activity pointer
			File outputFile = File.createTempFile(note.getGuid(), "note", outputDir);
			
			outFile = new FileOutputStream(outputFile);
			OutputStreamWriter osw = new OutputStreamWriter(outFile);
			osw.write(xmlOutput);
			osw.flush();
			osw.close();
			
			noteUri = Uri.fromFile(outputFile);
			 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(noteUri == null) {
			TLog.e(TAG, "Unable to create note to send");
			return;
		}
		
	    // Create a new Intent to send messages
	    Intent sendIntent = new Intent(Intent.ACTION_SEND);

	    // Add attributes to the intent
	    sendIntent.putExtra(Intent.EXTRA_STREAM, noteUri);
	    sendIntent.setType("application/tomboy");
	    activity.startActivity(Intent.createChooser(sendIntent, note.getTitle()));
		return;
	}
	
	private void sendNoteAsText() {
		String body = noteContent.toString();
		
	    // Create a new Intent to send messages
	    Intent sendIntent = new Intent(Intent.ACTION_SEND);
	    // Add attributes to the intent

	    sendIntent.putExtra(Intent.EXTRA_SUBJECT, note.getTitle());
	    sendIntent.putExtra(Intent.EXTRA_TEXT, body);
	    sendIntent.setType("text/plain");

	    activity.startActivity(Intent.createChooser(sendIntent, note.getTitle()));

	}
}
