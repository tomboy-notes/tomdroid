package org.tomdroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

// TODO have some sort of logging so I can step through stuff easier
public class tomdroid extends Activity {
	private static final int ACTIVITY_VIEW=0;
	
	// UI elements
	private EditText txtURL;
	
    /** Called when the activity is created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        // Connect UI elements to variables
        txtURL = (EditText) findViewById(R.id.txtURL);
        txtURL.setText("http://www.bottomlesspit.org/files/note.xml");
        Button button = (Button)findViewById(R.id.btnURL);
        
        // Annon inner-class for button listener 
        button.setOnClickListener(new OnClickListener() {
        	
            public void onClick(View v)
            {
                Log.i(tomdroid.this.toString(), "info: Button clicked. URL requested: "+txtURL.getText().toString());
            	
            	Intent i = new Intent(tomdroid.this, NoteView.class);
                i.putExtra(Note.URL, txtURL.getText().toString());
                startActivityForResult(i, ACTIVITY_VIEW);
            }
        });
    }
    

}