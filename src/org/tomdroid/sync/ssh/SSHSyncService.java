package org.tomdroid.sync.ssh;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.tomdroid.Note;
import org.tomdroid.sync.SyncService;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.util.TimeFormatException;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;

/**
 * Sync service for syncing tombody notes from a remote ssh folder.
 * 
 * @author alexander rausch <mail@arausch.de>
 * 
 */
public class SSHSyncService extends SyncService {

	private static final String TAG = "SSHSyncService";

	private File path;

	private int numberOfFilesToSync;

	private String hostname;

	private String username;

	private String password;

	private String privkeyFile;

	private int port;

	private String folder;

	public SSHSyncService(Activity activity, Handler handler) {
		super(activity, handler);
		path = new File(Tomdroid.NOTES_PATH + "ssh");

		if (!path.exists()) {
			path.mkdirs();
		}
	}

	@Override
	protected void sync() {
		// start
		setSyncProgress(0);

		if (Tomdroid.LOGGING_ENABLED) {
			Log.v(TAG, "Loading notes from ssh");
		}

		// clear up ssh note dir
		File[] files = path.listFiles();
		for (File f : files) {
			if (f.isFile() && f.getName().contains(".note")) {
				f.delete();
			}
		}
		hostname = Preferences.getString(Preferences.Key.SYNC_SERVER_SSH_HOST);
		username = Preferences
				.getString(Preferences.Key.SYNC_SERVER_SSH_USERNAME);
		password = CryptHelper.decodePW(Preferences
				.getString(Preferences.Key.SYNC_SERVER_SSH_PASSWORD));
		port = Integer.parseInt(Preferences
				.getString(Preferences.Key.SYNC_SERVER_SSH_PORT));
		folder = Preferences.getString(Preferences.Key.SYNC_SERVER_SSH_FODLER);
		privkeyFile = Preferences
				.getString(Preferences.Key.SYNC_SERVER_SSH_PRIVATE_KEY);

		Connection connection = null;
		try {
			connection = new Connection(hostname, port);
			connection.connect();
			boolean isAuthenticated = false;

			if (privkeyFile == null
					|| (privkeyFile != null && privkeyFile.length() == 0)
					&& username != null && username.length() > 0
					&& password != null && password.length() > 0) {
				isAuthenticated = connection.authenticateWithPassword(username,
						password);
			} else if (privkeyFile != null && privkeyFile.length() > 0) {
				File keyfile = new File(path + "/" + privkeyFile);
				if (password == null) {
					password = "";
				}
				// private key password is ignored if it isn't needed by the
				// cert
				isAuthenticated = connection.authenticateWithPublicKey(
						username, keyfile, password);
			}

			if (isAuthenticated) {
				/* Create a SCP client */
				SCPClient scpClient = new SCPClient(connection);

				scpClient.get(folder + "/manifest.xml", path.getPath());

				try {
					DocumentBuilderFactory factory = DocumentBuilderFactory
							.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document document = builder.parse(new File(path.getPath()
							+ "/manifest.xml"));

					NodeList notes = document.getElementsByTagName("note");

					numberOfFilesToSync = notes.getLength();

					for (int i = 0; i < notes.getLength(); i++) {
						Node noteInformation = notes.item(i);
						execInThread(new Worker(scpClient, connection,
								noteInformation, i == notes.getLength() - 1));
					}
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, e.getLocalizedMessage());
					sendMessage(SSH_CONNECT_FAIL);
				}
			} else {
				sendMessage(SSH_LOGIN_FAIL);
				connection.close();
				setSyncProgress(100);
			}
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, e.getLocalizedMessage());
			sendMessage(SSH_CONNECT_FAIL);
			if (connection != null) {
				connection.close();
			}
			setSyncProgress(100);
		}
	}

	@Override
	public boolean needsServer() {
		return true;
	}

	@Override
	public boolean needsAuth() {
		return true;
	}

	@Override
	public String getName() {
		return "sshsync";
	}

	@Override
	public String getDescription() {
		return "SSH Sync";
	}

	/**
	 * The worker spawns a new note, parse the file its being given by the
	 * executor.
	 */
	private class Worker implements Runnable {

		// the note to be loaded and parsed
		private Note note = new Note();
		private Node noteInformation;
		private final SCPClient scpClient;
		private boolean isLast;
		private final Connection connection;

		public Worker(SCPClient scpClient, Connection connection,
				Node noteInformation, boolean isLast) {
			this.noteInformation = noteInformation;
			this.isLast = isLast;
			this.scpClient = scpClient;
			this.connection = connection;
		}

		public void run() {

			String id = noteInformation.getAttributes().getNamedItem("id")
					.getNodeValue();
			String rev = noteInformation.getAttributes().getNamedItem("rev")
					.getNodeValue();

			try {
				scpClient.get(folder + "/0/" + rev + "/" + id + ".note",
						path.getPath());

				String noteLocalPath = path.getPath() + "/" + id + ".note";
				note.setFileName(noteLocalPath);

				note.setGuid(id);

				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document document = builder.parse(new File(noteLocalPath));

				NodeList lastChangeElements = document
						.getElementsByTagName("last-change-date");
				String lastChanged = lastChangeElements.item(0).getFirstChild()
						.getNodeValue();
				if (lastChanged != null) {
					note.setLastChangeDate(lastChanged);
				}

				NodeList titleChangedElements = document
						.getElementsByTagName("title");
				String title = titleChangedElements.item(0).getFirstChild()
						.getNodeValue();

				if (title != null) {
					note.setTitle(title);
				}

				NodeList contentElements = document
						.getElementsByTagName("note-content");
				StringBuilder sb = new StringBuilder();

				if (contentElements.item(0).hasChildNodes()) {
					extractNodeContent(sb, contentElements.item(0));
				}

				if (sb != null && sb.length() > 0) {
					note.setXmlContent(sb.toString());
				}

				// TODO wrap and throw a new exception here
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				Log.e(TAG, e.getLocalizedMessage());
				sendMessage(PARSING_FAILED);
			} catch (SAXException e) {
				e.printStackTrace();
				Log.e(TAG, e.getLocalizedMessage());
				sendMessage(PARSING_FAILED);
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, e.getLocalizedMessage());
				sendMessage(PARSING_FAILED);
			} catch (TimeFormatException e) {
				e.printStackTrace();
				Log.e(TAG, "Problem parsing the note's date and time");
				sendMessage(PARSING_FAILED);
				onWorkDone();
				return;
			}

			insertNote(note);
			onWorkDone();
		}

		/**
		 * recusivly extracts the xml content from the note's <note-content>
		 * tag.
		 * 
		 * @param sb
		 * @param nodeToParse
		 */
		private void extractNodeContent(StringBuilder sb, Node nodeToParse) {
			if (nodeToParse.getChildNodes().getLength() > 0) {
				for (int i = 0; i < nodeToParse.getChildNodes().getLength(); i++) {
					Node node = nodeToParse.getChildNodes().item(i);
					if (node instanceof Text) {
						String escapedXML = XmlUtils.escape(((Text) node)
								.getNodeValue());
						sb.append(escapedXML);
					} else if (node instanceof Element) {
						Element nodeAsElement = (Element) node;
						sb.append("<" + nodeAsElement.getTagName() + ">");
						extractNodeContent(sb, node);
						sb.append("</" + nodeAsElement.getTagName() + ">");
					}
				}
			}
		}

		private void onWorkDone() {
			if (isLast) {
				connection.close();
				setSyncProgress(100);
			} else {
				setSyncProgress((int) (getSyncProgress() + 100.0 / numberOfFilesToSync));
			}
		}
	}
}
