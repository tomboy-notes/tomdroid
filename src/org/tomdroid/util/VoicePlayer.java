package org.tomdroid.util;

import java.io.File;
import java.io.FileInputStream;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.Message;

public class VoicePlayer implements OnCompletionListener {

	private FileInputStream fis;
	private MediaPlayer player;
	private boolean paused=false;
	private Handler handler;
	public final static int COMPLETION_OK = 3;

	public VoicePlayer(File file, Handler handler) {
		try {
			this.fis = new FileInputStream(file);
			this.player=new MediaPlayer();
			this.player.setOnCompletionListener(this);
			this.handler=handler;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void beginPlayback() {

			try {
				player.setDataSource(fis.getFD());
				player.prepare();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			player.start();

	}

	public void endPlayback() {
		player.stop();
		player.reset();
	}
	
	public void pausePlayback() {
		paused=true;
		player.pause();
	}
	
	public void resumePLayback(){
		paused=false;
		player.start();
	}
	
	public void goTo(int msec){
		player.seekTo(msec);
	}
	
	public void releasePlackback() {
		player.release();
	}

	public void onCompletion(MediaPlayer arg0) {
		// notify the main UI that we are done here
		Message msg = Message.obtain();
		msg.what = COMPLETION_OK;
		handler.sendMessage(msg);

	}

	public int getProgress() {
		if (!player.isPlaying()) {
			return 0;
		} else {
			// convert into percent
			float progress = ((float) player.getCurrentPosition() / (float) player.getDuration())* (float) 100;
			return (Math.round(progress));
		}
	}
	
	public boolean isPlaying() {
		return player.isPlaying();
	}
	
	public int getDuration() {
		return player.getDuration();
	}
	
	public boolean isPaused(){
		return paused;
	}
}
