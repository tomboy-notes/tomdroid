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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncService;
import org.tomdroid.ui.actionbar.ActionBarListActivity;
import org.tomdroid.util.FirstNote;
import org.tomdroid.util.LinkifyPhone;
import org.tomdroid.util.NewNote;
import org.tomdroid.util.NoteContentBuilder;
import org.tomdroid.util.NoteViewShortcutsHelper;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.Send;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.tomdroid.util.TLog;

public class Tomdroid extends ActionBarListActivity {

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
	private Handler	syncMessageHandler	= new SyncMessageHandler(this);

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
	
	/** Called when the activity is created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		Preferences.init(this, CLEAR_PREFERENCES);
		
		// get the Path to the notes-folder from Preferences
		NOTES_PATH = Environment.getExternalStorageDirectory()
				+ "/" + Preferences.getString(Preferences.Key.SD_LOCATION) + "/";
		
		// did we already show the warning and got destroyed by android's activity killer?
		if (Preferences.getBoolean(Preferences.Key.FIRST_RUN)) {
			TLog.i(TAG, "Tomdroid is first run.");
			
			// add a first explanatory note
			NoteManager.putNote(this, FirstNote.createFirstNote(), false);
			
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

		title.setTextColor(Color.DKGRAY);
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
		if (item.getCount() == 0) {
            TLog.d(TAG, "Index {0} not found in list", position);
            title.setText("");
            content.setText("");
			return;
		}
		long noteId = item.getInt(item.getColumnIndexOrThrow(Note.ID));	
		uri = Uri.parse(CONTENT_URI + "/" + noteId);

        note = NoteManager.getNote(this, uri);

        if(note != null) {
			title.setText((CharSequence) note.getTitle());
            noteContent = note.getNoteContent(noteContentHandler);
            showNote();
        } else {
            TLog.d(TAG, "The note {0} doesn't exist", uri);
            showNoteNotFoundDialog(uri);
	        }
	}
	private void showNote() {

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
				showNote();
	
			//parsed not ok - error
			} else if(msg.what == NoteContentBuilder.PARSE_ERROR) {
	
				new AlertDialog.Builder(Tomdroid.this)
					.setMessage(getString(R.string.messageErrorNoteParsing))
					.setTitle(getString(R.string.error))
					.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Create the menu based on what is defined in res/menu/main.xml
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

        	// Calling super after populating the menu is necessary here to ensure that the
       		// action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
        	case android.R.id.home:
        		ViewList(this);
            	return true;
			case R.id.menuAbout:
				showAboutDialog();
				return true;
			case R.id.menuSync:
				this.syncMenuItem = item;
				if(NoteManager.getNewNotes(this).getCount() > 0) {
					new AlertDialog.Builder(this)
			        .setIcon(android.R.drawable.ic_dialog_alert)
			        .setTitle(R.string.push_changes_title)
			        .setMessage(R.string.push_changes_message)
			        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int which) {
							item.setTitle(Tomdroid.this.getString(R.string.syncing));
			            	SyncManager.getInstance().startSynchronization(true);
			            }
			        })
			        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int which) {
							item.setTitle(Tomdroid.this.getString(R.string.syncing));
							SyncManager.getInstance().startSynchronization(false);
			            }
			        })
			        .show();
				}
				else {
					item.setTitle(Tomdroid.this.getString(R.string.syncing));
					SyncManager.getInstance().startSynchronization(false);
				}
				return true;
			case R.id.menuNew:
				newNote();
				return true;

			case R.id.menuPrefs:
				startActivity(new Intent(this, PreferencesActivity.class));
				return true;
				
			case R.id.menuSearch:
				startSearch(null, false, null, false);
				return true;

			// tablet
			case R.id.menuEdit:
				startEditNote();
				return true;
			case R.id.menuDelete:
				deleteNote(note.getDbId());
				return true;
		}
		return super.onOptionsItemSelected(item);
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
			showNoteInPane(lastIndex);
		}
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
		lastIndex = position;
		
		if (rightPane != null) {
			showNoteInPane(position);
		}
		else {
			Cursor item = (Cursor) adapter.getItem(position);
			long noteId = item.getInt(item.getColumnIndexOrThrow(Note.ID));
				this.ViewNote(noteId);
		}
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
		Uri uri = NoteManager.putNote(this, note, false);
		
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
}
