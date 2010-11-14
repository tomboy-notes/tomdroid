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

import org.tomdroid.Note;

public class TestUpdatingTheServer extends MockedSyncServerTestCase {

	public void testChangingNoteOnClient() throws Exception {
		String guid = getServer().testDataManipulator.createNewNote().getGuid();
		getSyncMethod().syncWith(getServer());

		assertEquals("there should be no new/updated notes", 0, getLocalStorage()
				.getNewAndUpdatedNotes().size());
		Note localNote = modifyLocalNote(guid);
		assertEquals("should have a changed note", 1, getLocalStorage().getNewAndUpdatedNotes()
				.size());
		assertEquals("should have the changed note", guid, getLocalStorage()
				.getNewAndUpdatedNotes().get(0).getGuid());
		assertEquals("note should contain the update", localNote.getXmlContent(), getLocalStorage()
				.getNewAndUpdatedNotes().get(0).getXmlContent());
		assertFalse("should be out of sync", getServer().isInSync(getLocalStorage()));

		getSyncMethod().syncWith(getServer());
		assertEquals("should have no changed notes anymore", 0, getLocalStorage()
				.getNewAndUpdatedNotes().size());
		assertTrue("should be in sync", getServer().isInSync(getLocalStorage()));

		Note remoteNote = getServer().testDataManipulator.getNote(guid);
		assertEquals("remote note should have correct timestamp", localNote.getLastChangeDate()
				.format3339(false), remoteNote.getLastChangeDate().format3339(false));
		assertEquals("remote note should have been updated", localNote.getXmlContent(), remoteNote
				.getXmlContent());
		assertEquals("locally stored note should bee the same as remote", getLocalStorage()
				.getNote(guid).getXmlContent(), remoteNote.getXmlContent());

	}

	public void testChangingDifferentNotesOnClientAndServer() throws Exception {
		String guid = getServer().testDataManipulator.createNewNote().getGuid();
		getSyncMethod().syncWith(getServer());

		modifyLocalNote(guid);
		getServer().testDataManipulator.createNewNote();
		getSyncMethod().syncWith(getServer());

		assertEquals(2, getLocalStorage().getNoteGuids().size());
		assertEquals(2, getServer().getNoteIds().size());
		assertTrue("should be in sync", getServer().isInSync(getLocalStorage()));
	}

	public void testServerNotStoringLocalModificationWhileSyncing() throws Exception {
		String guid = getServer().testDataManipulator.createNewNote().getGuid();
		getSyncMethod().syncWith(getServer());

		modifyLocalNote(guid);
		getServer().lockStoring();
		getSyncMethod().syncWith(getServer());
		getServer().unlockStoring();
		
		assertEquals("should still have the changed note", 1, getLocalStorage()
				.getNewAndUpdatedNotes().size());
		assertFalse("should be out of sync", getServer().isInSync(getLocalStorage()));
	}

	public void testMergingLocalModificationWithModificationOnServer() throws Exception {
		String guid = getServer().testDataManipulator.createNewNote().getGuid();
		getSyncMethod().syncWith(getServer());

		modifyLocalNote(guid);
		getServer().testDataManipulator.setContentOfNewestNote("server modification");
		getSyncMethod().syncWith(getServer());

		assertEquals(1, getLocalStorage().getNoteGuids().size());
		assertEquals(1, getServer().getNoteIds().size());

		assertTrue("should be in sync", getServer().isInSync(getLocalStorage()));
		assertEquals("content should be merged", "server modification --merged-- Note content. Appended text for our test note!", getLocalStorage().getNote(guid)
				.getXmlContent());
		assertEquals("content should be equal on client and server", getLocalStorage()
				.getNote(guid).getXmlContent(), getServer().testDataManipulator.getNewestNote()
				.getXmlContent());
	}

	
	private Note modifyLocalNote(String guid) throws Exception {
		Note note = getLocalStorage().getNote(guid);
		long creationTime = note.getLastChangeDate().toMillis(false);
		Thread.sleep(1100);
		String newContent = note.getXmlContent() + " Appended text for our test note!";
		note.changeXmlContent(newContent);

		long modificationTime = note.getLastChangeDate().toMillis(false);
		assertTrue("timestamp should have changed", creationTime < modificationTime);

		getLocalStorage().insertNote(note);
		note = getLocalStorage().getNote(guid);
		assertEquals("timestamp should have been updated", modificationTime, note
				.getLastChangeDate().toMillis(false));
		assertEquals("local note should have been updated", newContent, note.getXmlContent());
		assertFalse("locally stored note should be marked as 'out of sync with server'", note.isSynced());
		return note;
	}
}
