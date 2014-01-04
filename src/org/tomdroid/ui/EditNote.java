/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2013 Stefan Hammer <j.4@gmx.at>
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

import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.sd.SdCardSyncService;
import org.tomdroid.ui.actionbar.ActionBarActivity;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.TLog;
import org.tomdroid.xml.LinkInternalSpan;
import org.tomdroid.xml.LinkifyPhone;
import org.tomdroid.xml.NoteContentBuilder;
import org.tomdroid.xml.NoteContentHandler;
import org.tomdroid.xml.NoteXMLContentBuilder;
import org.xml.sax.InputSource;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

// TODO this class is starting to smell
public class EditNote extends ActionBarActivity {
	
	// UI elements
	private EditText title;
	private EditText content;
	private SlidingDrawer formatBar;
	
	// Model objects
	private Note note;
	private SpannableStringBuilder noteContent;
	
	// Logging info
	private static final String TAG = "EditNote";
	
	private Uri uri;
	public static final String CALLED_FROM_SHORTCUT_EXTRA = "org.tomdroid.CALLED_FROM_SHORTCUT";
    public static final String SHORTCUT_NAME = "org.tomdroid.SHORTCUT_NAME";
    
	// rich text variables
	
	int styleStart = -1, cursorLoc = 0;
    private float size = 1.0f;
	private boolean xmlOn = false;
	private int sizeSelectionStart = 0;
	private int sizeSelectionEnd = 0;
	
	// check whether text has been changed yet
	private boolean textChanged = false;
	// discard changes -> not will not be saved
	private boolean discardChanges = false;
	// force close without onDestroy() function when note not existing!
	private boolean forceClose = false;
	// indicates whether the note was ever saved before (to be able to discard brand new notes)
	private boolean neverSaved = true;
	
	// remember if we are writing a BulletSpan at the moment
	private boolean inBulletSpan = false;
	private int listLevel = 1;
	
	// TODO extract methods in here
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Preferences.init(this, Tomdroid.CLEAR_PREFERENCES);
		
		setContentView(R.layout.note_edit);
		
		content = (EditText) findViewById(R.id.content);
		title = (EditText) findViewById(R.id.title);
		
		formatBar = (SlidingDrawer) findViewById(R.id.formatBar);

		content.setOnFocusChangeListener(new OnFocusChangeListener() {

		    public void onFocusChange(View v, boolean hasFocus) {
		    	if(hasFocus && !xmlOn) {
		    		formatBar.setVisibility(View.VISIBLE);
		    	}
		    	else {
		    		formatBar.setVisibility(View.GONE);
		    	}
		    }
		});
		
		neverSaved = getIntent().getBooleanExtra(Tomdroid.IS_NEW_NOTE_EXTRA, false);
		
