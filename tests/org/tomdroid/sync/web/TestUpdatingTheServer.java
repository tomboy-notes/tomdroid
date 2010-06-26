package org.tomdroid.sync.web;

import java.util.UUID;

import org.tomdroid.Note;

public class TestUpdatingTheServer extends MockedSyncServerTestCase {

	public void testChangingNoteOnClient() throws Exception {
		UUID guid = getServer().testDataManipulator.createNewNote().getGuid();
		getSyncMethod().syncWith(getServer());

		Note localNote = modifyLocalNote(guid);
		getSyncMethod().syncWith(getServer());
		Note remoteNote = getServer().testDataManipulator.getNote(guid);
		assertEquals("remote note should have been updated", localNote.getXmlContent(), remoteNote
				.getXmlContent());
		assertEquals("remote note should have correct timestamp", localNote.getLastChangeDate(),
				remoteNote.getLastChangeDate());
	}

	private Note modifyLocalNote(UUID guid) {
		Note note = getLocalStorage().getNote(guid);
		long creationTime = note.getLastChangeDate().toMillis(false);
		String newContent = note.getXmlContent() + "\nNew text for our test note!";
		note.changeXmlContent(newContent);
		long modificationTime = note.getLastChangeDate().toMillis(false);
		assertTrue("timestamp should have changed", creationTime < modificationTime);
		getLocalStorage().insertNote(note);
		note = getLocalStorage().getNote(guid);
		modificationTime = note.getLastChangeDate().toMillis(false);
		assertTrue("timestamp should have changed", creationTime < modificationTime);

		assertEquals("local note should have been updated", newContent, note.getXmlContent());
		return note;
	}
}
