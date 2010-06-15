package org.tomdroid.sync.web;

import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.util.Preferences;

import android.util.Log;

public class SyncServer {

	final String	TAG				= "UserInfo";
	final String	userReference	= Preferences.getString(Preferences.Key.SYNC_SERVER_USER_API);
	private String	notesApiReference;
	private String	userName;
	private String	firstName;
	private String	lastName;
	private Long	syncVersionOnServer;
	private Long	currentSyncGuid;

	public SyncServer(OAuthConnection auth) throws UnknownHostException, JSONException {
		String rawResponse = auth.get(userReference);
		JSONObject response = new JSONObject(rawResponse);
		Log.v(TAG, "userRef response: " + response.toString());
		notesApiReference = response.getJSONObject("notes-ref").getString("api-ref");
		
		userName = response.getString("user-name");
		firstName = response.getString("first-name");
		lastName = response.getString("last-name");
		
		syncVersionOnServer = response.getLong("latest-sync-revision");
		currentSyncGuid = response.getLong("current-sync-guid");
	}

	public String getNotesUri() {
		return notesApiReference;
	}
	
	public boolean isUpToDate(){
		long syncVersionOnClient = (Long)Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION);
		return syncVersionOnClient < syncVersionOnServer;	
	}
}
