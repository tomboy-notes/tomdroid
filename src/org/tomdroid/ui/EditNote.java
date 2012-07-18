/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2008, 2009, 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
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
import org.tomdroid.util.LinkifyPhone;
import org.tomdroid.util.NoteContentBuilder;
import org.tomdroid.util.Send;
import org.tomdroid.util.NoteXMLContentBuilder;
import org.tomdroid.util.TLog;
import org.tomdroid.xml.NoteContentHandler;
import org.xml.sax.InputSource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.format.Time;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

// TODO this class is starting to smell
public class EditNote extends Activity implements TextSizeDialog.OnSizeChangedListener {
	
	// UI elements
	private EditText title;
	private EditText content;
	private LinearLayout formatBar;
	private ToggleButton xmlButton;
	
	// Model objects
	private Note note;
	private SpannableStringBuilder noteContent;
	
	// Logging info
	private static final String TAG = "EditNote";
	
	// UI feedback handler
	private Handler	syncMessageHandler	= new SyncMessageHandler(this);

	// rich text variables
	
	int styleStart = -1, cursorLoc = 0;
    private int sselectionStart;
	private int sselectionEnd;
	
	// TODO extract methods in here
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.note_edit);
		content = (EditText) findViewById(R.id.content);
		content.setBackgroundColor(0xffffffff);
		content.setTextColor(Color.DKGRAY);
		content.setTextSize(18.0f);
		title = (EditText) findViewById(R.id.title);
		title.setTextColor(Color.DKGRAY);
		title.setBackgroundColor(0xffffffff);
		title.setTextSize(24.0f);

		xmlButton = (ToggleButton) findViewById(R.id.xml);   
		
		final ImageView saveButton = (ImageView) findViewById(R.id.save);
		saveButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				saveNote();
			}
		});
		
		formatBar = (LinearLayout) findViewById(R.id.format_bar);

		content.setOnFocusChangeListener(new OnFocusChangeListener() {

		    public void onFocusChange(View v, boolean hasFocus) {
		    	if(hasFocus && !xmlButton.isChecked()) {
		    		formatBar.setVisibility(View.VISIBLE);
		    	}
		    	else {
		    		formatBar.setVisibility(View.GONE);
		    	}
		    }
		});
		
		// add format bar listeners
		
		addFormatListeners();
		
		final Intent intent = getIntent();
		Uri uri = intent.getData();
		
		if (uri != null) {
			
			// We were triggered by an Intent URI 
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "EditNote started: Intent-filter triggered.");

			// TODO validate the good action?
			// intent.getAction()
			
			// TODO verify that getNote is doing the proper validation
			note = NoteManager.getNote(this, uri);
			
			if(note != null) {
				title.setText((CharSequence) note.getTitle());
				
				noteContent = note.getNoteContent(noteXMLParseHandler);
				//Log.i(TAG, "THE NOTE IS: " + note.getXmlContent().toString());
				
			} else {
				
				if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "The note "+uri+" doesn't exist");
				
				// TODO put error string in a translatable resource
				new AlertDialog.Builder(this)
					.setMessage("The requested note could not be found. If you see this error " +
							    "and you are able to replicate it, please file a bug!")
					.setTitle("Error")
					.setNeutralButton("Ok", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}})
					.show();
			}
		} else {
			
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "The Intent's data was null.");
			
			// TODO put error string in a translatable resource
			new AlertDialog.Builder(this)
			.setMessage("The requested note could not be found. If you see this error " +
					    " and you are able to replicate it, please file a bug!")
			.setTitle(getString(R.string.error))
			.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					finish();
				}})
			.show();
		}
	}
	
	@Override
	public void onResume(){
		super.onResume();
		SyncManager.setActivity(this);
		SyncManager.setHandler(this.syncMessageHandler);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Create the menu based on what is defined in res/menu/noteview.xml
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_note, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.view_note_send:
				(new Send(this, note)).send();
				return true;
		}
		return super.onOptionsItemSelected(item);
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
		Linkify.addLinks(content, 
						 buildNoteLinkifyPattern(),
						 Tomdroid.CONTENT_URI+"/",
						 null,
						 noteTitleTransformFilter);
	}
	
	private Handler noteXMLParseHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			//parsed ok - show
			if(msg.what == NoteContentBuilder.PARSE_OK) {
				showNote();

			//parsed not ok - error
			} else if(msg.what == NoteContentBuilder.PARSE_ERROR) {
				
				// TODO put this String in a translatable resource
				new AlertDialog.Builder(EditNote.this)
					.setMessage("The requested note could not be parsed. If you see this error " +
								" and you are able to replicate it, please file a bug!")
					.setTitle("Error")
					.setNeutralButton("Ok", new OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
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
				
				// TODO put this String in a translatable resource
				new AlertDialog.Builder(EditNote.this)
					.setMessage("The requested note could not be parsed. If you see this error " +
								" and you are able to replicate it, please file a bug!")
					.setTitle("Error")
					.setNeutralButton("Ok", new OnClickListener() {
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
				
				// Pattern.quote() here make sure that special characters in the note's title are properly escaped 
				sb.append("("+Pattern.quote(title)+")|");
				
			} while (cursor.moveToNext());
			
			// get rid of the last | that is not needed (I know, its ugly.. better idea?)
			String pt = sb.substring(0, sb.length()-1);

			// return a compiled match pattern
			return Pattern.compile(pt);
			
		} else {
			
			// TODO send an error to the user
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "Cursor returned null or 0 notes");
		}
		
		return null;
	}
	
	// custom transform filter that takes the note's title part of the URI and translate it into the note id
	// this was done to avoid problems with invalid characters in URI (ex: ? is the query separator but could be in a note title)
	private TransformFilter noteTitleTransformFilter = new TransformFilter() {

		public String transformUrl(Matcher m, String str) {

			int id = NoteManager.getNoteId(EditNote.this, str);
			
			// return something like content://org.tomdroid.notes/notes/3
			return Tomdroid.CONTENT_URI.toString()+"/"+id;
		}  
	};

	private void updateNoteContent(boolean xml) {
		
		SpannableStringBuilder newNoteContent = new SpannableStringBuilder();
		
		if(xml) {
			// parse XML
			String xmlContent = this.content.getText().toString();
			String subjectName = this.title.getText().toString();
	        TLog.d(TAG, "update from xml content: {0}",xmlContent);
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

				TLog.v(TAG, "parsing note {0}", subjectName);
				
		        sp.parse(noteContentIs, new NoteContentHandler(newNoteContent));
			} catch (Exception e) {
				e.printStackTrace();
				// TODO handle error in a more granular way
				TLog.e(TAG, "There was an error parsing the note {0}", subjectName);
				return;
			}
			
		}
		else
			newNoteContent = (SpannableStringBuilder) this.content.getText();

		// store changed note content
		String newXmlContent = new NoteXMLContentBuilder().setCaller(noteXMLWriteHandler).setInputSource(newNoteContent).build();
		// Since 0.5 EditNote expects the redundant title being removed from the note content, but we still may need this for debugging:
		//note.setXmlContent("<note-content version=\"0.1\">"+note.getTitle()+"\n\n"+newXmlContent+"</note-content>");
		note.setXmlContent("<note-content version=\"0.1\">"+newXmlContent+"</note-content>");
		noteContent = note.getNoteContent(noteXMLParseHandler);
	}
	
	private void saveNote() {
		updateNoteContent(xmlButton.isChecked());
		
		note.setTitle(title.getText().toString());

		Time now = new Time();
		now.setToNow();
		String time = now.format3339(false);
		note.setLastChangeDate(time);

		NoteManager.putNote( this, note );
		noteContent = note.getNoteContent(noteXMLParseHandler);
		
		// put note to server
		
		SyncManager.getInstance().pushNote(note);

	}

	private void addFormatListeners()
	{

		final ToggleButton xmlButton = (ToggleButton) findViewById(R.id.xml);   
        
		xmlButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	 
            	if(xmlButton.isChecked()) {
        			SpannableStringBuilder newNoteContent = (SpannableStringBuilder) content.getText();

        			// store changed note content
        			String newXmlContent = new NoteXMLContentBuilder().setCaller(noteXMLWriteHandler).setInputSource(newNoteContent).build();
        			// Since 0.5 EditNote expects the redundant title being removed from the note content, but we still may need this for debugging:
        			//note.setXmlContent("<note-content version=\"0.1\">"+note.getTitle()+"\n\n"+newXmlContent+"</note-content>");
        			note.setXmlContent("<note-content version=\"0.1\">"+newXmlContent+"</note-content>");
            		formatBar.setVisibility(View.GONE);
            		content.setText(note.getXmlContent());
            	}
            	else {
            		updateNoteContent(true);
            		if(content.isFocused())
            			formatBar.setVisibility(View.VISIBLE);
		    		showNote();
            	}
            }
        });
		
		final ToggleButton boldButton = (ToggleButton)findViewById(R.id.bold);
		
		boldButton.setOnClickListener(new Button.OnClickListener() {

			public void onClick(View v) {
		    	
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
            		Spannable str = content.getText();
            		StyleSpan[] ss = str.getSpans(selectionStart, selectionEnd, StyleSpan.class);
            		
            		boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            			if (ss[i].getStyle() == android.graphics.Typeface.BOLD){
            				str.removeSpan(ss[i]);
            				exists = true;
            			}
                    }
            		
            		if (!exists){
            			str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		updateNoteContent(xmlButton.isChecked());
	           		content.clearFocus();
            		boldButton.setChecked(false);
            	}
            }
		});
		
		final ToggleButton italicButton = (ToggleButton)findViewById(R.id.italic);
		
		italicButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	            	
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
            		Spannable str = content.getText();
            		StyleSpan[] ss = str.getSpans(selectionStart, selectionEnd, StyleSpan.class);
            		
            		boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            			if (ss[i].getStyle() == android.graphics.Typeface.ITALIC){
            				str.removeSpan(ss[i]);
            				exists = true;
            			}
                    }
            		
            		if (!exists){
            			str.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		
            		updateNoteContent(xmlButton.isChecked());
	           		content.clearFocus();
	          		italicButton.setChecked(false);
            	}
            }
		});

		final Button highButton = (Button)findViewById(R.id.highlight);
		
		highButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	            	
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
            		Spannable str = content.getText();
            		BackgroundColorSpan[] ss = str.getSpans(selectionStart, selectionEnd, BackgroundColorSpan.class);
            		
            		boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
        				str.removeSpan(ss[i]);
        				exists = true;
                    }
            		
            		if (!exists){
            			str.setSpan(new BackgroundColorSpan(Note.NOTE_HIGHLIGHT_COLOR), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		
            		updateNoteContent(xmlButton.isChecked());
	           		content.clearFocus();
            	}
            }
		});
		
		final ToggleButton monoButton = (ToggleButton)findViewById(R.id.mono);
		
		monoButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	            	
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
            		Spannable str = content.getText();
            		TypefaceSpan[] ss = str.getSpans(selectionStart, selectionEnd, TypefaceSpan.class);
            		
            		boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            			if (ss[i].getFamily()==Note.NOTE_MONOSPACE_TYPEFACE){
            				str.removeSpan(ss[i]);
            				exists = true;
            			}
                    }
            		
            		if (!exists){
            			str.setSpan(new TypefaceSpan(Note.NOTE_MONOSPACE_TYPEFACE), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		
            		updateNoteContent(xmlButton.isChecked());
	           		content.clearFocus();
	           		monoButton.setChecked(false);
            	}
            }
		});
		
		final ToggleButton strikeoutButton = (ToggleButton) findViewById(R.id.strike);   
        
		strikeoutButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	 
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
            		Spannable str = content.getText();
            		StrikethroughSpan[] ss = str.getSpans(selectionStart, selectionEnd, StrikethroughSpan.class);
            		
            		boolean exists = false;
            		for (int i = 0; i < ss.length; i++) {
            				str.removeSpan(ss[i]);
            				exists = true;
                    }
            		
            		if (!exists){
            			str.setSpan(new StrikethroughSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            		}
            		updateNoteContent(xmlButton.isChecked());
	           		content.clearFocus();
            		strikeoutButton.setChecked(false);
            	}
            }
        });
        
        content.addTextChangedListener(new TextWatcher() { 
            public void afterTextChanged(Editable s) { 
            	
            	//add style as the user types if a toggle button is enabled
            	ToggleButton boldButton = (ToggleButton) findViewById(R.id.bold);
            	ToggleButton emButton = (ToggleButton) findViewById(R.id.italic);
            	
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
        			
                	if (boldButton.isChecked()){  
                		StyleSpan[] ss = s.getSpans(styleStart, position, StyleSpan.class);

                		for (int i = 0; i < ss.length; i++) {
                			if (ss[i].getStyle() == android.graphics.Typeface.BOLD){
                				s.removeSpan(ss[i]);
                			}
                        }
                		s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                		updateNoteContent(xmlButton.isChecked());
                	}
                	if (emButton.isChecked()){
                		StyleSpan[] ss = s.getSpans(styleStart, position, StyleSpan.class);
                		
                		for (int i = 0; i < ss.length; i++) {
                			if (ss[i].getStyle() == android.graphics.Typeface.ITALIC){
                				s.removeSpan(ss[i]);
                			}
                        }
                		s.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), styleStart, position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                		updateNoteContent(xmlButton.isChecked());
                	}
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
        
		final Button sizeButton = (Button)findViewById(R.id.size);
		
		sizeButton.setOnClickListener(new Button.OnClickListener() {

			public void onClick(View v) {
                sselectionStart = content.getSelectionStart();
            	sselectionEnd = content.getSelectionEnd();
            	
	            TextSizeDialog colorDlg = new TextSizeDialog(EditNote.this, (TextSizeDialog.OnSizeChangedListener)EditNote.this, sselectionStart, sselectionEnd);
            	colorDlg.show();
            }
		});


		
	}
	public void sizeChanged(float size) 
	{
        //((Button)findViewById(R.id.color)).setTextColor(color);
        
        int selectionStart = sselectionStart;
    	int selectionEnd = sselectionEnd;

    	if(selectionStart != selectionEnd)
    	{
        	if (selectionStart > selectionEnd){
        		int temp = selectionEnd;
        		selectionEnd = selectionStart;
        		selectionStart = temp;
        	}
        	
        	Spannable str = content.getText();
        	
        	RelativeSizeSpan[] ss = str.getSpans(selectionStart, selectionEnd, RelativeSizeSpan.class);
    		
    		for (int i = 0; i < ss.length; i++) {
    				str.removeSpan(ss[i]);
            }
        	if(size != 1.0f) {
        		str.setSpan(new RelativeSizeSpan(size), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    			updateNoteContent(xmlButton.isChecked());
        	}
    	}
    }	
}
