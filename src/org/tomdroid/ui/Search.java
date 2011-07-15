package org.tomdroid.ui;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.SyncManager;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Search extends ListActivity {
	
	// Logging info
	private static final String	TAG					= "Tomdroid Search";
	// UI elements
	private TextView title;
	// UI to data model glue
	private TextView			listEmptyView;	
	// UI feedback handler
	private Handler	syncMessageHandler	= new SyncMessageHandler(this);

	private ListAdapter			adapter;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.search);
	    
		title = (TextView) findViewById(R.id.title);
		title.setTextColor(Color.DKGRAY);
		title.setTextSize(18.0f);
		title.setText((CharSequence) getString(R.string.SearchResultTitle));
	    
	    handleIntent(getIntent());

	}

	@Override
	protected void onNewIntent(Intent intent) {
	    setIntent(intent);
	    handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		//handles a search query
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      showResults(query);
	    }
	    
	}

	public void showResults(String query) {
		if (Tomdroid.LOGGING_ENABLED) Log.d(TAG,"Start searching for: "+query);
		
		
		// adapter that binds the ListView UI to the notes in the note manager
		adapter = NoteManager.getListAdapter(this, query);
		setListAdapter(adapter);
		
		// set the view shown when query not found
		listEmptyView = (TextView) findViewById(R.id.no_results);
		listEmptyView.setText(getString(R.string.strNoResults, new Object[] {query}));
		getListView().setEmptyView(listEmptyView);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		Cursor item = (Cursor) adapter.getItem(position);
		int noteId = item.getInt(item.getColumnIndexOrThrow(Note.ID));

		Uri intentUri = Uri.parse(Tomdroid.CONTENT_URI + "/" + noteId);
		Intent i = new Intent(Intent.ACTION_VIEW, intentUri, this, ViewNote.class);
		startActivity(i);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		SyncManager.setActivity(this);
		SyncManager.setHandler(this.syncMessageHandler);
	}
}
