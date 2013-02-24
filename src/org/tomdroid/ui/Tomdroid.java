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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncService;
import org.tomdroid.util.ErrorList;
import org.tomdroid.ui.actionbar.ActionBarListActivity;
import org.tomdroid.util.FirstNote;
import org.tomdroid.util.Honeycomb;
import org.tomdroid.util.LinkifyPhone;
import org.tomdroid.util.NewNote;
import org.tomdroid.util.NoteContentBuilder;
import org.tomdroid.util.NoteViewShortcutsHelper;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.Receive;
import org.tomdroid.util.SearchSuggestionProvider;
import org.tomdroid.util.Send;
import org.tomdroid.util.TLog;
import org.tomdroid.xml.LinkInternalSpan;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.SearchRecentSuggestions;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Tomdroid extends ActionBarListActivity {

	// Global definition for Tomdroid
	public static final String	AUTHORITY			= "org.tomdroid.notes";
	public static final Uri		CONTENT_URI			= Uri.parse("content://" + AUTHORITY + "/notes");
	public static final String	CONTENT_TYPE		= "vnd.android.cursor.dir/vnd.tomdroid.note";
	public static final String	CONTENT_ITEM_TYPE	= "vnd.android.cursor.item/vnd.tomdroid.note";
	public static final String	PROJECT_HOMEPAGE	= "http://www.launchpad.net/tomdroid/";
	public static final String CALLED_FROM_SHORTCUT_EXTRA = "org.tomdroid.CALLED_FROM_SHORTCUT";
    public static final String SHORTCUT_NAME = "org.tomdroid.SHORTCUT_NAME";
	
    private static final int DIALOG_SYNC = 0;
	private static final int DIALOG_ABOUT = 1;
	private static final int DIALOG_FIRST_RUN = 2;
	private static final int DIALOG_NOT_FOUND = 3;
	public static final int DIALOG_PARSE_ERROR = 4;
	private static final int DIALOG_REVERT_ALL = 5;
	private static final int DIALOG_AUTH_PROGRESS = 6;
	private static final int DIALOG_CONNECT_FAILED = 7;
	private static final int DIALOG_DELETE_NOTE = 8;
	private static final int DIALOG_REVERT_NOTE = 9;
	private static final int DIALOG_SYNC_ERRORS = 10;
	static final int DIALOG_SEND_CHOOSE = 11;
	private static final int DIALOG_VIEW_TAGS = 12;
	private static final int DIALOG_NOT_FOUND_SHORTCUT = 13;

	private static String dialogString;
	private static Note dialogNote;
	private static boolean dialogBoolean;
	private static int dialogInt;
	private static int dialogInt2;
	private EditText dialogInput;
	private int dialogPosition;

	public int syncTotalNotes;
	public int syncProcessedNotes;

	// config parameters
	public static String	NOTES_PATH				= null;
	
	// Set this to false for release builds, the reason should be obvious
	public static final boolean	CLEAR_PREFERENCES	= false;

	// Logging info
	private static final String	TAG					= "Tomdroid";

	public static Uri getNoteIntentUri(long noteId) {
        return Uri.parse(CONTENT_URI + "/" + noteId);
    }

	private View main;
	
	// UI to data model glue
	private TextView			listEmptyView;
	private ListAdapter			adapter;

	// UI feedback handler
	private Handler	 syncMessageHandler	= new SyncMessageHandler(this);

	// sync variables
	private boolean creating = true;
	private static ProgressDialog authProgressDialog;
	
	// UI for tablet
	private LinearLayout rightPane;
	private TextView content;
	private TextView title;
	
	// other tablet-based variables

	private Note note;
	private SpannableStringBuilder noteContent;
	private Uri uri;
	private int lastIndex = -1;
	public MenuItem syncMenuItem;
	public static Tomdroid context;

	// for searches
	
	private Intent intent;
	private String query;
	
	/** Called when the activity is created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Preferences.init(this, CLEAR_PREFERENCES);
		context = this;
		SyncManager.setActivity(this);
		SyncManager.setHandler(this.syncMessageHandler);
		
        main =  View.inflate(this, R.layout.main, null);
		
        setContentView(main);
		
		// get the Path to the notes-folder from Preferences
		NOTES_PATH = Environment.getExternalStorageDirectory()
				+ "/" + Preferences.getString(Preferences.Key.SD_LOCATION) + "/";
		
		// did we already show the warning and got destroyed by android's activity killer?
		if (Preferences.getBoolean(Preferences.Key.FIRST_RUN)) {
			TLog.i(TAG, "Tomdroid is first run.");
			
			// add a first explanatory note
			NoteManager.putNote(this, FirstNote.createFirstNote(this));
			
			// Warn that this is a "will eat your babies" release
			showDialog(DIALOG_FIRST_RUN);

		}
		
		this.intent = getIntent();

	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	    	this.setTitle(getString(R.string.app_name) + " - " + getString(R.string.SearchResultTitle));
	    	query = intent.getStringExtra(SearchManager.QUERY);
	    	
	    	//adds query to search history suggestions
	        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
	                SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
	        suggestions.saveRecentQuery(query, null);
		}
	    
		String defaultSortOrder = Preferences.getString(Preferences.Key.SORT_ORDER);
		NoteManager.setSortOrder(defaultSortOrder);
		
	    // set list adapter
	    updateNotesList(query, -1);
	    
		// add note to pane for tablet
		rightPane = (LinearLayout) findViewById(R.id.right_pane);
		registerForContextMenu(findViewById(android.R.id.list));

		// check if receiving note
		if(getIntent().hasExtra("view_note")) {
			uri = getIntent().getData();
			getIntent().setData(null);
			Intent i = new Intent(Intent.ACTION_VIEW, uri, this, ViewNote.class);
			startActivity(i);
		}
		
		if(rightPane != null) {
			content = (TextView) findViewById(R.id.content);
			title = (TextView) findViewById(R.id.title);
			
			// this we will call on resume as well.
			updateTextAttributes();
			showNoteInPane(0);
		}
		
		// set the view shown when the list is empty
		updateEmptyList(query);
	}

	@TargetApi(11)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Create the menu based on what is defined in res/menu/main.xml
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

    	String sortOrder = NoteManager.getSortOrder();
		if(sortOrder == null) {
			menu.findItem(R.id.menuSort).setTitle(R.string.sortByTitle);
		} else if(sortOrder.equals("sort_title")) {
			menu.findItem(R.id.menuSort).setTitle(R.string.sortByDate);
		} else {
			menu.findItem(R.id.menuSort).setTitle(R.string.sortByTitle);
		}

        // Calling super after populating the menu is necessary here to ensure that the
       	// action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
		
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
        	case android.R.id.home:
        		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
        			// app icon in action bar clicked in search results; go home
        			Intent intent = new Intent(this, Tomdroid.class);
        			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        			startActivity(intent);
        		}
        		return true;
			case R.id.menuAbout:
				showDialog(DIALOG_ABOUT);
				return true;
			case R.id.menuSync:
				startSyncing(true);
				return true;
			case R.id.menuNew:
				newNote();
				return true;
			case R.id.menuSort:
				String sortOrder = NoteManager.toggleSortOrder();
				if(sortOrder.equals("sort_title")) {
					item.setTitle(R.string.sortByDate);
				} else {
					item.setTitle(R.string.sortByTitle);
				}
				updateNotesList(query, lastIndex);
				return true;
			case R.id.menuRevert:
				showDialog(DIALOG_REVERT_ALL);
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
				if(note != null) {
			    	dialogString = note.getGuid(); // why can't we put it in the bundle?  deletes the wrong note!?
					dialogInt = lastIndex;
					showDialog(DIALOG_DELETE_NOTE);
				}
				return true;
			case R.id.menuImport:
				// Create a new Intent for the file picker activity
				Intent intent = new Intent(this, FilePickerActivity.class);
				
				// Set the initial directory to be the sdcard
				//intent.putExtra(FilePickerActivity.EXTRA_FILE_PATH, Environment.getExternalStorageDirectory());
				
				// Show hidden files
				//intent.putExtra(FilePickerActivity.EXTRA_SHOW_HIDDEN_FILES, true);
				
				// Only make .png files visible
				//ArrayList<String> extensions = new ArrayList<String>();
				//extensions.add(".png");
				//intent.putExtra(FilePickerActivity.EXTRA_ACCEPTED_FILE_EXTENSIONS, extensions);
				
				// Start the activity
				startActivityForResult(intent, 5718);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();

		long noteId = ((AdapterContextMenuInfo)menuInfo).id;
		dialogPosition = ((AdapterContextMenuInfo)menuInfo).position;

		Uri intentUri = Uri.parse(Tomdroid.CONTENT_URI+"/"+noteId);
        dialogNote = NoteManager.getNote(this, intentUri);
        
        if(dialogNote.getTags().contains("system:deleted"))
        	inflater.inflate(R.menu.main_longclick_deleted, menu);
        else
        	inflater.inflate(R.menu.main_longclick, menu);
        
	    menu.setHeaderTitle(getString(R.string.noteOptions));
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		long noteId = info.id;

		Uri intentUri = Uri.parse(Tomdroid.CONTENT_URI+"/"+noteId);

        switch (item.getItemId()) {
            case R.id.menu_send:
            	dialogString = intentUri.toString();
            	showDialog(DIALOG_SEND_CHOOSE);
				return true;
			case R.id.view:
				this.ViewNote(noteId);
				break;
			case R.id.edit:
				this.startEditNote(noteId);
				break;
			case R.id.tags:
				showDialog(DIALOG_VIEW_TAGS);
				break;
			case R.id.revert:
				this.revertNote(dialogNote.getGuid());
				break;
			case R.id.delete:
				dialogString = dialogNote.getGuid();
				dialogInt = dialogPosition;
				showDialog(DIALOG_DELETE_NOTE);
				return true;
			case R.id.undelete:
				undeleteNote(dialogNote);
				return true;
			case R.id.create_shortcut:
                final NoteViewShortcutsHelper helper = new NoteViewShortcutsHelper(this);
                sendBroadcast(helper.getBroadcastableCreateShortcutIntent(intentUri, dialogNote.getTitle()));
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
				
				showDialog(DIALOG_AUTH_PROGRESS);

				Handler handler = new Handler() {

					@Override
					public void handleMessage(Message msg) {
						if(authProgressDialog != null)
							authProgressDialog.dismiss();
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
		else 
			updateNotesList(query, lastIndex);
		
		// set the view shown when the list is empty
		updateEmptyList(query);
		creating = false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
	    super.onCreateDialog (id);
	    final Activity activity = this;
		AlertDialog alertDialog;
		final ProgressDialog progressDialog = new ProgressDialog(this);
		SyncService currentService = SyncManager.getInstance().getCurrentService();
		String serviceDescription = currentService.getDescription();
    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		switch(id) {
		    case DIALOG_SYNC:
				progressDialog.setIndeterminate(true);
				progressDialog.setTitle(String.format(getString(R.string.syncing),serviceDescription));
				progressDialog.setMessage(dialogString);
				progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
	    			
					public void onCancel(DialogInterface dialog) {
						SyncManager.getInstance().cancel();
					}
					
				});
				progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						progressDialog.cancel();
					}
				});
		    	return progressDialog;
		    case DIALOG_AUTH_PROGRESS:
		    	authProgressDialog = new ProgressDialog(this);
		    	authProgressDialog.setTitle("");
		    	authProgressDialog.setMessage(getString(R.string.prefSyncCompleteAuth));
		    	authProgressDialog.setIndeterminate(true);
		    	authProgressDialog.setCancelable(false);
		        return authProgressDialog;
		    case DIALOG_ABOUT:
				// grab version info
				String ver;
				try {
					ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
				} catch (NameNotFoundException e) {
					e.printStackTrace();
					ver = "Not found!";
					return null;
				}
		    	
		    	// format the string
				String aboutDialogFormat = getString(R.string.strAbout);
				String aboutDialogStr = String.format(aboutDialogFormat, getString(R.string.app_desc), // App description
						getString(R.string.author), // Author name
						ver // Version
						);

				// build and show the dialog
				return new AlertDialog.Builder(this).setMessage(aboutDialogStr).setTitle(getString(R.string.titleAbout))
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
						}).create();
		    case DIALOG_FIRST_RUN:
		    	return new AlertDialog.Builder(this).setMessage(getString(R.string.strWelcome)).setTitle(
						getString(R.string.titleWelcome)).setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Preferences.putBoolean(Preferences.Key.FIRST_RUN, false);
						dialog.dismiss();
					}
				}).setIcon(R.drawable.icon).create();
		    case DIALOG_NOT_FOUND:
			    addCommonNoteNotFoundDialogElements(builder);
			    return builder.create();
		    case DIALOG_NOT_FOUND_SHORTCUT:
			    addCommonNoteNotFoundDialogElements(builder);
		        final Intent removeIntent = new NoteViewShortcutsHelper(this).getRemoveShortcutIntent(dialogString, uri);
		        builder.setPositiveButton(getString(R.string.btnRemoveShortcut), new OnClickListener() {
		            public void onClick(final DialogInterface dialogInterface, final int i) {
		                sendBroadcast(removeIntent);
		                finish();
		            }
		        });
			    return builder.create();
		    case DIALOG_PARSE_ERROR:
		    	return new AlertDialog.Builder(this)
				.setMessage(getString(R.string.messageErrorNoteParsing))
				.setTitle(getString(R.string.error))
				.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						showNote(true);
					}})
				.create();
		    case DIALOG_REVERT_ALL:
		    	return new AlertDialog.Builder(this)
		        .setIcon(android.R.drawable.ic_dialog_alert)
		        .setTitle(R.string.revert_notes)
		        .setMessage(R.string.revert_notes_message)
		    	.setPositiveButton(getString(R.string.yes), new OnClickListener() {

		            public void onClick(DialogInterface dialog, int which) {
		        		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, (Long)Preferences.Key.LATEST_SYNC_REVISION.getDefault());
		        		Preferences.putString(Preferences.Key.LATEST_SYNC_DATE, new Time().format3339(false));
		            	startSyncing(false);
		           }

		        })
		        .setNegativeButton(R.string.no, null)
		        .create();
		    case DIALOG_CONNECT_FAILED:
		    	return new AlertDialog.Builder(this)
				.setMessage(getString(R.string.prefSyncConnectionFailed))
				.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}})
				.create();
		    case DIALOG_DELETE_NOTE:
		    	return new AlertDialog.Builder(this)
		        .setIcon(android.R.drawable.ic_dialog_alert)
		        .setTitle(R.string.delete_note)
		        .setMessage(R.string.delete_message)
		        .setPositiveButton(getString(R.string.yes), null)
		        .setNegativeButton(R.string.no, null)
		        .create();
		    case DIALOG_REVERT_NOTE:
		    	return new AlertDialog.Builder(this)
		        .setIcon(android.R.drawable.ic_dialog_alert)
		        .setTitle(R.string.revert_note)
		        .setMessage(R.string.revert_note_message)
		        .setPositiveButton(getString(R.string.yes), null)
		        .setNegativeButton(R.string.no, null)
		        .create();
		    case DIALOG_SYNC_ERRORS:
		    	return new AlertDialog.Builder(activity)
				.setTitle(getString(R.string.error))
		    	.setMessage(dialogString)
		        .setPositiveButton(getString(R.string.yes), null)
				.setNegativeButton(getString(R.string.close), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { finishSync(); }
				}).create();
		    case DIALOG_SEND_CHOOSE:
                final Uri intentUri = Uri.parse(dialogString);
                return new AlertDialog.Builder(activity)
				.setMessage(getString(R.string.sendChoice))
				.setTitle(getString(R.string.sendChoiceTitle))
		        .setPositiveButton(getString(R.string.btnSendAsFile), null)
				.setNegativeButton(getString(R.string.btnSendAsText), null)
				.create();
		    case DIALOG_VIEW_TAGS:
		    	dialogInput = new EditText(this);
		    	return new AlertDialog.Builder(activity)
		    	.setMessage(getString(R.string.edit_tags))
		    	.setTitle(String.format(getString(R.string.note_x_tags),dialogNote.getTitle()))
		    	.setView(dialogInput)
		    	.setNegativeButton(R.string.btnCancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						removeDialog(DIALOG_VIEW_TAGS);
					}
		    	})
		    	.setPositiveButton(R.string.btnOk, null)
		    	.create();
		    default:
		    	alertDialog = null;
		    }
		return alertDialog;
	}

	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
	    super.onPrepareDialog (id, dialog);
	    final Activity activity = this;
	    switch(id) {
	    	case DIALOG_SYNC:
				SyncService currentService = SyncManager.getInstance().getCurrentService();
				String serviceDescription = currentService.getDescription();
	    		((ProgressDialog) dialog).setTitle(String.format(getString(R.string.syncing),serviceDescription));
	    		((ProgressDialog) dialog).setMessage(dialogString);
	    		((ProgressDialog) dialog).setOnCancelListener(new DialogInterface.OnCancelListener() {
	    			
					public void onCancel(DialogInterface dialog) {
						SyncManager.getInstance().cancel();
					}
					
				});
	    		break;
		    case DIALOG_NOT_FOUND_SHORTCUT:
		        final Intent removeIntent = new NoteViewShortcutsHelper(this).getRemoveShortcutIntent(dialogString, uri);
		        ((AlertDialog) dialog).setButton(Dialog.BUTTON_POSITIVE, getString(R.string.btnRemoveShortcut), new OnClickListener() {
		            public void onClick(final DialogInterface dialogInterface, final int i) {
		                sendBroadcast(removeIntent);
		                finish();
		            }
		        });
		        break;
		    case DIALOG_REVERT_ALL:
		    	((AlertDialog) dialog).setButton(Dialog.BUTTON_POSITIVE, getString(R.string.yes), new OnClickListener() {

		            public void onClick(DialogInterface dialog, int which) {
		            	Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, (Long)Preferences.Key.LATEST_SYNC_REVISION.getDefault());
		        		Preferences.putString(Preferences.Key.LATEST_SYNC_DATE, new Time().format3339(false));
		            	startSyncing(false);
		           }

		        });
			    break;
		    case DIALOG_DELETE_NOTE:
		    	((AlertDialog) dialog).setButton(Dialog.BUTTON_POSITIVE, getString(R.string.yes), new OnClickListener() {

		            public void onClick(DialogInterface dialog, int which) {
		        		deleteNote(dialogString, dialogInt);
		           }

		        });
			    break;
		    case DIALOG_REVERT_NOTE:
		    	((AlertDialog) dialog).setButton(Dialog.BUTTON_POSITIVE, getString(R.string.yes), new OnClickListener() {

		            public void onClick(DialogInterface dialog, int which) {
						SyncManager.getInstance().pullNote(dialogString);
		           }

		        });
			    break;
		    case DIALOG_SYNC_ERRORS:
		    	((AlertDialog) dialog).setMessage(dialogString);
		    	((AlertDialog) dialog).setButton(Dialog.BUTTON_POSITIVE, getString(R.string.btnSavetoSD), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if(!dialogBoolean) {
							Toast.makeText(activity, activity.getString(R.string.messageCouldNotSave),
									Toast.LENGTH_SHORT).show();
						}
						finishSync();
					}
				});
			    break;
		    case DIALOG_SEND_CHOOSE:
                final Uri intentUri = Uri.parse(dialogString);
		    	((AlertDialog) dialog).setButton(Dialog.BUTTON_POSITIVE, getString(R.string.btnSendAsFile), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						(new Send(activity, intentUri, true)).send();

					}
				});
		    	((AlertDialog) dialog).setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.btnSendAsText), new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
		                (new Send(activity, intentUri, false)).send();
					}
				});
			    break;
		    case DIALOG_VIEW_TAGS:
		    	((AlertDialog) dialog).setTitle(String.format(getString(R.string.note_x_tags),dialogNote.getTitle()));
		    	dialogInput.setText(dialogNote.getTags());

		    	((AlertDialog) dialog).setButton(Dialog.BUTTON_POSITIVE, getString(R.string.btnOk), new DialogInterface.OnClickListener() {
		    		public void onClick(DialogInterface dialog, int whichButton) {
		    			String value = dialogInput.getText().toString();
			    		dialogNote.setTags(value);
			    		dialogNote.setLastChangeDate();
						NoteManager.putNote(activity, dialogNote);
						removeDialog(DIALOG_VIEW_TAGS);
		    		}
		    	});
		    	break;
		}
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
	}
	
	// called when rotating screen
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
	    super.onConfigurationChanged(newConfig);
        main =  View.inflate(this, R.layout.main, null);
        setContentView(main);

        if (Integer.parseInt(Build.VERSION.SDK) >= 11) {
            Honeycomb.invalidateOptionsMenuWrapper(this); 
        }
		
		registerForContextMenu(findViewById(android.R.id.list));

		// add note to pane for tablet
		rightPane = (LinearLayout) findViewById(R.id.right_pane);
		
		if(rightPane != null) {
			content = (TextView) findViewById(R.id.content);
			title = (TextView) findViewById(R.id.title);
			updateTextAttributes();
			showNoteInPane(lastIndex);
		}
		else
			updateNotesList(query,-1);
		
		// set the view shown when the list is empty
		updateEmptyList(query);
	}

	private void updateNotesList(String aquery, int aposition) {
	    // adapter that binds the ListView UI to the notes in the note manager
		adapter = NoteManager.getListAdapter(this, aquery, rightPane != null ? aposition : -1);
		setListAdapter(adapter);
	}
	
	private void updateEmptyList(String aquery) {
		// set the view shown when the list is empty
		listEmptyView = (TextView) findViewById(R.id.list_empty);
		if (rightPane == null) {
			if (aquery != null) {
				listEmptyView.setText(getString(R.string.strNoResults, aquery)); }
			else if (adapter.getCount() != 0) {
				listEmptyView.setText(getString(R.string.strListEmptyWaiting)); }
			else {
				listEmptyView.setText(getString(R.string.strListEmptyNoNotes)); }
		} else {
			if (aquery != null) {
				title.setText(getString(R.string.strNoResults, aquery)); }
			else if (adapter.getCount() != 0) {
				title.setText(getString(R.string.strListEmptyWaiting)); }
			else {
				title.setText(getString(R.string.strListEmptyNoNotes)); }
		}
		getListView().setEmptyView(listEmptyView);
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
		
		if(position == -1)
			position = 0;

        title.setText("");
        content.setText("");
		
     // save index and top position

        int index = getListView().getFirstVisiblePosition();
        View v = getListView().getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();

        updateNotesList(query, position);

    // restore
	
		getListView().setSelectionFromTop(index, top);
		
		if(position >= adapter.getCount())
			position = 0;
		
		Cursor item = (Cursor) adapter.getItem(position);
		if (item == null || item.getCount() == 0) {
            TLog.d(TAG, "Index {0} not found in list", position);
			return;
		}
		TLog.d(TAG, "Getting note {0}", position);

		long noteId = item.getInt(item.getColumnIndexOrThrow(Note.ID));	
		uri = Uri.parse(CONTENT_URI + "/" + noteId);

        note = NoteManager.getNote(this, uri);
		TLog.v(TAG, "Note guid: {0}", note.getGuid());

        if(note != null) {
        	TLog.d(TAG, "note {0} found", position);
            noteContent = new NoteContentBuilder().setCaller(noteContentHandler).setInputSource(note.getXmlContent()).setTitle(note.getTitle()).build();
    		lastIndex = position;
        } else {
            TLog.d(TAG, "The note {0} doesn't exist", uri);
		    final boolean proposeShortcutRemoval;
		    final boolean calledFromShortcut = getIntent().getBooleanExtra(CALLED_FROM_SHORTCUT_EXTRA, false);
		    final String shortcutName = getIntent().getStringExtra(SHORTCUT_NAME);
		    proposeShortcutRemoval = calledFromShortcut && uri != null && shortcutName != null;
		
		    if (proposeShortcutRemoval) {
		    	dialogString = shortcutName;
	            showDialog(DIALOG_NOT_FOUND_SHORTCUT);
		    }
		    else
	            showDialog(DIALOG_NOT_FOUND);

        }
	}
	private void showNote(boolean xml) {
		
		if(xml) {
			content.setText(note.getXmlContent());
			title.setText((CharSequence) note.getTitle());
			this.setTitle(this.getTitle() + " - XML");
			return;
		}

		LinkInternalSpan[] links = noteContent.getSpans(0, noteContent.length(), LinkInternalSpan.class);
		MatchFilter noteLinkMatchFilter = LinkInternalSpan.getNoteLinkMatchFilter(noteContent, links);

		// show the note (spannable makes the TextView able to output styled text)
		content.setText(noteContent, TextView.BufferType.SPANNABLE);

		// add links to stuff that is understood by Android except phone numbers because it's too aggressive
		// TODO this is SLOWWWW!!!!
		
		int linkFlags = 0;
		
		if(Preferences.getBoolean(Preferences.Key.LINK_EMAILS))
			linkFlags |= Linkify.EMAIL_ADDRESSES;
		if(Preferences.getBoolean(Preferences.Key.LINK_URLS))
			linkFlags |= Linkify.WEB_URLS;
		if(Preferences.getBoolean(Preferences.Key.LINK_ADDRESSES))
			linkFlags |= Linkify.MAP_ADDRESSES;
		
		Linkify.addLinks(content, linkFlags);

		// Custom phone number linkifier (fixes lp:512204)
		if(Preferences.getBoolean(Preferences.Key.LINK_PHONES))
			Linkify.addLinks(content, LinkifyPhone.PHONE_PATTERN, "tel:", LinkifyPhone.sPhoneNumberMatchFilter, Linkify.sPhoneNumberTransformFilter);

		// This will create a link every time a note title is found in the text.
		// The pattern contains a very dumb (title1)|(title2) escaped correctly
		// Then we transform the url from the note name to the note id to avoid characters that mess up with the URI (ex: ?)
		if(Preferences.getBoolean(Preferences.Key.LINK_TITLES)) {
			Pattern pattern = NoteManager.buildNoteLinkifyPattern(this, note.getTitle());
	
			if(pattern != null) {
				Linkify.addLinks(
					content,
					pattern,
					Tomdroid.CONTENT_URI+"/",
					noteLinkMatchFilter,
					noteTitleTransformFilter
				);
	
				// content.setMovementMethod(LinkMovementMethod.getInstance());
			}
		}
		title.setText((CharSequence) note.getTitle());
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
	
				showDialog(DIALOG_PARSE_ERROR);
	    	}
		}
	};

	// custom transform filter that takes the note's title part of the URI and translate it into the note id
	// this was done to avoid problems with invalid characters in URI (ex: ? is the query separator but could be in a note title)
	public TransformFilter noteTitleTransformFilter = new TransformFilter() {
	
		public String transformUrl(Matcher m, String str) {
	
			int id = NoteManager.getNoteId(Tomdroid.this, str);
	
			// return something like content://org.tomdroid.notes/notes/3
			return Tomdroid.CONTENT_URI.toString()+"/"+id;
		}
	};
	
	@SuppressWarnings("deprecation")
	private void startSyncing(boolean push) {

		String serverUri = Preferences.getString(Preferences.Key.SYNC_SERVER);
		SyncService currentService = SyncManager.getInstance().getCurrentService();
		
		if (currentService.needsAuth()) {
	
			// service needs authentication
			TLog.i(TAG, "Creating dialog");

			showDialog(DIALOG_AUTH_PROGRESS);
	
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
	
					if (authProgressDialog != null)
						authProgressDialog.dismiss();
	
					if (wasSuccessful) {
						resetLocalDatabase();
					} else {
						showDialog(DIALOG_CONNECT_FAILED);
					}
				}
			};

			((ServiceAuth) currentService).getAuthUri(serverUri, handler);
		}
		else {
			syncProcessedNotes = 0;
			syncTotalNotes = 0;
			dialogString = getString(R.string.syncing_connect);
	        showDialog(DIALOG_SYNC);
	        SyncManager.getInstance().startSynchronization(push); // push by default
		}
	}
	
	//TODO use LocalStorage wrapper from two-way-sync branch when it get's merged
	private void resetLocalDatabase() {
		getContentResolver().delete(Tomdroid.CONTENT_URI, null, null);
		Preferences.putLong(Preferences.Key.LATEST_SYNC_REVISION, (Long)Preferences.Key.LATEST_SYNC_REVISION.getDefault());
		
		// first explanatory note will be deleted on sync
		//NoteManager.putNote(this, FirstNote.createFirstNote());
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

	public void newNote() {
		
		// add a new note
		
		Note note = NewNote.createNewNote(this, "", "");
		Uri uri = NoteManager.putNote(this, note);
		
		// recreate listAdapter
		
		updateNotesList(query, 0);

		// show new note and update list

		showNoteInPane(0);
		
		// view new note
		
		Intent i = new Intent(Intent.ACTION_VIEW, uri, this, EditNote.class);
		startActivity(i);

		
	}
	private void deleteNote(String guid, int position) {
		NoteManager.deleteNote(this, guid);
		showNoteInPane(position);
	}
	
	private void undeleteNote(Note anote) {
		NoteManager.undeleteNote(this, anote);
		updateNotesList(query,lastIndex);
	}
		
	@SuppressWarnings("deprecation")
	private void revertNote(final String guid) {
		dialogString = guid;
		showDialog(DIALOG_REVERT_NOTE);
	}

	public class SyncMessageHandler extends Handler {
	
		private Activity activity;
		
		public SyncMessageHandler(Activity activity) {
			this.activity = activity;
		}
	
		@Override
		public void handleMessage(Message msg) {
	
			SyncService currentService = SyncManager.getInstance().getCurrentService();
			String serviceDescription = currentService.getDescription();
			String message = "";
			boolean dismiss = false;

			switch (msg.what) {
				case SyncService.AUTH_COMPLETE:
					message = getString(R.string.messageAuthComplete);
					message = String.format(message,serviceDescription);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					break;
				case SyncService.AUTH_FAILED:
					dismiss = true;
					message = getString(R.string.messageAuthFailed);
					message = String.format(message,serviceDescription);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					break;
				case SyncService.PARSING_COMPLETE:
					final ErrorList errors = (ErrorList)msg.obj;
					if(errors == null || errors.isEmpty()) {
						message = getString(R.string.messageSyncComplete);
						message = String.format(message,serviceDescription);
						Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
						finishSync();
					} else {
						TLog.v(TAG, "syncErrors: {0}", TextUtils.join("\n",errors.toArray()));
						dialogString = getString(R.string.messageSyncError);
						dialogBoolean = errors.save();
						showDialog(DIALOG_SYNC_ERRORS);
					}
					break;
				case SyncService.CONNECTING_FAILED:
					dismiss = true;
					message = getString(R.string.messageSyncConnectingFailed);
					message = String.format(message,serviceDescription);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					break;
				case SyncService.PARSING_FAILED:
					dismiss = true;
					message = getString(R.string.messageSyncParseFailed);
					message = String.format(message,serviceDescription);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					break;
				case SyncService.PARSING_NO_NOTES:
					dismiss = true;
					message = getString(R.string.messageSyncNoNote);
					message = String.format(message,serviceDescription);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					break;
					
				case SyncService.NO_INTERNET:
					dismiss = true;
					Toast.makeText(activity, getString(R.string.messageSyncNoConnection),Toast.LENGTH_SHORT).show();
					break;
					
				case SyncService.NO_SD_CARD:
					dismiss = true;
					Toast.makeText(activity, activity.getString(R.string.messageNoSDCard),
							Toast.LENGTH_SHORT).show();
					break;
				case SyncService.SYNC_CONNECTED:
					dialogString = getString(R.string.gettings_notes);
					showDialog(DIALOG_SYNC);
					break;
				case SyncService.BEGIN_PROGRESS:
					syncTotalNotes = msg.arg1;
					syncProcessedNotes = 0;
					dialogString = getString(R.string.syncing_local);
					showDialog(DIALOG_SYNC);
					break;
				case SyncService.SYNC_PROGRESS:
					if(msg.arg1 == 90) {
						dialogString = getString(R.string.syncing_remote);						
						showDialog(DIALOG_SYNC);
					}
					break;
				case SyncService.NOTE_DELETED:
					message = getString(R.string.messageSyncNoteDeleted);
					message = String.format(message,serviceDescription);
					//Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					break;
	
				case SyncService.NOTE_PUSHED:
					message = getString(R.string.messageSyncNotePushed);
					message = String.format(message,serviceDescription);
					//Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();

					break;
				case SyncService.NOTE_PULLED:
					message = getString(R.string.messageSyncNotePulled);
					message = String.format(message,serviceDescription);
					//Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					break;
														
				case SyncService.NOTE_DELETE_ERROR:
					dismiss = true;
					message = getString(R.string.messageSyncNoteDeleteError);
					message = String.format(message,serviceDescription);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					break;
	
				case SyncService.NOTE_PUSH_ERROR:
					dismiss = true;
					message = getString(R.string.messageSyncNotePushError);
					message = String.format(message,serviceDescription);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					break;
				case SyncService.NOTE_PULL_ERROR:
					dismiss = true;
					message = getString(R.string.messageSyncNotePullError);
					message = String.format(message,serviceDescription);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					break;
				case SyncService.IN_PROGRESS:
					Toast.makeText(activity, activity.getString(R.string.messageSyncAlreadyInProgress), Toast.LENGTH_SHORT).show();
					dismiss = true;
					break;
				case SyncService.NOTES_BACKED_UP:
					Toast.makeText(activity, activity.getString(R.string.messageNotesBackedUp), Toast.LENGTH_SHORT).show();
					break;
				case SyncService.SYNC_CANCELLED:
					dismiss = true;
					message = getString(R.string.messageSyncCancelled);
					message = String.format(message,serviceDescription);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
	
			}
			if(dismiss)
				removeDialog(DIALOG_SYNC);
		}
	}

	protected void  onActivityResult (int requestCode, int resultCode, Intent  data) {
		TLog.d(TAG, "onActivityResult called with result {0}", resultCode);
		
		// returning from file picker
		if(data != null && data.hasExtra(FilePickerActivity.EXTRA_FILE_PATH)) {
			// Get the file path
			File f = new File(data.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH));
			Uri noteUri = Uri.fromFile(f);
			Intent intent = new Intent(this, Receive.class);
			intent.setData(noteUri);
			startActivity(intent);
		}
		else { // returning from sync conflict
			SyncService currentService = SyncManager.getInstance().getCurrentService();
			currentService.resolvedConflict(requestCode);			
		}
	}
	
	public void finishSync() {
		TLog.v(TAG, "Finishing Sync");
		
		removeDialog(DIALOG_SYNC);
		
		if(rightPane != null)
			showNoteInPane(lastIndex);
	}
}
