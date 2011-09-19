package org.tomdroid.ui;

import org.tomdroid.Notebook;
import org.tomdroid.NotebookManager;
import org.tomdroid.R;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Notebooks extends ListActivity {
	
	// Logging info
	private static final String	TAG					= "Notebooks";
	
	// UI to data model glue
	private ListAdapter			adapter;
	
	/** Called when the activity is created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notebooks);
		Log.i(TAG, "setContentView OK");
		
		// adapter that binds the ListView UI to the notebooks in the note manager
		//adapter = NoteManager.getListAdapterNotebook(this);
		adapter = NotebookManager.getNotbookListAdapter(this);
		setListAdapter(adapter);		
		
		/*
		// all notebooks
		final TextView allNotebooks = (TextView) findViewById(R.id.allNotebooks);
		allNotebooks.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {						
				Bundle bundle = new Bundle();
				bundle.putString("notebook", null);

				Intent i = new Intent(v.getContext(), Tomdroid.class);
				i.putExtras(bundle);
				startActivity(i);
			}
		});
		*/
			
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
		
		Uri uriNotebooks = Tomdroid.CONTENT_URI_NOTEBOOK;
		String[] whereArgs = new String[1];
		whereArgs[0] = notebook;
		
		ContentValues values = new ContentValues();
		values.put(Notebook.DISPLAY, (checkBox.isChecked()) ? 1 : 0);
		
		ContentResolver cr = getContentResolver();		
		cr.update(uriNotebooks, values, Notebook.NAME +" = ?", whereArgs);
		
		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Notebook updated in content provider. Name:"+notebook);
	}
}
