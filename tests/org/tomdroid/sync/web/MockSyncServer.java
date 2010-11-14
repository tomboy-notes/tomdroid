/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2010, Rodja Trappe <mail@rodja.net>
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
 * along with Tomdroid. If not, see <http://www.gnu.org/licenses/>.
 */
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

	HashMap<String, Note> storedNotes = new HashMap<String, Note>();
	private final ArrayList<Note>	noteUpdates			= new ArrayList<Note>();

	TestDataManipulator		testDataManipulator	= new TestDataManipulator();
	private boolean			isStoringLocked;

	public MockSyncServer() throws UnknownHostException, JSONException {
		super();
	}

	@Override
	protected JSONObject getMetadata() throws JSONException {
		JSONObject mockedResponse = new JSONObject(
				"{'user-name':'<in reality this is a http address>',"
						+ "'notes-ref':{'api-ref':'https://one.ubuntu.com/notes/api/1.0/op/',"
						+ "'href':'https://one.ubuntu.com/notes/'}," + "'current-sync-guid':'0',"
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
	public boolean createNewRevisionWith(ArrayList<Note> newAndUpdatedNotes) {
		if (isStoringLocked)
			return false;

		for (Note note : newAndUpdatedNotes) {
			storedNotes.put(note.getGuid(), note);
			noteUpdates.add(note.clone());
		}
		return true;
	}

	public void lockStoring() {
		isStoringLocked = true;
	}

	public void unlockStoring() {
		isStoringLocked = false;
	}

	class TestDataManipulator {

		private void onStoredDataChanged() {
			syncVersionOnServer++;
		}

		public Note createNewNote() {
			onStoredDataChanged();
			Note note = new Note();
			note.setTitle("A Title");
			note.setGuid(UUID.randomUUID().toString());
			note.changeXmlContent("Note content.");
			note.setLastSyncRevision(syncVersionOnServer);

			storedNotes.put(note.getGuid(), note);
			noteUpdates.add(note.clone());
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
			onStoredDataChanged();

			Note note = getNewestNote();
			note.setTitle(title);
			note.setLastSyncRevision(syncVersionOnServer);

			noteUpdates.add(note.clone());
			return note;
		}

		public Note setContentOfNewestNote(String content) {
			onStoredDataChanged();
			Note note = getNewestNote();
			note.setLastSyncRevision(syncVersionOnServer);
			note.changeXmlContent(content);
			noteUpdates.add(note.clone());
			return note;
		}

		public void deleteNote(String guid) {
			storedNotes.remove(guid);
			onStoredDataChanged();
		}

		public Note getNote(String guid) {
			if (!storedNotes.containsKey(guid))
				throw new NoSuchElementException();

			return storedNotes.get(guid);
		}
	}
}
