package org.tomdroid.sync.web;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.Note;

import android.text.format.Time;

public class MockSyncServer extends SyncServer {

	ArrayList<Note>			storedNotes	= new ArrayList<Note>();
	private ArrayList<Note>	noteUpdates	= new ArrayList<Note>();

	public MockSyncServer() throws UnknownHostException, JSONException {
		super();
	}

	@Override
	protected JSONObject getMetadata() throws JSONException {
		JSONObject mockedResponse = new JSONObject(
				"{'user-name':'<in reality here comes a http address>',"
						+ "'notes-ref':{'api-ref':'https://one.ubuntu.com/notes/api/1.0/op/',"
						+ "'href':'https://one.ubuntu.com/notes/'}," + "'current-sync-guid':'-1',"
						+ "'last-name':'Mustermann','first-name':'Max','latest-sync-revision':0}");
		return mockedResponse;
	}

	@Override
	protected JSONObject getNoteUpdatesSince(long since) throws JSONException, UnknownHostException {
		JSONArray notes = new JSONArray();
		for (int i = (int) since; i < noteUpdates.size(); i++) {
			notes.put(noteUpdates.get(i).toJson());
		}

		JSONObject updates = new JSONObject();
		updates.put("notes", notes);
		return updates;
	}

	@Override
	protected JSONObject getAllNotesWithoutContent() throws JSONException, UnknownHostException {
		JSONArray notes = new JSONArray();
		for (Note note : storedNotes) {
			notes.put(note.toJsonWithoutContent());
		}

		JSONObject data = new JSONObject();
		data.put("notes", notes);
		return data;
	}

	public Note createNewNote() {
		Note note = new Note();
		note.setTitle("A Title");
		note.setGuid(UUID.randomUUID().toString());
		Time time = new Time();
		time.setToNow();
		note.setLastChangeDate(time);
		note.setXmlContent("plain note content.");

		storedNotes.add(note);
		noteUpdates.add(note);
		onStoredDataChanged();
		return note;
	}

	private void onStoredDataChanged() {
		syncVersionOnServer++;
	}
}
