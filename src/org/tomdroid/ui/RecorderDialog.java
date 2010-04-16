package org.tomdroid.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.util.VoicePlayer;
import org.tomdroid.util.VoiceRecorder;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Chronometer.OnChronometerTickListener;

public class RecorderDialog extends Activity implements OnClickListener, OnChronometerTickListener {
	
	public static final String FILE_NAME_TEMP = "rec.tmp";
	File tempFile;
	static VoiceRecorder voiceRecorder;
	static VoicePlayer voicePlayer;
	
	static boolean		isPlaying   = false;
	static boolean		isRecording = false;
	static long 		base=0;
	
	static ImageButton btnRec;
	static ImageButton btnPlay;
	static ImageButton btnStop;
	static Button btnCancel;
	static Button btnSave;
	static Chronometer chronometre;
	
	static String noteGuid;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.recorder);
		
		setTitle(R.string.titleRecorderDialog);
		btnRec = (ImageButton) findViewById(R.id.voiceRec);
		btnPlay = (ImageButton) findViewById(R.id.voicePlay);
		btnStop = (ImageButton) findViewById(R.id.voiceStop);
		btnSave = (Button) findViewById(R.id.voiceSave);
		btnCancel = (Button) findViewById(R.id.VoiceCancel);
		chronometre = (Chronometer) findViewById(R.id.chrono);
		tempFile = new File(this.getFilesDir(), "rec.tmp");
		
		btnRec.setOnClickListener(this);
		btnPlay.setOnClickListener(this);
		btnStop.setOnClickListener(this);
		btnCancel.setOnClickListener(this);
		btnSave.setOnClickListener(this);
		chronometre.setOnChronometerTickListener(this);

		if (savedInstanceState==null) {
			btnPlay.setEnabled(false);
			btnSave.setEnabled(false);
			btnStop.setEnabled(false);
		}
		
		// launch the chronometer after a rotation
		if ((isPlaying)||(isRecording)) {
			chronometre.setBase(base);
			chronometre.start();
		}
	
		noteGuid=NoteManager.getNote(this, getIntent().getData()).getGuid().toString();
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		btnPlay.setEnabled(savedInstanceState.getBoolean("play"));
		btnStop.setEnabled(savedInstanceState.getBoolean("stop"));
		btnRec.setEnabled(savedInstanceState.getBoolean("rec"));
		btnSave.setEnabled(savedInstanceState.getBoolean("ok"));
		super.onRestoreInstanceState(savedInstanceState);
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("play", btnPlay.isEnabled());
		outState.putBoolean("stop", btnStop.isEnabled());
		outState.putBoolean("rec", btnRec.isEnabled());
		outState.putBoolean("ok", btnSave.isEnabled());
		super.onSaveInstanceState(outState);
	}
	
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			//parsed ok - show
			if(msg.what == VoicePlayer.COMPLETION_OK) {
				chronometre.stop();
				voicePlayer.releasePlayback();
				isPlaying=false;
				btnRec.setEnabled(true);
				btnStop.setEnabled(false);
				btnPlay.setEnabled(true);
				btnSave.setEnabled(true);
			}
		}
	};

	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.voiceRec:
			if (!isRecording) beginRecord();
			break;

		case R.id.voicePlay:
			if (!isPlaying) beginPlayback();
			break;
			
		case R.id.voiceStop:
			if (isRecording)	endRecord();
			if (isPlaying) 		endPlayback();
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
		voicePlayer.releasePlayback();
		
		isPlaying=false;

		btnRec.setEnabled(true);
		btnStop.setEnabled(false);
		btnPlay.setEnabled(true);
		btnSave.setEnabled(true);

 	}
	
	public void beginPlayback(){
		try {
			voicePlayer = new VoicePlayer(tempFile, handler);
			voicePlayer.beginPlayback();
			base = SystemClock.elapsedRealtime();
        	chronometre.setBase(base);
        	chronometre.start();

			isPlaying=true;
			
			btnRec.setEnabled(false);
			btnStop.setEnabled(true);
			btnPlay.setEnabled(false);
			btnSave.setEnabled(true);
			
		} catch (Exception e) {
			e.printStackTrace();			
		}
	}
		
	public void beginRecord(){	
        try {
        	base = SystemClock.elapsedRealtime();
        	chronometre.setBase(base);
			chronometre.start();
			voiceRecorder=new VoiceRecorder(tempFile);
			voiceRecorder.initRecord();
        	voiceRecorder.beginRecord();

			isRecording=true;
			
			btnRec.setEnabled(false);
			btnStop.setEnabled(true);
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

        btnRec.setEnabled(true);
        btnStop.setEnabled(false);
		btnPlay.setEnabled(true);
		btnSave.setEnabled(true);
 	}
	
	public void onChronometerTick(Chronometer chronometer) {
		long elapsedTime = SystemClock.elapsedRealtime() - base;
		int min = (int) (elapsedTime / 60000);
		int sec = (int) ((elapsedTime / 1000) % 60);
		String time = min < 10 ? "0" + min : String.valueOf(min);
		time += ":";
		time += sec < 10 ? "0" + sec : String.valueOf(sec);
		chronometer.setText(time);

	}

	public void save() {
		// put voicenote in the tomdroid's directory
		File voiceNote = new File(Tomdroid.NOTES_PATH, noteGuid + ".note.voice");
		
		try {
		FileInputStream fis = new FileInputStream(tempFile);
		FileOutputStream fos = new FileOutputStream(voiceNote);
		BufferedInputStream buf = new BufferedInputStream(fis);
		
		byte buffer[] = new byte[1024*4];
		

        while ((buf.read(buffer)) != -1) {

            fos.write(buffer);
        }
        
        fos.flush();
        fos.close();
        fis.close();
		tempFile.delete();
        setResult(RESULT_OK);
		Toast.makeText(this, "Voice note saved.", Toast.LENGTH_SHORT).show();
		
		} catch (FileNotFoundException e) {
			Toast.makeText(this, "Error while saving the file. Please report the bug.", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} catch (IOException e) {
			Toast.makeText(this, "Error while saving the file. Please report the bug.", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}

		finish();
	}

	public void cancel() {
		tempFile.delete();
		finish();
	}

}