        uri = getIntent().getData();
	}

	private void handleNoteUri(final Uri uri) {// We were triggered by an Intent URI
        TLog.d(TAG, "EditNote started: Intent-filter triggered.");

        // TODO validate the good action?
        // intent.getAction()

        // TODO verify that getNote is doing the proper validation
        note = NoteManager.getNote(this, uri);

        if(note != null) {
			title.setText((CharSequence) note.getTitle());
            noteContent = note.getNoteContent(noteContentHandler);
        } else {
            TLog.d(TAG, "The note {0} doesn't exist", uri);
            showNoteNotFoundDialog(uri);
        }
    }

	private Handler noteContentHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			//parsed ok - show
			if(msg.what == NoteContentBuilder.PARSE_OK) {
				showNote(false);
				
				// add format bar listeners here
				
				addFormatListeners();

			//parsed not ok - error
			} else if(msg.what == NoteContentBuilder.PARSE_ERROR) {

				new AlertDialog.Builder(EditNote.this)
					.setMessage(getString(R.string.messageErrorNoteParsing))
					.setTitle(getString(R.string.error))
					.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							showNote(true);
						}})
					.show();
        	}
		}
	};
	
    private void showNoteNotFoundDialog(final Uri uri) {
    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.messageNoteNotFound))
                .setTitle(getString(R.string.titleNoteNotFound))
                .setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        forceClose = true;
                        finish();
                    }
                });
        builder.show();
    }
    
    @Override
    protected void onPause() {
    	if (uri != null) {
        	if(!discardChanges && textChanged) // changed and not discarding changes
       			saveNote();
        	else if (discardChanges && neverSaved)
        		NoteManager.deleteNote(this, note);
        		neverSaved = false;
        }
    	super.onPause();
    }

    @Override
    protected void onDestroy() {
    	if(!forceClose) {
    		if(note.getTitle().length() == 0 && note.getXmlContent().length() == 0 && !textChanged) // if the note is empty, e.g. new
				NoteManager.deleteNote(this, note);
    	}
    	super.onDestroy();
    }
    
	@Override
	public void onResume(){
		TLog.v(TAG, "resume edit note");
		super.onResume();

        if (uri == null) {
			TLog.d(TAG, "The Intent's data was null.");
            showNoteNotFoundDialog(uri);
        } else handleNoteUri(uri);

		updateTextAttributes();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_note, menu);

        // Calling super after populating the menu is necessary here to ensure that the
        // action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	        case android.R.id.home:
	        	// app icon in action bar clicked; go home
                Intent intent = new Intent(this, Tomdroid.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            	return true;
			case R.id.menuPrefs:
				startActivity(new Intent(this, PreferencesActivity.class));
				return true;
			case R.id.edit_note_save:
				finish();
				return true;
			case R.id.edit_note_discard:
				discardNoteContent();
				return true;
/*			case R.id.edit_note_xml:
            	if(!xmlOn) {
            		item.setTitle(getString(R.string.text));
            		item.setIcon(R.drawable.text);
            		xmlOn = true;
        			SpannableStringBuilder newNoteContent = (SpannableStringBuilder) content.getText();

        			// store changed note content
        			String newXmlContent = new NoteXMLContentBuilder().setCaller(noteXMLWriteHandler).setInputSource(newNoteContent).build();
        			// Since 0.5 EditNote expects the redundant title being removed from the note content, but we still may need this for debugging:
        			//note.setXmlContent("<note-content version=\"0.1\">"+note.getTitle()+"\n\n"+newXmlContent+"</note-content>");
        			TLog.d(TAG, "new xml content: {0}", newXmlContent);
        			note.setXmlContent(newXmlContent);
            		formatBarShell.setVisibility(View.GONE);
            		content.setText(note.getXmlContent());
            	}
            	else {
            		item.setTitle(getString(R.string.xml));
            		item.setIcon(R.drawable.xml);
            		xmlOn = false;
            		updateNoteContent(true);  // update based on xml that we are switching FROM
            		if(content.isFocused())
            			formatBarShell.setVisibility(View.VISIBLE);
            	}
				return true;*/
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	private void showNote(boolean xml) {
		if(xml) {

			formatBar.setVisibility(View.GONE);
			content.setText(note.getXmlContent());
			title.setText((CharSequence) note.getTitle());
			this.setTitle(this.getTitle() + " - XML");
			xmlOn = true;
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
	
	private Handler noteXMLParseHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			//parsed ok - show
			if(msg.what == NoteContentBuilder.PARSE_OK) {
				showNote(false);

			//parsed not ok - error
			} else if(msg.what == NoteContentBuilder.PARSE_ERROR) {
				new AlertDialog.Builder(EditNote.this)
				.setMessage(getString(R.string.messageErrorParsingXML))
				.setTitle(getString(R.string.titleErrorParsingXML))
				.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							showNote(true);
						}})
					.show();
        	}
		}
	};

	private Handler noteXMLWriteHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			//parsed ok - do nothing
			if(msg.what == NoteXMLContentBuilder.PARSE_OK) {
			//parsed not ok - error
			} else if(msg.what == NoteXMLContentBuilder.PARSE_ERROR) {
				new AlertDialog.Builder(EditNote.this)
					.setMessage(getString(R.string.messageErrorParsingXML))
					.setTitle(getString(R.string.titleErrorParsingXML))
					.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}})
					.show();
        	}
		}
	};

	// custom transform filter that takes the note's title part of the URI and translate it into the note id
	// this was done to avoid problems with invalid characters in URI (ex: ? is the query separator but could be in a note title)
	private TransformFilter noteTitleTransformFilter = new TransformFilter() {

		public String transformUrl(Matcher m, String str) {

			int id = NoteManager.getNoteId(EditNote.this, str);
			
			// return something like content://org.tomdroid.notes/notes/3
			return Tomdroid.CONTENT_URI.toString()+"/"+id;
		}  
	};

	private boolean updateNoteContent(boolean xml) {

		SpannableStringBuilder newNoteContent = new SpannableStringBuilder();
		if(xml) {
			// parse XML
			String xmlContent = "<note-content version=\"1.0\">"+this.content.getText().toString()+"</note-content>";
			String subjectName = this.title.getText().toString();
	        InputSource noteContentIs = new InputSource(new StringReader(xmlContent));
			try {
				// Parsing
		    	// XML 
		    	// Get a SAXParser from the SAXPArserFactory
		        SAXParserFactory spf = SAXParserFactory.newInstance();

		        // trashing the namespaces but keep prefixes (since we don't have the xml header)
		        spf.setFeature("http://xml.org/sax/features/namespaces", false);
		        spf.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		        SAXParser sp = spf.newSAXParser();

		        sp.parse(noteContentIs, new NoteContentHandler(newNoteContent));
			} catch (Exception e) {
				e.printStackTrace();
				// TODO handle error in a more granular way
				TLog.e(TAG, "There was an error parsing the note {0}", subjectName);
				return false;
			}
			
		}
		else
			newNoteContent = (SpannableStringBuilder) this.content.getText();

		// store changed note content
		String newXmlContent = new NoteXMLContentBuilder().setCaller(noteXMLWriteHandler).setInputSource(newNoteContent).build();
		
		// Since 0.5 EditNote expects the redundant title being removed from the note content, but we still may need this for debugging:
		//note.setXmlContent("<note-content version=\"0.1\">"+note.getTitle()+"\n\n"+newXmlContent+"</note-content>");
		note.setXmlContent(newXmlContent);
		noteContent = note.getNoteContent(noteXMLWriteHandler);
		textChanged = true;
		return true;
	}
	
	private void saveNote() {
		TLog.v(TAG, "saving note");
		
		boolean updated = updateNoteContent(xmlOn);
		if(!updated) {
			Toast.makeText(this, getString(R.string.messageErrorParsingXML), Toast.LENGTH_SHORT).show();
			return;
		}
		
		String validTitle = NoteManager.validateNoteTitle(this, title.getText().toString(), note.getGuid()); 
		title.setText(validTitle);
		note.setTitle(validTitle);

		note.setLastChangeDate();
		NoteManager.putNote( this, note);
		if(!SyncManager.getInstance().getCurrentService().needsLocation() && Preferences.getBoolean(Preferences.Key.AUTO_BACKUP_NOTES)) {
			TLog.v(TAG, "backing note up");
			SdCardSyncService.backupNote(note);
		}
		textChanged = false;
		neverSaved = false;

		Toast.makeText(this, getString(R.string.messageNoteSaved), Toast.LENGTH_SHORT).show();
		TLog.v(TAG, "note saved");
		//showNote(false);	// redraw the note, do not show XML
		//finish();
	}

	private void discardNoteContent() {
		new AlertDialog.Builder(EditNote.this)
			.setMessage(getString(R.string.messageDiscardChanges))
			.setTitle(getString(R.string.titleDiscardChanges))
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int which) {
			            	discardChanges = true;
			            	dialog.dismiss();
							finish();
			            }
			        })
			        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
			            }
				})
			.show();
	}
	
	// generic function which does the magic as soon as a style-toggle button is clicked.
	private void toggleButtonOnClick (ToggleButton button, Object span) {
		
		int selectionStart = content.getSelectionStart();
		styleStart = selectionStart;
		int selectionEnd = content.getSelectionEnd();
		
		if (selectionStart > selectionEnd){
			int temp = selectionEnd;
			selectionEnd = selectionStart;
			selectionStart = temp;
		}
		
		if (selectionEnd > selectionStart)
		{
			boolean exists = false;
			
			Spannable str = content.getText();
			if (span instanceof StyleSpan) {
				// get all the spans in a certain range
				StyleSpan[] ss = str.getSpans(selectionStart, selectionEnd, StyleSpan.class);
				// get the style of this span
				int style = ((StyleSpan) span).getStyle();
				// go through all this spans and update them if the are equal
				for (StyleSpan oldSpan : ss) {
					if (oldSpan.getStyle() == style) {
						StyleSpan newSpan = new StyleSpan(style);
						// Actually updates the old span (trim, cut, delete)
						updateOldSpan(oldSpan, newSpan, selectionStart, selectionEnd, str);
						exists = true;
					}
				}
			} else if (span instanceof StrikethroughSpan) {
				StrikethroughSpan[] ss = str.getSpans(selectionStart, selectionEnd, StrikethroughSpan.class);
				for (StrikethroughSpan oldSpan : ss) {
					StrikethroughSpan newSpan = new StrikethroughSpan();
					updateOldSpan(oldSpan, newSpan, selectionStart, selectionEnd, str);
					exists = true;
				}
			} else if (span instanceof BackgroundColorSpan) {
				BackgroundColorSpan[] ss = str.getSpans(selectionStart, selectionEnd, BackgroundColorSpan.class);
				for (BackgroundColorSpan oldSpan : ss) {
					BackgroundColorSpan newSpan = new BackgroundColorSpan(oldSpan.getBackgroundColor());
					updateOldSpan(oldSpan, newSpan, selectionStart, selectionEnd, str);
					exists = true;
				}
			} else if (span instanceof TypefaceSpan) {
				TypefaceSpan[] ss = str.getSpans(selectionStart, selectionEnd, TypefaceSpan.class);
				String family = ((TypefaceSpan) span).getFamily();
				for (TypefaceSpan oldSpan : ss) {
					if (oldSpan.getFamily() == family){
						TypefaceSpan newSpan = new TypefaceSpan(family);
						updateOldSpan(oldSpan, newSpan, selectionStart, selectionEnd, str);
						exists = true;
					}
				}
			}
			
			if (!exists){
				str.setSpan(span , selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			updateNoteContent(xmlOn);
			button.setChecked(false);
		} else
			cursorLoc = selectionStart;
	}
	
	// this function removes, trims or divides the old span according to the cursor positions
	private void updateOldSpan (Object oldSpan, Object newSpan, int selectionStart, int selectionEnd, Spannable str) {
    	   	
		int oldStart = str.getSpanStart(oldSpan);
		int oldEnd = str.getSpanEnd(oldSpan);
		
		if (oldStart < selectionStart && selectionEnd < oldEnd) {
			// old span starts end ends outside selection
			str.setSpan(oldSpan, oldStart, selectionStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    		str.setSpan(newSpan, selectionEnd, oldEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else if (oldStart < selectionStart && oldEnd <= selectionEnd){
			// old span starts outside, ends inside the selection
    		str.setSpan(oldSpan, oldStart, selectionStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else if (selectionStart <= oldStart && selectionEnd < oldEnd){
			// old span starts inside, ends outside the selection
			str.setSpan(oldSpan, selectionEnd, oldEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else if (selectionStart <= oldStart && oldEnd <= selectionEnd) {
			// old span was equal or within the selection -> just delete it and make the new one.
			str.removeSpan(oldSpan);
		}
	}

	private void addFormatListeners()
	{
		
		final ToggleButton boldButton = (ToggleButton)findViewById(R.id.bold);
		
		boldButton.setOnClickListener(new Button.OnClickListener() {

			public void onClick(View v) {
				Object span = new StyleSpan(android.graphics.Typeface.BOLD);
				toggleButtonOnClick (boldButton, span);
            }
		});
		
		final ToggleButton italicButton = (ToggleButton)findViewById(R.id.italic);
		
		italicButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	    
            	Object span = new StyleSpan(android.graphics.Typeface.ITALIC);
				toggleButtonOnClick (italicButton, span);
            }
		});
		
		final ToggleButton strikeoutButton = (ToggleButton) findViewById(R.id.strike);   
		
		strikeoutButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	Object span = new StrikethroughSpan();
				toggleButtonOnClick (strikeoutButton, span);
            }
        });
		
		final ToggleButton highButton = (ToggleButton)findViewById(R.id.highlight);
		
		highButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	            	
            	Object span = new BackgroundColorSpan(Note.NOTE_HIGHLIGHT_COLOR);
				toggleButtonOnClick (highButton, span);
            }
		});
		
		final ToggleButton monoButton = (ToggleButton)findViewById(R.id.mono);
		
		monoButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	Object span = new TypefaceSpan(Note.NOTE_MONOSPACE_TYPEFACE);
				toggleButtonOnClick (monoButton, span);
            }
		});
		
		final Button sizeButton = (Button)findViewById(R.id.size);
		
		sizeButton.setOnClickListener(new Button.OnClickListener() {

			public void onClick(View v) {

				sizeSelectionStart = content.getSelectionStart();
				styleStart = sizeSelectionStart;
				sizeSelectionEnd = content.getSelectionEnd();
				
				showSizeDialog();
            }
		});
        
        content.addTextChangedListener(new TextWatcher() { 
            public void afterTextChanged(Editable s) {
            	
                // set text as changed to force auto save if preferred
            	textChanged = true;
 
            	//add style as the user types if a toggle button is enabled
            	int position = Selection.getSelectionStart(content.getText());
            	
        		if (position < 0){
        			position = 0;
        		}
            	
        		if (position > 0){
        			
        			if (styleStart > position || position > (cursorLoc + 1)){
						//user changed cursor location, reset
						if (position - cursorLoc > 1){
							//user pasted text
							styleStart = cursorLoc;
						}
						else{
							styleStart = position - 1;
						}
						
					}
        			
        			// check if we are expanding a Bullet span at the end of a line and set the boolean variable accordingly
        			if (!inBulletSpan && !(s.subSequence(position-1, position).toString().equals("\n"))) {
	        			BulletSpan[] bulletSpans = s.getSpans(position, position, BulletSpan.class);
	        			if (bulletSpans.length > 0) {
	        				int bulletEnd = s.getSpanEnd(bulletSpans[0]);
	        				if (bulletEnd == position) {
	        					int bulletStart = s.getSpanStart(bulletSpans[0]);
	        					styleStart = bulletStart;
	        					inBulletSpan = true;
	        				}
	        			}
        			}
        			
                	if (boldButton.isChecked()){  
                		StyleSpan[] ss = s.getSpans(styleStart, position, StyleSpan.class);

                		for (int i = 0; i < ss.length; i++) {
            				s.removeSpan(ss[i]);
                        }
                		s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                	}
                	if (italicButton.isChecked()){
                		StyleSpan[] ss = s.getSpans(styleStart, position, StyleSpan.class);
                		
                		for (int i = 0; i < ss.length; i++) {
                    		if (ss[i].getStyle() == android.graphics.Typeface.ITALIC){
                    			s.removeSpan(ss[i]);
                    		}
                        }
                		s.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                	}
                	if (strikeoutButton.isChecked()){
                		StrikethroughSpan[] ss = s.getSpans(styleStart, position, StrikethroughSpan.class);
                		
                		for (int i = 0; i < ss.length; i++) {
            				s.removeSpan(ss[i]);
                        }
            			s.setSpan(new StrikethroughSpan(), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                	}
                	if (highButton.isChecked()){
                		BackgroundColorSpan[] ss = s.getSpans(styleStart, position, BackgroundColorSpan.class);
                		
                		for (int i = 0; i < ss.length; i++) {
            				s.removeSpan(ss[i]);
                        }
            			s.setSpan(new BackgroundColorSpan(Note.NOTE_HIGHLIGHT_COLOR), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                	}
                	if (monoButton.isChecked()){
                		TypefaceSpan[] ss = s.getSpans(styleStart, position, TypefaceSpan.class);
                		
                		for (int i = 0; i < ss.length; i++) {
                			if (ss[i].getFamily()==Note.NOTE_MONOSPACE_TYPEFACE){
                				s.removeSpan(ss[i]);
                			}
                        }
            			s.setSpan(new TypefaceSpan(Note.NOTE_MONOSPACE_TYPEFACE), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                	}
                	if (size != 1.0f){
                		RelativeSizeSpan[] ss = s.getSpans(styleStart, position, RelativeSizeSpan.class);
                		
                		for (int i = 0; i < ss.length; i++) {
                			s.removeSpan(ss[i]);
                		}
                		s.setSpan(new RelativeSizeSpan(size), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                	}
                	if (inBulletSpan) {
                		LeadingMarginSpan[] ms = s.getSpans(styleStart, position, LeadingMarginSpan.class);
                		for (int i = 0; i < ms.length; i++) {
                			if( ms[i] instanceof LeadingMarginSpan.Standard ) {
                				listLevel = ms[i].getLeadingMargin(true) / Note.NOTE_BULLET_INTENT_FACTOR;
                			}
                			s.removeSpan(ms[i]);
                		}
                		
                		s.setSpan(new LeadingMarginSpan.Standard(Note.NOTE_BULLET_INTENT_FACTOR*listLevel), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                		s.setSpan(new BulletSpan(Integer.valueOf(6)), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                	}
        		}
        		
        		// Recognise a \n to end a bullet span and start a new one
        		if (position > 0 && inBulletSpan && content.getText().subSequence(position-1, position).toString().equals("\n")) {
        			inBulletSpan = false;
        		}

        		// Recognise "\n * " pattern and create bullet from it
        		if (((position == 3) && (content.getText().subSequence(position-3, position-1).toString().equals("* "))) ||
        		((position > 3) && (content.getText().subSequence(position-4, position-1).toString().equals("\n* ")))) {
    				s.replace(position-3, position-1, "");
    				position = Selection.getSelectionStart(content.getText());
    				styleStart = position-1;
    				s.setSpan(new LeadingMarginSpan.Standard(Note.NOTE_BULLET_INTENT_FACTOR*listLevel), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		s.setSpan(new BulletSpan(Integer.valueOf(6)), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    				inBulletSpan = true;
    			}
        		
        		cursorLoc = Selection.getSelectionStart(content.getText());
            } 
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { 
            	//unused
            } 
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            	//unused
            } 
        });

        // set text as changed to force auto save if preferred
        
        title.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            	textChanged = true;
            } 
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { 
                    //unused
            } 
            public void onTextChanged(CharSequence s, int start, int before, int count) { 
                    //unused
            } 
        });
	}
	
	private void showSizeDialog() {
		final CharSequence[] items = {getString(R.string.small), getString(R.string.normal), getString(R.string.large), getString(R.string.huge)};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.messageSelectSize);
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();	
		        switch (item) {
	        		case 0: size = 0.8f; break;
	        		case 1: size = 1.0f; break;
	        		case 2: size = 1.5f; break;
	        		case 3: size = 1.8f; break;
				}
		        // change the size
		        changeTextSize();
                dialog.dismiss();
		    }
		});
		builder.show();
	}
	
	private void changeTextSize() {
		
		if (sizeSelectionStart > sizeSelectionEnd) {
			int temp = sizeSelectionEnd;
			sizeSelectionEnd = sizeSelectionStart;
			sizeSelectionStart = temp;
		}
		
    	if (sizeSelectionStart < sizeSelectionEnd) {
        	Spannable str = content.getText();
        	
        	RelativeSizeSpan[] ss = str.getSpans(sizeSelectionStart, sizeSelectionEnd, RelativeSizeSpan.class);
    		for (RelativeSizeSpan oldSpan : ss) {
    			float oldSize = oldSpan.getSizeChange();
    			RelativeSizeSpan newSpan = new RelativeSizeSpan(oldSize);
    			// update the old span
    			updateOldSpan(oldSpan, newSpan, sizeSelectionStart, sizeSelectionEnd, str);
            }
    		
    		// generate the new span in the selected range
        	if(size != 1.0f) {
        		str.setSpan(new RelativeSizeSpan(size), sizeSelectionStart, sizeSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        	}
        	
			updateNoteContent(xmlOn);
			size = 1.0f;
			
    	} else
			cursorLoc = sizeSelectionStart;
	}
}
