package org.tomdroid;

import org.tomdroid.dao.NotesDAO;
import org.tomdroid.dao.mock.NotesDAOMock;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

public class NoteView extends Activity {
	
	private String url; 
	
	// UI elements
	private TextView content;
	
	// Data Access Objects
	private NotesDAO notesDAO;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.note_view);
		
		content = (TextView) findViewById(R.id.content);
		
		// get url to fetch from Intent
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			url = extras.getString(Note.URL);
		} else {
			Log.i(this.toString(), "info: Bundle was empty.");
		}
		
		if (url != null) {
			notesDAO = new NotesDAOMock(url);
			content.setText(notesDAO.getContent());
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		super.onKeyDown(keyCode, event);
		
		finish();
		
		return true;
	}



}
