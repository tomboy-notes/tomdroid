package org.tomdroid.sync.web;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.Note;
import org.tomdroid.sync.LocalStorage;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;

import android.util.Log;

public class SyncServer {

	final String			TAG				= "SyncServer";
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

		readMetaData(getMetadata());
	}

	protected JSONObject getMetadata() throws UnknownHostException, JSONException {
		String rawResponse = authConnection.get(userReference);
		JSONObject response = new JSONObject(rawResponse);
		return response;
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

	public boolean isInSync(LocalStorage localStorage) {
		return localStorage.getLatestSyncVersion() == syncVersionOnServer
				&& localStorage.getNewAndUpdatedNotes().isEmpty();
	}
	
	public Long getSyncRevision() {
		return syncVersionOnServer;
	}

	public JSONArray getNotes() throws UnknownHostException, JSONException {
		JSONObject response = new JSONObject(authConnection.get(getNotesUri()
				+ "?include_notes=true"));
		return response.getJSONArray("notes");
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

	public ArrayList<Note> getNoteUpdates() throws UnknownHostException, JSONException {
		ArrayList<Note> updates = new ArrayList<Note>();

		long since = Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION);
		JSONObject response = getNoteUpdatesSince(since);
	
		JSONArray jsonNotes = response.getJSONArray("notes");
		for (int i = 0; i < jsonNotes.length(); i++){
			Note note = new Note(jsonNotes.getJSONObject(i));

			if (note.getLastSyncRevision() < since) {
				if (Tomdroid.LOGGING_ENABLED)
					Log.e(TAG, "note '" + note.getTitle() + "' was not modified since "
							+ note.getLastSyncRevision()
							+ " but non the less included in response from server");
				continue;
			}
			updates.add(note);
		}

		return updates;
	}

	protected JSONObject getNoteUpdatesSince(long since) throws JSONException, UnknownHostException {
		JSONObject response = new JSONObject(authConnection.get(getNotesUri()
				+ "?include_notes=true&since="
				+ since));
		return response;
	}

	public Set<String> getNoteIds() throws UnknownHostException, JSONException {
		Set<String> guids = new HashSet<String>();

		JSONObject response = getAllNotesWithoutContent();
	
		JSONArray jsonNotes = response.getJSONArray("notes");
		for (int i = 0; i < jsonNotes.length(); i++){
			guids.add(jsonNotes.getJSONObject(i).getString("guid"));
		}

		return guids;
	}

	protected JSONObject getAllNotesWithoutContent() throws JSONException, UnknownHostException {
		JSONObject response = new JSONObject(authConnection.get(getNotesUri()));
		return response;
	}

	/**
	 * @return true if successful
	 */
	public boolean createNewRevisionWith(ArrayList<Note> newAndUpdatedNotes) throws JSONException {
		if (newAndUpdatedNotes.isEmpty()){
			return true;
		}
		
		JSONArray jsonNotes = new JSONArray();
		for (Note note : newAndUpdatedNotes) {
			jsonNotes.put(note.toJson());
		}

		JSONObject updates = new JSONObject();
		updates.put("latest-sync-revision", getSyncRevision() + 1);
		updates.put("note-changes", jsonNotes);
		
		long newRevision = upload(updates);

		if (newRevision == getSyncRevision() + 1) {
			syncVersionOnServer = newRevision;
			return true;
		}
		return false;
	}

	/**
	 * @return new revision if successful, -1 if not
	 */
	protected int upload(JSONObject data){
		int revision = -1;
		try {
			JSONObject response = new JSONObject(authConnection.put(getNotesUri(), data.toString()));
			revision = response.getInt("latest-sync-revision");
		} catch (UnknownHostException e) {
			if (Tomdroid.LOGGING_ENABLED) Log.e(TAG, e.toString());
		} catch (JSONException e) {
			if (Tomdroid.LOGGING_ENABLED) Log.e(TAG, e.toString());
		}

		return revision;
	}

	
	public void delete(Set<String> disposedNoteIds) {
		// TODO Auto-generated method stub
	}

}
