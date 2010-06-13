package org.tomdroid.sync.web;

import java.net.UnknownHostException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.Note;
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncMethod;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class SnowySyncMethod extends SyncMethod implements ServiceAuth {
	
	private static final String TAG = "SnowySyncMethod";
	
	public SnowySyncMethod(Activity activity, Handler handler) {
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
	
	public Uri getAuthUri(String server)  throws UnknownHostException {
		
		// Reset the authentication credentials
		OAuthConnection auth = new OAuthConnection();
		return auth.getAuthorizationUrl(server);
	}
	
	public void remoteAuthComplete(final Uri uri) {
		
		execInThread(new Runnable() {
			
			public void run() {

				try {
					// TODO: might be intelligent to show something like a progress dialog
					// else the user might try to sync before the authorization process
					// is complete
					OAuthConnection auth = getAuthConnection();
					boolean result = auth.getAccess(uri.getQueryParameter("oauth_verifier"));

					if (Tomdroid.LOGGING_ENABLED) {
						if (result) {
							Log.i(TAG, "The authorization process is complete.");
						} else
							Log.e(TAG, "Something went wrong during the authorization process.");
					}
				} catch (UnknownHostException e) {
					if (Tomdroid.LOGGING_ENABLED)
						Log.e(TAG, "Internet connection not available");
					sendMessage(NO_INTERNET);
					return;
				}
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
		
		execInThread(new Runnable() {
			
			public void run() {
				OAuthConnection auth = getAuthConnection();
				
				try {
					String rawResponse = auth.get(userRef);
					setSyncProgress(30);
					JSONObject response = new JSONObject(rawResponse);
					String notesUrl = response.getJSONObject("notes-ref").getString("api-ref");
					
					response = new JSONObject(auth.get(notesUrl));
					
					long latestSyncRevision = (Long)Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION);
					setSyncProgress(35);
					
					if (response.getLong("latest-sync-revision") < latestSyncRevision) {
						setSyncProgress(100);
						return;
					}
					
					response = new JSONObject(auth.get(notesUrl + "?include_notes=true"));
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

						JSONObject jsonNote = notes.getJSONObject(i);
						insertNote(new Note(jsonNote), false);
					}
					setSyncProgress(90);
					
					JSONObject jsonNote = notes.getJSONObject(notes.length() - 1);
					insertNote(new Note(jsonNote), true);

					Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, response
							.getLong("latest-sync-revision"));
					setSyncProgress(100);
					
				} catch (JSONException e1) {
					if (Tomdroid.LOGGING_ENABLED) Log.e(TAG, "Problem parsing the server response", e1);
					sendMessage(PARSING_FAILED);
					setSyncProgress(100);
					return;
				} catch (java.net.UnknownHostException e) {
					if (Tomdroid.LOGGING_ENABLED) Log.e(TAG, "Internet connection not available");
					sendMessage(NO_INTERNET);
					setSyncProgress(100);
					return;
				}
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
