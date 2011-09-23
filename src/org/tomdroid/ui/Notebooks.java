package org.tomdroid.ui;

import org.tomdroid.Notebook;
import org.tomdroid.NotebookManager;
import org.tomdroid.R;
import org.tomdroid.util.NotebookListCursorAdapter;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Notebooks extends ListActivity {
	
	// Logging info
	private static final String	TAG					= "Notebooks";
	
	// UI to data model glue
	private NotebookListCursorAdapter adapter;
	
	/** Called when the activity is created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notebooks);
		Log.i(TAG, "setContentView OK");
		
		// adapter that binds the ListView UI to the notebooks in the note manager
		adapter = NotebookManager.getNotbookListAdapter(this);
		setListAdapter(adapter);		
		
		TextView title = (TextView) findViewById(R.id.title);
		title.setText(getString(R.string.notebookSelectTitle));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}
	
	public void onResume() {
		super.onResume();
	}
	

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG,"onListItemClick");
		
		Cursor item = (Cursor) adapter.getItem(position);
		String notebook = item.getString(item.getColumnIndexOrThrow(Notebook.NAME));
		
		Bundle bundle = new Bundle();
		bundle.putString("notebook", notebook);

		Intent i = new Intent(this.getApplicationContext(), Tomdroid.class);
		i.putExtras(bundle);
		startActivity(i);
		
		//Intent i = new Intent(Intent.ACTION_VIEW,Tomdroid.CONTENT_URI, this, Tomdroid.class);
		//startActivity(i);
	}
	
	public void changeDisplay(View v){
		Log.i(TAG, "changeDisplay:");
		CheckBox checkBox = (CheckBox) v;
		if (checkBox.isChecked()){
			Log.i(TAG, "changeDisplay:a afficher");
		} else {
			Log.i(TAG, "changeDisplay:a masquer");
		}
		RelativeLayout parent = (RelativeLayout) v.getParent();
		Log.i(TAG, "nb child:"+parent.getChildCount());
		TextView name = (TextView) parent.getChildAt(0);
		String notebook = (String) name.getText();
		Log.i(TAG, "name:"+notebook);

		Button button = (Button) findViewById(R.id.buttonOk);
		if (adapter.changeValue(notebook, (checkBox.isChecked()) ? 1 : 0)==0){
			button.setEnabled(false);
		} else {
			button.setEnabled(true);
		}
	}

	public void validate(View v){
		Log.i(TAG, "validate");
		if (adapter.getNbCheck()==0){
			Log.i(TAG, "There is no notebook checked ! You must check one or more to continue");
		} else {
			adapter.updateChange(this);
			this.finish();
			//this.startActivity(new Intent(this,Tomdroid.class));
		}
	}
	
	public void close(View v){
		this.finish();
	}
}
