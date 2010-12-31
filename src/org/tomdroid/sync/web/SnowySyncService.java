/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
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
package org.tomdroid.sync.web;

import java.net.UnknownHostException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.SyncService;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.ErrorList;
import org.tomdroid.util.FirstNote;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.util.Log;
import android.widget.Toast;

public class SnowySyncService extends SyncService {
	
	private static final String TAG = "SnowySyncService";
	
	public SnowySyncService(Activity activity, Handler handler) {
		super(activity, handler);
	}
	
	@Override
	public String getDescription() {
		return "Tomboy Web";
	}

	@Override
	public String getName() {
		return "tomboy-web";
	}
	
	public boolean isConfigured() {
		OAuthConnection auth = getAuthConnection();
		return auth.isAuthenticated();
	}

	public void getAuthUri(final String server, final Handler handler) {
		
		execInThread(new Runnable() {
			
			public void run() {
				
				// Reset the authentication credentials
				OAuthConnection auth = new OAuthConnection();
				Uri authUri = null;
				
				try {
					authUri = auth.getAuthorizationUrl(server);
					
				} catch (UnknownHostException e) {
					Log.e(TAG, "Internet connection not available");
					sendMessage(NO_INTERNET);
				}
				
				Message message = new Message();
				message.obj = authUri;
				handler.sendMessage(message);
			}
			
		});
	}
	
	public void remoteAuthComplete(final Uri uri, final Handler handler) {
		
		execInThread(new Runnable() {
			
			public void run() {

				try {
					// TODO: might be intelligent to show something like a progress dialog
					// else the user might try to sync before the authorization process
					// is complete
					OAuthConnection auth = getAuthConnection();
					boolean result = auth.getAccess(uri.getQueryParameter("oauth_verifier"));

					if (result) {
						if (Tomdroid.LOGGING_ENABLED) Log.i(TAG, "The authorization process is complete.");
					} else {
						Log.e(TAG, "Something went wrong during the authorization process.");
					}
				} catch (UnknownHostException e) {
					Log.e(TAG, "Internet connection not available");
					sendMessage(NO_INTERNET);
				}
				
				// We don't care what we send, just remove the dialog
				handler.sendEmptyMessage(0);
			}
		});
	}
	
	@Override
	public boolean isSyncable(){
		 return super.isSyncable() && isConfigured();
	}
	

	@Override
	protected void sync() {
		
		// start loading snowy notes
		setSyncProgress(0);
		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG, "Loading Snowy notes");
		
		final String userRef = Preferences.getString(Preferences.Key.SYNC_SERVER_USER_API);
		
