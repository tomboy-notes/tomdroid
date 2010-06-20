package org.tomdroid.sync.web;

import java.net.UnknownHostException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.Note;
import org.tomdroid.sync.LocalStorage;
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

	private static final String	TAG	= "SnowySyncMethod";

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

	public Uri getAuthUri(String serverUri) throws UnknownHostException {

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
	public boolean isSyncable() {
		return super.isSyncable() && isConfigured();
	}

	@Override
	protected void sync() {

		// start loading snowy notes
		setSyncProgress(0);
		if (Tomdroid.LOGGING_ENABLED)
			Log.v(TAG, "Loading Snowy notes");

		execInThread(new Runnable() {

			public void run() {

				try {
					SyncServer server = new SyncServer();
					setSyncProgress(30);

					syncWith(server);

				} catch (JSONException e1) {
					if (Tomdroid.LOGGING_ENABLED)
						Log.e(TAG, "Problem parsing the server response", e1);
					sendMessage(PARSING_FAILED);
					setSyncProgress(100);
					return;
				} catch (java.net.UnknownHostException e) {
					if (Tomdroid.LOGGING_ENABLED)
						Log.e(TAG, "Internet connection not available");
					sendMessage(NO_INTERNET);
					setSyncProgress(100);
					return;
				}
			}
		});
	}

	void syncWith(SyncServer server) throws UnknownHostException, JSONException {
	
		if (server.isInSync()) {
			setSyncProgress(100);
			return;
		}

		ensureServerIdIsAsExpected();

		ArrayList<NoteUpdate> updatesFromServer = server.getNoteUpdates();
		setSyncProgress(50);

		fixTitleConflicts(updatesFromServer);

		mergeWithLocalNotes(updatesFromServer);
		setSyncProgress(70);

		ArrayList<String> noteIdsOnServer = server.getNoteIds();
		getLocalStorage().deleteNotes(noteIdsOnServer);

		server.upload(getNewAndUpdatedNotes());
		setSyncProgress(90);

		server.delete(getLocalStorage().getLocalNoteIds().removeAll(noteIdsOnServer));

		server.onSyncDone();
		setSyncProgress(100);
	}

	private ArrayList<NoteUpdate> getNewAndUpdatedNotes() {
		return new ArrayList<NoteUpdate>();
	}

	private void ensureServerIdIsAsExpected() {
		// TODO check if the server's guid is as expected to prevent deleting local
		// notes, etc when the server has been wiped or reinitialized by another client

		/*
		 * // If the server has been wiped or reinitialized by another client // for some
		 * reason, our local manifest is inaccurate and could misguide // sync into
		 * erroneously deleting local notes, etc. We reset the client // to prevent this
		 * situation. string serverId = server.Id; if (client.AssociatedServerId !=
		 * serverId) { client.Reset (); client.AssociatedServerId = serverId; }
		 */
	}

	private void mergeWithLocalNotes(ArrayList<NoteUpdate> serverUpdates) {
		// TODO Auto-generated method stub
	}

	private void fixTitleConflicts(ArrayList<NoteUpdate> noteUpdates) {

		// TODO implement in a similar way as Tomboy (see code below)

		/*
		 * // First, check for new local notes that might have title conflicts // with the
		 * updates coming from the server. Prompt the user if necessary. // TODO: Lots of
		 * searching here and in the next foreach... // Want this stuff to happen all at
		 * once first, but // maybe there's a way to store this info and pass it on? foreach
		 * (NoteUpdate noteUpdate in noteUpdates.Values) { if (FindNoteByUUID
		 * (noteUpdate.UUID) == null) { Note existingNote = NoteMgr.Find (noteUpdate.Title);
		 * if (existingNote != null && !noteUpdate.BasicallyEqualTo (existingNote)) { //
		 * Logger.Debug ("Sync: Early conflict detection for '{0}'", noteUpdate.Title); if
		 * (syncUI != null) { syncUI.NoteConflictDetected (NoteMgr, existingNote,
		 * noteUpdate, noteUpdateTitles); // Suspend this thread while the GUI is presented
		 * to // the user. syncThread.Suspend (); } } } }
		 */
	}
}
