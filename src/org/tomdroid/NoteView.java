package org.tomdroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

public class NoteView extends Activity {
	
	private String url; 
	
	// UI elements
	private TextView content;
	
	

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
			try {
				
				content.setText(fetch(url).toString());
				
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		super.onKeyDown(keyCode, event);
		
		finish();
		
		return true;
	}


	/**
	 * Grab the content at the target address and convert it to a string.
	 * @param address
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private Object fetch(String address) throws MalformedURLException, IOException {
		
		//grab URL
		URL url = new URL(address);
		InputStream is = (InputStream) url.getContent();
		
		//Init BufferedReader and StringBuilder
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		//Convert from InputStream to String using StringBuilder
		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();

		//Return the string
		return sb.toString();
	}
}
