package org.tomdroid.ui;

import java.io.File;

import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.util.VoicePlayer;
import org.tomdroid.util.VoiceRecorder;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.Chronometer.OnChronometerTickListener;

public class RecorderDialog extends Activity implements OnClickListener, OnChronometerTickListener {
	
	public static final String FILE_NAME_TEMP = "rec.tmp";
	
	VoiceRecorder voiceRecorder;
	VoicePlayer voicePlayer;
	
	static boolean		isPlaying   = false;
	static boolean		isRecording = false;

	ToggleButton btnRec;
	Button btnPlay;
	Button btnCancel;
	Button btnSave;
	Chronometer chronometre;
	
	String noteGuid;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.recorder);
		
		setTitle(R.string.titleRecorderDialog);
		if (isPlaying) Log.i("TOMBOY", "is playing");
		btnRec = (ToggleButton) findViewById(R.id.voiceRec);
		btnPlay = (Button) findViewById(R.id.voicePlay);
		btnSave = (Button) findViewById(R.id.voiceSave);
		btnCancel = (Button) findViewById(R.id.VoiceCancel);
		chronometre = (Chronometer) findViewById(R.id.chrono);
		
		btnRec.setOnClickListener(this);
		btnPlay.setOnClickListener(this);
		btnCancel.setOnClickListener(this);
		btnSave.setOnClickListener(this);
		chronometre.setOnChronometerTickListener(this);
		
		if (savedInstanceState==null) {
			btnPlay.setEnabled(false);
			btnSave.setEnabled(false);
		}
	
		noteGuid=NoteManager.getNote(this, getIntent().getData()).getGuid().toString();
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		btnPlay.setEnabled(savedInstanceState.getBoolean("play"));
		btnSave.setEnabled(savedInstanceState.getBoolean("ok"));
		super.onRestoreInstanceState(savedInstanceState);
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("play", btnPlay.isEnabled());
		outState.putBoolean("ok", btnSave.isEnabled());

		super.onSaveInstanceState(outState);
	}
	
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			//parsed ok - show
			if(msg.what == VoicePlayer.COMPLETION_OK) {
				Log.i("tomboy","ouuuuais");
				endPlayback();
			}
		}
	};

	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.voiceRec:
			if (isPlaying) endPlayback();
			if (isRecording) {
				endRecord();
			} else {
				beginRecord();
			}
			break;

		case R.id.voicePlay:
			if (isRecording) endRecord();
			if (isPlaying) {
				endPlayback();
			} else {
				beginPlayback();
			}
			break;
		
		case R.id.voiceSave:
			if (isPlaying) endPlayback();
			if (isRecording) endRecord();
			save();
			break;
			
		case R.id.VoiceCancel:
			if (isPlaying) endPlayback();
			if (isRecording) endRecord();
			cancel();
			break;
			
		default:
			break;
		}

	}

	public void endPlayback(){
		chronometre.stop();
		voicePlayer.endPlayback();
		voicePlayer.releasePlackback();

        isPlaying=false;
		btnPlay.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_media_play, 0);
 	}
	
	public void beginPlayback(){
		try {
			voicePlayer = new VoicePlayer(new File(getFilesDir(), FILE_NAME_TEMP), handler);
			voicePlayer.beginPlayback();
			chronometre.setBase(SystemClock.elapsedRealtime());
			chronometre.start();

			isPlaying=true;
			btnPlay.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_media_pause, 0);
			
		} catch (Exception e) {
			e.printStackTrace();			
		}
	}
		
	public void beginRecord(){	
        try {
        	chronometre.setBase(SystemClock.elapsedRealtime());
			chronometre.start();
			voiceRecorder=new VoiceRecorder(new File(getFilesDir(), FILE_NAME_TEMP));
			voiceRecorder.initRecord();
        	voiceRecorder.beginRecord();

			isRecording=true;

			btnPlay.setEnabled(false);
			btnSave.setEnabled(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void endRecord(){
		
		voiceRecorder.endRecord();
		voiceRecorder.finishRecord();
        chronometre.stop();

        isRecording=false;

        btnRec.setPressed(false);
		btnPlay.setEnabled(true);
		btnSave.setEnabled(true);
 	}
	
	public void onChronometerTick(Chronometer chronometer) {
		long elapsedTime = SystemClock.elapsedRealtime() - chronometer.getBase();
		int min = (int) (elapsedTime / 60000);
		int sec = (int) ((elapsedTime / 1000) % 60);
		String time = min < 10 ? "0" + min : String.valueOf(min);
		time += ":";
		time += sec < 10 ? "0" + sec : String.valueOf(sec);
		((Chronometer) findViewById(R.id.chrono)).setText(time);
		if ((isPlaying)&&(!voicePlayer.isPlaying())) endPlayback();

	}

	public void save() {
		// 'renameTo' rename de temp file with the note's name.
		File tempfile = this.getFileStreamPath(FILE_NAME_TEMP);
		File voiceNote = new File(this.getFilesDir(), noteGuid + ".note.amr");
		if (!tempfile.renameTo(voiceNote)) {
			Toast.makeText(this, "Error while saving the file. Please report the bug.", Toast.LENGTH_LONG).show();
		} else {
			setResult(RESULT_OK);
			Toast.makeText(this, "Voice note saved.", Toast.LENGTH_SHORT).show();
		}
		finish();
	}

	public void cancel() {
		finish();
	}

}
