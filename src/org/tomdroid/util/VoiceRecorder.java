package org.tomdroid.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.media.MediaRecorder;

public class VoiceRecorder {
	
	MediaRecorder 				recorder;
	FileOutputStream 			fos;
	File						file;

	public VoiceRecorder(File file) {
		
				try {
					fos = new FileOutputStream(file);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
	}
	
	public void initRecord(){	
        try {
	        recorder = new MediaRecorder();
	        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
	        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
	        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);	        
	        recorder.setOutputFile(fos.getFD());
	        recorder.prepare();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void beginRecord(){	
			recorder.start();
	}
	
	public void endRecord(){
        recorder.stop();

 	}
	
	public void finishRecord(){

        recorder.reset();
        recorder.release();
        try {
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}        

 	}

}
