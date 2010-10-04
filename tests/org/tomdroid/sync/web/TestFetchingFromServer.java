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

import java.util.UUID;

import org.tomdroid.Note;

public class TestFetchingFromServer extends MockedSyncServerTestCase {

	@SuppressWarnings("unused")
	private static final String	TAG	= "TestFetchingFromServer";

	public void testInitialization() throws Exception {
		assertEquals("Max", getServer().firstName);
		assertEquals("Mustermann", getServer().lastName);

		assertTrue("should be in sync", getServer().isInSync(getLocalStorage()));

		getSyncMethod().syncWith(getServer());
		assertTrue("should be in sync", getServer().isInSync(getLocalStorage()));
	}

	public void testLoadingNewNoteFromServer() throws Exception {
		Note remoteNote = getServer().testDataManipulator.createNewNote();
		assertFalse("should be out of sync", getServer().isInSync(getLocalStorage()));

		getSyncMethod().syncWith(getServer());
		assertTrue("should be in sync again", getServer().isInSync(getLocalStorage()));

		assertEquals("note ids should be the same", getServer().getNoteIds(), getLocalStorage()
				.getNoteGuids());
		Note localNote = getLocalStorage().getNote(remoteNote.getGuid());
		assertEquals(remoteNote, localNote);
	}

	public void testChangingNoteTitleOnServer() throws Exception {
		Note remoteNote = getServer().testDataManipulator.createNewNote();
		getSyncMethod().syncWith(getServer());

		remoteNote = getServer().testDataManipulator.setTitleOfNewestNote("Another Title");
		assertEquals("server should still have one note", 1, getServer().storedNotes.size());
		assertFalse("should be out of sync", getServer().isInSync(getLocalStorage()));

		getSyncMethod().syncWith(getServer());
		assertTrue("should be in sync again", getServer().isInSync(getLocalStorage()));

		assertEquals(1, getLocalStorage().getNoteGuids().size());
		assertEquals("note ids should be the same", getServer().getNoteIds(), getLocalStorage()
				.getNoteGuids());

		Note localNote = getLocalStorage().getNote(remoteNote.getGuid());
		assertEquals(remoteNote, localNote);
		assertEquals("local title should have changed", "Another Title", localNote.getTitle());
	}

	public void testChangingNoteContentOnServer() throws Exception {
		Note remoteNote = getServer().testDataManipulator.createNewNote();
		getSyncMethod().syncWith(getServer());

		remoteNote = getServer().testDataManipulator
				.setContentOfNewestNote("some other note content");
		assertEquals("server should still have one note", 1, getServer().storedNotes.size());
		assertFalse("should be out of sync", getServer().isInSync(getLocalStorage()));

		getSyncMethod().syncWith(getServer());
		assertTrue("should be in sync again", getServer().isInSync(getLocalStorage()));

		assertEquals("note count", 1, getLocalStorage().getNoteGuids().size());
		assertEquals("note ids should be the same", getServer().getNoteIds(), getLocalStorage()
				.getNoteGuids());

		Note localNote = getLocalStorage().getNote(remoteNote.getGuid());
		assertEquals(remoteNote, localNote);
		assertEquals("local content should have changed", "some other note content", localNote
				.getXmlContent());
	}

	public void testDeletingNoteOnServer() throws Exception {
		getServer().testDataManipulator.createNewNote();
		UUID deletedNoteGuid = getServer().testDataManipulator.createNewNote().getGuid();
		getServer().testDataManipulator.createNewNote();
		getSyncMethod().syncWith(getServer());
		assertEquals(3, getLocalStorage().getNoteGuids().size());

		getServer().testDataManipulator.deleteNote(deletedNoteGuid);
		assertEquals("server should have two notes", 2, getServer().storedNotes.size());
		assertFalse("should be out of sync", getServer().isInSync(getLocalStorage()));

		getSyncMethod().syncWith(getServer());
		assertTrue("should be in sync again", getServer().isInSync(getLocalStorage()));

		assertEquals(2, getLocalStorage().getNoteGuids().size());
		assertEquals("note ids should be the same", getServer().getNoteIds(), getLocalStorage()
				.getNoteGuids());

		assertNull("guid should be in use", getLocalStorage().getNote(deletedNoteGuid));
	}
}
