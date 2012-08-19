/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2010, 2011 Olivier Bilodeau <olivier@bottomlesspit.org>
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.SearchRecentSuggestions;
import android.text.format.Time;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncService;
import org.tomdroid.sync.web.OAuthConnection;
import org.tomdroid.ui.Tomdroid.SyncMessageHandler;
import org.tomdroid.ui.actionbar.ActionBarPreferenceActivity;
import org.tomdroid.util.FirstNote;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.SearchSuggestionProvider;
import org.tomdroid.util.TLog;

import java.io.File;
import java.util.ArrayList;

public class PreferencesActivity extends ActionBarPreferenceActivity {
	
	private static final String TAG = "PreferencesActivity";
	
	// TODO: put the various preferences in fields and figure out what to do on activity suspend/resume
	private EditTextPreference baseSize = null;
	private EditTextPreference syncServer = null;
	private ListPreference syncService = null;
	private EditTextPreference sdLocation = null;
	private Preference delNotes = null;
	private Preference clearSearchHistory = null;
	private Preference backupNotes = null;
	private Preference delRemoteNotes = null;

	private Activity activity;

	private Handler	 preferencesMessageHandler	= new PreferencesMessageHandler(this);


	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Build.VERSION.SDK_INT < 11)
			requestWindowFeature(Window.FEATURE_CUSTOM_TITLE); // added for actionbarcompat
		
		super.onCreate(savedInstanceState);
		
		this.activity = this;
		SyncManager.setActivity(this);
		SyncManager.setHandler(this.preferencesMessageHandler);
		
		addPreferencesFromResource(R.xml.preferences);
		
		// Fill the Preferences fields
		baseSize = (EditTextPreference)findPreference(Preferences.Key.BASE_TEXT_SIZE.getName());
		syncServer = (EditTextPreference)findPreference(Preferences.Key.SYNC_SERVER.getName());
		syncService = (ListPreference)findPreference(Preferences.Key.SYNC_SERVICE.getName());
		sdLocation = (EditTextPreference)findPreference(Preferences.Key.SD_LOCATION.getName());
		clearSearchHistory = (Preference)findPreference(Preferences.Key.CLEAR_SEARCH_HISTORY.getName());
		delNotes = (Preference)findPreference(Preferences.Key.DEL_ALL_NOTES.getName());
		delRemoteNotes = (Preference)findPreference(Preferences.Key.DEL_REMOTE_NOTES.getName());
		backupNotes = (Preference)findPreference(Preferences.Key.BACKUP_NOTES.getName());
		// Set the default values if nothing exists
		setDefaults();
		
		// Fill the services combo list
		fillLists();
		
		// Enable or disable the server field depending on the selected sync service
		setServer(syncService.getValue());
		
		syncService.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String selectedSyncServiceKey = (String)newValue;
				
				// did the selection change?
				if (!syncService.getValue().contentEquals(selectedSyncServiceKey)) {
					TLog.d(TAG, "preference change triggered");
					
					syncServiceChanged(selectedSyncServiceKey);
				}
				return true;
			}
		});
		
		// force reauthentication if the sync server changes
		syncServer.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference,
					Object serverUri) {
				
				if (serverUri == null) {
					Toast.makeText(PreferencesActivity.this,
							getString(R.string.prefServerEmpty),
							Toast.LENGTH_SHORT).show();
					return false;
				}
				if ((serverUri.toString().contains("\t")) || (serverUri.toString().contains(" ")) || (serverUri.toString().contains("\n"))){
					noValidEntry(serverUri.toString());
					return false;
				}
				syncServer.setSummary((String)serverUri);
				
				// TODO is this necessary? hasn't it changed already?
				Preferences.putString(Preferences.Key.SYNC_SERVER, (String)serverUri);

				reauthenticate();
				return true;
			}
			
		});
		
		// Change the Folder Location
		sdLocation.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference, Object locationUri) {

				if (locationUri.equals(Preferences.getString(Preferences.Key.SD_LOCATION))) { 
					return false;
				}
				if ((locationUri.toString().contains("\t")) || (locationUri.toString().contains("\n"))) { 
					noValidEntry(locationUri.toString());
					return false;
				}
				
				File path = new File(Environment.getExternalStorageDirectory()
						+ "/" + locationUri + "/");

				if(!path.exists()) {
					TLog.w(TAG, "Folder {0} does not exist.", path);
					folderNotExisting(path.toString());
					return false;
				}
				
				Preferences.putString(Preferences.Key.SD_LOCATION, locationUri.toString());
				TLog.d(TAG, "Changed Folder to: " + path.toString());

				Tomdroid.NOTES_PATH = path.toString();
				sdLocation.setSummary(Tomdroid.NOTES_PATH);

				resetLocalDatabase();
				return true;
			}
		});
		
		//delete Search History
		clearSearchHistory.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        public boolean onPreferenceClick(Preference preference) {
	            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(PreferencesActivity.this,
	                    SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
	            suggestions.clearHistory();
	            	
	        	Toast.makeText(getBaseContext(),
                        getString(R.string.deletedSearchHistory),
                        Toast.LENGTH_LONG).show();
	        	TLog.d(TAG, "Deleted search history.");
	        	
	        	return true;
	        }
	    });

		baseSize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				try {
					Float.parseFloat((String)newValue);
					Preferences.putString(Preferences.Key.BASE_TEXT_SIZE, (String)newValue);
				}
				catch(Exception e) {
		        	Toast.makeText(getBaseContext(),
	                        getString(R.string.illegalTextSize),
	                        Toast.LENGTH_LONG).show();
		        	TLog.e(TAG, "Illegal text size in preferences");
		        	return false;
				}
				baseSize.setSummary((String)newValue);
				return true;
			}
		});

		delNotes.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
	        public boolean onPreferenceClick(Preference preference) {
				final Activity activity = PreferencesActivity.this;
				new AlertDialog.Builder(activity)
		        .setIcon(android.R.drawable.ic_dialog_alert)
		        .setTitle(R.string.delete_all)
		        .setMessage(R.string.delete_all_message)
		        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

		            public void onClick(DialogInterface dialog, int which) {
		            	resetLocalDatabase();
		           }

		        })
		        .setNegativeButton(R.string.no, null)
		        .show();

				return true;
			}
		});

		delRemoteNotes.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
	        public boolean onPreferenceClick(Preference preference) {
				final Activity activity = PreferencesActivity.this;
				new AlertDialog.Builder(activity)
		        .setIcon(android.R.drawable.ic_dialog_alert)
		        .setTitle(R.string.delete_remote_notes)
		        .setMessage(R.string.delete_remote_notes_message)
		        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

		            public void onClick(DialogInterface dialog, int which) {
		            	resetRemoteService();
		           }

		        })
		        .setNegativeButton(R.string.no, null)
		        .show();

				return true;
			}
		});
		
		
		backupNotes.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
	        public boolean onPreferenceClick(Preference preference) {
				final Activity activity = PreferencesActivity.this;
				new AlertDialog.Builder(activity)
		        .setIcon(android.R.drawable.ic_dialog_alert)
		        .setTitle(R.string.backup_notes_title)
		        .setMessage(R.string.backup_notes)
		        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

		            public void onClick(DialogInterface dialog, int which) {
		            	SyncManager.getService("sdcard").backupNotes();
		           }

		        })
		        .setNegativeButton(R.string.no, null)
		        .show();

				return true;
			}
		});		
	}

	private void reauthenticate() {

		// don't do anything, we'll authenticate on sync instead
		// save empty config, wiping old config
		
		OAuthConnection auth = new OAuthConnection();
		auth.saveConfiguration();
	}
	
	private void fillLists()
	{
		ArrayList<SyncService> availableServices = SyncManager.getInstance().getServices();
		CharSequence[] entries = new CharSequence[availableServices.size()];
		CharSequence[] entryValues = new CharSequence[availableServices.size()];
		
		for (int i = 0; i < availableServices.size(); i++) {
			entries[i] = availableServices.get(i).getDescription();
			entryValues[i] = availableServices.get(i).getName();
		}
		
		syncService.setEntries(entries);
		syncService.setEntryValues(entryValues);

	}
	
	private void setDefaults()
	{
		String defaultServer = (String)Preferences.Key.SYNC_SERVER.getDefault();
		syncServer.setDefaultValue(defaultServer);
		if(syncServer.getText() == null)
			syncServer.setText(defaultServer);
		syncServer.setSummary(Preferences.getString(Preferences.Key.SYNC_SERVER));

		String defaultService = (String)Preferences.Key.SYNC_SERVICE.getDefault();
		syncService.setDefaultValue(defaultService);
		if(syncService.getValue() == null)
			syncService.setValue(defaultService);
		
		String defaultLocation = (String)Preferences.Key.SD_LOCATION.getDefault();
		sdLocation.setDefaultValue(defaultLocation);
		if(sdLocation.getText() == null)
			sdLocation.setText(defaultLocation);

		String defaultSize = (String)Preferences.Key.BASE_TEXT_SIZE.getDefault();
		baseSize.setDefaultValue(defaultSize);
		baseSize.setSummary(Preferences.getString(Preferences.Key.BASE_TEXT_SIZE));
		if(baseSize.getText() == null)
			baseSize.setText(defaultSize);
	}

	private void setServer(String syncServiceKey) {

		SyncService service = SyncManager.getInstance().getService(syncServiceKey);

		if (service == null)
			return;

		syncServer.setEnabled(service.needsServer());
		syncService.setSummary(service.getDescription());
		backupNotes.setEnabled(!service.needsLocation()); // if not using sd card, allow backup
		sdLocation.setSummary(Tomdroid.NOTES_PATH);
	}
		
	private void connectionFailed() {
		new AlertDialog.Builder(this)
			.setMessage(getString(R.string.prefSyncConnectionFailed))
			.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}})
			.show();
	}
	
	private void folderNotExisting(String path) {
		new AlertDialog.Builder(this)
			.setTitle(getString(R.string.error))
			.setMessage(String.format(getString(R.string.prefFolderCreated), path))
			.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}})
			.show();
	}
	
	private void noValidEntry(String Entry) {
		new AlertDialog.Builder(this)
			.setTitle(getString(R.string.error))
			.setMessage(String.format(getString(R.string.prefNoValidEntry), Entry))
			.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}})
			.show();
	}

	//TODO use LocalStorage wrapper from two-way-sync branch when it get's merged
	private void resetLocalDatabase() {
		getContentResolver().delete(Tomdroid.CONTENT_URI, null, null);
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, 0);
		Preferences.putString(Preferences.Key.LATEST_SYNC_DATE, new Time().format3339(false));
		
		// add a first explanatory note
		NoteManager.putNote(this, FirstNote.createFirstNote());
		
		String text = getString(R.string.messageDatabaseReset);
		Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
	}
	
	private void resetRemoteService() {
		SyncManager.getInstance().getCurrentService().deleteAllNotes();

	}
	
	/**
	 * Housekeeping when a syncServer changes
	 * @param syncServiceKey - key of the new sync service 
	 */
	private void syncServiceChanged(String syncServiceKey) {
		
		setServer(syncServiceKey);
		
		// TODO this should be refactored further, notice that setServer performs the same operations 
		SyncService service = SyncManager.getInstance().getService(syncServiceKey);
		
		if (service == null)
			return;
		
		// not resetting database, since now we may have new notes, and want to move them from one service to another, etc.
		
		// reset last sync date, so we can push local notes to the service - to pull instead, we have "revert all"
		
		Preferences.putString(Preferences.Key.LATEST_SYNC_DATE, new Time().format3339(false));
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, 0);

	}

	public class PreferencesMessageHandler extends Handler {
		
		private Activity activity;
		
		public PreferencesMessageHandler(Activity activity) {
			this.activity = activity;
		}
	
		@Override
		public void handleMessage(Message msg) {
	
			String serviceDescription = SyncManager.getInstance().getCurrentService().getDescription();
			String text = "";

			TLog.d(TAG, "PreferencesMessageHandler message: {0}",msg.what);

			switch (msg.what) {
				case SyncService.REMOTE_NOTES_DELETED:
					text = getString(R.string.messageRemoteNotesDeleted);
					text = String.format(text,serviceDescription);
					Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
					break;
				case SyncService.NOTES_BACKED_UP:
					text = getString(R.string.messageNotesBackedUp);
					Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
					break;
				case SyncService.NOTES_RESTORED:
					text = getString(R.string.messageNotesRestored);
					Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
					break;
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
	        	// app icon in action bar clicked; go home
                Intent intent = new Intent(this, Tomdroid.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            	return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
