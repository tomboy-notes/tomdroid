package org.tomdroid.sync.web;

import java.net.UnknownHostException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.util.Preferences;

import android.util.Log;

public class SyncServer {

	final String			TAG				= "UserInfo";
	final String			userReference	= Preferences
													.getString(Preferences.Key.SYNC_SERVER_USER_API);
	private String			notesApiReference;
	String					userName;
	String					firstName;
	String					lastName;
	Long					syncVersionOnServer;
	Long					currentSyncGuid;
	private OAuthConnection	authConnection;

	public SyncServer() throws UnknownHostException, JSONException {
		authConnection = getAuthConnection();

		String rawResponse = authConnection.get(userReference);
		JSONObject response = new JSONObject(rawResponse);
		readMetaData(response);
	}

	protected void readMetaData(JSONObject response) throws JSONException {
		Log.v(TAG, "userRef response: " + response.toString());
		notesApiReference = response.getJSONObject("notes-ref").getString("api-ref");

		userName = response.getString("user-name");
		firstName = response.getString("first-name");
		lastName = response.getString("last-name");

		syncVersionOnServer = response.getLong("latest-sync-revision");
		currentSyncGuid = response.getLong("current-sync-guid");
	}

	private String getNotesUri() {
		return notesApiReference;
	}

	public boolean isInSync() {
		long syncVersionOnClient = (Long) Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION);
		return syncVersionOnClient == syncVersionOnServer;
	}

	public JSONArray getNotes() throws UnknownHostException, JSONException {
		JSONObject response = new JSONObject(authConnection.get(getNotesUri()
				+ "?include_notes=true"));
		return response.getJSONArray("notes");
	}

	public void onSyncDone() {
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, syncVersionOnServer);
	}

	public static OAuthConnection getAuthConnection() {

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

	public ArrayList<NoteUpdate> getNoteUpdates() throws UnknownHostException, JSONException {
		ArrayList<NoteUpdate> updates = new ArrayList<NoteUpdate>();

		JSONObject response = new JSONObject(authConnection.get(getNotesUri()
				+ "?include_notes=true&since="
				+ Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION)));
	
		JSONArray jsonNotes = response.getJSONArray("notes");
		for (int i = 0; i < jsonNotes.length(); i++){
			updates.add(new NoteUpdate(jsonNotes.getJSONObject(i)));
		}

		return updates;
	}

	public ArrayList<String> getNoteIds() throws UnknownHostException, JSONException {
		ArrayList<String> guids = new ArrayList<String>();

		JSONObject response = new JSONObject(authConnection.get(getNotesUri()));
	
		JSONArray jsonNotes = response.getJSONArray("notes");
		for (int i = 0; i < jsonNotes.length(); i++){
			guids.add(jsonNotes.getJSONObject(i).getString("guid"));
		}

		return guids;
	}

	public void upload(ArrayList<NoteUpdate> newAndUpdatedNotes) {
		// TODO Auto-generated method stub
	}

	public void delete(boolean removeAll) {
		// TODO Auto-generated method stub
	}

}