		syncInThread(new Runnable() {
			
			public void run() {
				
				OAuthConnection auth = getAuthConnection();
				
				try {
					String rawResponse = auth.get(userRef);
					setSyncProgress(30);
					
					try {
						JSONObject response = new JSONObject(rawResponse);
						String notesUrl = response.getJSONObject("notes-ref").getString("api-ref");
						
						response = new JSONObject(auth.get(notesUrl));
						
						long latestSyncRevision = (Long)Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION);
						setSyncProgress(35);
						
						if (response.getLong("latest-sync-revision") < latestSyncRevision) {
							setSyncProgress(100);
							return;
						}
						
						rawResponse = auth.get(notesUrl + "?include_notes=true");
						
						response = new JSONObject(rawResponse);
						JSONArray notes = response.getJSONArray("notes");
						setSyncProgress(60);
						
						// Delete the notes that are not in the database
						ArrayList<String> remoteGuids = new ArrayList<String>();

						for (int i = 0; i < notes.length(); i++) {
							remoteGuids.add(notes.getJSONObject(i).getString("guid"));
						}

						deleteNotes(remoteGuids);
						setSyncProgress(70);
						
						// Insert or update the rest of the notes
						for (int i = 0; i < notes.length() - 1; i++) {
							
							JSONObject jsonNote = null;
							
							try {
								jsonNote = notes.getJSONObject(i);
								insertNote(new Note(jsonNote));
							} catch (JSONException e) {
								Log.e(TAG, "Problem parsing the server response", e);
								String json = (jsonNote != null) ? jsonNote.toString(2) : rawResponse;
								sendMessage(PARSING_FAILED, ErrorList.createErrorWithContents("JSON parsing", "json", e, json));
							}
						}
						setSyncProgress(90);
						
						JSONObject jsonNote = notes.getJSONObject(notes.length() - 1);
						insertNote(new Note(jsonNote));

						Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, response
								.getLong("latest-sync-revision"));
						
					} catch (JSONException e) {
						Log.e(TAG, "Problem parsing the server response", e);
						sendMessage(PARSING_FAILED, ErrorList.createErrorWithContents("JSON parsing", "json", e, rawResponse));
						setSyncProgress(100);
						return;
					}
					
					setSyncProgress(100);
					
				} catch (java.net.UnknownHostException e) {
					Log.e(TAG, "Internet connection not available");
					sendMessage(NO_INTERNET);
					setSyncProgress(100);
					return;
				}
				
				sendMessage(PARSING_COMPLETE);
			}
		});
	}

	@Override
	public void fillPreferences(PreferenceGroup group, final Activity activity) {
		
//		PreferenceCategory pc = new PreferenceCategory(activity);
//		pc.setTitle("Web Preferences");
//		group.addPreference(pc);
		
		EditTextPreference syncServer = new EditTextPreference(activity);
		syncServer.setTitle(R.string.prefSyncServer);
		syncServer.setPositiveButtonText(R.string.prefAuthenticate);
		
		String defaultServer = (String)Preferences.Key.SYNC_SERVER.getDefault();
		syncServer.setDefaultValue(defaultServer);
		if(syncServer.getText() == null)
			syncServer.setText(defaultServer);

//		pc.addPreference(syncServer);
		group.addPreference(syncServer);
		
		// Re-authenticate if the sync server changes
		syncServer.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference,
					Object serverUri) {
				
				if (serverUri == null) {
					Toast.makeText(activity,
							activity.getString(R.string.prefServerEmpty),
							Toast.LENGTH_SHORT).show();
					return false;
				}
			    
				authenticate((String) serverUri, activity);
				return true;
			}
			
		});
	}
	
	private void authenticate(String serverUri, final Activity activity) {

		// update the value before doing anything
		Preferences.putString(Preferences.Key.SYNC_SERVER, serverUri);

		// service needs authentication
		Log.i(TAG, "Creating dialog");

		final ProgressDialog authProgress = ProgressDialog.show(activity, "",
				"Authenticating. Please wait...", true, false);

		Handler handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {

				boolean wasSuccsessful = false;
				Uri authorizationUri = (Uri) msg.obj;
				if (authorizationUri != null) {

					Intent i = new Intent(Intent.ACTION_VIEW, authorizationUri);
					activity.startActivity(i);
					wasSuccsessful = true;

				} else {
					// Auth failed, don't update the value
					wasSuccsessful = false;
				}

				if (authProgress != null)
					authProgress.dismiss();

				if (wasSuccsessful) {
					resetLocalDatabase(activity);
				} else {
					connectionFailed(activity);
				}
			}
		};

		this.getAuthUri(serverUri, handler);
	}

	private void connectionFailed(Activity activity) {
		new AlertDialog.Builder(activity)
			.setMessage(activity.getString(R.string.prefSyncConnectionFailed))
			.setNeutralButton(activity.getString(R.string.btnOk), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}})
			.show();
	}
	
	//TODO use LocalStorage wrapper from two-way-sync branch when it get's merged
	private void resetLocalDatabase(Activity activity) {
		activity.getContentResolver().delete(Tomdroid.CONTENT_URI, null, null);
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, 0);
		
		// add a first explanatory note
		// TODO this will be problematic with two-way sync
		NoteManager.putNote(activity, FirstNote.createFirstNote());
	}
	
	
	private OAuthConnection getAuthConnection() {
		
		OAuthConnection auth = new OAuthConnection();
		
		auth.accessToken = Preferences.getString(Preferences.Key.ACCESS_TOKEN);
		auth.accessTokenSecret = Preferences.getString(Preferences.Key.ACCESS_TOKEN_SECRET);
		auth.requestToken = Preferences.getString(Preferences.Key.REQUEST_TOKEN);
		auth.requestTokenSecret = Preferences.getString(Preferences.Key.REQUEST_TOKEN_SECRET);
		auth.oauth10a = Preferences.getBoolean(Preferences.Key.OAUTH_10A);
		auth.authorizeUrl = Preferences.getString(Preferences.Key.AUTHORIZE_URL);
		auth.accessTokenUrl = Preferences.getString(Preferences.Key.ACCESS_TOKEN_URL);
		auth.requestTokenUrl = Preferences.getString(Preferences.Key.REQUEST_TOKEN_URL);
		auth.rootApi = Preferences.getString(Preferences.Key.SYNC_SERVER_ROOT_API);
		auth.userApi = Preferences.getString(Preferences.Key.SYNC_SERVER_USER_API);
		
		return auth;
	}
}
