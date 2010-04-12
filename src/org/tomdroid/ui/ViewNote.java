/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.util.LinkifyPhone;
import org.tomdroid.util.NoteContentBuilder;
import org.tomdroid.util.VoicePlayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

// TODO this class is starting to smell
public class ViewNote extends Activity implements android.view.View.OnClickListener {
	
	// UI elements
	private static TextView 	content;
	private static ImageButton	btnPlay;
	private static ImageButton	btnStop;
	private static SeekBar		seekbar;
	private static Menu		menu;
	
	// Voiceplayer elements
	static int mProgressStatus = 1;
	private Handler mHandler = new Handler();
	private static boolean stopThread = false;
	private boolean threadExist = false;
	private static final int RECORD_RESULT = 1111;
	
	// Model objects
	private Note note;
	private SpannableStringBuilder noteContent;
	private static File voiceNote;
	private static VoicePlayer player;

	// Logging info
	private static final String TAG = "ViewNote";
	
	private Uri uri;
	// TODO extract methods in here
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.note_view);
		content = (TextView) 	findViewById(R.id.content);
		btnPlay = (ImageButton)	findViewById(R.id.PlayImageButton);
		btnStop = (ImageButton)	findViewById(R.id.StopImageButton);
		seekbar	= (SeekBar)		findViewById(R.id.SeekBar);
		
		btnPlay.setOnClickListener(this);
		btnStop.setOnClickListener(this);
		seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}
			
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
			
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					// start the voice note if isn't started
					if (!player.isPlaying()) {
						player.beginPlayback();
						startRefresh();
					}
					// get progression in msec
					int msec=Math.round(((float)progress/(float)100)*(float)player.getDuration());

					player.goTo(msec);
					btnPlay.setImageResource(R.drawable.playback_pause);
				}				
			}
		});
		
		final Intent intent = getIntent();
		uri = intent.getData();

		if (uri != null) {
			
			// We were triggered by an Intent URI 
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "ViewNote started: Intent-filter triggered.");

			// TODO validate the good action?
			// intent.getAction()
			
			// TODO verify that getNote is doing the proper validation
			note = NoteManager.getNote(this, uri);
			
			if(note != null) {
				
				if (savedInstanceState==null) {
					Log.i("ViewNote", "initialisation");
					playerBarInit();
				}
				noteContent = note.getNoteContent(handler);
				
			} else {
				
				if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "The note "+uri+" doesn't exist");
				
				// TODO put error string in a translatable resource
				new AlertDialog.Builder(this)
					.setMessage("The requested note could not be found. If you see this error by " +
							    "mistake and you are able to replicate it, please file a bug!")
					.setTitle("Error")
					.setNeutralButton("Ok", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}})
					.show();
			}
		} else {
			
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "The Intent's data was null.");
			
			// TODO put error string in a translatable resource
			new AlertDialog.Builder(this)
			.setMessage("The requested note could not be found. If you see this error by " +
					    "mistake and you are able to replicate it, please file a bug!")
			.setTitle("Error")
			.setNeutralButton("Ok", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					finish();
				}})
			.show();
		}
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (voiceNote!=null) {
			View playerView = findViewById(R.id.player);
			playerView.setVisibility(View.VISIBLE);
			
			// show the correct drawable on the play button

			if (player.isPlaying()) btnPlay.setImageResource(R.drawable.playback_pause);
			else btnPlay.setImageResource(R.drawable.playback_start);
			
			btnStop.setEnabled(savedInstanceState.getBoolean("stop", true));
		}
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {

		
		if (player!=null) {
			outState.putBoolean("stop", btnStop.isEnabled());
		}
		super.onSaveInstanceState(outState);
	}
	
	public boolean onCreateOptionsMenu(Menu m) {

		// Create the menu based on what is defined in res/menu/note.xml
		menu = m;
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.note, menu);
	    if (player!=null) {
	    	menu.findItem(R.id.menuDeleteVoice).setVisible(true);
	    }
	    return true;
	}


	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case R.id.menuRecord:
        		Intent intent = new Intent(Intent.ACTION_VIEW, uri, this, RecorderDialog.class);
        		startActivityForResult(intent, RECORD_RESULT);
        		return true;
        	case R.id.menuDeleteVoice:
        		removeVoiceNote();
        }
        return super.onOptionsItemSelected(item);
	}

	private void showNote() {
		setTitle(note.getTitle());

		// get rid of the title that is doubled in the note's content
		// using quote to escape potential regexp chars in pattern
		Pattern removeTitle = Pattern.compile("^\\s*"+Pattern.quote(note.getTitle())+"\\n\\n"); 
		Matcher m = removeTitle.matcher(noteContent);
		if (m.find()) {
			noteContent = noteContent.replace(0, m.end(), "");
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "stripped the title from note-content");
		}
		
		// show the note (spannable makes the TextView able to output styled text)
		content.setText(noteContent, TextView.BufferType.SPANNABLE);
		
		// add links to stuff that is understood by Android except phone numbers because it's too aggressive
		// TODO this is SLOWWWW!!!!
		Linkify.addLinks(content, Linkify.EMAIL_ADDRESSES|Linkify.WEB_URLS|Linkify.MAP_ADDRESSES);
		
		// Custom phone number linkifier (fixes lp:512204)
		Linkify.addLinks(content, LinkifyPhone.PHONE_PATTERN, "tel:", LinkifyPhone.sPhoneNumberMatchFilter, Linkify.sPhoneNumberTransformFilter);
		
		// This will create a link every time a note title is found in the text.
		// The pattern contains a very dumb (title1)|(title2) escaped correctly
		// Then we transform the url from the note name to the note id to avoid characters that mess up with the URI (ex: ?)
		Linkify.addLinks(content, 
						 buildNoteLinkifyPattern(),
						 Tomdroid.CONTENT_URI+"/",
						 null,
						 noteTitleTransformFilter);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case RECORD_RESULT:
			if (resultCode==RESULT_OK) {
				menu.findItem(R.id.menuDeleteVoice).setVisible(true);
				playerBarInit();
				
			}
			break;

		default:
			break;
		}
		
	}
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			//parsed ok - show
			if(msg.what == NoteContentBuilder.PARSE_OK) {
				showNote();

			//parsed not ok - error
			} else if(msg.what == NoteContentBuilder.PARSE_ERROR) {
				
				// TODO put this String in a translatable resource
				new AlertDialog.Builder(ViewNote.this)
					.setMessage("The requested note could not be parsed. If you see this error by " +
								"mistake and you are able to replicate it, please file a bug!")
					.setTitle("Error")
					.setNeutralButton("Ok", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}})
					.show();
        	} else if (msg.what == VoicePlayer.COMPLETION_OK) {
        		stopRefresh();
        		seekbar.setProgress(0);
        		btnStop.setEnabled(false);
        		btnPlay.setImageResource(R.drawable.playback_start);
        	}
		}
	};
	
	/**
	 * Builds a regular expression pattern that will match any of the note title currently in the collection.
	 * Useful for the Linkify to create the links to the notes.
	 * @return regexp pattern
	 */
	private Pattern buildNoteLinkifyPattern()  {
		
		StringBuilder sb = new StringBuilder();
		Cursor cursor = NoteManager.getTitles(this);
		
		// cursor must not be null and must return more than 0 entry 
		if (!(cursor == null || cursor.getCount() == 0)) {
			
			String title;
			
			cursor.moveToFirst();
			
			do {
				title = cursor.getString(cursor.getColumnIndexOrThrow(Note.TITLE));
				
				// Pattern.quote() here make sure that special characters in the note's title are properly escaped 
				sb.append("("+Pattern.quote(title)+")|");
				
			} while (cursor.moveToNext());
			
			// get rid of the last | that is not needed (I know, its ugly.. better idea?)
			String pt = sb.substring(0, sb.length()-1);

			// return a compiled match pattern
			return Pattern.compile(pt);
			
		} else {
			
			// TODO send an error to the user
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "Cursor returned null or 0 notes");
		}
		
		return null;
	}
	
	// custom transform filter that takes the note's title part of the URI and translate it into the note id
	// this was done to avoid problems with invalid characters in URI (ex: ? is the query separator but could be in a note title)
	private TransformFilter noteTitleTransformFilter = new TransformFilter() {

		public String transformUrl(Matcher m, String str) {

			int id = NoteManager.getNoteId(ViewNote.this, str);
			
			// return something like content://org.tomdroid.notes/notes/3
			return Tomdroid.CONTENT_URI.toString()+"/"+id;
		}  
	};
	
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.PlayImageButton:
			if (player.isPaused()) {
				//resume
				player.resumePLayback();
				startRefresh();
				btnPlay.setImageResource(R.drawable.playback_pause);
			} else {
				if (player.isPlaying()) {
					//pause
					stopRefresh();
					player.pausePlayback();
					
					btnPlay.setImageResource(R.drawable.playback_start);
				} else {
					// play
					player.beginPlayback();
					startRefresh();
					btnStop.setEnabled(true);
					btnPlay.setImageResource(R.drawable.playback_pause);
				}
			}
			break;
			
		case R.id.StopImageButton:
			//stop
			stopRefresh();
			seekbar.setProgress(0);
			player.endPlayback();
			btnStop.setEnabled(false);
			btnPlay.setImageResource(R.drawable.playback_start);
			break;

		default:
			break;
		}
	}
	
	private void removeVoiceNote() {
		if (player!=null) {
			if (player.isPlaying()) {
				player.endPlayback();
				
			}
			player.releasePlayback();
		}
		if (voiceNote.delete()){
			playerBarInit();
			player=null;
			voiceNote=null;
			menu.findItem(R.id.menuDeleteVoice).setVisible(false);
		}
	}
	
	private void playerBarInit(){
		voiceNote = new File(Tomdroid.NOTES_PATH,note.getGuid()+".note.amr");
		View playerView = findViewById(R.id.player);
		if (voiceNote.exists()) {
			playerView.setVisibility(View.VISIBLE);
			btnStop.setEnabled(false);
			player = new VoicePlayer(voiceNote, handler);
		} else {
			playerView.setVisibility(View.GONE);
			player = null;
		}
	}

	public synchronized void stopRefresh() {
        stopThread = true;
	}
	
	public void startRefresh() {
        stopThread = false;
     // Start lengthy operation in a background thread
        if (!threadExist){
			new Thread(new Runnable() {
				public void run() {
					threadExist=true;
					mProgressStatus = player.getProgress();
					while ((mProgressStatus < 100)&&(!stopThread)) {
						try {
							mProgressStatus = player.getProgress();
							// Update the progress bar
							mHandler.post(new Runnable() {
								public void run() {
									seekbar.setProgress(mProgressStatus);
								}
							});
							Thread.sleep(300);
							
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					threadExist=false;
					Log.i("Thread", "stop");
					
	
				}
			}).start();
		}
	}
}
