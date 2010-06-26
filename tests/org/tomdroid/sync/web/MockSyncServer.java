package org.tomdroid.sync.web;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.Note;

import android.text.format.Time;

public class MockSyncServer extends SyncServer {

	ArrayList<Note>			storedNotes	= new ArrayList<Note>();
	private ArrayList<Note>	noteUpdates	= new ArrayList<Note>();

	TestDataManipulator testDataManipulator = new TestDataManipulator();
	
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

	class TestDataManipulator {

		private void onStoredDataChanged() {
			syncVersionOnServer++;
		}

		public Note createNewNote() {
			Note note = new Note();
			note.setTitle("A Title");
			note.setGuid(UUID.randomUUID());
			note.setLastSyncRevision((int) (Math.random() * 10));
			note.changeXmlContent("plain note content.");

			storedNotes.add(note);
			noteUpdates.add(note.clone());
			onStoredDataChanged();
			return note;
		}

		public Note setTitleOfNewestNote(String title) {
			Note note = storedNotes.get(storedNotes.size() - 1);
			note.setTitle(title);
			noteUpdates.add(note.clone());
			onStoredDataChanged();
			return note;
		}

		public Note setContentOfNewestNote(String content) {
			Note note = storedNotes.get(storedNotes.size() - 1);
			note.changeXmlContent(content);
			noteUpdates.add(note.clone());
			onStoredDataChanged();
			return note;
		}

		public void deleteNote(UUID guid) {
			for (Note note : storedNotes) {
				if (note.getGuid().equals(guid)) {
					storedNotes.remove(note);
					onStoredDataChanged();
					return;
				}
			}
		}
		
		public Note getNote(UUID guid) {
			for (Note note : storedNotes) {
				if (note.getGuid().equals(guid)) {
					return note;
				}
			}
			throw new NoSuchElementException();
		}
	}
}
