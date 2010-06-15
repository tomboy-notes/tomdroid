package org.tomdroid.sync.web;

import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.util.Preferences;

import android.util.Log;

public class UserInfo {

	final String	TAG		= "UserInfo";
	final String	userReference	= Preferences.getString(Preferences.Key.SYNC_SERVER_USER_API);
	String			notesUrl;
	
	public UserInfo(OAuthConnection auth) throws UnknownHostException, JSONException {
		String rawResponse = auth.get(userReference);
		JSONObject response = new JSONObject(rawResponse);
		Log.v(TAG, "userRef response: " + response.toString());
		notesUrl = response.getJSONObject("notes-ref").getString("api-ref");
	}

	public String getNotesUrl() {
		return notesUrl;
	}
}
