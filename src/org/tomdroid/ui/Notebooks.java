package org.tomdroid.ui;

import org.tomdroid.NoteManager;
import org.tomdroid.R;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Notebooks extends ListActivity {
	
	// Logging info
	private static final String	TAG					= "Notebooks";
	
	// UI to data model glue
	private TextView			listEmptyView;
	private ListAdapter			adapter;
	
	/** Called when the activity is created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notebooks);
		
		// adapter that binds the ListView UI to the notebooks in the note manager
		Log.i(TAG,"setContentView OK");
		adapter = NoteManager.getListAdapterNotebook(this);
		Log.i(TAG,"adapter OK");
		setListAdapter(adapter);
		Log.i(TAG,"setListAdapter OK");

		// set the view shown when the list is empty
		// TODO default empty-list text is butt-ugly!
		listEmptyView = (TextView) findViewById(R.id.list_empty);
		Log.i(TAG,"listEmptyView OK");
		getListView().setEmptyView(listEmptyView);
		Log.i(TAG,"fin OK");
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
		
	}
}
