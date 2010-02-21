package org.tomdroid.sync.web;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.Note;
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncService;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;

public class SnowySyncService extends SyncService implements ServiceAuth {
	
	private static final String TAG = "SnowySyncService";
	
	public SnowySyncService(Activity activity) {
		super(activity);
	}
	
	@Override
	public String getDescription() {
		return "Snowy";
	}

	@Override
	public String getName() {
		return "snowy";
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
	
	public Uri getAuthUri(String server) {
		
		// Reset the authentication credentials
		OAuthConnection auth = new OAuthConnection();
		return auth.getAuthorizationUrl(server);
	}
	
	public void remoteAuthComplete(final Uri uri) {
		
		execInThread(new Runnable() {
			
			public void run() {
				
				// TODO: might be intelligent to show something like a progress dialog
				// else the user might try to sync before the authorization process
				// is complete
				OAuthConnection auth = getAuthConnection();
				boolean result = auth.getAccess(uri.getQueryParameter("oauth_verifier"));
				
				if (Tomdroid.LOGGING_ENABLED) {
					if (result) {
						Log.i(TAG, "The authorization process is complete.");
					}
					else
						Log.e(TAG, "Something went wrong during the authorization process.");
				}
			}
		});
	}

	@Override
	public void sync() {
		
		// start loading snowy notes
		if (Tomdroid.LOGGING_ENABLED) Log.v(TAG, "Loading Snowy notes");
		
		final String userRef = Preferences.getString(Preferences.Key.SYNC_SERVER_USER_API);
		
		execInThread(new Runnable() {
			
			public void run() {
				
				OAuthConnection auth = getAuthConnection();
				
				try {
					JSONObject response = new JSONObject(auth.get(userRef));
					String notesUrl = response.getJSONObject("notes-ref").getString("api-ref")+"?include_notes=true";
					
					response = new JSONObject(auth.get(notesUrl));
					JSONArray notes = response.getJSONArray("notes");
					
					for (int i = 0; i < notes.length(); i++) {
						
						JSONObject jsonNote = notes.getJSONObject(i);
						insertNote(new Note(jsonNote));
					}
					
				} catch (JSONException e1) {
					e1.printStackTrace();
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
