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

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.ui.actionbar.ActionBarListActivity;
import org.tomdroid.util.SearchSuggestionProvider;
import org.tomdroid.util.TLog;

public class Search extends ActionBarListActivity {
	
	// Logging info
	private static final String	TAG					= "Tomdroid Search";
	// UI to data model glue
	private TextView			listEmptyView;	
	// UI feedback handler

	private ListAdapter			adapter;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.search);

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
		TLog.d(TAG, "Start searching for: {0}", query);
		
		
		// adapter that binds the ListView UI to the notes in the note manager
		adapter = NoteManager.getListAdapterForSearchResults(this, query);
		setListAdapter(adapter);
		
		// set the view shown when query not found
		listEmptyView = (TextView) findViewById(R.id.no_results);
		listEmptyView.setText(getString(R.string.strNoResults, query));
		getListView().setEmptyView(listEmptyView);
	}
	
	@TargetApi(11)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Create the menu based on what is defined in res/menu/main.xml
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search, menu);

        // Calling super after populating the menu is necessary here to ensure that the
        // action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);

	}
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
        	case android.R.id.home:
        		// app icon in action bar clicked; go home
                Intent intent = new Intent(this, Tomdroid.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            	return true;
				
			case R.id.menuSearch:
				startSearch(null, false, null, false);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri intentUri = Tomdroid.getNoteIntentUri(((Note) adapter.getItem(position)).getDbId());
		Intent i = new Intent(Intent.ACTION_VIEW, intentUri, this, ViewNote.class);
		startActivity(i);
	}
	
	@Override
	public void onResume(){
		super.onResume();
	}
}
