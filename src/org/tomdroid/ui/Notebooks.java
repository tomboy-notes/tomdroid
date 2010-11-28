package org.tomdroid.ui;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.Notebook;
import org.tomdroid.R;
import org.tomdroid.sync.SyncManager;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
		adapter = NoteManager.getListAdapterNotebook(this);
		setListAdapter(adapter);		
		
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
}
