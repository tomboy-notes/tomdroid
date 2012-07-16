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
package org.noahy.tomdroid.sync.web;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.noahy.tomdroid.Note;
import org.noahy.tomdroid.R;
import org.noahy.tomdroid.sync.ServiceAuth;
import org.noahy.tomdroid.sync.SyncService;
import org.noahy.tomdroid.util.ErrorList;
import org.noahy.tomdroid.util.Preferences;
import org.noahy.tomdroid.util.TLog;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SnowySyncService extends SyncService implements ServiceAuth {
	
	private static final String TAG = "SnowySyncService";
	
	public SnowySyncService(Activity activity, Handler handler) {
		super(activity, handler);
	}
	
	@Override
	public int getDescriptionAsId() {
		return R.string.prefTomboyWeb;
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
	public boolean needsLocation() {
		return false;
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
					TLog.e(TAG, "Internet connection not available");
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
						TLog.i(TAG, "The authorization process is complete.");
						sync();
					} else {
						TLog.e(TAG, "Something went wrong during the authorization process.");
					}
				} catch (UnknownHostException e) {
					TLog.e(TAG, "Internet connection not available");
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
		TLog.v(TAG, "Loading Snowy notes");
		
		final String userRef = Preferences.getString(Preferences.Key.SYNC_SERVER_USER_API);
		
		syncInThread(new Runnable() {
			
			public void run() {
				
				OAuthConnection auth = getAuthConnection();
				
				try {
					TLog.v(TAG, "contacting "+userRef);
					String rawResponse = auth.get(userRef);
					setSyncProgress(30);
					
					try {
						JSONObject response = new JSONObject(rawResponse);
						String notesUrl = response.getJSONObject("notes-ref").getString("api-ref");
						
						TLog.v(TAG, "contacting "+notesUrl);
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

						TLog.v(TAG, "number of notes: {0}", notes.length());

						
						// Insert or update the rest of the notes
						for (int i = 0; i < notes.length() - 1; i++) {
							
							TLog.v(TAG, "parsing note numer: {0}", i+1);
							
							JSONObject jsonNote = null;
							
							try {
								jsonNote = notes.getJSONObject(i);
								insertNote(new Note(jsonNote));
							} catch (JSONException e) {
								TLog.e(TAG, e, "Problem parsing the server response");
								String json = (jsonNote != null) ? jsonNote.toString(2) : rawResponse;
								sendMessage(PARSING_FAILED, ErrorList.createErrorWithContents("JSON parsing", "json", e, json));
							}
						}
						setSyncProgress(90);
						
						// Editor comment: do all but one? can someone clarify?
						JSONObject jsonNote = notes.getJSONObject(notes.length() - 1);
						insertNote(new Note(jsonNote));

						Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, response
								.getLong("latest-sync-revision"));
						
					} catch (JSONException e) {
						TLog.e(TAG, e, "Problem parsing the server response");
						sendMessage(PARSING_FAILED, ErrorList.createErrorWithContents("JSON parsing", "json", e, rawResponse));
						setSyncProgress(100);
						return;
					}
					
					setSyncProgress(100);
					
				} catch (java.net.UnknownHostException e) {
					TLog.e(TAG, "Internet connection not available");
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
	
	// new methods to T Edit

	@Override
	protected void pushNote(Note note){
		
		OAuthConnection auth = getAuthConnection();
		try {
			TLog.v(TAG, "putting note to server");
			String userRef = Preferences.getString(Preferences.Key.SYNC_SERVER_USER_API);
			String rawResponse = auth.get(userRef);

			try {
				TLog.v(TAG, "creating JSON");
				JSONObject data=new JSONObject();
				data.put("latest-sync-revision",(Long)Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION)+1);

				JSONArray notesJ = new JSONArray();

				JSONObject noteJ=new JSONObject();
				noteJ.put("guid", note.getGuid());
				noteJ.put("title",note.getTitle());
				noteJ.put ("note-content",note.getXmlContent());
				noteJ.put ("note-content-version","0.1");
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz");
				noteJ.put ("last-change-date", sdf.format(note.getLastChangeDate().toMillis(false)));
				
				notesJ.put(noteJ);
				data.put("note-changes",notesJ);

				JSONObject response = new JSONObject(rawResponse);

				TLog.v(TAG, "request data: {0}",rawResponse);

				String notesUrl = response.getJSONObject("notes-ref").getString("api-ref");


				TLog.v(TAG, "put url: {0}", notesUrl);

				TLog.v(TAG, "put data: {0}",data.toString());
				
				response = new JSONObject(auth.put(notesUrl,data.toString()));
				
				TLog.v(TAG, "put response: {0}",response.toString());
				Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, response
						.getLong("latest-sync-revision"));
			} 
			catch (JSONException e) {
				TLog.e(TAG, e, "Problem parsing the server response");
				sendMessage(PARSING_FAILED, ErrorList.createErrorWithContents("JSON parsing", "json", e, rawResponse));
				return;
			}
		}
		catch (java.net.UnknownHostException e) {
			TLog.e(TAG, "Internet connection not available");
			sendMessage(NO_INTERNET);
			return;
		}
		sendMessage(NOTE_PUSHED);
	}
	@Override
	protected void deleteNote(String guid){
		
		OAuthConnection auth = getAuthConnection();
		try {
			TLog.v(TAG, "putting note to server");
			String userRef = Preferences.getString(Preferences.Key.SYNC_SERVER_USER_API);
			String rawResponse = auth.get(userRef);

			try {
				TLog.v(TAG, "creating JSON");
				JSONObject data=new JSONObject();
				data.put("latest-sync-revision",(Long)Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION)+1);

				JSONArray notesJ = new JSONArray();

				JSONObject noteJ=new JSONObject();
				noteJ.put("guid", guid);
				noteJ.put("command","delete");
				
				notesJ.put(noteJ);
				data.put("note-changes",notesJ);

				JSONObject response = new JSONObject(rawResponse);

				TLog.v(TAG, "request data: {0}",rawResponse);

				String notesUrl = response.getJSONObject("notes-ref").getString("api-ref");


				TLog.v(TAG, "put url: {0}", notesUrl);

				TLog.v(TAG, "put data: {0}",data.toString());
				
				response = new JSONObject(auth.put(notesUrl,data.toString()));
				
				TLog.v(TAG, "put response: {0}",response.toString());
				Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, response
						.getLong("latest-sync-revision"));
			} 
			catch (JSONException e) {
				TLog.e(TAG, e, "Problem parsing the server response");
				sendMessage(PARSING_FAILED, ErrorList.createErrorWithContents("JSON parsing", "json", e, rawResponse));
				return;
			}
		}
		catch (java.net.UnknownHostException e) {
			TLog.e(TAG, "Internet connection not available");
			sendMessage(NO_INTERNET);
			return;
		}
		sendMessage(NOTE_DELETED);
	}	
}
