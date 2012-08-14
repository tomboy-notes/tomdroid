/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, 2010, 2011 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
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
 * along with Tomdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomdroid.ui;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncService;
import org.tomdroid.util.ErrorList;
import org.tomdroid.util.FirstNote;
import org.tomdroid.util.LinkifyPhone;
import org.tomdroid.util.NewNote;
import org.tomdroid.util.NoteContentBuilder;
import org.tomdroid.util.NoteViewShortcutsHelper;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.Send;
import org.tomdroid.util.TLog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.format.Time;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class Tomdroid extends ListActivity {

	// Global definition for Tomdroid
	public static final String	AUTHORITY			= "org.tomdroid.notes";
	public static final Uri		CONTENT_URI			= Uri.parse("content://" + AUTHORITY + "/notes");
	public static final String	CONTENT_TYPE		= "vnd.android.cursor.dir/vnd.tomdroid.note";
	public static final String	CONTENT_ITEM_TYPE	= "vnd.android.cursor.item/vnd.tomdroid.note";
	public static final String	PROJECT_HOMEPAGE	= "http://www.launchpad.net/tomdroid/";
	public static final String CALLED_FROM_SHORTCUT_EXTRA = "org.tomdroid.CALLED_FROM_SHORTCUT";
    public static final String SHORTCUT_NAME = "org.tomdroid.SHORTCUT_NAME";

	// config parameters
	public static String	NOTES_PATH				= null;
	
	// Set this to false for release builds, the reason should be obvious
	public static final boolean	CLEAR_PREFERENCES	= false;

	// Logging info
	private static final String	TAG					= "Tomdroid";

	// Logging should be disabled for release builds
	public static final boolean	LOGGING_ENABLED		= false;

	public static Uri getNoteIntentUri(long noteId) {
        return Uri.parse(CONTENT_URI + "/" + noteId);
    }

	// UI to data model glue
	private TextView			listEmptyView;
	private ListAdapter			adapter;

	// UI feedback handler
	private Handler	 syncMessageHandler	= new SyncMessageHandler(this);

	// UI for tablet
	private LinearLayout rightPane;
	private TextView content;
	private TextView title;
	
	// other tablet-based variables

	private Note note;
	private SpannableStringBuilder noteContent;
	private Uri uri;
	private int lastIndex = 0;
	public MenuItem syncMenuItem;
	public static Tomdroid context;
	
	/** Called when the activity is created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.context = this;

		setContentView(R.layout.main);
		Preferences.init(this, CLEAR_PREFERENCES);
		
		// get the Path to the notes-folder from Preferences
		NOTES_PATH = Environment.getExternalStorageDirectory()
				+ "/" + Preferences.getString(Preferences.Key.SD_LOCATION) + "/";
		
		// did we already show the warning and got destroyed by android's activity killer?
		if (Preferences.getBoolean(Preferences.Key.FIRST_RUN)) {
			TLog.i(TAG, "Tomdroid is first run.");
			
			// add a first explanatory note
			NoteManager.putNote(this, FirstNote.createFirstNote());
			
			// Warn that this is a "will eat your babies" release
			new AlertDialog.Builder(this).setMessage(getString(R.string.strWelcome)).setTitle(
					getString(R.string.titleWelcome)).setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					Preferences.putBoolean(Preferences.Key.FIRST_RUN, false);
					dialog.dismiss();
				}
			}).setIcon(R.drawable.icon).show();
		}
		
		// adapter that binds the ListView UI to the notes in the note manager
		adapter = NoteManager.getListAdapter(this);
		setListAdapter(adapter);

		// set the view shown when the list is empty
		listEmptyView = (TextView) findViewById(R.id.list_empty);
		getListView().setEmptyView(listEmptyView);

		registerForContextMenu(findViewById(android.R.id.list));
		
		// add note to pane for tablet
		
		rightPane = (LinearLayout) findViewById(R.id.right_pane);
		
		if(rightPane != null) {
			content = (TextView) findViewById(R.id.content);
			title = (TextView) findViewById(R.id.title);
			
			// this we will call on resume as well.
			updateTextAttributes();
			showNoteInPane(-1);
		}
	}
	private void updateTextAttributes() {
		float baseSize = Float.parseFloat(Preferences.getString(Preferences.Key.BASE_TEXT_SIZE));
		content.setTextSize(baseSize);
		title.setTextSize(baseSize*1.3f);

		title.setTextColor(Color.BLUE);
		title.setPaintFlags(title.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		title.setBackgroundColor(0xffffffff);

		content.setBackgroundColor(0xffffffff);
		content.setTextColor(Color.DKGRAY);		
	}
	private void showNoteInPane(int position) {
		if(rightPane == null)
			return;
		if(position == -1) {
			adapter = NoteManager.getListAdapter(this);
			setListAdapter(adapter);
			position = 0;
		}
		Cursor item = (Cursor) adapter.getItem(position);
		if (item == null || item.getCount() == 0) {
            TLog.d(TAG, "Index {0} not found in list", position);
            title.setText("");
            content.setText("");
			return;
		}
		long noteId = item.getInt(item.getColumnIndexOrThrow(Note.ID));	
		uri = Uri.parse(CONTENT_URI + "/" + noteId);

		TLog.d(TAG, "Getting note {0}", position);

        note = NoteManager.getNote(this, uri);

        if(note != null) {
    		TLog.d(TAG, "note {0} found", position);
			title.setText((CharSequence) note.getTitle());
            noteContent = new NoteContentBuilder().setCaller(noteContentHandler).setInputSource(note.getXmlContent()).setTitle(note.getTitle()).build();
        } else {
            TLog.d(TAG, "The note {0} doesn't exist", uri);
            showNoteNotFoundDialog(uri);
        }
	}
	private void showNote(boolean xml) {
		
		if(xml) {
			content.setText(note.getXmlContent());
			return;
		}
		

		// show the note (spannable makes the TextView able to output styled text)
		content.setText(noteContent, TextView.BufferType.SPANNABLE);

		// add links to stuff that is understood by Android except phone numbers because it's too aggressive
		// TODO this is SLOWWWW!!!!
		Linkify.addLinks(content, Linkify.EMAIL_ADDRESSES|Linkify.WEB_URLS|Linkify.MAP_ADDRESSES);

		// Custom phone number linkifier (fixes lp:512204)
		Linkify.addLinks(content, LinkifyPhone.PHONE_PATTERN, "tel:", LinkifyPhone.sPhoneNumberMatchFilter, Linkify.sPhoneNumberTransformFilter);

		// This will create a link every time a note title is found in the text.
		// The pattern contains a very dumb (title1)|(title2) escaped correctly
		// Then we transform the url from the note name to the note id to avoid characters that mess up with the URI (ex: ?)
		
		Pattern pattern = buildNoteLinkifyPattern();
		
		if(pattern != null)
			Linkify.addLinks(content,
							 buildNoteLinkifyPattern(),
							 Tomdroid.CONTENT_URI+"/",
							 null,
							 noteTitleTransformFilter);
	}
	private void showNoteNotFoundDialog(final Uri uri) {
	    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    addCommonNoteNotFoundDialogElements(builder);
	    addShortcutNoteNotFoundElements(uri, builder);
	    builder.show();
	}
	private void addShortcutNoteNotFoundElements(final Uri uri, final AlertDialog.Builder builder) {
	    final boolean proposeShortcutRemoval;
	    final boolean calledFromShortcut = getIntent().getBooleanExtra(CALLED_FROM_SHORTCUT_EXTRA, false);
	    final String shortcutName = getIntent().getStringExtra(SHORTCUT_NAME);
	    proposeShortcutRemoval = calledFromShortcut && uri != null && shortcutName != null;
	
	    if (proposeShortcutRemoval) {
	        final Intent removeIntent = new NoteViewShortcutsHelper(this).getRemoveShortcutIntent(shortcutName, uri);
	        builder.setPositiveButton(getString(R.string.btnRemoveShortcut), new OnClickListener() {
	            public void onClick(final DialogInterface dialogInterface, final int i) {
	                sendBroadcast(removeIntent);
	                finish();
	            }
	        });
	    }
	}
	
	private void addCommonNoteNotFoundDialogElements(final AlertDialog.Builder builder) {
	    builder.setMessage(getString(R.string.messageNoteNotFound))
	            .setTitle(getString(R.string.titleNoteNotFound))
	            .setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
	                public void onClick(DialogInterface dialog, int which) {
	                    dialog.dismiss();
	                    finish();
	                }
	            });
	}	
	
	private Handler noteContentHandler = new Handler() {
	
		@Override
		public void handleMessage(Message msg) {
	
			//parsed ok - show
			if(msg.what == NoteContentBuilder.PARSE_OK) {
				showNote(false);
	
			//parsed not ok - error
			} else if(msg.what == NoteContentBuilder.PARSE_ERROR) {
	
				new AlertDialog.Builder(Tomdroid.this)
					.setMessage(getString(R.string.messageErrorNoteParsing))
					.setTitle(getString(R.string.error))
					.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							showNote(true);
						}})
					.show();
	    	}
		}
	};
	
	/**
	 * Builds a regular expression pattern that will match any of the note title currently in the collection.
	 * Useful for the Linkify to create the links to the notes.
	 * @return regexp pattern
	 */
	private Pattern buildNoteLinkifyPattern()  {
	
		StringBuilder sb = new StringBuilder();
		Cursor cursor = NoteManager.getTitles(this);
	
		// cursor must not be null and must return more than 0 entry
		if (!(cursor == null || cursor.getCount() == 0)) {
	
			String title;
	
			cursor.moveToFirst();
	
			do {
				title = cursor.getString(cursor.getColumnIndexOrThrow(Note.TITLE));
				if(title.length() == 0)
					continue;
				// Pattern.quote() here make sure that special characters in the note's title are properly escaped
				sb.append("("+Pattern.quote(title)+")|");
	
			} while (cursor.moveToNext());
			
			// if only empty titles, return
			if (sb.length() == 0)
				return null;
			
			// get rid of the last | that is not needed (I know, its ugly.. better idea?)
			String pt = sb.substring(0, sb.length()-1);
	
			// return a compiled match pattern
			return Pattern.compile(pt);
	
		} else {
	
			// TODO send an error to the user
			TLog.d(TAG, "Cursor returned null or 0 notes");
		}
	
		return null;
	}
	
	// custom transform filter that takes the note's title part of the URI and translate it into the note id
	// this was done to avoid problems with invalid characters in URI (ex: ? is the query separator but could be in a note title)
	private TransformFilter noteTitleTransformFilter = new TransformFilter() {
	
		public String transformUrl(Matcher m, String str) {
	
			int id = NoteManager.getNoteId(Tomdroid.this, str);
	
			// return something like content://org.tomdroid.notes/notes/3
			return Tomdroid.CONTENT_URI.toString()+"/"+id;
		}
	};
	private boolean creating = true;
	private SyncManager sync;
	private static ProgressDialog syncProgressDialog;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Create the menu based on what is defined in res/menu/main.xml
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuAbout:
				showAboutDialog();
				return true;
			case R.id.menuSync:
//				this.syncMenuItem = item;
//				item.setTitle(Tomdroid.this.getString(R.string.syncing));
				startSyncing(true);
				return true;
			case R.id.menuNew:
				newNote();
				return true;
			case R.id.menuRevert:
				new AlertDialog.Builder(this)
		        .setIcon(android.R.drawable.ic_dialog_alert)
		        .setTitle(R.string.revert_notes)
		        .setMessage(R.string.revert_notes_message)
		        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

		            public void onClick(DialogInterface dialog, int which) {
						startSyncing(false);
		           }

		        })
		        .setNegativeButton(R.string.no, null)
		        .show();
				return true;
			case R.id.menuPrefs:
				startActivity(new Intent(this, PreferencesActivity.class));
				return true;
				
			case R.id.menuSearch:
				startSearch(null, false, null, false);
				return true;

			// tablet 
			
			case R.id.menuEdit:
				if(note != null)
					startEditNote();
				return true;
			case R.id.menuDelete:
				if(note != null)
					deleteNote(note.getDbId());
				return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	private void startSyncing(boolean push) {

		SyncService currentService = SyncManager.getInstance().getCurrentService();
		String serverUri = Preferences.getString(Preferences.Key.SYNC_SERVER);
		
		if (currentService.needsAuth()) {
	
			// service needs authentication
			TLog.i(TAG, "Creating dialog");
	
			final ProgressDialog authProgress = ProgressDialog.show(this, "", getString(R.string.prefSyncCompleteAuth), true, false);
	
			Handler handler = new Handler() {
	
				@Override
				public void handleMessage(Message msg) {
	
					boolean wasSuccessful = false;
					Uri authorizationUri = (Uri) msg.obj;
					if (authorizationUri != null) {
	
						Intent i = new Intent(Intent.ACTION_VIEW, authorizationUri);
						startActivity(i);
						wasSuccessful = true;
	
					} else {
						// Auth failed, don't update the value
						wasSuccessful = false;
					}
	
					if (authProgress != null)
						authProgress.dismiss();
	
					if (wasSuccessful) {
						resetLocalDatabase();
					} else {
						connectionFailed();
					}
				}
			};

			((ServiceAuth) currentService).getAuthUri(serverUri, handler);
		}
		else {
			String serviceDescription = SyncManager.getInstance().getCurrentService().getDescription();
			
			syncProgressDialog = new ProgressDialog(this);
			syncProgressDialog.setTitle(String.format(getString(R.string.syncing),serviceDescription));
			syncProgressDialog.setMessage(getString(R.string.syncing_connect));
			syncProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			syncProgressDialog.setCancelable(false);
			syncProgressDialog.setMax(0);
	        syncProgressDialog.setIndeterminate(true);
			syncProgressDialog.show();
	
	        sync = SyncManager.getInstance();
	    	sync.startSynchronization(push); // push by default
		}
	}
	
	//TODO use LocalStorage wrapper from two-way-sync branch when it get's merged
	private void resetLocalDatabase() {
		getContentResolver().delete(Tomdroid.CONTENT_URI, null, null);
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, 0);
		
		// first explanatory note will be deleted on sync
		//NoteManager.putNote(this, FirstNote.createFirstNote());
	}
	private void connectionFailed() {
		new AlertDialog.Builder(this)
			.setMessage(getString(R.string.prefSyncConnectionFailed))
			.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}})
			.show();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_longclick, menu);
	    menu.setHeaderTitle(getString(R.string.noteOptions));
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		long noteId = info.id;
		Uri intentUri = Uri.parse(Tomdroid.CONTENT_URI+"/"+noteId);
        Note note = NoteManager.getNote(this, intentUri);

        switch (item.getItemId()) {
            case R.id.menu_send:
                (new Send(this, note)).send();
				break;
			case R.id.view:
				this.ViewNote(noteId);
				break;
			case R.id.edit:
				this.startEditNote(noteId);
				break;
			case R.id.revert:
				this.revertNote(note.getGuid());
				break;
			case R.id.delete:
				this.deleteNote(noteId);
				break;
			case R.id.create_shortcut:
                final NoteViewShortcutsHelper helper = new NoteViewShortcutsHelper(this);
                sendBroadcast(helper.getBroadcastableCreateShortcutIntent(intentUri, note.getTitle()));
                break;
            default:
				break;
		}
		
		return super.onContextItemSelected(item);
	}

	public void onResume() {
		super.onResume();
		Intent intent = this.getIntent();

		SyncService currentService = SyncManager.getInstance().getCurrentService();
		
		if (currentService.needsAuth() && intent != null) {
			Uri uri = intent.getData();

			if (uri != null && uri.getScheme().equals("tomdroid")) {
				TLog.i(TAG, "Got url : {0}", uri.toString());

				final ProgressDialog dialog = ProgressDialog.show(this, "",	getString(R.string.prefSyncCompleteAuth), true, false);

				Handler handler = new Handler() {

					@Override
					public void handleMessage(Message msg) {
						dialog.dismiss();
						if(msg.what == SyncService.AUTH_COMPLETE)
							startSyncing(true);
					}

				};

				((ServiceAuth) currentService).remoteAuthComplete(uri, handler);
			}
		}
		
		SyncManager.setActivity(this);
		SyncManager.setHandler(this.syncMessageHandler);
		
		// tablet refresh
		
		if(rightPane != null) {
			updateTextAttributes();
			if(!creating)
				showNoteInPane(lastIndex);
		}
		creating = false;
	}

	private void showAboutDialog() {

		// grab version info
		String ver;
		try {
			ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			ver = "Not found!";
		}

		// format the string
		String aboutDialogFormat = getString(R.string.strAbout);
		String aboutDialogStr = String.format(aboutDialogFormat, getString(R.string.app_desc), // App description
				getString(R.string.author), // Author name
				ver // Version
				);

		// build and show the dialog
		new AlertDialog.Builder(this).setMessage(aboutDialogStr).setTitle(getString(R.string.titleAbout))
				.setIcon(R.drawable.icon).setNegativeButton(getString(R.string.btnProjectPage), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri
								.parse(Tomdroid.PROJECT_HOMEPAGE)));
						dialog.dismiss();
					}
				}).setPositiveButton(getString(R.string.btnOk), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (rightPane != null) {
			if(position == lastIndex) // same index, edit
				this.startEditNote();
			else
				showNoteInPane(position);
		}
		else {
			Cursor item = (Cursor) adapter.getItem(position);
			long noteId = item.getInt(item.getColumnIndexOrThrow(Note.ID));
				this.ViewNote(noteId);
		}
		lastIndex = position;
	}
	
	public void ViewNote(long noteId) {
		Uri intentUri = getNoteIntentUri(noteId);
		Intent i = new Intent(Intent.ACTION_VIEW, intentUri, this, ViewNote.class);
		startActivity(i);
	}
	
	protected void startEditNote() {
		final Intent i = new Intent(Intent.ACTION_VIEW, uri, this, EditNote.class);
		startActivity(i);
	}
	
	protected void startEditNote(long noteId) {
		Uri intentUri = getNoteIntentUri(noteId);
		final Intent i = new Intent(Intent.ACTION_VIEW, intentUri, this, EditNote.class);
		startActivity(i);
	}
	
	public static void ViewList(Context View) {
		if ( ! ( View instanceof Tomdroid ) )
	    {
			View.startActivity(new Intent(View, Tomdroid.class)
			.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	    }
	}

	public void newNote() {
		
		// add a new note
		
		Note note = NewNote.createNewNote(this);
		Uri uri = NoteManager.putNote(this, note);
		
		// set list item to top
		
		lastIndex = 0;
		
		// recreate listAdapter
		
		adapter = NoteManager.getListAdapter(this);
		setListAdapter(adapter);
		
		// view new note
		
		Intent i = new Intent(Intent.ACTION_VIEW, uri, this, EditNote.class);
		startActivity(i);

		
	}
	private void deleteNote(long noteId) {
		
		final Note note = NoteManager.getNote(this, Uri.parse(Tomdroid.CONTENT_URI + "/" + noteId));
		
		final Activity activity = this;
		new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.delete_note)
        .setMessage(R.string.delete_message)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
        		NoteManager.deleteNote(activity, note);
        		lastIndex = 0;
    			showNoteInPane(-1);
           }

        })
        .setNegativeButton(R.string.no, null)
        .show();
	}
	private void revertNote(final String guid) {
		
		new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.revert_note)
        .setMessage(R.string.revert_note_message)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
				SyncManager.getInstance().pullNote(guid);
           }

        })
        .setNegativeButton(R.string.no, null)
        .show();
	}

	public class SyncMessageHandler extends Handler {
	
		private Activity activity;
		
		public SyncMessageHandler(Activity activity) {
			this.activity = activity;
		}
	
		@Override
		public void handleMessage(Message msg) {
	
			String serviceDescription = SyncManager.getInstance().getCurrentService().getDescription();
			String message = "";
			boolean increment = false;
			boolean dismiss = false;
			TLog.d(TAG, "SyncMessageHandler message: {0}",msg.what);
			
			try {
				switch (msg.what) {
					case SyncService.AUTH_COMPLETE:
						message = this.activity.getString(R.string.messageAuthComplete);
						message = String.format(message,serviceDescription);
						Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
						break;
					case SyncService.AUTH_FAILED:
						dismiss = true;
						message = this.activity.getString(R.string.messageAuthFailed);
						message = String.format(message,serviceDescription);
						Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
						break;
					case SyncService.PARSING_COMPLETE:
						final ErrorList errors = (ErrorList)msg.obj;
						if(errors == null || errors.isEmpty()) {
							message = this.activity.getString(R.string.messageSyncComplete);
							message = String.format(message,serviceDescription);
							Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
							finishSync();
						} else {
		
							message = this.activity.getString(R.string.messageSyncError);
							new AlertDialog.Builder(activity).setMessage(message)
								.setTitle(this.activity.getString(R.string.error))
								.setPositiveButton(this.activity.getString(R.string.btnSavetoSD), new OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										if(!errors.save()) {
											Toast.makeText(activity, activity.getString(R.string.messageCouldNotSave),
													Toast.LENGTH_LONG).show();
										}
										finishSync();
									}
								})
								.setNegativeButton("Close", new OnClickListener() {
									public void onClick(DialogInterface dialog, int which) { finishSync(); }
								}).show();
						}
						break;
					case SyncService.CONNECTING_FAILED:
						dismiss = true;
						message = this.activity.getString(R.string.messageSyncConnectingFailed);
						message = String.format(message,serviceDescription);
						Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
						break;
					case SyncService.PARSING_FAILED:
						dismiss = true;
						message = this.activity.getString(R.string.messageSyncParseFailed);
						message = String.format(message,serviceDescription);
						Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
						break;
					case SyncService.PARSING_NO_NOTES:
						dismiss = true;
						message = this.activity.getString(R.string.messageSyncNoNote);
						message = String.format(message,serviceDescription);
						Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
						break;
						
					case SyncService.NO_INTERNET:
						dismiss = true;
						Toast.makeText(activity, this.activity.getString(R.string.messageSyncNoConnection),Toast.LENGTH_LONG).show();
						break;
						
					case SyncService.NO_SD_CARD:
						dismiss = true;
						Toast.makeText(activity, activity.getString(R.string.messageNoSDCard),
								Toast.LENGTH_SHORT).show();
						break;
						
					case SyncService.BEGIN_PROGRESS:
						if(syncProgressDialog != null) {
							HashMap<String, Object> hm = (HashMap<String, Object>) msg.obj;
					        syncProgressDialog.setIndeterminate(false);
					        syncProgressDialog.setMessage(getString(R.string.syncing_local));
					        syncProgressDialog.setProgress(0);
							syncProgressDialog.setMax((Integer) hm.get("total"));
							syncProgressDialog.show();
						}
						break;
						
					case SyncService.SYNC_PROGRESS:
						if(msg.arg1 == 90)
							if(syncProgressDialog != null)
								syncProgressDialog.setMessage(getString(R.string.syncing_remote));
						break;

					case SyncService.NOTE_DELETED:
						increment = true;
						message = this.activity.getString(R.string.messageSyncNoteDeleted);
						message = String.format(message,serviceDescription);
						//Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
						break;
		
					case SyncService.NOTE_PUSHED:
						increment = true;
						message = this.activity.getString(R.string.messageSyncNotePushed);
						message = String.format(message,serviceDescription);
						//Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
	
						break;
					case SyncService.NOTE_PULLED:
						message = this.activity.getString(R.string.messageSyncNotePulled);
						message = String.format(message,serviceDescription);
						//Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
						increment = true;
						break;
															
					case SyncService.NOTE_DELETE_ERROR:
						increment = true;
						//Toast.makeText(activity, activity.getString(R.string.messageSyncNoteDeleteError), Toast.LENGTH_SHORT).show();
						break;
		
					case SyncService.NOTE_PUSH_ERROR:
						increment = true;
						//Toast.makeText(activity, activity.getString(R.string.messageSyncNotePushError), Toast.LENGTH_SHORT).show();
						break;
					case SyncService.NOTE_PULL_ERROR:
						message = this.activity.getString(R.string.messageSyncNotePullError);
						message = String.format(message,serviceDescription);
						//Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
						break;
					case SyncService.INCREMENT_PROGRESS:
						increment = true;
						break;
					case SyncService.IN_PROGRESS:
						Toast.makeText(activity, activity.getString(R.string.messageSyncAlreadyInProgress), Toast.LENGTH_LONG).show();
						dismiss = true;
						break;
					case SyncService.NOTES_BACKED_UP:
						Toast.makeText(activity, activity.getString(R.string.messageNotesBackedUp), Toast.LENGTH_SHORT).show();
						break;
					default:
						TLog.i(TAG, "handler called with an unknown message");
						dismiss = true;
						break;
		
				}
				if(syncProgressDialog != null) {
					if(increment) {
						TLog.d(TAG, "sync progress {0} of {1}",syncProgressDialog.getProgress(),syncProgressDialog.getMax());
						syncProgressDialog.setProgress(syncProgressDialog.getProgress()+1);
						if(syncProgressDialog.getProgress() >= syncProgressDialog.getMax())
							syncMessageHandler.sendEmptyMessage(SyncService.PARSING_COMPLETE);
					}
					if(dismiss)
						syncProgressDialog.dismiss();
				}	
			}
			catch(Exception e) {
				TLog.e(TAG, "SyncMessageHandler Error: {0}",e);
			}
		}
	}

	protected void  onActivityResult (int requestCode, int resultCode, Intent  data) {
		TLog.d(TAG, "onActivityResult called");
		syncMessageHandler.sendEmptyMessage(SyncService.INCREMENT_PROGRESS);
	}
	
	public void finishSync() {
		
		Time now = new Time();
		now.setToNow();
		String nowString = now.format3339(false);
		
	// delete leftover local notes
		
		NoteManager.deleteDeletedNotes(this);

		Preferences.putString(Preferences.Key.LATEST_SYNC_DATE, nowString);
		SyncService currentService = SyncManager.getInstance().getCurrentService();
		currentService.setSyncProgress(100);
		
		if(syncProgressDialog != null)
			syncProgressDialog.dismiss();
		if(rightPane != null)
			showNoteInPane(lastIndex);
	}
}
