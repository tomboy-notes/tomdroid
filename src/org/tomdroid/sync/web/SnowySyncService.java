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
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncService;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.ErrorList;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SnowySyncService extends SyncService implements ServiceAuth {
	
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

	@Override
	public boolean needsServer() {
		return true;
	}
	
	@Override
	public boolean needsAuth() {
		return true;
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
