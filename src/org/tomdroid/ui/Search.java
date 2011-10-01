/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2011 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2011 Stefan Hammer <j.4@gmx.at>
 * 
 * This file is part of Tomdroid.
 * 
 * Tomdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Tomdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Tomdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomdroid.ui;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.util.SearchSuggestionProvider;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchRecentSuggestions;
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
	    
	    //adds query to search history suggestions
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	        String query = intent.getStringExtra(SearchManager.QUERY);
	        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
	                SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
	        suggestions.saveRecentQuery(query, null);
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
