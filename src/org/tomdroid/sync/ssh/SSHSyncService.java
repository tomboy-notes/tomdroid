package org.tomdroid.sync.ssh;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.sync.SyncService;
import org.tomdroid.sync.sd.NoteHandler;
import org.tomdroid.sync.sd.SdCardSyncService;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.ErrorList;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.TLog;
import org.tomdroid.util.Time;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.tomdroid.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.TimeFormatException;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SFTPv3Client;
import ch.ethz.ssh2.SFTPv3FileAttributes;

/**
 * Sync service for syncing tombody notes from a remote ssh folder.
 *
 * @author alexander rausch <mail@arausch.de>
 *
 * modified by Sebastian Friedemann <s.friedemann@gmx.net>
 *
 */
public class SSHSyncService extends SyncService {

	// list of notes to sync
	private ArrayList<Note> syncableNotes = new ArrayList<Note>();

	// logging related
	private static final String TAG = "SSHSyncService";

	public static final String NAME = "sshsync";

	private static final String SERVER_LOCKED_ERROR_STRING = "Server locked. Another client is still synchronising. Try again later!";

	// don't access directly! use path()
	private File path_;

	private String hostname;

	private String username;

	private String password;

	private String privkeyFile;

	private int port;
	
	/// on server side
	private String folder;
	
	private long newRevision;
	
	private long latestRemoteRevision;
	
	/// on server side
	private String newRevisionPath;

	private Document manifest;

	private int lockRenewCount = -1;
	
	private String transactionId;
	
	private Timer lockTimer = null;
	
	/// true if no manifest.xml on server.
	private boolean firstSync;
	
	public SSHSyncService(Activity activity, Handler handler) {
		super(activity, handler);
		if (Preferences.getString(Preferences.Key.CLIENT_ID) == "")
			Preferences.putString(Preferences.Key.CLIENT_ID, UUID.randomUUID().toString());
	}	

	@Override
	public boolean needsServer() {		
		return true;
	}

	@Override
	public boolean needsAuth() {
		return false;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "SSH Sync";
	}
	
	/// returns true if it may sync ( sync lock file was successfully created) and false if not.
	private void checkSyncLock(SCPClient scpClient) throws Exception {
		// try to delete last locla lock file
		new File(localPath().getPath() + "/lock").delete();
		if (lockRenewCount != -1) 
			throw new Exception("still locked. Could not lock");
			
		try {
			scpClient.get(folder + "/lock", localPath().getPath());
			// TODO: check for timeout! if old, overwrite!
			//   inspect sync lock file. if exists wait at least time it says.
			// TODO: also recreate lock file if lock timer timed out.
			throw new Exception(SERVER_LOCKED_ERROR_STRING);
		} catch (IOException e) {
			// assume not locked. create lock
			updateLockFile(scpClient);
		}		
	}

