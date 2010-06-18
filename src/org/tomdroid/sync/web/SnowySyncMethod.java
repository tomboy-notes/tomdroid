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
		OAuthConnection auth = SyncServer.getAuthConnection();
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
	
	public Uri getAuthUri(String serverUri)  throws UnknownHostException {
		
		// Reset the authentication credentials
		OAuthConnection auth = new OAuthConnection();
		return auth.getAuthorizationUrl(serverUri);
	}
	
	public void remoteAuthComplete(final Uri uri) {
		
		execInThread(new Runnable() {
			
			public void run() {

				try {
					// TODO: might be intelligent to show something like a progress dialog
					// else the user might try to sync before the authorization process
					// is complete
					OAuthConnection auth = SyncServer.getAuthConnection();
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
		
		execInThread(new Runnable() {
			
			public void run() {
				
				try {
					SyncServer server = new SyncServer();
					setSyncProgress(30);

					if (server.isInSync()) {
						setSyncProgress(100);
						return;
					}

					JSONArray notes = server.getNotes(); 
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

					server.onSyncDone();
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
	
}
