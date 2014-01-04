/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2010, Matthew Stevenson <saturnreturn@gmail.com>
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
package org.tomdroid.util;

import java.text.DateFormat;
import java.util.Date;

import org.tomdroid.Note;
import org.tomdroid.R;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/* Provides a custom ListView layout for Note List */

public class NoteListCursorAdapter extends SimpleCursorAdapter {

	// static properties
	private static final String TAG = "NoteListCursorAdapter";

	
    private int layout;
    private Context context;

    private DateFormat localeDateFormat;
    private DateFormat localeTimeFormat;
    
    private int selectedIndex;


    public NoteListCursorAdapter (Context context, int layout, Cursor c, String[] from, int[] to, int selectedIndex) {
        super(context, layout, c, from, to);
        this.layout = layout;
        this.context = context;
        this.selectedIndex = selectedIndex;
        
        localeDateFormat = android.text.format.DateFormat.getDateFormat(context);
        localeTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
    }
    

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        Cursor c = getCursor();

        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(layout, parent, false);

        populateFields(v, c);

        return v;
    }

    @Override
    public void bindView(View v, Context context, Cursor c) {

        populateFields(v, c);
    }
    
    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
    	View v = super.getView(position, convertView, parent);
    	if(this.selectedIndex == position) {
            TextView note_title = (TextView) v.findViewById(R.id.note_title);
            if (note_title != null) {
            	note_title.setTextColor(0xFFFFFFFF);
            }
            TextView note_modified = (TextView) v.findViewById(R.id.note_date);
            if (note_modified != null) {
            	note_modified.setTextColor(0xFFFFFFFF);
            }
    		v.setBackgroundResource(R.drawable.drop_shadow_selected);
    		v.findViewById(R.id.triangle).setBackgroundResource(R.drawable.white_triangle);
    	}
    	else {
            TextView note_title = (TextView) v.findViewById(R.id.note_title);
            if (note_title != null) {
            	note_title.setTextColor(0xFF000000);
            }
            TextView note_modified = (TextView) v.findViewById(R.id.note_date);
            if (note_modified != null) {
            	note_modified.setTextColor(0xFF000000);
            }
    		v.setBackgroundResource(0);
    		v.findViewById(R.id.triangle).setBackgroundResource(0);
    	}
    	return v;
	}
    
    private void populateFields(View v, Cursor c){

        int nameCol = c.getColumnIndex(Note.TITLE);
        int modifiedCol = c.getColumnIndex(Note.MODIFIED_DATE);
        int tagCol = c.getColumnIndex(Note.TAGS);
        
        String title = c.getString(nameCol);
        String tags = c.getString(tagCol);
        
        //Format last modified dates to be similar to desktop Tomboy
        //TODO this is messy - must be a better way than having 3 separate date types
        Time lastModified = new Time();
        lastModified.parseTomboy(c.getString(modifiedCol));
        Long lastModifiedMillis = lastModified.toMillis(false);
        Date lastModifiedDate = new Date(lastModifiedMillis);
        
        String strModified = this.context.getString(R.string.textModified)+" ";
        //TODO this is very inefficient
        if (DateUtils.isToday(lastModifiedMillis)){
        	strModified += this.context.getString(R.string.textToday) +", " + localeTimeFormat.format(lastModifiedDate);
        } else {
        	// Add a day to the last modified date - if the date is now today, it means the note was edited yesterday
        	Time yesterdayTest = lastModified;
        	yesterdayTest.monthDay += 1;
        	if (DateUtils.isToday(yesterdayTest.toMillis(false))){
        		strModified += this.context.getString(R.string.textYexterday) +", " + localeTimeFormat.format(lastModifiedDate);
        	} else {
        		strModified += localeDateFormat.format(lastModifiedDate) + ", " + localeTimeFormat.format(lastModifiedDate);
        	}
        }

        /**
         * Next set the name of the entry.
         */
        TextView note_title = (TextView) v.findViewById(R.id.note_title);
        if (note_title != null) {
        	note_title.setText(title);
            if(tags.contains("system:deleted"))
            	note_title.setPaintFlags(note_title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            else
            	note_title.setPaintFlags(note_title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        TextView note_modified = (TextView) v.findViewById(R.id.note_date);
        if (note_modified != null) {
        	note_modified.setText(strModified);
        }
    }

}