	// XXX dirty
	@SuppressLint("NewApi") private void updateLockFile(final SCPClient scpClient) throws ParserConfigurationException, TransformerException, IOException {
		// TODO: check if lock file looks good, test! (correct transactionId?... but I also don't want to use so much bandwidth...)
		lockRenewCount ++;
		
		Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
		
		// updates lcok file with current try and time..
		// hope the following steps don't take too long! maybe recheck if there is still och lock file is necessary.
		Node lock = doc.appendChild(doc.createElement("lock"));

		lock.appendChild(doc.createElement("transcript-id")).setTextContent(transactionId);

		lock.appendChild(doc.createElement("client-id")).setTextContent(Preferences.getString(Preferences.Key.CLIENT_ID));

		lock.appendChild(doc.createElement("renew-count")).setTextContent("" + lockRenewCount);

		// standard: 2min
		lock.appendChild(doc.createElement("lock-expiration-duration")).setTextContent("00:02:00");

		lock.appendChild(doc.createElement("revision")).setTextContent("" + newRevision);

		String localLockPath = localPath().getPath() + "/lock";
		writeXML(doc, localLockPath);
		// AdjustPermissions ?? seems not necessary.   
		scpClient.put(localLockPath, folder);
		
		lockTimer = new Timer();
		lockTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					updateLockFile(scpClient);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 100000);  // 100s
	}
	
	@Override
	public void removeLock() {
		removeLock(null);
	}
	
	
	private void removeLock(Connection connection) {
		if (lockRenewCount == -1)
			return;
		SFTPv3Client sftpClient = null;
		try {
			if (lockTimer == null)  // nothing is locked. Somebody else locked maybe?
				throw new Exception("no lock Timer running.");  
			lockTimer.cancel();
			if (connection == null) {
				connection = spawnSSHConnection();
			}
			sftpClient = new SFTPv3Client(connection);
			sftpClient.rm(folder + "/lock");
			lockRenewCount = -1;
			
		} catch (Exception e) {
			Log.w(TAG, "could not remove lock file.");
		} finally {
			if (sftpClient != null)
				sftpClient.close();
			if (connection != null) 
				connection.close();
		}
		// remove local lock file too? Why should I?
	}

	private File localPath() {
		if (path_ == null) {
			path_ = new File(Tomdroid.NOTES_PATH + (Tomdroid.NOTES_PATH.endsWith("/") ? "" : "/") + "ssh");
		}
		return path_;
	}
	
	private Connection spawnSSHConnection() throws Exception {
		String connectionInfos = Preferences.getString(Preferences.Key.SYNC_SERVER);
		// format: (atm hostnameformat: username@server:port/pfad/ , nichts ist optional!)
		/*username = Preferences TODO: activate!
				.getString(Preferences.Key.SYNC_SERVER_SSH_USERNAME);
		password = CryptHelper.decodePW(Preferences
				.getString(Preferences.Key.SYNC_SERVER_SSH_PASSWORD));
		port = Integer.parseInt(Preferences
				.getString(Preferences.Key.SYNC_SERVER_SSH_PORT));*/
		//folder = Preferences.getString(Preferences.Key.SYNC_SERVER_SSH_FODLER);
		// TODO: activate ^^(atm hostnameformat: username@server:port/pfad/ , nichts ist optional!)
		//privkeyFile = Preferences
		//.getString(Preferences.Key.SYNC_SERVER_SSH_PRIVATE_KEY);

		privkeyFile = Preferences.getString(Preferences.Key.ID_RSA_FILE);
		File keyfile = new File(privkeyFile);
		if (!keyfile.isFile()) {
			// intelligent guessing...
			privkeyFile = Environment.getExternalStorageDirectory() + "/id_rsa";
			keyfile = new File(privkeyFile);
			if (!keyfile.isFile()) {
				privkeyFile = localPath().getParentFile().getParent() + "/id_rsa";
				keyfile = new File(privkeyFile);
				
			}   
		}

		if (!(new File(privkeyFile).isFile())) {
			sendMessage(SSH_KEYFILE_NOT_FOUND);
			throw new Exception("SSH key file not found!");
		}

		
		Matcher m = getConnectionStringRegex().matcher(connectionInfos);
		if (!m.find()) {
			sendMessage(PARSING_FAILED);
			throw new Exception("SSH connection string wrong.");
		}

		username = m.group(1);
		hostname = m.group(2);
		
		port = Integer.parseInt(m.group(3));
		folder = m.group(4);

		
		Connection connection = new Connection(hostname, port);
		try {
			connection.connect();
		} catch (IOException e) {
			sendMessage(SSH_CONNECT_FAIL);
			throw e;
		}
		
		boolean isAuthenticated = false;

		if (privkeyFile == null
				|| (privkeyFile != null && privkeyFile.length() == 0)
				&& username != null && username.length() > 0
				&& password != null && password.length() > 0) {
			isAuthenticated = connection.authenticateWithPassword(username,
					password);
		} else if (privkeyFile != null && privkeyFile.length() > 0) {
			if (password == null) {
				password = "";
			}
			Log.i(TAG, String.format("scp with user: %s, hostname: %s, port: %d folder: %s, privKeyfile %s ",
					username, hostname, port, folder, keyfile.getPath()));
			// private key password is ignored if it isn't needed by the
			// cert
			isAuthenticated = connection.authenticateWithPublicKey(
					username, keyfile, password);
		}

		if (isAuthenticated) {
			return connection;
		} else {			
			sendMessage(SSH_LOGIN_FAIL);
			throw new Exception("Authentification failed!");
		}
			
	
	}

	@Override
	protected void getNotesForSync(boolean push) {
		// Entrypoint for sync.
		transactionId = UUID.randomUUID().toString();
		firstSync = false;

		// gets all notes for sync
		// start
		setSyncProgress(0);
		
		if (!localPath().exists()) {
			localPath().mkdirs();
		}

		this.push = push;

		// must run in a thread to not waste mainactivity with networkidleing!
		execInThread(new Runnable() {
			@Override
			public void run() {
				Log.v(TAG, "Loading notes from ssh");

				// don't clean up ssh note dir (As we reuse this as note cache ;) )
				/*File[] files = localPath().listFiles();
				for (File f : files) {
					if (f.isFile() && f.getName().contains(".note")) {
						f.delete();
					}
				}*/

				Connection connection = null;
				
				try {
					connection = spawnSSHConnection();
					
					
					if(cancelled) {
						connection.close();
						doCancel();
						return; 
					}	
					/* Create a SCP client */
					SCPClient scpClient = new SCPClient(connection);
					
					DocumentBuilderFactory factory = DocumentBuilderFactory
							.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					
					
					/// TODO safe when canceling?
								
					sendMessage(SYNC_CONNECTED);
					
					try {
						// get current manifest
						scpClient.get(folder + "/manifest.xml", localPath().getPath());
						manifest = builder.parse(new File(localPath().getPath()
								+ "/manifest.xml"));
					} catch (IOException e) {
						//I'm the first to sync this server. I should create the manifest
						Log.i(TAG, "I seem to be the first to push to this server. Creating new manifest.");
						manifest = builder.newDocument();
						Node sync = manifest.appendChild(manifest.createElement("sync"));
						Node revision = manifest.createAttribute("revision");
						revision.setNodeValue("0");
						Node serverId = manifest.createAttribute("server-id");
						serverId.setNodeValue(UUID.randomUUID().toString());
						sync.getAttributes().setNamedItem(revision);						
						sync.getAttributes().setNamedItem(serverId);
						firstSync = true;
					}
					
					setSyncProgress(10);					
					if(cancelled) {
						doCancel();
						return; 
					}	

					// generate revision variables 
					long latestLocalRevision = (Long)Preferences.getLong(Preferences.Key.LATEST_SYNC_REVISION);
					latestRemoteRevision = getRevFromManifest(manifest);
					newRevision = latestRemoteRevision + 1;
					// generate new revision path for later when pushing notes.
					newRevisionPath = folder + "/" + (newRevision / 100) + "/" + newRevision;

					// check synclock and try to create sync lock file.
					checkSyncLock(scpClient);

					NodeList notes = manifest.getElementsByTagName("note");

					
					if (notes.getLength() == 0) {
						connection.close();
						Log.i(TAG, "no notes on server");
						
						prepareSyncableNotes(syncableNotes);
						
						return;
					}
						
					setSyncProgress(15);					
					if(cancelled) {
						removeLock(connection);
						doCancel();
						return; 
					}	
					
					ArrayList<String> filesToDownload = new ArrayList<String>();
					for (int i = 0; i < notes.getLength(); i++) {
						Node noteInformation = notes.item(i);
						int noteRev = Integer.parseInt(noteInformation.getAttributes().getNamedItem("rev").getNodeValue());
						if (noteRev > latestLocalRevision) { // I need u alive!!!! 
							filesToDownload.add(getNoteServerUri(noteInformation));
						}
					}
					
					setSyncProgress(25);					
					if(cancelled) {
						removeLock(connection);
						doCancel();
						return; 
					}	
					
					String[] strings = filesToDownload.toArray(new String[filesToDownload.size()]);
					scpClient.get(strings, localPath().getPath());
					
					
					for (int i = 0; i < notes.getLength(); i++) {
						setSyncProgress(40 + (20*i/notes.getLength()));					
						if(cancelled) {
							removeLock(connection);
							doCancel();
							return; 
						}	
						
						// hope that is fast too (multicores will not be used as its single thread...): no threads...
						
						parseNote(notes.item(i), scpClient);
					}
					
					setSyncProgress(60);
					
					prepareSyncableNotes(syncableNotes);
					
					
				} catch (Exception e) {
					e.printStackTrace();
					// TODO sometimes two messages (toasts) will be sent. Only the last one is visible! 
					if (e.getMessage() == SERVER_LOCKED_ERROR_STRING) {
						sendMessage(SSH_LOCK);
					} else {	
						sendMessage(SSH_CONNECT_FAIL);
						removeLock(null);
					}
					Log.e(TAG, e.getLocalizedMessage());
					setSyncProgress(100);
				} finally {
					if (connection != null)
						connection.close();
				}
					
				
			}

			/**
			 * parses a note and adds it to syncable notes. if the note does not exist in the correct version locally, it will be downloaded first
			 * @param noteInformation
			 * @throws Exception if exception is thrown, syncing should be aborted.
			 */
			private void parseNote(Node noteInformation, SCPClient scpClient) throws Exception {
				String id = noteInformation.getAttributes().getNamedItem("id")
						.getNodeValue();
				
				File noteFile = new File(localPath().getPath() + "/" + id + ".note"); 
				
				if (!noteFile.isFile()) {
					pullNote(noteInformation, scpClient);
				}
			
				// start parsing it.
				Note note = new Note();
				final char[] buffer = new char[0x1000];  // TODO isn't that a kinda very dirty approach?
				String contents = "";
				try {
					note.setFileName(noteFile.getPath());
					
					note.setGuid(id);
					
					// Try reading the file first
					try {
						contents = SdCardSyncService.readFile(noteFile, buffer);
					} catch (IOException e) {
						TLog.w(TAG, "Something went wrong trying to read the note");
						sendMessage(PARSING_FAILED, ErrorList.createError(note, e));
						throw e;
					}
					
					// Parsing
					// XML
					// Get a SAXParser from the SAXPArserFactory
					SAXParserFactory spf = SAXParserFactory.newInstance();
					SAXParser sp = spf.newSAXParser();
					
					// Get the XMLReader of the SAXParser we created
					XMLReader xr = sp.getXMLReader();
					
					// Create a new ContentHandler, send it this note to fill and apply it to the XML-Reader
					NoteHandler xmlHandler = new NoteHandler(note);
					xr.setContentHandler(xmlHandler);
					
					// Create the proper input source
					StringReader sr = new StringReader(contents);
					InputSource is = new InputSource(sr);
					
					TLog.d(TAG, "parsing note. filename: {0}", noteFile.getName());
					xr.parse(is);
					
				} catch (Exception e) {
					e.printStackTrace();
					if(e instanceof TimeFormatException) TLog.e(TAG, "Problem parsing the note's date and time");
					sendMessage(PARSING_FAILED, ErrorList.createErrorWithContents(note, e, contents));
					throw e;
				}
				
				// FIXME here we are re-reading the whole note just to grab note-content out, there is probably a better way to do this (I'm talking to you xmlpull.org!)
				Matcher m = SdCardSyncService.note_content.matcher(contents);
				if (m.find()) {
					note.setXmlContent(NoteManager.stripTitleFromContent(m.group(1), note.getTitle()));
				} else {
					TLog.w(TAG, "Something went wrong trying to grab the note-content out of a note");
					sendMessage(PARSING_FAILED, ErrorList.createErrorWithContents(note, "Something went wrong trying to grab the note-content out of a note", contents));
					throw new Exception("Something went wrong trying to grab the note-content out of a note");
				}

				syncableNotes.add(note);
			}

			private void pullNote(Node noteInformation, SCPClient scpClient) throws IOException {
				String upstream = getNoteServerUri(noteInformation);
				scpClient.get(upstream, localPath().getPath());
			}

			private int getRevFromManifest(Document doc) throws Exception {
				NodeList syncTags = doc.getElementsByTagName("sync");
				if (syncTags.getLength() != 1) {
					throw new Exception("malformed manifest.xml");
				}
				return Integer.parseInt(syncTags.item(0).getAttributes().getNamedItem("revision").getNodeValue());
			}
			
			private String getNoteServerUri(Node noteInformation) {
				String id = noteInformation.getAttributes().getNamedItem("id")
						.getNodeValue();
				String rev = noteInformation.getAttributes().getNamedItem("rev")
						.getNodeValue();
				return folder + "/" + Integer.parseInt(rev) / 100 + "/" + rev + "/" + id + ".note";
			}
		});
	}


	public static Pattern getConnectionStringRegex() {
		return Pattern.compile("^(.*)@(.*)\\:(\\d+)(\\/.*)$");
	}

	@Override
	public boolean needsLocation() {
		// as we need a local folder to store temp notes in!
		return true;
	}

	@Override
	public int getDescriptionAsId() {
		return R.string.prefSshServer;
	}

	@Override
	protected void pullNote(String guid) {
		// start loading local notes
				// NEVER NEVER gets called!
		//TODO remove as deprecated!

	}


	private void finishSync(boolean refresh, boolean remove_lock) {
		if (remove_lock)
			removeLock(null);
		
		// delete leftover local notes
		NoteManager.purgeDeletedNotes(activity);

		Time now = new Time();
		now.setToNow();
		String nowString = now.formatTomboy();
		Preferences.putString(Preferences.Key.LATEST_SYNC_DATE, nowString);
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, newRevision);

		setSyncProgress(100);
		if (refresh)
			sendMessage(PARSING_COMPLETE);
	}
	
	@Override
	public void finishSync(boolean refresh) {
		finishSync(refresh, true);
	}
	
	// actually pushes a note to ssh server
	private void pushNote(Note note, SCPClient scpClient) throws Exception {
		// save file in ssh folder
		if (SdCardSyncService.doPushNote(note, "/ssh") == NOTE_PUSHED) {
			// now upload file.
			String noteFileName = note.getGuid() + ".note";
			scpClient.put(localPath().getPath() + "/" + noteFileName, newRevisionPath);
		} else {
			TLog.e(TAG, "upload {0} to server did not work", note.getGuid());	
			// cancel syncing...
			throw new Exception("push failed as some notes could not be uploaded.");
			
		}
		
	}

	/// checks if directory exists on server and if not tries to create it and adjusts permissions
	private void assertServerDir(String dir, SFTPv3Client sftpClient) throws Exception {
		try {
			if (!sftpClient.stat(dir).isDirectory()) {
				// strange error. no Dir but a file or link. abord here!
				// XXX TIME!!! check if time is correct everywhere.
				throw new Exception(String.format("could not create new Revision path %s", newRevisionPath));
			}
		} catch (IOException e) {
			// file/folder seems to not exist.
			sftpClient.mkdir(dir, 0777);
			// AdjustPermissions ?
		}
	}

	// this function either deletes or pushes, based on existence of deleted tag
	@Override
	public void pushNotes(final ArrayList<Note> notes) { // DUH! simply use git!!
		// must perform it in thread again...  AU!
		execInThread(new Runnable() {
			@Override
			public void run() {
				
				// reverse engineering FileSystemSyncServer.cs here...
				// when pushing first create a new rev.
				// create rev dir on server and copy all to push notes in it.
				// if this was successfully than...
				
				Connection connection = null;
				try {
					connection = spawnSSHConnection();
				} catch (Exception e) {
					return;
				}
				
				SFTPv3Client sftpClient = null;

				if(notes.size() == 0)
					return;

				// TODO: error-checking, etc
				try {
					sftpClient = new SFTPv3Client(connection);
					assertServerDir(new File(newRevisionPath).getParent(), sftpClient);
					assertServerDir(newRevisionPath, sftpClient);

					// need an scp client too for easy upload.
					SCPClient scpClient = new SCPClient(connection);

					// generate new manifest
					// old manifest as template. remove all to be removed, change rev of all where rev should be changed.. dan

					Node sync = manifest.getElementsByTagName("sync").item(0);
					sync.getAttributes().getNamedItem("revision").setNodeValue(""+newRevision);
					// delete if note.getTags().contains("system:deleted") otherwise push.
					for (Note note : notes) {				
						// remove from manifest
						Node it = manifest.getElementById(note.getGuid()); 
						if (it == null) 
							it = manifest.createElement("note");
						else
							sync.removeChild(it);
						
						if(note.getTags().contains("system:deleted")) {// deleted note
							// if deleted we don't need to do anything server-side. Simply don't upload it again.
						} else {
							// add to manifest with correct version!
							// TODO: xml not well formatted!

							Attr idAttr = manifest.createAttribute("id");
							idAttr.setValue(note.getGuid());
							it.getAttributes().setNamedItem(idAttr);

							Attr revAttr = manifest.createAttribute("rev");
							revAttr.setValue(""+newRevision);
							it.getAttributes().setNamedItem(revAttr);

							sync.appendChild(it);

							pushNote(note, scpClient);
						}
						
						if(cancelled) {
							sftpClient.close();
							removeLock(connection);
							doCancel();
							return; 
						}	
					}

					// create and save new manifest locally.
					// write the content into local xml file
					writeXML(manifest, localPath().getPath() + "/manifest.xml");
					
					// Rename original /manifest.xml to /manifest.xml.old if exists.
					if (!firstSync)
						sftpClient.mv(folder + "/manifest.xml", folder + "/manifest.xml.old");

					//sftpClient.cp(newRevisionPath + "/manifest.xml", folder + "/manifest.xml"); does not exist
					scpClient.put(localPath().getPath() + "/manifest.xml", newRevisionPath);
					scpClient.put(localPath().getPath() + "/manifest.xml", folder);

					// must set permissions online as offline does not work properly in early java versions
					SFTPv3FileAttributes attr;
					attr = sftpClient.stat(newRevisionPath + "/manifest.xml");
					sftpClient.setstat(newRevisionPath + "/manifest.xml", attr);
					attr = sftpClient.stat(folder + "/manifest.xml");
					sftpClient.setstat(folder + "/manifest.xml", attr);

					// Delete /manifest.xml.old
					if (!firstSync)
						sftpClient.rm(folder + "/manifest.xml.old");

					removeLock(connection);
					finishSync(true, false);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "push failed");
					Log.e(TAG, e.getLocalizedMessage());
					sendMessage(SSH_PUSH_FAIL);
					removeLock(connection);
					setSyncProgress(100);
				} finally {	
					if (sftpClient != null)
						sftpClient.close();
					if (connection != null)
						connection.close();					
				}
			}

		});
	}

	// XXX dirty!
	@SuppressLint("NewApi") private void writeXML(Document document, String filename) throws TransformerException {		
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(new File(filename));
 
		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);
 
		transformer.transform(source, result);
	}

	@Override
	public void backupNotes() {
		// TODO Auto-generated method stub
		// back up notes locally!?
	}

	@Override
	public void deleteAllNotes() {
		// TODO
		// nope I don't like that atm!
		// show (AlertDialog) dialog).setButton(text, listener; -- isn't it a bit dirty to do this here?
		sendMessage(NOTE_DELETE_ERROR);

	}

	@Override
	protected void localSyncComplete() {
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, latestRemoteRevision);
	}
}
