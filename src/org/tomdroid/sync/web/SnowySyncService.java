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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncService;
import org.tomdroid.ui.SyncDialog;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.ErrorList;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.TLog;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SnowySyncService extends SyncService implements ServiceAuth {

	private static final String TAG = "SnowySyncService";
	private String lastGUID;
	private long latestRevision;

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
		OAuthConnection auth = getAuthConnection();
		return !auth.isAuthenticated();
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
					// TODO: might be intelligent to show something like a
					// progress dialog
					// else the user might try to sync before the authorization
					// process
					// is complete
					OAuthConnection auth = getAuthConnection();
					boolean result = auth.getAccess(uri
							.getQueryParameter("oauth_verifier"));

					if (result) {
						TLog.i(TAG, "The authorization process is complete.");
						handler.sendEmptyMessage(AUTH_COMPLETE);
						return;
						//sync(true);
					} else {
						TLog.e(TAG,
								"Something went wrong during the authorization process.");
						sendMessage(AUTH_FAILED);
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
	public boolean isSyncable() {
		return super.isSyncable() && isConfigured();
	}

	@Override
	protected void sync(final boolean push) {

		// start loading snowy notes
		setSyncProgress(0);
		this.lastGUID = null;

		TLog.v(TAG, "Loading Snowy notes");

		final String userRef = Preferences
				.getString(Preferences.Key.SYNC_SERVER_USER_API);

		syncInThread(new Runnable() {

			public void run() {

				OAuthConnection auth = getAuthConnection();
				latestRevision = Preferences
						.getLong(Preferences.Key.LATEST_SYNC_REVISION);

				try {
					TLog.v(TAG, "contacting " + userRef);
					String rawResponse = auth.get(userRef);
					if (rawResponse == null) {
						sendMessage(CONNECTING_FAILED);
						setSyncProgress(100);
						return;
					}

					setSyncProgress(30);

					try {
						JSONObject response = new JSONObject(rawResponse);
						String notesUrl = response.getJSONObject("notes-ref")
								.getString("api-ref");

						TLog.v(TAG, "contacting " + notesUrl);
						response = new JSONObject(auth.get(notesUrl));

						setSyncProgress(40);

						rawResponse = auth
								.get(notesUrl + "?include_notes=true");

						response = new JSONObject(rawResponse);
						JSONArray notes = response.getJSONArray("notes");
						setSyncProgress(50);

						TLog.v(TAG, "number of notes: {0}", notes.length());

						ArrayList<Note> notesList = new ArrayList<Note>();

						for (int i = 0; i < notes.length(); i++)
							notesList.add(new Note(notes.getJSONObject(i)));

						syncNotes(notesList, push);

						latestRevision = response
								.getLong("latest-sync-revision");

					} catch (JSONException e) {
						TLog.e(TAG, e, "Problem parsing the server response");
						sendMessage(PARSING_FAILED,
								ErrorList.createErrorWithContents(
										"JSON parsing", "json", e, rawResponse));
						setSyncProgress(100);
						return;
					}
				} catch (java.net.UnknownHostException e) {
					TLog.e(TAG, "Internet connection not available");
					sendMessage(NO_INTERNET);
					setSyncProgress(100);
					return;
				}
				Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION,
						latestRevision);
				if (isSyncable())
					finishSync(true);
			}
		});
	}

	public void finishSync(boolean refresh) {
		setSyncProgress(100);
		this.lastGUID = null;

		Time now = new Time();
		now.setToNow();
		String nowString = now.format3339(false);

		Preferences.putString(Preferences.Key.LATEST_SYNC_DATE, nowString);
		if (refresh)
			sendMessage(PARSING_COMPLETE);
	}

	public void setLastGUID(String guid) {
		this.lastGUID = guid;
	}

	protected String getLastGUID(String guid) {
		return this.lastGUID;
	}

	private OAuthConnection getAuthConnection() {

		// TODO: there needs to be a way to reset these values, otherwise cannot
		// change server!

		OAuthConnection auth = new OAuthConnection();

		auth.accessToken = Preferences.getString(Preferences.Key.ACCESS_TOKEN);
		auth.accessTokenSecret = Preferences
				.getString(Preferences.Key.ACCESS_TOKEN_SECRET);
		auth.requestToken = Preferences
				.getString(Preferences.Key.REQUEST_TOKEN);
		auth.requestTokenSecret = Preferences
				.getString(Preferences.Key.REQUEST_TOKEN_SECRET);
		auth.oauth10a = Preferences.getBoolean(Preferences.Key.OAUTH_10A);
		auth.authorizeUrl = Preferences
				.getString(Preferences.Key.AUTHORIZE_URL);
		auth.accessTokenUrl = Preferences
				.getString(Preferences.Key.ACCESS_TOKEN_URL);
		auth.requestTokenUrl = Preferences
				.getString(Preferences.Key.REQUEST_TOKEN_URL);
		auth.rootApi = Preferences
				.getString(Preferences.Key.SYNC_SERVER_ROOT_API);
		auth.userApi = Preferences
				.getString(Preferences.Key.SYNC_SERVER_USER_API);

		return auth;
	}

	// new methods to T Edit

	@Override
	protected void pushNote(final Note note) {
		final String userRef = Preferences
				.getString(Preferences.Key.SYNC_SERVER_USER_API);

		syncInThread(new Runnable() {
			public void run() {
				OAuthConnection auth = getAuthConnection();
				try {
					TLog.v(TAG, "putting note to server");
					String rawResponse = auth.get(userRef);
					try {
						TLog.v(TAG, "creating JSON");

						JSONObject data = new JSONObject();
						data.put(
								"latest-sync-revision",
								Preferences
										.getLong(Preferences.Key.LATEST_SYNC_REVISION) + 1);
						JSONArray Jnotes = new JSONArray();
						JSONObject Jnote = new JSONObject();
						Jnote.put("guid", note.getGuid());
						Jnote.put("title", note.getTitle());
						Jnote.put("note-content", note.getXmlContent());
						Jnote.put("note-content-version", "0.1");
						Jnote.put("last-change-date", note.getLastChangeDate()
								.format3339(false));
						Jnotes.put(Jnote);
						data.put("note-changes", Jnotes);

						JSONObject response = new JSONObject(rawResponse);

						String notesUrl = response.getJSONObject("notes-ref")
								.getString("api-ref");

						TLog.v(TAG, "put url: {0}", notesUrl);

						response = new JSONObject(auth.put(notesUrl,
								data.toString()));

						TLog.v(TAG, "put response: {0}", response.toString());

						Preferences.putLong(
								Preferences.Key.LATEST_SYNC_REVISION,
								response.getLong("latest-sync-revision"));
					} catch (JSONException e) {
						TLog.e(TAG, e, "Problem parsing the server response");
						sendMessage(NOTE_PUSH_ERROR,
								ErrorList.createErrorWithContents(
										"JSON parsing", "json", e, rawResponse));

						return;
					}
				} catch (java.net.UnknownHostException e) {
					TLog.e(TAG, "Internet connection not available");
					sendMessage(NO_INTERNET);
					return;
				}
				sendMessage(NOTE_PUSHED);

				if (note.lastSync) { // update sync date based on conflict
										// resolution
					TLog.d(TAG, "note is last to sync");
					Time now = new Time();
					now.setToNow();
					String nowString = now.format3339(false);
					Preferences.putString(Preferences.Key.LATEST_SYNC_DATE,
							nowString);
					finishSync(true);
				} else if (note.getGuid().equals(lastGUID))
					finishSync(true);
			}
		});
	}

	@Override
	protected void deleteNote(final String guid) {

		syncInThread(new Runnable() {
			public void run() {
				OAuthConnection auth = getAuthConnection();
				try {
					TLog.v(TAG, "deleting note on server");
					String userRef = Preferences
							.getString(Preferences.Key.SYNC_SERVER_USER_API);
					String rawResponse = auth.get(userRef);

					try {
						JSONObject data = new JSONObject();
						data.put(
								"latest-sync-revision",
								Preferences
										.getLong(Preferences.Key.LATEST_SYNC_REVISION) + 1);

						JSONArray notesJ = new JSONArray();

						JSONObject noteJ = new JSONObject();
						noteJ.put("guid", guid);
						noteJ.put("command", "delete");

						notesJ.put(noteJ);
						data.put("note-changes", notesJ);

						JSONObject response = new JSONObject(rawResponse);

						TLog.v(TAG, "request data: {0}", rawResponse);

						String notesUrl = response.getJSONObject("notes-ref")
								.getString("api-ref");

						response = new JSONObject(auth.put(notesUrl,
								data.toString()));

						TLog.v(TAG, "delete response: {0}", response.toString());
						Preferences.putLong(
								Preferences.Key.LATEST_SYNC_REVISION,
								response.getLong("latest-sync-revision"));
					} catch (JSONException e) {
						TLog.e(TAG, e, "Problem parsing the server response");
						sendMessage(NOTE_DELETE_ERROR,
								ErrorList.createErrorWithContents(
										"JSON parsing", "json", e, rawResponse));
						return;
					}
				} catch (java.net.UnknownHostException e) {
					TLog.e(TAG, "Internet connection not available");
					sendMessage(NO_INTERNET);
					return;
				}
				sendMessage(NOTE_DELETED);
				if (guid.equals(lastGUID)) {
					TLog.d(TAG, "note is last to sync");
					finishSync(true);
				}
			}
		});
	}

	@Override
	protected void pullNote(final String guid) {

		// start loading snowy notes

		TLog.v(TAG, "pulling remote note");

		final String userRef = Preferences
				.getString(Preferences.Key.SYNC_SERVER_USER_API);

		syncInThread(new Runnable() {

			public void run() {

				OAuthConnection auth = getAuthConnection();

				try {
					TLog.v(TAG, "contacting " + userRef);
					String rawResponse = auth.get(userRef);

					try {
						JSONObject response = new JSONObject(rawResponse);
						String notesUrl = response.getJSONObject("notes-ref")
								.getString("api-ref");

						TLog.v(TAG, "contacting " + notesUrl + guid);

						rawResponse = auth.get(notesUrl + guid
								+ "?include_notes=true");

						response = new JSONObject(rawResponse);
						JSONArray notes = response.getJSONArray("notes");
						JSONObject jsonNote = notes.getJSONObject(0);

						TLog.v(TAG, "parsing remote note");

						insertNote(new Note(jsonNote));

					} catch (JSONException e) {
						TLog.e(TAG, e, "Problem parsing the server response");
						sendMessage(NOTE_PULL_ERROR,
								ErrorList.createErrorWithContents(
										"JSON parsing", "json", e, rawResponse));
						return;
					}

				} catch (java.net.UnknownHostException e) {
					TLog.e(TAG, "Internet connection not available");
					sendMessage(NO_INTERNET);
					return;
				}

				sendMessage(NOTE_PULLED);
			}
		});
	}
}
