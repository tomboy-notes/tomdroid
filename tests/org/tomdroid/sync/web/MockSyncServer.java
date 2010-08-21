package org.tomdroid.sync.web;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.Note;

public class MockSyncServer extends SyncServer {

	HashMap<UUID, Note>		storedNotes			= new HashMap<UUID, Note>();
	private ArrayList<Note>	noteUpdates			= new ArrayList<Note>();

	TestDataManipulator		testDataManipulator	= new TestDataManipulator();
	private boolean	isStoringLocked;

	public MockSyncServer() throws UnknownHostException, JSONException {
		super();
	}

	@Override
	protected JSONObject getMetadata() throws JSONException {
		JSONObject mockedResponse = new JSONObject(
				"{'user-name':'<in reality this is a http address>',"
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
		for (Note note : storedNotes.values()) {
			notes.put(note.toJsonWithoutContent());
		}

		JSONObject data = new JSONObject();
		data.put("notes", notes);
		return data;
	}

	@Override
	public boolean upload(ArrayList<Note> newAndUpdatedNotes) {
		if (isStoringLocked) return false;

		for (Note note : newAndUpdatedNotes) {
			storedNotes.put(note.getGuid(), note);
			noteUpdates.add(note.clone());
		}
		return true;
	}

	public void lockStoring(){
		isStoringLocked = true;
	}

	public void unlockStoring(){
		isStoringLocked = false;
	}

	class TestDataManipulator {

		private void onStoredDataChanged() {
			syncVersionOnServer++;
		}

		public Note createNewNote() {
			Note note = new Note();
			note.setTitle("A Title");
			note.setGuid(UUID.randomUUID());
			note.changeXmlContent("Note content.");

			storedNotes.put(note.getGuid(), note);
			noteUpdates.add(note.clone());
			onStoredDataChanged();
			return note;
		}

		public Note getNewestNote() {
			Note newestNote = null;
			for (Note note : storedNotes.values()) {
				if (newestNote != null
						&& note.getLastChangeDate().toMillis(false) > newestNote
								.getLastChangeDate().toMillis(false)) {

				} else {
					newestNote = note;
				}
			}
			return newestNote;
		}

		public Note setTitleOfNewestNote(String title) {
			Note note = getNewestNote();
			note.setTitle(title);
			noteUpdates.add(note.clone());
			onStoredDataChanged();
			return note;
		}

		public Note setContentOfNewestNote(String content) {
			Note note = getNewestNote();
			note.changeXmlContent(content);
			noteUpdates.add(note.clone());
			onStoredDataChanged();
			return note;
		}

		public void deleteNote(UUID guid) {
			storedNotes.remove(guid);
			onStoredDataChanged();
		}

		public Note getNote(UUID guid) {
			if (!storedNotes.containsKey(guid))
				throw new NoSuchElementException();

			return storedNotes.get(guid);
		}
	}
}
